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

@Suppress("UNUSED_PARAMETER")
internal fun appTopBarColor(
    surfaceColor: Color,
    enableBackdropEffects: Boolean,
): Color = Color.Transparent
