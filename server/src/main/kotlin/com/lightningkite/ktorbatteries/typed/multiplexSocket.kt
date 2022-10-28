package com.lightningkite.ktorbatteries.typed

import com.lightningkite.ktorbatteries.ServerRequestCounter
import com.lightningkite.ktorbatteries.routes.fullPath
import com.lightningkite.ktorbatteries.serialization.Serialization
import com.lightningkite.ktordb.MultiplexMessage
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.safeCast


private data class OpenChannel(val channel: Channel<String>, val job: Job)

fun handleIncomingCount(header: String?, message: String) {
    ServerRequestCounter.socketChannelMessagesReceived.incrementAndGet()
    val size = message.toByteArray().size.toLong()
    when {
        header == null -> {
            ServerRequestCounter.otherDeviceSocketSent.incrementAndGet()
            ServerRequestCounter.otherSocketDataTotal.addAndGet(size)
        }

        header.contains("WEB") -> {
            ServerRequestCounter.webSocketSent.incrementAndGet()
            ServerRequestCounter.webSocketDataTotal.addAndGet(size)
        }

        header.contains("DESKTOP") -> {
            ServerRequestCounter.desktopSocketSent.incrementAndGet()
            ServerRequestCounter.destkopSocketDataTotal.addAndGet(size)
        }

        header.contains("IOS") -> {
            ServerRequestCounter.iOSSocketSent.incrementAndGet()
            ServerRequestCounter.iOSSocketDataTotal.addAndGet(size)
        }

        header.contains("ANDROID") -> {
            ServerRequestCounter.androidSocketSent.incrementAndGet()
            ServerRequestCounter.androidSocketDataTotal.addAndGet(size)
        }

        else -> {
            ServerRequestCounter.otherDeviceSocketSent.incrementAndGet()
            ServerRequestCounter.otherSocketDataTotal.addAndGet(size)
        }
    }
}

fun handleOutgoingCount(header: String?, message: String) {
    ServerRequestCounter.socketChannelMessagesSent.incrementAndGet()
    val size = message.toByteArray().size.toLong()
    when {
        header == null -> {
            ServerRequestCounter.otherDeviceSocketReceived.incrementAndGet()
            ServerRequestCounter.otherSocketDataTotal.addAndGet(size)
        }

        header.contains("WEB") -> {
            ServerRequestCounter.webSocketReceived.incrementAndGet()
            ServerRequestCounter.webSocketDataTotal.addAndGet(size)
        }

        header.contains("DESKTOP") -> {
            ServerRequestCounter.desktopSocketReceived.incrementAndGet()
            ServerRequestCounter.destkopSocketDataTotal.addAndGet(size)
        }

        header.contains("IOS") -> {
            ServerRequestCounter.iOSSocketReceived.incrementAndGet()
            ServerRequestCounter.iOSSocketDataTotal.addAndGet(size)
        }

        header.contains("ANDROID") -> {
            ServerRequestCounter.androidSocketReceived.incrementAndGet()
            ServerRequestCounter.androidSocketDataTotal.addAndGet(size)
        }

        else -> {
            ServerRequestCounter.otherDeviceSocketReceived.incrementAndGet()
            ServerRequestCounter.otherSocketDataTotal.addAndGet(size)
        }
    }
}

fun handleSocketOpened(header: String?) {
    ServerRequestCounter.socketsOpened.incrementAndGet()
    when {
        header == null -> {
            ServerRequestCounter.otherDeviceSocketsOpened.incrementAndGet()
        }

        header.contains("WEB") -> {
            ServerRequestCounter.webSocketsOpened.incrementAndGet()
        }

        header.contains("DESKTOP") -> {
            ServerRequestCounter.desktopSocketsOpened.incrementAndGet()
        }

        header.contains("IOS") -> {
            ServerRequestCounter.iOSSocketsOpened.incrementAndGet()
        }

        header.contains("ANDROID") -> {
            ServerRequestCounter.androidSocketsOpened.incrementAndGet()
        }

        else -> {
            ServerRequestCounter.otherDeviceSocketsOpened.incrementAndGet()
        }
    }
}

fun handleSocketClosed(header: String?) {
    ServerRequestCounter.socketsClosed.incrementAndGet()
    when {
        header == null -> {
            ServerRequestCounter.otherDeviceSocketsClosed.incrementAndGet()
        }

        header.contains("WEB") -> {
            ServerRequestCounter.webSocketsClosed.incrementAndGet()
        }

        header.contains("DESKTOP") -> {
            ServerRequestCounter.desktopSocketsClosed.incrementAndGet()
        }

        header.contains("IOS") -> {
            ServerRequestCounter.iOSSocketsClosed.incrementAndGet()
        }

        header.contains("ANDROID") -> {
            ServerRequestCounter.androidSocketsClosed.incrementAndGet()
        }

        else -> {
            ServerRequestCounter.otherDeviceSocketsClosed.incrementAndGet()
        }
    }
}

fun Route.multiplexWebSocket(path: String = "") {
    webSocket(path = path) {
        val header = call.parameters.get("X-Device-Info")
        handleSocketOpened(header)
        val user = call.principal<Principal>()
        val myOpenSockets = ConcurrentHashMap<String, OpenChannel>()
        try {
            incomingLoop@ for (message in incoming) {
                if (message is Frame.Close) break@incomingLoop
                if (message !is Frame.Text) continue
                val text = message.readText()

                handleIncomingCount(header, text)

                if (text == "") {
                    handleOutgoingCount(header, "")
                    send("")
                    continue
                }
                val decoded: MultiplexMessage = Serialization.json.decodeFromString(text)
                when {
                    decoded.start -> {
                        ServerRequestCounter.socketChannelsOpened.incrementAndGet()
                        @Suppress("UNCHECKED_CAST") val apiWebsocket =
                            ApiWebsocket.known.find { it.route.fullPath == decoded.path } as? ApiWebsocket<Any?, Any?, Any?>
                                ?: continue@incomingLoop

                        val erasedUserType = apiWebsocket.userType?.jvmErasure
                        val apiUser = erasedUserType?.safeCast(user)
                            ?: (user as? BoxPrincipal<*>)?.user?.let { erasedUserType?.safeCast(it) }
                        if (apiWebsocket.userType != null && apiUser == null && !apiWebsocket.userType.isMarkedNullable) continue@incomingLoop
                        val incomingChannel = Channel<String>()
                        val outSerializer = Serialization.json.serializersModule.serializer(apiWebsocket.outputType)
                        val inSerializer = Serialization.json.serializersModule.serializer(apiWebsocket.inputType)
                        myOpenSockets[decoded.channel] = OpenChannel(
                            channel = incomingChannel,
                            job = launch {
                                apiWebsocket.implementation(
                                    ApiWebsocket.Session(
                                        send = {
                                            val message = Serialization.json.encodeToString(
                                                MultiplexMessage(
                                                    channel = decoded.channel,
                                                    data = Serialization.json.encodeToString(outSerializer, it)
                                                )
                                            )
                                            handleOutgoingCount(header, message)
                                            send(message)
                                        },
                                        incoming = incomingChannel.consumeAsFlow().map {
                                            Serialization.json.decodeFromString(inSerializer, it)
                                        }
                                    ),
                                    user
                                )
                            }
                        )

                        val message = Serialization.json.encodeToString(
                            MultiplexMessage(
                                channel = decoded.channel,
                                path = decoded.path,
                                start = true
                            )
                        )
                        handleOutgoingCount(header, message)
                        send(message)
                    }

                    decoded.end -> {
                        ServerRequestCounter.socketChannelsClosed.incrementAndGet()
                        val open = myOpenSockets.remove(decoded.channel) ?: continue
                        open.job.cancel()
                    }

                    decoded.data != null -> {
                        val open = myOpenSockets[decoded.channel] ?: continue
                        val message = decoded.data!!
                        handleOutgoingCount(header, message)
                        open.channel.send(message)
                    }
                }
            }
        } finally {
            for (value in myOpenSockets.values) {
                value.job.cancel()
                value.channel.close()
            }
            this.close()
            handleSocketClosed(header)
        }
    }
}

