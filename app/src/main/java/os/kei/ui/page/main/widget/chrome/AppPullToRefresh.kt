package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

/**
 * Finger travel a pull has to cover before the refresh arms, counted from the moment the list
 * runs out of content to scroll. Touch slop (~8dp) lands on top of it.
 *
 * The pages behind pull-to-refresh gave up their toolbar refresh button, so an accidental pull
 * costs a full network round trip and the scroll position, while a deliberate one costs a longer
 * sweep. Measured on a 1220x2656 window, the Miuix default arms at ~75dp of travel, ~83dp counting
 * slop — short enough that an ordinary downward flick at the top of a list reaches it. 128dp reads
 * as a gesture the user meant to make.
 */
val AppPullToRefreshTriggerTravel: Dp = 128.dp

/**
 * [rememberPullToRefreshState] with the trigger distance stated as finger travel.
 *
 * Miuix takes its threshold as a share of the full damped drag range, and that range is a third
 * of the window height, so one fraction covers very different physical distances on a short 1080p
 * phone and on a tablet. Converting from [Dp] holds the gesture steady across both.
 */
@Composable
fun rememberAppPullToRefreshState(travel: Dp = AppPullToRefreshTriggerTravel): PullToRefreshState {
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    // The same measurement Miuix feeds into maxDragDistancePx, so the two stay in the same space.
    val windowHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    return rememberPullToRefreshState(
        refreshThreshold =
            appPullToRefreshThreshold(
                travelPx = travelPx,
                windowHeightPx = windowHeightPx,
            ),
    )
}

/**
 * Converts [travelPx] of finger travel into the pull fraction Miuix expects.
 *
 * Miuix damps the drag with `x - x^2 + x^3/3` over the window height, and treats a third of the
 * window — the value that damping converges to — as the full drag range. A threshold of
 * `3 * damped(travelPx / windowHeightPx)` therefore arms the refresh at exactly [travelPx].
 */
internal fun appPullToRefreshThreshold(
    travelPx: Float,
    windowHeightPx: Float,
): Float {
    if (travelPx <= 0f || windowHeightPx <= 0f) return MIUIX_DEFAULT_REFRESH_THRESHOLD
    // Keep the pull inside one thumb sweep on short windows: split screen, a folded cover display.
    val travelRatio = (travelPx / windowHeightPx).coerceAtMost(MAX_TRAVEL_RATIO)
    val damped = travelRatio - travelRatio * travelRatio + travelRatio * travelRatio * travelRatio / 3f
    // Hold the library default as the floor so a tall window keeps the pull at least as deliberate.
    return (3f * damped).coerceAtLeast(MIUIX_DEFAULT_REFRESH_THRESHOLD)
}

/** `rememberPullToRefreshState`'s own default, kept as the floor. */
private const val MIUIX_DEFAULT_REFRESH_THRESHOLD = 0.25f

/** Ceiling on travel as a share of the window height. */
private const val MAX_TRAVEL_RATIO = 0.25f
