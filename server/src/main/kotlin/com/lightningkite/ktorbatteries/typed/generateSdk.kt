package com.lightningkite.ktorbatteries.typed

import com.lightningkite.ktorbatteries.routes.fullPath
import com.lightningkite.ktorbatteries.routes.maybeMethod
import com.lightningkite.ktordb.comparator
import io.ktor.server.routing.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

object SDK {
    fun apiFile(packageName: String): CodeEmitter = CodeEmitter(packageName).apply {
        imports.add("io.reactivex.rxjava3.core.Single")
        imports.add("io.reactivex.rxjava3.core.Observable")
        imports.add("com.lightningkite.rx.okhttp.*")
        val byGroup = (ApiEndpoint.known + ApiWebsocket.known).groupBy { it.docGroup }
        val groups = byGroup.keys.filterNotNull()
        appendLine("interface Api {")
        for(group in groups) {
            appendLine("    val ${group.groupToPartName()}: ${group.groupToInterfaceName()}")
        }
        for(entry in byGroup[null] ?: listOf()) {
            append("    ")
            this.functionHeader(entry)
            appendLine()
        }
        for(group in groups) {
            appendLine("    interface ${group.groupToInterfaceName()} {")
            for(entry in byGroup[group]!!) {
                append("        ")
                this.functionHeader(entry)
                appendLine()
            }
            appendLine("    }")
        }
        appendLine("}")
        appendLine()
    }
    fun liveFile(packageName: String): CodeEmitter = CodeEmitter(packageName).apply {
        imports.add("io.reactivex.rxjava3.core.Single")
        imports.add("io.reactivex.rxjava3.core.Observable")
        imports.add("com.lightningkite.rx.android.resources.ImageReference")
        imports.add("com.lightningkite.rx.kotlin")
        imports.add("com.lightningkite.rx.okhttp.*")
        imports.add("com.lightningkite.ktordb.live.*")
        val byGroup = (ApiEndpoint.known + ApiWebsocket.known).groupBy { it.docGroup }
        val groups = byGroup.keys.filterNotNull()
        appendLine("class LiveApi(val httpUrl: String, val socketUrl: String = httpUrl): Api {")
        for(group in groups) {
            appendLine("    override val ${group.groupToPartName()}: Live${group.groupToInterfaceName()} = Live${group.groupToInterfaceName()}(httpUrl = httpUrl, socketUrl = socketUrl)")
        }
        for(entry in byGroup[null] ?: listOf()) {
            append("    override ")
            this.functionHeader(entry)
            when(entry) {
                is ApiEndpoint<*, *, *> -> {
                    appendLine(" = HttpClient.call(")
                    appendLine("        url = \"\$httpUrl${entry.route.fullPath}\",")
                    appendLine("        method = HttpClient.${entry.route.selector.maybeMethod?.value?.uppercase() ?: "GET"},")
                    entry.inputType?.let {
                        appendLine("        body = input.toJsonRequestBody()")
                    }
                    entry.outputType?.let {
                        appendLine("    ).readJson()")
                    } ?: run {
                        appendLine("    ).discard()")
                    }
                }
                is ApiWebsocket<*, *, *> -> {
                    appendLine(" = multiplexedSocket(url = \"\$httpUrl/multiplex\", path = \"${entry.route.fullPath}\")")
                }
            }
        }
        for(group in groups) {
            appendLine("    class Live${group.groupToInterfaceName()}(val httpUrl: String, val socketUrl: String = httpUrl): Api.${group.groupToInterfaceName()} {")
            for(entry in byGroup[group]!!) {
                append("        override ")
                this.functionHeader(entry)
                when(entry) {
                    is ApiEndpoint<*, *, *> -> {
                        appendLine(" = HttpClient.call(")
                        appendLine("            url = \"\$httpUrl${entry.route.fullPath}\",")
                        appendLine("            method = HttpClient.${entry.route.selector.maybeMethod?.value?.uppercase() ?: "GET"},")
                        entry.inputType?.let {
                            appendLine("            body = input.toJsonRequestBody()")
                        }
                        entry.outputType?.let {
                            appendLine("        ).readJson()")
                        } ?: run {
                            appendLine("        ).discard()")
                        }
                    }
                    is ApiWebsocket<*, *, *> -> {
                        appendLine(" = multiplexedSocket(url = \"\$httpUrl/multiplex\", path = \"${entry.route.fullPath}\")")
                    }
                }
            }
            appendLine("    }")
        }
        appendLine("}")
        appendLine()
    }
}

public class CodeEmitter(val packageName: String, val body: StringBuilder = StringBuilder()): Appendable by body {
    val imports = mutableSetOf<String>()
    fun append(type: KType) {
        imports.add(type.toString().substringBefore('<'))
        body.append((type.classifier as? KClass<*>)?.simpleName)
        type.arguments.takeIf { it.isNotEmpty() }?.let {
            body.append('<')
            var first = true
            it.forEach {
                if(first) first = false
                else body.append(", ")
                it.type?.let { append(it) } ?: body.append('*')
            }
            body.append('>')
        }
    }
    fun dump(to: Appendable) = with(to) {
        appendLine("package $packageName")
        appendLine()
        imports
            .filter { it.substringAfterLast('.') != packageName }
            .forEach { appendLine("import $it") }
        appendLine()
        append(body)
    }

    override fun toString(): String = StringBuilder().also { dump(it) }.toString()
}

private fun String.groupToInterfaceName(): String = replaceFirstChar { it.uppercase() } + "Api"
private fun String.groupToPartName(): String = replaceFirstChar { it.lowercase() }

private fun CodeEmitter.functionHeader(documentable: Documentable): java.lang.Appendable? {
    append("fun ${documentable.functionName}(")
    var argComma = false
    arguments(documentable).forEach {
        if(argComma) append(", ")
        else argComma = true
        append(it.name)
        append(": ")
        it.type?.let { append(it) } ?: append(it.stringType ?: "Never")
    }
    append("): ")
    return when (documentable) {
        is ApiEndpoint<*, *, *> -> {
            documentable.outputType?.let {
                append("Single<")
                append(it)
                append(">")
            } ?: append("Single<Unit>")
        }
        is ApiWebsocket<*, *, *> -> {
            append("Observable<WebSocketIsh<")
            append(documentable.inputType)
            append(", ")
            append(documentable.outputType)
            append(">>")
        }
        else -> TODO()
    }
}

private data class Arg(val name: String, val type: KType? = null, val stringType: String? = null, val default: String? = null)

private fun arguments(documentable: Documentable): List<Arg> = when (documentable) {
    is ApiEndpoint<*, *, *> -> listOfNotNull(
        documentable.userType?.let {
            Arg(name = (it.classifier as? KClass<Any>)?.simpleName?.replaceFirstChar { it.lowercase() }
                ?.plus("Token") ?: "token", stringType = "String")
        }?.let(::listOf),
        generateSequence(documentable.route) { it.parent }.toList().reversed()
            .mapNotNull { it.selector as? PathSegmentParameterRouteSelector }
            .map {
                Arg(name = it.name, type = documentable.routeTypes[it.name], stringType = "String")
            },
        documentable.inputType?.let {
            Arg(name = "input", type = it)
        }?.let(::listOf)
    ).flatten()
    is ApiWebsocket<*, *, *> -> listOfNotNull(
        documentable.userType?.let {
            Arg(name = (it.classifier as? KClass<Any>)?.simpleName?.replaceFirstChar { it.lowercase() }
                ?.plus("Token") ?: "token", stringType = "String")
        }?.let(::listOf),
        generateSequence(documentable.route) { it.parent }.toList().reversed()
            .mapNotNull { it.selector as? PathSegmentParameterRouteSelector }
            .map {
                Arg(name = it.name, stringType = "String")
            }
    ).flatten()
    else -> TODO()
}
