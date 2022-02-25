package com.lightningkite.ktorbatteries.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FcmNotificationInterface : NotificationInterface {
    override suspend fun send(
        targets: List<String>,
        title: String?,
        body: String?,
        imageUrl: String?,
        data: Map<String, String>
    ) {
        withContext(Dispatchers.IO) {
            targets.chunked(500)
                .map {
                    MulticastMessage.builder()
                        .apply {
                            addAllTokens(it)
                            putAllData(data)
                            if (title != null || body != null || imageUrl != null) {
                                setNotification(
                                    Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .setImage(imageUrl)
                                        .build()
                                )

                            }
                        }
                        .build()
                }
                .forEach { FirebaseMessaging.getInstance().sendMulticast(it) }
        }
    }

}