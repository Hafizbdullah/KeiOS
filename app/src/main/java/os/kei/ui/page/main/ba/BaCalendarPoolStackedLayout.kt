package os.kei.ui.page.main.ba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.glass.AppEdgeStackListTopInset
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackContainer
import os.kei.ui.page.main.widget.glass.rememberAppEdgeStackState

@Composable
internal fun BaCalendarPoolStackedLayout(
    innerPadding: PaddingValues,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    backdrop: Backdrop,
    serverOptions: List<String>,
    serverIndex: Int,
    syncText: String,
    syncTextColor: Color,
    showServerPopup: Boolean,
    serverPopupAnchorBounds: IntRect?,
    onServerPopupChange: (Boolean) -> Unit,
    onServerPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onServerSelected: (Int) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val edgeStackState = rememberAppEdgeStackState(stackLine = AppEdgeStackListTopInset)

    Column(modifier = Modifier.fillMaxSize()) {
        // Server selection is the stable page anchor. Data cards scroll and stack below it.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppChromeTokens.pageHorizontalPadding,
                        end = AppChromeTokens.pageHorizontalPadding,
                        top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap,
                    ),
        ) {
            BaCalendarPoolServerPanel(
                backdrop = backdrop,
                serverOptions = serverOptions,
                serverIndex = serverIndex,
                syncText = syncText,
                syncTextColor = syncTextColor,
                expanded = showServerPopup,
                anchorBounds = serverPopupAnchorBounds,
                onExpandedChange = onServerPopupChange,
                onAnchorBoundsChange = onServerPopupAnchorBoundsChange,
                onServerSelected = onServerSelected,
            )
        }

        CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState) {
            AppPageLazyColumn(
                innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection)
                        .appEdgeStackContainer(edgeStackState),
                bottomExtra = 40.dp,
                topExtra = AppEdgeStackListTopInset,
                sectionSpacing = 14.dp,
                content = content,
            )
        }
    }
}
