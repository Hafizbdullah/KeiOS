package os.kei.ui.page.main.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.core.log.AppLogger
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavError
import os.kei.feature.webdav.client.WebDavSyncClient
import os.kei.feature.webdav.client.WebDavTestConnectionResult
import os.kei.feature.webdav.client.WebDavUploadResult
import os.kei.feature.webdav.model.WebDavConfig
import java.security.MessageDigest

/**
 * Coordinates upload / download / two-way sync for each [WebDavSyncItem].
 *
 * Holds a single [WebDavSyncClient] that is rebuilt only when the connection config changes, so
 * repeated calls reuse the same HTTP client and auth state. All domain export/import logic is
 * injected via [WebDavSyncDataPort] — the engine never touches a concrete domain store.
 *
 * Every method returns a string-free [WebDavItemOutcome] / [WebDavConnectionOutcome]; turning
 * those into user-facing text is the UI layer's job, which keeps this engine usable from
 * background (non-Compose) callers such as the auto-sync coordinator.
 */
internal class WebDavSyncEngine(
    private val clientFactory: (WebDavConfig) -> WebDavSyncClientBridge = {
        RealWebDavSyncClientBridge(WebDavSyncClient(it))
    },
    private val metadataStore: WebDavSyncMetadataStore = StoreBackedWebDavSyncMetadataStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cachedConfig: WebDavConfig? = null
    private var cachedClient: WebDavSyncClientBridge? = null

    private suspend fun client(config: WebDavConfig): WebDavSyncClientBridge = mutex.withLock {
        val existing = cachedClient
        if (existing != null && cachedConfig == config) {
            existing
        } else {
            existing?.close()
            clientFactory(config).also {
                cachedClient = it
                cachedConfig = config
            }
        }
    }

    /** Drop the cached client (e.g. after the user clears or rewrites the config). */
    fun invalidate() {
        cachedClient?.close()
        cachedClient = null
        cachedConfig = null
    }

    fun recordCurrentLocalAsSynced(
        item: WebDavSyncItem,
        etag: String?,
        port: WebDavSyncDataPort,
    ) {
        val local = port.exportJson()
        recordSynced(item, etag, fingerprintSnapshot(port, local))
        saveRemoteSummaryFromLocal(
            item = item,
            port = port,
            content = local,
            etag = etag,
        )
    }

    // ── Connection test ────────────────────────────────────────────────

    suspend fun testConnection(config: WebDavConfig): WebDavConnectionOutcome =
        when (val result = client(config).testConnection()) {
            is WebDavTestConnectionResult.Success ->
                WebDavConnectionOutcome(
                    if (result.dirCreated) {
                        WebDavConnectionStatus.SuccessDirCreated
                    } else {
                        WebDavConnectionStatus.Success
                    },
                )
            WebDavTestConnectionResult.AuthFailed ->
                WebDavConnectionOutcome(WebDavConnectionStatus.AuthFailed)
            WebDavTestConnectionResult.PermissionDenied ->
                WebDavConnectionOutcome(WebDavConnectionStatus.PermissionDenied)
            is WebDavTestConnectionResult.NetworkError ->
                WebDavConnectionOutcome(WebDavConnectionStatus.NetworkError, result.message)
            is WebDavTestConnectionResult.InvalidUrl ->
                WebDavConnectionOutcome(WebDavConnectionStatus.InvalidUrl, result.message)
            is WebDavTestConnectionResult.Error ->
                WebDavConnectionOutcome(WebDavConnectionStatus.Unknown, result.message)
        }

    // ── Two-way sync (pull → merge → push) ─────────────────────────────

    /**
     * Read the current remote payload and build a user-confirmable change plan for one item.
     * This method never mutates domain data; it only updates the remote summary cache so the UI
     * and the eventual execution path share the same fresh remote baseline.
     */
    suspend fun prepareChange(
        config: WebDavConfig,
        kind: WebDavBatchKind,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavSyncPlanItem {
        val c = client(config)
        val local = runCatching { port.fingerprintJson() }.getOrElse { "" }
        val localHash = contentHash(local)
        val localCount = runCatching { port.localCount() }.getOrDefault(-1)
        return try {
            val nowMs = nowMillis()
            when (val download = c.download(item.fileName)) {
                is WebDavDownloadResult.Success -> {
                    val remoteHash = contentHash(port.remoteFingerprintJson(download.content))
                    val remoteCount = runCatching { port.countRemoteItems(download.content) }
                        .getOrDefault(-1)
                    val byteSize = download.content.toByteArray(Charsets.UTF_8).size.toLong()
                    metadataStore.saveRemoteSummaryFound(
                        item = item,
                        itemCount = remoteCount,
                        byteSize = byteSize,
                        etag = download.etag,
                        probedAtMs = nowMs,
                    )
                    WebDavSyncPlanItem(
                        item = item,
                        localCount = localCount,
                        localHash = localHash,
                        remoteState =
                            WebDavSyncPlanRemoteState.Found(
                                itemCount = remoteCount,
                                byteSize = byteSize,
                                etag = download.etag,
                                contentHash = remoteHash,
                            ),
                        effect =
                            when {
                                localHash == remoteHash -> WebDavSyncPlanEffect.NoChange
                                kind == WebDavBatchKind.Upload -> WebDavSyncPlanEffect.UploadOverwrite
                                kind == WebDavBatchKind.Download -> WebDavSyncPlanEffect.DownloadMerge
                                else -> WebDavSyncPlanEffect.MergeThenUpload
                            },
                    )
                }

                WebDavDownloadResult.Empty -> {
                    metadataStore.saveRemoteSummaryEmpty(item, nowMs)
                    WebDavSyncPlanItem(
                        item = item,
                        localCount = localCount,
                        localHash = localHash,
                        remoteState = WebDavSyncPlanRemoteState.Empty,
                        effect =
                            when (kind) {
                                WebDavBatchKind.Sync,
                                WebDavBatchKind.Upload -> WebDavSyncPlanEffect.CreateRemote

                                WebDavBatchKind.Download -> WebDavSyncPlanEffect.RemoteEmpty
                            },
                    )
                }

                is WebDavDownloadResult.Error ->
                    WebDavSyncPlanItem(
                        item = item,
                        localCount = localCount,
                        localHash = localHash,
                        remoteState = download.error.toPlanRemoteError(),
                        effect = WebDavSyncPlanEffect.Error,
                    )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "prepareChange ${item.name} failed", e)
            WebDavSyncPlanItem(
                item = item,
                localCount = localCount,
                localHash = localHash,
                remoteState =
                    WebDavSyncPlanRemoteState.Error(
                        status = WebDavItemStatus.Error,
                        detail = e.message,
                    ),
                effect = WebDavSyncPlanEffect.Error,
            )
        }
    }

    /**
     * Reconcile a single item with the remote: pull the remote copy, merge it into local, then
     * push the merged local copy back. Updates the persisted ETag + content hash + last-sync time
     * on success.
     */
    suspend fun sync(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome = WebDavSyncOperationCoordinator.run {
        syncInternal(config, item, port)
    }

    private suspend fun syncInternal(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome {
        val c = client(config)
        return try {
            when (val download = c.download(item.fileName)) {
                is WebDavDownloadResult.Success -> {
                    saveRemoteSummaryFromRemote(item, port, download.content, download.etag)
                    val localBeforeHash = contentHash(port.fingerprintJson())
                    val remoteHash = contentHash(port.remoteFingerprintJson(download.content))
                    if (localBeforeHash == remoteHash) {
                        recordSynced(item, download.etag, port.fingerprintJson())
                        return WebDavItemOutcome(WebDavItemStatus.UpToDate)
                    }
                    // Remote exists → merge it locally, then push the merged result up.
                    port.merge(download.content)
                    val merged = port.exportJson()
                    pushMerged(c, item, port, merged, download.etag)
                }
                WebDavDownloadResult.Empty -> {
                    // No remote copy yet → first push of local data.
                    val local = port.exportJson()
                    pushMerged(
                        c = c,
                        item = item,
                        port = port,
                        content = local,
                        etag = null,
                        remoteKnownEmpty = true,
                        statusWhenWritten = WebDavItemStatus.Uploaded,
                    )
                }
                is WebDavDownloadResult.Error -> errorOutcome(download.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "sync ${item.name} failed", e)
            WebDavItemOutcome(WebDavItemStatus.Error, e.message)
        }
    }

    private suspend fun pushMerged(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        content: String,
        etag: String?,
        remoteKnownEmpty: Boolean = false,
        statusWhenWritten: WebDavItemStatus = WebDavItemStatus.Merged,
    ): WebDavItemOutcome =
        when (
            val upload =
                if (remoteKnownEmpty) {
                    c.uploadIfAbsent(item.fileName, content)
                } else {
                    uploadAgainstRemoteSnapshot(
                        c = c,
                        item = item,
                        content = content,
                        etag = etag,
                        allowUnconditionalFallback = port.mergeRemoteOnAutoConflict,
                    )
                }
        ) {
            is WebDavUploadResult.Success -> {
                recordUploadedSnapshot(item, upload.etag, port, content)
                saveRemoteSummaryFromLocal(item, port, content, upload.etag)
                WebDavItemOutcome(statusWhenWritten)
            }
            WebDavUploadResult.Conflict -> {
                // Remote moved under us: pull again, re-merge, and retry against the refreshed ETag.
                when (val retry = c.download(item.fileName)) {
                    is WebDavDownloadResult.Success -> {
                        port.merge(retry.content)
                        val reMerged = port.exportJson()
                        when (
                            val second =
                                uploadAgainstRemoteSnapshot(
                                    c = c,
                                    item = item,
                                    content = reMerged,
                                    etag = retry.etag,
                                    allowUnconditionalFallback = port.mergeRemoteOnAutoConflict,
                                )
                        ) {
                            is WebDavUploadResult.Success -> {
                                recordUploadedSnapshot(item, second.etag, port, reMerged)
                                saveRemoteSummaryFromLocal(item, port, reMerged, second.etag)
                                WebDavItemOutcome(WebDavItemStatus.Merged)
                            }
                            WebDavUploadResult.Conflict ->
                                if (port.mergeRemoteOnAutoConflict) {
                                    resolveMergeConflictOptimistically(
                                        c = c,
                                        item = item,
                                        port = port,
                                        remainingAttempts = MAX_OPTIMISTIC_MERGE_ATTEMPTS - 1,
                                        lastRejectedStrongEtag = usableStrongEtag(retry.etag),
                                    )
                                } else {
                                    conflictOutcome(item)
                                }
                            is WebDavUploadResult.Error -> errorOutcome(second.error)
                        }
                    }
                    else -> conflictOutcome(item)
                }
            }
            is WebDavUploadResult.Error -> errorOutcome(upload.error)
        }

    /**
     * Push local changes detected by auto-sync without pulling remote content into local first.
     * The previous remote ETag/hash is used as the baseline. If the remote moved, local data is
     * preserved; lossless ports re-merge with conditional writes, while other ports request review.
     */
    suspend fun uploadLocalChange(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        expectedRemoteEtag: String?,
        expectedRemoteHash: String?,
    ): WebDavItemOutcome = WebDavSyncOperationCoordinator.run {
        uploadLocalChangeInternal(
            config = config,
            item = item,
            port = port,
            expectedRemoteEtag = expectedRemoteEtag,
            expectedRemoteHash = expectedRemoteHash,
        )
    }

    private suspend fun uploadLocalChangeInternal(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        expectedRemoteEtag: String?,
        expectedRemoteHash: String?,
    ): WebDavItemOutcome {
        val c = client(config)
        return try {
            val local = port.exportJson()
            if (expectedRemoteEtag == null && expectedRemoteHash == null) {
                metadataStore.setItemPendingState(
                    item = item,
                    state = WebDavSyncPendingState.BaselineRequired,
                    updatedAtMs = nowMillis(),
                )
                return WebDavItemOutcome(WebDavItemStatus.BaselineRequired)
            }
            val expectedStrongEtag = usableStrongEtag(expectedRemoteEtag)
            val uploadResult =
                if (expectedStrongEtag != null) {
                    c.upload(item.fileName, local, etag = expectedStrongEtag)
                } else {
                    uploadLocalChangeWithoutEtag(
                        c = c,
                        item = item,
                        port = port,
                        local = local,
                        expectedRemoteHash = expectedRemoteHash,
                    )
                }
            when (val upload = uploadResult) {
                is WebDavUploadResult.Success -> {
                    recordUploadedSnapshot(item, upload.etag, port, local)
                    saveRemoteSummaryFromLocal(item, port, local, upload.etag)
                    WebDavItemOutcome(WebDavItemStatus.Uploaded)
                }
                WebDavUploadResult.Conflict ->
                    retryUploadLocalChangeAfterConflict(
                        c = c,
                        item = item,
                        port = port,
                        local = local,
                        expectedRemoteHash = expectedRemoteHash,
                    )
                is WebDavUploadResult.Error -> errorOutcome(upload.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "uploadLocalChange ${item.name} failed", e)
            WebDavItemOutcome(WebDavItemStatus.Error, e.message)
        }
    }

    private suspend fun uploadLocalChangeWithoutEtag(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        local: String,
        expectedRemoteHash: String?,
    ): WebDavUploadResult {
        if (expectedRemoteHash == null) return WebDavUploadResult.Conflict
        return when (val remote = c.download(item.fileName)) {
            WebDavDownloadResult.Empty -> c.uploadIfAbsent(item.fileName, local)
            is WebDavDownloadResult.Success -> {
                val remoteHash = contentHash(port.remoteFingerprintJson(remote.content))
                if (remoteHash != expectedRemoteHash) {
                    WebDavUploadResult.Conflict
                } else {
                    uploadAgainstRemoteSnapshot(
                        c = c,
                        item = item,
                        content = local,
                        etag = remote.etag,
                        allowUnconditionalFallback = port.mergeRemoteOnAutoConflict,
                    )
                }
            }
            is WebDavDownloadResult.Error -> WebDavUploadResult.Error(remote.error)
        }
    }

    private suspend fun retryUploadLocalChangeAfterConflict(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        local: String,
        expectedRemoteHash: String?,
    ): WebDavItemOutcome =
        when (val refreshed = c.download(item.fileName)) {
            WebDavDownloadResult.Empty -> {
                metadataStore.saveRemoteSummaryEmpty(item, nowMillis())
                when (val retryUpload = c.uploadIfAbsent(item.fileName, local)) {
                    is WebDavUploadResult.Success -> {
                        recordUploadedSnapshot(item, retryUpload.etag, port, local)
                        saveRemoteSummaryFromLocal(item, port, local, retryUpload.etag)
                        WebDavItemOutcome(WebDavItemStatus.Uploaded)
                    }
                    WebDavUploadResult.Conflict -> conflictOutcome(item)
                    is WebDavUploadResult.Error -> errorOutcome(retryUpload.error)
                }
            }
            is WebDavDownloadResult.Success -> {
                saveRemoteSummaryFromRemote(item, port, refreshed.content, refreshed.etag)
                val localFingerprint = fingerprintSnapshot(port, local)
                val localHash = contentHash(localFingerprint)
                val refreshedRemoteHash = contentHash(port.remoteFingerprintJson(refreshed.content))
                when {
                    refreshedRemoteHash == localHash -> {
                        recordSynced(item, refreshed.etag, localFingerprint)
                        WebDavItemOutcome(WebDavItemStatus.UpToDate)
                    }
                    expectedRemoteHash != null && refreshedRemoteHash == expectedRemoteHash -> {
                        when (
                            val retryUpload =
                                uploadAgainstRemoteSnapshot(
                                    c = c,
                                    item = item,
                                    content = local,
                                    etag = refreshed.etag,
                                    allowUnconditionalFallback = port.mergeRemoteOnAutoConflict,
                                )
                        ) {
                            is WebDavUploadResult.Success -> {
                                recordUploadedSnapshot(item, retryUpload.etag, port, local)
                                saveRemoteSummaryFromLocal(item, port, local, retryUpload.etag)
                                WebDavItemOutcome(WebDavItemStatus.Uploaded)
                            }
                            WebDavUploadResult.Conflict ->
                                if (port.mergeRemoteOnAutoConflict) {
                                    resolveMergeConflictOptimistically(
                                        c = c,
                                        item = item,
                                        port = port,
                                        remainingAttempts = MAX_OPTIMISTIC_MERGE_ATTEMPTS - 1,
                                        lastRejectedStrongEtag = usableStrongEtag(refreshed.etag),
                                    )
                                } else {
                                    conflictOutcome(item)
                                }
                            is WebDavUploadResult.Error -> errorOutcome(retryUpload.error)
                        }
                    }
                    port.mergeRemoteOnAutoConflict -> {
                        port.merge(refreshed.content)
                        val merged = port.exportJson()
                        val mergedFingerprint = fingerprintSnapshot(port, merged)
                        val mergedHash = contentHash(mergedFingerprint)
                        if (mergedHash == refreshedRemoteHash) {
                            recordSynced(item, refreshed.etag, mergedFingerprint)
                            WebDavItemOutcome(WebDavItemStatus.UpToDate)
                        } else {
                            when (
                                val retryUpload =
                                    uploadAgainstRemoteSnapshot(
                                        c = c,
                                        item = item,
                                        content = merged,
                                        etag = refreshed.etag,
                                        allowUnconditionalFallback = true,
                                    )
                            ) {
                                is WebDavUploadResult.Success -> {
                                    recordUploadedSnapshot(item, retryUpload.etag, port, merged)
                                    saveRemoteSummaryFromLocal(item, port, merged, retryUpload.etag)
                                    WebDavItemOutcome(WebDavItemStatus.Merged)
                                }
                                WebDavUploadResult.Conflict ->
                                    resolveMergeConflictOptimistically(
                                        c = c,
                                        item = item,
                                        port = port,
                                        remainingAttempts = MAX_OPTIMISTIC_MERGE_ATTEMPTS - 1,
                                        lastRejectedStrongEtag = usableStrongEtag(refreshed.etag),
                                    )
                                is WebDavUploadResult.Error -> errorOutcome(retryUpload.error)
                            }
                        }
                    }
                    else -> conflictOutcome(item)
                }
            }
            is WebDavDownloadResult.Error -> errorOutcome(refreshed.error)
        }

    // ── Manual upload (push local → remote, overwrite) ─────────────────

    suspend fun upload(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        expectedRemoteEtag: String? = null,
        remoteKnownEmpty: Boolean = false,
        confirmedOverwrite: Boolean = false,
    ): WebDavItemOutcome = WebDavSyncOperationCoordinator.run {
        uploadInternal(
            config = config,
            item = item,
            port = port,
            expectedRemoteEtag = expectedRemoteEtag,
            remoteKnownEmpty = remoteKnownEmpty,
            confirmedOverwrite = confirmedOverwrite,
        )
    }

    private suspend fun uploadInternal(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        expectedRemoteEtag: String? = null,
        remoteKnownEmpty: Boolean = false,
        confirmedOverwrite: Boolean = false,
    ): WebDavItemOutcome {
        val c = client(config)
        return try {
            val local = port.exportJson()
            // Manual upload is a "local wins" action. When a confirmation plan has observed a
            // remote file, pass its ETag so concurrent remote changes produce Conflict instead
            // of a silent overwrite.
            val uploadResult =
                if (expectedRemoteEtag == null && remoteKnownEmpty) {
                    c.uploadIfAbsent(item.fileName, local)
                } else {
                    c.upload(item.fileName, local, etag = expectedRemoteEtag)
                }
            when (val upload = uploadResult) {
                is WebDavUploadResult.Success -> {
                    recordUploadedSnapshot(item, upload.etag, port, local)
                    saveRemoteSummaryFromLocal(item, port, local, upload.etag)
                    WebDavItemOutcome(WebDavItemStatus.Uploaded)
                }
                WebDavUploadResult.Conflict ->
                    if (confirmedOverwrite) {
                        refreshRemoteAndOverwrite(
                            c = c,
                            item = item,
                            port = port,
                            local = local,
                        )
                    } else {
                        conflictOutcome(item)
                    }
                is WebDavUploadResult.Error -> errorOutcome(upload.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "upload ${item.name} failed", e)
            WebDavItemOutcome(WebDavItemStatus.Error, e.message)
        }
    }

    // ── Manual download (pull remote → merge into local) ───────────────

    suspend fun download(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome = WebDavSyncOperationCoordinator.run {
        downloadInternal(config, item, port)
    }

    private suspend fun downloadInternal(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome {
        val c = client(config)
        return try {
            when (val download = c.download(item.fileName)) {
                is WebDavDownloadResult.Success -> {
                    saveRemoteSummaryFromRemote(item, port, download.content, download.etag)
                    val localBeforeHash = contentHash(port.fingerprintJson())
                    val remoteHash = contentHash(port.remoteFingerprintJson(download.content))
                    if (localBeforeHash == remoteHash) {
                        recordSynced(item, download.etag, port.fingerprintJson())
                        return WebDavItemOutcome(WebDavItemStatus.UpToDate)
                    }
                    port.merge(download.content)
                    // Re-export so the stored hash reflects the post-merge local state.
                    recordSynced(item, download.etag, port.fingerprintJson())
                    WebDavItemOutcome(WebDavItemStatus.Downloaded)
                }
                WebDavDownloadResult.Empty -> WebDavItemOutcome(WebDavItemStatus.RemoteEmpty)
                is WebDavDownloadResult.Error -> errorOutcome(download.error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "download ${item.name} failed", e)
            WebDavItemOutcome(WebDavItemStatus.Error, e.message)
        }
    }

    // ── Refresh remote summary (read-only) ─────────────────────────────

    /**
     * Fetch the remote payload for a single item and report what's on the server *without*
     * mutating local state. Used by the manual refresh action so other devices can see whether
     * the remote has data, how many items it holds, and how stale it is before choosing
     * Sync / Upload / Download.
     */
    suspend fun probeRemote(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavRemoteProbeOutcome {
        val c = client(config)
        return try {
            val nowMs = nowMillis()
            when (val download = c.download(item.fileName)) {
                is WebDavDownloadResult.Success -> {
                    val itemCount = runCatching { port.countRemoteItems(download.content) }
                        .getOrElse { -1 }
                    val byteSize = download.content.toByteArray(Charsets.UTF_8).size.toLong()
                    metadataStore.saveRemoteSummaryFound(
                        item = item,
                        itemCount = itemCount,
                        byteSize = byteSize,
                        etag = download.etag,
                        probedAtMs = nowMs,
                    )
                    WebDavRemoteProbeOutcome.Found(
                        itemCount = itemCount,
                        byteSize = byteSize,
                        etag = download.etag,
                    )
                }
                WebDavDownloadResult.Empty -> {
                    metadataStore.saveRemoteSummaryEmpty(item, nowMs)
                    WebDavRemoteProbeOutcome.Empty
                }
                is WebDavDownloadResult.Error -> WebDavRemoteProbeOutcome.Error(
                    when (download.error) {
                        WebDavError.AuthFailed -> WebDavItemStatus.AuthFailed
                        WebDavError.PermissionDenied -> WebDavItemStatus.PermissionDenied
                        WebDavError.NetworkUnreachable -> WebDavItemStatus.NetworkError
                        is WebDavError.Unknown -> WebDavItemStatus.Error
                    },
                    detail = (download.error as? WebDavError.Unknown)?.message,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "probeRemote ${item.name} failed", e)
            WebDavRemoteProbeOutcome.Error(WebDavItemStatus.Error, e.message)
        }
    }

    private fun recordSynced(item: WebDavSyncItem, etag: String?, content: String) {
        metadataStore.setItemEtag(item, etag)
        metadataStore.setItemContentHash(item, contentHash(content))
        metadataStore.setLastSyncTime(item, nowMillis())
        metadataStore.clearItemPendingState(item)
    }

    private suspend fun refreshRemoteAndOverwrite(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        local: String,
    ): WebDavItemOutcome =
        when (val refreshed = c.download(item.fileName)) {
            is WebDavDownloadResult.Success -> {
                saveRemoteSummaryFromRemote(item, port, refreshed.content, refreshed.etag)
                when (val second = c.upload(item.fileName, local, etag = null)) {
                    is WebDavUploadResult.Success -> {
                        recordUploadedSnapshot(item, second.etag, port, local)
                        saveRemoteSummaryFromLocal(item, port, local, second.etag)
                        WebDavItemOutcome(WebDavItemStatus.Uploaded)
                    }
                    WebDavUploadResult.Conflict -> conflictOutcome(item)
                    is WebDavUploadResult.Error -> errorOutcome(second.error)
                }
            }
            WebDavDownloadResult.Empty -> {
                metadataStore.saveRemoteSummaryEmpty(item, nowMillis())
                when (val second = c.upload(item.fileName, local, etag = null)) {
                    is WebDavUploadResult.Success -> {
                        recordUploadedSnapshot(item, second.etag, port, local)
                        saveRemoteSummaryFromLocal(item, port, local, second.etag)
                        WebDavItemOutcome(WebDavItemStatus.Uploaded)
                    }
                    WebDavUploadResult.Conflict -> conflictOutcome(item)
                    is WebDavUploadResult.Error -> errorOutcome(second.error)
                }
            }
            is WebDavDownloadResult.Error -> errorOutcome(refreshed.error)
        }

    private suspend fun resolveMergeConflictOptimistically(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        remainingAttempts: Int,
        lastRejectedStrongEtag: String?,
    ): WebDavItemOutcome {
        var rejectedStrongEtag = lastRejectedStrongEtag
        for (attempt in 1..remainingAttempts.coerceAtLeast(0)) {
            when (val latest = c.download(item.fileName)) {
                is WebDavDownloadResult.Success -> {
                    saveRemoteSummaryFromRemote(item, port, latest.content, latest.etag)
                    val latestStrongEtag = usableStrongEtag(latest.etag)
                    port.merge(latest.content)
                    val merged = port.exportJson()
                    val mergedFingerprint = fingerprintSnapshot(port, merged)
                    val latestRemoteHash = contentHash(port.remoteFingerprintJson(latest.content))
                    if (contentHash(mergedFingerprint) == latestRemoteHash) {
                        recordSynced(item, latest.etag, mergedFingerprint)
                        return WebDavItemOutcome(WebDavItemStatus.UpToDate)
                    }
                    if (latestStrongEtag != null && latestStrongEtag == rejectedStrongEtag) {
                        AppLogger.i(
                            TAG,
                            "${item.name} returned the same strong ETag after rejecting it; " +
                                "using merged overwrite fallback",
                        )
                        return writeMergedWithoutConditionalValidator(c, item, port, merged)
                    }
                    when (
                        val upload =
                            uploadAgainstRemoteSnapshot(
                                c = c,
                                item = item,
                                content = merged,
                                etag = latest.etag,
                                allowUnconditionalFallback = true,
                            )
                    ) {
                        is WebDavUploadResult.Success -> {
                            recordUploadedSnapshot(item, upload.etag, port, merged)
                            saveRemoteSummaryFromLocal(item, port, merged, upload.etag)
                            AppLogger.i(
                                TAG,
                                "optimistic merge resolved ${item.name} on attempt $attempt",
                            )
                            return WebDavItemOutcome(WebDavItemStatus.Merged)
                        }
                        WebDavUploadResult.Conflict -> {
                            rejectedStrongEtag = latestStrongEtag
                            AppLogger.i(
                                TAG,
                                "optimistic merge retry ${item.name} after ETag changed on attempt $attempt",
                            )
                        }
                        is WebDavUploadResult.Error -> return errorOutcome(upload.error)
                    }
                }
                WebDavDownloadResult.Empty -> {
                    metadataStore.saveRemoteSummaryEmpty(item, nowMillis())
                    val local = port.exportJson()
                    when (val upload = c.uploadIfAbsent(item.fileName, local)) {
                        is WebDavUploadResult.Success -> {
                            recordUploadedSnapshot(item, upload.etag, port, local)
                            saveRemoteSummaryFromLocal(item, port, local, upload.etag)
                            return WebDavItemOutcome(WebDavItemStatus.Uploaded)
                        }
                        WebDavUploadResult.Conflict -> {
                            AppLogger.i(
                                TAG,
                                "optimistic merge retry ${item.name} after remote file appeared on attempt $attempt",
                            )
                        }
                        is WebDavUploadResult.Error -> return errorOutcome(upload.error)
                    }
                }
                is WebDavDownloadResult.Error -> return errorOutcome(latest.error)
            }
        }
        AppLogger.i(
            TAG,
            "optimistic merge exhausted $MAX_OPTIMISTIC_MERGE_ATTEMPTS attempts for ${item.name}",
        )
        return conflictOutcome(item)
    }

    private fun recordUploadedSnapshot(
        item: WebDavSyncItem,
        etag: String?,
        port: WebDavSyncDataPort,
        uploadedContent: String,
    ) {
        recordSynced(item, etag, fingerprintSnapshot(port, uploadedContent))
    }

    private suspend fun uploadAgainstRemoteSnapshot(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        content: String,
        etag: String?,
        allowUnconditionalFallback: Boolean,
    ): WebDavUploadResult {
        val strongEtag = usableStrongEtag(etag)
        return when {
            strongEtag != null -> c.upload(item.fileName, content, etag = strongEtag)
            allowUnconditionalFallback -> {
                AppLogger.i(TAG, "${item.name} has no usable strong ETag; using merged overwrite fallback")
                c.upload(item.fileName, content, etag = null)
            }
            else -> WebDavUploadResult.Conflict
        }
    }

    private suspend fun writeMergedWithoutConditionalValidator(
        c: WebDavSyncClientBridge,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        merged: String,
    ): WebDavItemOutcome =
        when (val upload = c.upload(item.fileName, merged, etag = null)) {
            is WebDavUploadResult.Success -> {
                recordUploadedSnapshot(item, upload.etag, port, merged)
                saveRemoteSummaryFromLocal(item, port, merged, upload.etag)
                WebDavItemOutcome(WebDavItemStatus.Merged)
            }
            WebDavUploadResult.Conflict -> conflictOutcome(item)
            is WebDavUploadResult.Error -> errorOutcome(upload.error)
        }

    private fun usableStrongEtag(etag: String?): String? =
        etag
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() && !value.startsWith("W/", ignoreCase = true) }

    private fun fingerprintSnapshot(
        port: WebDavSyncDataPort,
        content: String,
    ): String =
        runCatching { port.remoteFingerprintJson(content) }
            .onFailure { error ->
                AppLogger.w(TAG, "failed to fingerprint uploaded WebDAV snapshot", error)
            }
            .getOrDefault(content)

    private fun saveRemoteSummaryFromRemote(
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        content: String,
        etag: String?,
    ) {
        metadataStore.saveRemoteSummaryFound(
            item = item,
            itemCount = runCatching { port.countRemoteItems(content) }.getOrDefault(-1),
            byteSize = content.toByteArray(Charsets.UTF_8).size.toLong(),
            etag = etag,
            probedAtMs = nowMillis(),
        )
    }

    private fun saveRemoteSummaryFromLocal(
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        content: String,
        etag: String?,
    ) {
        metadataStore.saveRemoteSummaryFound(
            item = item,
            itemCount = runCatching { port.localCount() }.getOrDefault(-1),
            byteSize = content.toByteArray(Charsets.UTF_8).size.toLong(),
            etag = etag,
            probedAtMs = nowMillis(),
        )
    }

    private fun conflictOutcome(item: WebDavSyncItem): WebDavItemOutcome {
        metadataStore.setItemPendingState(item, WebDavSyncPendingState.RemoteConflict, nowMillis())
        return WebDavItemOutcome(WebDavItemStatus.ConflictUnresolved)
    }

    private fun errorOutcome(error: WebDavError): WebDavItemOutcome = when (error) {
        WebDavError.NetworkUnreachable -> WebDavItemOutcome(WebDavItemStatus.NetworkError)
        WebDavError.AuthFailed -> WebDavItemOutcome(WebDavItemStatus.AuthFailed)
        WebDavError.PermissionDenied -> WebDavItemOutcome(WebDavItemStatus.PermissionDenied)
        is WebDavError.Unknown -> WebDavItemOutcome(WebDavItemStatus.Error, error.message)
    }

    private fun WebDavError.toPlanRemoteError(): WebDavSyncPlanRemoteState.Error = when (this) {
        WebDavError.NetworkUnreachable ->
            WebDavSyncPlanRemoteState.Error(WebDavItemStatus.NetworkError, null)

        WebDavError.AuthFailed ->
            WebDavSyncPlanRemoteState.Error(WebDavItemStatus.AuthFailed, null)

        WebDavError.PermissionDenied ->
            WebDavSyncPlanRemoteState.Error(WebDavItemStatus.PermissionDenied, null)

        is WebDavError.Unknown ->
            WebDavSyncPlanRemoteState.Error(WebDavItemStatus.Error, message)
    }

    companion object {
        private const val TAG = "WebDavSyncEngine"
        private const val MAX_OPTIMISTIC_MERGE_ATTEMPTS = 3

        /** Stable content fingerprint used to detect real local changes for auto-sync. */
        fun contentHash(content: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
