package os.kei.feature.github.install

import android.content.Context
import android.content.pm.PackageInstaller
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.download.segmented.SegmentedDownloadClient
import os.kei.core.download.segmented.SegmentedDownloadConnectionStrategy
import os.kei.core.download.segmented.DownloadSizeMismatchException
import os.kei.core.download.segmented.SegmentedDownloadOptions
import os.kei.core.download.segmented.SegmentedDownloadProgress
import os.kei.core.download.segmented.SegmentedDownloadRequest
import os.kei.core.download.segmented.SegmentedDownloadSpeedProfile
import os.kei.core.log.AppLogger
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.isPotentialNestedApkArchive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val GITHUB_INSTALL_SESSION_WRITER_TAG = "GitHubInstallWriter"
private const val GITHUB_DOWNLOAD_PROGRESS_INTERVAL_MS = 200L
private const val GITHUB_SEGMENTED_MIN_SIZE_BYTES = 8L * 1024L * 1024L
private const val GITHUB_SEGMENTED_PART_SIZE_BYTES = 8L * 1024L * 1024L
private const val GITHUB_SEGMENTED_MAX_CONNECTIONS = 4
private const val GITHUB_FOREGROUND_BOOST_MIN_SIZE_BYTES = 4L * 1024L * 1024L
private const val GITHUB_FOREGROUND_BOOST_PART_SIZE_BYTES = 4L * 1024L * 1024L
private const val GITHUB_FOREGROUND_BOOST_MAX_CONNECTIONS = 12
private const val GITHUB_MIN_BYTES_PER_CONNECTION = 16L * 1024L * 1024L

data class GitHubInstallSessionWriteResult(
    val bytesWritten: Long,
    val totalBytes: Long,
    val archiveInfo: GitHubApkArchiveInfo = GitHubApkArchiveInfo(),
)

class GitHubInstallSessionWriter(
    private val client: OkHttpClient,
    balancedDownloadDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
    foregroundBoostDownloadDispatcher: CoroutineDispatcher = AppDispatchers.githubManagedDownload,
) {
    private val balancedDownloadClient =
        SegmentedDownloadClient(
            client = client,
            dispatcher = balancedDownloadDispatcher,
        )
    private val foregroundBoostDownloadClient =
        SegmentedDownloadClient(
            client = client,
            dispatcher = foregroundBoostDownloadDispatcher,
        )

    suspend fun streamApkIntoSession(
        context: Context,
        resolvedUrl: String,
        asset: GitHubReleaseAssetFile,
        downloadSpeedProfile: SegmentedDownloadSpeedProfile,
        sink: GitHubInstallApkSink,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult =
        if (asset.isPotentialNestedApkArchive()) {
            streamApkArchiveIntoSession(
                context = context,
                resolvedUrl = resolvedUrl,
                assetName = asset.name,
                expectedDigest = asset.digest,
                declaredSizeBytes = asset.sizeBytes,
                downloadSpeedProfile = downloadSpeedProfile,
                sink = sink,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        } else {
            streamDirectApkIntoSession(
                context = context,
                resolvedUrl = resolvedUrl,
                asset = asset,
                downloadSpeedProfile = downloadSpeedProfile,
                sink = sink,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        }

    private suspend fun streamDirectApkIntoSession(
        context: Context,
        resolvedUrl: String,
        asset: GitHubReleaseAssetFile,
        downloadSpeedProfile: SegmentedDownloadSpeedProfile,
        sink: GitHubInstallApkSink,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val tempApkFile = createGitHubTempApkFile(context, asset.name)
        try {
            downloadToTempFile(
                resolvedUrl = resolvedUrl,
                expectedDigest = asset.digest,
                declaredSizeBytes = asset.sizeBytes,
                outputFile = tempApkFile,
                acceptHeader = "application/vnd.android.package-archive, application/octet-stream;q=0.9, */*;q=0.1",
                downloadSpeedProfile = downloadSpeedProfile,
                sessionId = sessionId,
                onProgress = onProgress,
            )
            return streamDownloadedApkFileIntoSession(
                context = context,
                apkFile = tempApkFile,
                sessionName = asset.name.toGitHubApkSessionName(),
                sink = sink,
                sessionId = sessionId,
                onProgress = onProgress,
            )
        } finally {
            runCatching { tempApkFile.delete() }
        }
    }

    private suspend fun streamApkArchiveIntoSession(
        context: Context,
        resolvedUrl: String,
        assetName: String,
        expectedDigest: String,
        declaredSizeBytes: Long,
        downloadSpeedProfile: SegmentedDownloadSpeedProfile,
        sink: GitHubInstallApkSink,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubInstallSessionWriteResult {
        val archiveFile = createGitHubTempInstallFile(context, assetName, ".zip")
        try {
            downloadToTempFile(
                resolvedUrl = resolvedUrl,
                expectedDigest = expectedDigest,
                declaredSizeBytes = declaredSizeBytes,
                outputFile = archiveFile,
                acceptHeader = "application/zip, application/octet-stream;q=0.9, */*;q=0.1",
                downloadSpeedProfile = downloadSpeedProfile,
                sessionId = sessionId,
                onProgress = onProgress,
            )
            ZipFile(archiveFile).use { zipFile ->
                if (zipFile.isGitHubDirectApkArchive()) {
                    return streamDownloadedApkFileIntoSession(
                        context = context,
                        apkFile = archiveFile,
                        sessionName = assetName.toGitHubApkSessionName(),
                        sink = sink,
                        sessionId = sessionId,
                        onProgress = onProgress,
                    )
                }
                val apkEntry =
                    selectGitHubInstallApkEntry(zipFile)
                        ?: throw IOException("APK archive contains no installable APK")
                return streamZipEntryIntoSession(
                    context = context,
                    zipFile = zipFile,
                    apkEntry = apkEntry,
                    sink = sink,
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
        sink: GitHubInstallApkSink,
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
                sink.openWrite(sessionName, 0, entrySize).use { output ->
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
                    sink.fsync(output)
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
        sink: GitHubInstallApkSink,
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
            sink.openWrite(sessionName, 0, totalBytes).use { output ->
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
                sink.fsync(output)
                return GitHubInstallSessionWriteResult(progress.totalRead, totalBytes, archiveInfo)
            }
        }
    }

    private suspend fun downloadToTempFile(
        resolvedUrl: String,
        expectedDigest: String,
        declaredSizeBytes: Long,
        outputFile: File,
        acceptHeader: String,
        downloadSpeedProfile: SegmentedDownloadSpeedProfile,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ) {
        val downloadClient = segmentedDownloadClient(downloadSpeedProfile)
        val result =
            try {
                downloadClient.downloadToFile(
                    request =
                        buildSegmentedDownloadRequest(
                            resolvedUrl = resolvedUrl,
                            expectedDigest = expectedDigest,
                            expectedSizeBytes = declaredSizeBytes,
                            outputFile = outputFile,
                            acceptHeader = acceptHeader,
                        ),
                    options = githubSegmentedDownloadOptions(downloadSpeedProfile),
                    onProgress = { progress ->
                        emitDownloadProgress(
                            progress = progress,
                            declaredSizeBytes = declaredSizeBytes,
                            sessionId = sessionId,
                            onProgress = onProgress,
                        )
                    },
                )
            } catch (error: DownloadSizeMismatchException) {
                AppLogger.w(
                    GITHUB_INSTALL_SESSION_WRITER_TAG,
                    "asset size metadata mismatch expected=${error.expectedBytes} " +
                        "actual=${error.actualBytes} file=${outputFile.name}; retrying with probed size",
                )
                downloadClient.downloadToFile(
                    request =
                        buildSegmentedDownloadRequest(
                            resolvedUrl = resolvedUrl,
                            expectedDigest = expectedDigest,
                            expectedSizeBytes = -1L,
                            outputFile = outputFile,
                            acceptHeader = acceptHeader,
                        ),
                    options = githubSegmentedDownloadOptions(downloadSpeedProfile),
                    onProgress = { progress ->
                        emitDownloadProgress(
                            progress = progress,
                            declaredSizeBytes = error.actualBytes,
                            sessionId = sessionId,
                            onProgress = onProgress,
                        )
                    },
                )
            }
        AppLogger.i(GITHUB_INSTALL_SESSION_WRITER_TAG) {
            "asset downloaded profile=${downloadSpeedProfile.name} parallel=${result.parallel} " +
                "range=${result.rangeSupported} " +
                "bytes=${result.totalBytes} workers=${result.workerConnections} " +
                "connection=${result.connectionStrategy.name} " +
                "peak=${result.peakActiveConnections} retry=${result.retryCount} steal=${result.stealCount} " +
                "handoff=${result.handoffCount} fallback=${result.fallbackReason.orEmpty()}"
        }
    }

    private fun buildSegmentedDownloadRequest(
        resolvedUrl: String,
        expectedDigest: String,
        expectedSizeBytes: Long,
        outputFile: File,
        acceptHeader: String,
    ): SegmentedDownloadRequest =
        SegmentedDownloadRequest(
            url = resolvedUrl,
            outputFile = outputFile,
            headers = mapOf(
                "User-Agent" to "KeiOS-App/1.0 (Android)",
                "Accept" to acceptHeader,
            ),
            fileNameHint = outputFile.name,
            expectedSizeBytes = expectedSizeBytes.takeIf { it > 0L } ?: -1L,
            expectedSha256 = expectedDigest,
        )

    private suspend fun emitDownloadProgress(
        progress: SegmentedDownloadProgress,
        declaredSizeBytes: Long,
        sessionId: Int,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ) {
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
    }

    private fun segmentedDownloadClient(
        speedProfile: SegmentedDownloadSpeedProfile,
    ): SegmentedDownloadClient =
        when (speedProfile) {
            SegmentedDownloadSpeedProfile.Balanced -> balancedDownloadClient
            SegmentedDownloadSpeedProfile.ForegroundBoost -> foregroundBoostDownloadClient
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

internal fun githubSegmentedDownloadOptions(
    speedProfile: SegmentedDownloadSpeedProfile,
): SegmentedDownloadOptions =
    when (speedProfile) {
        SegmentedDownloadSpeedProfile.Balanced ->
            SegmentedDownloadOptions(
                minParallelSizeBytes = GITHUB_SEGMENTED_MIN_SIZE_BYTES,
                initialPartSizeBytes = GITHUB_SEGMENTED_PART_SIZE_BYTES,
                minBytesPerConnection = GITHUB_MIN_BYTES_PER_CONNECTION,
                maxConnections = GITHUB_SEGMENTED_MAX_CONNECTIONS,
                maxRetriesPerPart = 3,
                retryDelayMs = 1_000L,
                progressIntervalMs = GITHUB_DOWNLOAD_PROGRESS_INTERVAL_MS,
                requireHttpsForParallel = true,
                bufferSizeBytes = GITHUB_APK_STREAM_BUFFER_SIZE,
                speedProfile = speedProfile,
                connectionStrategy = SegmentedDownloadConnectionStrategy.Adaptive,
            )

        SegmentedDownloadSpeedProfile.ForegroundBoost ->
            SegmentedDownloadOptions(
                minParallelSizeBytes = GITHUB_FOREGROUND_BOOST_MIN_SIZE_BYTES,
                initialPartSizeBytes = GITHUB_FOREGROUND_BOOST_PART_SIZE_BYTES,
                minBytesPerConnection = GITHUB_MIN_BYTES_PER_CONNECTION,
                maxConnections = GITHUB_FOREGROUND_BOOST_MAX_CONNECTIONS,
                maxRetriesPerPart = 3,
                retryDelayMs = 1_000L,
                progressIntervalMs = GITHUB_DOWNLOAD_PROGRESS_INTERVAL_MS,
                requireHttpsForParallel = true,
                bufferSizeBytes = GITHUB_APK_STREAM_BUFFER_SIZE,
                speedProfile = speedProfile,
                connectionStrategy = SegmentedDownloadConnectionStrategy.Adaptive,
            )
    }
