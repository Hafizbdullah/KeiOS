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
    fun `encode and decode keeps self check fields`() {
        val encoded =
            AccessibilityGuardHistoryStore.encodeEntry(
                entry(
                    id = "roundtrip",
                    timestampMs = 123L,
                    status = AccessibilityGuardCheckStatus.Healthy,
                    triggerAction = "manual_check",
                    checkCount = 3,
                    healthyCount = 3,
                    warningCount = 0,
                ),
            ).toString()

        val decoded = AccessibilityGuardHistoryStore.decodeEntry(encoded)

        assertNotNull(decoded)
        assertEquals("roundtrip", decoded.id)
        assertEquals(AccessibilityGuardCheckStatus.Healthy, decoded.status)
        assertEquals("manual_check", decoded.triggerAction)
        assertEquals(3, decoded.checkCount)
        assertEquals(3, decoded.healthyCount)
        assertEquals(0, decoded.warningCount)
    }

    @Test
    fun `decode maps legacy restore records to check records`() {
        val decoded =
            AccessibilityGuardHistoryStore.decodeEntry(
                """
                {
                  "id":"legacy",
                  "timestampMs":100,
                  "reason":"ScreenOn",
                  "status":"SkippedMissingPrivilege",
                  "triggerAction":"screen_on",
                  "selectedCount":2,
                  "restoredCount":0,
                  "skippedCount":2,
                  "elapsedMs":25,
                  "failureReason":"permission denied",
                  "serviceIds":[
                    {"packageName":"com.alpha","serviceName":"com.alpha.Service"}
                  ]
                }
                """.trimIndent(),
            )

        assertNotNull(decoded)
        assertEquals(AccessibilityGuardCheckReason.ScreenOn, decoded.reason)
        assertEquals(AccessibilityGuardCheckStatus.MissingPrivilege, decoded.status)
        assertEquals(2, decoded.checkCount)
        assertEquals(0, decoded.healthyCount)
        assertEquals(2, decoded.warningCount)
    }

    @Test
    fun `export json marks history as local only`() {
        val exported =
            AccessibilityGuardHistoryStore.buildExportJson(
                records =
                    listOf(
                        entry(id = "checked", timestampMs = 100L, status = AccessibilityGuardCheckStatus.Checked),
                        entry(id = "healthy", timestampMs = 200L, status = AccessibilityGuardCheckStatus.Healthy),
                    ),
                exportedAtMillis = 300L,
            )

        val root = exported.parseJsonObjectOrNull()
        assertNotNull(root)
        assertEquals("keios.keepalive.accessibility-guard-history", root.optString("format"))
        assertEquals("local_only", root.optString("syncScope"))
        assertEquals(2, root.optArray("records")?.size)
        assertEquals(2, root.optObject("summary")?.get("storedCount")?.toString()?.toInt())
        assertEquals("healthy", root.optArray("records")?.optObject(0)?.optString("id"))
    }

    @Test
    fun `history entry can be built from check result`() {
        val result =
            AccessibilityGuardCheckResult(
                status = AccessibilityGuardCheckStatus.MissingPrivilege,
                reason = AccessibilityGuardCheckReason.ScreenOn,
                checkCount = 2,
                healthyCount = 1,
                warningCount = 1,
                startedAtMs = 1_000L,
                finishedAtMs = 1_200L,
                elapsedMs = 200L,
                shizukuStatus = "permission denied",
                failureReason = "permission denied",
            )

        val entry =
            AccessibilityGuardHistoryEntry.fromResult(
                result = result,
                id = "from-result",
                triggerAction = "screen_on_receiver",
            )

        assertEquals("from-result", entry.id)
        assertEquals(1_200L, entry.timestampMs)
        assertEquals(AccessibilityGuardCheckReason.ScreenOn, entry.reason)
        assertEquals(AccessibilityGuardCheckStatus.MissingPrivilege, entry.status)
        assertEquals("screen_on_receiver", entry.triggerAction)
        assertEquals(2, entry.checkCount)
        assertEquals(1, entry.healthyCount)
        assertEquals(1, entry.warningCount)
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
        status: AccessibilityGuardCheckStatus = AccessibilityGuardCheckStatus.Checked,
        triggerAction: String = "manual_check",
        checkCount: Int = 1,
        healthyCount: Int = if (status == AccessibilityGuardCheckStatus.MissingPrivilege) 0 else checkCount,
        warningCount: Int = if (status == AccessibilityGuardCheckStatus.MissingPrivilege) 1 else 0,
        failureReason: String = "",
    ): AccessibilityGuardHistoryEntry =
        AccessibilityGuardHistoryEntry(
            id = id,
            timestampMs = timestampMs,
            reason = AccessibilityGuardCheckReason.Manual,
            status = status,
            triggerAction = triggerAction,
            checkCount = checkCount,
            healthyCount = healthyCount,
            warningCount = warningCount,
            elapsedMs = 25L,
            shizukuStatus = "ready",
            failureReason = failureReason,
        )
}
