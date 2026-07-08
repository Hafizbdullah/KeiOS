package os.kei.core.notification.live

import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.core.notification.live.SessionNotifier
import os.kei.core.notification.live.builder.EnvironmentContext
import os.kei.core.notification.live.builder.LegacyNotificationBuilder
import os.kei.core.notification.live.builder.MiIslandNotificationBuilder
import os.kei.core.notification.live.builder.ModernNotificationBuilder
import os.kei.core.notification.live.builder.NotificationPayload
import os.kei.core.notification.live.builder.NotificationRenderStyle
import os.kei.core.notification.live.builder.UserSettings

class SessionNotifierImpl(
    private val helper: NotificationHelper
) : SessionNotifier {
    private companion object {
        private const val TAG = "McpSessionNotifier"
    }

    private val modernBuilder by lazy { ModernNotificationBuilder(helper.context) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(helper.context) }
    private val miIslandBuilder by lazy { MiIslandNotificationBuilder(helper.context) }

    override fun build(payload: LiveNotificationPayload): SessionNotifier.NotificationBuildResult {
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val bypassRestriction = UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        val style = resolveStyle(preferSuperIsland = preferSuperIsland)
        AppLogger.i(
            TAG,
            "build preferSuperIsland=$preferSuperIsland supportMiIsland=${helper.isSupportMiIsland} " +
                "focusPermission=${helper.hasMiIslandPermission} style=$style bypass=$bypassRestriction"
        )
        val wrapped = NotificationPayload(
            state = payload,
            settings = UserSettings(miIslandOuterGlow = payload.outerGlow),
            environment = EnvironmentContext(
                channelId = helper.resolveChannel(style),
                isHyperOS = helper.isHyperOS,
                preferOemLiveIconLayout = helper.preferOemLiveIconLayout
            )
        )
        val notification = when (style) {
            NotificationRenderStyle.MI_ISLAND -> miIslandBuilder.build(wrapped)
            NotificationRenderStyle.LIVE_UPDATE -> {
                if (helper.isModernLiveUpdateEligible) modernBuilder.build(wrapped) else legacyBuilder.build(wrapped)
            }
            NotificationRenderStyle.LEGACY -> legacyBuilder.build(wrapped)
        }
        return SessionNotifier.NotificationBuildResult(
            notification = notification,
            style = style,
            useXiaomiMagic = style == NotificationRenderStyle.MI_ISLAND && bypassRestriction
        )
    }

    private fun resolveStyle(preferSuperIsland: Boolean): NotificationRenderStyle {
        if (preferSuperIsland && helper.isSupportMiIsland) {
            return NotificationRenderStyle.MI_ISLAND
        }
        return NotificationRenderStyle.LIVE_UPDATE
    }
}
