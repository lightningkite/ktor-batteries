package com.lightningkite.lightningserver

import com.lightningkite.lightningdb.Database
import com.lightningkite.lightningserver.cache.CacheInterface
import com.lightningkite.lightningserver.cache.LocalCache
import com.lightningkite.lightningserver.db.DatabaseRequirement
import com.lightningkite.lightningserver.db.InMemoryDatabase
import com.lightningkite.lightningserver.email.ConsoleEmailClient
import com.lightningkite.lightningserver.email.EmailClient
import com.lightningkite.lightningserver.files.FileSystem
import com.lightningkite.lightningserver.files.FileSystemRequirement
import com.lightningkite.lightningserver.files.localFilesPath
import com.lightningkite.lightningserver.files.signedUrlExpirationSeconds
import com.lightningkite.lightningserver.notifications.ConsoleNotificationInterface
import com.lightningkite.lightningserver.notifications.NotificationInterface
import com.lightningkite.lightningserver.pubsub.LocalPubSub
import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.task.Task
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.local.LocalFile

class TestServerRunner(
    server: Server,
    override val publicUrl: String = "http://localhost",
    override val debug: Boolean = true
): AbstractServerRunner(server, mapOf(), mapOf()) {

    val pubSub = LocalPubSub(serialization)

    override fun <T, S> interceptResource(resource: Server.ResourceRequirement<T, S>): T? {
        @Suppress("UNCHECKED_CAST")
        return when(resource) {
            is DatabaseRequirement -> InMemoryDatabase(serialization) as T
            is CacheInterface.Requirement -> LocalCache(serialization) as T
            is FileSystemRequirement -> FileSystem("http://localhost", localFilesPath(), signedUrlExpirationSeconds(), resource.name, VFS.getManager().resolveFile("local/test")) as T
            is NotificationInterface.Requirement -> ConsoleNotificationInterface as T
            is EmailClient.Requirement -> ConsoleEmailClient as T
            else -> with(resource) { fromExplicit(default()) }
        }
    }

    override fun <T> interceptSetting(resource: Server.Setting<T>): T? {
        return resource.default()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun <INPUT> Task<INPUT>.invoke(input: INPUT) {
        GlobalScope.launch {
            this@invoke.implementation(this@TestServerRunner, input)
        }
    }

    override suspend fun sendWebSocket(id: String, message: String) {
        pubSub.string("ws-$id").emit(message)
    }
}