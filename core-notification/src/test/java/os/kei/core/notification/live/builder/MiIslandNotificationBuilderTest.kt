package os.kei.core.notification.live.builder

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.graphics.toColorInt
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import os.kei.core.notification.R
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(
    application = MiIslandNotificationBuilderTestApp::class,
    sdk = [35]
)
class MiIslandNotificationBuilderTest {
    @Test
    fun `running mcp service uses fixed client count summary`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 491,
            action = "os.kei.test.OPEN_RUNNING_MCP"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            492,
            Intent("os.kei.test.STOP_RUNNING_MCP").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = "My MCP",
                running = true,
                port = 8080,
                path = "/mcp",
                clients = 3,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = openPendingIntent,
                stopPendingIntent = stopPendingIntent,
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusJson = JSONObject(
            notification.extras.getString("miui.focus.param").orEmpty()
        ).getJSONObject("param_v2")
        val bigIsland =
            focusJson.getJSONObject("param_island").getJSONObject("bigIslandArea")
        val smallIsland =
            focusJson.getJSONObject("param_island").getJSONObject("smallIslandArea")
        val baseInfo = focusJson.getJSONObject("baseInfo")

        assertEquals("3", bigIsland.getJSONObject("fixedWidthDigitInfo").getString("digit"))
        assertEquals(
            context.getString(R.string.mcp_clients_label),
            bigIsland.getJSONObject("fixedWidthDigitInfo").getString("content"),
        )
        assertEquals(6, smallIsland.getJSONObject("imageTextInfoRight").getInt("type"))
        assertEquals(
            "3",
            smallIsland.getJSONObject("imageTextInfoRight")
                .getJSONObject("textInfo")
                .getString("title"),
        )
        assertEquals("My MCP", baseInfo.getString("title"))
        assertEquals(context.getString(R.string.mcp_status_running), baseInfo.getString("specialTitle"))
        assertTrue(baseInfo.getString("content").contains("8080"))
        assertTrue(baseInfo.getString("content").contains("/mcp"))
        assertFalse(focusJson.toString().contains("progressInfo"))
    }

    @Test
    fun `focus open action keeps plain activity pending intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 501,
            action = "os.kei.test.OPEN_NOTIFICATION"
        )
        val focusOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 502,
            action = "os.kei.test.OPEN_FOCUS"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            503,
            Intent("os.kei.test.STOP_MCP").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = "KeiOS MCP",
                running = false,
                port = 8080,
                path = "/mcp",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = focusOpenPendingIntent,
                notificationId = 38888,
                miFocusOrderId = "mcp_keepalive"
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusOpenAction = notification.focusAction("mcp_action_open")
        val focusStopAction = notification.focusAction("mcp_action_stop")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(notificationOpenPendingIntent, notification.contentIntent)
        assertEquals(focusOpenPendingIntent, focusOpenAction.actionIntent)
        assertEquals(stopPendingIntent, focusStopAction.actionIntent)
        assertTrue(focusParam.contains("mcp_action_open"))
        assertTrue(focusParam.contains("mcp_action_stop"))
        assertTrue(focusParam.contains("\"clickWithCollapse\":true"))
        assertTrue(focusParam.contains("\"business\":\"keios\""))
        assertTrue(focusParam.contains("\"notifyId\":\"38888\""))
        assertTrue(focusParam.contains("\"orderId\":\"mcp_keepalive\""))
    }

    @Test
    fun `first float follows user setting`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 511,
            action = "os.kei.test.OPEN_NOTIFICATION_FIRST_FLOAT"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            512,
            Intent("os.kei.test.STOP_MCP_FIRST_FLOAT").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = "KeiOS MCP",
                running = false,
                port = 8080,
                path = "/mcp",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                notificationId = 38889,
                miFocusOrderId = "mcp_keepalive_first_float"
            ),
            settings =
                UserSettings(
                    miIslandOuterGlow = true,
                    miIslandFirstFloat = false,
                ),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("\"islandFirstFloat\":false"))
        assertTrue(focusParam.contains("\"enableFloat\":true"))
    }

    @Test
    fun `finish float follows user setting`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 521,
            action = "os.kei.test.OPEN_NOTIFICATION_FINISH_FLOAT"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            522,
            Intent("os.kei.test.STOP_MCP_FINISH_FLOAT").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = "KeiOS MCP",
                running = false,
                port = 8080,
                path = "/mcp",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                notificationId = 38890,
                miFocusOrderId = "mcp_keepalive_finish_float"
            ),
            settings =
                UserSettings(
                    miIslandOuterGlow = true,
                    miIslandFirstFloat = false,
                    miIslandFinishFloat = false,
                ),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("\"islandFirstFloat\":false"))
        assertTrue(focusParam.contains("\"enableFloat\":false"))
    }

    @Test
    fun `ba ap progress island title uses current ap value`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 601,
            action = "os.kei.test.OPEN_BA_AP"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            602,
            Intent("os.kei.test.MARK_BA_AP_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = 128,
                path = "120",
                clients = 240,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(
            actual = focusParam.contains("\"title\":\"128\""),
            message = "AP progress island title should show current AP. focusParam=$focusParam"
        )
        assertTrue(focusParam.contains("progressTextInfo"))
        assertTrue(focusParam.contains("combinePicInfo"))
        assertTrue(focusParam.contains("\"progress\":53"))
        assertTrue(focusParam.contains("\"colorProgress\":\"#4DA3FF\""))
        assertFalse(focusParam.contains("multiProgressInfo"))
        assertTrue(focusParam.contains("\"actionBgColor\":\"#4DA3FF\""))
        assertTrue(focusParam.contains("\"enableFloat\":false"))
        assertFalse(focusParam.contains("\"actionBgColor\":\"#E25B6A\""))
    }

    @Test
    fun `ba ap first alert enables island float`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 611,
            action = "os.kei.test.OPEN_BA_AP_FIRST_ALERT"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            612,
            Intent("os.kei.test.MARK_BA_AP_FIRST_ALERT_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = 128,
                path = "120",
                clients = 240,
                ongoing = true,
                onlyAlertOnce = false,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("\"enableFloat\":true"))
        assertTrue(focusParam.contains("\"islandFirstFloat\":true"))
    }

    @Test
    fun `ba cafe visit event enables island float`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 621,
            action = "os.kei.test.OPEN_BA_CAFE_VISIT"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            622,
            Intent("os.kei.test.MARK_BA_CAFE_VISIT_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_CAFE_VISIT_SERVER_NAME,
                running = true,
                port = 0,
                path = "学生访问刷新",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = false,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("\"enableFloat\":true"))
        assertTrue(focusParam.contains("imageTextInfoRight"))
        assertTrue(focusParam.contains("\"highlightColor\":\"#4DA3FF\""))
    }

    @Test
    fun `ba arena refresh registers vector semantic icon and floatable event`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 631,
            action = "os.kei.test.OPEN_BA_ARENA_REFRESH"
        )
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            632,
            Intent("os.kei.test.MARK_BA_ARENA_REFRESH_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_ARENA_REFRESH_SERVER_NAME,
                running = true,
                port = 0,
                path = "日服 14:00 竞技场已刷新",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = false,
                openPendingIntent = openPendingIntent,
                stopPendingIntent = markReadPendingIntent,
                focusOpenPendingIntent = openPendingIntent,
                notificationId = 38891,
                miFocusOrderId = "bluearchive_arena_refresh-38891",
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val displayIcon = notification.focusPicture("key_logo_display")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(Icon.TYPE_RESOURCE, displayIcon.type)
        assertEquals(R.drawable.ic_ba_arena_coin_island, displayIcon.resId)
        assertTrue(focusParam.contains("\"enableFloat\":true"))
        assertTrue(focusParam.contains("\"islandFirstFloat\":true"))
        assertTrue(focusParam.contains("bluearchive_arena_refresh"))
        assertTrue(focusParam.contains("imageTextInfoRight"))
        assertTrue(focusParam.contains("\"highlightColor\":\"#4DA3FF\""))
    }

    @Test
    fun `calendar pool island uses countdown digit template and acknowledge action`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 701,
            action = "os.kei.test.OPEN_BA_CALENDAR_POOL"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            702,
            Intent("os.kei.test.MARK_BA_CALENDAR_POOL_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_CALENDAR_POOL_SERVER_NAME,
                running = true,
                port = 72,
                path = "Event starts soon",
                clients = 1,
                ongoing = true,
                onlyAlertOnce = false,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                secondaryActionLabel = "知道了",
                overrideTitle = "活动即将开始",
                overrideContent = "测试活动 将在 05-06 04:00 开始",
                overrideOnlineText = "开始",
                overrideShortText = "活动",
                overrideProgressPercent = 72,
                miFocusTitle = "活动即将开始",
                miFocusSpecialTitle = "日服",
                miFocusContent = "测试活动 · 05-06 04:00",
                deadlineAtMs = 1778007600000L
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusStopAction = notification.focusAction("mcp_action_stop")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(stopPendingIntent, focusStopAction.actionIntent)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(focusParam.contains("sameWidthDigitInfo"))
        assertTrue(focusParam.contains("\"content\":\"活动\""))
        assertTrue(focusParam.contains("\"timerType\":-1"))
        assertTrue(focusParam.contains("\"timerWhen\":1778007600000"))
        assertTrue(focusParam.contains("\"timerSystemCurrent\""))
        assertTrue(focusParam.contains("\"title\":\"活动即将开始\""))
        assertTrue(focusParam.contains("\"specialTitle\":\"日服\""))
        assertTrue(focusParam.contains("测试活动 · 05-06 04:00"))
        assertEquals("活动即将开始", notification.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(focusParam.contains("multiProgressInfo"))
        assertFalse(focusParam.contains("\"colorProgress\""))
        assertTrue(focusParam.contains("mcp_action_stop"))
        assertTrue(focusParam.contains("\"enableFloat\":true"))
    }

    @Test
    fun `calendar pool changed island uses compact terminal text`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 711,
            action = "os.kei.test.OPEN_BA_POOL_CHANGE"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            712,
            Intent("os.kei.test.MARK_BA_POOL_CHANGE_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_CALENDAR_POOL_SERVER_NAME,
                running = true,
                port = 0,
                path = "卡池变动 1 项",
                clients = 1,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                secondaryActionLabel = "知道了",
                overrideTitle = "日服卡池已更新",
                overrideContent = "卡池变动 1 项",
                overrideOnlineText = "卡池",
                overrideShortText = "更新",
                overrideProgressPercent = 0,
                deadlineAtMs = null
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals(stopPendingIntent, notification.deleteIntent)
        assertEquals(Notification.CATEGORY_STATUS, notification.category)
        assertTrue(focusParam.contains("imageTextInfoRight"))
        assertTrue(focusParam.contains("\"title\":\"更新\""))
        assertFalse(focusParam.contains("\"content\":\"卡池\""))
        assertTrue(focusParam.contains("\"enableFloat\":true"))
        assertFalse(focusParam.contains("progressTextInfo"))
        assertFalse(focusParam.contains("combinePicInfo"))
        assertFalse(focusParam.contains("multiProgressInfo"))
    }

    @Test
    fun `non ongoing webdav event stays floatable without promoted ongoing request`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 721,
            action = "os.kei.test.OPEN_WEBDAV_EVENT"
        )
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            722,
            Intent("os.kei.test.MARK_WEBDAV_EVENT_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
                running = true,
                port = 100,
                path = "sync",
                clients = 1,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = openPendingIntent,
                stopPendingIntent = markReadPendingIntent,
                focusOpenPendingIntent = openPendingIntent,
                overrideTitle = "WebDAV sync complete",
                overrideContent = "Synced 1/1",
                overrideOnlineText = "Complete",
                overrideShortText = "1/1",
                overrideProgressPercent = 100,
                miFocusTitle = "Sync",
                miFocusSpecialTitle = "Done",
                miFocusContent = "1/1",
                notificationId = 38891,
                miFocusOrderId = "webdav-sync"
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusJson = JSONObject(
            notification.extras.getString("miui.focus.param").orEmpty()
        ).getJSONObject("param_v2")

        assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertFalse(notification.extras.getBoolean("android.requestPromotedOngoing"))
        assertTrue(focusJson.getBoolean("enableFloat"))
        assertEquals("Sync", focusJson.getJSONObject("baseInfo").getString("title"))
        assertEquals("Done", focusJson.getJSONObject("baseInfo").getString("specialTitle"))
        assertEquals("1/1", focusJson.getJSONObject("baseInfo").getString("content"))
        assertEquals(
            "WebDAV sync complete",
            notification.extras.getString(Notification.EXTRA_TITLE),
        )
    }

    @Test
    fun `running webdav sync uses one continuous progress bar`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 731,
            action = "os.kei.test.OPEN_WEBDAV_RUNNING"
        )
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            732,
            Intent("os.kei.test.MARK_WEBDAV_RUNNING_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
                running = true,
                port = 40,
                path = "upload",
                clients = 5,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = openPendingIntent,
                stopPendingIntent = markReadPendingIntent,
                overrideTitle = "WebDAV sync",
                overrideContent = "Uploading 2/5",
                overrideOnlineText = "Upload",
                overrideShortText = "2/5",
                overrideProgressPercent = 40
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("progressTextInfo"))
        assertTrue(focusParam.contains("combinePicInfo"))
        assertTrue(focusParam.contains("\"colorProgress\":\"#2563EB\""))
        assertTrue(focusParam.contains("\"progress\":40"))
        assertFalse(focusParam.contains("multiProgressInfo"))
    }

    @Test
    fun `github share import island uses progress and notification action labels`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 801,
            action = "os.kei.test.OPEN_GITHUB_SHARE_IMPORT"
        )
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            802,
            Intent("os.kei.test.CANCEL_GITHUB_SHARE_IMPORT").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appIconBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
                running = true,
                port = 72,
                path = "owner/repo · demo.app · exact match · 12 min left",
                clients = 1,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = cancelPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                primaryActionLabel = "Check install",
                secondaryActionLabel = "Cancel linkage",
                showSecondaryActionWhenStopped = true,
                overrideTitle = "Waiting for install",
                overrideContent = "owner/repo · demo.app · exact match · 12 min left",
                overrideOnlineText = "Install",
                overrideShortText = "Install",
                overrideProgressPercent = 72
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            ),
            semanticIconBitmap = appIconBitmap
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusOpenAction = notification.focusAction("mcp_action_open")
        val focusStopAction = notification.focusAction("mcp_action_stop")
        val focusDisplayIcon = notification.focusPicture("key_logo_display")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(notificationOpenPendingIntent, focusOpenAction.actionIntent)
        assertEquals(cancelPendingIntent, focusStopAction.actionIntent)
        assertEquals("Check install", focusOpenAction.title.toString())
        assertEquals("Cancel linkage", focusStopAction.title.toString())
        assertTrue(focusParam.contains("\"title\":\"Install\""))
        assertTrue(focusParam.contains("progressTextInfo"))
        assertTrue(focusParam.contains("combinePicInfo"))
        assertTrue(focusParam.contains("\"colorReach\":\"#2563EB\""))
        assertTrue(focusParam.contains("\"colorProgress\":\"#2563EB\""))
        assertFalse(focusParam.contains("multiProgressInfo"))
        assertTrue(focusParam.contains("\"highlightColor\":\"#2563EB\""))
        assertTrue(focusParam.contains("\"showHighlightColor\":true"))
        assertTrue(focusParam.contains("\"colorContent\":\"#475569\""))
        assertTrue(focusParam.contains("\"actionBgColor\":\"#2563EB\""))
        assertTrue(focusParam.contains("\"actionBgColor\":\"#E25B6A\""))
        assertTrue(focusParam.contains("\"actionBgColorDark\":\"#FF6B7C\""))
        assertTrue(focusParam.contains("\"title\":\"Install\""))
        assertTrue(focusParam.contains("demo.app"))
        assertTrue(focusParam.contains("\"progress\":72"))
        assertTrue(focusParam.contains("\"picDark\":\"key_logo_display\""))
        val renderedBitmap = Shadows.shadowOf(focusDisplayIcon).bitmap
        assertNotNull(renderedBitmap)
        assertEquals(appIconBitmap.width, renderedBitmap.width)
        assertEquals(appIconBitmap.height, renderedBitmap.height)
    }

    @Test
    fun `github share import direct install action uses light blue secondary button`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 806,
            action = "os.kei.test.OPEN_GITHUB_SHARE_IMPORT_DIRECT_INSTALL"
        )
        val sendInstallPendingIntent = PendingIntent.getBroadcast(
            context,
            807,
            Intent("os.kei.test.SEND_GITHUB_SHARE_IMPORT_INSTALL")
                .setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
                running = true,
                port = 32,
                path = "owner/repo · asset ready",
                clients = 1,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = sendInstallPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                primaryActionLabel = "Open flow",
                secondaryActionLabel = context.getString(
                    R.string.github_share_import_notify_action_send_install
                ),
                showSecondaryActionWhenStopped = true,
                overrideTitle = "Asset ready",
                overrideContent = "owner/repo · asset ready",
                overrideOnlineText = "APK",
                overrideShortText = "APK",
                overrideProgressPercent = 32
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("\"actionBgColor\":\"#2563EB\""))
        assertTrue(focusParam.contains("\"actionBgColor\":\"#DBEAFE\""))
        assertTrue(focusParam.contains("\"actionBgColorDark\":\"#1E3A8A\""))
        assertTrue(focusParam.contains("\"actionTitleColor\":\"#1D4ED8\""))
        assertTrue(focusParam.contains("\"actionTitleColorDark\":\"#DBEAFE\""))
        assertFalse(focusParam.contains("\"actionBgColor\":\"#E25B6A\""))
    }

    @Test
    fun `github share import success island uses compact completed text`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 811,
            action = "os.kei.test.OPEN_GITHUB_SHARE_IMPORT_SUCCESS"
        )
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            812,
            Intent("os.kei.test.MARK_GITHUB_SHARE_IMPORT_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = LiveNotificationPayload(
                serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
                running = true,
                port = 100,
                path = "Demo was added to owner/repo tracking",
                clients = 0,
                ongoing = true,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = markReadPendingIntent,
                focusOpenPendingIntent = notificationOpenPendingIntent,
                primaryActionLabel = "View tracking",
                secondaryActionLabel = "Mark read",
                showSecondaryActionWhenStopped = true,
                overrideTitle = "GitHub tracking added",
                overrideContent = "Demo was added to owner/repo tracking",
                overrideOnlineText = "Tracked",
                overrideShortText = "Tracked",
                overrideProgressPercent = 100
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            ),
            miIslandProgressColorOverride = "#22C55E"
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertTrue(focusParam.contains("imageTextInfoRight"))
        assertTrue(focusParam.contains("\"title\":\"Tracked\""))
        assertFalse(focusParam.contains("progressTextInfo"))
        assertFalse(focusParam.contains("combinePicInfo"))
        assertEquals("#22C55E".toColorInt(), notification.color)
        assertTrue(focusParam.contains("\"highlightColor\":\"#22C55E\""))
        assertTrue(focusParam.contains("\"showHighlightColor\":true"))
        assertTrue(focusParam.contains("\"actionBgColor\":\"#2563EB\""))
        assertFalse(focusParam.contains("\"actionTitle\":\"Mark read\",\"actionBgColor\""))
        assertFalse(focusParam.contains("\"actionBgColor\":\"#E25B6A\""))
        assertTrue(focusParam.contains("mcp_action_open"))
        assertTrue(focusParam.contains("mcp_action_stop"))
    }

    private fun buildOpenPendingIntent(
        context: Application,
        requestCode: Int,
        action: String
    ): PendingIntent {
        val intent = Intent().apply {
            setPackage(context.packageName)
            setAction(action)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("target_bottom_page", "mcp")
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun Notification.focusAction(key: String): Notification.Action {
        val actions = extras.getBundle("miui.focus.actions")
        assertNotNull(actions, "Focus actions bundle should be present")
        return actions.getActionCompat(key)
    }

    private fun Notification.focusPicture(key: String): Icon {
        val pics = extras.getBundle("miui.focus.pics")
        assertNotNull(pics, "Focus pictures bundle should be present")
        return pics.getPictureCompat(key)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getActionCompat(key: String): Notification.Action {
        return getParcelable<Notification.Action>(key)
            ?: error("Missing focus action: $key")
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getPictureCompat(key: String): Icon {
        return getParcelable<Icon>(key)
            ?: error("Missing focus picture: $key")
    }
}

class MiIslandNotificationBuilderTestApp : Application()
