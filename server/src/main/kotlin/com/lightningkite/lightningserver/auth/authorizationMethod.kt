package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.exceptions.UnauthorizedException
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.websocket.WebSocketConnectEvent

typealias AuthorizationMethod = suspend ServerRunner.(HttpRequest) -> Any?
typealias WsAuthorizationMethod = suspend ServerRunner.(WebSocketConnectEvent) -> Any?