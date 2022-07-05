package com.lightningkite.lightningserver.files

import com.dalet.vfs2.provider.azure.AzFileObject
import com.github.vfss3.S3FileObject
import com.lightningkite.lightningserver.client
import com.lightningkite.lightningdb.ServerFile
import com.lightningkite.lightningserver.ServerRunner
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.URLDecoder
import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Used to serialize and deserialize a ServerFile as a String. This will also handle security for ServerFiles.
 * If security is required it will serialize as a pre-signed URL. It will also check deserializing of url to confirm it is valid.
 */
class ExternalServerFileSerializer(val runner: ServerRunner): KSerializer<ServerFile> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = object: SerialDescriptor {
        override val kind: SerialKind = PrimitiveKind.STRING
        override val serialName: String = "ServerFile"
        override val elementsCount: Int get() = 0
        override fun getElementName(index: Int): String = error()
        override fun getElementIndex(name: String): Int = error()
        override fun isElementOptional(index: Int): Boolean = error()
        override fun getElementDescriptor(index: Int): SerialDescriptor = error()
        override fun getElementAnnotations(index: Int): List<Annotation> = error()
        override fun toString(): String = "PrimitiveDescriptor($serialName)"
        private fun error(): Nothing = throw IllegalStateException("Primitive descriptor does not have elements")
        override val annotations: List<Annotation> = ServerFile::class.annotations
    }

    override fun serialize(encoder: Encoder, value: ServerFile) = with(runner) {
        val root = files().root
        val rootUrl = root.publicUrlUnsigned
        if(!value.location.startsWith(rootUrl)) {
            LoggerFactory.getLogger("com.lightningkite.lightningserver.files").warn("The given url (${value.location}) does not start with the files root ($rootUrl).")
            encoder.encodeString(value.location)
        } else {
            val newFile = root.resolveFile(value.location.removePrefix(rootUrl).substringBefore('?'))
            encoder.encodeString(newFile.publicUrl)
        }
    }

    override fun deserialize(decoder: Decoder): ServerFile = with(runner) {
        val root = files().root
        val rootUrl = root.publicUrlUnsigned
        val raw = decoder.decodeString()
        if(!raw.startsWith(rootUrl)) throw BadRequestException("The given url ($raw) does not start with the files root ($rootUrl).")
        val newFile = root.resolveFile(raw.removePrefix(rootUrl))
        when(newFile) {
            is AzFileObject -> {
                if(signedUrlExpirationSeconds() != null) {
                    // TODO: A local check like we do for AWS would be more performant
                    runBlocking {
                        if(!client.get(raw) { header("Range", "bytes=0-0") }.status.isSuccess()) throw BadRequestException("URL does not appear to be signed properly")
                    }
                }
            }
            is S3FileObject -> {
                signedUrlExpirationSeconds()?.let { exp ->
                    val headers = raw.substringAfter('?').split('&').associate {
                        URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to URLDecoder.decode(it.substringAfter('=', ""), Charsets.UTF_8)
                    }
                    val date = headers["X-Amz-Date"]!!
                    val dateParsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date)

                    if(newFile.unstupidSignUrl(exp, dateParsed) == raw) {
                        runBlocking {
                            if(!client.get(raw) { header("Range", "bytes=0-0") }.status.isSuccess()) throw BadRequestException("URL does not appear to be signed properly")
                        }
                    }
                }
            }
        }
        return ServerFile(raw)
    }
}

private fun ByteArray.toHex(): String = BigInteger(1, this@toHex).toString(16)
private fun ByteArray.mac(key: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").apply {
    init(SecretKeySpec(key, "HmacSHA256"))
}.doFinal(this)
private fun String.sha256(): String = MessageDigest.getInstance("SHA-256").digest(toByteArray()).toHex()
