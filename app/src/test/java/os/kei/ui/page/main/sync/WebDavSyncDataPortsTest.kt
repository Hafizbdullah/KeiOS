package os.kei.ui.page.main.sync

import org.junit.Test
import kotlin.test.assertEquals

class WebDavSyncDataPortsTest {
    @Test
    fun `BA account fingerprint revision changes with reminder setting schema`() {
        assertEquals(3, BA_ACCOUNTS_FINGERPRINT_REVISION)
    }
}
