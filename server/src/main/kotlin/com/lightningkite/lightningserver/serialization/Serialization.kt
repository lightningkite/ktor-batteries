package com.lightningkite.lightningserver.serialization

import com.github.jershell.kbson.Configuration
import com.github.jershell.kbson.KBson
import com.lightningkite.lightningserver.SetOnce
import com.lightningkite.lightningdb.ClientModule
import com.lightningkite.lightningdb.ServerFile
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.core.ContentType
import com.lightningkite.lightningserver.exceptions.BadRequestException
import com.lightningkite.lightningserver.files.*
import com.lightningkite.lightningserver.http.HttpContent
import com.lightningkite.lightningserver.http.HttpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.cbor.CborBuilder
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.csv.config.CsvBuilder
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.properties.Properties
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.reflect.*

/**
 * A place to hold all the support Serialization types.
 */
data class Serialization(
    val module: SerializersModule = ClientModule, // ClientModule.overwriteWith(serializersModuleOf(ExternalServerFileSerializer(runner)))
    val jsonConfig: JsonBuilder.()->Unit = {
        ignoreUnknownKeys = true
    },
    val csvConfig: CsvBuilder.()->Unit = {
        hasHeaderRecord = true
        ignoreUnknownColumns = true
    },
    val bsonConfig: Configuration = Configuration(),
    val cborConfig: CborBuilder.()->Unit = {
        ignoreUnknownKeys = true
        serializersModule = module
    }
) {
    val json: Json = Json() {
        jsonConfig()
        serializersModule = module
    }
    val csv: Csv = Csv {
        csvConfig()
        serializersModule = module
    }
    val bson: KBson = KBson(module, bsonConfig)
    val cbor: Cbor = Cbor {
        cborConfig()
        serializersModule = module
    }
    val javaData: JavaData = JavaData(module)
    val properties: Properties = Properties(module)
}

interface HasSerialization {
    val serialization: Serialization
}