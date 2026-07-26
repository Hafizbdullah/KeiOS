@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.widget.glass.AppLiquidWindowBoundary
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects
import top.yukonga.miuix.kmp.utils.WindowNavigationEventScope
import top.yukonga.miuix.kmp.utils.platformDialogProperties
import top.yukonga.miuix.kmp.window.WindowDialog

enum class AppWindowDialogPresentation {
    Card,
    Fullscreen,
}

@Composable
fun AppWindowDialogHost(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    dismissible: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    presentation: AppWindowDialogPresentation = AppWindowDialogPresentation.Card,
    content: @Composable () -> Unit,
) {
    if (presentation == AppWindowDialogPresentation.Card) {
        if (UiPrefs.isLiquidDialogEnabled()) {
            LiquidGlassDialog(
                show = show,
                modifier = modifier,
                title = title,
                summary = summary,
                onDismissRequest = onDismissRequest,
                dismissible = dismissible,
                onDismissFinished = onDismissFinished,
                maxWidth = maxWidth,
                content = content,
            )
        } else {
            AppLiquidWindowBoundary {
                WindowDialog(
                    show = show,
                    modifier = modifier,
                    title = title,
                    summary = summary,
                    onDismissRequest = onDismissRequest.takeIf { dismissible },
                    onDismissFinished = onDismissFinished,
                    maxWidth = maxWidth,
                    content = content,
                )
            }
        }
        return
    }

    var rawDialogWasShown by remember { mutableStateOf(false) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(show) {
        if (show) {
            rawDialogWasShown = true
        } else if (rawDialogWasShown) {
            rawDialogWasShown = false
            currentOnDismissFinished?.invoke()
        }
    }
    if (!show) return

    AppLiquidWindowBoundary {
        Dialog(
            onDismissRequest = {
                if (dismissible) {
                    currentOnDismissRequest?.invoke()
                }
            },
            properties = platformDialogProperties(),
        ) {
            RemovePlatformDialogDefaultEffects()
            // A navigation-event owner provided in the host composition (miuix-nav's
            // entry dispatcher) is inherited across the platform-window boundary and
            // never receives this window's back events; re-resolve the dialog's own.
            WindowNavigationEventScope {
                BackHandler(enabled = dismissible) {
                    currentOnDismissRequest?.invoke()
                }
                content()
            }
        }
    }
}
