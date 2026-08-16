package os.kei.ui.page.main.host.pager

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.platform.LocalContext

/**
 * @param sceneActive true when the non-Home background is painting, in which case the page composite is
 *   drawn under this layer by `MainPagerLayout` and an opaque base here would cover it. See
 *   [os.kei.ui.page.main.widget.chrome.LocalAppManagedSceneBackdrop].
 */
@Composable
internal fun rememberMainPagerBackdropLifecycle(sceneActive: Boolean = false): LayerBackdrop {
    val context = LocalContext.current
    var backdropGeneration by rememberSaveable { mutableIntStateOf(0) }
    val activityLifecycle = remember(context) { (context as? ComponentActivity)?.lifecycle }

    DisposableEffect(activityLifecycle) {
        val lifecycle = activityLifecycle ?: return@DisposableEffect onDispose { }
        var appWentBackground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> appWentBackground = true
                Lifecycle.Event.ON_START -> {
                    if (appWentBackground) {
                        backdropGeneration++
                        appWentBackground = false
                    }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // `background`, not `surface`: this layer stands in for what `MainPagerLayout` paints behind the
    // pager, and that is `colorScheme.background`. miuix sets the tokens apart — `#242424` against
    // Black in dark theme — so recording `surface` had the bottom bar's glass sampling a base 36 levels
    // darker than the page it floats over.
    val baseColor = MiuixTheme.colorScheme.background
    return key("main-backdrop-$backdropGeneration") {
        rememberLayerBackdrop {
            if (!sceneActive) {
                drawRect(baseColor)
            }
            drawContent()
        }
    }
}
