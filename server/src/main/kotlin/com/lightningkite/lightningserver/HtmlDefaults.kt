package com.lightningkite.lightningserver

object HtmlDefaults {
    var basePage: ServerRunner.(content: String) -> String = { content ->
        """
            <!DOCTYPE html>
            <html>
              <head>
                <meta charset="utf-8">
                <title>${server.name}</title>
              </head>
              <body>
                $content
              </body>
            </html>
        """.trimIndent()
    }
    var baseEmail: ServerRunner.(content: String) -> String = { content ->
        """
            <!DOCTYPE html>
            <html>
              <head>
                <meta charset="utf-8">
                <title>${server.name}</title>
              </head>
              <body>
                $content
              </body>
            </html>
        """.trimIndent()
    }
    var defaultLoginEmailTemplate: (suspend ServerRunner.(email: String, link: String) -> String) = { email: String, link: String ->
        baseEmail("""
        <p>We received a request for a login email for ${email}. To log in, please click the link below.</p>
        <a href="$link">Click here to login</a>
        <p>If you did not request to be logged in, you can simply ignore this email.</p>
        <h3>${server.name}</h3>
        """.trimIndent())
    }
}