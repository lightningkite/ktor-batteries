package com.lightningkite.lightningserver.files

import com.dalet.vfs2.provider.azure.AzFileObject
import com.github.vfss3.S3FileObject
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.auth.JwtSignSettings
import kotlinx.serialization.builtins.serializer
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.provider.local.LocalFile
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.relativeTo

private const val allowedChars = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"

/**
 * Will generate a Public facing URL that does not account for any security.
 */
val FileObject.publicUrlUnsigned: String
    get() {
        return when(this) {
            is LocalFile -> "${fileSystem.lightningServer.publicUrl}/${
                path.relativeTo(Path.of(fileSystem.lightningServer.localFilesPath)).toString()
                    .replace("\\", "/")
            }"
            else -> URL("https", url.host, url.port, url.file).toString()
        }
    }

/**
 * Will generate a Public facing URL that will account for security.
 */
val FileObject.publicUrl: String
    get() {
        return when(this) {
            is LocalFile -> {
                "${fileSystem.lightningServer.publicUrl}/${
                    path.relativeTo(Path.of(fileSystem.lightningServer.localFilesPath)).toString()
                        .replace("\\", "/")
                }"
            }
            is S3FileObject -> {
                fileSystem.lightningServer.expirationSeconds?.let { seconds ->
                    unstupidSignUrl(seconds)
                } ?: URL("https", url.host, url.port, url.file).toString()
            }
            is AzFileObject -> {
                val url = fileSystem.lightningServer.expirationSeconds?.let { seconds ->
                    getSignedUrl(seconds)
                } ?: URL("https", url.host, url.port, url.file)
                url.toString()
            }
            else -> {
                URL("https", url.host, url.port, url.file).toString()
            }
        }
    }

/**
 * Generates a URL that will allow a direct upload of a file to the Filesystem.
 */
fun FileObject.signedUploadUrl(expirationSeconds: Int = (7 * 60)): String {
    return when(this) {
        is LocalFile -> {
            val path = path.relativeTo(Path.of(fileSystem.lightningServer.localFilesPath)).toString()
                .replace("\\", "/")
            "${fileSystem.lightningServer.publicUrl}/$path"
        }
        is S3FileObject -> {
            this.uploadUrl(expirationSeconds)
        }
        is AzFileObject -> {
            this.uploadUrl(expirationSeconds)
        }
        else -> throw UnsupportedOperationException("No supported upload URL for $this")
    }
}

fun getRandomString(length: Int, allowedChars: String): String = (1..length)
    .map { allowedChars.random() }
    .joinToString("")


fun FileObject.resolveFileWithUniqueName(path: String): FileObject {
    val name = path.substringBeforeLast(".")
    val extension = path.substringAfterLast('.', "")
    var random = ""
    var exists = true
    while (exists) {
        exists = resolveFile("$name$random.$extension").exists()
        if (exists) {
            random = "-${getRandomString(10, allowedChars)}"
        }
    }
    return resolveFile("$name$random.$extension")
}

fun FileObject.upload(stream: InputStream): FileObject {
    this.content.outputStream
        .buffered()
        .use { out -> stream.copyTo(out) }
    return this
}