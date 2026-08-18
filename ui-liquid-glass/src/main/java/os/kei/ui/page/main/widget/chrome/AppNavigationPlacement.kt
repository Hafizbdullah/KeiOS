package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where the app's top-level navigation lives.
 *
 * The three forms come from Apple's `sidebarAdaptable` tab view rather than from scaling the phone's bar up.
 * The Human Interface Guidelines put a tab bar at the **bottom** on iOS and **near the top** on iPadOS, and let
 * the iPadOS one carry a button that converts it into a sidebar; both forms of that control keep the button, so
 * the two are one control in two shapes rather than two separate designs.
 */
enum class AppNavigationPlacement {
    /** Floating bar at the bottom. The phone shape, and the one the HIG prescribes for iOS. */
    Bottom,

    /** Bar near the top of the window. The iPadOS default. */
    Top,

    /** Leading rail. The iPadOS tab bar's converted form. */
    Sidebar,
}

/**
 * Narrowest window that reads as "regular width" and takes the iPadOS treatment.
 *
 * 600dp is the line Android already draws for a large screen — it is what `sw600dp` resource buckets key on,
 * and what the platform itself uses from targetSdk 36 to decide that an orientation request no longer applies.
 * Reusing it means the navigation changes shape at exactly the width where the system already considers the
 * app to be on a big screen, instead of at a second, private boundary a few dp away from the first.
 *
 * Deliberately **not** [AppDualPaneMinWidth]. That one answers "is there room for two content columns" and is
 * derived from two of them; this one answers "is this a tablet-shaped window". A 640dp window is a tablet that
 * cannot hold two content panes, and it should still get a top tab bar.
 */
val AppRegularWidthMinWidth: Dp = 600.dp

/** Width of the sidebar rail when it is shown. */
val AppSidebarWidth: Dp = 280.dp

/**
 * Narrowest window a sidebar may appear in.
 *
 * The HIG is explicit that "a sidebar requires a large amount of vertical and horizontal space", and that when
 * space is limited "a more compact control such as a tab bar may provide a better navigation experience". So
 * the rule is derived rather than declared: a sidebar is allowed only while what it leaves behind is still a
 * viable content column — [AppPaneMinWidth], the same floor the two-pane split uses.
 *
 * That makes the fallback automatic on rotation and resize, which is what `sidebarAdaptable` promises: narrow
 * the window past this and the sidebar becomes a top tab bar on its own, without the user's preference being
 * discarded.
 */
val AppSidebarMinWindowWidth: Dp = AppSidebarWidth + AppPaneMinWidth

/**
 * Pure placement decision.
 *
 * [sidebarPreferred] is the user's own choice, carried across sessions. It is honoured only where a sidebar
 * fits; below that it is *kept but not applied*, so widening the window brings the sidebar back rather than
 * making the user ask for it again.
 */
fun appNavigationPlacementFor(
    availableWidth: Dp,
    sidebarPreferred: Boolean,
): AppNavigationPlacement =
    when {
        availableWidth < AppRegularWidthMinWidth -> AppNavigationPlacement.Bottom
        sidebarPreferred && availableWidth >= AppSidebarMinWindowWidth -> AppNavigationPlacement.Sidebar
        else -> AppNavigationPlacement.Top
    }

/** Whether offering the sidebar toggle makes sense at this width at all. */
fun appSidebarAvailableAt(availableWidth: Dp): Boolean = availableWidth >= AppSidebarMinWindowWidth

/**
 * Width left for content once the navigation has taken its share.
 *
 * Only the sidebar takes horizontal space. A bottom or top bar floats over the content in the Liquid Glass
 * layer — the HIG asks for content to extend *beneath* both — so neither narrows the column.
 */
fun appNavigationContentWidthFor(
    availableWidth: Dp,
    placement: AppNavigationPlacement,
): Dp =
    when (placement) {
        AppNavigationPlacement.Sidebar -> (availableWidth - AppSidebarWidth).coerceAtLeast(0.dp)
        AppNavigationPlacement.Bottom, AppNavigationPlacement.Top -> availableWidth
    }

/** Placement for the current window, given the user's stored preference. */
@Composable
fun appNavigationPlacement(sidebarPreferred: Boolean): AppNavigationPlacement =
    appNavigationPlacementFor(
        availableWidth = LocalConfiguration.current.screenWidthDp.dp,
        sidebarPreferred = sidebarPreferred,
    )

/**
 * The placement in force for the content being composed.
 *
 * Published by the pager rather than derived per page, for two reasons. The obvious one is that the user's
 * sidebar preference lives up there. The load-bearing one is that the tab bar is a *single* overlay owned by
 * the pager — all five pages are composed at once behind the scenes, so a bar drawn by each page's own chrome
 * would exist five times and slide with the swipe.
 *
 * What a page does with this is stay out of the way: at [AppNavigationPlacement.Top] the centre of the top row
 * belongs to the tab bar, so the page title moves to the leading edge, which is where iPadOS puts it.
 *
 * Defaults to [AppNavigationPlacement.Bottom], so a pushed route — which has no tab bar over it — keeps the
 * centred title it has always had without needing to opt out.
 */
val LocalAppNavigationPlacement =
    androidx.compose.runtime.compositionLocalOf { AppNavigationPlacement.Bottom }

/** True while the top row's centre is spoken for by the tab bar. */
@Composable
fun appTopBarCentreIsNavigation(): Boolean =
    LocalAppNavigationPlacement.current == AppNavigationPlacement.Top

/**
 * Trailing inset for the actions in the top row.
 *
 * Zero once the tab bar is up there. The content gutter exists to keep a page's trailing controls attached to
 * its content column, which is right while the top row belongs to the page — but at [AppNavigationPlacement.Top]
 * the row belongs to the *app*: title at the leading edge, tab bar centred, actions at the trailing edge, all
 * spanning the window. Keeping the gutter there pulls the actions inward until they collide with the centred
 * tab bar, which is exactly what it did on the Pad at 1280dp.
 *
 * Everywhere else this is just [appPageSideGutter], so a pushed route — which has no tab bar over it — keeps
 * its actions on its own content column.
 */
@Composable
fun appTopBarActionGutter(): Dp =
    if (LocalAppNavigationPlacement.current == AppNavigationPlacement.Top) 0.dp else appPageSideGutter()

/**
 * Edge margin for the top row's own chrome — the title card and the trailing toolbar.
 *
 * `AppChromeTokens.topBarHorizontalPadding` is 14dp, and that is a *phone* margin: on a 426dp window a floating
 * pill 14dp from the edge reads as inset, because the edge is close to everything else too. On a 1280dp panel
 * the same 14dp reads as the pill clinging to the bezel, with the whole width behind it unused.
 *
 * So it steps up once the window does. 28dp is twice the phone margin rather than a new number — margins are
 * meant to grow with the canvas, and the content layer already does exactly this through [appPageSideGutter].
 * This is the same idea applied to the one row that cannot use the content column, because it spans the window.
 */
val AppTopBarRegularEdgePadding: Dp = AppChromeTokens.topBarHorizontalPadding * 2f

/** Edge margin for the top row's chrome at the current placement. */
@Composable
fun appTopBarEdgePadding(): Dp =
    if (LocalAppNavigationPlacement.current == AppNavigationPlacement.Bottom) {
        AppChromeTokens.topBarHorizontalPadding
    } else {
        AppTopBarRegularEdgePadding
    }

/** Side of the square sidebar-toggle button. */
val AppSidebarToggleSize: Dp = 44.dp

/**
 * Leading space the top row must keep clear for app-level navigation before the title starts.
 *
 * Only [AppNavigationPlacement.Top] needs it: that is the one shape where the sidebar toggle lives in the top
 * row, ahead of the title, which is where iPadOS puts it. In the sidebar shape the toggle is inside the rail,
 * and in the bottom shape there is no toggle and the title is centred anyway.
 */
@Composable
fun appTopBarTitleLeadingInset(): Dp =
    if (LocalAppNavigationPlacement.current == AppNavigationPlacement.Top) {
        AppSidebarToggleSize + 8.dp
    } else {
        0.dp
    }

/**
 * Widest the centred tab bar may be in the top row without running into the trailing actions.
 *
 * The row holds three things and only the middle one is centred, so the bar's safe width is the window minus
 * *twice* the space the actions need — symmetric, because a centred element is only clear of the trailing side
 * if it is equally clear of the leading one.
 *
 * Found on the Pad AVD at 650dp, not by reasoning: at 1280dp a 388dp bar and a trailing toolbar have room to
 * spare, and at 650dp they overlapped, with the last tab sitting underneath the actions. The bar is allowed to
 * shrink rather than collide; five tabs in 294dp is tight and legible, five tabs under a toolbar is neither.
 */
fun appTopBarNavigationMaxWidth(availableWidth: Dp): Dp =
    (availableWidth - AppChromeTokens.topBarTitleActionReserve * 2f).coerceAtLeast(240.dp)

/**
 * Whether the top row can afford the page title alongside the centred tab bar.
 *
 * When it cannot, the title is what goes. At [AppNavigationPlacement.Top] the tab bar already shows which
 * section is selected, so the title is the one item in the row that is saying something twice — and the
 * alternative, letting three items overlap, communicates nothing at all.
 */
fun appTopRowFitsTitle(
    availableWidth: Dp,
    barWidth: Dp,
): Boolean = availableWidth - barWidth >= (AppChromeTokens.topBarTitleActionReserve + AppChromeTokens.topBarTitleMinWidth) * 2f
