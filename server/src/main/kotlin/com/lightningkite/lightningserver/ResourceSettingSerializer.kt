package com.lightningkite.lightningserver

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

class ResourceSettingSerializer(val server: Server) : KSerializer<Map<Server.ResourceRequirement<*, *>, Any?>> {
    val parts = server.requirements.toList()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(server.name + "ResourceSettings") {
        for (setting in parts) {
            element(setting.name, setting.serializer.descriptor, isOptional = true)
        }
    }

    override fun deserialize(decoder: Decoder): Map<Server.ResourceRequirement<*, *>, Any?> {
        val map = HashMap<Server.ResourceRequirement<*, *>, Any?>()
        decoder.decodeStructure(descriptor) {
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                if (index == CompositeDecoder.UNKNOWN_NAME) continue
                val setting = parts[index]
                @Suppress("UNCHECKED_CAST")
                map[setting] = decodeSerializableElement(setting.serializer.descriptor, index, setting.serializer)
            }
        }
        return map
    }

    override fun serialize(encoder: Encoder, value: Map<Server.ResourceRequirement<*, *>, Any?>) {
        encoder.encodeStructure(descriptor) {
            for ((index, setting) in parts.withIndex()) {
                @Suppress("UNCHECKED_CAST")
                encodeSerializableElement(
                    setting.serializer.descriptor,
                    index,
                    setting.serializer as KSerializer<Any?>,
                    value[setting]
                )
            }
        }
    }
}