package os.kei.ui.page.main.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import os.kei.core.prefs.AppThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

internal fun Context.launchDebugActivity(activityClass: Class<out Activity>) {
    val hostActivity = findHostActivity()
    val intent = Intent(this, activityClass).apply {
        if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (hostActivity != null) {
        hostActivity.startActivity(intent)
    } else {
        startActivity(intent)
    }
}

internal fun ComponentActivity.enableDebugEdgeToEdge(appThemeMode: AppThemeMode) {
    val transparent = Color.TRANSPARENT
    val systemBarStyle =
        when (appThemeMode) {
            AppThemeMode.FOLLOW_SYSTEM -> SystemBarStyle.auto(transparent, transparent)
            AppThemeMode.LIGHT -> SystemBarStyle.light(transparent, transparent)
            AppThemeMode.DARK -> SystemBarStyle.dark(transparent)
        }

    enableEdgeToEdge(
        statusBarStyle = systemBarStyle,
        navigationBarStyle = systemBarStyle,
    )
}

@Composable
internal fun DebugActivityTheme(
    appThemeMode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode =
        when (appThemeMode) {
            AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
            AppThemeMode.LIGHT -> ColorSchemeMode.Light
            AppThemeMode.DARK -> ColorSchemeMode.Dark
        }

    MiuixTheme(controller = ThemeController(colorSchemeMode)) {
        content()
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}
