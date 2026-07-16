@file:Suppress("FunctionName")

package os.kei.ui.page.main.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.ui.effect.background.BgEffectBackground
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.feature.home.model.defaultHomeOverviewCards
import os.kei.ui.page.main.home.state.rememberHomePageContentState
import os.kei.ui.page.main.home.state.rememberHomePageHeroMotionState
import os.kei.ui.page.main.home.state.rememberHomePageOverviewCardState
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.osLucideSettingsIcon
import os.kei.ui.page.main.widget.chrome.AppScaffold
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.BindLazyListScrollBoundsEffect
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberActionBarBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop

internal val HomePageCompactLandscapeHeightMax: Dp = 480.dp
private val HOME_PAGE_HERO_TOP_EXTRA = 24.dp

internal fun homePageUsesCompactLandscapeLayout(
    availableWidth: Dp,
    availableHeight: Dp,
): Boolean =
    availableWidth > availableHeight &&
        availableHeight <= HomePageCompactLandscapeHeightMax

internal fun homePageLogoTopPadding(
    scaffoldTopPadding: Dp,
    contentTopPadding: Dp,
    compactHeightPresentation: Boolean,
): Dp =
    if (compactHeightPresentation) {
        scaffoldTopPadding
    } else {
        scaffoldTopPadding + contentTopPadding + HOME_PAGE_HERO_TOP_EXTRA
    }

@Composable
fun HomePage(
    shizukuStatus: String,
    homeAppOverview: HomeAppOverview = HomeAppOverview(),
    mcpOverview: HomeMcpOverview = HomeMcpOverview(),
    homeGitHubOverview: HomeGitHubOverview = HomeGitHubOverview(),
    homeWebDavOverview: HomeWebDavOverview = HomeWebDavOverview(),
    homeBaOverview: HomeBaOverview = HomeBaOverview(),
    runtimeNowMs: Long,
    homeIconHdrEnabled: Boolean,
    homeDynamicFullEffectEnabled: Boolean = false,
    runtime: MainPageRuntime = MainPageRuntime(),
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    visibleBottomPages: Set<BottomPage>,
    visibleOverviewCards: Set<HomeOverviewCard> = defaultHomeOverviewCards(),
    showCacheFreshnessInCards: Boolean = false,
    actionBarSelectedIndex: Int = 1,
    showBottomPageEditor: Boolean = false,
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit = { _, _ -> },
    onCacheFreshnessVisibilityChange: (Boolean) -> Unit = {},
    onActionBarSelectedIndexChange: (Int) -> Unit = {},
    onBottomPageEditorVisibleChange: (Boolean) -> Unit = {},
    onOpenWebDavSync: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val compactLandscapeLayout =
        homePageUsesCompactLandscapeLayout(
            availableWidth = configuration.screenWidthDp.dp,
            availableHeight = configuration.screenHeightDp.dp,
        )
    val lazyListState = rememberLazyListState()
    val pageScope = rememberCoroutineScope()
    BindLazyListScrollBoundsEffect(
        listState = lazyListState,
        isActive = runtime.isPageActive,
        onScrollBoundsChange = runtime.onScrollBoundsChange,
    )

    val effectBackgroundEnabled = runtime.isPageActive
    val homeDynamicActive =
        runtime.isDataActive ||
            (homeDynamicFullEffectEnabled && runtime.isPageActive)
    val dynamicBackgroundEnabled =
        homeDynamicActive &&
            (homeDynamicFullEffectEnabled || !runtime.isPagerScrollInProgress)
    val fullBackdropEffectsEnabled =
        runtime.isPageActive &&
            (
                homeDynamicFullEffectEnabled ||
                    !runtime.isPagerScrollInProgress
            )
    val foregroundBlurActive = fullBackdropEffectsEnabled
    val surfaceColor = MiuixTheme.colorScheme.surface
    val actionBarBackdrop =
        rememberActionBarBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    val homeCardBackdrop =
        if (fullBackdropEffectsEnabled) {
            rememberActionBarBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val foregroundBackdrop =
        if (foregroundBlurActive) {
            rememberMiuixLayerBackdrop()
        } else {
            null
        }
    val contentState =
        rememberHomePageContentState(
            shizukuStatus = shizukuStatus,
            appOverview = homeAppOverview,
            mcpOverview = mcpOverview,
            githubOverview = homeGitHubOverview,
            webDavOverview = homeWebDavOverview,
            baOverview = homeBaOverview,
            runtimeNowMs = runtimeNowMs,
        )

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    val layersIcon = appLucideLayersIcon()
    val aboutIcon = appLucideInfoIcon()
    val settingsIcon = osLucideSettingsIcon()
    val editBottomPagesContentDescription = stringResource(R.string.home_cd_edit_bottom_pages)
    val aboutContentDescription = stringResource(R.string.about_page_title)
    val settingsContentDescription = stringResource(R.string.settings_title)
    val homeActionItems =
        remember(
            editBottomPagesContentDescription,
            aboutContentDescription,
            settingsContentDescription,
            onActionBarSelectedIndexChange,
            onBottomPageEditorVisibleChange,
            onOpenAbout,
            onOpenSettings,
        ) {
            listOf(
                LiquidActionItem(
                    icon = layersIcon,
                    contentDescription = editBottomPagesContentDescription,
                    onClick = {
                        onBottomPageEditorVisibleChange(true)
                    },
                ),
                LiquidActionItem(
                    icon = aboutIcon,
                    contentDescription = aboutContentDescription,
                    onClick = {
                        onActionBarSelectedIndexChange(1)
                        onOpenAbout()
                    },
                ),
                LiquidActionItem(
                    icon = settingsIcon,
                    contentDescription = settingsContentDescription,
                    onClick = {
                        onActionBarSelectedIndexChange(2)
                        onOpenSettings()
                    },
                ),
            )
        }
    val hiddenOverviewCardCount = (HomeOverviewCard.entries.size - visibleOverviewCards.size).coerceAtLeast(0)
    val heroMotionState =
        rememberHomePageHeroMotionState(
            lazyListState = lazyListState,
            homeIconHdrEnabled = homeIconHdrEnabled,
            runtime = runtime,
            hiddenOverviewCardCount = hiddenOverviewCardCount,
        )
    val overviewCardState =
        rememberHomePageOverviewCardState(
            content = contentState,
            mcpOverview = mcpOverview,
            githubOverview = homeGitHubOverview,
            webDavOverview = homeWebDavOverview,
            baOverview = homeBaOverview,
            showCacheFreshnessInCards = showCacheFreshnessInCards,
        )

    Box(modifier = Modifier.fillMaxSize()) {
        AppScaffold(
            topBar = {
                AppTopBarSection(
                    title = "",
                    largeTitle = "",
                    color = Color.Transparent,
                    onTitleClick = {
                        pageScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                )
            },
        ) { innerPadding ->
            HomePageControlSheet(
                show = showBottomPageEditor,
                actionBarBackdrop = actionBarBackdrop,
                visibleBottomPages = visibleBottomPages,
                visibleOverviewCards = visibleOverviewCards,
                homeSheetTitle = stringResource(R.string.home_sheet_bottom_pages_title),
                tableTitle = stringResource(R.string.home_sheet_table_title),
                tableDesc = stringResource(R.string.home_sheet_table_desc),
                homeCardMcp = contentState.homeCardMcp,
                homeCardGitHub = contentState.homeCardGitHub,
                homeCardWebDav = contentState.homeCardWebDav,
                homeCardBa = contentState.homeCardBa,
                showCacheFreshnessInCards = showCacheFreshnessInCards,
                cacheFreshnessToggleLabel = stringResource(R.string.home_sheet_show_cache_freshness),
                cacheFreshnessToggleDesc = stringResource(R.string.home_sheet_show_cache_freshness_desc),
                debugSectionTitle = stringResource(R.string.home_sheet_debug_title),
                onDismissRequest = { onBottomPageEditorVisibleChange(false) },
                onBottomPageVisibilityChange = onBottomPageVisibilityChange,
                onOverviewCardVisibilityChange = onOverviewCardVisibilityChange,
                onCacheFreshnessVisibilityChange = onCacheFreshnessVisibilityChange,
            )

            val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val listContentPadding =
                PaddingValues(
                    start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding() + runtime.contentTopPadding,
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding() + runtime.contentBottomPadding + 16.dp,
                )
            val logoPadding =
                PaddingValues(
                    top =
                        homePageLogoTopPadding(
                            scaffoldTopPadding = innerPadding.calculateTopPadding(),
                            contentTopPadding = runtime.contentTopPadding,
                            compactHeightPresentation = compactLandscapeLayout,
                        ),
                    start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .kyantLayerBackdrop(actionBarBackdrop),
            ) {
                BgEffectBackground(
                    dynamicBackground = dynamicBackgroundEnabled,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                homeCardBackdrop?.let { backdrop ->
                                    Modifier.kyantLayerBackdrop(backdrop)
                                } ?: Modifier,
                            ),
                    bgModifier = foregroundBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                    effectBackground = effectBackgroundEnabled,
                    isFullSize = true,
                    alpha = heroMotionState.bgAlpha,
                ) {
                    HomePageHero(
                        foregroundBackdrop = foregroundBackdrop,
                        foregroundBlurEnabled = foregroundBlurActive,
                        homeIconHdrEnabled = homeIconHdrEnabled,
                        hdrSweepProgress = heroMotionState.hdrSweepProgress,
                        homeHeaderSinkOffset = heroMotionState.homeHeaderSinkOffset,
                        logoPadding = logoPadding,
                        layoutDirection = layoutDirection,
                        homeAppName = contentState.homeAppName,
                        homeTagline = contentState.homeTagline,
                        appVersionText = contentState.appVersionText,
                        avoidanceProgress = heroMotionState.avoidanceProgress,
                        iconProgress = heroMotionState.iconProgress,
                        titleProgress = heroMotionState.titleProgress,
                        summaryProgress = heroMotionState.summaryProgress,
                        statusPills = overviewCardState.homeHeaderStatusPills,
                        compactHeightPresentation = compactLandscapeLayout,
                        onHeroHeightChanged = heroMotionState.onHeroHeightPxChanged,
                        onIconBottomChanged = heroMotionState.onIconBottomChanged,
                        onTitleBottomChanged = heroMotionState.onTitleBottomChanged,
                        onSummaryBottomChanged = heroMotionState.onSummaryBottomChanged,
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding,
                    overscrollEffect = null,
                ) {
                    item(
                        key = "logo_spacer",
                        contentType = "home_logo_spacer",
                    ) {
                        HomePageHeroSpacer(
                            logoHeightDp = heroMotionState.logoHeightDp,
                            logoPadding = logoPadding,
                            listContentPadding = listContentPadding,
                            homeHeaderSinkOffset = heroMotionState.homeHeaderSinkOffset,
                            compactHeightPresentation = compactLandscapeLayout,
                            onLogoHeightPxChanged = heroMotionState.onLogoHeightPxChanged,
                            onLogoAreaBottomChanged = heroMotionState.onLogoAreaBottomChanged,
                        )
                    }

                    item(
                        key = "home_content",
                        contentType = "home_content",
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = listContentPadding.calculateBottomPadding()),
                        ) {
                            HomePageOverviewCards(
                                visibleOverviewCards = visibleOverviewCards,
                                homeCardBackdrop = homeCardBackdrop,
                                blurEnabled = fullBackdropEffectsEnabled,
                                homeNa = contentState.homeNa,
                                mcpPills = overviewCardState.mcpOverviewPills,
                                githubPills = overviewCardState.githubOverviewPills,
                                onOpenWebDavSync = onOpenWebDavSync,
                                webDavPills = overviewCardState.webDavOverviewPills,
                                baPills = overviewCardState.baOverviewPills,
                            )
                        }
                    }
                }
            }
        }

        AppTopEndActionBarOverlay {
            LiquidActionBar(
                backdrop = actionBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                items = homeActionItems,
                selectedIndex = actionBarSelectedIndex,
                onInteractionChanged = onActionBarInteractingChanged,
            )
        }
    }
}
