@file:UseContextualSerialization(LocalDate::class, Instant::class, UUID::class, ServerFile::class)
package com.lightningkite.lightningserver.jsonschema

import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningdb.*
import io.ktor.server.auth.*
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import org.junit.Test
import java.io.File
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.properties.encodeToStringMap
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.typeOf
import kotlin.test.assertTrue

class SchemaTest {

    val serialization = Serialization()

    @Test
    fun quick() {
        println(serialization.json.encodeToSchema(Post.serializer()))
    }

    @Test
    fun condition() {
        prepareModels()
        println(serialization.json.encodeToSchema(Condition.serializer(Post.serializer())))
    }

    @Test
    fun params() {
        println(
            Properties.encodeToStringMap(condition<Post> { (it.author eq "Bill") and (it.title eq "Bills Greatest") }).entries.joinToString(
                "&"
            ) { it.key + "=" + URLEncoder.encode(it.value, Charsets.UTF_8) })
    }

    @Serializable
    data class Acknowledgement(
        val test: UUID? = null
    )

    @Test
    fun nullableSchema() {
        val schema = serialization.json.encodeToSchema(Acknowledgement.serializer())
        assertTrue(schema.contains("null"))
    }

}


private class TestPrincipal : Principal