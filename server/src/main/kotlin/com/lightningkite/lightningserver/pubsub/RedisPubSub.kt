package com.lightningkite.lightningserver.pubsub

import com.lightningkite.lightningserver.serialization.Serialization
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.collect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RedisPubSub(override val serialization: Serialization, val client: RedisClient): PubSubInterface {
    val observables = ConcurrentHashMap<String, Flux<String>>()
    val subscribeConnection = client.connectPubSub().reactive()
    val publishConnection = client.connectPubSub().reactive()
    private fun key(key: String) = observables.getOrPut(key) {
        val reactive = subscribeConnection
        Flux.usingWhen(
            reactive.subscribe(key).then(Mono.just(reactive)) ,
            {
                it.observeChannels()
                    .filter { it.channel == key }
            },
            { it.unsubscribe(key) }
        ).map { it.message }
            .doOnError { it.printStackTrace() }
            .share()
    }
    override fun <T> get(key: String, serializer: KSerializer<T>): PubSubChannel<T> {
        return object: PubSubChannel<T> {
            override suspend fun collect(collector: FlowCollector<T>) {
                key(key).map { serialization.json.decodeFromString(serializer, it) }.collect { collector.emit(it) }
            }

            override suspend fun emit(value: T) {
                publishConnection.publish(key, serialization.json.encodeToString(serializer, value)).awaitFirst()
            }
        }
    }

    override fun string(key: String): PubSubChannel<String> {
        return object: PubSubChannel<String> {
            override suspend fun collect(collector: FlowCollector<String>) {
                key(key).collect { collector.emit(it) }
            }

            override suspend fun emit(value: String) {
                publishConnection.publish(key, value).awaitFirst()
            }
        }
    }
}