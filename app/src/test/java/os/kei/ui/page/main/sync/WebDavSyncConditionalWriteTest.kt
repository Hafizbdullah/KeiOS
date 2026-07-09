package os.kei.ui.page.main.sync

import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavUploadResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavSyncConditionalWriteTest {
    @Test
    fun `auto merge conflict refuses refreshed remote without etag`() = runBlocking {
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults =
                    mutableListOf(
                        WebDavDownloadResult.Success("remote-new", etag = null),
                    ),
                uploadResults = mutableListOf(WebDavUploadResult.Conflict),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port =
            FakeWebDavSyncDataPort(
                localJson = "local-new",
                mergeRemoteOnAutoConflict = true,
            )

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = "etag-old",
                    expectedRemoteHash = WebDavSyncEngine.contentHash("remote-old"),
                )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(listOf("etag-old"), client.uploadCalls.map { it.etag })
        assertEquals(listOf("remote-new"), port.mergeCalls)
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }

    @Test
    fun `optimistic remerge refuses latest remote without etag`() = runBlocking {
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults =
                    mutableListOf(
                        WebDavDownloadResult.Success("remote-1", "etag-1"),
                        WebDavDownloadResult.Success("remote-2", etag = null),
                    ),
                uploadResults =
                    mutableListOf(
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Conflict,
                    ),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port =
            FakeWebDavSyncDataPort(
                localJson = "local-new",
                mergeRemoteOnAutoConflict = true,
            )

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = "etag-old",
                    expectedRemoteHash = WebDavSyncEngine.contentHash("remote-old"),
                )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(listOf("etag-old", "etag-1"), client.uploadCalls.map { it.etag })
        assertEquals(listOf("remote-1", "remote-2"), port.mergeCalls)
    }

    @Test
    fun `hash fallback refuses conditional write when server omits etag`() = runBlocking {
        val remote = WebDavDownloadResult.Success("remote-old", etag = null)
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults = mutableListOf(remote, remote),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort(localJson = "local-new")

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = null,
                    expectedRemoteHash = WebDavSyncEngine.contentHash("remote-old"),
                )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertTrue(client.uploadCalls.isEmpty())
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }
}
