package com.lightningkite.lightningserver.serialization

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.SetOnce
import com.lightningkite.lightningserver.logging.LoggingSettings
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.TestServerRunner
import com.lightningkite.lightningserver.auth.ConfigureAuthKtTest
import com.lightningkite.lightningserver.auth.signer
import com.lightningkite.lightningserver.buildServer
import com.lightningkite.lightningserver.core.ContentType
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.http.HttpHeaders
import com.lightningkite.lightningserver.typed.typed
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CorrectErrorTest {
    @Serializable
    data class TestModel(
        val number: Int = 0
    )

    val server = buildServer {
        routing {
            post("test").typed(
                summary = "Test endpoint",
                errorCases = listOf(),
                implementation = { user: Unit, input: TestModel -> input }
            )
            get("soomething").handler {
                HttpResponse()
            }
        }
    }

    @Test fun testBadRequest() {
        with(TestServerRunner(server)) {
            runBlocking {
                test(
                    route = ServerPath("test").post,
                    body = HttpContent.Text("""{"number": 2}""", ContentType.Application.Json),
                    headers = HttpHeaders(HttpHeader.Accept to ContentType.Application.Json.toString())
                ).let {
                    assertEquals(HttpStatus.OK, it.status)
                    assertEquals(TestModel(number = 2), it.body?.parse(this@with))
                }
                test(
                    route = ServerPath("test").post,
                    body = HttpContent.Text("""{"number": "asdf"}""", ContentType.Application.Json),
                    headers = HttpHeaders(HttpHeader.Accept to ContentType.Application.Json.toString())
                ).let {
                    assertEquals(HttpStatus.BadRequest, it.status)
                    println(it.body?.text())
                }
            }
        }
    }
}