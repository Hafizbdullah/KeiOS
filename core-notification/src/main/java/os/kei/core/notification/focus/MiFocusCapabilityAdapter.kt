package os.kei.core.notification.focus

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import os.kei.core.system.findPropString

/** Local HyperOS capability probe used before building a Focus Notification payload. */
internal object MiFocusCapabilityAdapter {
    private const val FOCUS_PROTOCOL_SETTING = "notification_focus_protocol"
    private const val ISLAND_FEATURE_PROPERTY = "persist.sys.feature.island"
    private val focusPermissionUri = Uri.parse("content://miui.statusbar.notification.public")

    fun getFocusProtocolVersion(context: Context): Int =
        Settings.System.getInt(context.contentResolver, FOCUS_PROTOCOL_SETTING, 0)

    fun isSupportIsland(): Boolean =
        parseMiFocusBooleanProperty(findPropString(ISLAND_FEATURE_PROPERTY, "false"))

    fun hasFocusPermission(context: Context): Boolean = runCatching {
        val extras = Bundle().apply { putString("package", context.packageName) }
        context.contentResolver.call(focusPermissionUri, "canShowFocus", null, extras)
            ?.getBoolean("canShowFocus", false) == true
    }.getOrDefault(false)
}

internal fun parseMiFocusBooleanProperty(value: String): Boolean =
    when (value.trim().lowercase()) {
        "1", "y", "yes", "true", "on" -> true
        else -> false
    }
