package com.lightningkite.lightningserver.jsonschema

import com.lightningkite.lightningserver.jsonschema.internal.ForJson
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

/**
 * Global Json object for basic serialization. uses Stable Configuration.
 */
val globalJson by lazy {
  Json {
    prettyPrintIndent = "  "
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
  }
}

/**
 * Represents the type of a json type
 */
enum class JsonType(jsonType: String) {
  /**
   * Represents the json array type
   */
  ARRAY("array"),

  /**
   * Represents the json number type
   */
  NUMBER("number"),

  /**
   * Represents the string type
   */
  STRING("string"),

  /**
   * Represents the boolean type
   */
  BOOLEAN("boolean"),

  /**
   * Represents the object type, this is used for serializing normal classes
   */
  OBJECT("object"),

  /**
   * Represents the object type, this is used for serializing sealed classes
   */
  OBJECT_SEALED("object"),

  /**
   * Represents the object type, this is used for serializing maps
   */
  OBJECT_MAP("object");

  val json = JsonPrimitive(jsonType)

  override fun toString(): String = json.content
}

/**
 * Adds a `$schema` property with the provided [url] that points to the Json Schema,
 * this can be a File location or a HTTP URL
 *
 * This is so when you serialize your [value] it will use [url] as it's Json Schema for code completion.
 */
fun <T> Json.encodeWithSchema(serializer: SerializationStrategy<T>, value: T, url: String): String {
  val json = encodeToJsonElement(serializer, value) as JsonObject
  val append = mapOf("\$schema" to JsonPrimitive(url))

  return encodeToString(JsonObject.serializer(), JsonObject(append + json))
}

/**
 * Stringifies the provided [descriptor] with [buildJsonSchema]
 *
 * @param generateDefinitions Should this generate definitions by default
 */
fun Json.encodeToSchema(descriptor: SerialDescriptor, generateDefinitions: Boolean = true): String {
  return encodeToString(JsonObject.serializer(), ForJson(this).buildJsonSchema(descriptor, generateDefinitions))
}

/**
 * Stringifies the provided [serializer] with [buildJsonSchema], same as doing
 * ```kotlin
 * json.encodeToSchema(serializer.descriptor)
 * ```
 * @param generateDefinitions Should this generate definitions by default
 */
fun Json.encodeToSchema(serializer: SerializationStrategy<*>, generateDefinitions: Boolean = true): String {
  return encodeToSchema(serializer.descriptor, generateDefinitions)
}

