package os.kei.ui.page.main.ba

import androidx.compose.runtime.mutableLongStateOf
import org.junit.Test
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The persisted Craft Chamber expansion has to survive four hops before the card sees it:
 * snapshot → runtime UI state → route state → content state. Each is hand-written, so a dropped
 * field would silently pin the card open with no compiler complaint.
 */
class BaCraftCardExpansionStateTest {
    private fun contentState(runtimeUiState: BaOfficeRuntimeUiState): BaPageContentState =
        buildBaPageContentState(
            officeState = BaOfficeController(BaPageSnapshot()).state(),
            routeState =
                buildBaPageRouteState(
                    calendarUiState = BaCalendarUiState(),
                    poolUiState = BaPoolUiState(),
                    chromeUiState = BaOfficeChromeUiState(),
                    syncUiState = BaOfficeSyncUiState(),
                    accountUiState = BaOfficeAccountUiState(),
                    serverUiState = BaOfficeServerUiState(),
                    runtimeUiState = runtimeUiState,
                    settingsDraftUiState = BaOfficeSettingsDraftUiState(),
                    notificationDraftUiState = BaOfficeNotificationDraftUiState(),
                ),
            clockState =
                BaPageClockState(
                    uiNowMs = mutableLongStateOf(0L),
                    uiMinuteMs = mutableLongStateOf(0L),
                ),
            serverOptions = listOf("CN", "Global", "JP"),
            cafeLevelOptions = listOf(1, 2, 3),
        )

    @Test
    fun `the card is expanded until the teacher folds it`() {
        // Existing installs have no stored value, and hiding six rows they were already using would
        // read as data loss.
        assertTrue(BaPageSnapshot().craftCardExpanded)
        assertTrue(BaOfficeRuntimeUiState().craftCardExpanded)
        assertTrue(contentState(BaOfficeRuntimeUiState()).craftCardExpanded)
    }

    @Test
    fun `a collapsed snapshot reaches the content state`() {
        val runtimeUiState = BaPageSnapshot(craftCardExpanded = false).toRuntimeUiState()

        assertFalse(runtimeUiState.craftCardExpanded)
        assertFalse(contentState(runtimeUiState).craftCardExpanded)
    }
}
