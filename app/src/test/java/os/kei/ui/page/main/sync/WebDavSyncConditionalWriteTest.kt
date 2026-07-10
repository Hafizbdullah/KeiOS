package os.kei.ui.page.main.sync

import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavUploadResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavSyncConditionalWriteTest {
    @Test
    fun `lossless auto upload uses hash guarded fallback for weak validator`() = runBlocking {
        val remoteFingerprint = """{"value":"remote-old"}"""
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults =
                    mutableListOf(
                        WebDavDownloadResult.Success(
                            """{"value":"remote-old","exportedAtMs":1}""",
                            etag = "W/\"etag-weak\"",
                        ),
                    ),
                uploadResults = mutableListOf(WebDavUploadResult.Success("W/\"etag-new\"")),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port =
            FakeWebDavSyncDataPort(
                localJson = """{"value":"local-new","exportedAtMs":2}""",
                localFingerprintJson = """{"value":"local-new"}""",
                remoteFingerprintJson = { raw ->
                    if ("local-new" in raw) """{"value":"local-new"}""" else remoteFingerprint
                },
                mergeRemoteOnAutoConflict = true,
            )

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = null,
                    expectedRemoteHash = WebDavSyncEngine.contentHash(remoteFingerprint),
                )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals(listOf<String?>(null), client.uploadCalls.map { it.etag })
        assertEquals("W/\"etag-new\"", metadata.etags[WebDavSyncItem.BaAccounts])
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `lossless auto merge preserves conflict while strong validators keep changing`() = runBlocking {
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults =
                    mutableListOf(
                        WebDavDownloadResult.Success("remote-1", "etag-1"),
                        WebDavDownloadResult.Success("remote-2", "etag-2"),
                        WebDavDownloadResult.Success("remote-3", "etag-3"),
                    ),
                uploadResults =
                    mutableListOf(
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Conflict,
                    ),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port =
            FakeWebDavSyncDataPort(
                localJson = "local",
                mergeRemoteOnAutoConflict = true,
            )

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = "etag-base",
                    expectedRemoteHash = WebDavSyncEngine.contentHash("remote-base"),
                )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(
            listOf("etag-base", "etag-1", "etag-2", "etag-3"),
            client.uploadCalls.map { it.etag },
        )
        assertEquals(listOf("remote-1", "remote-2", "remote-3"), port.mergeCalls)
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }

    @Test
    fun `lossless auto merge falls back when same strong validator is rejected twice`() = runBlocking {
        val client =
            FakeWebDavSyncClientBridge(
                downloadResults =
                    mutableListOf(
                        WebDavDownloadResult.Success("remote-1", "etag-same"),
                        WebDavDownloadResult.Success("remote-2", "etag-same"),
                    ),
                uploadResults =
                    mutableListOf(
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Conflict,
                        WebDavUploadResult.Success("etag-final"),
                    ),
            )
        val metadata = FakeWebDavSyncMetadataStore()
        val port =
            FakeWebDavSyncDataPort(
                localJson = "local",
                mergeRemoteOnAutoConflict = true,
            )

        val outcome =
            WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
                .uploadLocalChange(
                    config = fakeConfig(),
                    item = WebDavSyncItem.BaAccounts,
                    port = port.port,
                    expectedRemoteEtag = "etag-base",
                    expectedRemoteHash = WebDavSyncEngine.contentHash("remote-base"),
                )

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(listOf("etag-base", "etag-same", null), client.uploadCalls.map { it.etag })
        assertEquals(listOf("remote-1", "remote-2"), port.mergeCalls)
        assertEquals("etag-final", metadata.etags[WebDavSyncItem.BaAccounts])
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `auto merge conflict falls back after refreshed remote omits etag`() = runBlocking {
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

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(listOf("etag-old", null), client.uploadCalls.map { it.etag })
        assertEquals(listOf("remote-new"), port.mergeCalls)
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `optimistic remerge falls back when latest remote omits etag`() = runBlocking {
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

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(listOf("etag-old", "etag-1", null), client.uploadCalls.map { it.etag })
        assertEquals(listOf("remote-1", "remote-2"), port.mergeCalls)
        assertTrue(metadata.pendingStates.isEmpty())
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
