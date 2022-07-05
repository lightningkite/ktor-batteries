package com.lightningkite.lightningserver

import com.lightningkite.lightningserver.serialization.HasSerialization
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.task.Task

interface ServerRunner : HasSerialization {
    val server: Server
    val publicUrl: String
    val debug: Boolean
    override val serialization: Serialization
    operator fun <T, S> Server.ResourceRequirement<T, S>.invoke(): T
    operator fun <T> Server.Setting<T>.invoke(): T
    suspend fun sendWebSocket(id: String, message: String)
    operator fun <INPUT> Task<INPUT>.invoke(input: INPUT)
}
// goal syntax

/*

KNOWN REQUIREMENT TYPES
- File System
- Database
- Cache
- Email
- Notifications
- Logging

class MyServer: Server() {
    val db by database
    val rest = ServerPath("test-model").rest
}

AWS.deploy(server)

 */

