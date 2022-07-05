@file:UseContextualSerialization(Instant::class)

package com.lightningkite.lightningserver.serverhealth

import com.lightningkite.lightningdb.HealthCheckable
import com.lightningkite.lightningdb.HealthStatus
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.exceptions.ForbiddenException
import com.lightningkite.lightningserver.typed.typed
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.lang.management.ManagementFactory
import java.net.NetworkInterface
import java.time.Instant

@Serializable
data class ServerHealth(
    val serverId: String = NetworkInterface.getNetworkInterfaces().toList().sortedBy { it.name }
        .firstOrNull()?.hardwareAddress?.sumOf { it.hashCode() }?.toString(16) ?: "?",
    val memory: Memory = Memory(),
    val features: Map<String, HealthStatus> = mapOf(),
    val loadAverageCpu: Double = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
) {
    companion object {
        val healthCache = HashMap<HealthCheckable, HealthStatus>()
    }

    @Serializable
    data class Memory(
        val maxMem: Long,
        val totalMemory: Long,
        val freeMemory: Long,
        val systemAllocated: Long,
        val memUsagePercent: Float,
    ) {
        constructor() : this(
            maxMem = Runtime.getRuntime().maxMemory(),
            totalMemory = Runtime.getRuntime().totalMemory(),
            freeMemory = Runtime.getRuntime().freeMemory(),
            systemAllocated = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
            memUsagePercent = ((((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toFloat() / Runtime.getRuntime().maxMemory().toFloat() * 100f) * 100).toInt()) / 100f,
        )
    }
}

/**
 * A route for accessing status of features, external service connections, and general server information.
 * Examples of features that can be checked on are Email, Database, and Exception Reporting.
 *
 * @param path The path you wish the endpoint to be at.
 * @param features A list of `HealthCheckable` features that you want reports on.
 */
inline fun <reified USER> ServerBuilder.Path.healthCheck(crossinline allowed: suspend (USER) -> Boolean = { true }) {
    get.typed(
        summary = "Get Server Health",
        description = "Gets the current status of the server",
        errorCases = listOf(),
        implementation = { user: USER, _: Unit ->
            if (!allowed(user)) throw ForbiddenException()
            val now = Instant.now()
            ServerHealth(
                features = server.resources
                    .asSequence()
                    .map { it() }
                    .filterIsInstance<HealthCheckable>()
                    .associate {
                        ServerHealth.healthCache[it]?.takeIf {
                            now.toEpochMilli() - it.checkedAt.toEpochMilli() < 60_000 && it.level <= HealthStatus.Level.WARNING
                        }?.let { s -> return@associate it.healthCheckName to s }
                        val result = it.healthCheck()
                        ServerHealth.healthCache[it] = result
                        it.healthCheckName to result
                    }
            )
        }
    )
}
