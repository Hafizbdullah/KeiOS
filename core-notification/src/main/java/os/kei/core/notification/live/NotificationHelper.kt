package os.kei.core.notification.live

import android.content.Context
import android.os.Build
import com.xzakota.hyper.notification.focus.util.FocusUtils
import os.kei.core.platform.AndroidPlatformVersions
import os.kei.core.system.findPropString
import os.kei.core.notification.live.builder.NotificationRenderStyle
import java.util.Locale

class NotificationHelper(
    val context: Context,
    private val channels: LiveNotificationChannels = LiveNotificationChannels.DefaultMcp
) {
    val isHyperOS: Boolean by lazy {
        findPropString("ro.mi.os.version.name").startsWith("OS")
    }

    val preferOemLiveIconLayout: Boolean by lazy {
        isHyperOS || isColorOsFamily()
    }

    val isSupportMiIsland: Boolean by lazy {
        runCatching {
            FocusUtils.getFocusProtocolVersion(context) == 3
        }.getOrDefault(false)
    }

    val hasMiIslandPermission: Boolean by lazy {
        runCatching {
            FocusUtils.hasFocusPermission(context)
        }.getOrDefault(false)
    }

    val isMiIslandAvailable: Boolean
        get() = isHyperOS && isSupportMiIsland && hasMiIslandPermission

    val isModernLiveUpdateEligible: Boolean
        get() = AndroidPlatformVersions.isAtLeastAndroid16

    fun resolveChannel(style: NotificationRenderStyle): String {
        return when (style) {
            NotificationRenderStyle.MI_ISLAND -> channels.islandChannelId
            NotificationRenderStyle.LIVE_UPDATE -> channels.liveUpdateChannelId
            NotificationRenderStyle.LEGACY -> channels.legacyChannelId
        }
    }

    private fun isColorOsFamily(): Boolean {
        val buildFields = listOf(
            Build.BRAND,
            Build.MANUFACTURER,
            Build.DISPLAY
        ).joinToString(separator = " ").lowercase(Locale.ROOT)
        if (
            listOf("oppo", "oneplus", "realme", "coloros", "oplus")
                .any(buildFields::contains)
        ) {
            return true
        }
        if (listOf(
            "ro.build.version.opporom",
            "ro.build.version.oplusrom",
            "ro.build.version.realmeui",
            "ro.oplus.version"
        ).any { key -> findPropString(key).isNotBlank() }) {
            return true
        }
        val romVersion = findPropString("ro.rom.version").lowercase(Locale.ROOT)
        return listOf("oppo", "oneplus", "realme", "coloros", "oplus")
            .any(romVersion::contains)
    }
}

data class LiveNotificationChannels(
    val islandChannelId: String,
    val liveUpdateChannelId: String,
    val legacyChannelId: String = islandChannelId
) {
    companion object {
        val DefaultMcp = LiveNotificationChannels(
            islandChannelId = "mcp_keepalive_channel_v2",
            liveUpdateChannelId = "mcp_live_update_channel_v1"
        )
    }
}
