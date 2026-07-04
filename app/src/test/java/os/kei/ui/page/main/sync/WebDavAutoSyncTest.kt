package os.kei.ui.page.main.sync

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebDavAutoSyncTest {
    @Test
    fun `jianguoyun launch auto sync uses quota window cooldown`() {
        assertEquals(30L * 60L * 1000L, launchAutoSyncCooldownMs(WebDavProvider.Jianguoyun))
    }

    @Test
    fun `failed auto sync uses shorter retry cooldown`() {
        assertEquals(5L * 60L * 1000L, retryAutoSyncCooldownMs(WebDavProvider.Jianguoyun))
        assertEquals(
            retryAutoSyncCooldownMs(WebDavProvider.Jianguoyun),
            autoSyncScheduleCooldownMs(WebDavProvider.Jianguoyun, WebDavAutoSyncStatus.Failed),
        )
        assertEquals(
            launchAutoSyncCooldownMs(WebDavProvider.Jianguoyun),
            autoSyncScheduleCooldownMs(WebDavProvider.Jianguoyun, WebDavAutoSyncStatus.Success),
        )
    }

    @Test
    fun `launch auto sync runs when no previous attempt or sync exists`() {
        assertTrue(
            shouldRunLaunchAutoSync(
                nowMs = 100_000L,
                lastAutoAttemptMs = 0L,
                lastFullSyncMs = 0L,
                cooldownMs = 30_000L,
            ),
        )
    }

    @Test
    fun `launch auto sync waits for latest attempt or full sync cooldown`() {
        assertFalse(
            shouldRunLaunchAutoSync(
                nowMs = 100_000L,
                lastAutoAttemptMs = 90_000L,
                lastFullSyncMs = 20_000L,
                cooldownMs = 30_000L,
            ),
        )
        assertFalse(
            shouldRunLaunchAutoSync(
                nowMs = 100_000L,
                lastAutoAttemptMs = 20_000L,
                lastFullSyncMs = 90_000L,
                cooldownMs = 30_000L,
            ),
        )
    }

    @Test
    fun `launch auto sync resumes after cooldown`() {
        assertTrue(
            shouldRunLaunchAutoSync(
                nowMs = 130_001L,
                lastAutoAttemptMs = 100_000L,
                lastFullSyncMs = 50_000L,
                cooldownMs = 30_000L,
            ),
        )
    }

    @Test
    fun `pending conflict and missing baseline defer automatic sync`() {
        assertTrue(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.RemoteConflict))
        assertTrue(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.BaselineRequired))
        assertFalse(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.LocalUploadPending))
        assertFalse(shouldDeferPendingWebDavAutoSync(null))
    }

    @Test
    fun `auto merge ports can recover pending review during automatic sync`() {
        val guardedPort = webDavAutoSyncTestPort(mergeRemoteOnAutoConflict = false)
        val mergePort = webDavAutoSyncTestPort(mergeRemoteOnAutoConflict = true)

        assertTrue(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.RemoteConflict, guardedPort))
        assertTrue(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.BaselineRequired, guardedPort))
        assertFalse(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.RemoteConflict, mergePort))
        assertFalse(shouldDeferPendingWebDavAutoSync(WebDavSyncPendingState.BaselineRequired, mergePort))
    }
}

private fun webDavAutoSyncTestPort(mergeRemoteOnAutoConflict: Boolean): WebDavSyncDataPort =
    WebDavSyncDataPort(
        exportJson = { "{}" },
        merge = {},
        localCount = { 0 },
        countRemoteItems = { 0 },
        mergeRemoteOnAutoConflict = mergeRemoteOnAutoConflict,
    )
