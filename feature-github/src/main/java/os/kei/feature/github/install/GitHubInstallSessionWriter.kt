package os.kei.feature.github.install

import android.content.Context
import android.content.pm.PackageInstaller
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.download.segmented.SegmentedDownloadClient
import os.kei.core.download.segmented.SegmentedDownloadOptions
import os.kei.core.download.segmented.SegmentedDownloadRequest
import os.kei.core.log.AppLogger
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.isGitHubActionsApkArtifactArchive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.roundToInt

private const val GITHUB_INSTALL_SESSION_WRITER_TAG = "GitHubInstallWriter"
private const val GITHUB_DOWNLOAD_PROGRESS_INTERVAL_MS = 200L
private const val GITHUB_SEGMENTED_MIN_SIZE_BYTES = 8L * 1024L * 1024L
private const val GITHUB_SEGMENTED_PART_SIZE_BYTES = 4L * 1024L * 1024L
private const val GITHUB_SEGMENTED_MAX_CONNECTIONS = 4

data class GitHubInstallSessionWriteResult(
    val bytesWritten: Long,
    val totalBytes: Long,
    val archiveInfo: GitHubApkArchiveInfo = GitHubApkArchiveInfo(),
)

class GitHubInstallSessionWriter(
    private val client: OkHttpClient,
    downloadDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
) {
    private val segmentedDownloadClient =
        SegmentedDownloadClient(
            client = client,
            dispatcher = downloadDispatcher,
        )

    suspend fun streamApkIntoSession(
        context: Context,
        resolvedUrl: String,
        asset: GitHubReleaseAssetFile,
        session: PackageInstaller.Session,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult =
        if (asset.isGitHubActionsApkArtifactArchive()) {
            streamActionsApkArchiveIntoSession(
                context = context,
                resolvedUrl = resolvedUrl,
                assetName = asset.name,
                declaredSizeBytes = asset.sizeBytes,
                session = session,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        } else {
            streamDirectApkIntoSession(
                context = context,
                resolvedUrl = resolvedUrl,
                asset = asset,
                session = session,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        }

    private suspend fun streamDirectApkIntoSession(
        context: Context,
        resolvedUrl: String,
        asset: GitHubReleaseAssetFile,
        session: PackageInstaller.Session,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val tempApkFile = createGitHubTempApkFile(context, asset.name)
        try {
            downloadToTempFile(
                resolvedUrl = resolvedUrl,
                declaredSizeBytes = asset.sizeBytes,
                outputFile = tempApkFile,
                acceptHeader = "application/vnd.android.package-archive, application/octet-stream;q=0.9, */*;q=0.1",
                sessionId = sessionId,
                onProgress = onProgress,
            )
            return streamDownloadedApkFileIntoSession(
                context = context,
                apkFile = tempApkFile,
                sessionName = asset.name.toGitHubApkSessionName(),
                session = session,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        } finally {
            runCatching { tempApkFile.delete() }
        }
    }

    private suspend fun streamActionsApkArchiveIntoSession(
        context: Context,
        resolvedUrl: String,
        assetName: String,
        declaredSizeBytes: Long,
        session: PackageInstaller.Session,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val archiveFile = createGitHubTempInstallFile(context, assetName, ".zip")
        try {
            downloadToTempFile(
                resolvedUrl = resolvedUrl,
                declaredSizeBytes = declaredSizeBytes,
                outputFile = archiveFile,
                acceptHeader = "application/zip, application/octet-stream;q=0.9, */*;q=0.1",
                sessionId = sessionId,
                onProgress = onProgress,
            )
            ZipFile(archiveFile).use { zipFile ->
                if (zipFile.isGitHubDirectApkArchive()) {
                    return streamDownloadedApkFileIntoSession(
                        context = context,
                        apkFile = archiveFile,
                        sessionName = assetName.toGitHubApkSessionName(),
                        session = session,
                        sessionId = sessionId,
                        onProgress = onProgress,
                    )
                }
                val apkEntry =
                    selectGitHubInstallApkEntry(zipFile)
                        ?: throw IOException("Actions artifact contains no installable APK")
                return streamZipEntryIntoSession(
                    context = context,
                    zipFile = zipFile,
                    apkEntry = apkEntry,
                    session = session,
                    sessionId = sessionId,
                    onProgress = onProgress,
                )
            }
        } finally {
            runCatching { archiveFile.delete() }
        }
    }

    private suspend fun streamZipEntryIntoSession(
        context: Context,
        zipFile: ZipFile,
        apkEntry: ZipEntry,
        session: PackageInstaller.Session,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val entrySize = apkEntry.size.takeIf { it > 0L } ?: -1L
        val sessionName = apkEntry.name.substringAfterLast('/').ifBlank { "base.apk" }
        val progress =
            GitHubInstallProgressEmitter(
                sessionId = sessionId,
                totalBytes = entrySize,
                onProgress = onProgress,
            )
        progress.emit(force = true)
        val tempApkFile = createGitHubTempApkFile(context, sessionName)
        try {
            zipFile.getInputStream(apkEntry).use { input ->
                session.openWrite(sessionName, 0, entrySize).use { output ->
                    FileOutputStream(tempApkFile).use { archiveOutput ->
                        val buffer = ByteArray(GITHUB_APK_STREAM_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            archiveOutput.write(buffer, 0, read)
                            progress.add(read.toLong())
                            progress.emit()
                        }
                        archiveOutput.flush()
                    }
                    progress.emit(force = true)
                    val archiveInfo = readGitHubApkArchiveInfo(context, tempApkFile)
                    emitStagingProgress(
                        sessionId = sessionId,
                        totalRead = progress.totalRead,
                        totalBytes = entrySize,
                        archiveInfo = archiveInfo,
                        onProgress = onProgress,
                    )
                    session.fsync(output)
                    return GitHubInstallSessionWriteResult(progress.totalRead, entrySize, archiveInfo)
                }
            }
        } finally {
            runCatching { tempApkFile.delete() }
        }
    }

    private suspend fun streamDownloadedApkFileIntoSession(
        context: Context,
        apkFile: File,
        sessionName: String,
        session: PackageInstaller.Session,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val totalBytes = apkFile.length().takeIf { it > 0L } ?: -1L
        val progress =
            GitHubInstallProgressEmitter(
                sessionId = sessionId,
                totalBytes = totalBytes,
                onProgress = onProgress,
            )
        progress.emit(force = true)
        FileInputStream(apkFile).use { input ->
            session.openWrite(sessionName, 0, totalBytes).use { output ->
                val buffer = ByteArray(GITHUB_APK_STREAM_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    progress.add(read.toLong())
                    progress.emit()
                }
                progress.emit(force = true)
                val archiveInfo = readGitHubApkArchiveInfo(context, apkFile)
                emitStagingProgress(
                    sessionId = sessionId,
                    totalRead = progress.totalRead,
                    totalBytes = totalBytes,
                    archiveInfo = archiveInfo,
                    onProgress = onProgress,
                )
                session.fsync(output)
                return GitHubInstallSessionWriteResult(progress.totalRead, totalBytes, archiveInfo)
            }
        }
    }

    private suspend fun downloadToTempFile(
        resolvedUrl: String,
        declaredSizeBytes: Long,
        outputFile: File,
        acceptHeader: String,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ) {
        val result =
            segmentedDownloadClient.downloadToFile(
                request = SegmentedDownloadRequest(
                    url = resolvedUrl,
                    outputFile = outputFile,
                    headers = mapOf(
                        "User-Agent" to "KeiOS-App/1.0 (Android)",
                        "Accept" to acceptHeader,
                    ),
                    fileNameHint = outputFile.name,
                ),
                options = SegmentedDownloadOptions(
                    minParallelSizeBytes = GITHUB_SEGMENTED_MIN_SIZE_BYTES,
                    initialPartSizeBytes = GITHUB_SEGMENTED_PART_SIZE_BYTES,
                    maxConnections = GITHUB_SEGMENTED_MAX_CONNECTIONS,
                    maxRetriesPerPart = 3,
                    retryDelayMs = 1_000L,
                    progressIntervalMs = GITHUB_DOWNLOAD_PROGRESS_INTERVAL_MS,
                    requireHttpsForParallel = true,
                    bufferSizeBytes = GITHUB_APK_STREAM_BUFFER_SIZE,
                ),
                onProgress = { progress ->
                    val totalBytes =
                        when {
                            progress.totalBytes > 0L -> progress.totalBytes
                            declaredSizeBytes > 0L -> declaredSizeBytes
                            else -> -1L
                        }
                    onProgress(
                        GitHubApkInstallProgress(
                            stage = GitHubApkInstallStage.Downloading,
                            progressPercent = downloadProgressPercent(
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = totalBytes,
                            ),
                            downloadedBytes = progress.downloadedBytes,
                            totalBytes = totalBytes,
                            sessionId = sessionId,
                        )
                    )
                },
            )
        AppLogger.i(GITHUB_INSTALL_SESSION_WRITER_TAG) {
            "asset downloaded parallel=${result.parallel} range=${result.rangeSupported} " +
                "bytes=${result.totalBytes} retry=${result.retryCount} fallback=${result.fallbackReason.orEmpty()}"
        }
    }

    private suspend fun emitStagingProgress(
        sessionId: Int,
        totalRead: Long,
        totalBytes: Long,
        archiveInfo: GitHubApkArchiveInfo,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ) {
        onProgress(
            GitHubApkInstallProgress(
                stage = GitHubApkInstallStage.Staging,
                progressPercent = 100,
                downloadedBytes = totalRead,
                totalBytes = totalBytes,
                sessionId = sessionId,
                appLabel = archiveInfo.appLabel,
                packageName = archiveInfo.packageName,
                versionName = archiveInfo.versionName,
                versionCode = archiveInfo.versionCode,
                minSdk = archiveInfo.minSdk,
                targetSdk = archiveInfo.targetSdk,
            ),
        )
    }
}

private fun downloadProgressPercent(
    downloadedBytes: Long,
    totalBytes: Long,
): Int {
    if (totalBytes <= 0L) return 0
    val fraction = downloadedBytes.toDouble() / totalBytes.toDouble()
    return (fraction.coerceIn(0.0, 1.0) * 100.0).roundToInt().coerceIn(0, 100)
}
