package com.lightningkite.lightningserver.websocket

import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.TestServerRunner
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.exceptions.HttpStatusException
import com.lightningkite.lightningserver.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.*

data class WebSocketConnectEvent(
    val path: ServerPath,
    val parts: Map<String, String>,
    val wildcard: String? = null,
    val queryParameters: List<Pair<String, String>>,
    val id: String,
    val headers: HttpHeaders,
    val domain: String,
    val protocol: String,
    val sourceIp: String
) {
    fun queryParameter(key: String): String? = queryParameters.find { it.first == key }?.second
}
data class WebSocketMessageEvent(val id: String, val content: String)
data class WebSocketDisconnectEvent(val id: String)

interface WebSocketHandler {
    suspend fun ServerRunner.connect(event: WebSocketConnectEvent)
    suspend fun ServerRunner.message(event: WebSocketMessageEvent)
    suspend fun ServerRunner.disconnect(event: WebSocketDisconnectEvent)
}

data class VirtualSocket(val incoming: ReceiveChannel<String>, val send: suspend (String)->Unit)
suspend fun TestServerRunner.test(
    path: ServerPath,
    parts: Map<String, String> = mapOf(),
    wildcard: String? = null,
    queryParameters: List<Pair<String, String>> = listOf(),
    headers: HttpHeaders = HttpHeaders.EMPTY,
    domain: String = publicUrl.substringAfter("://").substringBefore("/"),
    protocol: String = publicUrl.substringBefore("://"),
    sourceIp: String = "0.0.0.0",
    test: suspend VirtualSocket.()->Unit
) {
    val id = "TEST-${UUID.randomUUID()}"
    val req = WebSocketConnectEvent(
        path = path,
        parts = parts,
        wildcard = wildcard,
        queryParameters = queryParameters,
        headers = headers,
        domain = domain,
        protocol = protocol,
        sourceIp = sourceIp,
        id = id
    )
    val h = server.websockets[path]!!

    val channel = Channel<String>(20)
    coroutineScope {
        val connectHandle = async {
            println("Connecting $id...")
            with(h) { connect(req) }
            println("Connected $id.")
        }
        val receiveHandler = async {
            pubSub.string("ws-$id").collect {
                println("Got $it")
                channel.send(it)
            }
        }
        val testHandle = async {
            test(
                VirtualSocket(
                    incoming = channel,
                    send = {
                        println("$id --> $it")
                        with(h) { message(WebSocketMessageEvent(id, it)) }
                    }
                )
            )
            println("Disconnecting $id...")
            with(h) { disconnect(WebSocketDisconnectEvent(id)) }
            println("Disconnected $id.")
        }
        listOf(connectHandle, testHandle).awaitAll()
        receiveHandler.cancelAndJoin()
    }
}