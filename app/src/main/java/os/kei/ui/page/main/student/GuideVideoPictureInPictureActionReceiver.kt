package os.kei.ui.page.main.student

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GuideVideoPictureInPictureActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            GUIDE_VIDEO_ACTION_CLOSE_PIP,
            GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK -> GuideVideoFullscreenActivity.dispatchPictureInPictureAction(action)
        }
    }
}
