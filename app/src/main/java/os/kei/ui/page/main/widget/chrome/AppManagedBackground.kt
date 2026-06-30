@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
import os.kei.core.prefs.NonHomeBackgroundContentScale
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class AppManagedBackgroundStyle(
    val opacityMultiplier: Float = 1f,
    val lightOverlayAlpha: Float = 0f,
    val darkOverlayAlpha: Float = 0f,
)

object AppManagedBackgroundStyles {
    val Standard = AppManagedBackgroundStyle()
    val FocusedTask =
        AppManagedBackgroundStyle(
            opacityMultiplier = 0.58f,
            lightOverlayAlpha = 0.18f,
            darkOverlayAlpha = 0.24f,
        )
}

@Composable
fun AppManagedBackgroundHost(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    contentScale: NonHomeBackgroundContentScale,
    scrim: Float,
    modifier: Modifier = Modifier,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
    content: @Composable () -> Unit,
) {
    val trimmedUri = imageUri.trim()
    val active = enabled && trimmedUri.isNotBlank()
    val baseColor = MiuixTheme.colorScheme.background
    val darkBase = baseColor.luminance() < 0.5f
    val overlayAlpha =
        (if (darkBase) {
            style.darkOverlayAlpha
        } else {
            style.lightOverlayAlpha
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
                opacity = opacity * style.opacityMultiplier,
                contentScale = contentScale,
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
    contentScale: NonHomeBackgroundContentScale = NonHomeBackgroundContentScale.Crop,
    modifier: Modifier = Modifier,
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
        remember(imageUri, targetWidthPx, targetHeightPx) {
            ImageRequest
                .Builder(context)
                .data(imageUri)
                .size(targetWidthPx, targetHeightPx)
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale.toComposeContentScale(),
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
