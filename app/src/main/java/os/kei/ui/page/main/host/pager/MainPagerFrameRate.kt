package os.kei.ui.page.main.host.pager

import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.preferredFrameRate

/**
 * Requests the display's high frame-rate category only while pager motion is visible.
 *
 * The category lets ARR/LTPO, thermal policy, and the available display modes select the actual
 * cadence. Removing the modifier at rest clears the vote so a static page can downshift again.
 */
internal fun Modifier.preferHighFrameRateForPagerMotion(active: Boolean): Modifier =
    if (active) {
        preferredFrameRate(FrameRateCategory.High)
    } else {
        this
    }

internal fun shouldPreferHighFrameRateForPagerMotion(
    pagerScrollInProgress: Boolean,
    additionalMotionInProgress: Boolean,
): Boolean = pagerScrollInProgress || additionalMotionInProgress
