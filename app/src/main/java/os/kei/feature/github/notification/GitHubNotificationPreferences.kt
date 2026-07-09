package os.kei.feature.github.notification

import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.UiPrefs

internal object GitHubNotificationPreferences {
    @Volatile
    private var superIslandFloatBehaviorOverride: SuperIslandFloatBehavior? = null

    fun superIslandFloatBehavior(): SuperIslandFloatBehavior =
        superIslandFloatBehaviorOverride
            ?: runCatching {
                UiPrefs.getSuperIslandFloatBehavior(
                    defaultValue = SuperIslandFloatBehavior.StartAndFinish,
                )
            }.getOrDefault(SuperIslandFloatBehavior.StartAndFinish)

    fun isSuperIslandFirstFloatEnabled(): Boolean =
        superIslandFloatBehavior().firstFloatEnabled

    fun isSuperIslandFinishFloatEnabled(): Boolean =
        superIslandFloatBehavior().finishFloatEnabled

    fun overrideSuperIslandFirstFloatForTests(value: Boolean?) {
        superIslandFloatBehaviorOverride =
            value?.let {
                if (it) {
                    SuperIslandFloatBehavior.StartAndFinish
                } else {
                    SuperIslandFloatBehavior.SummaryOnly
                }
            }
    }

    fun overrideSuperIslandFloatBehaviorForTests(value: SuperIslandFloatBehavior?) {
        superIslandFloatBehaviorOverride = value
    }
}
