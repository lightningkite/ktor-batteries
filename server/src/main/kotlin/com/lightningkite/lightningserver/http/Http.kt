package com.lightningkite.lightningserver.http

import com.lightningkite.lightningserver.TestServerRunner
import com.lightningkite.lightningserver.exceptions.HttpStatusException

suspend fun TestServerRunner.test(
    route: HttpEndpoint,
    parts: Map<String, String> = mapOf(),
    wildcard: String? = null,
    queryParameters: List<Pair<String, String>> = listOf(),
    headers: HttpHeaders = HttpHeaders.EMPTY,
    body: HttpContent? = null,
    domain: String = "localhost",
    protocol: String = "http",
    sourceIp: String = "0.0.0.0"
): HttpResponse {
    val req = HttpRequest(
        route = route,
        parts = parts,
        wildcard = wildcard,
        queryParameters = queryParameters,
        headers = headers,
        body = body,
        domain = domain,
        protocol = protocol,
        sourceIp = sourceIp,
    )
    return try {
        server.endpoints[route]!!.invoke(this@test, req)
    } catch(e: HttpStatusException) {
        e.toResponse(serialization, req)
    }
}