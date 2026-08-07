@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LiquidDialogMaxWidth = 420.dp

/**
 * Deprecated alias for [LiquidAlert].
 *
 * These were the same component: a centred glass card with a title, a summary and buttons. The only
 * difference was how the buttons arrived — a caller-owned `content` slot here, a typed
 * [LiquidPresentationAction] list there — and [LiquidAlert] accepts both, so keeping two
 * implementations only meant two places for the material and the motion to drift apart.
 *
 * Existing call sites keep working unchanged. New code should call [LiquidAlert] and pass `actions`
 * rather than building its own button row, so that Apple's ordering rules — expected choice trailing,
 * destructive marked — come for free. When the buttons are choices attached to something the person
 * just did rather than an acknowledgement, use [LiquidActionSheet].
 */
@Deprecated(
    message = "Use LiquidAlert, which this now delegates to. Prefer its `actions` list over a " +
        "hand-built button row so the alert ordering rules are applied for you.",
    replaceWith = ReplaceWith("LiquidAlert(show, title, modifier, summary, emptyList(), dismissible, onDismissRequest, onDismissFinished, maxWidth, content)"),
)
@Composable
fun LiquidGlassDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    dismissible: Boolean = true,
    maxWidth: Dp = LiquidDialogMaxWidth,
    content: @Composable () -> Unit = {},
) {
    LiquidAlert(
        show = show,
        title = title,
        modifier = modifier,
        message = summary,
        dismissible = dismissible,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        maxWidth = maxWidth,
        content = content,
    )
}
