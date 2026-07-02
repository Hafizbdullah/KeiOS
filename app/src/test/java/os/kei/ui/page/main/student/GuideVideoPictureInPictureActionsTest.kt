package os.kei.ui.page.main.student

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(application = Application::class, sdk = [35])
@RunWith(AndroidJUnit4::class)
class GuideVideoPictureInPictureActionsTest {
    @Test
    fun `close action is also present in visible actions for SystemUI close handling`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val actionSet =
            buildGuidePictureInPictureActionSet(
                context = context,
                sessionId = 42L,
                playWhenReady = true,
            )

        val closeAction = assertNotNull(actionSet.closeAction)
        val visibleCloseAction =
            actionSet.actions.singleOrNull { action ->
                action.actionIntent == closeAction.actionIntent
            }

        assertNotNull(visibleCloseAction)
        assertEquals(closeAction.title.toString(), visibleCloseAction.title.toString())
        assertTrue(actionSet.actions.size >= 2)
    }

    @Test
    fun `max actions keeps close action when current guide video action count fits`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val actionSet =
            buildGuidePictureInPictureActionSet(
                context = context,
                sessionId = 43L,
                playWhenReady = false,
                maxActions = 3,
            )

        val closeAction = assertNotNull(actionSet.closeAction)
        assertTrue(actionSet.actions.any { action -> action.actionIntent == closeAction.actionIntent })
    }
}
