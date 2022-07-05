package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.exceptions.UnauthorizedException
import com.lightningkite.lightningserver.http.HttpRequest
import com.lightningkite.lightningserver.websocket.WebSocketConnectEvent

private var authorizationMethodImpl: suspend ServerRunner.(HttpRequest) -> Any? = { null }
var ServerBuilder.authorizationMethod: suspend ServerRunner.(HttpRequest) -> Any?
    get() = authorizationMethodImpl
    set(value) {
        authorizationMethodImpl = value
    }

context(ServerBuilder, ServerRunner)
suspend fun HttpRequest.rawUser(): Any? = authorizationMethod(this@ServerRunner, this@rawUser)
context(ServerBuilder, ServerRunner)
suspend inline fun <reified USER> HttpRequest.user(): USER {
    val raw = authorizationMethod(this@ServerRunner, this@user)
    raw?.let { it as? USER }?.let { return it }
    try {
        return raw as USER
    } catch(e: Exception) {
        throw UnauthorizedException(
            if(raw == null) "You need to be authorized to use this." else "You need to be a ${USER::class.simpleName} to use this.",
            cause = e
        )
    }
}

private var wsAuthorizationMethodImpl: suspend ServerRunner.(WebSocketConnectEvent) -> Any? = { null }
var ServerBuilder.wsAuthorizationMethod: suspend ServerRunner.(WebSocketConnectEvent) -> Any?
    get() = wsAuthorizationMethodImpl
    set(value) {
        wsAuthorizationMethodImpl = value
    }

context(ServerBuilder, ServerRunner)
suspend fun WebSocketConnectEvent.rawUser(): Any? = wsAuthorizationMethod(this@ServerRunner, this@rawUser)
context(ServerBuilder, ServerRunner)
suspend inline fun <reified USER> WebSocketConnectEvent.user(): USER {
    val raw = wsAuthorizationMethod(this@ServerRunner, this@user)
    raw?.let { it as? USER }?.let { return it }
    try {
        return raw as USER
    } catch(e: Exception) {
        throw UnauthorizedException(
            if(raw == null) "You need to be authorized to use this." else "You need to be a ${USER::class.simpleName} to use this.",
            cause = e
        )
    }
}