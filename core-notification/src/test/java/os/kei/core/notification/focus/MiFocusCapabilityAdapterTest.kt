package os.kei.core.notification.focus

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiFocusCapabilityAdapterTest {
    @Test
    fun `island property parser accepts Android system true values`() {
        listOf("1", "y", "yes", "true", "on", " TRUE ").forEach { value ->
            assertTrue(parseMiFocusBooleanProperty(value), "Expected true for $value")
        }
        listOf("0", "n", "no", "false", "off", "unknown", "").forEach { value ->
            assertFalse(parseMiFocusBooleanProperty(value), "Expected false for $value")
        }
    }
}
