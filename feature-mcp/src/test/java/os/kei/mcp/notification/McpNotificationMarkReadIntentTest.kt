package os.kei.mcp.notification

import android.app.PendingIntent
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `ordinary AP production PendingIntent updates current account metadata`() {
        assertProductionPendingIntentMetadataUpdate(
            notificationId = 243_220,
            serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
        )
    }

    @Test
    fun `cafe AP production PendingIntent updates current account metadata`() {
        assertProductionPendingIntentMetadataUpdate(
            notificationId = 243_221,
            serverName = LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME,
        )
    }

    @Test
    fun `production PendingIntent upgrades old ID only action extras`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationId = 243_222
        val requestCode = 210_200 + notificationId
        val legacyIntent =
            Intent().apply {
                setClassName(
                    context.packageName,
                    McpNotificationActionContract.MI_FOCUS_ACTION_RECEIVER_CLASS_NAME,
                )
                action = McpNotificationMarkReadContract.ACTION
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                putExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, notificationId)
            }
        val legacy =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                legacyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val updated =
            McpNotificationHelper.markReadPendingIntent(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                targetBaAccountId = "cn-main",
            )
        val shadow = shadowOf(updated)
        val currentIntent = shadow.savedIntent

        assertEquals(legacy, updated)
        assertEquals(requestCode, shadow.requestCode)
        assertEquals(
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            shadow.flags,
        )
        assertEquals(
            LiveNotificationPayload.BA_AP_SERVER_NAME,
            currentIntent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME),
        )
        assertEquals(
            "cn-main",
            currentIntent.getStringExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID),
        )
    }

    private fun assertProductionPendingIntentMetadataUpdate(
        notificationId: Int,
        serverName: String,
    ) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first =
            McpNotificationHelper.markReadPendingIntent(
                context = context,
                notificationId = notificationId,
                serverName = serverName,
                targetBaAccountId = "cn-old",
            )
        val updated =
            McpNotificationHelper.markReadPendingIntent(
                context = context,
                notificationId = notificationId,
                serverName = serverName,
                targetBaAccountId = "cn-new",
            )
        val shadow = shadowOf(updated)
        val currentIntent = shadow.savedIntent

        assertEquals(first, updated)
        assertEquals(210_200 + notificationId, shadow.requestCode)
        assertEquals(
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            shadow.flags,
        )
        assertEquals(
            ComponentName(
                context.packageName,
                McpNotificationActionContract.MI_FOCUS_ACTION_RECEIVER_CLASS_NAME,
            ),
            currentIntent.component,
        )
        assertEquals(McpNotificationMarkReadContract.ACTION, currentIntent.action)
        assertTrue(currentIntent.flags and Intent.FLAG_RECEIVER_FOREGROUND != 0)
        assertEquals(
            notificationId,
            currentIntent.getIntExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, -1),
        )
        assertEquals(
            serverName,
            currentIntent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME),
        )
        assertEquals(
            "cn-new",
            currentIntent.getStringExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID),
        )
    }
}

class McpNotificationMarkReadIntentTestApp : Application()
