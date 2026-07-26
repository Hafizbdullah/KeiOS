@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackedCard
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    shape: Shape = RoundedRectangle(CardLayoutRhythm.cardCornerRadius),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    borderWidth: Dp = 0.dp,
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    depthEffect: Boolean = false,
    highlightAlpha: Float? = null,
    showIndication: Boolean = true,
    exportBackdropToContent: Boolean = false,
    clipContent: Boolean = true,
    pressSafePadding: Dp = Dp.Unspecified,
    blurRadius: Dp = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content),
    lensRadius: Dp = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    stateDescription: String? = null,
    role: Role = Role.Button,
    selected: Boolean? = null,
    toggleableState: ToggleableState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val edgeStack = LocalAppEdgeStackCards.current
    val edgeStackModifier =
        if (edgeStack != null) {
            Modifier.appEdgeStackedCard(edgeStack)
        } else {
            Modifier
        }
    AppSurfaceBox(
        modifier = edgeStackModifier.then(modifier),
        backdrop = backdrop,
        surfaceColor = containerColor,
        shape = shape,
        borderColor = borderColor,
        borderWidth = borderWidth,
        contentColor = contentColor,
        enabled = enabled,
        isInteractive = showIndication && (onClick != null || onLongClick != null),
        depthEffect = depthEffect,
        highlightAlpha = highlightAlpha,
        exportBackdropToContent = exportBackdropToContent,
        clipContent = clipContent,
        pressSafePadding = pressSafePadding,
        blurRadius = blurRadius,
        lensRadius = lensRadius,
        onClick = onClick,
        onLongClick = onLongClick,
        stateDescription = stateDescription,
        role = role,
        selected = selected,
        toggleableState = toggleableState,
    ) {
        // Only the outermost page card stacks; nested surfaces inside the card body
        // must not re-apply the edge transform.
        CompositionLocalProvider(LocalAppEdgeStackCards provides null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
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
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 2,
    titleTypography: AppTypographyToken = AppTypographyTokens.SectionTitle,
    subtitleTypography: AppTypographyToken = AppTypographyTokens.Supporting,
    headerTextVerticalSpacing: Dp = CardLayoutRhythm.controlRowTextGap,
    headerStartActionSize: Dp = AppInteractiveTokens.cardHeaderLeadingSlotSize,
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
    collapseOnSurfaceClick: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val toggleExpanded: () -> Unit = { onExpandedChange(!expanded) }
    val headerClick =
        when {
            collapsible -> toggleExpanded
            onClick != null -> onClick
            else -> null
        }
    val surfaceClick =
        when {
            collapsible && collapseOnSurfaceClick -> toggleExpanded
            collapsible -> null
            else -> onClick
        }
    AppSurfaceCard(
        modifier = modifier,
        backdrop = backdrop,
        exportBackdropToContent = exportBackdropToContent,
        containerColor = containerColor,
        borderColor = borderColor,
        contentColor = contentColor,
        showIndication = showIndication,
        onClick = surfaceClick,
        onLongClick = onLongClick,
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            eyebrow = eyebrow,
            eyebrowColor = eyebrowColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            titleMaxLines = titleMaxLines,
            subtitleMaxLines = subtitleMaxLines,
            titleTypography = titleTypography,
            subtitleTypography = subtitleTypography,
            textVerticalSpacing = headerTextVerticalSpacing,
            startActionSize = headerStartActionSize,
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
