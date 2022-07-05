package com.lightningkite.lightningserver.http

data class HttpRequest(
    val route: HttpEndpoint,
    val parts: Map<String, String> = mapOf(),
    val wildcard: String? = null,
    val queryParameters: List<Pair<String, String>> = listOf(),
    val headers: HttpHeaders = HttpHeaders.EMPTY,
    val body: HttpContent? = null,
    val domain: String = "localhost",
    val protocol: String = "http",
    val sourceIp: String = "0.0.0.0"
) {
    fun queryParameter(key: String): String? = queryParameters.find { it.first == key }?.second
}
