package os.kei.ui.page.main.student.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.core.AppSurfaceBox
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import top.yukonga.miuix.kmp.theme.LocalContentColor

@Composable
internal fun GuideLiquidCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardLayoutRhythm.cardCornerRadius,
    surfaceColor: Color = Color(0x223B82F6),
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    effectVariant: GlassVariant? = GlassVariant.Content,
    depthEffect: Boolean = true,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val pressSafePadding = if (isInteractive && enabled) {
        AppInteractiveTokens.compactLiquidPressSafePadding
    } else {
        0.dp
    }

    AppSurfaceBox(
        modifier = modifier,
        surfaceColor = surfaceColor,
        shape = RoundedRectangle(cornerRadius),
        contentColor = LocalContentColor.current,
        enabled = enabled,
        isInteractive = isInteractive,
        tint = tint,
        depthEffect = depthEffect,
        shadow = shadow,
        exportBackdropToContent = true,
        pressSafePadding = pressSafePadding,
        blurRadius = blurRadius,
        lensRadius = lensRadius,
        effectVariant = effectVariant,
        onClick = onClick,
        content = content,
    )
}
