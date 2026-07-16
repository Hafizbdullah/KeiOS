package os.kei.ui.page.main.widget

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@ReadOnlyComposable
fun isAppInDarkTheme(): Boolean =
    resolveAppDarkTheme(
        colorSchemeMode = MiuixTheme.colorSchemeMode,
        systemInDarkTheme = isSystemInDarkTheme(),
    )

internal fun resolveAppDarkTheme(
    colorSchemeMode: ColorSchemeMode?,
    systemInDarkTheme: Boolean,
): Boolean =
    when (colorSchemeMode) {
        ColorSchemeMode.Light,
        ColorSchemeMode.MonetLight,
        -> false

        ColorSchemeMode.Dark,
        ColorSchemeMode.MonetDark,
        -> true

        ColorSchemeMode.System,
        ColorSchemeMode.MonetSystem,
        null,
        -> systemInDarkTheme
    }
