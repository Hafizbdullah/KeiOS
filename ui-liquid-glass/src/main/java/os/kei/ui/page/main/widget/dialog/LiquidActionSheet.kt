@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.LocalLiquidDialogBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LiquidActionSheetCornerRadius = 26.dp
private val LiquidActionSheetMaxWidth = 480.dp
private val LiquidActionSheetPadding = 18.dp
private val LiquidActionSheetEdgeMargin = 12.dp

/**
 * A Liquid Glass action sheet: choices attached to an action the person deliberately started.
 *
 * Distinct from [LiquidAlert] on purpose. An alert is unexpected and reports a problem; an action
 * sheet is the answer to something the person just did — cancelling a draft, sharing an item — and it
 * offers *choices* rather than acknowledgement. Reach for it whenever confirming would otherwise mean
 * an alert with more than two buttons.
 *
 * Button order is enforced, not left to the caller: destructive choices go to the top where they are
 * most noticeable, and Cancel goes to the bottom, visually separated. Apple asks for at most four
 * buttons including Cancel — so no more than three real choices — and for the sheet never to scroll,
 * because scrolling a list of buttons invites mis-taps.
 */
@Composable
fun LiquidActionSheet(
    show: Boolean,
    actions: List<LiquidPresentationAction>,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    dismissible: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxWidth: Dp = LiquidActionSheetMaxWidth,
) {
    LiquidModalPresentation(
        show = show,
        placement = LiquidModalPlacement.Bottom,
        dismissible = dismissible,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) { progressProvider ->
        val isDark = isAppInDarkTheme()
        val cardHeightPx = remember { mutableFloatStateOf(0f) }
        val ordered = liquidActionSheetOrder(actions)
        val choices = ordered.filter { it.role != LiquidActionRole.Cancel }
        val cancels = ordered.filter { it.role == LiquidActionRole.Cancel }
        // Rises from below rather than scaling: it belongs to the bottom edge, so scaling from the
        // centre would read as an alert.
        val surface = rememberLiquidModalSurface(
            cornerRadius = LiquidActionSheetCornerRadius,
            isDark = isDark,
            scaleProvider = { 1f },
        )
        Column(
            modifier = modifier
                .safeDrawingPadding()
                .padding(horizontal = LiquidActionSheetEdgeMargin)
                .padding(bottom = LiquidActionSheetEdgeMargin)
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .onSizeChanged { cardHeightPx.floatValue = it.height.toFloat() }
                .graphicsLayer {
                    alpha = liquidModalCardAlpha(progressProvider())
                    translationY = liquidModalBottomOffsetPx(
                        progress = progressProvider(),
                        cardHeightPx = cardHeightPx.floatValue,
                    )
                }.then(surface.modifier)
                .semantics {
                    isTraversalGroup = true
                    title?.takeIf { it.isNotBlank() }?.let { paneTitle = it }
                }.padding(LiquidActionSheetPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(
                // Controls inside the card sample the *card*, never the page behind it. Without the
                // parent override they would inherit the page backdrop the caller was using and show
                // unblurred content through a surface that is sitting on glass.
                LocalLiquidParentBackdrop provides surface.exportedBackdrop,
                LocalLiquidParentBackdropOverridesFallback provides true,
                LocalLiquidDialogBackdrop provides surface.exportedBackdrop,
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.SectionTitle.fontSize,
                        lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                        fontWeight = AppTypographyTokens.SectionTitle.fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().semantics { heading() },
                        maxLines = 1,
                    )
                }
                if (!message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = AppTypographyTokens.Body.fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    choices.forEach { action ->
                        LiquidPresentationActionButton(
                            action = action,
                            backdrop = surface.exportedBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (cancels.isNotEmpty()) {
                    // Wider gap than between choices: Cancel is not one of them.
                    Spacer(modifier = Modifier.height(16.dp))
                    cancels.forEach { action ->
                        LiquidPresentationActionButton(
                            action = action,
                            backdrop = surface.exportedBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
