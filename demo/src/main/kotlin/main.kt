@file:UseContextualSerialization(Instant::class, UUID::class, ServerFile::class)

package com.lightningkite.lightningserver.demo

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.auth.authEndpoints
import com.lightningkite.lightningserver.buildServer
import com.lightningkite.lightningserver.core.ContentType
import com.lightningkite.lightningserver.db.adminPages
import com.lightningkite.lightningserver.db.default
import com.lightningkite.lightningserver.db.restApi
import com.lightningkite.lightningserver.http.HttpContent
import com.lightningkite.lightningserver.http.HttpResponse
import com.lightningkite.lightningserver.http.HttpStatus
import com.lightningkite.lightningserver.ktor.KtorRunner
import com.lightningkite.lightningserver.logging.LoggingSettings
import com.lightningkite.lightningserver.pubsub.LocalPubSub
import com.lightningkite.lightningserver.serverhealth.healthCheck
import com.lightningkite.lightningserver.typed.apiHelp
import com.lightningkite.lightningserver.typed.typed
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.io.File
import java.time.Instant
import java.util.*

@Serializable
@DatabaseModel
data class TestModel(
    override val _id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val name: String = "No Name",
    val number: Int = 3123,
    @JsonSchemaFormat("jodit") val content: String = "",
    val file: ServerFile? = null
) : HasId<UUID>

@Serializable
@DatabaseModel
data class User(
    override val _id: UUID = UUID.randomUUID(),
    override val email: String
) : HasId<UUID>, HasEmail

val server = buildServer {
    val database = require(Database.default)
    routing {
        path("auth").authEndpoints { User(email = it) }
        path("test-model") {
            path("rest").restApi { user: User? -> database().collection<TestModel>() }
            path("admin").adminPages(::TestModel) { user: User? -> database().collection() }
        }
        get.handler {
             HttpResponse.text("Hello ${it.let { authorizationMethod(it) }}")
        }
        path("docs").apiHelp()
        path("health").healthCheck() { user: Unit -> true }
        path("test-primitive").get.typed(
            summary = "Get Test Primitive",
            errorCases = listOf(),
            implementation = { user: User?, input: Unit -> "42 is great" }
        )
        path("die").get.handler { throw Exception("OUCH") }
    }
}

fun main(vararg args: String) {
    KtorRunner(
        server = server,
        configFile = File("./settings.yaml")
    ).run()
}