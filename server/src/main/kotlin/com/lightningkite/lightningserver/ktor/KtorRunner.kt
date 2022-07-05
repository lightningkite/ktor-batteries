package com.lightningkite.lightningserver.ktor

import com.charleskorn.kaml.Yaml
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.cache.*
import com.lightningkite.lightningserver.files.FileSystem
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.http.HttpHeaders
import com.lightningkite.lightningserver.pubsub.LocalPubSub
import com.lightningkite.lightningserver.pubsub.RedisPubSub
import com.lightningkite.lightningserver.pubsub.get
import com.lightningkite.lightningserver.schedule.Schedule
import com.lightningkite.lightningserver.task.Task
import com.lightningkite.lightningserver.websocket.WebSocketConnectEvent
import com.lightningkite.lightningserver.websocket.WebSocketDisconnectEvent
import com.lightningkite.lightningserver.websocket.WebSocketMessageEvent
import io.ktor.http.*
import io.ktor.http.HttpMethod
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cors.CORSConfig.Companion.CorsSimpleResponseHeaders
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import io.lettuce.core.RedisClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.Encoder
import org.apache.commons.vfs2.provider.local.LocalFile
import redis.embedded.RedisServer
import java.io.File
import java.time.*
import java.util.*
import kotlin.collections.HashMap
import com.lightningkite.lightningserver.core.ContentType as HttpContentType

class KtorRunner(
    server: Server,
    val config: Config
) : AbstractServerRunner(
    server = server.copy(resources = server.resources + CacheInterface.default),
    resourceSettings = config.resources.let {
        if (it.containsKey(CacheInterface.default)) it
        else it + (CacheInterface.default to "local")
    },
    settings = config.settings,
) {
    constructor(server: Server):this(
        server = server,
        config = Config(
            ktor = GeneralSettings("0.0.0.0", 8080),
            resources = server.defaultResourceSettings(),
            settings = server.defaultSettings()
        )
    )
    constructor(server: Server, configFile: File) : this(
        server = server,
        config = Unit.run {
            if(configFile.exists()) {
                val resulting = configFile.readText().let { Yaml.default.decodeFromString(KtorSettingsSerializer(server), it) }
                val correctedResources = server.validateResourceSettings(resulting.resources)
                val correctedSettings = server.validateSettings(resulting.settings)
                if(correctedResources != null || correctedSettings != null) {
                    val fix = configFile.parentFile.resolve(configFile.nameWithoutExtension + ".suggested.yml")
                    println("Config file $configFile is incomplete - wrote a suggested fix to ${fix}.")
                    fix.writeText(Yaml.default.encodeToString(KtorSettingsSerializer(server), Config(
                        ktor = GeneralSettings(
                            host = "0.0.0.0",
                            port = 8080
                        ),
                        resources = correctedResources ?: resulting.resources,
                        settings = correctedSettings ?: resulting.settings
                    )))
                    throw IllegalStateException("Config file $configFile is incomplete - wrote a suggested fix to ${fix}.")
                }
                resulting
            } else {
                println("Config file $configFile does not exist - creating an example settings file at that location.")
                configFile.writeText(Yaml.default.encodeToString(KtorSettingsSerializer(server), Config(
                    ktor = GeneralSettings(
                        host = "0.0.0.0",
                        port = 8080
                    ),
                    resources = server.defaultResourceSettings(),
                    settings = server.defaultSettings()
                )))
                throw IllegalStateException("Config file $configFile does not exist - creating an example settings file at that location.")
            }
        }
    )

    val pubSub = when {
        config.ktor.pubSub == "local" -> LocalPubSub(serialization)
        config.ktor.pubSub == "redis" -> {
            val redisServer = RedisServer.builder()
                .port(6379)
                .setting("bind 127.0.0.1") // good for local development on Windows to prevent security popups
                .slaveOf("localhost", 6378)
                .setting("daemonize no")
                .setting("appendonly no")
                .setting("maxmemory 128M")
                .build()
            redisServer.start()
            RedisPubSub(serialization, RedisClient.create("redis://127.0.0.1:6378"))
        }
        config.ktor.pubSub.startsWith("redis://") -> RedisPubSub(serialization, RedisClient.create(config.ktor.pubSub))
        else -> throw NotImplementedError("PubSub URI ${config.ktor.pubSub} not recognized")
    }
    override val publicUrl: String
        get() = config.ktor.publicUrl
    override val debug: Boolean
        get() = config.ktor.debug

    @kotlinx.serialization.Serializable
    data class GeneralSettings(
        val host: String,
        val port: Int,
        val publicUrl: String = "http://$host:$port",
        val debug: Boolean = false,
        val cors: List<String>? = null,
        val pubSub: String = "local"
    )

    data class Config(
        val ktor: GeneralSettings,
        val resources: Map<Server.ResourceRequirement<*, *>, Any?>,
        val settings: Map<Server.Setting<*>, Any?>,
    )

    class KtorSettingsSerializer(val server: Server) : KSerializer<Config> {
        val resourcesSer = ResourceSettingSerializer(server)
        val settingsSer = SettingsSerializer(server)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("${server.name}Settings") {
            element("ktor", GeneralSettings.serializer().descriptor)
            element("resources", resourcesSer.descriptor)
            element("settings", settingsSer.descriptor)
        }

        override fun deserialize(decoder: Decoder): Config {
            lateinit var ktor: GeneralSettings
            lateinit var resources: Map<Server.ResourceRequirement<*, *>, Any?>
            lateinit var settings: Map<Server.Setting<*>, Any?>
            decoder.decodeStructure(descriptor) {
                while (true) {
                    val index = decodeElementIndex(descriptor)
                    when (index) {
                        CompositeDecoder.DECODE_DONE -> break
                        CompositeDecoder.UNKNOWN_NAME -> continue
                        0 -> ktor = decodeSerializableElement(
                            descriptor,
                            0,
                            GeneralSettings.serializer()
                        )
                        1 -> resources = decodeSerializableElement(descriptor, 0, resourcesSer)
                        2 -> settings = decodeSerializableElement(descriptor, 0, settingsSer)
                    }
                }
            }
            return Config(ktor, resources, settings)
        }

        override fun serialize(encoder: Encoder, value: Config) {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(
                    descriptor,
                    0,
                    GeneralSettings.serializer(),
                    value.ktor
                )
                encodeSerializableElement(descriptor, 1, resourcesSer, value.resources)
                encodeSerializableElement(descriptor, 2, settingsSer, value.settings)
            }
        }
    }

    override fun <T, S> interceptResource(resource: Server.ResourceRequirement<T, S>): T? = null
    override fun <T> interceptSetting(resource: Server.Setting<T>): T? = null
    override suspend fun sendWebSocket(id: String, message: String) {
        pubSub.get("ws-$id", String.serializer()).emit(message)
    }

    override fun <INPUT> Task<INPUT>.invoke(input: INPUT) {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
            this@invoke.implementation(this@KtorRunner, input)
        }
    }

    fun setup(application: Application) = with(application) {
        try {
            install(WebSockets)
            install(CORS) {
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Patch)
                allowMethod(HttpMethod.Delete)

                allowHeaders { true }
                exposedHeaders.addAll(CorsSimpleResponseHeaders)

                config.ktor.cors?.forEach {
                    allowHost(it, listOf("http", "https", "ws", "wss"))
                } ?: if (debug) anyHost()
            }
            install(StatusPages) {
                exception<Exception> { call, it ->
                    call.respondText(
                        status = HttpStatusCode.InternalServerError,
                        contentType = ContentType.Text.Html,
                        text = HtmlDefaults.basePage(
                            this@KtorRunner, """
            <h1>Oh no!</h1>
            <p>Something went wrong.  We're terribly sorry.  If this continues, see if you can contact the developer.</p>
        """.trimIndent()
                        )
                    )
//                    call.adapt(HttpRoute(call.request.path(), MyHttpMethod(call.request.httpMethod.value))).reportException(it)
                }
            }
            server.endpoints.forEach { entry ->
                routing {
                    route(entry.key.path.toString(), HttpMethod.parse(entry.key.method.toString())) {
                        handle {
                            val request = call.adapt(entry.key)
                            val result = try {
                                entry.value(this@KtorRunner, request)
                            } catch (e: Exception) {
                                if (debug) e.printStackTrace()
                                server.endpointException(this@KtorRunner, request, e)
                            }
                            for (header in result.headers.entries) {
                                call.response.header(header.first, header.second)
                            }
                            call.response.status(HttpStatusCode.fromValue(result.status.code))
                            when (val b = result.body) {
                                null -> call.respondText("")
                                is HttpContent.Binary -> call.respondBytes(
                                    b.bytes,
                                    ContentType.parse(b.type.toString())
                                )
                                is HttpContent.Text -> call.respondText(b.string, ContentType.parse(b.type.toString()))
                                is HttpContent.OutStream -> call.respondOutputStream(ContentType.parse(b.type.toString())) {
                                    b.write(
                                        this
                                    )
                                }
                                is HttpContent.Stream -> call.respondBytesWriter(ContentType.parse(b.type.toString())) {
                                    b.getStream().copyTo(this)
                                }
                                is HttpContent.Multipart -> TODO()
                            }
                        }
                    }
                }
            }
            server.resources
                .map { it() }
                .filterIsInstance<FileSystem>()
                .filter { it.root is LocalFile }
                .forEach { fs ->
                    routing {
                        static(fs.publicUrl.substringAfter(this@KtorRunner.publicUrl)) {
                            files(fs.root.path.toString())
                        }
                        post(fs.publicUrl.substringAfter(this@KtorRunner.publicUrl) + "/{fileName}") {
                            val file = fs.root.resolveFile(call.parameters["fileName"]!!)
                            call.receiveStream().copyTo(file.content.outputStream)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            server.websockets.forEach { entry ->
                routing {
                    route(entry.key.toString()) {
                        webSocket {
                            val parts = HashMap<String, String>()
                            var wildcard: String? = null
                            call.parameters.forEach { s, strings ->
                                if (strings.size > 1) wildcard = strings.joinToString("/")
                                parts[s] = strings.single()
                            }
                            val id = UUID.randomUUID().toString()
                            try {
                                with(entry.value) {
                                    connect(
                                        WebSocketConnectEvent(
                                            path = entry.key,
                                            parts = parts,
                                            wildcard = wildcard,
                                            queryParameters = call.request.queryParameters.flattenEntries(),
                                            id = id,
                                            headers = call.request.headers.adapt(),
                                            domain = call.request.origin.host,
                                            protocol = call.request.origin.scheme,
                                            sourceIp = call.request.origin.remoteHost
                                        )
                                    )
                                }
                                launch {
                                    pubSub.get<String>("ws-$id").collect {
                                        send(it)
                                    }
                                }
                                for (incoming in this.incoming) {
                                    with(entry.value) {
                                        message(
                                            WebSocketMessageEvent(
                                                id = id,
                                                content = (incoming as? Frame.Text)?.readText() ?: ""
                                            )
                                        )
                                    }
                                }
                            } finally {
                                with(entry.value) {
                                    disconnect(WebSocketDisconnectEvent(id))
                                }
                            }
                        }
                    }
                }
            }
            server.schedules.forEach {
                val cache = CacheInterface.default()
                @Suppress("OPT_IN_USAGE")
                GlobalScope.launch {
                    while (true) {
                        val upcomingRun = cache.get<Long>(it.name + "-nextRun") ?: run {
                            val now = System.currentTimeMillis()
                            cache.set<Long>(it.name + "-nextRun", now)
                            now
                        }
                        delay((upcomingRun - System.currentTimeMillis()).coerceAtLeast(1L))
                        if (cache.setIfNotExists(it.name + "-lock", true)) {
                            cache.set(it.name + "-lock", true, Duration.ofHours(1).toMillis())
                            try {
                                it.handler(this@KtorRunner)
                            } catch (t: Throwable) {
                                t.printStackTrace()
//                                ExceptionSettings.instance.report(t)
                            }
                            val nextRun = when (val s = it.schedule) {
                                is Schedule.Daily -> ZonedDateTime.of(LocalDate.now().plusDays(1), s.time, s.zone)
                                    .toInstant().toEpochMilli()
                                is Schedule.Frequency -> upcomingRun + s.gap.toMillis()
                            }
                            cache.set<Long>(it.name + "-nextRun", nextRun)
                            cache.remove(it.name + "-lock")
                        } else {
                            delay(1000L)
                        }
                    }
                }
            }
            // Tasks - No registration necessary
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun run() = embeddedServer(
        factory = CIO,
        port = config.ktor.port,
        host = config.ktor.host,
        module = { setup(this) },
        watchPaths = listOf("classes")
    ).start(wait = true)
}

private fun ContentType.adapt(): HttpContentType =
    HttpContentType(type = contentType, subtype = contentSubtype)

private fun HttpContentType.adapt(): ContentType =
    ContentType(contentType = type, contentSubtype = subtype)

private fun Headers.adapt(): HttpHeaders = HttpHeaders(flattenEntries())

private suspend fun ApplicationCall.adapt(route: HttpEndpoint): HttpRequest {
    val parts = HashMap<String, String>()
    var wildcard: String? = null
    parameters.forEach { s, strings ->
        if (strings.size > 1) wildcard = strings.joinToString("/")
        parts[s] = strings.single()
    }
    return HttpRequest(
        route = route,
        parts = parts,
        wildcard = wildcard,
        queryParameters = request.queryParameters.flattenEntries(),
        headers = request.headers.adapt(),
        body = run {
            val ktorType = request.contentType()
            val myType = ktorType.adapt()
            if (ktorType.contentType == "multipart")
                HttpContent.Multipart(object : Flow<HttpContent.Multipart.Part> {
                    override suspend fun collect(collector: FlowCollector<HttpContent.Multipart.Part>) {
                        receiveMultipart().forEachPart {
                            collector.emit(
                                when (it) {
                                    is PartData.FormItem -> HttpContent.Multipart.Part.FormItem(
                                        it.name ?: "",
                                        it.value
                                    )
                                    is PartData.FileItem -> {
                                        val h = it.headers.adapt()
                                        HttpContent.Multipart.Part.DataItem(
                                            key = it.name ?: "",
                                            filename = it.originalFileName ?: "",
                                            headers = h,
                                            content = HttpContent.Stream(
                                                it.streamProvider,
                                                h.contentLength,
                                                it.contentType?.adapt() ?: HttpContentType.Application.OctetStream
                                            )
                                        )
                                    }
                                    is PartData.BinaryItem -> {
                                        val h = it.headers.adapt()
                                        HttpContent.Multipart.Part.DataItem(
                                            key = it.name ?: "",
                                            filename = "",
                                            headers = h,
                                            content = HttpContent.Stream(
                                                { it.provider().asStream() },
                                                h.contentLength,
                                                it.contentType?.adapt() ?: HttpContentType.Application.OctetStream
                                            )
                                        )
                                    }
                                    is PartData.BinaryChannelItem -> TODO()
                                }
                            )
                        }
                    }
                }, myType)
            else {
                HttpContent.Stream(
                    { receiveStream() },
                    request.contentLength(),
                    request.contentType().adapt()
                )
            }

        },
        domain = request.origin.host,
        protocol = request.origin.scheme,
        sourceIp = request.origin.remoteHost
    )
}