package os.kei.feature.keepalive.accessibility

import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv

data class AccessibilityGuardSettings(
    val daemonEnabled: Boolean = false,
    val bootCheckEnabled: Boolean = false,
    val screenOnCheckEnabled: Boolean = false,
)

data class AccessibilityGuardSnapshot(
    val settings: AccessibilityGuardSettings,
    val capability: AccessibilityGuardCapability,
)

interface AccessibilityGuardStateStore {
    fun loadSettings(): AccessibilityGuardSettings

    fun saveSettings(settings: AccessibilityGuardSettings)
}

class AccessibilityGuardStore(
    private val store: MMKV = KeiMmkv.byId(KV_ID),
) : AccessibilityGuardStateStore {
    override fun loadSettings(): AccessibilityGuardSettings =
        AccessibilityGuardSettings(
            daemonEnabled = store.decodeBool(KEY_DAEMON_ENABLED, false),
            bootCheckEnabled = store.decodeBool(KEY_BOOT_CHECK_ENABLED, false),
            screenOnCheckEnabled = store.decodeBool(KEY_SCREEN_ON_CHECK_ENABLED, false),
        )

    override fun saveSettings(settings: AccessibilityGuardSettings) {
        store.encode(KEY_DAEMON_ENABLED, settings.daemonEnabled)
        store.encode(KEY_BOOT_CHECK_ENABLED, settings.bootCheckEnabled)
        store.encode(KEY_SCREEN_ON_CHECK_ENABLED, settings.screenOnCheckEnabled)
        store.removeValuesForKeys(
            arrayOf(
                KEY_GUARDED_IDS,
                KEY_COOLDOWN_UNTIL_BY_ID,
                KEY_FAILURE_COUNT_BY_ID,
            ),
        )
    }

    companion object {
        private const val KV_ID = "accessibility_guard"
        private const val KEY_GUARDED_IDS = "guarded_ids"
        private const val KEY_DAEMON_ENABLED = "daemon_enabled"
        private const val KEY_BOOT_CHECK_ENABLED = "boot_restore_enabled"
        private const val KEY_SCREEN_ON_CHECK_ENABLED = "screen_on_check_enabled"
        private const val KEY_COOLDOWN_UNTIL_BY_ID = "cooldown_until_by_id"
        private const val KEY_FAILURE_COUNT_BY_ID = "failure_count_by_id"
    }
}
