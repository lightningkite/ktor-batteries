package com.lightningkite.ktordb

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DatabaseModel

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class AllowedTypes(vararg val types: String)

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class StoragePrefix(val prefix: String)