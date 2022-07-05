package com.lightningkite.lightningserver.websocket

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath

@LightningServerDsl
fun ServerBuilder.Path.websocket(
    connect: suspend ServerRunner.(WebSocketConnectEvent) -> Unit = { },
    message: suspend ServerRunner.(WebSocketMessageEvent) -> Unit = { },
    disconnect: suspend ServerRunner.(WebSocketDisconnectEvent) -> Unit = {}
): ServerPath = with(builder) {
    path.webSocketHandler = object: WebSocketHandler {
        override suspend fun ServerRunner.connect(event: WebSocketConnectEvent) = connect(event)
        override suspend fun ServerRunner.message(event: WebSocketMessageEvent) = message(event)
        override suspend fun ServerRunner.disconnect(event: WebSocketDisconnectEvent) = disconnect(event)
    }
    return path
}
