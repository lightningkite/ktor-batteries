@file:UseContextualSerialization(Instant::class)
package com.lightningkite.lightningdb

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.time.Instant


interface HealthCheckable {
    val healthCheckName: String get() = this::class.simpleName ?: "???"
    suspend fun healthCheck(): HealthStatus
}

@Serializable
data class HealthStatus(val level: Level, val checkedAt: Instant = Instant.now(), val additionalMessage: String? = null) {
    @Serializable
    enum class Level(val color: String) {
        OK("green"),
        WARNING("yellow"),
        URGENT("orange"),
        ERROR("red")
    }
}