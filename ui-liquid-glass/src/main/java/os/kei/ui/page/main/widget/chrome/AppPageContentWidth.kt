package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Widest a page's content column is allowed to get before the surplus becomes gutter.
 *
 * Every row in this app is drawn as "label at the leading edge, controls at the trailing edge". At a phone's
 * ~426dp that reads as one object. Stretched to a tablet's 1280dp the two halves end up roughly 1100dp apart
 * with nothing in between, and the row stops reading as a row — the eye has to travel the width of the panel
 * to connect a switch to the thing it switches.
 *
 * 720dp is the measured answer to that, not a breakpoint borrowed from a spec. It is wide enough that the
 * tablet does not look like a scaled-up phone, and narrow enough that a label and its control stay in one
 * glance. It also lands *below* the Pad AVD's portrait width (800dp) and well below its landscape width
 * (1280dp), so a row is laid out identically in both orientations — rotating the tablet re-flows nothing,
 * which is the property that makes rotation on a large screen unremarkable now that the app can no longer
 * refuse it.
 */
val AppPageContentMaxWidth: Dp = 720.dp

/**
 * Extra inset each side needs so a content column of at most [maxContentWidth] sits centred in
 * [availableWidth].
 *
 * Returns **zero below the cap**, which is the important half of the contract: every phone this app installs
 * on is 360–440dp wide, so this is identically `0.dp` there and no phone layout moves by a pixel. The gutter
 * only ever appears on a panel that has width to spare.
 *
 * Pure, so the arithmetic can be pinned by a test rather than read off a screenshot.
 */
fun appPageSideGutterFor(
    availableWidth: Dp,
    maxContentWidth: Dp = AppPageContentMaxWidth,
): Dp = ((availableWidth - maxContentWidth) / 2f).coerceAtLeast(0.dp)

/**
 * The gutter for the current window.
 *
 * Reads `LocalConfiguration.screenWidthDp` — the app window's width, not the display's — so a multi-window or
 * split-screen host narrows the gutter along with the window instead of centring content against a screen the
 * app does not have.
 *
 * Everything that anchors to a page edge should add this: the content padding of the lists, and equally the
 * chrome that floats over them. An overlay pinned to the true window edge while the content sits 280dp inside
 * it is worse than no gutter at all — the actions stop belonging to the page they act on.
 */
@Composable
fun appPageSideGutter(maxContentWidth: Dp = AppPageContentMaxWidth): Dp =
    appPageSideGutterFor(
        availableWidth = LocalConfiguration.current.screenWidthDp.dp,
        maxContentWidth = maxContentWidth,
    )

/**
 * Horizontal padding for something that spans the page and is laid out *outside* a list's content padding.
 *
 * The status hub each main page pins above its list is the case this exists for: it is a sibling of the list,
 * not an item in it, so it never sees [appPageContentPadding] and would stay full-bleed while every row below
 * it narrowed. Anything that reaches the page's own left and right edges should use this instead of reading
 * `AppChromeTokens.pageHorizontalPadding` directly — that token is still correct *inside* a card, where there
 * is no page edge to centre against.
 */
@Composable
fun appPageEdgePadding(): Dp = AppChromeTokens.pageHorizontalPadding + appPageSideGutter()

/** Gap between a page-edge floating dock and the edge of the content column it belongs to. */
val AppFloatingDockEdgeSpacing: Dp = 14.dp

/**
 * Horizontal padding for a floating action dock pinned to one side of the page.
 *
 * [isDockSide] is whether *this* edge is the one the dock is aligned to; the opposite edge gets zero, because
 * only the aligned side's padding moves an edge-aligned child. Four pages placed this dock with the same
 * hand-written pair of expressions and the same `14.dp`; they now share one, which is also what makes adding
 * the large-screen gutter a single change rather than four.
 */
@Composable
fun appFloatingDockSidePadding(isDockSide: Boolean): Dp =
    if (isDockSide) AppFloatingDockEdgeSpacing + appPageSideGutter() else 0.dp
