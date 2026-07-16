@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    borderWidth: Dp = 0.dp,
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    depthEffect: Boolean = false,
    highlightAlpha: Float? = null,
    showIndication: Boolean = true,
    exportBackdropToContent: Boolean = false,
    clipContent: Boolean = true,
    pressSafePadding: Dp = Dp.Unspecified,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    stateDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickable = onClick != null || onLongClick != null
    val useLiquidClick = onClick != null && onLongClick == null
    val clickModifier =
        if (clickable && !useLiquidClick) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
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
    val blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content)
    val lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content)
    val parentBackdrop = LocalLiquidParentBackdrop.current
    val inheritedBackdrop = backdrop ?: parentBackdrop
    val exportedContentBackdrop =
        if (exportBackdropToContent && inheritedBackdrop != null) {
            rememberLayerBackdrop()
        } else {
            null
        }
    val resolvedPressSafePadding =
        if (pressSafePadding == Dp.Unspecified) {
            if (showIndication && clickable) {
                AppInteractiveTokens.compactLiquidPressSafePadding
            } else {
                0.dp
            }
        } else {
            pressSafePadding
        }
    if (inheritedBackdrop != null) {
        AppSurfaceCardFrame(
            modifier = modifier,
            backdrop = inheritedBackdrop,
            exportedBackdrop = exportedContentBackdrop,
            clickModifier = clickModifier,
            stateModifier = stateModifier,
            interactionSource = interactionSource,
            resolvedPressSafePadding = resolvedPressSafePadding,
            containerColor = containerColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            contentColor = contentColor,
            depthEffect = depthEffect,
            highlightAlpha = highlightAlpha,
            showIndication = showIndication,
            clickable = clickable,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            clipContent = clipContent,
            useLiquidClick = useLiquidClick,
            onClick = onClick,
            content = content,
        )
    } else {
        AppSurfaceCardFrame(
            modifier = modifier,
            backdrop = null,
            exportedBackdrop = null,
            clickModifier = clickModifier,
            stateModifier = stateModifier,
            interactionSource = interactionSource,
            resolvedPressSafePadding = resolvedPressSafePadding,
            containerColor = containerColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            contentColor = contentColor,
            depthEffect = depthEffect,
            highlightAlpha = highlightAlpha,
            showIndication = showIndication,
            clickable = clickable,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            clipContent = clipContent,
            useLiquidClick = useLiquidClick,
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
private fun AppSurfaceCardFrame(
    modifier: Modifier,
    backdrop: Backdrop?,
    exportedBackdrop: LayerBackdrop?,
    clickModifier: Modifier,
    stateModifier: Modifier,
    interactionSource: MutableInteractionSource,
    resolvedPressSafePadding: Dp,
    containerColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    contentColor: Color,
    depthEffect: Boolean,
    highlightAlpha: Float?,
    showIndication: Boolean,
    clickable: Boolean,
    blurRadius: Dp,
    lensRadius: Dp,
    clipContent: Boolean,
    useLiquidClick: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(resolvedPressSafePadding),
    ) {
        LiquidSurface(
            backdrop = backdrop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .then(stateModifier),
            shape = RoundedRectangle(CardLayoutRhythm.cardCornerRadius),
            isInteractive = showIndication && clickable,
            surfaceColor = containerColor,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            depthEffect = depthEffect,
            highlightAlpha = highlightAlpha,
            borderColor = borderColor,
            borderWidth = borderWidth,
            interactionSource = interactionSource,
            clipContent = clipContent,
            exportedBackdrop = exportedBackdrop,
            onClick = if (useLiquidClick) onClick else null,
        ) {
            if (exportedBackdrop != null) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides exportedBackdrop,
                    LocalLiquidParentBackdropOverridesFallback provides true,
                    LocalContentColor provides contentColor,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
            } else {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
fun AppFeatureCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    exportBackdropToContent: Boolean = false,
    eyebrow: String? = null,
    eyebrowColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = contentColor,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    sectionIcon: ImageVector? = null,
    sectionStartAction: (@Composable () -> Unit)? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headerEndActions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues =
        PaddingValues(
            start = CardLayoutRhythm.cardHorizontalPadding,
            end = CardLayoutRhythm.cardHorizontalPadding,
            bottom = CardLayoutRhythm.cardVerticalPadding,
        ),
    contentVerticalSpacing: Dp = CardLayoutRhythm.sectionGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    val headerClick =
        when {
            collapsible -> ({ onExpandedChange(!expanded) })
            onClick != null -> onClick
            else -> null
        }
    AppSurfaceCard(
        modifier = modifier,
        backdrop = backdrop,
        exportBackdropToContent = exportBackdropToContent,
        containerColor = containerColor,
        borderColor = borderColor,
        contentColor = contentColor,
        showIndication = showIndication,
        onClick = if (collapsible) null else onClick,
        onLongClick = onLongClick,
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            eyebrow = eyebrow,
            eyebrowColor = eyebrowColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            startAction =
                sectionStartAction ?: sectionIcon?.let { icon ->
                    {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = titleColor,
                        )
                    }
                },
            endActions = headerEndActions,
            expandable = collapsible,
            expanded = expanded,
            expandTint = titleColor,
            onClick = headerClick,
            onLongClick = onLongClick,
        )
        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = appExpandIn(),
            exit = appExpandOut(),
        ) {
            AppCardBodyColumn(
                contentPadding = contentPadding,
                verticalSpacing = contentVerticalSpacing,
                content = content,
            )
        }
    }
}
