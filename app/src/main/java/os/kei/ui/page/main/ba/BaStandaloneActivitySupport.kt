package os.kei.ui.page.main.ba

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.back.ProvideBackNavigationRuntime
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory

@Composable
internal fun BaStandaloneActivityTheme(content: @Composable () -> Unit) {
    val transitionAnimationsEnabled = UiPrefs.isTransitionAnimationsEnabled()
    val liquidControlsEnabled = UiPrefs.isLiquidSwitchEnabled()
    val predictiveBackPolicy = PredictiveBackOemCompat.currentPolicy(
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        predictiveBackAnimationsEnabled = UiPrefs.isPredictiveBackAnimationsEnabled()
    )
    val colorSchemeMode = when (UiPrefs.getAppThemeMode()) {
        AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
        AppThemeMode.LIGHT -> ColorSchemeMode.Light
        AppThemeMode.DARK -> ColorSchemeMode.Dark
    }

    MiuixTheme(controller = ThemeController(colorSchemeMode)) {
        ProvideBackNavigationRuntime(policy = predictiveBackPolicy) {
            CompositionLocalProvider(
                LocalOverscrollFactory provides MiuixOverscrollFactory,
                LocalTransitionAnimationsEnabled provides transitionAnimationsEnabled,
                LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled,
                LocalLiquidControlsEnabled provides liquidControlsEnabled,
            ) {
                content()
            }
        }
    }
}

internal tailrec fun Context.findBaHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findBaHostActivity()
        else -> null
    }
}
