package com.lightningkite.lightningserver

import com.lightningkite.lightningdb.ClientModule
import com.lightningkite.lightningserver.serialization.Serialization
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { this.serializersModule = ClientModule })
    }
}