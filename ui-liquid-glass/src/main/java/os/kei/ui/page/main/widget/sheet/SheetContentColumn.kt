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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val scrollStateReporter by rememberUpdatedState(LocalLiquidSheetContentScrollStateReporter.current)
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
    LaunchedEffect(scrollable, scrollState) {
        snapshotFlow { scrollable && scrollState.value > 0 }
            .distinctUntilChanged()
            .collect { canScrollUp ->
                scrollStateReporter(canScrollUp)
            }
    }
    DisposableEffect(
        scrollable,
        overflowReporter,
        scrollStateReporter,
        managedScrollableContentReporter,
    ) {
        onDispose {
            overflowReporter(false)
            scrollStateReporter(false)
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
