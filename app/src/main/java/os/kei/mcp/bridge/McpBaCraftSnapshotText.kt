package os.kei.mcp.bridge

import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftState
import os.kei.ui.page.main.ba.support.effectiveDurationMs
import os.kei.ui.page.main.ba.support.endAtMs
import os.kei.ui.page.main.ba.support.isActive
import os.kei.ui.page.main.ba.support.isComplete
import os.kei.ui.page.main.ba.support.slotAt
import java.util.Locale

/**
 * One Craft Chamber slot, in the `entry[i]=k:v | k:v` shape the calendar and pool texts already use.
 *
 * Grades are emitted as their enum names, not the localized labels the card shows, because this is read
 * by a machine: the names are exactly the `@SerialName` values on disk, so they survive a device
 * changing language. Same reason `state` is a fixed token rather than the card's "Idle"/"Complete".
 *
 * Slots are numbered from one, matching the game and the card.
 *
 * Lives outside [AppMcpBaToolDelegate] only so it can be tested without an MMKV: everything it reads is
 * already pure, and the delegate's own entry point is not.
 */
internal fun mcpBaCraftSlotLine(
    craft: BaCraftState,
    function: BaCraftFunction,
    index: Int,
    nowMs: Long,
): String {
    val slot = craft.slotAt(function, index)
    val state =
        when {
            !slot.isActive() -> "idle"
            slot.isComplete(nowMs) -> "ready"
            else -> "running"
        }
    val name = function.name.lowercase(Locale.ROOT)
    return buildString {
        append("craftSlot[$name${index + 1}]=state:$state")
        append(" | startedAtMs:${slot.startedAtMs}")
        append(" | endAtMs:${slot.endAtMs()}")
        append(" | durationMs:${slot.effectiveDurationMs()}")
        append(" | customDuration:${slot.customDurationMs > 0L}")
        append(" | grades:${slot.grades.joinToString(",") { it.name.lowercase(Locale.ROOT) }}")
        append(" | label:${slot.label}")
    }
}
