@file:UseContextualSerialization(UUID::class)

package com.lightningkite.lightningserver.db

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.SetOnce
import com.lightningkite.lightningserver.TestServerRunner
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.websocket.test
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.encodeToString
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class ChangeSocketTest {
    @Serializable
    @DatabaseModel
    data class TestThing(override val _id: UUID = UUID.randomUUID()) : HasId<UUID>

    val server = ServerBuilder("ChangeSocketTest").apply {
        val database = require(Database.default)
        path("test").restApiWebsocket(
            baseCollection = { database().collection<TestThing>() as AbstractSignalFieldCollection<TestThing> },
            collection = { it, user: Unit -> it }
        )
    }.build()

    @Test
    fun test() {
        with(TestServerRunner(server)) {
            runBlocking {
                test(ServerPath("test")) {
                    this.send(serialization.json.encodeToString(Query<TestThing>()))
                    assertEquals(
                        withTimeout(100L) { incoming.receive() },
                        serialization.json.encodeToString(ListChange<TestThing>(wholeList = listOf()))
                    )
                    val newThing = TestThing()
                    Database.default().collection<TestThing>().insertOne(newThing)
                    assertEquals(
                        withTimeout(100L) { incoming.receive() },
                        serialization.json.encodeToString(ListChange<TestThing>(new = newThing))
                    )
                }
            }
        }
    }
}