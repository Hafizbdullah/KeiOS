package os.kei.core.ui.effect

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals

class AppTopBarColorTest {
    @Test
    fun backdropTopBarsStayTransparentInLightAndDarkThemes() {
        listOf(
            Color(0xFFFAFAFC),
            Color(0xFF1A1B1E),
        ).forEach { surface ->
            assertEquals(
                Color.Transparent,
                appTopBarColor(
                    surfaceColor = surface,
                    enableBackdropEffects = true,
                ),
            )
        }
    }

    @Test
    fun plainTopBarsStayTransparentInLightAndDarkThemes() {
        listOf(
            Color(0xFFFAFAFC),
            Color(0xFF1A1B1E),
        ).forEach { surface ->
            assertEquals(
                Color.Transparent,
                appTopBarColor(
                    surfaceColor = surface,
                    enableBackdropEffects = false,
                ),
            )
        }
    }
}
