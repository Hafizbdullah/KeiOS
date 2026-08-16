package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The sheet's live view of its content's scroll position.
 *
 * Every vertical drag on a sheet has to be arbitrated between resizing the sheet and scrolling its
 * content, and that decision is only ever right if the sheet knows two things *at the instant the
 * event arrives*: whether the content is still at its top, and whether it overflows at all.
 *
 * The signal this replaces travelled `snapshotFlow -> collect -> reporter lambda -> state write ->
 * recomposition`, which was both a frame or more late and outright lossy. `SheetContentColumn`'s
 * `DisposableEffect` was keyed on the reporter *lambdas*, so any recomposition of the host sheet ran
 * its `onDispose` and published "cannot scroll up" while the content sat mid-scroll — and because
 * the publishing flow was `distinctUntilChanged`, it never re-sent the `true` it had already
 * emitted. Nothing corrected it until the user scrolled back to the very top and away again. In the
 * add/edit tracking sheet, which recomposes on every keystroke, the flag was therefore false almost
 * all of the time, and a false flag is precisely what tells the sheet to take the drag away from
 * the content.
 *
 * Holding the [ScrollState] itself removes the round trip. The nested-scroll callbacks read it
 * outside composition, so they see the value the frame's gesture actually has.
 */
@Stable
class LiquidSheetContentScroll internal constructor() {
    private var scrollState by mutableStateOf<ScrollState?>(null)

    /** True once the content has been scrolled away from its top, so it owns an upward drag. */
    val canScrollUp: Boolean
        get() = (scrollState?.value ?: 0) > 0

    /**
     * True when the content is taller than its viewport.
     *
     * Load-bearing for the expand-to-scroll gesture: growing the sheet to reveal more content is
     * only worth taking a drag away from the content if there *is* more content. Without this the
     * sheet inflated to full height on the first upward drag of any short sheet and left several
     * hundred pixels of empty glass below the content.
     */
    val overflows: Boolean
        get() = (scrollState?.maxValue ?: 0) > 0

    internal fun attach(state: ScrollState) {
        scrollState = state
    }

    internal fun detach(state: ScrollState) {
        if (scrollState === state) scrollState = null
    }
}

@Composable
internal fun rememberLiquidSheetContentScroll(): LiquidSheetContentScroll = remember { LiquidSheetContentScroll() }

/**
 * Null outside a Liquid sheet, which is the normal case for [SheetContentColumn] — it is also used
 * inside alerts and plain pages, and those have no sheet height to arbitrate against.
 */
internal val LocalLiquidSheetContentScroll = staticCompositionLocalOf<LiquidSheetContentScroll?> { null }
