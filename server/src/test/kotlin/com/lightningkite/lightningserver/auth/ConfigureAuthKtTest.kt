@file:UseContextualSerialization(UUID::class)

package com.lightningkite.lightningserver.auth

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.TestServerRunner
import com.lightningkite.lightningserver.buildServer
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.db.default
import com.lightningkite.lightningserver.http.HttpHeader
import com.lightningkite.lightningserver.http.HttpHeaders
import com.lightningkite.lightningserver.http.get
import com.lightningkite.lightningserver.http.test
import com.lightningkite.lightningserver.ktor.KtorRunner
import com.lightningkite.lightningserver.pubsub.LocalPubSub
import com.lightningkite.lightningserver.serialization.parse
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Test
import java.util.*
import kotlin.reflect.typeOf
import kotlin.test.assertEquals


class ConfigureAuthKtTest {
    @Serializable
    data class TestUser(
        override val _id: UUID = UUID.randomUUID(),
        override val email: String = ""
    ) : HasId<UUID>, HasEmail

    @Test
    fun signTest() {
        with(TestServerRunner(ServerBuilder("x").build())) {
            signer().verify<String>(signer().token("test"))
        }
    }

    val server = buildServer("ConfigureAuthKtTest.testSelf") {
        val database = require(Database.default)
        path("auth").authEndpoints { email -> TestUser(email = email) }
    }

    @Test
    fun testSelfKtor() {
        val ktorRunner = KtorRunner(server)
        val user = TestUser(email = "test@test.com")
        testApplication {
            application {
                ktorRunner.setup(this)
            }
            val client = createClient {
                install(ContentNegotiation) {
                    json(Json {
                        serializersModule = ClientModule
                    })
                }
            }
            val token = with(ktorRunner) {
                Database.default().collection<TestUser>().insertOne(user)
                signer().token(user._id)
            }
            perfTest {
                val self = client.get("/auth/self") {
                    header("Authorization", "Bearer $token")
                    accept(ContentType.Application.Json)
                }.body<TestUser>()
                assertEquals(user, self)
            }.also { println("Time: $it ms for testSelfKtor") }
        }
    }

    @Test
    fun testSelf() {
        with(TestServerRunner(server)) {
            val user = TestUser(email = "test@test.com")
            runBlocking {
                Database.default().collection<TestUser>().insertOne(user)
                val token = signer().token(user._id)
                perfTest {
                    val self = test(
                        ServerPath("auth/self").get,
                        headers = HttpHeaders(
                            mapOf(
                                HttpHeader.Authorization to "Bearer $token",
                                HttpHeader.Accept to ContentType.Application.Json.toString()
                            )
                        )
                    ).body!!.parse<TestUser>(this@with)
                    assertEquals(user, self)
                }.also { println("Time: $it ms for testSelf") }
            }
        }
    }

    private inline fun perfTest(action: () -> Unit): Long {
        val s = System.currentTimeMillis()
        repeat(10000) { action() }
        return System.currentTimeMillis() - s
    }
}
