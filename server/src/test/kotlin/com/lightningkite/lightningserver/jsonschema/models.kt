@file:UseContextualSerialization(UUID::class)
package com.lightningkite.lightningserver.jsonschema

import com.lightningkite.lightningdb.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.UseSerializers
import java.util.*

@Serializable
@DatabaseModel
data class Post(
    override val _id: UUID = UUID.randomUUID(),
    val author: String,
    val title: String,
    val content: String
) : HasId<UUID>
