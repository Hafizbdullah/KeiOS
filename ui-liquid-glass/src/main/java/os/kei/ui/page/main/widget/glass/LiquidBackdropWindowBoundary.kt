@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.backdrops.emptyBackdrop
import os.kei.ui.page.main.widget.sheet.LocalSceneBackdrop

/**
 * Marks a secondary Compose window whose coordinates are independent from its parent window.
 *
 * A `LayerBackdrop` is coordinates-dependent, so Dialog and Popup content must not inherit a
 * producer from the activity window. Descendants can still create a producer inside this window
 * and provide it through [LocalLiquidParentBackdrop].
 */
internal val LocalLiquidBackdropWindowBoundary = staticCompositionLocalOf { false }

@Composable
internal fun LiquidBackdropWindowBoundary(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLiquidBackdropWindowBoundary provides true,
        LocalSceneBackdrop provides emptyBackdrop(),
        LocalLiquidParentBackdrop provides null,
        LocalLiquidParentBackdropOverridesFallback provides false,
        LocalLiquidDialogBackdrop provides null,
        content = content,
    )
}

@Composable
internal fun LiquidBackdropWindowDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit,
) {
    LiquidBackdropWindowBoundary {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    }
}

@Composable
internal fun LiquidBackdropWindowPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    LiquidBackdropWindowBoundary {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    }
}
