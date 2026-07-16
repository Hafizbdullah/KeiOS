@file:Suppress("FunctionName", "PropertyName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidDialogBackdrop
import os.kei.ui.page.main.widget.glass.LiquidBackdropWindowDialog
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.safeLiquidLens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.shape.appSquircleSurface
import os.kei.ui.page.main.widget.sheet.LocalSceneBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects

/**
 * v2 Liquid Glass Dialog — a frosted-glass confirmation dialog with spring scale animation.
 *
 * Replaces Miuix [WindowDialog] when the user enables liquid glass dialogs in settings.
 * Matches the API surface of WindowDialog (show, title, summary, onDismissRequest, content)
 * so migration is a drop-in replacement at the routing layer.
 *
 * Design:
 * - Centered card with rounded corners and semi-transparent glass surface
 * - Spring scale-in animation (0.85 → 1.0) for a bouncy, alive entrance
 * - Scrim dim behind the dialog
 * - Title + summary + custom content slot (typically action buttons)
 */

private val LiquidDialogCornerRadius = 24.dp
private val LiquidDialogMaxWidth = 420.dp
private const val LiquidDialogScrimAlpha = 0.38f
private const val LiquidDialogExitDurationMillis = 220

@Composable
fun LiquidGlassDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    dismissible: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    var renderDialog by remember { mutableStateOf(show) }
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current

    SideEffect {
        if (!transitionAnimationsEnabled) {
            when {
                show && !renderDialog -> {
                    renderDialog = true
                }

                !show && renderDialog -> {
                    renderDialog = false
                    currentOnDismissFinished?.invoke()
                }
            }
        }
    }

    LaunchedEffect(show, transitionAnimationsEnabled) {
        if (!transitionAnimationsEnabled) return@LaunchedEffect
        if (show) {
            renderDialog = true
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 450f),
                )
            }
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 600f),
            )
        } else {
            if (!renderDialog) return@LaunchedEffect
            val scaleJob =
                launch {
                    scale.animateTo(
                        targetValue = 0.92f,
                        animationSpec = tween(durationMillis = LiquidDialogExitDurationMillis),
                    )
                }
            val alphaJob =
                launch {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = LiquidDialogExitDurationMillis),
                    )
                }
            scaleJob.join()
            alphaJob.join()
            renderDialog = false
            currentOnDismissFinished?.invoke()
        }
    }

    val shouldRenderDialog = if (transitionAnimationsEnabled) renderDialog else show
    if (!shouldRenderDialog) return

    val renderedScale = if (transitionAnimationsEnabled) scale.value else 1f
    val renderedAlpha = if (transitionAnimationsEnabled) alpha.value else 1f

    LiquidBackdropWindowDialog(
        onDismissRequest = {
            if (dismissible) {
                currentOnDismissRequest?.invoke()
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        // Remove system's default dim background — we draw our own scrim.
        RemovePlatformDialogDefaultEffects()

        val sceneBackdrop = LocalSceneBackdrop.current
        val dialogBackdrop = rememberLayerBackdrop()
        val liquidControlsEnabled = LocalLiquidControlsEnabled.current
        val dialogShape: Shape = RoundedRectangle(LiquidDialogCornerRadius)
        val surfaceColor = Color.White.copy(alpha = 0.5f)
        val titleColor = MiuixTheme.colorScheme.onBackground
        val summaryColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
        val glassRuntime = LocalGlassEffectRuntime.current
        val blurRadius = UiPerformanceBudget.backdropBlur * glassRuntime.blurScaleFor(GlassVariant.Floating)
        val lensRadius = 30.dp * glassRuntime.lensScaleFor(GlassVariant.Floating)
        val surfaceModifier =
            if (liquidControlsEnabled) {
                Modifier.drawBackdrop(
                    backdrop = sceneBackdrop,
                    shape = { dialogShape },
                    effects = {
                        vibrancy()
                        blur(blurRadius.toPx())
                        safeLiquidLens(
                            lensRadius.toPx(),
                            (lensRadius * 1.6f).toPx(),
                            chromaticAberration = true,
                            depthEffect = true,
                        )
                    },
                    exportedBackdrop = dialogBackdrop,
                    highlight = {
                        Highlight.Default.copy(alpha = 0.88f)
                    },
                    shadow = {
                        Shadow.Default.copy(color = Color.Black.copy(alpha = 0.18f))
                    },
                    innerShadow = {
                        InnerShadow(radius = 10.dp, alpha = 0.14f)
                    },
                    onDrawSurface = {
                        drawRect(surfaceColor)
                        drawRect(Color.White.copy(alpha = 0.06f), blendMode = BlendMode.Screen)
                    },
                )
            } else {
                Modifier.appSquircleSurface(
                    color = surfaceColor,
                    cornerRadius = LiquidDialogCornerRadius,
                )
            }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Keep the outside-dismiss target out of TalkBack traversal. The dialog content keeps
            // its own semantics and actions as a separate sibling above this scrim.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = renderedAlpha }
                        .background(Color.Black.copy(alpha = LiquidDialogScrimAlpha))
                        .then(
                            if (dismissible) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures {
                                        currentOnDismissRequest?.invoke()
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
            )

            // Dialog card
            Column(
                modifier =
                    modifier
                        .widthIn(max = LiquidDialogMaxWidth)
                        .fillMaxWidth(0.88f)
                        .graphicsLayer {
                            scaleX = renderedScale
                            scaleY = renderedScale
                            this.alpha = renderedAlpha
                            transformOrigin = TransformOrigin.Center
                        }.then(surfaceModifier)
                        .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                        .semantics {
                            isTraversalGroup = true
                            title?.takeIf { it.isNotBlank() }?.let { paneTitle = it }
                        }.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(LocalLiquidDialogBackdrop provides if (liquidControlsEnabled) dialogBackdrop else null) {
                    // Title
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            color = titleColor,
                            fontSize = AppTypographyTokens.SectionTitle.fontSize,
                            lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                            fontWeight = AppTypographyTokens.SectionTitle.fontWeight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().semantics { heading() },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Summary
                    if (!summary.isNullOrBlank()) {
                        Text(
                            text = summary,
                            color = summaryColor,
                            fontSize = AppTypographyTokens.Body.fontSize,
                            lineHeight = AppTypographyTokens.Body.lineHeight,
                            fontWeight = AppTypographyTokens.Body.fontWeight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Custom content (typically action buttons)
                    content()
                }
            }
        }
    }
}
