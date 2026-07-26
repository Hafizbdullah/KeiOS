@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.appMotionDpState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

internal val LocalAppScaffoldContainerColor = staticCompositionLocalOf<Color?> { null }

fun appPageContentPadding(
    innerPadding: PaddingValues,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = 0.dp,
): PaddingValues =
    PaddingValues(
        top = innerPadding.calculateTopPadding() + topExtra,
        bottom = innerPadding.calculateBottomPadding() + bottomExtra,
        start = AppChromeTokens.pageHorizontalPadding,
        end = AppChromeTokens.pageHorizontalPadding,
    )

fun appPageBottomPaddingWithFloatingOverlay(contentBottomPadding: Dp): Dp =
    contentBottomPadding + AppChromeTokens.pageFloatingOverlayBottomExtra

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val resolvedContainerColor =
        containerColor
            ?: LocalAppScaffoldContainerColor.current
            ?: MiuixTheme.colorScheme.surface
    MiuixScaffold(
        modifier = modifier,
        containerColor = resolvedContainerColor,
        topBar = topBar,
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun AppPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    scrollBehavior: ScrollBehavior? = null,
    topBarColor: Color = Color.Transparent,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleBackdrop: Backdrop? = null,
    reserveTopEndActionSpace: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
    searchBarVisible: Boolean = false,
    searchBarAnimationLabelPrefix: String = "appPageSearch",
    searchBarContent: (@Composable BoxScope.() -> Unit)? = null,
    onTitleClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val hasSearchBar = searchBarContent != null
    val searchBarPlaceholderHeightState =
        appMotionDpState(
            targetValue =
                if (hasSearchBar && searchBarVisible) {
                    AppChromeTokens.searchBarHostHeight
                } else {
                    0.dp
                },
            durationMillis = AppMotionTokens.searchBarSlideMs,
            label = "${searchBarAnimationLabelPrefix}ScaffoldPadding",
        )
    val currentContent = rememberUpdatedState(content)
    val scaffoldTopBar: @Composable () -> Unit =
        remember(
            scrollBehavior,
            topBarColor,
        ) {
            {
                AppTopBarSection(
                    title = "",
                    largeTitle = "",
                    scrollBehavior = scrollBehavior,
                    color = topBarColor,
                )
            }
        }
    Box(modifier = modifier) {
        AppScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = scaffoldTopBar,
            bottomBar = bottomBar,
            content = { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                val searchBarPlaceholderHeight = searchBarPlaceholderHeightState.value
                val adjustedPadding =
                    remember(innerPadding, layoutDirection, searchBarPlaceholderHeight) {
                        PaddingValues(
                            top = innerPadding.calculateTopPadding() + searchBarPlaceholderHeight,
                            bottom = innerPadding.calculateBottomPadding(),
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        )
                    }
                currentContent.value(adjustedPadding)
            },
        )
        AppTopBarSection(
            title = title,
            largeTitle = largeTitle,
            scrollBehavior = scrollBehavior,
            color = Color.Transparent,
            navigationIcon = navigationIcon,
            titleBackdrop = titleBackdrop,
            titleEndReserve =
                if (reserveTopEndActionSpace) {
                    AppChromeTokens.topBarTitleActionReserve
                } else {
                    null
                },
            onTitleClick = onTitleClick,
            searchBarVisible = searchBarVisible,
            searchBarAnimationLabelPrefix = searchBarAnimationLabelPrefix,
            searchBarContent = searchBarContent,
        )
        AppTopEndActionBarOverlay {
            Row {
                actions()
            }
        }
    }
}

@Composable
fun AppPageLazyColumn(
    innerPadding: PaddingValues,
    state: LazyListState,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = AppChromeTokens.pageBottomInsetExtra,
    topExtra: Dp = AppChromeTokens.topBarToHeaderGap,
    sectionSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
    userScrollEnabled: Boolean = true,
    // Shared pages keep bounce disabled (767b191c3); pilots opt in with an explicit effect.
    overscrollEffect: OverscrollEffect? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        overscrollEffect = overscrollEffect,
        userScrollEnabled = userScrollEnabled,
        contentPadding =
            appPageContentPadding(
                innerPadding = innerPadding,
                bottomExtra = bottomExtra,
                topExtra = topExtra,
            ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        content = content,
    )
}
