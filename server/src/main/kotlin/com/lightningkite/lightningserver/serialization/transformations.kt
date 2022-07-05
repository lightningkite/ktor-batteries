package com.lightningkite.lightningserver.serialization

import com.lightningkite.lightningdb.ServerFile
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.ContentType
import com.lightningkite.lightningserver.exceptions.BadRequestException
import com.lightningkite.lightningserver.files.files
import com.lightningkite.lightningserver.files.publicUrl
import com.lightningkite.lightningserver.files.resolveFileWithUniqueName
import com.lightningkite.lightningserver.files.upload
import com.lightningkite.lightningserver.http.HttpContent
import com.lightningkite.lightningserver.http.HttpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer


private val multipartJsonKey = "__json"

inline fun <reified T> HttpRequest.queryParameters(serialization: Serialization, ): T = queryParameters(serialization, serializerOrContextual())
fun <T> HttpRequest.queryParameters(serialization: Serialization, serializer: KSerializer<T>): T {
    @Suppress("UNCHECKED_CAST")
    if(serializer == Unit.serializer()) return Unit as T
    return serialization.properties.decodeFromStringMap<T>(
        serializer,
        queryParameters.groupBy { it.first }.mapValues { it.value.joinToString(",") }
    )
}

suspend inline fun <reified T> HttpContent.parse(runner: ServerRunner, ): T = parse(runner, serializerOrContextual())
suspend fun <T> HttpContent.parse(runner: ServerRunner, serializer: KSerializer<T>): T = with(runner) {
    try {
        @Suppress("UNCHECKED_CAST")
        if (serializer == Unit.serializer()) return Unit as T
        val content = this@parse
        return when (content.type) {
            ContentType.Application.Json -> when (val body = content) {
                is HttpContent.Text -> serialization.json.decodeFromString(serializer, body.string)
                is HttpContent.Binary -> serialization.json.decodeFromString(
                    serializer,
                    body.bytes.toString(Charsets.UTF_8)
                )
                is HttpContent.Multipart -> throw BadRequestException("Expected JSON, but got a multipart body.")
                else -> withContext(Dispatchers.IO) {
                    serialization.json.decodeFromStream(
                        serializer,
                        body.stream()
                    )
                }
            }
            ContentType.Text.CSV -> when (val body = content) {
                is HttpContent.Text -> serialization.csv.decodeFromString(serializer, body.string)
                is HttpContent.Binary -> serialization.csv.decodeFromString(
                    serializer,
                    body.bytes.toString(Charsets.UTF_8)
                )
                is HttpContent.Multipart -> throw BadRequestException("Expected JSON, but got a multipart body.")
                else -> withContext(Dispatchers.IO) {
                    serialization.csv.decodeFromString(
                        serializer,
                        stream().bufferedReader().readText()
                    )
                }
            }
            ContentType.Application.Bson -> when (val body = content) {
                is HttpContent.Text -> serialization.bson.load(serializer, body.string.toByteArray())
                is HttpContent.Binary -> serialization.bson.load(serializer, body.bytes)
                is HttpContent.Multipart -> throw BadRequestException("Expected JSON, but got a multipart body.")
                else -> withContext(Dispatchers.IO) {
                    serialization.bson.load(
                        serializer,
                        stream().readBytes()
                    )
                }
            }
            ContentType.Application.Cbor -> when (val body = content) {
                is HttpContent.Text -> serialization.cbor.decodeFromByteArray(serializer, body.string.toByteArray())
                is HttpContent.Binary -> serialization.cbor.decodeFromByteArray(serializer, body.bytes)
                is HttpContent.Multipart -> throw BadRequestException("Expected JSON, but got a multipart body.")
                else -> withContext(Dispatchers.IO) {
                    serialization.cbor.decodeFromByteArray(
                        serializer,
                        body.stream().readBytes()
                    )
                }
            }
            ContentType.MultiPart.FormData -> when (val body = content) {
                is HttpContent.Multipart -> {
                    val mainData = HashMap<String, Any?>()
                    val overrideData = HashMap<String, Any?>()
                    var baselineJson: JsonElement = JsonNull
                    body.parts.collect { part ->
                        when (part) {
                            is HttpContent.Multipart.Part.FormItem -> {
                                if (part.key == multipartJsonKey) {
                                    baselineJson = serialization.json.parseToJsonElement(part.value)
                                }
                            }
                            is HttpContent.Multipart.Part.DataItem -> {
                                if (part.filename.isBlank()) return@collect
                                val path = part.key.split('.')
                                if (!serializer.isFile(serialization, path)) throw BadRequestException("${part.key} is not a ServerFile.")
                                if (part.headers.contentType == null) throw BadRequestException("Content type not provided for uploaded file")
                                //if (
                                //    isFile.allowedTypes
                                //        .asSequence()
                                //        .map { ContentType.parse(it) }
                                //        .none { part.contentType!!.match(it) }
                                //) {
                                //    throw BadRequestException("Content type ${part.contentType} doesn't match any of the accepted types: ${isFile.allowedTypes.joinToString()}")
                                //}
                                part.content.stream()
                                    .use { input ->
                                        files().root.resolveFileWithUniqueName(
                                            "files/${part.filename}"
                                        ).upload(input)
                                    }
                                    .let {
                                        var current: MutableMap<String, Any?> = overrideData
                                        for (pathPart in path.dropLast(1)) {
                                            @Suppress("UNCHECKED_CAST")
                                            current = current[pathPart] as? MutableMap<String, Any?> ?: HashMap()
                                        }
                                        current[path.last()] = JsonPrimitive(it.publicUrl)
                                    }
                            }
                        }
                    }
                    if (baselineJson is JsonObject) {
                        baselineJson.jsonObject.writeInto(mainData)
                        mainData.putAll(overrideData)
                        serialization.json.decodeFromJsonElement(serializer, mainData.toJsonObject())
                    } else throw BadRequestException("")
                }
                else -> throw BadRequestException("Expected multipart body, but got a ${body::class.simpleName}.")
            }
            else -> throw BadRequestException("Content type ${content.type} not supported.")
        }
    } catch(e: SerializationException) {
        throw BadRequestException(e.message, cause = e.cause)
    }
}

suspend inline fun <reified T> T.toHttpContent(serialization: Serialization, acceptedTypes: List<ContentType>): HttpContent? =
    toHttpContent(serialization, acceptedTypes, serializerOrContextual())

suspend fun <T> T.toHttpContent(serialization: Serialization, acceptedTypes: List<ContentType>, serializer: KSerializer<T>): HttpContent? {
    if(this == Unit) return null
    for (contentType in acceptedTypes) {
        when (contentType) {
            ContentType.Application.Json -> return HttpContent.Text(
                serialization.json.encodeToString(serializer, this),
                contentType
            )
            ContentType.Text.CSV -> return HttpContent.Text(
                serialization.csv.encodeToString(serializer, this),
                contentType
            )
            ContentType.Application.Bson -> return HttpContent.Binary(
                serialization.bson.dump(serializer, this),
                contentType
            )
            ContentType.Application.Cbor -> return HttpContent.Binary(
                serialization.cbor.encodeToByteArray(
                    serializer,
                    this
                ), contentType
            )
            else -> {}
        }
    }
    return HttpContent.Text(
        serialization.json.encodeToString(serializer, this),
        ContentType.Application.Json
    )
}


@OptIn(ExperimentalSerializationApi::class)
private fun KSerializer<*>.isFile(serialization: Serialization, parts: List<String>): Boolean {
    var current = this.descriptor
    for (part in parts.dropLast(1)) {
        val index = current.getElementIndex(part)
        if (index == CompositeDecoder.UNKNOWN_NAME) return false
        current = current.getElementDescriptor(index)
    }
    var descriptor = current.getElementDescriptor(current.getElementIndex(parts.last()))
    if (descriptor.kind == SerialKind.CONTEXTUAL) {
        descriptor = serialization.module.getContextualDescriptor(descriptor)!!
    }
    return descriptor == serialization.module.getContextual(ServerFile::class)?.descriptor
}

@Suppress("UNCHECKED_CAST")
private fun JsonObject.writeInto(map: MutableMap<String, Any?> = HashMap()): MutableMap<String, Any?> {
    for ((key, value) in this) {
        map[key] = when (value) {
            is JsonObject -> value.writeInto(map[key] as? HashMap<String, Any?> ?: HashMap())
            else -> value
        }
    }
    return map
}

private fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    for ((key, value) in this@toJsonObject) {
        @Suppress("UNCHECKED_CAST")
        put(
            key, when (value) {
                is JsonElement -> value
                is Map<*, *> -> (value as Map<String, Any?>).toJsonObject()
                else -> throw NotImplementedError()
            }
        )
    }
}