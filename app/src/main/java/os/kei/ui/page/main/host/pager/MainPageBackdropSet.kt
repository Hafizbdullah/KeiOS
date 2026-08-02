package os.kei.ui.page.main.host.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class MainPageBackdropSet(
    val topBar: LayerBackdrop,
    val content: LayerBackdrop,
    val sheet: LayerBackdrop,
    val contentMaterial: Backdrop,
)

/**
 * Hosts LayerBackdrop producers before the consumer slot. Canvas-backed materials draw directly
 * inside each consumer and pass through this host without allocating a page-sized producer.
 */
@Composable
internal fun MainPageContentBackdropScene(
    contentBackdrop: Backdrop,
    sheetBackdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    producerActive: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        val contentLayerBackdrop = contentBackdrop as? LayerBackdrop
        if (producerActive && contentLayerBackdrop != null) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(contentLayerBackdrop),
            )
        }
        val sheetLayerBackdrop = sheetBackdrop as? LayerBackdrop
        if (
            producerActive &&
                sheetLayerBackdrop != null &&
                sheetLayerBackdrop !== contentLayerBackdrop
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(sheetLayerBackdrop),
            )
        }
        content()
    }
}

@Composable
fun rememberMainPageBackdropSet(
    keyPrefix: String,
    refreshOnCompositionEnter: Boolean = false,
    distinctLayers: Boolean = true,
    useSolidSurfaceBackdrops: Boolean = false,
): MainPageBackdropSet {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val instanceKeySuffix = if (refreshOnCompositionEnter) {
        var activationCount by rememberSaveable(keyPrefix) { mutableIntStateOf(0) }
        DisposableEffect(Unit) {
            activationCount++
            onDispose { }
        }
        "-$activationCount"
    } else {
        ""
    }

    @Composable
    fun rememberPageBackdrop(slot: String): LayerBackdrop = key("$keyPrefix-$slot$instanceKeySuffix") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }

    // The top bar captures scrolling content from its dedicated list producer. Page cards sample
    // either their legacy surface-color layer or the equivalent direct canvas material.
    val topBarBackdrop = rememberPageBackdrop("topbar")
    val contentBackdrop =
        if (useSolidSurfaceBackdrops) {
            topBarBackdrop
        } else {
            rememberPageBackdrop("content")
        }
    val sheetBackdrop =
        if (distinctLayers) {
            rememberPageBackdrop("sheet")
        } else {
            contentBackdrop
        }
    val contentMaterial =
        key("$keyPrefix-content-material$instanceKeySuffix") {
            rememberCanvasBackdrop { drawRect(surfaceColor) }
        }
    return MainPageBackdropSet(
        topBar = topBarBackdrop,
        content = contentBackdrop,
        sheet = sheetBackdrop,
        contentMaterial = contentMaterial,
    )
}
