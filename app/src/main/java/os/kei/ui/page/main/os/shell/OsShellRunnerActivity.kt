package os.kei.ui.page.main.os.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.core.privilege.PrivilegedShell
import os.kei.ui.page.main.back.ProvideBackNavigationRuntime
import os.kei.ui.page.main.os.shell.page.OsShellRunnerPage
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundHost
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyles
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.sheet.LocalLiquidSheetEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory

class OsShellRunnerActivity : ComponentActivity() {
    private var canRunShellCommand by mutableStateOf(false)
    private val privilegedShell = PrivilegedShell()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        privilegedShell.attach { refreshShellCommandReadyState() }

        setContent {
            val shellRunnerViewModel: OsShellRunnerViewModel = viewModel()
            LaunchedEffect(shellRunnerViewModel) {
                shellRunnerViewModel.refreshChromePrefs()
            }
            val chromePrefs by shellRunnerViewModel.chromePrefs.collectAsStateWithLifecycle()
            val liquidSheetEnabled = UiPrefs.isLiquidSheetEnabled()
            val colorSchemeMode = when (chromePrefs.appThemeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                AppThemeMode.LIGHT -> ColorSchemeMode.Light
                AppThemeMode.DARK -> ColorSchemeMode.Dark
            }
            val controller = ThemeController(colorSchemeMode)
            val predictiveBackPolicy = PredictiveBackOemCompat.currentPolicy(
                transitionAnimationsEnabled = chromePrefs.transitionAnimationsEnabled,
                predictiveBackAnimationsEnabled = chromePrefs.predictiveBackAnimationsEnabled
            )

            MiuixTheme(controller = controller) {
                ProvideBackNavigationRuntime(policy = predictiveBackPolicy) {
                    CompositionLocalProvider(
                        LocalOverscrollFactory provides MiuixOverscrollFactory,
                        LocalTransitionAnimationsEnabled provides chromePrefs.transitionAnimationsEnabled,
                        LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled,
                        LocalLiquidSheetEnabled provides liquidSheetEnabled,
                    ) {
                        AppManagedBackgroundHost(
                            enabled = chromePrefs.nonHomeBackgroundEnabled,
                            imageUri = chromePrefs.nonHomeBackgroundUri,
                            opacity = chromePrefs.nonHomeBackgroundOpacity,
                            contentScale = chromePrefs.nonHomeBackgroundContentScale,
                            alignment = chromePrefs.nonHomeBackgroundAlignment,
                            pageStyle = chromePrefs.nonHomeBackgroundPageStyle,
                            scrim = chromePrefs.nonHomeBackgroundScrim,
                            style = AppManagedBackgroundStyles.FocusedTask,
                            exportBackdropToContent = true,
                        ) {
                            OsShellRunnerPage(
                                canRunShellCommand = canRunShellCommand,
                                onRequestPrivilegeAccess = {
                                    privilegedShell.requestAccessIfNeeded()
                                    refreshShellCommandReadyState()
                                },
                                onRunShellCommand = { command, timeoutMs, onOutput ->
                                    privilegedShell.execCommandCancellableStreaming(
                                        command = command,
                                        timeoutMs = timeoutMs
                                    ) { output -> onOutput(output) }
                                },
                                onClose = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        privilegedShell.detach()
        super.onDestroy()
    }

    private fun refreshShellCommandReadyState() {
        canRunShellCommand = privilegedShell.canUseCommand()
    }

    companion object {
        fun launch(context: Context) {
            val hostActivity = context.findHostActivity()
            val intent = Intent(context, OsShellRunnerActivity::class.java).apply {
                if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (hostActivity != null) {
                hostActivity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}
