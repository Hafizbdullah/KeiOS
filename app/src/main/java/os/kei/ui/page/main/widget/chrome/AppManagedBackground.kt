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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import os.kei.core.prefs.NonHomeBackgroundAlignment
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.core.prefs.NonHomeBackgroundPageStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class AppManagedBackgroundStyle(
    val opacityMultiplier: Float = 1f,
    val lightOverlayAlpha: Float = 0f,
    val darkOverlayAlpha: Float = 0f,
)

object AppManagedBackgroundStyles {
    val Standard = AppManagedBackgroundStyle()
    val Soft =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.82f,
            lightOverlayAlpha = 0.04f,
            darkOverlayAlpha = 0.06f,
        )
    val Readable =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.75f,
            lightOverlayAlpha = 0.08f,
            darkOverlayAlpha = 0.12f,
        )
    val Focused =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.62f,
            lightOverlayAlpha = 0.14f,
            darkOverlayAlpha = 0.18f,
        )
    val FocusedTask =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.58f,
            lightOverlayAlpha = 0.18f,
            darkOverlayAlpha = 0.24f,
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
        )
    }
}

@Composable
fun AppManagedBackgroundHost(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    contentScale: NonHomeBackgroundContentScale,
    scrim: Float,
    modifier: Modifier = Modifier,
    alignment: NonHomeBackgroundAlignment = NonHomeBackgroundAlignment.Center,
    pageStyle: NonHomeBackgroundPageStyle = NonHomeBackgroundPageStyle.Standard,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
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
    val overlayAlpha =
        (if (darkBase) {
            resolvedStyle.darkOverlayAlpha
        } else {
            resolvedStyle.lightOverlayAlpha
        } + scrim).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(baseColor),
    ) {
        if (active) {
            AppManagedBackgroundImage(
                enabled = true,
                imageUri = trimmedUri,
                opacity = opacity * resolvedStyle.opacityMultiplier,
                contentScale = contentScale,
                alignment = alignment,
                modifier = Modifier.fillMaxSize(),
            )
            if (overlayAlpha > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(baseColor.copy(alpha = overlayAlpha)),
                )
            }
        }
        CompositionLocalProvider(
            LocalAppScaffoldContainerColor provides if (active) Color.Transparent else null,
        ) {
            content()
        }
    }
}

@Composable
fun AppManagedBackgroundImage(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    modifier: Modifier = Modifier,
    contentScale: NonHomeBackgroundContentScale = NonHomeBackgroundContentScale.Crop,
    alignment: NonHomeBackgroundAlignment = NonHomeBackgroundAlignment.Center,
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
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale.toComposeContentScale(),
        alignment = alignment.toComposeAlignment(),
        alpha = opacity.coerceIn(0f, 1f),
        modifier = modifier,
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
