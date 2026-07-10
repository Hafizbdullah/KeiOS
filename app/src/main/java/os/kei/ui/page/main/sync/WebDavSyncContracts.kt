package os.kei.ui.page.main.sync

import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavSyncClient
import os.kei.feature.webdav.client.WebDavTestConnectionResult
import os.kei.feature.webdav.client.WebDavUploadResult

internal interface WebDavSyncClientBridge {
    suspend fun testConnection(): WebDavTestConnectionResult
    suspend fun upload(fileName: String, content: String, etag: String? = null): WebDavUploadResult
    suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult
    suspend fun download(fileName: String): WebDavDownloadResult
    fun close() = Unit
}

internal class RealWebDavSyncClientBridge(
    private val delegate: WebDavSyncClient,
) : WebDavSyncClientBridge {
    override suspend fun testConnection(): WebDavTestConnectionResult = delegate.testConnection()

    override suspend fun upload(
        fileName: String,
        content: String,
        etag: String?,
    ): WebDavUploadResult = delegate.upload(fileName, content, etag)

    override suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult =
        delegate.uploadIfAbsent(fileName, content)

    override suspend fun download(fileName: String): WebDavDownloadResult =
        delegate.download(fileName)

    override fun close() {
        delegate.close()
    }
}

internal interface WebDavSyncMetadataStore {
    fun setItemEtag(item: WebDavSyncItem, etag: String?)
    fun setItemContentHash(item: WebDavSyncItem, hash: String)
    fun setLastSyncTime(item: WebDavSyncItem, timeMs: Long)
    fun setItemPendingState(
        item: WebDavSyncItem,
        state: WebDavSyncPendingState,
        updatedAtMs: Long,
    )
    fun clearItemPendingState(item: WebDavSyncItem)
    fun saveRemoteSummaryFound(
        item: WebDavSyncItem,
        itemCount: Int,
        byteSize: Long,
        etag: String?,
        probedAtMs: Long,
    )

    fun saveRemoteSummaryEmpty(item: WebDavSyncItem, probedAtMs: Long)
}

internal object StoreBackedWebDavSyncMetadataStore : WebDavSyncMetadataStore {
    override fun setItemEtag(item: WebDavSyncItem, etag: String?) {
        WebDavSyncStore.setItemEtag(item, etag)
    }

    override fun setItemContentHash(item: WebDavSyncItem, hash: String) {
        WebDavSyncStore.setItemContentHash(item, hash)
    }

    override fun setLastSyncTime(item: WebDavSyncItem, timeMs: Long) {
        WebDavSyncStore.setLastSyncTime(item, timeMs)
    }

    override fun setItemPendingState(
        item: WebDavSyncItem,
        state: WebDavSyncPendingState,
        updatedAtMs: Long,
    ) {
        WebDavSyncStore.setItemPendingState(item, state, updatedAtMs)
    }

    override fun clearItemPendingState(item: WebDavSyncItem) {
        WebDavSyncStore.clearItemPendingState(item)
    }

    override fun saveRemoteSummaryFound(
        item: WebDavSyncItem,
        itemCount: Int,
        byteSize: Long,
        etag: String?,
        probedAtMs: Long,
    ) {
        WebDavSyncStore.saveRemoteSummaryFound(item, itemCount, byteSize, etag, probedAtMs)
    }

    override fun saveRemoteSummaryEmpty(item: WebDavSyncItem, probedAtMs: Long) {
        WebDavSyncStore.saveRemoteSummaryEmpty(item, probedAtMs)
    }
}

internal enum class WebDavConnectionStatus {
    Success,
    SuccessDirCreated,
    AuthFailed,
    PermissionDenied,
    NetworkError,
    InvalidUrl,
    Unknown,
}

internal data class WebDavConnectionOutcome(
    val status: WebDavConnectionStatus,
    val detail: String? = null,
) {
    val isSuccess: Boolean
        get() = status == WebDavConnectionStatus.Success || status == WebDavConnectionStatus.SuccessDirCreated
}

internal enum class WebDavItemStatus {
    Uploaded,
    Downloaded,
    Merged,
    UpToDate,
    RemoteEmpty,
    AuthFailed,
    PermissionDenied,
    NetworkError,
    ConflictUnresolved,
    BaselineRequired,
    Error,
}

internal data class WebDavItemOutcome(
    val status: WebDavItemStatus,
    val detail: String? = null,
) {
    val isSuccess: Boolean
        get() = when (status) {
            WebDavItemStatus.Uploaded,
            WebDavItemStatus.Downloaded,
            WebDavItemStatus.Merged,
            WebDavItemStatus.UpToDate,
            WebDavItemStatus.RemoteEmpty,
            -> true
            else -> false
        }
}

/** Read-only remote payload summary used by the sync UI. */
internal sealed interface WebDavRemoteProbeOutcome {
    data class Found(
        val itemCount: Int,
        val byteSize: Long,
        val etag: String?,
    ) : WebDavRemoteProbeOutcome
    data object Empty : WebDavRemoteProbeOutcome
    data class Error(val status: WebDavItemStatus, val detail: String?) : WebDavRemoteProbeOutcome
}

/** Bridge between a sync item and its domain store. */
internal data class WebDavSyncDataPort(
    val exportJson: () -> String,
    val merge: (remoteJson: String) -> Unit,
    val localCount: () -> Int,
    val countRemoteItems: (raw: String) -> Int,
    val fingerprintJson: () -> String = exportJson,
    val remoteFingerprintJson: (raw: String) -> String = { it },
    val mergeRemoteOnAutoConflict: Boolean = false,
    val fingerprintRevision: Int = 1,
)
