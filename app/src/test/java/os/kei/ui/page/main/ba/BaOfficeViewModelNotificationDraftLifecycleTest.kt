package os.kei.ui.page.main.ba

import org.junit.Test
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaOfficeViewModelNotificationDraftLifecycleTest {
    @Test
    fun `synchronized AP read setting survives sheet close reopen and save`() {
        val office =
            BaOfficeController(
                BaPageSnapshot(keepApRemindersReadUntilBelowThreshold = true),
            )
        var savedDraft =
            BaPageSnapshot(keepApRemindersReadUntilBelowThreshold = true)
                .toNotificationDraftState()

        var draft = notificationRuntimeDraft(base = savedDraft, office = office)
        assertTrue(draft.keepApRemindersReadUntilBelowThreshold)

        office.applySnapshot(
            BaPageSnapshot(keepApRemindersReadUntilBelowThreshold = false),
        )
        draft = savedDraft
        draft = notificationRuntimeDraft(base = draft, office = office)
        val saveInput = buildBaNotificationSettingsSheetState(draft)
        savedDraft = draft

        assertFalse(saveInput.keepApRemindersReadUntilBelowThreshold)
        assertFalse(savedDraft.keepApRemindersReadUntilBelowThreshold)
    }
}
