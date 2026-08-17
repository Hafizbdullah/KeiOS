package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaNativeBgmMediaNotificationPrefsTest {
    @Test
    fun `native BGM media notification defaults on and persists`() {
        val store = FakeKeyValueStore()
        val prefs = BaNativeBgmMediaNotificationPrefs(store)

        assertTrue(prefs.loadEnabled())

        prefs.saveEnabled(false)
        assertFalse(prefs.loadEnabled())

        prefs.saveEnabled(true)
        assertTrue(prefs.loadEnabled())
    }

    /**
     * The default applies to an absent key only.
     *
     * This is the whole reason the flip is safe to ship: MMKV consults [BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT]
     * when nothing has been written, so someone who deliberately turned the switch off keeps it off across the
     * update rather than having it silently turned back on.
     */
    @Test
    fun `an explicit off survives the default being on`() {
        val store = FakeKeyValueStore()
        BaNativeBgmMediaNotificationPrefs(store).saveEnabled(false)

        assertFalse(BaNativeBgmMediaNotificationPrefs(store).loadEnabled())
    }

    @Test
    fun `native BGM media notification uses stable preference key`() {
        val store = FakeKeyValueStore()
        val prefs = BaNativeBgmMediaNotificationPrefs(store)

        prefs.saveEnabled(true)

        assertTrue(store.values[BA_NATIVE_BGM_MEDIA_NOTIFICATION_KEY] == true)
    }

    private class FakeKeyValueStore : BaNativeBgmMediaNotificationKeyValueStore {
        val values = mutableMapOf<String, Boolean>()

        override fun decodeBool(key: String, defaultValue: Boolean): Boolean {
            return values[key] ?: defaultValue
        }

        override fun encode(key: String, value: Boolean) {
            values[key] = value
        }
    }
}
