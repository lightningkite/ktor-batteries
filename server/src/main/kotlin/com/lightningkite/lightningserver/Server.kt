package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.HttpHandler
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.http.HttpResponse
import com.lightningkite.lightningserver.http.HttpRoute
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
    val requirements: Set<Server.ResourceRequirement<*, *>>,
    val settings: Set<Server.Setting<*>>,
    val endpoints: Map<HttpRoute, HttpHandler>,
    val endpointException: suspend ServerRunner.(HttpRequest, Exception) -> HttpResponse,
    val websockets: Map<ServerPath, WebSocketHandler>,
    val schedules: Set<ScheduledTask>,
    val tasks: Set<Task<*>>,
    val startupTasks: Set<StartupTask>
): HasSerialization {
    interface ResourceRequirement<T, S> {
        val name: String
        val type: String
        val serializer: KSerializer<S>
        fun ServerRunner.fromExplicit(setting: S): T
        fun default(): S
    }
    data class Setting<T>(val name: String, val serializer: KSerializer<T>, val default: ServerRunner.() -> T)
}