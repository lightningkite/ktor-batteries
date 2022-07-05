package com.lightningkite.lightningserver.files

import com.dalet.vfs2.provider.azure.AzFileProvider
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.SettingSingleton
import com.lightningkite.lightningserver.core.ContentType
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.serverhealth.HealthCheckable
import com.lightningkite.lightningserver.serverhealth.HealthStatus
import io.ktor.server.plugins.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.local.LocalFileSystem
import java.io.File

val signedUrlExpirationSeconds = Server.Setting("signedUrlExpiration", Int.serializer().nullable) { null }
val localFilesPath = Server.Setting("localFilesPath", String.serializer()) { "./local" }
internal const val userContentPath: String = "user-content"

//        if(storageUrl.startsWith("az")) {
//            val auth = StaticUserAuthenticator("", storageUrl.substringAfter("://").substringBefore('.'), this.key ?: throw IllegalStateException("Azure file system requested, but no key was provided."))
//            println("Establishing authenticator for Azure as $auth")
//            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions(), auth)
//            println(DefaultFileSystemConfigBuilder.getInstance().getUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions()))
//        }

//        if(root is LocalFileSystem) {
//            routing {
//                path(userContentPath + "/*").apply{
//                    get.handler {
//                        if(it.wildcard?.contains('.') != false) throw IllegalStateException()
//                        val file = root.resolveFile(it.wildcard).content
//                        HttpResponse(
//                            body = HttpContent.Stream(
//                                getStream = { file.inputStream },
//                                length = file.size,
//                                type = ContentType(file.contentInfo.contentType)
//                            ),
//                        )
//                    }
//                    post.handler {
//                        val location = AuthSettings.instance.verify<String>(it.queryParameter("token") ?: throw BadRequestException("No token provided"))
//                        if(location != it.wildcard) throw BadRequestException("Token does not match file")
//                        if(it.wildcard.contains("..")) throw IllegalStateException()
//                        val file = root.resolveFile(it.wildcard).content
//                        it.body?.stream()?.copyTo(file.outputStream)
//                        HttpResponse(status = HttpStatus.NoContent)
//                    }
//                }
//            }
//        }

class FileSystem(val name: String, val root: FileObject): HealthCheckable {
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
data class FileSystemRequirement(val name: String): Server.ResourceRequirement<FileSystem, String> {
    override val type: String
        get() = "fileSystem"
    override val serializer: KSerializer<String> get() = String.serializer()
    override fun ServerRunner.fromExplicit(setting: String): FileSystem {
        val storageUrl = setting
        if(storageUrl.startsWith("az")) {
            val auth = StaticUserAuthenticator("", storageUrl.substringAfter("://").substringBefore('.'), this.key ?: throw IllegalStateException("Azure file system requested, but no key was provided."))
            println("Establishing authenticator for Azure as $auth")
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions(), auth)
            println(DefaultFileSystemConfigBuilder.getInstance().getUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions()))
        }
        return FileSystem(name, VFS.getManager().resolveFile(storageUrl))
    }

    override fun default(): String = "file://./local"
}
val files = FileSystemRequirement("fileSystem")
