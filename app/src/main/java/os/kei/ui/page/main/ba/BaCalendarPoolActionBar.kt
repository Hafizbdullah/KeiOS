@file:Suppress("FunctionName")

package os.kei.ui.page.main.ba

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction

@Composable
internal fun BaCalendarPoolActionBar(
    backdrop: Backdrop,
    settingsContentDescription: String,
    refreshContentDescription: String,
    refreshing: Boolean,
    refreshIconRotation: Float,
    refreshingTint: Color,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val settingsIcon = appLucideConfigIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems =
        remember(
            settingsContentDescription,
            refreshContentDescription,
            refreshing,
            refreshIconRotation,
            refreshingTint,
            onOpenSettings,
            onRefresh,
        ) {
            listOf(
                LiquidToolbarAction(
                    icon = settingsIcon,
                    contentDescription = settingsContentDescription,
                    onClick = onOpenSettings,
                ),
                LiquidToolbarAction(
                    icon = refreshIcon,
                    contentDescription = refreshContentDescription,
                    onClick = onRefresh,
                    iconRotationDegrees = refreshIconRotation,
                    iconTint = refreshingTint.takeIf { refreshing },
                    // Was `selectedIndex = if (refreshing) 1 else 0` on the bar, which is a tab
                    // bar's vocabulary. The refresh action is simply on while it runs.
                    active = refreshing,
                ),
            )
        }

    LiquidToolbar(
        backdrop = backdrop,
        actions = actionItems,
    )
}
