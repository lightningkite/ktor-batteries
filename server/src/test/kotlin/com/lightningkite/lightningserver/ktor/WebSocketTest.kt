package com.lightningkite.lightningserver.ktor

import com.lightningkite.lightningdb.ClientModule
import com.lightningkite.lightningdb.collection
import com.lightningkite.lightningdb.insertOne
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.SetOnce
import com.lightningkite.lightningserver.auth.ConfigureAuthKtTest
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.pubsub.LocalPubSub
import com.lightningkite.lightningserver.websocket.websocket
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("OPT_IN_USAGE")
class WebSocketTest {

    var socketId: String? = null
    val server = ServerBuilder("WebSocketTest").apply {
        path("socket-test").websocket(
            connect = {
                println("connect $it")
                socketId = it.id
                GlobalScope.launch {
                    delay(200L)
                    sendWebSocket(socketId!!, "Test")
                }
            },
            message = { println("message $it") },
            disconnect = { println("disconnect $it") },
        )
    }.build()

    @Test
    fun socketTest() {
        val ktorRunner = KtorRunner(server = server)
        testApplication {
            application {
                ktorRunner.setup(this)
            }
            val client = createClient {
                install(WebSockets)
                install(ContentNegotiation) {
                    json(Json {
                        serializersModule = ClientModule
                    })
                }
            }
            client.webSocket("socket-test") {
                delay(100L)
                send("Hello world!")
                delay(150L)
                incoming.tryReceive().getOrNull()?.let { it as? Frame.Text }?.let { println(it.readText()) }
                delay(100L)
            }
        }
    }
}