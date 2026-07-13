package os.kei.core.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberAppTopBarColor(enableBackdropEffects: Boolean): Color =
    appTopBarColor(
        surfaceColor = MiuixTheme.colorScheme.surface,
        enableBackdropEffects = enableBackdropEffects,
    )

internal fun appTopBarColor(
    surfaceColor: Color,
    enableBackdropEffects: Boolean,
): Color =
    if (enableBackdropEffects) {
        surfaceColor.copy(alpha = 0.96f)
    } else {
        surfaceColor
    }
