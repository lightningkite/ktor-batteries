package com.lightningkite.lightningserver.email

/**
 * An email client that will simply print out everything to the console
 */
object ConsoleEmailClient : EmailClient {
    override suspend fun send(
        subject: String,
        to: List<String>,
        message: String,
        htmlMessage: String?,
        attachments: List<Attachment>
    ) {
        if (to.isEmpty() || (System.getenv("test") == "true")) return
        println(buildString {
            appendLine("-----EMAIL-----")
            appendLine(subject)
            appendLine()
            appendLine(to.joinToString())
            appendLine()
//            htmlMessage?.let{
//                appendLine(it)
//                appendLine()
//            }
            appendLine(message)
            appendLine()
            attachments.forEach {
                appendLine(it.name)
                when (it) {
                    is Attachment.Local -> appendLine(it.file)
                    is Attachment.Remote -> appendLine(it.url)
                }
                appendLine(it.description)
                appendLine()
            }
        })
    }
}