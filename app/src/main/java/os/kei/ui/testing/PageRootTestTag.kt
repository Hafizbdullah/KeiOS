package os.kei.ui.testing

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Marks a route's root so a macrobenchmark journey can wait for the page to arrive.
 *
 * A bare [testTag] is invisible to UiAutomator: it only reaches the view hierarchy's resource id
 * once an ancestor sets [testTagsAsResourceId]. Every page that has needed a baseline-profile
 * journey has had to remember both halves, and forgetting the second one fails only at run time on
 * a device, several minutes into a profile run. Pairing them here means a page root is tagged
 * correctly or not at all.
 */
fun Modifier.pageRootTestTag(tag: String): Modifier =
    this
        .semantics { testTagsAsResourceId = true }
        .testTag(tag)
