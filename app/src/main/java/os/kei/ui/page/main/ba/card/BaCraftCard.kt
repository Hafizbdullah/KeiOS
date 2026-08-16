package os.kei.ui.page.main.ba.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.ba.BaLiquidCard
import os.kei.ui.page.main.ba.BaPageClockState
import os.kei.ui.page.main.ba.baCraftFunctionLabelRes
import os.kei.ui.page.main.ba.support.BA_CRAFT_SLOT_COUNT
import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftState
import os.kei.ui.page.main.ba.support.endAtMs
import os.kei.ui.page.main.ba.support.formatBaDateTimeNoSeconds
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.ba.support.isActive
import os.kei.ui.page.main.ba.support.isComplete
import os.kei.ui.page.main.ba.support.slotAt

/**
 * Craft Chamber timers: three Generate slots and three Fusion slots, all six per account.
 *
 * Rows reuse [BaInlineActionPanel] rather than growing a new shape, because a craft slot is the same
 * three facts the cafe cooldown rows already show — an actionable button, a countdown, and the instant
 * it lands — and the gesture language carries over too: tap to configure, long-press to clear.
 *
 * The countdown reads [BaPageClockState.uiMinuteMs], the page's existing minute ticker. Nothing here
 * starts a clock of its own: a craft is 30 minutes at the shortest, so a per-second tick would buy
 * nothing but wakeups.
 */
@Composable
internal fun BaCraftCard(
    backdrop: Backdrop?,
    clockState: BaPageClockState,
    craft: BaCraftState,
    onConfigureSlot: (BaCraftFunction, Int) -> Unit,
    onClearSlot: (BaCraftFunction, Int) -> Unit,
) {
    val uiNowMs = clockState.uiMinuteMs.longValue
    val accentAmber = Color(0xFFFBBF24)
    val notSyncedText = stringResource(R.string.ba_state_not_synced)
    val idleCountdownText = stringResource(R.string.ba_craft_slot_idle_countdown)
    val idleText = stringResource(R.string.ba_craft_slot_idle)
    val doneText = stringResource(R.string.ba_craft_slot_done)

    BaLiquidCard(
        backdrop = backdrop,
        accentColor = accentAmber,
        accentAlpha = 0f,
    ) {
        BaCardHeader(title = stringResource(R.string.ba_craft_title))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BaCraftFunction.entries.forEach { function ->
                val functionLabel = stringResource(baCraftFunctionLabelRes(function))
                repeat(BA_CRAFT_SLOT_COUNT) { index ->
                    val slot = craft.slotAt(function, index)
                    val complete = slot.isComplete(uiNowMs)
                    val endAtMs = slot.endAtMs()
                    BaInlineActionPanel(
                        backdrop = backdrop,
                        buttonText =
                            stringResource(
                                R.string.ba_craft_slot_button_format,
                                functionLabel,
                                // The game numbers its slots from one; do not leak the index.
                                index + 1,
                            ),
                        countdownText =
                            when {
                                !slot.isActive() -> idleCountdownText
                                complete -> "0m"
                                else -> formatBaRemainingTime(endAtMs, uiNowMs)
                            },
                        timeText =
                            when {
                                !slot.isActive() -> idleText
                                complete -> doneText
                                else -> formatBaDateTimeNoSeconds(endAtMs, notSyncedText)
                            },
                        accentColor = accentAmber,
                        enabled = true,
                        onClick = { onConfigureSlot(function, index) },
                        onLongClick = { onClearSlot(function, index) },
                    )
                }
            }
        }
    }
}
