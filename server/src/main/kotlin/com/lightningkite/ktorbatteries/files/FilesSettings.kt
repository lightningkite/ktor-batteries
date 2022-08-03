package com.lightningkite.ktorbatteries.files

import com.dalet.vfs2.provider.azure.AzConstants
import com.dalet.vfs2.provider.azure.AzFileProvider
import com.github.vfss3.S3FileProvider
import com.lightningkite.ktorbatteries.SettingSingleton
import com.lightningkite.ktorbatteries.serverhealth.HealthCheckable
import com.lightningkite.ktorbatteries.serverhealth.HealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.impl.DefaultFileSystemManager
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider
import java.io.File

/**
 * FileSettings defines where server files and user content is stored. This used ApacheVFS which allows the filesystem to be
 * a variety of sources. For now this is set up to handle a local file system, a s3 bucket, or an azure blob container.
 *
 * @param storageUrl Defines where the file system is. This follows ApacheVFS standards.
 * @param key Used only by Azure right now. Used to authenticate with Azure.
 * @param userContentPath A path you wish all file paths to be prefixed with.
 * @param signedUrlExpirationSeconds When dealing with secured filesystems that require url signing this will determine how long pre-signed URLs will be valid for.
 */

@Serializable
data class FilesSettings(
    val storageUrl: String = "file://${File("./local/files").absolutePath}",
    val key: String? = null,
    val userContentPath: String = "user-content",
    val signedUrlExpirationSeconds: Int? = null
) : HealthCheckable {
    companion object : SettingSingleton<FilesSettings>() {
        val manager by lazy {
            DefaultFileSystemManager().apply {
                addProvider(AzConstants.AZBSSCHEME, AzFileProvider())
                addProvider("s3", S3FileProvider())
                addProvider("file", DefaultLocalFileProvider())
                init()
            }
        }
    }

    init {
        if (storageUrl.startsWith("az")) {
            val auth = StaticUserAuthenticator(
                "",
                storageUrl.substringAfter("://").substringBefore('.'),
                this.key ?: throw IllegalStateException("Azure file system requested, but no key was provided.")
            )
            println("Establishing authenticator for Azure as $auth")
            DefaultFileSystemConfigBuilder.getInstance()
                .setUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions(), auth)
            println(
                DefaultFileSystemConfigBuilder.getInstance()
                    .getUserAuthenticator(AzFileProvider.getDefaultFileSystemOptions())
            )
        }
        instance = this
    }

    override val healthCheckName: String get() = "Storage"
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

    val root get() = manager.resolveFile(instance.storageUrl)
}
