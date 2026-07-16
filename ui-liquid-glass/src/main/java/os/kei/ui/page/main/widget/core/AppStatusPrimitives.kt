@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

enum class AppStatusPillSize {
    Compact,
    Default,
    Prominent,
}

@Immutable
data class AppStatusPillMetrics(
    val contentPadding: PaddingValues,
    val typography: AppTypographyToken,
)

internal object AppStatusPrimitives {
    val pillShape = ContinuousCapsule
    val compactPillMetrics =
        AppStatusPillMetrics(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            typography = AppTypographyTokens.Caption,
        )
    val defaultPillMetrics =
        AppStatusPillMetrics(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            typography = AppTypographyTokens.Caption,
        )
    val prominentPillMetrics =
        AppStatusPillMetrics(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            typography = AppTypographyTokens.Body,
        )
}

@Composable
fun rememberAppStatusPillMetrics(size: AppStatusPillSize): AppStatusPillMetrics =
    when (size) {
        AppStatusPillSize.Compact -> AppStatusPrimitives.compactPillMetrics
        AppStatusPillSize.Default -> AppStatusPrimitives.defaultPillMetrics
        AppStatusPillSize.Prominent -> AppStatusPrimitives.prominentPillMetrics
    }

@Composable
fun AppSupportingBlock(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    typography: AppTypographyToken = AppTypographyTokens.Supporting,
    cornerRadius: Dp = 12.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    fillWidth: Boolean = false,
    depthEffect: Boolean = false,
    highlightAlpha: Float? = null,
    shadow: Boolean = false,
    shadowAlpha: Float = 0.10f,
) {
    val isDark = isAppInDarkTheme()
    val shape = RoundedRectangle(cornerRadius)
    val backgroundColor =
        containerColor ?: if (isDark) {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.56f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f)
        }
    val resolvedContentColor =
        contentColor ?: MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f)
    val textContent: @Composable () -> Unit = {
        top.yukonga.miuix.kmp.basic.Text(
            text = text,
            color = resolvedContentColor,
            fontSize = typography.fontSize,
            lineHeight = typography.lineHeight,
            fontWeight = typography.fontWeight,
            modifier = Modifier.padding(contentPadding),
            maxLines = maxLines,
            overflow = overflow,
        )
    }

    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

    val activeBackdrop = activeGlassBackdrop(LocalLiquidParentBackdrop.current)
    Box(
        modifier =
            modifier
                .then(clickModifier),
    ) {
        if (activeBackdrop != null) {
            AppSupportingBlockLiquid(
                backdrop = activeBackdrop,
                backgroundColor = backgroundColor,
                accentColor = accentColor,
                isDark = isDark,
                shape = shape,
                borderColor = borderColor,
                borderWidth = borderWidth,
                fillWidth = fillWidth,
                depthEffect = depthEffect,
                highlightAlpha = highlightAlpha,
                shadow = shadow,
                shadowAlpha = shadowAlpha,
                textContent = textContent,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                        .appSquircleBackground(backgroundColor, cornerRadius)
                        .then(
                            if (borderWidth > 0.dp && borderColor.isSpecified && borderColor.alpha > 0f) {
                                Modifier.appSquircleBorder(borderWidth, borderColor, cornerRadius)
                            } else {
                                Modifier
                            },
                        ),
            ) {
                textContent()
            }
        }
    }
}

@Composable
private fun AppSupportingBlockLiquid(
    backdrop: Backdrop,
    backgroundColor: Color,
    accentColor: Color,
    isDark: Boolean,
    shape: Shape,
    borderColor: Color,
    borderWidth: Dp,
    fillWidth: Boolean,
    depthEffect: Boolean,
    highlightAlpha: Float?,
    shadow: Boolean,
    shadowAlpha: Float,
    textContent: @Composable () -> Unit,
) {
    LiquidSurface(
        backdrop = backdrop,
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
        shape = shape,
        isInteractive = false,
        surfaceColor = backgroundColor,
        tint = accentColor.copy(alpha = if (isDark) 0.03f else 0.02f),
        blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Content),
        lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Content),
        depthEffect = depthEffect,
        highlightAlpha = highlightAlpha,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadow = shadow,
        shadowAlpha = shadowAlpha,
    ) {
        textContent()
    }
}

@Preview(name = "Status Primitive Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppStatusPrimitivePreviewLight() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        AppSupportingBlock(
            text = "Shared support blocks can be reused in setting sheets, policy notes, and diagnostics.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Status Primitive Dark", showBackground = true, backgroundColor = 0xFF111827)
@Composable
private fun AppStatusPrimitivePreviewDark() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
        AppSupportingBlock(
            text = "Status pills and support blocks now share the same visual rhythm.",
            modifier = Modifier.padding(16.dp),
            accentColor = Color(0xFF60A5FA),
        )
    }
}
