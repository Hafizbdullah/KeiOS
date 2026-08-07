@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

private val LiquidAlertCornerRadius = 26.dp
private val LiquidAlertMaxWidth = 420.dp
private val LiquidAlertPadding = 24.dp

/**
 * A Liquid Glass alert: critical information that needs acknowledging right away.
 *
 * Follows Apple's alert anatomy — a title, optional informative text, and **up to three** buttons.
 * Two buttons sit side by side with the expected choice trailing; three stack, because a row of three
 * cannot hold readable titles. Pass more than three and they still render, but the guidance says an
 * alert is the wrong container for that many choices — an [LiquidActionSheet] is.
 *
 * Use this for problems and irreversible confirmations. For choices attached to an action the person
 * deliberately started, use [LiquidActionSheet] instead.
 */
@Composable
fun LiquidAlert(
    show: Boolean,
    title: String? = null,
    modifier: Modifier = Modifier,
    message: String? = null,
    actions: List<LiquidPresentationAction> = emptyList(),
    dismissible: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxWidth: Dp = LiquidAlertMaxWidth,
    content: @Composable () -> Unit = {},
) {
    LiquidModalPresentation(
        show = show,
        placement = LiquidModalPlacement.Center,
        dismissible = dismissible,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) { progressProvider ->
        val isDark = isAppInDarkTheme()
        val surface = rememberLiquidModalSurface(
            cornerRadius = LiquidAlertCornerRadius,
            isDark = isDark,
            scaleProvider = { liquidModalCardScale(progressProvider()) },
        )
        Column(
            modifier = modifier
                .safeDrawingPadding()
                .widthIn(max = maxWidth)
                .fillMaxWidth(0.88f)
                .graphicsLayer { alpha = liquidModalCardAlpha(progressProvider()) }
                .then(surface.modifier)
                .semantics {
                    isTraversalGroup = true
                    title?.takeIf { it.isNotBlank() }?.let { paneTitle = it }
                }.padding(LiquidAlertPadding),
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
                    )
                }
                if (!message.isNullOrBlank()) {
                    if (!title.isNullOrBlank()) Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.86f),
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = AppTypographyTokens.Body.fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                content()
                if (actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    LiquidAlertActions(
                        actions = actions,
                        backdrop = surface.exportedBackdrop,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidAlertActions(
    actions: List<LiquidPresentationAction>,
    backdrop: com.kyant.backdrop.Backdrop?,
) {
    if (liquidAlertUsesButtonRow(actions.size)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            liquidAlertRowOrder(actions).forEach { action ->
                LiquidPresentationActionButton(
                    action = action,
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Stacked alerts read top-down, so the expected choice goes first rather than trailing.
            liquidAlertRowOrder(actions).reversed().forEach { action ->
                LiquidPresentationActionButton(
                    action = action,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
