@file:OptIn(InternalSerializationApi::class)

package com.lightningkite.lightningserver.typed

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.auth.AuthInfo
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.HttpMethod
import com.lightningkite.lightningserver.routes.docName
import com.lightningkite.lightningserver.serialization.Serialization
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.html.*
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KType

interface Documentable {
    val path: ServerPath
    val summary: String
    val description: String
    val authInfo: AuthInfo<*>

    companion object {
    }
}
val Documentable.docGroup: String? get() = generateSequence(path) { it.parent }.mapNotNull { it.docName }.firstOrNull()
val Documentable.functionName: String get() = summary.split(' ').joinToString("") { it.replaceFirstChar { it.uppercase() } }.replaceFirstChar { it.lowercase() }

internal fun KSerializer<*>.subSerializers(): Array<KSerializer<*>> = listElement()?.let { arrayOf(it) }
    ?: mapValueElement()?.let { arrayOf(it) }
    ?: (this as? GeneratedSerializer<*>)?.typeParametersSerializers()
    ?: (this as? ConditionSerializer<*>)?.inner?.let { arrayOf(it) }
    ?: (this as? ModificationSerializer<*>)?.inner?.let { arrayOf(it) }
    ?: arrayOf()
internal fun KSerializer<*>.subAndChildSerializers(): Array<KSerializer<*>> = listElement()?.let { arrayOf(it) }
    ?: mapValueElement()?.let { arrayOf(it) }
    ?: (this as? GeneratedSerializer<*>)?.run { childSerializers() + typeParametersSerializers() }
    ?: (this as? ConditionSerializer<*>)?.inner?.let { arrayOf(it) }
    ?: (this as? ModificationSerializer<*>)?.inner?.let { arrayOf(it) }
    ?: arrayOf()
internal fun KSerializer<*>.uncontextualize(serialization: Serialization): KSerializer<*> = if (this.descriptor.kind == SerialKind.CONTEXTUAL)
    serialization.json.serializersModule.getContextual(descriptor.capturedKClass!!)!!
else this

val Server.typedEndpoints get() = endpoints.values.asSequence().filterIsInstance<ApiEndpoint<*, *, *>>()
val Server.typedWebsockets get() = websockets.values.asSequence().filterIsInstance<ApiWebsocket<*, *, *>>()
val Server.allTyped get() = typedEndpoints + typedWebsockets
val Server.usedTypes: Collection<KSerializer<*>> get() {
    val seen: HashSet<String> = HashSet()
    fun onAllTypes(at: KSerializer<*>, action: (KSerializer<*>) -> Unit) {
        val real = (at.nullElement() ?: at).uncontextualize(serialization)
        if (!seen.add(real.descriptor.serialName.substringBefore('<'))) return
        action(real)
        real.subAndChildSerializers().forEach { onAllTypes(it, action) }
    }
    val types = HashMap<String, KSerializer<*>>()
    typedEndpoints.flatMap {
        sequenceOf(it.inputType, it.outputType)
    }.plus(typedWebsockets.flatMap {
        sequenceOf(it.inputType, it.outputType)
    })
        .forEach { onAllTypes(it) { types[it.descriptor.serialName.substringBefore('<')] = it } }
    return types.values
}