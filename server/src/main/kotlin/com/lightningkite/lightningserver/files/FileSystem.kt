package com.lightningkite.lightningserver.files

import com.dalet.vfs2.provider.azure.AzFileProvider
import com.lightningkite.lightningdb.HealthCheckable
import com.lightningkite.lightningdb.HealthStatus
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import java.util.concurrent.ConcurrentHashMap

val signedUrlExpirationSeconds = Server.Setting("signedUrlExpiration", Int.serializer().nullable) { null }
val localFilesPath = Server.Setting("localFilesPath", String.serializer()) { "./local" }

private val map = ConcurrentHashMap<org.apache.commons.vfs2.FileSystem, FileSystem>()
var org.apache.commons.vfs2.FileSystem.lightningServer: FileSystem
    get() = map.get(this)!!
    set(value) {
        map.put(this, value)
    }

class FileSystem(val publicUrl: String, val localFilesPath: String, val expirationSeconds: Int? = null, val name: String, val root: FileObject) :
    HealthCheckable {
    init {
        root.fileSystem.lightningServer = this
    }

    override val healthCheckName: String get() = "Storage ($name)"
    override suspend fun healthCheck(): HealthStatus = try {
        root
            .resolveFile("healthCheck.txt")
            .use { file ->
                file.content.outputStream
                    .buffered()
                    .use { out -> "Health Check".toByteArray().inputStream().copyTo(out) }
            }
        HealthStatus(HealthStatus.Level.OK)
    } catch (e: Exception) {
        HealthStatus(HealthStatus.Level.ERROR, additionalMessage = e.message)
    }
}

data class FileSystemRequirement(override val name: String) : Server.ResourceRequirement<FileSystem, String> {
    override val type: String
        get() = "fileSystem"
    override val serializer: KSerializer<String> get() = String.serializer()
    override fun ServerRunner.fromExplicit(setting: String): FileSystem {
        val storageUrl = setting.substringBefore('?')
        val params =
            setting.substringAfter('?').split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
        if (storageUrl.startsWith("az")) {
            val auth = StaticUserAuthenticator(
                "",
                storageUrl.substringAfter("://").substringBefore('.'),
                params["key"] ?: throw IllegalStateException("Azure file system requested, but no key was provided.")
            )
            println("Establishing authenticator for Azure as $auth")
            DefaultFileSystemConfigBuilder.getInstance()
                .setUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions(), auth)
            println(
                DefaultFileSystemConfigBuilder.getInstance()
                    .getUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions())
            )
        }
        return FileSystem("$publicUrl/user-content", localFilesPath(), signedUrlExpirationSeconds(), name, VFS.getManager().resolveFile(storageUrl))
    }

    override fun default(): String = "file://./local"
}

val files = FileSystemRequirement("fileSystem")
