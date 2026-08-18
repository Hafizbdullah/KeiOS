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
