package com.lightningkite.lightningserver.websocket

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath

context(ServerBuilder)
@LightningServerDsl
fun ServerPath.websocket(
    connect: suspend (WebSocketConnectEvent) -> Unit = { },
    message: suspend (WebSocketMessageEvent) -> Unit = { },
    disconnect: suspend (WebSocketDisconnectEvent) -> Unit = {}
): ServerPath {
    webSocketHandler = object: WebSocketHandler {
        override suspend fun ServerRunner.connect(event: WebSocketConnectEvent) = connect(event)
        override suspend fun ServerRunner.message(event: WebSocketMessageEvent) = message(event)
        override suspend fun ServerRunner.disconnect(event: WebSocketDisconnectEvent) = disconnect(event)
    }
    return this
}
