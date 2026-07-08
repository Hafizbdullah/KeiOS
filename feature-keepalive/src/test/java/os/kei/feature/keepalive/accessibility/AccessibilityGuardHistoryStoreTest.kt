package os.kei.feature.keepalive.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import os.kei.core.json.optArray
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccessibilityGuardHistoryStoreTest {
    private val tempDirs = mutableListOf<File>()
    private val alpha = AccessibilityServiceId("com.alpha", "com.alpha.Service")
    private val beta = AccessibilityServiceId("com.beta", "com.beta.Service")

    @After
    fun cleanup() {
        tempDirs.forEach { dir -> dir.deleteRecursively() }
        tempDirs.clear()
    }

    @Test
    fun `append stores latest entries newest first`() = runTest {
        val store = store()

        store.append(entry(id = "old", timestampMs = 100L))
        store.append(entry(id = "new", timestampMs = 300L))
        store.append(entry(id = "middle", timestampMs = 200L))

        assertEquals(listOf("new", "middle", "old"), store.latest(10).map { it.id })
        assertEquals(listOf("new", "middle"), store.latest(2).map { it.id })
    }

    @Test
    fun `append trims old entries by max entry count`() = runTest {
        val store = store(maxEntries = 3)

        repeat(5) { index ->
            store.append(entry(id = "entry-$index", timestampMs = index + 1L))
        }

        assertEquals(listOf("entry-4", "entry-3", "entry-2"), store.latest(10).map { it.id })
    }

    @Test
    fun `append trims old entries by byte count`() = runTest {
        val file = tempFile()
        val store =
            AccessibilityGuardHistoryStore(
                historyFile = file,
                maxEntries = 10,
                maxBytes = 1_600L,
            )

        repeat(8) { index ->
            store.append(
                entry(
                    id = "entry-$index",
                    timestampMs = index + 1L,
                    failureReason = "network timeout ".repeat(60),
                ),
            )
        }

        assertTrue(file.length() <= 1_600L, "history file length was ${file.length()}")
        assertTrue(store.latest(10).size < 8)
        assertEquals("entry-7", store.latest(1).single().id)
    }

    @Test
    fun `encode and decode keeps guard fields`() {
        val encoded =
            AccessibilityGuardHistoryStore.encodeEntry(
                entry(
                    id = "roundtrip",
                    timestampMs = 123L,
                    status = AccessibilityGuardRestoreStatus.Restored,
                    triggerAction = "manual_check",
                    restoredCount = 1,
                    serviceIds = listOf(beta, alpha),
                ),
            ).toString()

        val decoded = AccessibilityGuardHistoryStore.decodeEntry(encoded)

        assertNotNull(decoded)
        assertEquals("roundtrip", decoded.id)
        assertEquals(AccessibilityGuardRestoreStatus.Restored, decoded.status)
        assertEquals("manual_check", decoded.triggerAction)
        assertEquals(1, decoded.restoredCount)
        assertEquals(listOf(alpha, beta), decoded.serviceIds)
    }

    @Test
    fun `export json marks history as local only`() {
        val exported =
            AccessibilityGuardHistoryStore.buildExportJson(
                records =
                    listOf(
                        entry(id = "skip", timestampMs = 100L, status = AccessibilityGuardRestoreStatus.SkippedCooldown),
                        entry(id = "restore", timestampMs = 200L, status = AccessibilityGuardRestoreStatus.Restored),
                    ),
                exportedAtMillis = 300L,
            )

        val root = exported.parseJsonObjectOrNull()
        assertNotNull(root)
        assertEquals("keios.keepalive.accessibility-guard-history", root.optString("format"))
        assertEquals("local_only", root.optString("syncScope"))
        assertEquals(2, root.optArray("records")?.size)
        assertEquals(2, root.optObject("summary")?.get("storedCount")?.toString()?.toInt())
        assertEquals("restore", root.optArray("records")?.optObject(0)?.optString("id"))
    }

    @Test
    fun `history entry can be built from restore result`() {
        val result =
            AccessibilityGuardRestoreResult(
                status = AccessibilityGuardRestoreStatus.Failed,
                reason = AccessibilityGuardRestoreReason.ScreenOn,
                selectedIds = setOf(alpha, beta),
                beforeEnabledIds = setOf(alpha),
                afterEnabledIds = setOf(alpha),
                restoredIds = emptySet(),
                skippedIds = setOf(beta),
                startedAtMs = 1_000L,
                finishedAtMs = 1_200L,
                elapsedMs = 200L,
                shizukuStatus = "ready",
                failureReason = "write denied",
            )

        val entry =
            AccessibilityGuardHistoryEntry.fromResult(
                result = result,
                id = "from-result",
                triggerAction = "screen_on_receiver",
            )

        assertEquals("from-result", entry.id)
        assertEquals(1_200L, entry.timestampMs)
        assertEquals(AccessibilityGuardRestoreReason.ScreenOn, entry.reason)
        assertEquals(AccessibilityGuardRestoreStatus.Failed, entry.status)
        assertEquals("screen_on_receiver", entry.triggerAction)
        assertEquals(2, entry.selectedCount)
        assertEquals(1, entry.skippedCount)
        assertEquals(listOf(alpha, beta), entry.serviceIds)
    }

    private fun store(
        maxEntries: Int = 500,
        maxBytes: Long = 1L * 1024L * 1024L,
    ): AccessibilityGuardHistoryStore =
        AccessibilityGuardHistoryStore(
            historyFile = tempFile(),
            maxEntries = maxEntries,
            maxBytes = maxBytes,
        )

    private fun tempFile(): File {
        val dir = Files.createTempDirectory("accessibility-guard-history").toFile()
        tempDirs += dir
        return File(dir, "history.jsonl")
    }

    private fun entry(
        id: String = "entry",
        timestampMs: Long = 1L,
        status: AccessibilityGuardRestoreStatus = AccessibilityGuardRestoreStatus.SkippedAlreadyEnabled,
        triggerAction: String = "manual_check",
        restoredCount: Int = 0,
        failureReason: String = "",
        serviceIds: List<AccessibilityServiceId> = listOf(alpha),
    ): AccessibilityGuardHistoryEntry =
        AccessibilityGuardHistoryEntry(
            id = id,
            timestampMs = timestampMs,
            reason = AccessibilityGuardRestoreReason.Manual,
            status = status,
            triggerAction = triggerAction,
            selectedCount = serviceIds.size,
            restoredCount = restoredCount,
            skippedCount = 0,
            elapsedMs = 25L,
            shizukuStatus = "ready",
            failureReason = failureReason,
            serviceIds = serviceIds,
        )
}
