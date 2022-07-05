package com.lightningkite.lightningserver

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

class SettingsSerializer(val server: Server) : KSerializer<Map<Server.Setting<*>, Any?>> {
    val parts = server.settings.toList()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(server.name + "Settings") {
        for (setting in parts) {
            element(setting.name, setting.serializer.descriptor, isOptional = true)
        }
    }

    override fun deserialize(decoder: Decoder): Map<Server.Setting<*>, Any?> {
        val map = HashMap<Server.Setting<*>, Any?>()
        decoder.decodeStructure(descriptor) {
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                if (index == CompositeDecoder.UNKNOWN_NAME) continue
                val setting = parts[index]
                @Suppress("UNCHECKED_CAST")
                map[setting] = decodeSerializableElement(descriptor, index, setting.serializer)
            }
        }
        return map
    }

    override fun serialize(encoder: Encoder, value: Map<Server.Setting<*>, Any?>) {
        encoder.encodeStructure(descriptor) {
            for ((index, setting) in parts.withIndex()) {
                @Suppress("UNCHECKED_CAST")
                encodeSerializableElement(
                    descriptor,
                    index,
                    setting.serializer as KSerializer<Any?>,
                    value[setting]
                )
            }
        }
    }
}