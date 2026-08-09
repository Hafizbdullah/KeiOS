package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.AppEdgeStackSlot
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A single-layer liquid surface atom with [BoxScope] content.
 *
 * The surface exports material to descendants only while its inherited backdrop is active in the
 * current Compose window.
 */
@Composable
fun AppSurfaceBox(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    surfaceColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    shape: Shape = RoundedRectangle(CardLayoutRhythm.cardCornerRadius),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    borderWidth: Dp = 0.dp,
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    depthEffect: Boolean = false,
    highlightAlpha: Float? = null,
    shadow: Boolean = true,
    shadowAlpha: Float = 0.10f,
    exportBackdropToContent: Boolean = false,
    clipContent: Boolean = true,
    pressSafePadding: Dp = Dp.Unspecified,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    effectVariant: GlassVariant? = null,
    /** Forwarded to [LiquidSurface] so a page card recedes inside its own glass layer. */
    edgeStack: AppEdgeStackSlot = AppEdgeStackSlot.Inert,
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    stateDescription: String? = null,
    role: Role = Role.Button,
    selected: Boolean? = null,
    toggleableState: ToggleableState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickable = onClick != null || onLongClick != null
    val useLiquidClick = onClick != null && onLongClick == null
    val clickModifier =
        if (clickable && !useLiquidClick) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        } else {
            Modifier
        }
    val stateModifier =
        stateDescription?.let { description ->
            Modifier.semantics {
                this.stateDescription = description
            }
        } ?: Modifier
    val inheritedBackdrop = backdrop ?: LocalLiquidParentBackdrop.current
    val activeBackdrop = activeGlassBackdrop(inheritedBackdrop)
    val exportedContentBackdrop =
        if (exportBackdropToContent && activeBackdrop != null) {
            rememberLayerBackdrop()
        } else {
            null
        }
    val resolvedPressSafePadding =
        if (pressSafePadding == Dp.Unspecified) {
            if (isInteractive && enabled && clickable) {
                AppInteractiveTokens.compactLiquidPressSafePadding
            } else {
                0.dp
            }
        } else {
            pressSafePadding
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(resolvedPressSafePadding),
    ) {
        LiquidSurface(
            backdrop = activeBackdrop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .then(stateModifier),
            shape = shape,
            enabled = enabled,
            isInteractive = isInteractive,
            tint = tint,
            surfaceColor = surfaceColor,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            effectVariant = effectVariant,
            edgeStack = edgeStack,
            depthEffect = depthEffect,
            highlightAlpha = highlightAlpha,
            borderColor = borderColor,
            borderWidth = borderWidth,
            shadow = shadow,
            shadowAlpha = shadowAlpha,
            interactionSource = interactionSource,
            clipContent = clipContent,
            contentAlignment = contentAlignment,
            exportedBackdrop = exportedContentBackdrop,
            role = role,
            selected = selected,
            toggleableState = toggleableState,
            onClick = if (useLiquidClick) onClick else null,
        ) {
            if (exportedContentBackdrop != null) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides exportedContentBackdrop,
                    LocalLiquidParentBackdropOverridesFallback provides true,
                    LocalContentColor provides contentColor,
                ) {
                    content()
                }
            } else {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }
            }
        }
    }
}
