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
}
