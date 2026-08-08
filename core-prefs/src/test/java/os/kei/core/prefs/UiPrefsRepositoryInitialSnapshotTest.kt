package os.kei.core.prefs

import org.junit.Test
import kotlin.test.assertFalse

class UiPrefsRepositoryInitialSnapshotTest {
    /**
     * Retargeted from the liquid-sheet opt-out, which no longer exists — the sheet is always the
     * Liquid one. The property under test is unchanged: whatever was stored has to be observable
     * before the async refresh lands, or the first composed frame flashes the default.
     */
    @Test
    fun `stored opt out is visible before async refresh`() {
        val storedSnapshot =
            UiPrefs.defaultSnapshot().copy(
                liquidSwitchEnabled = false,
            )

        val repository = UiPrefsRepository(initialSnapshot = storedSnapshot)

        assertFalse(repository.observeSnapshots().value.liquidSwitchEnabled)
    }
}
