package os.kei.core.ui.effect

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals

class AppTopBarColorTest {
    @Test
    fun backdropTopBarKeepsAnOpaqueReadableScrim() {
        val surface = Color(0xFF1A1B1E)

        val resolved =
            appTopBarColor(
                surfaceColor = surface,
                enableBackdropEffects = true,
            )

        assertEquals(surface.copy(alpha = 0.96f), resolved)
    }

    @Test
    fun plainTopBarUsesTheThemeSurfaceDirectly() {
        val surface = Color(0xFFFAFAFC)

        val resolved =
            appTopBarColor(
                surfaceColor = surface,
                enableBackdropEffects = false,
            )

        assertEquals(surface, resolved)
    }
}
