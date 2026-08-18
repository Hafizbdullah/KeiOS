package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Narrowest a single pane may be before the rows inside it stop working.
 *
 * Derived, not chosen. The app is drawn for a phone: the phone AVD is 426dp wide and the narrowest device it
 * can install on is 360dp. A pane below that is a geometry no screen in the app has ever been laid out for, so
 * 380dp is "a comfortable phone, with a little slack".
 */
val AppPaneMinWidth: Dp = 380.dp

/**
 * Narrowest window that gets two panes: two [AppPaneMinWidth] panes, and nothing else.
 *
 * The point of deriving it rather than borrowing a breakpoint is that it lands where it has to on the devices
 * that matter, instead of near them:
 *
 * | Device | Width | Panes |
 * |---|---|---|
 * | Phone, any | 360–440dp | 1 |
 * | Fold, outer screen | ~360dp | 1 |
 * | 8" tablet, portrait | ~600dp | 1 |
 * | **Fold, inner screen, portrait** | ~775dp | **2** |
 * | **Pixel Tablet, portrait** | 800dp | **2** |
 * | **Fold, inner screen, landscape** | ~930dp | **2** |
 * | **Pixel Tablet, landscape** | 1280dp | **2** |
 *
 * A 600dp Material breakpoint would have put two 300dp panes on an 8" tablet; an 840dp one would have left the
 * fold's inner screen in portrait on a single pane, which is the case that most obviously wants two.
 */
val AppDualPaneMinWidth: Dp = AppPaneMinWidth * 2f

/** How many panes a window of [availableWidth] gets. */
enum class AppPaneMode {
    Single,
    Dual,
}

/**
 * Pure pane decision.
 *
 * Width alone, deliberately. A fold's inner screen is a wide window and its outer screen is a narrow one, so
 * width already separates them and no `androidx.window` dependency is needed to tell them apart. `FoldingFeature`
 * would only be needed to put the divider *on* the hinge or to react to tabletop/book posture, neither of which
 * this decides.
 */
fun appPaneModeFor(availableWidth: Dp): AppPaneMode =
    if (availableWidth >= AppDualPaneMinWidth) AppPaneMode.Dual else AppPaneMode.Single

/**
 * Width of one pane once [availableWidth] is split.
 *
 * An even split, which is not the 1:1.5 a list-detail layout would usually take. At the bottom of the dual-pane
 * range an uneven split would push the smaller pane under [AppPaneMinWidth] — 760dp at 40/60 gives a 304dp
 * pane — and the right pane here is not always a detail view. It is a route sometimes and a second full section
 * other times, and a section is not a subordinate of anything.
 *
 * Each pane then applies the content cap internally, so this composes with [appPageSideGutter] rather than
 * competing with it: at 1280dp each pane is 640dp and takes no gutter, and only on a panel wide enough to give
 * a pane more than [AppPageContentMaxWidth] does the gutter reappear inside the panes.
 */
fun appPaneWidthFor(availableWidth: Dp): Dp =
    when (appPaneModeFor(availableWidth)) {
        AppPaneMode.Single -> availableWidth
        AppPaneMode.Dual -> availableWidth / 2f
    }

/**
 * The width of the pane the caller is being composed inside, when it is narrower than the window.
 *
 * Without this, everything downstream that centres against the window — the content gutter, the top-end
 * toolbars, the floating docks — would centre a pane's content against a window twice its size and push it out
 * of its own pane. Null means "not in a pane", i.e. the single-pane case, where the window *is* the pane.
 */
val LocalAppPaneWidth = compositionLocalOf<Dp?> { null }

/** The width the current content is actually laid out in: the enclosing pane, or the window. */
@Composable
fun appContentWidth(): Dp = LocalAppPaneWidth.current ?: LocalConfiguration.current.screenWidthDp.dp

/** Pane mode for the current window. */
@Composable
fun appPaneMode(): AppPaneMode = appPaneModeFor(LocalConfiguration.current.screenWidthDp.dp)
