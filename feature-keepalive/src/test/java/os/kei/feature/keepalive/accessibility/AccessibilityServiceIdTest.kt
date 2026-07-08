package os.kei.feature.keepalive.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccessibilityServiceIdTest {
    @Test
    fun `parser returns empty set for blank setting`() {
        assertTrue(parseAccessibilityServiceIds("").isEmpty())
        assertTrue(parseAccessibilityServiceIds("  ").isEmpty())
    }

    @Test
    fun `parser reads colon separated flattened component names`() {
        val parsed =
            parseAccessibilityServiceIds(
                "com.example.alpha/.AlphaService:com.example.beta/com.example.beta.BetaService",
            ).toList()

        assertEquals(
            listOf(
                AccessibilityServiceId("com.example.alpha", "com.example.alpha.AlphaService"),
                AccessibilityServiceId("com.example.beta", "com.example.beta.BetaService"),
            ),
            parsed,
        )
    }

    @Test
    fun `parser ignores duplicate and malformed entries`() {
        val parsed =
            parseAccessibilityServiceIds(
                "broken:com.example/.Service:com.example/.Service:/missing/package",
            )

        assertEquals(
            setOf(AccessibilityServiceId("com.example", "com.example.Service")),
            parsed,
        )
    }

    @Test
    fun `flatten keeps full component identity`() {
        val id = AccessibilityServiceId("com.example", "com.example.Service")

        assertEquals("com.example/com.example.Service", id.flatten())
    }

}
