package com.lightningkite.lightningserver.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.impl.PublicClaims
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.exceptions.UnauthorizedException
import com.lightningkite.lightningserver.serialization.serializerOrContextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.security.SecureRandom
import java.time.Duration
import java.util.*

@Serializable
data class JwtSignSettings(
    val expirationMilliseconds: Long? = Duration.ofDays(365).toMillis(),
    val emailExpirationMilliseconds: Long = Duration.ofHours(1).toMillis(),
    val secret: String = buildString {
        val rand = SecureRandom.getInstanceStrong()
        repeat(64) {
            append(
                availableCharacters[rand.nextInt(availableCharacters.length)]
            )
        }
    }
) {
    companion object {
        val default: Server.Setting<JwtSignSettings> = Server.Setting("jwtSigner", serializerOrContextual()) { JwtSignSettings() }
    }
}

fun ServerRunner.signer(settings: Server.Setting<JwtSignSettings> = JwtSignSettings.default) = JwtSigner(settings(), this)
data class JwtSigner(val settings: JwtSignSettings, val runner: ServerRunner) {
    inline fun <reified T> token(subject: T, expireDuration: Long? = settings.expirationMilliseconds): String = token(serializerOrContextual(), subject, expireDuration)
    fun <T> token(serializer: KSerializer<T>, subject: T, expireDuration: Long? = settings.expirationMilliseconds): String = JWT.create()
        .withAudience(runner.publicUrl)
        .withIssuer(runner.publicUrl)
        .withIssuedAt(Date())
        .also {
            if (expireDuration != null)
                it.withExpiresAt(Date(System.currentTimeMillis() + expireDuration))
        }
        .withClaim(PublicClaims.SUBJECT, (runner.serialization.json.encodeToJsonElement(serializer, subject) as JsonPrimitive).content)
        .sign(Algorithm.HMAC256(settings.secret))

    inline fun <reified T> verify(token: String): T = verify(serializerOrContextual(), token)
    fun <T> verify(serializer: KSerializer<T>, token: String): T {
        return try {
            val v = JWT
                .require(Algorithm.HMAC256(settings.secret))
                .withIssuer(runner.publicUrl)
                .build()
                .verify(token)
            runner.serialization.json.decodeFromJsonElement(serializer, JsonPrimitive(v.subject ?: v.getClaim("userId").asString()))
        } catch (e: JWTVerificationException) {
            throw UnauthorizedException(
                body = "Invalid token $token: ${e.message}",
                cause = e
            )
        } catch (e: JWTDecodeException) {
            throw UnauthorizedException(
                body = "Invalid token $token: ${e.message}",
                cause = e
            )
        }
    }
}


private val availableCharacters =
    "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM~!@#%^&*()_+`-=[]{};':,./<>?"