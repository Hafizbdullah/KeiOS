package os.kei.core.notification.live.builder

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(application = NotificationActionRoutingTestApp::class)
class NotificationActionRoutingTest {
    @Test
    @Config(sdk = [36])
    fun `modern live update routes secondary action to mark read intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val (openPendingIntent, markReadPendingIntent) = pendingIntents(context)

        val modern = ModernNotificationBuilder(context).build(payload(context, openPendingIntent, markReadPendingIntent))

        assertEquals(markReadPendingIntent, modern.actions[1].actionIntent)
    }

    @Test
    @Config(sdk = [35])
    fun `legacy live update routes secondary action to mark read intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val (openPendingIntent, markReadPendingIntent) = pendingIntents(context)

        val legacy = LegacyNotificationBuilder(context).build(payload(context, openPendingIntent, markReadPendingIntent))

        assertEquals(markReadPendingIntent, legacy.actions[1].actionIntent)
    }

    @Test
    @Config(sdk = [35])
    fun `super island routes stop action to mark read intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val (openPendingIntent, markReadPendingIntent) = pendingIntents(context)

        val island = MiIslandNotificationBuilder(context).build(payload(context, openPendingIntent, markReadPendingIntent))

        assertEquals(markReadPendingIntent, island.focusAction("mcp_action_stop").actionIntent)
    }

    private fun pendingIntents(context: Application): Pair<PendingIntent, PendingIntent> =
        PendingIntent.getActivity(
            context,
            243_221,
            Intent("os.kei.test.OPEN_BA_AP").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) to PendingIntent.getBroadcast(
            context,
            243_222,
            Intent("os.kei.test.MARK_BA_AP_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun payload(
        context: Application,
        openPendingIntent: PendingIntent,
        markReadPendingIntent: PendingIntent,
    ): NotificationPayload =
        NotificationPayload(
            state =
                LiveNotificationPayload(
                    serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                    running = true,
                    port = 128,
                    path = "120",
                    clients = 240,
                    ongoing = true,
                    onlyAlertOnce = true,
                    openPendingIntent = openPendingIntent,
                    stopPendingIntent = markReadPendingIntent,
                    focusOpenPendingIntent = openPendingIntent,
                    notificationId = 243_220,
                    miFocusOrderId = "ba-ap-routing",
                ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment =
                EnvironmentContext(
                    channelId = "test_notification_action_routing",
                    isHyperOS = true,
                ),
        )

    private fun Notification.focusAction(key: String): Notification.Action {
        val actions = extras.getBundle("miui.focus.actions")
        assertNotNull(actions, "Focus actions bundle should be present")
        return actions.getActionCompat(key)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getActionCompat(key: String): Notification.Action =
        getParcelable<Notification.Action>(key)
            ?: error("Missing focus action: $key")
}

class NotificationActionRoutingTestApp : Application()
