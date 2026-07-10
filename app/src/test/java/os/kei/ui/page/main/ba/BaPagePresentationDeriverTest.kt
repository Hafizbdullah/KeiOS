package os.kei.ui.page.main.ba

import androidx.compose.runtime.mutableLongStateOf
import org.junit.Test
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaPagePresentationDeriverTest {
    @Test
    fun `saved settings draft is derived from immutable office snapshot`() {
        val officeState =
            BaOfficeController(
                BaPageSnapshot(
                    cafeLevel = 7,
                    showEndedActivities = true,
                    showEndedPools = true,
                    showCalendarPoolImages = false,
                ),
            ).state()
        val routeState =
            buildBaPageRouteState(
                calendarUiState = BaCalendarUiState(),
                poolUiState = BaPoolUiState(),
                chromeUiState = BaOfficeChromeUiState(),
                syncUiState = BaOfficeSyncUiState(),
                accountUiState = BaOfficeAccountUiState(),
                serverUiState = BaOfficeServerUiState(),
                runtimeUiState =
                    BaOfficeRuntimeUiState(
                        mediaAdaptiveRotationEnabled = false,
                        mediaSaveCustomEnabled = true,
                        mediaSaveFixedTreeUri = "content://ba-media",
                        showEndedActivities = true,
                        showEndedPools = true,
                        showCalendarPoolImages = false,
                    ),
                settingsDraftUiState = BaOfficeSettingsDraftUiState(),
                notificationDraftUiState = BaOfficeNotificationDraftUiState(),
            )

        val draft =
            buildBaSavedSettingsDraftState(
                officeState = officeState,
                routeState = routeState,
            )

        assertEquals(7, draft.cafeLevel)
        assertTrue(draft.mediaSaveCustomEnabled)
        assertEquals("content://ba-media", draft.mediaSaveFixedTreeUri)
    }

    @Test
    fun `notification presentation exposes persistent AP read suppression from the draft`() {
        val snapshot = BaPageSnapshot(keepApRemindersReadUntilBelowThreshold = true)

        val presentation = buildNotificationPresentation(snapshot)

        assertTrue(
            presentation.notificationSettingsSheetState
                .keepApRemindersReadUntilBelowThreshold,
        )
        assertTrue(
            presentation.savedNotificationSettingsSheetState
                .keepApRemindersReadUntilBelowThreshold,
        )
    }

    @Test
    fun `notification presentation retains hourly AP read suppression in current and saved sheets`() {
        val snapshot = BaPageSnapshot(keepApRemindersReadUntilBelowThreshold = false)

        val presentation = buildNotificationPresentation(snapshot)

        assertFalse(
            presentation.notificationSettingsSheetState
                .keepApRemindersReadUntilBelowThreshold,
        )
        assertFalse(
            presentation.savedNotificationSettingsSheetState
                .keepApRemindersReadUntilBelowThreshold,
        )
    }

    private fun buildNotificationPresentation(snapshot: BaPageSnapshot): BaPagePresentationState =
        buildBaPagePresentationState(
            isPageActive = true,
            officeState = BaOfficeController(snapshot).state(),
            calendarUiState = BaCalendarUiState(),
            poolUiState = BaPoolUiState(),
            officePageUiState =
                BaOfficePageUiState(
                    notificationDraftUiState =
                        BaOfficeNotificationDraftUiState(
                            draft = snapshot.toNotificationDraftState(),
                            savedDraft = snapshot.toNotificationDraftState(),
                        ),
                ),
            clockState =
                BaPageClockState(
                    uiNowMs = mutableLongStateOf(0L),
                    uiMinuteMs = mutableLongStateOf(0L),
                ),
            serverOptions = emptyList(),
            cafeLevelOptions = emptyList(),
        )
}
