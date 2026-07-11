package os.kei.ui.page.main.home.state

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.onEach
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.ui.page.main.home.HomeCardPillItem
import os.kei.ui.page.main.home.HomeHeaderStatusPillState
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import os.kei.ui.page.main.widget.status.AppStatusColors
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val HOME_HEADER_SINK_PER_HIDDEN_CARD_DP = 22
private val HOME_HERO_AVOIDANCE_SCROLL_DISTANCE_DP = 128.dp

internal data class HomePageHeroMotionState(
    val bgAlpha: () -> Float,
    val hdrSweepProgress: () -> Float,
    val logoHeightDp: Dp,
    val homeHeaderSinkOffset: Dp,
    val avoidanceProgress: () -> Float,
    val iconProgress: () -> Float,
    val titleProgress: () -> Float,
    val summaryProgress: () -> Float,
    val onHeroHeightPxChanged: (Int) -> Unit,
    val onLogoHeightPxChanged: (Int) -> Unit,
    val onLogoAreaBottomChanged: (Float) -> Unit,
    val onIconBottomChanged: (Float) -> Unit,
    val onTitleBottomChanged: (Float) -> Unit,
    val onSummaryBottomChanged: (Float) -> Unit,
)

@Immutable
internal data class HomePageOverviewCardState(
    val homeHeaderStatusPills: List<HomeHeaderStatusPillState>,
    val mcpOverviewPills: List<HomeCardPillItem>,
    val githubOverviewPills: List<HomeCardPillItem>,
    val webDavOverviewPills: List<HomeCardPillItem>,
    val baOverviewPills: List<HomeCardPillItem>,
)

@Composable
internal fun rememberHomePageHeroMotionState(
    lazyListState: LazyListState,
    homeIconHdrEnabled: Boolean,
    runtime: MainPageRuntime,
    hiddenOverviewCardCount: Int,
): HomePageHeroMotionState {
    val density = LocalDensity.current
    var logoHeightPx by remember { mutableIntStateOf(0) }
    var lastListIndex by remember { mutableIntStateOf(0) }
    var lastListOffsetPx by remember { mutableIntStateOf(0) }
    var bgAlpha by remember { mutableFloatStateOf(1f) }
    var logoHeightDp by remember { mutableStateOf(300.dp) }
    var logoAreaY by remember { mutableFloatStateOf(0f) }
    var iconY by remember { mutableFloatStateOf(0f) }
    var titleY by remember { mutableFloatStateOf(0f) }
    var summaryY by remember { mutableFloatStateOf(0f) }
    var initialLogoAreaY by remember { mutableFloatStateOf(0f) }
    val avoidanceScrollDistancePx =
        remember(density) {
            with(density) { HOME_HERO_AVOIDANCE_SCROLL_DISTANCE_DP.toPx() }
        }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val hdrSweepProgressProvider =
        if (
            homeIconHdrEnabled &&
            transitionAnimationsEnabled &&
            runtime.isDataActive &&
            !runtime.isPagerScrollInProgress
        ) {
            val hdrSweep = rememberInfiniteTransition(label = "kei_hdr_sweep")
            val animated =
                hdrSweep.animateFloat(
                    initialValue = -0.35f,
                    targetValue = 1.35f,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                tween(
                                    durationMillis = resolvedMotionDuration(4600, transitionAnimationsEnabled),
                                    easing = LinearEasing,
                                ),
                        ),
                    label = "kei_hdr_sweep_progress",
                )
            remember(animated) { { animated.value } }
        } else {
            remember { { 0f } }
        }
    var iconProgress by remember { mutableFloatStateOf(0f) }
    var titleProgress by remember { mutableFloatStateOf(0f) }
    var summaryProgress by remember { mutableFloatStateOf(0f) }
    var avoidanceProgress by remember { mutableFloatStateOf(0f) }
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    val bgAlphaProvider = remember { { bgAlpha } }
    val avoidanceProgressProvider = remember { { avoidanceProgress } }
    val iconProgressProvider = remember { { iconProgress } }
    val titleProgressProvider = remember { { titleProgress } }
    val summaryProgressProvider = remember { { summaryProgress } }

    LaunchedEffect(lazyListState, snapshotFlowManager) {
        snapshotFlowManager
            .snapshotFlow {
                lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
            }.onEach { (index, offset) ->
                lastListIndex = index
                lastListOffsetPx = offset
                val nextBgAlpha =
                    1f -
                        homeHeroScrollProgress(
                            index = index,
                            offsetPx = offset,
                            logoHeightPx = logoHeightPx,
                        )
                if (bgAlpha != nextBgAlpha) bgAlpha = nextBgAlpha

                if (index > 0) {
                    if (avoidanceProgress != 1f) avoidanceProgress = 1f
                    if (iconProgress != 1f) iconProgress = 1f
                    if (titleProgress != 1f) titleProgress = 1f
                    if (summaryProgress != 1f) summaryProgress = 1f
                    return@onEach
                }
                val nextAvoidanceProgress =
                    homeHeroAvoidanceProgress(
                        offsetPx = offset.toFloat(),
                        distancePx = avoidanceScrollDistancePx,
                    )
                if (avoidanceProgress != nextAvoidanceProgress) {
                    avoidanceProgress = nextAvoidanceProgress
                }

                if (initialLogoAreaY == 0f && logoAreaY > 0f) {
                    initialLogoAreaY = logoAreaY
                }
                val refLogoAreaY = if (initialLogoAreaY > 0f) initialLogoAreaY else logoAreaY

                val stage1 = (refLogoAreaY - summaryY).coerceAtLeast(1f)
                val stage2 = (summaryY - titleY).coerceAtLeast(1f)
                val stage3 = (titleY - iconY).coerceAtLeast(1f)

                val summaryDelay = stage1 * 0.5f
                val nextSummaryProgress =
                    ((offset.toFloat() - summaryDelay) / (stage1 - summaryDelay).coerceAtLeast(1f))
                        .coerceIn(0f, 1f)
                val nextTitleProgress =
                    ((offset.toFloat() - stage1) / stage2)
                        .coerceIn(0f, 1f)
                val nextIconProgress =
                    ((offset.toFloat() - stage1 - stage2) / stage3)
                        .coerceIn(0f, 1f)
                if (summaryProgress != nextSummaryProgress) summaryProgress = nextSummaryProgress
                if (titleProgress != nextTitleProgress) titleProgress = nextTitleProgress
                if (iconProgress != nextIconProgress) iconProgress = nextIconProgress
            }.collect { }
    }

    return remember(
        bgAlphaProvider,
        hdrSweepProgressProvider,
        logoHeightDp,
        hiddenOverviewCardCount,
        avoidanceProgressProvider,
        iconProgressProvider,
        titleProgressProvider,
        summaryProgressProvider,
        density,
    ) {
        HomePageHeroMotionState(
            bgAlpha = bgAlphaProvider,
            hdrSweepProgress = hdrSweepProgressProvider,
            logoHeightDp = logoHeightDp,
            homeHeaderSinkOffset = (hiddenOverviewCardCount * HOME_HEADER_SINK_PER_HIDDEN_CARD_DP).dp,
            avoidanceProgress = avoidanceProgressProvider,
            iconProgress = iconProgressProvider,
            titleProgress = titleProgressProvider,
            summaryProgress = summaryProgressProvider,
            onHeroHeightPxChanged = { heightPx ->
                with(density) { logoHeightDp = heightPx.toDp() }
            },
            onLogoHeightPxChanged = { heightPx ->
                logoHeightPx = heightPx
                val nextBgAlpha =
                    1f -
                        homeHeroScrollProgress(
                            index = lastListIndex,
                            offsetPx = lastListOffsetPx,
                            logoHeightPx = heightPx,
                        )
                if (bgAlpha != nextBgAlpha) bgAlpha = nextBgAlpha
            },
            onLogoAreaBottomChanged = { logoAreaY = it },
            onIconBottomChanged = { bottom -> if (iconY == 0f) iconY = bottom },
            onTitleBottomChanged = { bottom -> if (titleY == 0f) titleY = bottom },
            onSummaryBottomChanged = { bottom -> if (summaryY == 0f) summaryY = bottom },
        )
    }
}

private fun homeHeroScrollProgress(
    index: Int,
    offsetPx: Int,
    logoHeightPx: Int,
): Float {
    if (logoHeightPx <= 0) return 0f
    return if (index > 0) {
        1f
    } else {
        (offsetPx.toFloat() / logoHeightPx).coerceIn(0f, 1f)
    }
}

private fun homeHeroAvoidanceProgress(
    offsetPx: Float,
    distancePx: Float,
): Float {
    val progress = (offsetPx / distancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

@Composable
internal fun rememberHomePageOverviewCardState(
    content: HomePageContentState,
    mcpOverview: HomeMcpOverview,
    githubOverview: HomeGitHubOverview,
    webDavOverview: HomeWebDavOverview,
    baOverview: HomeBaOverview,
    showCacheFreshnessInCards: Boolean,
): HomePageOverviewCardState {
    val infoColor = MiuixTheme.colorScheme.primary
    val warningColor = Color(0xFFF59E0B)
    return remember(
        content,
        mcpOverview,
        githubOverview,
        webDavOverview,
        baOverview,
        showCacheFreshnessInCards,
        infoColor,
        warningColor,
    ) {
        val homeHeaderStatusPills =
            listOf(
                HomeHeaderStatusPillState(
                    label = content.homeStatusMcp,
                    color = if (mcpOverview.running) content.runningColor else content.stoppedColor,
                    minWidth = 62.dp,
                ),
                HomeHeaderStatusPillState(
                    label = content.homeStatusGitHub,
                    color = content.cacheStateColor,
                    minWidth = 72.dp,
                ),
                HomeHeaderStatusPillState(
                    label = content.homeStatusWebDav,
                    color = if (webDavOverview.configured) content.runningColor else content.stoppedColor,
                    minWidth = 78.dp,
                ),
                HomeHeaderStatusPillState(
                    label = content.homeStatusShizuku,
                    color = if (content.shizukuGranted) content.runningColor else content.stoppedColor,
                    minWidth = 70.dp,
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 5.dp,
                        ),
                ),
            )

        val mcpEndpoint =
            mcpOverview.port
                .takeIf { it > 0 }
                ?.let { port -> "$port${mcpOverview.endpointPath}" }
                ?: content.homeNa
        val mcpOverviewPills =
            buildList {
                add(
                    HomeCardPillItem(
                        value = mcpOverview.serverName.ifBlank { content.homeCardMcp },
                        color = infoColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        value = content.mcpStatusText,
                        color = if (mcpOverview.running) content.runningColor else content.stoppedColor,
                    ),
                )
                if (mcpOverview.running) {
                    add(HomeCardPillItem(value = content.mcpRuntimeText, color = content.runningColor))
                }
                add(HomeCardPillItem(value = content.networkModeText, color = infoColor))
                add(
                    HomeCardPillItem(
                        value = mcpOverview.connectedClients.toString(),
                        color = if (mcpOverview.connectedClients > 0) content.runningColor else content.inactiveColor,
                    ),
                )
                add(HomeCardPillItem(value = mcpEndpoint, color = infoColor))
                add(
                    HomeCardPillItem(
                        label = content.homeStatToken,
                        value = content.mcpTokenStatusText,
                        color = if (mcpOverview.authTokenConfigured) infoColor else content.inactiveColor,
                    ),
                )
            }

        val githubOverviewPills =
            buildList {
                add(HomeCardPillItem(value = content.homeCardGitHub, color = infoColor))
                add(HomeCardPillItem(value = content.githubStrategyLine, color = infoColor))
                if (!githubOverview.loaded) {
                    add(HomeCardPillItem(value = content.githubFocusLine, color = content.inactiveColor))
                    return@buildList
                }
                add(
                    HomeCardPillItem(
                        label = content.homeStatTracked,
                        value = githubOverview.trackedCount.toString(),
                        color = infoColor,
                    ),
                )
                if (githubOverview.trackedCount == 0) {
                    add(HomeCardPillItem(value = content.githubLastUpdateLine, color = content.inactiveColor))
                    return@buildList
                }
                add(
                    HomeCardPillItem(
                        label = content.homeStatStableUpdates,
                        value = githubOverview.updatableCount.toString(),
                        color = if (githubOverview.updatableCount > 0) AppStatusColors.Fresh else content.inactiveColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        label = content.homeStatPreReleaseUpdates,
                        value = githubOverview.preReleaseUpdateCount.toString(),
                        color = if (githubOverview.preReleaseUpdateCount > 0) warningColor else content.inactiveColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        label = content.homeStatActions,
                        value = githubOverview.actionsTrackedCount.toString(),
                        color = if (githubOverview.actionsTrackedCount > 0) infoColor else content.inactiveColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        label = content.homeStatCached,
                        value = githubOverview.cacheHitCount.toString(),
                        color = content.cacheStateColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        label = content.homeStatLastUpdate,
                        value = content.githubLastUpdateLine,
                        color = content.cacheStateColor,
                    ),
                )
                if (githubOverview.failedCount > 0) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatFailed,
                            value = githubOverview.failedCount.toString(),
                            color = AppStatusColors.Failed,
                        ),
                    )
                }
                if (githubOverview.pendingShareImport) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatShare,
                            value = content.githubShareLine,
                            color = warningColor,
                        ),
                    )
                }
                if (githubOverview.preciseApkVersionCount > 0) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatPreciseVersion,
                            value = githubOverview.preciseApkVersionCount.toString(),
                            color = infoColor,
                        ),
                    )
                }
                if (showCacheFreshnessInCards) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatCacheState,
                            value = content.githubCacheFreshnessLine,
                            color = content.cacheStateColor,
                        ),
                    )
                }
            }

        val webDavStatusColor =
            when {
                webDavOverview.autoSyncFailed -> AppStatusColors.Failed
                webDavOverview.autoSyncNeedsReview -> warningColor
                webDavOverview.configured -> AppStatusColors.Fresh
                else -> content.stoppedColor
            }
        val webDavOverviewPills =
            buildList {
                add(HomeCardPillItem(value = content.homeCardWebDav, color = infoColor))
                add(HomeCardPillItem(value = content.webDavStatusLine, color = webDavStatusColor))
                add(
                    HomeCardPillItem(
                        label = content.homeStatSyncItems,
                        value = content.webDavSyncItemsLine,
                        color = infoColor,
                    ),
                )
                if (webDavOverview.lastAutoSyncTimeMs <= 0L && webDavOverview.lastFullSyncTimeMs <= 0L) {
                    add(HomeCardPillItem(value = content.webDavLastFullSyncLine, color = content.inactiveColor))
                } else {
                    if (webDavOverview.lastAutoSyncTimeMs > 0L) {
                        add(
                            HomeCardPillItem(
                                label = content.homeStatLastAutoSync,
                                value = content.webDavLastAutoSyncLine,
                                color = infoColor,
                            ),
                        )
                    }
                    if (webDavOverview.lastFullSyncTimeMs > 0L) {
                        add(
                            HomeCardPillItem(
                                label = content.homeStatLastFullSync,
                                value = content.webDavLastFullSyncLine,
                                color = infoColor,
                            ),
                        )
                    }
                }
            }

        val baOverviewPills =
            buildList {
                if (!baOverview.loaded) {
                    add(
                        HomeCardPillItem(
                            label = content.homeCardBa,
                            value = content.baActivationLine,
                            color = content.inactiveColor,
                        ),
                    )
                    return@buildList
                }
                add(
                    HomeCardPillItem(
                        label = content.homeCardBa,
                        value = content.baActiveAccountLine,
                        color = if (baOverview.activated) infoColor else content.stoppedColor,
                    ),
                )
                add(
                    HomeCardPillItem(
                        label = content.homeStatBaAccounts,
                        value = content.baAccountsLine,
                        color = if (baOverview.enabledAccountCount > 0) AppStatusColors.Fresh else content.inactiveColor,
                    ),
                )
                add(HomeCardPillItem(value = content.baServerLine, color = infoColor))
                add(HomeCardPillItem(label = content.homeStatAp, value = content.baApLine, color = infoColor))
                add(HomeCardPillItem(label = content.homeStatCafeAp, value = content.baCafeApLine, color = infoColor))
                if (baOverview.apNotifyEnabled) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatBaNotify,
                            value = content.baNotifyLine,
                            color = warningColor,
                        ),
                    )
                }
                if (showCacheFreshnessInCards) {
                    add(
                        HomeCardPillItem(
                            label = content.homeStatCacheState,
                            value = content.baCacheFreshnessLine,
                            color = content.cacheStateColor,
                        ),
                    )
                }
            }

        HomePageOverviewCardState(
            homeHeaderStatusPills = homeHeaderStatusPills,
            mcpOverviewPills = mcpOverviewPills,
            githubOverviewPills = githubOverviewPills,
            webDavOverviewPills = webDavOverviewPills,
            baOverviewPills = baOverviewPills,
        )
    }
}
