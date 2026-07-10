package os.kei.mcp.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(
    application = McpNotificationMarkReadIntentTestApp::class,
    sdk = [35],
)
class McpNotificationMarkReadIntentTest {
    @Test
    fun `mark read intent carries immutable BA AP metadata`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val intent =
            McpNotificationHelper.buildMarkReadIntent(
                context = context,
                notificationId = 243_220,
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                targetBaAccountId = "cn-main",
            )

        assertEquals(
            243_220,
            intent.getIntExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, -1),
        )
        assertEquals(
            LiveNotificationPayload.BA_AP_SERVER_NAME,
            intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME),
        )
        assertEquals(
            "cn-main",
            intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID),
        )
    }
}

class McpNotificationMarkReadIntentTestApp : Application()
