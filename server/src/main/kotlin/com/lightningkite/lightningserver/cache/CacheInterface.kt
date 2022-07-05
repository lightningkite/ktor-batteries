package com.lightningkite.lightningserver.cache

import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.serverhealth.HealthCheckable
import com.lightningkite.lightningserver.serverhealth.HealthStatus
import io.lettuce.core.RedisClient
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import redis.embedded.RedisServer

interface CacheInterface: HealthCheckable {
    val serialization: Serialization
    suspend fun <T> get(key: String, serializer: KSerializer<T>): T?
    suspend fun <T> set(key: String, value: T, serializer: KSerializer<T>, timeToLiveMilliseconds: Long? = null)
    suspend fun <T> setIfNotExists(key: String, value: T, serializer: KSerializer<T>): Boolean
    suspend fun add(key: String, value: Int)
    suspend fun clear()
    suspend fun remove(key: String)
    override suspend fun healthCheck(): HealthStatus {
        return try {
            set("health-check-test-key", true)
            HealthStatus(HealthStatus.Level.OK)
        } catch(e: Exception) {
            HealthStatus(HealthStatus.Level.ERROR, additionalMessage = e.message)
        }
    }

    override val healthCheckName: String
        get() = "Cache"

    data class Requirement(override val name: String): Server.ResourceRequirement<CacheInterface, String> {
        override val type: String
            get() = "cache"
        override val serializer: KSerializer<String>
            get() = String.serializer()
        override fun default(): String = "local"
        override fun ServerRunner.fromExplicit(setting: String): CacheInterface = when {
            setting == "local" -> LocalCache(serialization)
            setting == "redis" -> {
            val redisServer = RedisServer.builder()
                .port(6379)
                .setting("bind 127.0.0.1") // good for local development on Windows to prevent security popups
                .slaveOf("localhost", 6378)
                .setting("daemonize no")
                .setting("appendonly no")
                .setting("maxmemory 128M")
                .build()
            redisServer.start()
            RedisCache(serialization, RedisClient.create("redis://127.0.0.1:6378"))
        }
            setting.startsWith("redis://") -> RedisCache(serialization, RedisClient.create(setting))
        else -> throw NotImplementedError("Cache URI $setting not recognized")
        }
    }
    companion object {
        val default = CacheInterface.Requirement("cache")
    }

}
val cache: CacheInterface.Requirement get() = CacheInterface.default

suspend inline fun <reified T: Any> CacheInterface.get(key: String): T? {
    return get(key, serialization.json.serializersModule.serializer<T>())
}
suspend inline fun <reified T: Any> CacheInterface.set(key: String, value: T, timeToLiveMilliseconds: Long? = null) {
    return set(key, value, serialization.json.serializersModule.serializer<T>(), timeToLiveMilliseconds)
}
suspend inline fun <reified T: Any> CacheInterface.setIfNotExists(key: String, value: T): Boolean {
    return setIfNotExists(key, value, serialization.json.serializersModule.serializer<T>())
}
