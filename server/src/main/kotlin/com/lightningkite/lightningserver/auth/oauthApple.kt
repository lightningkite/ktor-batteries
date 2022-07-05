package com.lightningkite.lightningserver.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.exceptions.NotFoundException
import com.lightningkite.lightningserver.http.HttpRoute
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.builtins.nullable
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.util.*

/**
 * A shortcut function that sets up OAuth for Apple accounts specifically.
 *
 * @param defaultLanding The final page to send the user after authentication.
 * @param emailToId A lambda that returns the users ID given an email.
 */
context(ServerBuilder)
@LightningServerDsl
fun ServerPath.oauthApple(
    landingRoute: HttpRoute,
    emailToId: suspend ServerRunner.(String) -> String
) {
    Security.addProvider(BouncyCastleProvider())
    val settings = require(Server.Setting("oauth-apple", OauthProviderCredentials.serializer().nullable) { null })
    return oauth(
    landingRoute = landingRoute,
        niceName = "Apple",
        codeName = "apple",
        authUrl = "https://appleid.apple.com/auth/authorize",
        getTokenUrl = "https://appleid.apple.com/auth/token",
        scope = "email",
        additionalParams="&response_mode=form_post",
        secretTransform = {
            val s = settings() ?: throw NotFoundException("OAuth for Apple is not configured")
            val teamId = s.secret.substringBefore("|")
            val keyString = s.secret.substringAfter("|")
            val algorithm = run {
                val pk = JcaPEMKeyConverter().getPrivateKey(PEMParser(StringReader("""
            -----BEGIN PRIVATE KEY-----
            ${keyString.replace(" ", "")}
            -----END PRIVATE KEY-----
        """.trimIndent())).use { it.readObject() as PrivateKeyInfo })
                Algorithm.ECDSA256(null, pk as ECPrivateKey)
            }
            JWT.create()
                .withIssuer(teamId)
                .withIssuedAt(Date())
                .withExpiresAt(Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L))
                .withAudience("https://appleid.apple.com")
                .withSubject(s.id)
                .sign(algorithm)
        }
    ) {
        val id = (it.id_token ?: throw BadRequestException("No id_token found in response"))
        val decoded = JWT.decode(id)
        if(!decoded.getClaim("email_verified").asBoolean()) throw BadRequestException("Apple has not verified the email address.")
        emailToId(decoded.getClaim("email").asString())
    }
}
