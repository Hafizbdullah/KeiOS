@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.core.prefs.NonHomeBackgroundAlignment
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.core.prefs.NonHomeBackgroundPageStyle
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

@Immutable
data class AppManagedBackgroundStyle(
    val opacityMultiplier: Float = 1f,
    val lightOverlayAlpha: Float = 0f,
    val darkOverlayAlpha: Float = 0f,
    val lightEdgeGradientAlpha: Float = 0f,
    val darkEdgeGradientAlpha: Float = 0f,
    val lightSideGradientAlpha: Float = 0f,
    val darkSideGradientAlpha: Float = 0f,
)

object AppManagedBackgroundStyles {
    val Standard =
        AppManagedBackgroundStyle(
            lightEdgeGradientAlpha = 0.04f,
            darkEdgeGradientAlpha = 0.05f,
            lightSideGradientAlpha = 0.018f,
            darkSideGradientAlpha = 0.024f,
        )
    val Soft =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.82f,
            lightOverlayAlpha = 0.04f,
            darkOverlayAlpha = 0.06f,
            lightEdgeGradientAlpha = 0.07f,
            darkEdgeGradientAlpha = 0.09f,
            lightSideGradientAlpha = 0.028f,
            darkSideGradientAlpha = 0.036f,
        )
    val Readable =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.75f,
            lightOverlayAlpha = 0.08f,
            darkOverlayAlpha = 0.12f,
            lightEdgeGradientAlpha = 0.12f,
            darkEdgeGradientAlpha = 0.16f,
            lightSideGradientAlpha = 0.048f,
            darkSideGradientAlpha = 0.064f,
        )
    val Focused =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.62f,
            lightOverlayAlpha = 0.14f,
            darkOverlayAlpha = 0.18f,
            lightEdgeGradientAlpha = 0.18f,
            darkEdgeGradientAlpha = 0.22f,
            lightSideGradientAlpha = 0.072f,
            darkSideGradientAlpha = 0.088f,
        )
    val FocusedTask =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.58f,
            lightOverlayAlpha = 0.18f,
            darkOverlayAlpha = 0.24f,
            lightEdgeGradientAlpha = 0.22f,
            darkEdgeGradientAlpha = 0.28f,
            lightSideGradientAlpha = 0.088f,
            darkSideGradientAlpha = 0.112f,
        )

    fun forPageStyle(pageStyle: NonHomeBackgroundPageStyle): AppManagedBackgroundStyle =
        when (pageStyle) {
            NonHomeBackgroundPageStyle.Standard -> Standard
            NonHomeBackgroundPageStyle.Readable -> Readable
            NonHomeBackgroundPageStyle.Soft -> Soft
            NonHomeBackgroundPageStyle.Focused -> Focused
        }

    fun resolve(
        pageStyle: NonHomeBackgroundPageStyle,
        sceneStyle: AppManagedBackgroundStyle,
    ): AppManagedBackgroundStyle {
        val preset = forPageStyle(pageStyle)
        return AppManagedBackgroundStyle(
            opacityMultiplier = minOf(preset.opacityMultiplier, sceneStyle.opacityMultiplier),
            lightOverlayAlpha = maxOf(preset.lightOverlayAlpha, sceneStyle.lightOverlayAlpha),
            darkOverlayAlpha = maxOf(preset.darkOverlayAlpha, sceneStyle.darkOverlayAlpha),
            lightEdgeGradientAlpha = maxOf(preset.lightEdgeGradientAlpha, sceneStyle.lightEdgeGradientAlpha),
            darkEdgeGradientAlpha = maxOf(preset.darkEdgeGradientAlpha, sceneStyle.darkEdgeGradientAlpha),
            lightSideGradientAlpha = maxOf(preset.lightSideGradientAlpha, sceneStyle.lightSideGradientAlpha),
            darkSideGradientAlpha = maxOf(preset.darkSideGradientAlpha, sceneStyle.darkSideGradientAlpha),
        )
    }
}

@Composable
fun AppManagedBackgroundHost(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    saturation: Float = 1f,
    contentScale: NonHomeBackgroundContentScale,
    scrim: Float,
    modifier: Modifier = Modifier,
    alignment: NonHomeBackgroundAlignment = NonHomeBackgroundAlignment.Center,
    pageStyle: NonHomeBackgroundPageStyle = NonHomeBackgroundPageStyle.Standard,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
    exportBackdropToContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val trimmedUri = imageUri.trim()
    val active = enabled && trimmedUri.isNotBlank()
    val baseColor = MiuixTheme.colorScheme.background
    val darkBase = baseColor.luminance() < 0.5f
    val resolvedStyle =
        remember(pageStyle, style) {
            AppManagedBackgroundStyles.resolve(
                pageStyle = pageStyle,
                sceneStyle = style,
            )
        }
    val sceneBackdrop =
        if (exportBackdropToContent) {
            rememberLayerBackdrop {
                drawRect(baseColor)
                drawContent()
            }
        } else {
            null
        }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (sceneBackdrop != null) {
                            Modifier.layerBackdrop(sceneBackdrop)
                        } else {
                            Modifier.background(baseColor)
                        },
                    ),
        ) {
            if (active) {
                AppManagedBackgroundImage(
                    enabled = true,
                    imageUri = trimmedUri,
                    opacity = opacity * resolvedStyle.opacityMultiplier,
                    saturation = saturation,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = Modifier.fillMaxSize(),
                )
                AppManagedBackgroundOverlay(
                    baseColor = baseColor,
                    darkBase = darkBase,
                    style = resolvedStyle,
                    scrim = scrim,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        CompositionLocalProvider(
            LocalAppScaffoldContainerColor provides if (active) Color.Transparent else null,
        ) {
            if (sceneBackdrop != null) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides sceneBackdrop,
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

@Composable
fun AppManagedBackgroundOverlay(
    baseColor: Color,
    style: AppManagedBackgroundStyle,
    scrim: Float,
    modifier: Modifier = Modifier,
    darkBase: Boolean = baseColor.luminance() < 0.5f,
) {
    val overlayAlpha =
        (if (darkBase) {
            style.darkOverlayAlpha
        } else {
            style.lightOverlayAlpha
        } + scrim).coerceIn(0f, 1f)
    val edgeGradientAlpha =
        (if (darkBase) {
            style.darkEdgeGradientAlpha
        } else {
            style.lightEdgeGradientAlpha
        } + scrim * 0.42f).coerceIn(0f, 1f)
    val sideGradientAlpha =
        (if (darkBase) {
            style.darkSideGradientAlpha
        } else {
            style.lightSideGradientAlpha
        } + scrim * 0.18f).coerceIn(0f, 1f)

    if (overlayAlpha <= 0f && edgeGradientAlpha <= 0f && sideGradientAlpha <= 0f) return

    Box(
        modifier =
            modifier
                .background(baseColor.copy(alpha = overlayAlpha))
                .drawWithCache {
                    val transparent = baseColor.copy(alpha = 0f)
                    val edgeColor = baseColor.copy(alpha = edgeGradientAlpha)
                    val sideColor = baseColor.copy(alpha = sideGradientAlpha)
                    val verticalMask =
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to edgeColor,
                                    0.30f to transparent,
                                    0.68f to transparent,
                                    1f to edgeColor,
                                ),
                        )
                    val horizontalMask =
                        Brush.horizontalGradient(
                            colorStops =
                                arrayOf(
                                    0f to sideColor,
                                    0.18f to transparent,
                                    0.82f to transparent,
                                    1f to sideColor,
                                ),
                        )
                    onDrawBehind {
                        drawRect(verticalMask)
                        drawRect(horizontalMask)
                    }
                },
    )
}

@Composable
fun AppManagedBackgroundImage(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    modifier: Modifier = Modifier,
    contentScale: NonHomeBackgroundContentScale = NonHomeBackgroundContentScale.Crop,
    alignment: NonHomeBackgroundAlignment = NonHomeBackgroundAlignment.Center,
    saturation: Float = 1f,
    motionScale: Float = 1f,
    motionTranslationXProvider: (() -> Float)? = null,
    motionTranslationYProvider: (() -> Float)? = null,
) {
    if (!enabled || imageUri.isBlank()) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowSize = appWindowSizeDp()
    val (targetWidthPx, targetHeightPx) =
        remember(windowSize, density) {
            with(density) {
                val width =
                    windowSize.width
                        .roundToPx()
                        .coerceAtLeast(1)
                val height =
                    windowSize.height
                        .roundToPx()
                        .coerceAtLeast(1)
                width to height
            }
        }
    val request =
        remember(imageUri, targetWidthPx, targetHeightPx, contentScale) {
            ImageRequest
                .Builder(context)
                .data(imageUri)
                .size(targetWidthPx, targetHeightPx)
                .scale(contentScale.toCoilScale())
                .precision(Precision.INEXACT)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    val safeMotionScale = motionScale.coerceAtLeast(1f)
    val safeSaturation = saturation.coerceIn(0f, 2f)
    val colorFilter =
        remember(safeSaturation) {
            if (abs(safeSaturation - 1f) < 0.01f) {
                null
            } else {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(safeSaturation)
                    },
                )
            }
        }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale.toComposeContentScale(),
        alignment = alignment.toComposeAlignment(),
        alpha = opacity.coerceIn(0f, 1f),
        colorFilter = colorFilter,
        modifier =
            modifier.then(
                if (
                    safeMotionScale != 1f ||
                    motionTranslationXProvider != null ||
                    motionTranslationYProvider != null
                ) {
                    Modifier.graphicsLayer {
                        scaleX = safeMotionScale
                        scaleY = safeMotionScale
                        translationX = motionTranslationXProvider?.invoke() ?: 0f
                        translationY = motionTranslationYProvider?.invoke() ?: 0f
                    }
                } else {
                    Modifier
                },
            ),
    )
}

private fun NonHomeBackgroundContentScale.toComposeContentScale(): ContentScale =
    when (this) {
        NonHomeBackgroundContentScale.Crop -> ContentScale.Crop
        NonHomeBackgroundContentScale.Fit -> ContentScale.Fit
        NonHomeBackgroundContentScale.FillBounds -> ContentScale.FillBounds
    }

private fun NonHomeBackgroundContentScale.toCoilScale(): Scale =
    when (this) {
        NonHomeBackgroundContentScale.Crop,
        NonHomeBackgroundContentScale.FillBounds -> Scale.FILL
        NonHomeBackgroundContentScale.Fit -> Scale.FIT
    }

private fun NonHomeBackgroundAlignment.toComposeAlignment(): Alignment =
    when (this) {
        NonHomeBackgroundAlignment.Top -> Alignment.TopCenter
        NonHomeBackgroundAlignment.Center -> Alignment.Center
        NonHomeBackgroundAlignment.Bottom -> Alignment.BottomCenter
        NonHomeBackgroundAlignment.Start -> Alignment.CenterStart
        NonHomeBackgroundAlignment.End -> Alignment.CenterEnd
    }
