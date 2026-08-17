package os.kei.ui.page.main.ba.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.ba.BaLiquidCard
import os.kei.ui.page.main.ba.BaPageClockState
import os.kei.ui.page.main.ba.baCraftFunctionLabelRes
import os.kei.ui.page.main.ba.support.BA_CRAFT_SLOT_COUNT
import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftState
import os.kei.ui.page.main.ba.support.BaCraftSummary
import os.kei.ui.page.main.ba.support.endAtMs
import os.kei.ui.page.main.ba.support.formatBaDateTimeNoSeconds
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.ba.support.isActive
import os.kei.ui.page.main.ba.support.isComplete
import os.kei.ui.page.main.ba.support.slotAt
import os.kei.ui.page.main.ba.support.summary
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.testing.KeiOsTestTags
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
 *
 * Six rows is the tallest card on the page and most of the time every one of them is idle, so the card
 * folds shut. Collapsing is non-lossy on purpose: the header takes over with a one-line summary of all
 * six slots, so the pile of rows is only worth opening when a slot actually needs configuring.
 * [expanded] is persisted — see `BaPageSnapshot.craftCardExpanded`.
 */
@Composable
internal fun BaCraftCard(
    backdrop: Backdrop?,
    clockState: BaPageClockState,
    craft: BaCraftState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
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
        BaCardHeader(
            title = stringResource(R.string.ba_craft_title),
            // The header is the disclosure control, and the only handle a baseline-profile journey has
            // on the expand/collapse transition. It carries a clickable role below, so the tag does
            // become its own node — a tag on a semantics-free container would not.
            modifier = Modifier.testTag(KeiOsTestTags.BaCraftCardHeader),
            expandable = true,
            expanded = expanded,
            expandTint = accentAmber,
            onClick = { onExpandedChange(!expanded) },
            // Only while collapsed: expanded, the rows below already say all of this, and repeating it
            // in the header is just a second thing to read.
            trailing =
                if (expanded) {
                    null
                } else {
                    {
                        Text(
                            text = baCraftSummaryText(craft.summary(uiNowMs), uiNowMs),
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
        )

        AnimatedVisibility(
            visible = expanded,
            enter = appExpandIn(),
            exit = appExpandOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BaCraftFunction.entries.forEach { function ->
                    val functionLabel = stringResource(baCraftFunctionLabelRes(function))
                    repeat(BA_CRAFT_SLOT_COUNT) { index ->
                        val slot = craft.slotAt(function, index)
                        val complete = slot.isComplete(uiNowMs)
                        val endAtMs = slot.endAtMs()
                        BaInlineActionPanel(
                            backdrop = backdrop,
                            // Only the first Generate slot is tagged: it is the journey's way into the
                            // craft sheet, and six tags would be five that nothing waits for.
                            buttonTestTag =
                                KeiOsTestTags.BaCraftSlotFirst
                                    .takeIf { function == BaCraftFunction.Generate && index == 0 },
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
}

/**
 * The collapsed header's one line.
 *
 * A finished craft outranks a running one: it is the only state the teacher has to act on, and it
 * carries no countdown worth showing. Anything still running reports the nearest completion, because
 * that is the single number the six rows were being scanned for.
 */
@Composable
private fun baCraftSummaryText(
    summary: BaCraftSummary,
    nowMs: Long,
): String {
    val nextAtMs = summary.nextCompletionAtMs
    return when {
        summary.isIdle -> stringResource(R.string.ba_craft_summary_idle)
        summary.readyCount > 0 && summary.runningCount > 0 ->
            stringResource(
                R.string.ba_craft_summary_ready_running_format,
                summary.readyCount,
                summary.runningCount,
            )

        summary.readyCount > 0 -> stringResource(R.string.ba_craft_summary_ready_format, summary.readyCount)
        nextAtMs != null ->
            stringResource(
                R.string.ba_craft_summary_running_format,
                summary.runningCount,
                formatBaRemainingTime(nextAtMs, nowMs),
            )

        // Running with no future completion is unreachable through the UI (a started slot always ends
        // after it started), so this only fires on a clock jump. Report the count and no time rather
        // than an inverted countdown.
        else -> stringResource(R.string.ba_craft_summary_running_only_format, summary.runningCount)
    }
}
