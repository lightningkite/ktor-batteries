package com.lightningkite.lightningserver.db

import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.serialization.Serialization
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.bson.UuidRepresentation
import org.litote.kmongo.reactivestreams.KMongo
import java.io.File
import kotlin.reflect.*

data class DatabaseRequirement(override val name: String): Server.ResourceRequirement<Database, DatabaseRequirement.Setting> {
    override val type: String
        get() = "database"
    @Serializable data class Setting(val url: String, val databaseName: String = "default")
    override val serializer: KSerializer<Setting> get() = Setting.serializer()
    override fun default(): Setting = Setting("ram")
    override fun ServerRunner.fromExplicit(setting: Setting): Database {
        val url = setting.url
        return when {
            url == "ram" -> InMemoryDatabase(serialization)
            url == "ram-preload" -> InMemoryDatabase(serialization, serialization.json.parseToJsonElement(File(url.substringAfter("://")).readText()) as? JsonObject)
            url == "ram-unsafe-persist" -> InMemoryUnsafePersistenceDatabase(serialization, File(url.substringAfter("://")))
            url == "mongodb-test" -> testMongo().database(setting.databaseName)
            url.startsWith("mongodb-file:") -> embeddedMongo(File(url.removePrefix("mongodb-file://"))).database(setting.databaseName)
            url.startsWith("mongodb:") -> KMongo.createClient(
                MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(url))
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build()
            ).database(setting.databaseName)
            else -> throw IllegalArgumentException("MongoDB connection style not recognized: got $url but only understand: " +
                    "ram\n" +
                    "ram-preload\n" +
                    "ram-unsafe-persist\n" +
                    "mongodb-test\n" +
                    "mongodb-file:\n" +
                    "mongodb:"
            )
        }
    }
}
val Database.Companion.default: DatabaseRequirement get() = database
val database = DatabaseRequirement("database")

class InMemoryDatabase(val serialization: Serialization, val premadeData: JsonObject? = null): Database {
    val collections = HashMap<String, FieldCollection<*>>()
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> collection(type: KType, name: String): FieldCollection<T>
            = collections.getOrPut(name) {
        val made = InMemoryFieldCollection<T>()
        premadeData?.get(name)?.let {
            val data = serialization.json.decodeFromJsonElement(ListSerializer(serialization.json.serializersModule.serializer(type) as KSerializer<T>), it)
            made.data.addAll(data)
        }
        made
    } as FieldCollection<T>
}

class InMemoryUnsafePersistenceDatabase(val serialization: Serialization, val folder: File): Database {
    init {
        folder.mkdirs()
    }
    val collections = HashMap<String, FieldCollection<*>>()
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> collection(type: KType, name: String): FieldCollection<T>
            = collections.getOrPut(name) {
        InMemoryUnsafePersistentFieldCollection(serialization.json, serialization.json.serializersModule.serializer(type) as KSerializer<T>, folder.resolve(name))
    } as FieldCollection<T>
}