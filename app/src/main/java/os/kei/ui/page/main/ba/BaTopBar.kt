package os.kei.ui.page.main.ba

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideBellIcon
import os.kei.ui.page.main.os.appLucideEditIcon
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun BaTopBar(
    topBarColor: Color,
    scrollBehavior: ScrollBehavior?,
    titleBackdrop: LayerBackdrop? = null,
    onTitleClick: () -> Unit = {},
) {
    AppTopBarSection(
        title = "",
        largeTitle = stringResource(R.string.ba_topbar_title),
        scrollBehavior = scrollBehavior,
        color = topBarColor,
        titleBackdrop = titleBackdrop,
        titleEndReserve = AppChromeTokens.topBarTitleActionReserve,
        onTitleClick = onTitleClick,
    )
}

@Composable
internal fun BaTopBarActions(
    backdrop: LayerBackdrop,
    liquidActionBarLayeredStyleEnabled: Boolean,
    onShowAccountManagement: () -> Unit,
    onShowSettings: () -> Unit,
    onShowNotificationSettings: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
) {
    val accountIcon = appLucideListIcon()
    val editIcon = appLucideEditIcon()
    val bellIcon = appLucideBellIcon()
    val accountContentDescription = stringResource(R.string.ba_cd_account_management)
    val editContentDescription = stringResource(R.string.ba_cd_edit)
    val notificationContentDescription = stringResource(R.string.ba_cd_notification_settings)
    val actionItems =
        remember(
            accountContentDescription,
            editContentDescription,
            notificationContentDescription,
            onShowAccountManagement,
            onShowSettings,
            onShowNotificationSettings,
        ) {
            listOf(
                LiquidToolbarAction(
                    icon = accountIcon,
                    contentDescription = accountContentDescription,
                    onClick = onShowAccountManagement,
                ),
                LiquidToolbarAction(
                    icon = bellIcon,
                    contentDescription = notificationContentDescription,
                    onClick = onShowNotificationSettings,
                ),
                LiquidToolbarAction(
                    icon = editIcon,
                    contentDescription = editContentDescription,
                    onClick = onShowSettings,
                ),
            )
        }

    LiquidToolbar(
        backdrop = backdrop,
        layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        actions = actionItems,
    )
}
