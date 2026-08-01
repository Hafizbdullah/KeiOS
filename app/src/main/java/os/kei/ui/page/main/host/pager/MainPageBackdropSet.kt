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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class MainPageBackdropSet(
    val topBar: LayerBackdrop,
    val content: LayerBackdrop,
    val sheet: LayerBackdrop,
)

/**
 * Hosts a page scene whose first sibling produces [contentBackdrop] and whose later siblings may
 * safely consume it. Keeping the producer out of [content] avoids the recursive glass-on-glass
 * path caused by wrapping consumers in `Modifier.layerBackdrop(contentBackdrop)`.
 *
 * [rememberMainPageBackdropSet] paints the current page surface before the producer's content, so
 * the full scene remains available even where the page content itself has transparent pixels.
 */
@Composable
internal fun MainPageContentBackdropScene(
    contentBackdrop: LayerBackdrop,
    sheetBackdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier,
    producerActive: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        if (producerActive) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(contentBackdrop),
            )
        }
        if (producerActive && sheetBackdrop != null && sheetBackdrop !== contentBackdrop) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(sheetBackdrop),
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

    // The top bar captures scrolling content whose descendants consume contentBackdrop.
    // A dedicated identity keeps the capture producer outside that descendant consumer path.
    val topBarBackdrop = rememberPageBackdrop("topbar")
    val contentBackdrop = rememberPageBackdrop("content")
    val sheetBackdrop =
        if (distinctLayers) {
            rememberPageBackdrop("sheet")
        } else {
            contentBackdrop
        }
    return MainPageBackdropSet(
        topBar = topBarBackdrop,
        content = contentBackdrop,
        sheet = sheetBackdrop,
    )
}
