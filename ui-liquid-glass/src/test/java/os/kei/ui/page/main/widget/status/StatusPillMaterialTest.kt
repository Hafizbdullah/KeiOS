package os.kei.ui.page.main.widget.status

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusPillMaterialTest {
    @Test
    fun lightContentKeepsTheAccentFresh() {
        val content = statusPillContentColor(isDark = false, accent = Color(0xFF60A5FA))

        assertTrue(content.alpha > 0.95f)
        assertEquals(Color(0xFF60A5FA).red, content.red)
        assertEquals(Color(0xFF60A5FA).green, content.green)
        assertEquals(Color(0xFF60A5FA).blue, content.blue)
    }

    @Test
    fun darkContentKeepsTheOriginalAccent() {
        val accent = Color(0xFF22C55E)

        assertEquals(accent, statusPillContentColor(isDark = true, accent = accent))
    }
}
