package os.kei.ui.page.main.settings.state

import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv

internal enum class SettingsCardExpansionId(
    val storageKey: String,
    val defaultExpanded: Boolean,
) {
    Permissions("permissions", true),
    KeepAlive("keep_alive", true),
    AccessibilityGuardPolicy("accessibility_guard_policy", true),
    AccessibilityGuardServices("accessibility_guard_services", true),
    AccessibilityGuardHistory("accessibility_guard_history", false),
    ThemeLanguage("theme_language", true),
    Performance("performance", false),
    HomeEffects("home_effects", false),
    BackgroundAsset("background_asset", false),
    BackgroundLayout("background_layout", false),
    BackgroundRendering("background_rendering", false),
    PageMotion("page_motion", true),
    LiquidControls("liquid_controls", false),
    Interaction("interaction", false),
    Notifications("notifications", false),
    CopySelection("copy_selection", true),
    WebDavSync("webdav_sync", false),
    CacheDiagnostics("cache_diagnostics", false),
    CacheItems("cache_items", false),
    LogLevel("log_level", false),
    LogFiles("log_files", false),
}

internal object SettingsCardExpansionStore {
    private const val KV_ID = "settings_card_expansion"
    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    fun loadSnapshot(): Map<SettingsCardExpansionId, Boolean> =
        SettingsCardExpansionId.entries.associateWith { id ->
            store.decodeBool(id.storageKey, id.defaultExpanded)
        }

    fun setExpanded(
        id: SettingsCardExpansionId,
        expanded: Boolean,
    ) {
        store.encode(id.storageKey, expanded)
    }
}

internal fun Map<SettingsCardExpansionId, Boolean>.isSettingsCardExpanded(id: SettingsCardExpansionId): Boolean =
    this[id] ?: id.defaultExpanded
