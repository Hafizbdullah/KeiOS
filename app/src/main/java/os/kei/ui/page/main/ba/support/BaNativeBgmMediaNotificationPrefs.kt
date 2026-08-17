package os.kei.ui.page.main.ba.support

internal const val BA_NATIVE_BGM_MEDIA_NOTIFICATION_KEY =
    "native_bgm_media_notification_enabled"

/**
 * Whether BGM plays through the Media3 session by default.
 *
 * On since the Android 17 audio-hardening pass. The two backends differ in more than a notification:
 * `Lightweight` has no `MediaSession`, so the system has nothing to route media keys or the shade's transport
 * controls to, and the page's own lifecycle pauses playback when it stops. `NativeMedia` runs a foreground
 * service that keeps playing while the app is backgrounded, which is what someone playing background music
 * asks for.
 *
 * It stayed off originally because the hardened-audio path was untested. It has since been driven on API 37
 * with `media_session set-enable-hardening 1` — playback survives backgrounding at `mutedState:none`, and a
 * `play` dispatched from the background is honoured, because Media3 starts the service while the app is `TOP`
 * and the FGS therefore carries the While-In-Use capability the hardening requires. See
 * `docs/planning/android-17-adaptation.md`.
 *
 * MMKV only falls back to this when the key is absent, so anyone who has already turned the switch off keeps
 * it off; this changes the default, not their choice.
 */
internal const val BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT = true

internal interface BaNativeBgmMediaNotificationKeyValueStore {
    fun decodeBool(key: String, defaultValue: Boolean): Boolean
    fun encode(key: String, value: Boolean)
}

internal class BaNativeBgmMediaNotificationPrefs(
    private val store: BaNativeBgmMediaNotificationKeyValueStore
) {
    fun loadEnabled(): Boolean {
        return store.decodeBool(
            BA_NATIVE_BGM_MEDIA_NOTIFICATION_KEY,
            BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT,
        )
    }

    fun saveEnabled(enabled: Boolean) {
        store.encode(BA_NATIVE_BGM_MEDIA_NOTIFICATION_KEY, enabled)
    }
}
