package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.files.ExternalServerFileSerializer
import com.lightningkite.lightningserver.serialization.Serialization
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractServerRunner(
    final override val server: Server,
    val resourceSettings: Map<Server.ResourceRequirement<*, *>, Any?>,
    val settings: Map<Server.Setting<*>, Any>
) : ServerRunner {
    @Suppress("UNCHECKED_CAST")
    val resources = ConcurrentHashMap<Server.ResourceRequirement<*, *>, Any?>()

    abstract fun resourceInterceptor

    @Suppress("LeakingThis")
    override val serialization: Serialization = server.serialization.copy(
        module = server.serialization.module.overwriteWith(
            serializersModuleOf(
                ExternalServerFileSerializer(this)
            )
        )
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T, S> Server.ResourceRequirement<T, S>.invoke(): T = resources.getOrPut(this) {
        with(this as Server.ResourceRequirement<Any?, Any?>) {
            fromExplicit(resourceSettings[this] ?: throw IllegalStateException("No setting found for ${this.name}"))
        }
    } as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> Server.Setting<T>.invoke(): T = settings[this] as T
}