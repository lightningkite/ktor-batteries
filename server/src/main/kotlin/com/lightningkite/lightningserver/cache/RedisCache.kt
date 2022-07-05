package com.lightningkite.lightningserver.cache

import com.lightningkite.lightningserver.serialization.Serialization
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.litote.kmongo.coroutine.toList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

class RedisCache(override val serialization: Serialization, val client: RedisClient): CacheInterface {
    val connection = client.connect().reactive()
    override suspend fun <T> get(key: String, serializer: KSerializer<T>): T? {
        return connection.get(key).awaitFirstOrNull()?.let { serialization.json.decodeFromString(serializer, it) }
    }

    override suspend fun <T> set(key: String, value: T, serializer: KSerializer<T>, timeToLiveMilliseconds: Long?) {
        connection.set(key, serialization.json.encodeToString(serializer, value), SetArgs().let { timeToLiveMilliseconds?.let { t -> it.ex(t) } ?: it }).collect {}
    }

    override suspend fun <T> setIfNotExists(
        key: String,
        value: T,
        serializer: KSerializer<T>
    ): Boolean {
        return connection.setnx(key, serialization.json.encodeToString(serializer, value)).awaitFirst()
    }

    override suspend fun add(key: String, value: Int) {
        connection.incrby(key, value.toLong()).collect {  }
    }

    override suspend fun clear() {
        connection.flushdb().collect {  }
    }

    override suspend fun remove(key: String) {
        connection.del(key).collect {  }
    }
}