package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/*
 * Toast policy and state. The host that renders it is in LiquidToastHost.kt; the pill's material is
 * in LiquidToastSurface.kt.
 */

/** Default on-screen time for a Short / Long toast. */
private val TOAST_DEFAULT_DISPLAY = 2800.milliseconds
private val TOAST_LONG_DISPLAY = 4500.milliseconds

/** Shortened on-screen time used while a backlog is waiting, so newer toasts surface sooner. */
private val TOAST_BACKLOG_DISPLAY = 1400.milliseconds

/**
 * Hard floor for the expedited display time. Backlog acceleration must never drop a toast below the
 * time it physically takes to animate in, settle, and animate out — otherwise a burst would flash
 * unreadable blips.
 */
private val TOAST_MIN_VISIBLE = 1100.milliseconds

/** Timer poll interval so a backlog appearing mid-display can still expedite the current toast. */
internal val TOAST_TIMER_TICK = 250.milliseconds

/** At most this many toasts are shown stacked at once; the rest are queued. */
private const val MAX_VISIBLE_TOASTS = 2

/** Upper bound on queued toasts so a runaway burst can't backlog into a multi-minute replay. */
private const val MAX_TOAST_QUEUE = 4

/** The base display time presets a caller picks between. */
enum class LiquidToastDuration(
    internal val duration: Duration,
) {
    Short(TOAST_DEFAULT_DISPLAY),
    Long(TOAST_LONG_DISPLAY),
}

/** One toast message. */
data class LiquidToastData(
    val message: String,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.Unspecified,
    val duration: LiquidToastDuration = LiquidToastDuration.Short,
)

/**
 * A single display slot: the toast payload plus a monotonic, always-unique [token].
 *
 * The token is what the host keys its state notifications and auto-dismiss timer on. Keying on the
 * [LiquidToastData] value (a data class) was the original bug: two identical messages compare equal,
 * so promoting an equal-valued queued item neither triggered a Compose state change nor restarted the
 * dismiss timer — leaving the toast stuck on screen forever.
 */
@Stable
internal data class LiquidToastSlot(
    val data: LiquidToastData,
    val token: Long,
)

/**
 * State holder for [LiquidToastHost]. Create via [rememberLiquidToastState].
 *
 * Display model:
 * - Up to [MAX_VISIBLE_TOASTS] toasts are shown stacked at once (each in its own slot/position).
 * - Further toasts wait in a FIFO queue (capped at [MAX_TOAST_QUEUE]); when a visible toast
 *   dismisses, the next queued one is promoted into its place.
 * - Identical messages already visible (or just queued) are collapsed so a double-tap doesn't replay
 *   the same toast.
 *
 * All mutations are guarded by [lock] so concurrent `show`/`dismiss` from different threads
 * (ViewModels, coroutines, the main thread) can't interleave into an inconsistent state.
 */
@Stable
class LiquidToastState {
    private val lock = Any()

    internal var visibleSlots by mutableStateOf<List<LiquidToastSlot>>(emptyList())
        private set

    /** Whether any toast is on screen. */
    val isVisible: Boolean
        get() = visibleSlots.isNotEmpty()

    private val queue = ArrayDeque<LiquidToastSlot>()
    private var tokenSeq = 0L

    private fun nextToken(): Long = ++tokenSeq

    /** True when more toasts are waiting behind the currently visible ones. */
    internal val hasBacklog: Boolean
        get() = synchronized(lock) { queue.isNotEmpty() }

    /**
     * Show a toast message. If the visible stack is full, the message is enqueued and promoted when a
     * slot frees up.
     */
    fun show(
        message: String,
        icon: ImageVector? = null,
        iconTint: Color = Color.Unspecified,
        duration: LiquidToastDuration = LiquidToastDuration.Short,
    ) {
        val data =
            LiquidToastData(
                message = message,
                icon = icon,
                iconTint = iconTint,
                duration = duration,
            )
        synchronized(lock) {
            // Collapse a repeated identical message (already on screen or last queued) so a
            // double-tap doesn't stack/replay the same toast.
            if (visibleSlots.any { it.data == data } || queue.lastOrNull()?.data == data) {
                return
            }
            val slot = LiquidToastSlot(data = data, token = nextToken())
            if (visibleSlots.size < MAX_VISIBLE_TOASTS) {
                visibleSlots = visibleSlots + slot
            } else {
                // Favor newer toasts: when the queue is full, drop the oldest still-waiting one to
                // make room, so the most recent message is the one that eventually surfaces.
                if (queue.size >= MAX_TOAST_QUEUE) {
                    queue.removeFirstOrNull()
                }
                queue.addLast(slot)
            }
        }
    }

    /**
     * Dismiss a specific visible toast by token, promoting a queued one into its place if any. Called
     * by the host once a toast's exit animation completes. Each promoted slot carries a fresh unique
     * token, so the host reliably arms a new timer for it.
     */
    internal fun dismiss(token: Long) {
        synchronized(lock) {
            val remaining = visibleSlots.filterNot { it.token == token }
            if (remaining.size == visibleSlots.size) return // already gone
            val next = queue.removeFirstOrNull()
            visibleSlots = if (next != null) remaining + next else remaining
        }
    }

    /** Clear every toast immediately (e.g. on navigation away). */
    fun dismissAll() {
        synchronized(lock) {
            queue.clear()
            visibleSlots = emptyList()
        }
    }
}

/** Remember a [LiquidToastState] across recompositions. */
@Composable
fun rememberLiquidToastState(): LiquidToastState = remember { LiquidToastState() }

/**
 * Resolve how long a toast should stay on screen.
 *
 * - Not expedited (no backlog, or accessibility-driven): use [base] verbatim.
 * - Expedited: shorten toward [TOAST_BACKLOG_DISPLAY] so newer toasts surface sooner, but clamp to
 *   [TOAST_MIN_VISIBLE] so a burst can never flash a toast away before it can be read. If [base] is
 *   itself shorter than the backlog target (unusual), it is respected — acceleration only shortens.
 *
 * Pure function (no Compose/Android deps) so the burst-vs-readability tradeoff is unit-testable.
 */
internal fun resolveToastDisplayLimit(
    base: Duration,
    expedited: Boolean,
): Duration {
    if (!expedited) return base
    return minOf(base, TOAST_BACKLOG_DISPLAY).coerceAtLeast(TOAST_MIN_VISIBLE)
}

/** The base duration used to probe whether an accessibility service is overriding timeouts. */
internal val toastAccessibilityProbeDuration: Duration = TOAST_DEFAULT_DISPLAY
