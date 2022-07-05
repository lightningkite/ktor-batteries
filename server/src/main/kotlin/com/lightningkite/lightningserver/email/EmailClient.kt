package com.lightningkite.lightningserver.email

import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * An interface for sending emails. This is used directly by the EmailSettings to abstract the implementation of
 * sending emails away, so it can go to multiple places.
 */
interface EmailClient {
    suspend fun send(
        subject: String,
        to: List<String>,
        message: String,
        htmlMessage: String? = null,
        attachments: List<Attachment> = listOf(),
    )
    data class Requirement(override val name: String): Server.ResourceRequirement<EmailClient, EmailConfig> {
        override fun default(): EmailConfig = EmailConfig(protocol = EmailConfig.Protocol.Console)
        override val type: String
            get() = "email"
        override val serializer: KSerializer<EmailConfig>
            get() = EmailConfig.serializer()

        override fun ServerRunner.fromExplicit(setting: EmailConfig): EmailClient {
            return when(setting.protocol) {
                EmailConfig.Protocol.Console -> ConsoleEmailClient
                EmailConfig.Protocol.Smtp -> SmtpEmailClient(setting)
            }
        }
    }
    companion object {
        val default = EmailClient.Requirement("email")
    }
}

val email: EmailClient.Requirement get() = EmailClient.default

@Serializable
data class EmailConfig(
    val protocol: Protocol = Protocol.Smtp,
    val hostName: String = "",
    val port: Int = 25,
    val username: String? = null,
    val password: String? = null,
    val useSSL: Boolean = true,
    val fromEmail: String = "",
) {
    @Serializable
    enum class Protocol {
        Smtp, Console
    }
}