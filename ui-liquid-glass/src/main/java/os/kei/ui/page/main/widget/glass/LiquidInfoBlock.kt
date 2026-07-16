@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class LiquidInfoBlockDensity {
    Standard,
    Compact,
}

private const val COMPACT_TITLE_WEIGHT = 0.3f
private const val COMPACT_SUBTITLE_WEIGHT = 0.7f

@Composable
fun LiquidInfoBlock(
    backdrop: Backdrop? = null,
    title: String,
    subtitle: String,
    body: String = "",
    accent: Color,
    modifier: Modifier = Modifier,
    density: LiquidInfoBlockDensity = LiquidInfoBlockDensity.Standard,
    content: (@Composable () -> Unit)? = null,
) {
    val isDark = isAppInDarkTheme()
    val cardSurface =
        if (isDark) {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f)
        } else {
            Color.White.copy(alpha = 0.66f)
        }
    val cornerRadius = 16.dp
    val parentBackdrop = LocalLiquidParentBackdrop.current
    val inheritedBackdrop = backdrop ?: parentBackdrop
    val activeBackdrop = activeGlassBackdrop(inheritedBackdrop)
    val exportedContentBackdrop =
        if (activeBackdrop != null) {
            rememberLayerBackdrop()
        } else {
            null
        }
    LiquidInfoBlockSurface(
        backdrop = activeBackdrop,
        exportedContentBackdrop = exportedContentBackdrop,
        title = title,
        subtitle = subtitle,
        body = body,
        accent = accent,
        density = density,
        titleColor =
            if (isDark) {
                accent
            } else {
                resolveLightGlassContentColor(
                    accent = accent,
                    backgroundAlpha = 0.18f,
                )
            },
        content = content,
        cardSurface = cardSurface,
        cornerRadius = cornerRadius,
        modifier = modifier,
    )
}

@Composable
private fun LiquidInfoBlockSurface(
    backdrop: Backdrop?,
    exportedContentBackdrop: LayerBackdrop?,
    title: String,
    subtitle: String,
    body: String,
    accent: Color,
    density: LiquidInfoBlockDensity,
    titleColor: Color,
    content: (@Composable () -> Unit)?,
    cardSurface: Color,
    cornerRadius: Dp,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LiquidSurface(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(cornerRadius),
            isInteractive = false,
            surfaceColor = cardSurface,
            blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content),
            lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content),
            exportedBackdrop = exportedContentBackdrop,
        ) {
            if (exportedContentBackdrop != null) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides exportedContentBackdrop,
                    LocalLiquidParentBackdropOverridesFallback provides true,
                ) {
                    LiquidInfoBlockContent(
                        exportedContentBackdrop = exportedContentBackdrop,
                        title = title,
                        subtitle = subtitle,
                        body = body,
                        accent = accent,
                        density = density,
                        titleColor = titleColor,
                        content = content,
                    )
                }
            } else {
                LiquidInfoBlockContent(
                    exportedContentBackdrop = null,
                    title = title,
                    subtitle = subtitle,
                    body = body,
                    accent = accent,
                    density = density,
                    titleColor = titleColor,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun LiquidInfoBlockContent(
    exportedContentBackdrop: Backdrop?,
    title: String,
    subtitle: String,
    body: String,
    accent: Color,
    density: LiquidInfoBlockDensity,
    titleColor: Color,
    content: (@Composable () -> Unit)?,
) {
    val compact = density == LiquidInfoBlockDensity.Compact
    val contentPadding =
        if (compact) {
            PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        } else {
            PaddingValues(16.dp)
        }
    val contentSpacing = if (compact) 6.dp else 8.dp
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(contentPadding),
    ) {
        if (compact) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiquidInfoBlockTitlePill(
                    backdrop = exportedContentBackdrop,
                    title = title,
                    accent = accent,
                    titleColor = titleColor,
                    density = density,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(COMPACT_SUBTITLE_WEIGHT),
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiquidInfoBlockTitlePill(
                    backdrop = exportedContentBackdrop,
                    title = title,
                    accent = accent,
                    titleColor = titleColor,
                    density = density,
                )
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(top = contentSpacing),
                )
            }
        }
        if (content != null) {
            Column(modifier = Modifier.padding(top = contentSpacing)) { content() }
        } else if (body.isNotBlank()) {
            Text(
                text = body,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = if (compact) AppTypographyTokens.Body.fontSize else TextUnit.Unspecified,
                lineHeight = if (compact) AppTypographyTokens.Body.lineHeight else TextUnit.Unspecified,
                modifier = Modifier.padding(top = contentSpacing),
            )
        }
    }
}

@Composable
private fun RowScope.LiquidInfoBlockTitlePill(
    backdrop: Backdrop?,
    title: String,
    accent: Color,
    titleColor: Color,
    density: LiquidInfoBlockDensity,
) {
    val compact = density == LiquidInfoBlockDensity.Compact
    LiquidSurface(
        backdrop = backdrop,
        modifier =
            Modifier.weight(
                weight = if (compact) COMPACT_TITLE_WEIGHT else 1f,
                fill = false,
            ),
        shape = RoundedRectangle(999.dp),
        isInteractive = false,
        surfaceColor = accent.copy(alpha = 0.18f),
        blurRadius = 4.dp,
        lensRadius = 18.dp,
        effectVariant = GlassVariant.Compact,
        shadow = false,
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = if (compact) AppTypographyTokens.Supporting.fontSize else TextUnit.Unspecified,
            lineHeight = if (compact) AppTypographyTokens.Supporting.lineHeight else TextUnit.Unspecified,
            modifier =
                Modifier.padding(
                    horizontal = if (compact) 8.dp else 10.dp,
                    vertical = if (compact) 3.dp else 4.dp,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
