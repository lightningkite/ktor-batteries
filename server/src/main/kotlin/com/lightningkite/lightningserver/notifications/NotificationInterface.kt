package com.lightningkite.lightningserver.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File


interface NotificationInterface {
    suspend fun send(
        targets: List<String>,
        title: String? = null,
        body: String? = null,
        imageUrl: String? = null,
        data: Map<String, String>? = null,
        critical: Boolean = false,
        androidChannel: String? = null
    )
    data class Requirement(override val name: String): Server.ResourceRequirement<NotificationInterface, String> {
        override val type: String
            get() = "notification"
        override val serializer: KSerializer<String>
            get() = String.serializer()

        override fun default(): String = "console"

        override fun ServerRunner.fromExplicit(setting: String): NotificationInterface = when {
            setting == "console" -> ConsoleNotificationInterface
            setting.startsWith("fcm://") -> {
                val file = File(setting.substringAfter("fcm://"))
                assert(file.exists()) { "FCM credentials file not found at '$file'" }
                FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(file.inputStream()))
                        .build()
                )
                FcmNotificationInterface
            }
            else -> throw IllegalStateException("Unknown scheme $setting - supported are 'console' and 'fcm'")
        }
    }
    companion object {
        val default = NotificationInterface.Requirement("notification")
    }
}
val notifications: NotificationInterface.Requirement get() = NotificationInterface.default