@file:OptIn(InternalSerializationApi::class)

package com.lightningkite.lightningserver.typed

import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.*
import kotlinx.html.*
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KType

context(ServerBuilder)
@LightningServerDsl
fun ServerPath.apiHelp() = get.handler { request ->
    val rootRoute = this
    HttpResponse(body = HttpContent.Html {
        head { title("Index") }
        body {
            h1 { +"API Docs" }
            h2 { +"Endpoints" }
            for (api in server.typedEndpoints) {
                h3 {
                    +(api.route.method.toString())
                    +" "
                    +api.route.path.toString()
                    +" - "
                    +api.summary
                }
                div {
                    p { +api.description }
                    p {
                        +"Input: "
                        api.inputType.let {
                            type(serialization, it)
                        } ?: run {
                            +"N/A"
                        }
                    }
                    p {
                        +"Output: "
                        api.outputType.let {
                            type(serialization, it)
                        } ?: run {
                            +"N/A"
                        }
                    }
                    p {
                        api.authInfo.type?.let {
                            if (api.authInfo.required) {
                                +"You need to be authenticated as a: "
                                +it
                            } else {
                                +"You may be authenticated as a: "
                                +it
                            }
                        } ?: run {
                            +"This endpoint requires no authentication."
                        }
                    }
                }
            }

            h2 { +"Types" }

            server.usedTypes
                .sortedBy { it.descriptor.serialName.substringBefore('<').substringAfterLast('.') }
                .forEach { serializer ->
                    val desc = serializer.descriptor
                    when (desc.kind) {
                        StructureKind.CLASS -> {
                            documentType(serializer) {
                                for ((index, part) in ((serializer as? GeneratedSerializer<*>)?.childSerializers()
                                    ?: arrayOf()).withIndex()) {
                                    p {
                                        +desc.getElementName(index)
                                        +": "
                                        type(serialization, part)
                                    }
                                }
                            }
                        }
                        SerialKind.ENUM -> {
                            documentType(serializer) {
                                p {
                                    +"A string containing one of the following values:"
                                }
                                ul {
                                    for (index in 0 until desc.elementsCount) {
                                        li {
                                            +desc.getElementName(index)
                                        }
                                    }
                                }
                            }
                        }
                        PrimitiveKind.BOOLEAN -> {
                            documentType(serializer) {
                                +"A JSON boolean, either true or false."
                            }
                        }
                        PrimitiveKind.STRING -> {
                            documentType(serializer) {
                                +"A JSON string."
                            }
                        }
                        PrimitiveKind.BYTE,
                        PrimitiveKind.CHAR,
                        PrimitiveKind.SHORT,
                        PrimitiveKind.INT,
                        PrimitiveKind.LONG,
                        PrimitiveKind.FLOAT,
                        PrimitiveKind.DOUBLE -> {
                            documentType(serializer) {
                                +"A JSON number."
                            }
                        }
                        StructureKind.LIST -> {
                            documentType(serializer) {
                                +"A JSON array."
                            }
                        }
                        StructureKind.MAP -> {
                            documentType(serializer) {
                                +"A JSON object, also known as a map or dictionary."
                            }
                        }
                        else -> {}
                    }
                }
        }
    })
}

fun FlowContent.documentType(serializer: KSerializer<*>, body: FlowContent.()->Unit) {
    val desc = serializer.descriptor
    val name = desc.serialName.substringBefore('<').substringAfterLast('.')
    h3 {
        id = name
        +(name)
    }
    desc.annotations.filterIsInstance<Description>().firstOrNull()?.let {
        div {
            it.text.trimIndent().split('\n').forEach {
                p { +it }
            }
        }
    }
    body()
}

private fun FlowContent.type(serialization: Serialization, type: KSerializer<*>) {
    type.nullElement()?.let {
        type(serialization, it)
        +"?"
        return
    }
    if (type is ContextualSerializer) {
        type(serialization, type.uncontextualize(serialization))
        return
    }
    val baseName = type.descriptor.serialName.substringBefore('<').substringAfterLast('.')
    val arguments: Array<KSerializer<*>> = type.subSerializers()
    span {
        a(href = "#$baseName") {
            +(baseName)
        }
        arguments.takeUnless { it.isEmpty() }?.let {
            +"<"
            var first = true
            it.forEach {
                if (first) first = false
                else +", "
                type(serialization, it)
            }
            +">"
        }
    }
}

private fun FlowContent.type(serialization: Serialization, type: KType) = type(serialization, serialization.json.serializersModule.serializer(type))