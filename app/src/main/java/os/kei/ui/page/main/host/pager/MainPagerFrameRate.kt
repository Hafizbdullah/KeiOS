package os.kei.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.preferredFrameRate

/**
 * The panel's fastest mode, in Hz, or `0f` when it cannot be resolved.
 *
 * [androidx.compose.ui.FrameRateCategory.High] is not "as fast as possible": SurfaceFlinger maps
 * the category through the display's own `frameRateCategoryRate`, and on 5eea1f50 that is
 * `{normal = 60, high = 90}`. Voting `High` on a 120Hz panel therefore asks for **90Hz** and the
 * section switch visibly drops out of 120 — the vote meant to keep motion smooth was the thing
 * capping it. Asking for an explicit rate skips the category table.
 */
@Composable
internal fun rememberDisplayPeakFrameRate(): Float {
    val context = LocalContext.current
    return remember(context) {
        val display = context.display ?: return@remember 0f
        display.supportedModes.maxOfOrNull { mode -> mode.refreshRate } ?: display.refreshRate
    }
}

/**
 * Requests the panel's peak rate while pager motion is visible.
 *
 * Removing the modifier at rest clears the vote so a static page can downshift again — ARR and
 * thermal policy still own the actual cadence, this only lifts the ceiling the category imposed.
 */
internal fun Modifier.preferHighFrameRateForPagerMotion(
    active: Boolean,
    peakFrameRate: Float,
): Modifier =
    if (active && peakFrameRate > 0f) {
        preferredFrameRate(peakFrameRate)
    } else {
        this
    }

internal fun shouldPreferHighFrameRateForPagerMotion(
    pagerScrollInProgress: Boolean,
    additionalMotionInProgress: Boolean,
): Boolean = pagerScrollInProgress || additionalMotionInProgress
