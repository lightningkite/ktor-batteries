package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.HtmlDefaults
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.db.DatabaseRequirement
import com.lightningkite.lightningserver.db.default
import com.lightningkite.lightningserver.email.email
import com.lightningkite.lightningserver.exceptions.NotFoundException
import com.lightningkite.lightningserver.exceptions.UnauthorizedException
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.routes.docName
import com.lightningkite.lightningserver.serialization.serializerOrContextual
import com.lightningkite.lightningserver.typed.typed
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.typeOf
import kotlin.reflect.*

inline fun <reified T> JwtSigner.jwt(request: HttpRequest): T? = jwt(request, serializerOrContextual())
fun <T> JwtSigner.jwt(request: HttpRequest, serializer: KSerializer<T>): T? =
    (request.headers[HttpHeader.Authorization]?.removePrefix("Bearer ") ?: request.headers.cookies[HttpHeader.Authorization]?.removePrefix("Bearer "))?.let {
        try {
            verify<T>(serializer, it)
        } catch(e: UnauthorizedException) {
            throw UnauthorizedException(
                body = e.body,
                headers = {
                    setCookie(HttpHeader.Authorization, "deleted", maxAge = 0)
                },
                cause = e.cause
            )
        }
    }

/**
 * A Shortcut function to define authentication on the server. This will set up magic link login, no passwords.
 * This will set up JWT authentication using quickJwt.
 * It will setup routing for: emailMagicLinkEndpoint, oauthGoogle, oauthGithub, oauthApple, refreshTokenEndpoint, and self
 * It will handle getting the user by their ID or their email.
 *
 * @param path The path you wish all endpoints to be prefixed with
 * @param onNewUser An optional lambda that returns a new user provided an email. This allows quick user creation if a login email is requested but the email has not been used before.
 * @param landing The url you wish users to be sent to in their login emails.
 * @param emailSubject The subject of the login emails that will be sent out.
 * @param template A lambda to return what the email to send will be given the email and the login link.
 */

inline fun <reified USER, reified ID : Comparable<ID>> ServerBuilder.Path.authEndpoints(
    landing: String = "/",
    database: DatabaseRequirement = Database.default,
    emailSubject: String = "Log In",
    noinline template: (suspend ServerRunner.(email: String, link: String) -> String) = HtmlDefaults.defaultLoginEmailTemplate,
    noinline onNewUser: suspend (email: String) -> USER? = { null },
): ServerPath where USER : HasEmail, USER : HasId<ID> = authEndpoints(
    authInfo = AuthInfo<USER>(),
    idType = serializerOrContextual<ID>(),
    userType = typeOf<USER>(),
    userTypeName = USER::class.simpleName!!,
    onNewUser = onNewUser,
    landing = landing,
    database = database,
    emailSubject = emailSubject,
    template = template,
)

fun <USER, ID : Comparable<ID>> ServerBuilder.Path.authEndpoints(
    idType: KSerializer<ID>,
    userType: KType,
    userTypeName: String,
    authInfo: AuthInfo<USER>,
    onNewUser: suspend (email: String) -> USER? = { null },
    landing: String = "/",
    database: DatabaseRequirement = Database.default,
    emailSubject: String = "Log In",
    template: (suspend ServerRunner.(email: String, link: String) -> String) = HtmlDefaults.defaultLoginEmailTemplate
): ServerPath where USER : HasEmail, USER : HasId<ID> {
    builder.require(database)
    @Suppress("UNCHECKED_CAST")
    return authEndpoints(
        idType = idType,
        userType = serializerOrContextual<USER>(userType),
        authInfo = authInfo,
        userId = { it._id },
        userById = {
            database().collection<USER>(userType, userTypeName).get(it)
        },
        userByEmail = {
            database().collection<USER>(userType, userTypeName)
                .find(Condition.OnField(HasEmailFields.email<USER>(), Condition.Equal(it)))
                .singleOrNull() ?: onNewUser(it)?.let { database().collection<USER>(userType, userTypeName).insertOne(it) }
            ?: throw NotFoundException()
        },
        landing = landing,
        emailSubject = emailSubject,
        template = template
    )
}


/**
 * A Shortcut function to define authentication on the server. This will set up magic link login, no passwords.
 * This will set up JWT authentication using quickJwt.
 * It will setup routing for: emailMagicLinkEndpoint, oauthGoogle, oauthGithub, oauthApple, refreshTokenEndpoint, and self
 *
 * @param path The path you wish all endpoints to be prefixed with
 * @param userById A lambda that should return the user being authenticated by their id.
 * @param userByEmail A lambda that should return the user being authenticated by their email.
 * @param landing The url you wish users to be sent to in their login emails.
 * @param emailSubject The subject of the login emails that will be sent out.
 * @param template A lambda to return what the email to send will be given the email and the login link.
 */
        inline fun <reified USER : Any, reified ID> ServerBuilder.Path.authEndpoints(
    noinline userId: ServerRunner.(user: USER) -> ID,
    noinline userById: suspend ServerRunner.(id: ID) -> USER?,
    noinline userByEmail: suspend ServerRunner.(email: String) -> USER,
    landing: String = "/",
    emailSubject: String = "Log In",
    noinline template: (suspend ServerRunner.(email: String, link: String) -> String) = HtmlDefaults.defaultLoginEmailTemplate
): ServerPath = authEndpoints(
    authInfo = AuthInfo(),
    idType = serializerOrContextual(),
    userType = serializerOrContextual(),
    userId = userId,
    userById = userById,
    userByEmail = userByEmail,
    landing = landing,
    emailSubject = emailSubject,
    template = template,
)

fun <USER : Any, ID> ServerBuilder.Path.authEndpoints(
    idType: KSerializer<ID>,
    userType: KSerializer<USER>,
    authInfo: AuthInfo<USER>,
    userId: ServerRunner.(user: USER) -> ID,
    userById: suspend ServerRunner.(id: ID) -> USER?,
    userByEmail: suspend ServerRunner.(email: String) -> USER,
    landing: String = "/",
    emailSubject: String = "Log In",
    template: (suspend ServerRunner.(email: String, link: String) -> String) = HtmlDefaults.defaultLoginEmailTemplate
): ServerPath = with(builder) {
    require(JwtSignSettings.default)
    require(email)
    authorizationMethod = {
        signer().jwt<ID>(it, idType)?.let { userById(it) }
    }
    wsAuthorizationMethod = {
        it.queryParameter("jwt")?.let { signer().verify<ID>(idType, it) }?.let { userById(it) }
    }

    path.docName = "Auth"
    val landingEndpoint: ServerBuilder.Endpoint = get("login-landing")
    landingEndpoint.handler { call ->
        val token = call.queryParameter("jwt")!!
        HttpResponse.redirectToGet(
            to = publicUrl + (call.queryParameter("destination") ?: landing),
            headers = {
                setCookie(HttpHeader.Authorization, token)
            }
        )
    }
    post("login-email").typed(
        summary = "Email Login Link",
        description = "Sends a login email to the given address",
        errorCases = listOf(),
        successCode = HttpStatus.NoContent,
        implementation = { user: Unit, address: String ->
            val jwt =
                signer().token(idType, userByEmail(address).let { userId(it) }, JwtSignSettings.default().emailExpirationMilliseconds)
            val link = "${publicUrl}${landingEndpoint.route.path}?jwt=$jwt"
            email().send(
                subject = emailSubject,
                to = listOf(address),
                message = "Log in to ${server.name} as ${address}:\n$link",
                htmlMessage = template(address, link)
            )
            Unit
        }
    )
    get("refresh-token").typed(
        inputType = Unit.serializer(),
        outputType = String.serializer(),
        authInfo = authInfo,
        summary = "Refresh token",
        description = "Retrieves a new token for the user.",
        errorCases = listOf(),
        implementation = { user: USER, input: Unit ->
            signer().token(idType, user.let { userId(it) })
        }
    )
    get("self").typed(
        inputType = Unit.serializer(),
        outputType = userType,
        authInfo = authInfo,
        summary = "Get Self",
        description = "Retrieves the user that you currently are",
        errorCases = listOf(),
        implementation = { user: USER, _: Unit -> user }
    )
    oauthGoogle(landingRoute = landingEndpoint.route) { userByEmail(it).let { userId(it) }.toString() }
    oauthGithub(landingRoute = landingEndpoint.route) { userByEmail(it).let { userId(it) }.toString() }
    oauthApple(landingRoute = landingEndpoint.route) { userByEmail(it).let { userId(it) }.toString() }
    return path
}
