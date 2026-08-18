@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.chrome.AppChromeTokens

private val SheetContentShadowEdgePadding = 8.dp
private val SheetContentBottomPadding = 12.dp

@Composable
fun SheetContentColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    verticalSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    val overflowReporter by rememberUpdatedState(LocalLiquidSheetContentOverflowReporter.current)
    val managedScrollableContentReporter by rememberUpdatedState(
        LocalLiquidSheetManagedScrollableContentReporter.current,
    )
    val scrollModifier =
        if (scrollable) {
            Modifier.verticalScroll(scrollState)
        } else {
            Modifier
        }
    LaunchedEffect(scrollable) {
        managedScrollableContentReporter(scrollable)
    }
    LaunchedEffect(scrollable, scrollState.maxValue) {
        overflowReporter(scrollable && scrollState.maxValue > 0)
    }
    // The sheet reads this state directly in its nested-scroll callbacks. See
    // [LiquidSheetContentScroll] for why the reporter round trip this replaces could not be made
    // correct: it published a stale value, and worse, it published a *wrong* one whenever the host
    // sheet recomposed.
    //
    // Keyed on the link and the scroll state, both of which are `remember`ed — never on a lambda, so
    // an unrelated recomposition of the host cannot detach a live scroll position.
    val contentScroll = LocalLiquidSheetContentScroll.current
    // `remember`ed so the key below is stable: a source allocated per composition would make this
    // effect re-run on every recomposition of the host, which is the exact failure the KDoc on
    // [LiquidSheetContentScroll] describes.
    val scrollSource = remember(scrollState) { ScrollStateSheetContentSource(scrollState) }
    DisposableEffect(contentScroll, scrollSource, scrollable) {
        if (scrollable) contentScroll?.attach(scrollSource)
        onDispose { contentScroll?.detach(scrollSource) }
    }
    DisposableEffect(scrollable, overflowReporter, managedScrollableContentReporter) {
        onDispose {
            overflowReporter(false)
            managedScrollableContentReporter(false)
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(scrollModifier)
                .navigationBarsPadding()
                .padding(
                    top = SheetContentShadowEdgePadding,
                    bottom = SheetContentBottomPadding + SheetContentShadowEdgePadding,
                ),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        content()
    }
}
