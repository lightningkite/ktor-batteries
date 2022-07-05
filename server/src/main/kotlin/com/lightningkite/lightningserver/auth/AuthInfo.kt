package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningserver.exceptions.UnauthorizedException
import kotlin.reflect.typeOf


data class AuthInfo<USER>(
    val checker: suspend (Any?)->USER,
    val type: String? = null,
    val required: Boolean = false,
)
inline fun <reified USER> AuthInfo() = if(USER::class == Unit::class) AuthInfo<USER>(checker = { Unit as USER }, type = null, required = false)
else AuthInfo<USER>(
    checker = { raw ->
        raw?.let { it as? USER } ?: try {
            raw as USER
        } catch(e: Exception) {
            throw UnauthorizedException(
                if(raw == null) "You need to be authorized to use this." else "You need to be a ${USER::class.simpleName} to use this.",
                cause = e
            )
        }
    },
    type = typeOf<USER>().toString().substringBefore('<').substringAfterLast('.'),
    required = !typeOf<USER>().isMarkedNullable
)