package com.lightningkite.lightningdb

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Database: HealthCheckable {
    fun <T: Any> collection(type: KType, name: String): FieldCollection<T>
    companion object
}

inline fun <reified T: Any> Database.collection(name: String = T::class.simpleName!!): FieldCollection<T> {
    return collection(typeOf<T>(), name)
}