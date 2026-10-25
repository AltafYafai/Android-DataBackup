package com.xayah.core.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.topjohnwu.superuser.Shell
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.network.model.Release
import com.xayah.core.network.retrofit.GitHubRepository
import com.xayah.core.model.util.formatSize
import com.xayah.core.util.LogUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val changelog: String,
    val sizeBytes: Long,
    val sizeText: String,
    val downloadUrl: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Checking(val isManual: Boolean = false) : UpdateState
    data class Available(val updateInfo: UpdateInfo) : UpdateState
    data class UpToDate(val versionName: String, val changelog: String) : UpdateState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long, val updateInfo: UpdateInfo) : UpdateState
    data class ReadyToInstall(val apkFile: File, val updateInfo: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

@Singleton
class UpdateRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubRepo: GitHubRepository,
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    companion object {
        const val DEFAULT_CHANGELOG = """• Material 3 Expressive UI & Humatic processing HUD
• Dual-track progress bars (16.dp Green batch bar, 5.dp Blue step bar)
• Accurate 6-partition size calculation (APK, User, User DE, Data, OBB, Media)
• Instant direct backup start bypassing setup screen
• 3-part partition breakdown cards (Icon + Name, Size, Status badge)
• Interactive Backed Up Apps overview dialog
• In-app updater with auto-check on start & manual check in Settings
• Root and PackageInstaller install fallbacks"""
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    suspend fun checkForUpdate(isManual: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Checking(isManual = isManual)
            val release: Release = githubRepo.getLatestRelease()
            val releaseVersion = release.tagName.ifEmpty { release.name }.trim().removePrefix("v").removePrefix("V")
            val currentVersion = BuildConfigUtil.VERSION_NAME.trim().removePrefix("v").removePrefix("V")

            if (isNewerVersion(currentVersion, releaseVersion)) {
                // Find matching APK asset
                val apkAsset = release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true) &&
                        (asset.name.contains(BuildConfigUtil.FLAVOR_abi, ignoreCase = true) || asset.url.contains(BuildConfigUtil.FLAVOR_abi, ignoreCase = true))
                } ?: release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true)
                } ?: release.assets.firstOrNull()

                val downloadUrl = apkAsset?.url ?: release.url
                val sizeBytes = apkAsset?.size ?: 0L
                val sizeText = if (sizeBytes > 0) sizeBytes.toDouble().formatSize() else ""

                val info = UpdateInfo(
                    versionName = release.tagName.ifEmpty { release.name },
                    changelog = release.body.ifEmpty { "New update available!" },
                    sizeBytes = sizeBytes,
                    sizeText = sizeText,
                    downloadUrl = downloadUrl,
                )
                _updateState.value = UpdateState.Available(info)
                info
            } else {
                if (isManual) {
                    val currentChangelog = release.body.ifEmpty { DEFAULT_CHANGELOG }
                    _updateState.value = UpdateState.UpToDate(
                        versionName = BuildConfigUtil.VERSION_NAME,
                        changelog = currentChangelog
                    )
                } else {
                    _updateState.value = UpdateState.Idle
                }
                null
            }
        } catch (e: Exception) {
            LogUtil.log("Update check error: ${e.message}")
            if (isManual) {
                _updateState.value = UpdateState.UpToDate(
                    versionName = BuildConfigUtil.VERSION_NAME,
                    changelog = DEFAULT_CHANGELOG
                )
            } else {
                _updateState.value = UpdateState.Idle
            }
            null
        }
    }

    suspend fun downloadAndInstall(updateInfo: UpdateInfo) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val updateFile = File(cacheDir, "update.apk")
            if (updateFile.exists()) updateFile.delete()

            val request = Request.Builder().url(updateInfo.downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _updateState.value = UpdateState.Error("Download failed with HTTP ${response.code}")
                return@withContext
            }

            val body = response.body
            val totalBytes = if ((body?.contentLength() ?: 0L) > 0L) body!!.contentLength() else updateInfo.sizeBytes

            body?.byteStream()?.use { input ->
                updateFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        _updateState.value = UpdateState.Downloading(progress, totalRead, totalBytes, updateInfo)
                    }
                    output.flush()
                }
            }

            _updateState.value = UpdateState.ReadyToInstall(updateFile, updateInfo)
            installApk(updateFile)
        } catch (e: Exception) {
            LogUtil.log("Download update error: ${e.message}")
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to download update")
        }
    }

    fun installApk(apkFile: File) {
        try {
            // First attempt root installation if available
            runCatching {
                Shell.cmd("pm install -r ${apkFile.absolutePath}").submit()
            }

            // Launch standard PackageInstaller via FileProvider
            val authority = "com.xayah.core.provider.FileSharingProvider.${BuildConfigUtil.FLAVOR_feature.lowercase()}"
            val uri = FileProvider.getUriForFile(
                context,
                authority,
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtil.log("Install update error: ${e.message}")
        }
    }

    private fun isNewerVersion(current: String, release: String): Boolean {
        if (release.isEmpty()) return false
        val cParts = current.split(Regex("[.-]")).mapNotNull { it.toIntOrNull() }
        val rParts = release.split(Regex("[.-]")).mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(cParts.size, rParts.size)
        for (i in 0 until maxLen) {
            val c = cParts.getOrElse(i) { 0 }
            val r = rParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
