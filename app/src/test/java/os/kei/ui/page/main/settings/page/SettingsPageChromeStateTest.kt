package os.kei.ui.page.main.settings.page

import org.junit.Test
import os.kei.ui.page.main.settings.state.SettingsCardExpansionId
import os.kei.ui.page.main.settings.state.isSettingsCardExpanded
import kotlin.test.assertEquals

class SettingsPageChromeStateTest {
    @Test
    fun `active category follows target page while scrolling`() {
        assertEquals(
            2,
            settingsActiveCategoryIndex(
                scrolling = true,
                targetPage = 2,
                settledPage = 0,
                lastIndex = 3,
            ),
        )
    }

    @Test
    fun `active category follows settled page when idle`() {
        assertEquals(
            1,
            settingsActiveCategoryIndex(
                scrolling = false,
                targetPage = 3,
                settledPage = 1,
                lastIndex = 3,
            ),
        )
    }

    @Test
    fun `active category clamps page bounds`() {
        assertEquals(
            3,
            settingsActiveCategoryIndex(
                scrolling = true,
                targetPage = 8,
                settledPage = 1,
                lastIndex = 3,
            ),
        )
        assertEquals(
            0,
            settingsActiveCategoryIndex(
                scrolling = false,
                targetPage = 2,
                settledPage = -5,
                lastIndex = 3,
            ),
        )
    }

    @Test
    fun `card expansion falls back to card default`() {
        assertEquals(
            SettingsCardExpansionId.Permissions.defaultExpanded,
            emptyMap<SettingsCardExpansionId, Boolean>().isSettingsCardExpanded(SettingsCardExpansionId.Permissions),
        )
        assertEquals(
            SettingsCardExpansionId.KeepAlive.defaultExpanded,
            emptyMap<SettingsCardExpansionId, Boolean>().isSettingsCardExpanded(SettingsCardExpansionId.KeepAlive),
        )
    }

    @Test
    fun `card expansion snapshot overrides card default`() {
        val snapshot = mapOf(SettingsCardExpansionId.KeepAlive to true)

        assertEquals(true, snapshot.isSettingsCardExpanded(SettingsCardExpansionId.KeepAlive))
    }

    @Test
    fun `search result expansion does not persist card state`() {
        assertEquals(true, shouldPersistSettingsCardExpansion(""))
        assertEquals(true, shouldPersistSettingsCardExpansion("   "))
        assertEquals(false, shouldPersistSettingsCardExpansion("cache"))
    }
}
