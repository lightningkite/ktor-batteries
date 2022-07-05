package com.lightningkite.lightningserver.typed

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.HttpHeaders
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.routes.docName
import com.lightningkite.lightningserver.serialization.HasSerialization
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.serialization.parse
import com.lightningkite.lightningserver.serialization.serializerOrContextual
import com.lightningkite.lightningserver.websocket.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.html.INPUT
import kotlinx.html.OUTPUT
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class ApiWebsocket<USER, INPUT, OUTPUT>(
    override val path: ServerPath,
    override val authInfo: AuthInfo<USER>,
    val inputType: KSerializer<INPUT>,
    val outputType: KSerializer<OUTPUT>,
    override val summary: String,
    override val description: String = summary,
    val errorCases: List<ErrorCase>,
    val routeTypes: Map<String, KSerializer<*>> = mapOf(),
    val connect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, TypedConnectEvent<USER>) -> Unit,
    val message: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, TypedMessageEvent<INPUT>) -> Unit,
    val disconnect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, WebSocketDisconnectEvent) -> Unit,
) : Documentable, WebSocketHandler {

    data class TypedConnectEvent<USER>(val user: USER, val id: String)
    data class TypedMessageEvent<INPUT>(val id: String, val content: INPUT)

    data class ErrorCase(val closeReason: WebSocketClose, val internalCode: Int, val description: String)

    override suspend fun ServerRunner.connect(event: WebSocketConnectEvent) {
        connect.invoke(this, this@ApiWebsocket, TypedConnectEvent(authInfo.checker(server.wsAuthorizationMethod(this, event)), event.id))
    }

    override suspend fun ServerRunner.message(event: WebSocketMessageEvent) {
        val parsed = event.content.let { serialization.json.decodeFromString(inputType, it) }
        message.invoke(this, this@ApiWebsocket, TypedMessageEvent(event.id, parsed))
    }

    override suspend fun ServerRunner.disconnect(event: WebSocketDisconnectEvent) {
        disconnect.invoke(this, this@ApiWebsocket, event)
    }

    suspend fun ServerRunner.send(id: String, content: OUTPUT) = sendWebSocket(id, serialization.json.encodeToString(outputType, content))
}

@LightningServerDsl
inline fun <reified USER, reified INPUT, reified OUTPUT> ServerBuilder.Path.typedWebsocket(
    summary: String,
    description: String = summary,
    errorCases: List<ApiWebsocket.ErrorCase>,
    noinline connect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, ApiWebsocket.TypedConnectEvent<USER>) -> Unit,
    noinline message: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, ApiWebsocket.TypedMessageEvent<INPUT>) -> Unit,
    noinline disconnect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, WebSocketDisconnectEvent) -> Unit,
): ApiWebsocket<USER, INPUT, OUTPUT> = typedWebsocket(
    authInfo = AuthInfo(),
    inputType = serializerOrContextual(),
    outputType = serializerOrContextual(),
    summary = summary,
            description = description,
            errorCases = errorCases,
            connect = connect,
            message = message,
            disconnect = disconnect,
)

@LightningServerDsl
fun <USER, INPUT, OUTPUT> ServerBuilder.Path.typedWebsocket(
    authInfo: AuthInfo<USER>,
    inputType: KSerializer<INPUT>,
    outputType: KSerializer<OUTPUT>,
    summary: String,
    description: String = summary,
    errorCases: List<ApiWebsocket.ErrorCase>,
    connect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, ApiWebsocket.TypedConnectEvent<USER>) -> Unit,
    message: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, ApiWebsocket.TypedMessageEvent<INPUT>) -> Unit,
    disconnect: suspend ServerRunner.(ApiWebsocket<USER, INPUT, OUTPUT>, WebSocketDisconnectEvent) -> Unit,
): ApiWebsocket<USER, INPUT, OUTPUT> {
    val ws = ApiWebsocket(
        path = path,
        authInfo = authInfo,
        inputType = inputType,
        outputType = outputType,
        summary = summary,
        description = description,
        errorCases = errorCases,
        connect = connect,
        message = message,
        disconnect = disconnect,
    )
    with(builder) {
        path.webSocketHandler = ws
    }
    return ws
}

data class TypedVirtualSocket<INPUT, OUTPUT>(val incoming: ReceiveChannel<OUTPUT>, val send: suspend (INPUT)->Unit)

//context(ServerRunner)
//suspend fun <USER, INPUT, OUTPUT> ApiWebsocket<USER, INPUT, OUTPUT>.test(
//    parts: Map<String, String> = mapOf(),
//    wildcard: String? = null,
//    queryParameters: List<Pair<String, String>> = listOf(),
//    headers: HttpHeaders = HttpHeaders.EMPTY,
//    domain: String = publicUrl.substringAfter("://").substringBefore("/"),
//    protocol: String = publicUrl.substringBefore("://"),
//    sourceIp: String = "0.0.0.0",
//    test: suspend TypedVirtualSocket<INPUT, OUTPUT>.()->Unit
//) {
//    this.path.test(
//        parts = parts,
//        wildcard = wildcard,
//        queryParameters = queryParameters,
//        headers = headers,
//        domain = domain,
//        protocol = protocol,
//        sourceIp = sourceIp,
//        test = {
//            val channel = Channel<OUTPUT>(20)
//            coroutineScope {
//                val job = launch {
//                    for(it in incoming) {
//                        channel.send(serialization.json.decodeFromString(outputType, it))
//                    }
//                }
//                test(TypedVirtualSocket<INPUT, OUTPUT>(
//                    incoming = channel,
//                    send = { send(serialization.json.encodeToString(inputType, it)) }
//                ))
//                job.cancelAndJoin()
//            }
//        },
//    )
//}