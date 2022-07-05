package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.exceptions.HttpStatusException
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.schedule.ScheduledTask
import com.lightningkite.lightningserver.serialization.HasSerialization
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.task.StartupTask
import com.lightningkite.lightningserver.task.Task
import com.lightningkite.lightningserver.websocket.WebSocketHandler

class ServerBuilder(val name: String): HasSerialization {
    override var serialization: Serialization = Serialization()
    private val requirements: MutableSet<Server.ResourceRequirement<*>> = HashSet()
    private val settings: MutableSet<Server.Setting<*>> = HashSet()
    private val endpoints: MutableMap<HttpRoute, HttpHandler> = HashMap()
    var endpointException: suspend ServerRunner.(HttpRequest, Exception) -> HttpResponse = { request, exception ->
        if (exception is HttpStatusException) {
            exception.toResponse(request)
        } else {
            HttpResponse(status = HttpStatus.InternalServerError)
        }
    }
    private val websockets: MutableMap<ServerPath, WebSocketHandler> = HashMap()
    private val schedules: MutableSet<ScheduledTask> = HashSet()
    private val tasks: MutableSet<Task<*>> = HashSet()
    private val startupTasks: MutableSet<StartupTask> = HashSet()

    fun <T> require(requirement: Server.ResourceRequirement<T>): Server.ResourceRequirement<T> {
        requirements.add(requirement)
        return requirement
    }

    fun <T> require(requirement: Server.Setting<T>): Server.Setting<T> {
        settings.add(requirement)
        return requirement
    }

    fun require(requirement: ScheduledTask): ScheduledTask {
        schedules.add(requirement)
        return requirement
    }

    fun <T> require(requirement: Task<T>): Task<T> {
        tasks.add(requirement)
        return requirement
    }

    fun <T> require(requirement: StartupTask): StartupTask {
        startupTasks.add(requirement)
        return requirement
    }

    var HttpRoute.handler: HttpHandler?
        get() = endpoints[this]
        set(value) {
            if(value != null) endpoints[this] = value
        }
    var ServerPath.webSocketHandler: WebSocketHandler?
        get() = websockets[this]
        set(value) {
            if(value != null) websockets[this] = value
        }

    fun build(): Server = Server(
        name = name,
        serialization = serialization,
        requirements = requirements,
        settings = settings.associateBy { it.name },
        endpoints = endpoints,
        endpointException = endpointException,
        websockets = websockets,
        schedules = schedules,
        tasks = tasks,
        startupTasks = startupTasks,
    )
}