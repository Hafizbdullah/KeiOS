@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.appSquircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppFloatingLiquidVerticalDockSurface(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val isDark = isSystemInDarkTheme()
    val material = floatingLiquidDockMaterial(isDark)
    val animationScope = rememberCoroutineScope()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val interactiveHighlight =
        remember(animationScope, transitionAnimationsEnabled) {
            InteractiveHighlight(
                animationScope = animationScope,
                highlightStrength = 1.18f,
                highlightRadiusScale = 1.34f,
                consumeDragChanges = false,
                animationsEnabled = transitionAnimationsEnabled,
            )
        }
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer

    Box(
        modifier =
            modifier
                .then(
                    if (activeBackdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = activeBackdrop,
                            shape = { ContinuousCapsule },
                            layerBlock = {
                                val progress = interactiveHighlight.deformationProgress
                                if (progress > 0f) {
                                    scaleX = 1f + 2.dp.toPx() / size.width.coerceAtLeast(1f) * progress
                                    scaleY = 1f + 2.dp.toPx() / size.height.coerceAtLeast(1f) * progress
                                }
                            },
                            effects = {
                                vibrancy()
                                blur(material.blur.toPx())
                                safeLiquidLens(
                                    material.lensHeight.toPx(),
                                    material.lensAmount.toPx(),
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = material.highlightAlpha)
                            },
                            shadow = {
                                Shadow.Default.copy(color = Color.Black.copy(alpha = material.shadowAlpha))
                            },
                            innerShadow = {
                                InnerShadow(radius = 7.dp, alpha = material.innerShadowAlpha)
                            },
                            onDrawSurface = {
                                drawRect(fallbackSurface.copy(alpha = material.surfaceAlpha))
                            },
                        )
                    } else {
                        Modifier.appSquircleBackground(fallbackSurface.copy(alpha = material.surfaceAlpha), 999.dp)
                    },
                ).then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
                .graphicsLayer { clip = false }
                .appSquircleClip(999.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(material.overlayTop, material.overlayBottom),
                    ),
                ).appSquircleBorder(
                    width = 1.dp,
                    color = material.edgeColor,
                    cornerRadius = 999.dp,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .appSquircleClip(999.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    material.sideRim,
                                    Color.Transparent,
                                    Color.Transparent,
                                    material.sideRim,
                                ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .appSquircleBorder(
                        width = 1.dp,
                        color = material.innerRim,
                        cornerRadius = 999.dp,
                    ),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

internal fun floatingLiquidDockMaterial(isDark: Boolean): FloatingLiquidDockMaterial =
    if (isDark) {
        FloatingLiquidDockMaterial(
            blur = 4.dp,
            lensHeight = 16.dp,
            lensAmount = 28.dp,
            surfaceAlpha = 0.22f,
            highlightAlpha = 0.42f,
            shadowAlpha = 0.14f,
            innerShadowAlpha = 0.14f,
            overlayTop = Color.White.copy(alpha = 0.045f),
            overlayBottom = Color(0xFF82B8FF).copy(alpha = 0.025f),
            sideRim = Color.White.copy(alpha = 0.055f),
            innerRim = Color.White.copy(alpha = 0.09f),
            edgeColor = Color.White.copy(alpha = 0.16f),
        )
    } else {
        FloatingLiquidDockMaterial(
            blur = 4.dp,
            lensHeight = 16.dp,
            lensAmount = 32.dp,
            surfaceAlpha = 0.22f,
            highlightAlpha = 0.62f,
            shadowAlpha = 0.10f,
            innerShadowAlpha = 0.20f,
            overlayTop = Color.White.copy(alpha = 0.075f),
            overlayBottom = Color(0xFFEDF6FF).copy(alpha = 0.045f),
            sideRim = Color.White.copy(alpha = 0.18f),
            innerRim = Color.White.copy(alpha = 0.28f),
            edgeColor = Color.White.copy(alpha = 0.58f),
        )
    }

internal data class FloatingLiquidDockMaterial(
    val blur: androidx.compose.ui.unit.Dp,
    val lensHeight: androidx.compose.ui.unit.Dp,
    val lensAmount: androidx.compose.ui.unit.Dp,
    val surfaceAlpha: Float,
    val highlightAlpha: Float,
    val shadowAlpha: Float,
    val innerShadowAlpha: Float,
    val overlayTop: Color,
    val overlayBottom: Color,
    val sideRim: Color,
    val innerRim: Color,
    val edgeColor: Color,
)
