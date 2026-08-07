package os.kei.ui.page.main.os

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun OsPageScaffoldShell(
    scrollBehavior: ScrollBehavior,
    topBarColor: Color,
    topBarBackdrop: LayerBackdrop,
    layeredStyleEnabled: Boolean,
    manageCardsContentDescription: String,
    manageActivitiesContentDescription: String,
    manageShellCardsContentDescription: String,
    refreshParamsContentDescription: String,
    refreshing: Boolean,
    onOpenCardManager: () -> Unit,
    onOpenActivityVisibilityManager: () -> Unit,
    onOpenShellCardVisibilityManager: () -> Unit,
    onRefresh: () -> Unit,
    onTitleClick: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val manageCardsIcon = appLucideLayersIcon()
    val manageActivitiesIcon = appLucideAppWindowIcon()
    val manageShellCardsIcon = osLucideShellIcon()
    val actionItems =
        remember(
            manageCardsContentDescription,
            manageActivitiesContentDescription,
            manageShellCardsContentDescription,
            onOpenCardManager,
            onOpenActivityVisibilityManager,
            onOpenShellCardVisibilityManager,
        ) {
            listOf(
                LiquidToolbarAction(
                    icon = manageCardsIcon,
                    contentDescription = manageCardsContentDescription,
                    onClick = onOpenCardManager,
                ),
                LiquidToolbarAction(
                    icon = manageActivitiesIcon,
                    contentDescription = manageActivitiesContentDescription,
                    onClick = onOpenActivityVisibilityManager,
                ),
                LiquidToolbarAction(
                    icon = manageShellCardsIcon,
                    contentDescription = manageShellCardsContentDescription,
                    onClick = onOpenShellCardVisibilityManager,
                ),
            )
        }

    AppPageScaffold(
        title = "",
        modifier = Modifier.fillMaxSize(),
        largeTitle = "OS",
        scrollBehavior = scrollBehavior,
        topBarColor = topBarColor,
        titleBackdrop = topBarBackdrop,
        reserveTopEndActionSpace = true,
        onTitleClick = onTitleClick,
        actions = {
            LiquidToolbar(
                backdrop = topBarBackdrop,
                layeredStyleEnabled = layeredStyleEnabled,
                actions = actionItems,
            )
        },
        content = content,
    )
}
