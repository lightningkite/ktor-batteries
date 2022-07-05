package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.auth.AuthorizationMethod
import com.lightningkite.lightningserver.auth.WsAuthorizationMethod
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.HttpHandler
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.http.HttpResponse
import com.lightningkite.lightningserver.http.HttpEndpoint
import com.lightningkite.lightningserver.schedule.ScheduledTask
import com.lightningkite.lightningserver.serialization.HasSerialization
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.task.StartupTask
import com.lightningkite.lightningserver.task.Task
import com.lightningkite.lightningserver.websocket.WebSocketHandler
import kotlinx.serialization.KSerializer

data class Server(
    val name: String,
    override val serialization: Serialization,
    val authorizationMethod: AuthorizationMethod = { null },
    val wsAuthorizationMethod: WsAuthorizationMethod = { null },
    val resources: Set<Server.ResourceRequirement<*, *>>,
    val settings: Set<Server.Setting<*>>,
    val endpoints: Map<HttpEndpoint, HttpHandler>,
    val endpointException: suspend ServerRunner.(HttpRequest, Exception) -> HttpResponse,
    val websockets: Map<ServerPath, WebSocketHandler>,
    val schedules: Set<ScheduledTask>,
    val tasks: Set<Task<*>>,
    val startupTasks: Set<StartupTask>,
    val startupActions: Set<ServerRunner.()->Unit>
): HasSerialization {
    interface ResourceRequirement<T, S> {
        val name: String
        val type: String
        val serializer: KSerializer<S>
        fun ServerRunner.fromExplicit(setting: S): T
        fun default(): S
    }
    class Setting<T>(val name: String, val serializer: KSerializer<T>, val default: () -> T) {
        override fun hashCode(): Int = name.hashCode()
        override fun equals(other: Any?): Boolean = other is Setting<*> && this.name == other.name
        override fun toString(): String = "$name: ${serializer.descriptor.serialName}"
    }
}

fun Server.defaultSettings(): Map<Server.Setting<*>, Any?> = settings.associateWith { it.default() }
fun Server.defaultResourceSettings(): Map<Server.ResourceRequirement<*, *>, Any?> = resources.associateWith { it.default() }
fun Server.validateSettings(settings: Map<Server.Setting<*>, Any?>): Map<Server.Setting<*>, Any?>? {
    val missing = (this.settings - settings.keys)
    if(missing.isEmpty()) return null
    else return settings + missing.associateWith { it.default() }
}
fun Server.validateResourceSettings(settings: Map<Server.ResourceRequirement<*, *>, Any?>): Map<Server.ResourceRequirement<*, *>, Any?>? {
    val missing = (this.resources - settings.keys)
    if(missing.isEmpty()) return null
    else return settings + missing.associateWith { it.default() }
}