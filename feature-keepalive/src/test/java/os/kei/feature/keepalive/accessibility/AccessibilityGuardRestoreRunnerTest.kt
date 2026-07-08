package os.kei.feature.keepalive.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccessibilityGuardRestoreRunnerTest {
    private val tempDirs = mutableListOf<File>()
    private val alpha = AccessibilityServiceId("com.alpha", "com.alpha.Service")

    @After
    fun cleanup() {
        tempDirs.forEach { dir -> dir.deleteRecursively() }
        tempDirs.clear()
    }

    @Test
    fun `restore and record writes successful history`() = runTest {
        val historyStore = historyStore()
        val runner =
            AccessibilityGuardRestoreRunner(
                restoreOperation =
                    AccessibilityGuardRestoreOperation {
                        result(status = AccessibilityGuardRestoreStatus.Restored)
                    },
                historyStore = historyStore,
                selectedIdsProvider = { setOf(alpha) },
            )

        val result =
            runner.restoreAndRecord(
                reason = AccessibilityGuardRestoreReason.Manual,
                triggerAction = "manual_check",
            )

        assertEquals(AccessibilityGuardRestoreStatus.Restored, result.status)
        val history = historyStore.latest(1).single()
        assertEquals(AccessibilityGuardRestoreStatus.Restored, history.status)
        assertEquals("manual_check", history.triggerAction)
        assertEquals(1, history.selectedCount)
    }

    @Test
    fun `restore and record writes timeout history`() = runTest {
        val historyStore = historyStore()
        val runner =
            AccessibilityGuardRestoreRunner(
                restoreOperation =
                    AccessibilityGuardRestoreOperation {
                        delay(10_000L)
                        result(status = AccessibilityGuardRestoreStatus.Restored)
                    },
                historyStore = historyStore,
                selectedIdsProvider = { setOf(alpha) },
                timeoutMs = 100L,
                wallClockMs = { 1_000L },
                elapsedClockMs = { 1_000L },
            )

        val result =
            runner.restoreAndRecord(
                reason = AccessibilityGuardRestoreReason.BootCompleted,
                triggerAction = "boot",
            )

        assertEquals(AccessibilityGuardRestoreStatus.TimedOut, result.status)
        assertEquals("timeout", result.failureReason)
        val history = historyStore.latest(1).single()
        assertEquals(AccessibilityGuardRestoreStatus.TimedOut, history.status)
        assertEquals("boot", history.triggerAction)
        assertEquals(1, history.skippedCount)
    }

    @Test
    fun `record timeout writes history without running restore operation`() = runTest {
        val historyStore = historyStore()
        var called = false
        val runner =
            AccessibilityGuardRestoreRunner(
                restoreOperation =
                    AccessibilityGuardRestoreOperation {
                        called = true
                        result(status = AccessibilityGuardRestoreStatus.Restored)
                    },
                historyStore = historyStore,
                selectedIdsProvider = { setOf(alpha) },
            )

        val result =
            runner.recordTimeout(
                reason = AccessibilityGuardRestoreReason.PackageReplaced,
                triggerAction = "package:timeout",
            )

        assertEquals(false, called)
        assertEquals(AccessibilityGuardRestoreStatus.TimedOut, result.status)
        assertEquals("package:timeout", historyStore.latest(1).single().triggerAction)
    }

    private fun historyStore(): AccessibilityGuardHistoryStore {
        val dir = Files.createTempDirectory("accessibility-guard-runner").toFile()
        tempDirs += dir
        return AccessibilityGuardHistoryStore(File(dir, "history.jsonl"))
    }

    private fun result(status: AccessibilityGuardRestoreStatus): AccessibilityGuardRestoreResult =
        AccessibilityGuardRestoreResult(
            status = status,
            reason = AccessibilityGuardRestoreReason.Manual,
            selectedIds = setOf(alpha),
            beforeEnabledIds = emptySet(),
            afterEnabledIds = setOf(alpha),
            restoredIds = if (status == AccessibilityGuardRestoreStatus.Restored) setOf(alpha) else emptySet(),
            skippedIds = emptySet(),
            startedAtMs = 1_000L,
            finishedAtMs = 1_100L,
            elapsedMs = 100L,
            shizukuStatus = "ready",
            failureReason = "",
        )
}
