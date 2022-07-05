package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.client
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.exceptions.BadRequestException
import com.lightningkite.lightningserver.exceptions.NotFoundException
import com.lightningkite.lightningserver.http.HttpResponse
import com.lightningkite.lightningserver.http.HttpEndpoint
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import java.util.*


@Serializable
data class OauthResponse(
    val access_token: String,
    val scope: String,
    val token_type: String = "Bearer",
    val id_token: String? = null
)

@Serializable
data class OauthProviderCredentials(
    val id: String,
    val secret: String
)


/**
 * A shortcut function for setting up an OAuth method.
 * It will set up a login and callback endpoint for the method.
 *
 * @param niceName A readable name for this method
 * @param codeName Used as the path as well as the key for the oauth config found in AuthSettings.
 * @param authUrl The url to redirect the user upon auth request. This will be the third parties auth url.
 * @param getTokenUrl The third parties url for retrieving their verification token.
 * @param scope The oath Scope.
 * @param additionalParams Any additional parameters to add to the third party url.
 * @param defaultLanding The final page to direct the user after authenticating.
 * @param secretTransform An optional lambda that allows any custom transformations on the client_secret before being used.
 * @param remoteTokenToUserId A lambda that will return the userId given the token from the third party.
 */
@LightningServerDsl
inline fun ServerBuilder.Path.oauth(
    landingRoute: HttpEndpoint,
    niceName: String,
    codeName: String,
    authUrl: String,
    getTokenUrl: String,
    scope: String,
    additionalParams: String = "",
    crossinline secretTransform: ServerRunner.(String) -> String = { it },
    crossinline remoteTokenToUserId: suspend ServerRunner.(OauthResponse)->String
) {
    val settings = builder.require(Server.Setting("oauth-${codeName}", OauthProviderCredentials.serializer().nullable) { null })
    builder.require(JwtSignSettings.default)

    val landing = landingRoute
    val callbackRoute = get("callback")
    get("login").handler { request ->
        HttpResponse.redirectToGet("""
                    $authUrl?
                    response_type=code&
                    scope=$scope&
                    redirect_uri=${publicUrl + callbackRoute.toString()}&
                    client_id=${settings()?.id ?: throw NotFoundException("OAuth for $niceName is not configured on this server.")}
                    $additionalParams
                """.trimIndent().replace("\n", ""))
    }
    callbackRoute.handler { request ->
        request.queryParameter("error")?.let {
            throw BadRequestException("Got error code '${it}' from $niceName.")
        } ?: request.queryParameter("code")?.let { code ->
            val response: OauthResponse = client.post(getTokenUrl) {
                formData {
                    parameter("code", code)
                    parameter("client_id", settings()?.id ?: throw NotFoundException("OAuth for $niceName is not configured on this server."))
                    parameter("client_secret", secretTransform(settings()?.id ?: throw NotFoundException("OAuth for $niceName is not configured on this server.")))
                    parameter("redirect_uri", publicUrl + callbackRoute.toString())
                    parameter("grant_type", "authorization_code")
                }
                accept(ContentType.Application.Json)
            }.body()

            HttpResponse.redirectToGet(publicUrl + landing.toString() + "?jwt=${signer().token(remoteTokenToUserId(response))}")
        } ?: throw IllegalStateException()
    }
}