package com.lightningkite.lightningserver.typed

import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.auth.AuthInfo
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.exceptions.BadRequestException
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.serialization.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

data class ApiEndpoint<USER, INPUT : Any, OUTPUT>(
    val route: HttpEndpoint,
    override val authInfo: AuthInfo<USER>,
    val inputType: KSerializer<INPUT>,
    val outputType: KSerializer<OUTPUT>,
    override val summary: String,
    override val description: String = summary,
    val successCode: HttpStatus = HttpStatus.OK,
    val errorCases: List<ErrorCase>,
    val routeTypes: Map<String, KSerializer<*>> = mapOf(),
    val implementation: suspend ServerRunner.(user: USER, input: INPUT, pathSegments: Map<String, String>) -> OUTPUT
): Documentable, HttpHandler {
    override val path: ServerPath
        get() = route.path

    data class ErrorCase(val status: HttpStatus, val internalCode: Int, val description: String)

    override suspend fun invoke(runner: ServerRunner, it: HttpRequest): HttpResponse = with(runner) {
        val user = authInfo.checker(server.authorizationMethod(runner, it))
        @Suppress("UNCHECKED_CAST") val input: INPUT = when(route.method) {
            HttpMethod.GET, HttpMethod.HEAD -> it.queryParameters(serialization, inputType)
            else -> if(inputType == Unit.serializer()) Unit as INPUT else it.body!!.parse(runner, inputType)
        }
        val result = implementation(user, input, it.parts)
        return HttpResponse(
            body = result.toHttpContent(serialization, it.headers.accept, outputType),
            status = successCode
        )
    }
}

@kotlinx.serialization.Serializable
data class IdHolder<ID>(val id: ID)
inline fun <reified T: Comparable<T>> Serialization.parseUrlPartOrBadRequest(part: String): T = parseUrlPartOrBadRequest(part, serializerOrContextual())
fun <T: Comparable<T>> Serialization.parseUrlPartOrBadRequest(part: String, serializer: KSerializer<T>): T = try {
    properties.decodeFromStringMap(IdHolder.serializer(serializer), mapOf("id" to part)).id
} catch(e: Exception) {
    e.printStackTrace()
    throw BadRequestException("ID ${this} could not be parsed as a ${serializer.descriptor.serialName}.")
}

/**
 * Builds a typed route.
 */
@LightningServerDsl
inline fun <reified USER, reified INPUT : Any, reified OUTPUT> ServerBuilder.Endpoint.typed(
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    routeTypes: Map<String, KSerializer<*>>,
    successCode: HttpStatus = HttpStatus.OK,
    noinline implementation: suspend ServerRunner.(user: USER, input: INPUT, pathSegments: Map<String, String>) -> OUTPUT
): HttpEndpoint = typed(
    authInfo = AuthInfo(),
    inputType = serializerOrContextual(),
    outputType = serializerOrContextual(),
    summary = summary,
    description = description,
    errorCases = errorCases,
    routeTypes = routeTypes,
    successCode = successCode,
    implementation = implementation
)

/**
 * Builds a typed route.
 */
@LightningServerDsl
fun <USER, INPUT : Any, OUTPUT> ServerBuilder.Endpoint.typed(
    authInfo: AuthInfo<USER>,
    inputType: KSerializer<INPUT>,
    outputType: KSerializer<OUTPUT>,
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    routeTypes: Map<String, KSerializer<*>> = mapOf(),
    successCode: HttpStatus = HttpStatus.OK,
    implementation: suspend ServerRunner.(user: USER, input: INPUT, pathSegments: Map<String, String>) -> OUTPUT
): HttpEndpoint {
    this.handler(
        ApiEndpoint(
            route = route,
            summary = summary,
            description = description,
            successCode = successCode,
            errorCases = errorCases,
            routeTypes = routeTypes,
            inputType = inputType,
            outputType = outputType,
            authInfo = authInfo,
            implementation = implementation
        )
    )
    return route
}



/**
 * Builds a typed route.
 */
@LightningServerDsl
inline fun <reified USER, reified INPUT : Any, reified OUTPUT> ServerBuilder.Endpoint.typed(
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    noinline implementation: suspend ServerRunner.(user: USER, input: INPUT) -> OUTPUT
): HttpEndpoint = typed(
    authInfo = AuthInfo(),
    inputType = serializerOrContextual(),
    outputType = serializerOrContextual(),
    summary = summary,
    description = description,
    errorCases = errorCases,
    successCode = successCode,
    implementation = implementation
)

/**
 * Builds a typed route.
 */
@LightningServerDsl
fun <USER, INPUT : Any, OUTPUT> ServerBuilder.Endpoint.typed(
    authInfo: AuthInfo<USER>,
    inputType: KSerializer<INPUT>,
    outputType: KSerializer<OUTPUT>,
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    implementation: suspend ServerRunner.(user: USER, input: INPUT) -> OUTPUT
): HttpEndpoint {
    val segmentNames = this.route.path.segments.filterIsInstance<ServerPath.Segment.Wildcard>().map { it.name }
    return typed(
        authInfo = authInfo,
        inputType = inputType,
        outputType = outputType,
        summary = summary,
        description = description,
        errorCases = errorCases,
        routeTypes = mapOf(),
        successCode = successCode,
        implementation = { user: USER, input: INPUT, pathSegments ->
            implementation(
                user,
                input,
            )
        })
}


/**
 * Builds a typed route.
 */
@LightningServerDsl
inline fun <reified USER, reified INPUT : Any, reified OUTPUT, reified ROUTE: Comparable<ROUTE>> ServerBuilder.Endpoint.typed(
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    noinline implementation: suspend ServerRunner.(user: USER, route: ROUTE, input: INPUT) -> OUTPUT
): HttpEndpoint {
    return typed(
        authInfo = AuthInfo(),
        inputType = serializerOrContextual(),
        outputType = serializerOrContextual(),
        routeType = serializerOrContextual(),
        summary = summary,
        description = description,
        errorCases = errorCases,
        successCode = successCode,
        implementation = implementation
    )
}

/**
 * Builds a typed route.
 */
@LightningServerDsl
fun <USER, INPUT : Any, OUTPUT, ROUTE: Comparable<ROUTE>> ServerBuilder.Endpoint.typed(
    authInfo: AuthInfo<USER>,
    inputType: KSerializer<INPUT>,
    outputType: KSerializer<OUTPUT>,
    routeType: KSerializer<ROUTE>,
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    implementation: suspend ServerRunner.(user: USER, route: ROUTE, input: INPUT) -> OUTPUT
): HttpEndpoint {
    val segmentNames = this.route.path.segments.filterIsInstance<ServerPath.Segment.Wildcard>().map { it.name }
    return typed(
        authInfo = authInfo,
        inputType = inputType,
        outputType = outputType,
        summary = summary,
        description = description,
        errorCases = errorCases,
        routeTypes = mapOf(
            segmentNames[0] to routeType,
        ),
        successCode = successCode,
        implementation = { user: USER, input: INPUT, pathSegments ->
            implementation(
                user,
                serialization.parseUrlPartOrBadRequest(pathSegments[segmentNames[0]]!!, routeType),
                input,
            )
        })
}


/**
 * Builds a typed route.
 */
@LightningServerDsl
inline fun <reified USER, reified INPUT : Any, reified OUTPUT, reified ROUTE: Comparable<ROUTE>, reified ROUTE2: Comparable<ROUTE2>> ServerBuilder.Endpoint.typed(
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    noinline implementation: suspend ServerRunner.(user: USER, route: ROUTE, route2: ROUTE2, input: INPUT) -> OUTPUT
): HttpEndpoint {
    return typed(
        authInfo = AuthInfo(),
        inputType = serializerOrContextual(),
        outputType = serializerOrContextual(),
        routeType = serializerOrContextual(),
        route2Type = serializerOrContextual(),
        summary = summary,
        description = description,
        errorCases = errorCases,
        successCode = successCode,
        implementation = implementation
    )
}

/**
 * Builds a typed route.
 */
@LightningServerDsl
fun <USER, INPUT : Any, OUTPUT, ROUTE: Comparable<ROUTE>, ROUTE2: Comparable<ROUTE2>> ServerBuilder.Endpoint.typed(
    authInfo: AuthInfo<USER>,
    inputType: KSerializer<INPUT>,
    outputType: KSerializer<OUTPUT>,
    routeType: KSerializer<ROUTE>,
    route2Type: KSerializer<ROUTE2>,
    summary: String,
    description: String = summary,
    errorCases: List<ApiEndpoint.ErrorCase>,
    successCode: HttpStatus = HttpStatus.OK,
    implementation: suspend ServerRunner.(user: USER, route: ROUTE, route2: ROUTE2, input: INPUT) -> OUTPUT
): HttpEndpoint {
    val segmentNames = this.route.path.segments.filterIsInstance<ServerPath.Segment.Wildcard>().map { it.name }
    return typed(
        authInfo = authInfo,
        inputType = inputType,
        outputType = outputType,
        summary = summary,
        description = description,
        errorCases = errorCases,
        routeTypes = mapOf(
            segmentNames[0] to routeType,
            segmentNames[0] to route2Type,
        ),
        successCode = successCode,
        implementation = { user: USER, input: INPUT, pathSegments ->
            implementation(
                user,
                serialization.parseUrlPartOrBadRequest(pathSegments[segmentNames[0]]!!, routeType),
                serialization.parseUrlPartOrBadRequest(pathSegments[segmentNames[1]]!!, route2Type),
                input,
            )
        })
}


