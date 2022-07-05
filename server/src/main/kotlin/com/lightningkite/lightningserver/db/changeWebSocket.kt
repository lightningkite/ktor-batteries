package com.lightningkite.lightningserver.db

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.auth.AuthInfo
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.serialization.serializerOrContextual
import com.lightningkite.lightningserver.task.Task
import com.lightningkite.lightningserver.typed.ApiWebsocket
import com.lightningkite.lightningserver.typed.typedWebsocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.*

        @LightningServerDsl
        inline fun <reified USER, reified T : HasId<ID>, reified ID : Comparable<ID>> ServerBuilder.Path.restApiWebsocket(
    noinline baseCollection: ServerRunner.() -> AbstractSignalFieldCollection<T>,
    noinline collection: suspend ServerRunner.(FieldCollection<T>, USER) -> FieldCollection<T>
): ApiWebsocket<USER, Query<T>, ListChange<T>> = restApiWebsocket(
    AuthInfo(),
    serializerOrContextual(),
    serializerOrContextual(),
    baseCollection,
    collection
)

@LightningServerDsl
fun <USER, T : HasId<ID>, ID : Comparable<ID>> ServerBuilder.Path.restApiWebsocket(
    authInfo: AuthInfo<USER>,
    serializer: KSerializer<T>,
    userSerializer: KSerializer<USER>,
    baseCollection: ServerRunner.() -> AbstractSignalFieldCollection<T>,
    collection: suspend ServerRunner.(FieldCollection<T>, USER) -> FieldCollection<T>
): ApiWebsocket<USER, Query<T>, ListChange<T>> {
    prepareModels()
    val database = builder.require(Database.default)
    val modelName = serializer.descriptor.serialName.substringBefore('<').substringAfterLast('.')
    val modelIdentifier = serializer.descriptor.serialName
    return typedWebsocket<USER, Query<T>, ListChange<T>>(
        authInfo = authInfo,
        inputType = Query.serializer(serializer),
        outputType = ListChange.serializer(serializer),
        summary = "Watch",
        description = "Gets a changing list of ${modelName}s that match the given query.",
        errorCases = listOf(),
        connect = { ws, event ->
            database().collection<__WebSocketDatabaseChangeSubscription>().insertOne(
                __WebSocketDatabaseChangeSubscription(
                    _id = event.id,
                    databaseId = modelIdentifier,
                    condition = "{\"Never\":true}",
                    user = serialization.json.encodeToString(userSerializer, event.user)
                )
            )
        },
        message = { ws, event ->
            val existing =
                database().collection<__WebSocketDatabaseChangeSubscription>().get(event.id) ?: return@typedWebsocket
            val user = serialization.json.decodeFromString(userSerializer, existing.user)
            val p = collection(baseCollection(), user)
            val q = event.content.copy(condition = p.fullCondition(event.content.condition))
            val c = serialization.json.encodeToString(Query.serializer(serializer), q)
            database().collection<__WebSocketDatabaseChangeSubscription>().updateOne(
                condition = condition { it._id eq event.id },
                modification = modification { it.condition assign c }
            )
            with(ws) {
                send(event.id, ListChange(wholeList = collection(baseCollection(), user).query(q).toList()))
            }
        },
        disconnect = { ws, event ->
            database().collection<__WebSocketDatabaseChangeSubscription>().deleteMany(condition { it._id eq event.id })
        }
    ).apply {
        val sendWsChanges = builder.require(
            Task(
                name = "$modelIdentifier.sendWsChanges",
                serializer = CollectionChanges.serializer(serializer),
                implementation = { changes: CollectionChanges<T> ->
                    coroutineScope {
                        val asyncs = ArrayList<Deferred<Unit>>()
                        database().collection<__WebSocketDatabaseChangeSubscription>()
                            .find(condition { it.databaseId eq modelIdentifier }).collect {
                                asyncs += async {
                                    val p = collection(
                                        baseCollection(),
                                        serialization.json.decodeFromString(userSerializer, it.user)
                                    )
                                    val c =
                                        serialization.json.decodeFromString(Query.serializer(serializer), it.condition)
                                    for (entry in changes.changes) {
                                        send(it._id, ListChange(
                                            old = entry.old?.takeIf { c.condition(it) }?.let { p.mask(it) },
                                            new = entry.new?.takeIf { c.condition(it) }?.let { p.mask(it) },
                                        )
                                        )
                                    }
                                }
                            }
                        asyncs.awaitAll()
                    }
                }
            )
        )
        builder.require {
            baseCollection().signals.add { changes -> sendWsChanges(changes) }
        }
    }
}

@Serializable
@DatabaseModel
@Suppress("ClassName")
data class __WebSocketDatabaseChangeSubscription(
    override val _id: String,
    @Index val databaseId: String,
    val user: String, //USER
    val condition: String //Condition<T>
) : HasId<String>