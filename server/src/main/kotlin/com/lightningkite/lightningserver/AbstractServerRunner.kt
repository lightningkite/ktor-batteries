package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.files.ExternalServerFileSerializer
import com.lightningkite.lightningserver.logging.loggingSetup
import com.lightningkite.lightningserver.serialization.Serialization
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractServerRunner(
    final override val server: Server,
    val resourceSettings: Map<Server.ResourceRequirement<*, *>, Any?>,
    settings: Map<Server.Setting<*>, Any?>
) : ServerRunner {
    @Suppress("UNCHECKED_CAST")
    data class SettingBox<T>(val item: T)
    private val resources = ConcurrentHashMap<Server.ResourceRequirement<*, *>, Any>()
    private val settings = ConcurrentHashMap<Server.Setting<*>, SettingBox<Any?>>().apply { putAll(settings.mapValues { SettingBox(it.value) }) }

    abstract fun <T, S> interceptResource(resource: Server.ResourceRequirement<T, S>): T?
    abstract fun <T> interceptSetting(resource: Server.Setting<T>): T?

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
            interceptResource(this) ?: fromExplicit(resourceSettings[this] ?: throw IllegalStateException("No resource found for ${this.name}"))
        }
    } as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> Server.Setting<T>.invoke(): T = (settings.getOrPut(this) {
        SettingBox(interceptSetting(this) ?: throw IllegalStateException("No setting found for ${this.name}"))
    } as SettingBox<T>).item

    init {
        loggingSetup()
        server.startupActions.forEach { it() }
    }
}