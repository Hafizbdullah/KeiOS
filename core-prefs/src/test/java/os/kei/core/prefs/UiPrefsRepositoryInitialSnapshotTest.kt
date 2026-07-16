package os.kei.core.prefs

import org.junit.Test
import kotlin.test.assertFalse

class UiPrefsRepositoryInitialSnapshotTest {
    @Test
    fun `stored liquid sheet opt out is visible before async refresh`() {
        val storedSnapshot =
            UiPrefs.defaultSnapshot().copy(
                liquidSheetEnabled = false,
            )

        val repository = UiPrefsRepository(initialSnapshot = storedSnapshot)

        assertFalse(repository.observeSnapshots().value.liquidSheetEnabled)
    }
}
