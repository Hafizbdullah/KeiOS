package os.kei.ui.page.main.host.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether the surrounding NavDisplay entry is the interactive top rather than a covered layer.
 *
 * A covered entry keeps rendering. NavDisplay's visible window is `-1 < d <= opaqueDepth`, and the
 * slide preset declares `opaqueDepth = 1f` so the layer directly below the top sits at exactly the
 * boundary and stays in the window — it has to, because the same window carries the parallax while
 * the transition runs. Once the push settles, that layer is behind a full-screen opaque page and
 * every producer on it is painting for nobody. As activities these pages were `onStop`ped instead,
 * which is the performance the route migration gave up.
 *
 * miuix-nav already publishes the distinction: `navMaxLifecycleFor` caps a covered entry at
 * [Lifecycle.State.STARTED] and only the top reaches [Lifecycle.State.RESUMED], surfaced through the
 * per-entry `LocalLifecycleOwner`. Reading it here lets a page suspend work that cannot be seen
 * without weakening anything that can.
 */
@Composable
internal fun rememberNavEntryAtTop(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    return state.isAtLeast(Lifecycle.State.RESUMED)
}
