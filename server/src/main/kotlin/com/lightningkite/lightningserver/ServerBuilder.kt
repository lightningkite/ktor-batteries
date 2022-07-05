package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.auth.AuthorizationMethod
import com.lightningkite.lightningserver.auth.WsAuthorizationMethod
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.exceptions.HttpStatusException
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.logging.LoggingAppender
import com.lightningkite.lightningserver.logging.LoggingSettings
import com.lightningkite.lightningserver.schedule.ScheduledTask
import com.lightningkite.lightningserver.serialization.HasSerialization
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.task.StartupTask
import com.lightningkite.lightningserver.task.Task
import com.lightningkite.lightningserver.websocket.WebSocketHandler

class ServerBuilder(val name: String) : HasSerialization {
    data class Endpoint(val route: HttpEndpoint, val builder: ServerBuilder) {
        @LightningServerDsl
        fun handler(handler: HttpHandler) = with(builder) {
            route.handler = handler
        }
    }

    data class Path(val path: ServerPath, val builder: ServerBuilder) {
        @LightningServerDsl
        fun route(method: HttpMethod): Endpoint = Endpoint(HttpEndpoint(path, method), builder)

        @LightningServerDsl
        fun path(string: String): Path = Path(path.path(string), builder)

        @LightningServerDsl
        val get: Endpoint get() = route(HttpMethod.GET)

        @LightningServerDsl
        val post: Endpoint get() = route(HttpMethod.POST)

        @LightningServerDsl
        val put: Endpoint get() = route(HttpMethod.PUT)

        @LightningServerDsl
        val patch: Endpoint get() = route(HttpMethod.PATCH)

        @LightningServerDsl
        val delete: Endpoint get() = route(HttpMethod.DELETE)

        @LightningServerDsl
        val head: Endpoint get() = route(HttpMethod.HEAD)

        @LightningServerDsl
        fun get(path: String): Endpoint = path(path).route(HttpMethod.GET)

        @LightningServerDsl
        fun post(path: String): Endpoint = path(path).route(HttpMethod.POST)

        @LightningServerDsl
        fun put(path: String): Endpoint = path(path).route(HttpMethod.PUT)

        @LightningServerDsl
        fun patch(path: String): Endpoint = path(path).route(HttpMethod.PATCH)

        @LightningServerDsl
        fun delete(path: String): Endpoint = path(path).route(HttpMethod.DELETE)

        @LightningServerDsl
        fun head(path: String): Endpoint = path(path).route(HttpMethod.HEAD)
    }

    @LightningServerDsl
    inline fun path(string: String, setup: Path.()->Unit = {}) = Path(ServerPath(string), this).apply(setup)

    @LightningServerDsl
    @Deprecated("Use 'path' instead", ReplaceWith("path(string, setup)"))
    inline fun route(string: String, setup: Path.()->Unit = {}) = Path(ServerPath(string), this).apply(setup)

    @LightningServerDsl
    inline fun routing(setup: Path.()->Unit = {}) = Path(ServerPath.root, this).apply(setup)

    var authorizationMethod: AuthorizationMethod = { null }
    var wsAuthorizationMethod: WsAuthorizationMethod = { null }
    override var serialization: Serialization = Serialization()
    private val requirements: MutableSet<Server.ResourceRequirement<*, *>> = HashSet()
    private val settings: MutableSet<Server.Setting<*>> = HashSet()
    private val endpoints: MutableMap<HttpEndpoint, HttpHandler> = HashMap()
    var endpointException: suspend ServerRunner.(HttpRequest, Exception) -> HttpResponse = { request, exception ->
        if (exception is HttpStatusException) {
            exception.toResponse(serialization, request)
        } else {
            HttpResponse(status = HttpStatus.InternalServerError)
        }
    }
    private val websockets: MutableMap<ServerPath, WebSocketHandler> = HashMap()
    private val schedules: MutableSet<ScheduledTask> = HashSet()
    private val tasks: MutableSet<Task<*>> = HashSet()
    private val startupTasks: MutableSet<StartupTask> = HashSet()
    private val startupActions: MutableSet<ServerRunner.() -> Unit> = HashSet()

    fun <T, S> require(requirement: Server.ResourceRequirement<T, S>): Server.ResourceRequirement<T, S> {
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

    fun require(requirement: StartupTask): StartupTask {
        startupTasks.add(requirement)
        return requirement
    }

    fun require(requirement: ServerRunner.() -> Unit) {
        startupActions.add(requirement)
    }

    var HttpEndpoint.handler: HttpHandler?
        get() = endpoints[this]
        set(value) {
            if (value != null) endpoints[this] = value
        }
    var ServerPath.webSocketHandler: WebSocketHandler?
        get() = websockets[this]
        set(value) {
            if (value != null) websockets[this] = value
        }

    fun build(): Server = Server(
        name = name,
        serialization = serialization,
        authorizationMethod = authorizationMethod,
        wsAuthorizationMethod = wsAuthorizationMethod,
        resources = requirements,
        settings = settings,
        endpoints = endpoints,
        endpointException = endpointException,
        websockets = websockets,
        schedules = schedules,
        tasks = tasks,
        startupTasks = startupTasks,
        startupActions = startupActions,
    )

    init {
        require(LoggingSettings.setting)
        require(LoggingAppender)
    }
}

inline fun buildServer(name: String = "My Server", builder: ServerBuilder.()->Unit) = ServerBuilder(name).apply(builder).build()