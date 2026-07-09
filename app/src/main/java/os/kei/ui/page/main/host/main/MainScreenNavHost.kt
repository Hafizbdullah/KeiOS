@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.AppThemeMode
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.about.page.AboutPage
import os.kei.ui.page.main.back.BackNavigationRuntimeController
import os.kei.ui.page.main.back.LocalBackNavigationRuntimeController
import os.kei.ui.page.main.back.LocalBackNavigationRuntimeState
import os.kei.ui.page.main.github.history.GitHubActionsNotificationHistoryPage
import os.kei.ui.page.main.host.pager.MainPagerLayout
import os.kei.ui.page.main.mcp.skill.page.McpSkillPage
import os.kei.ui.page.main.settings.page.SettingsPage
import os.kei.ui.page.main.student.catalog.page.BaGuideCatalogPage
import os.kei.ui.page.main.student.page.BaStudentGuidePage
import os.kei.ui.page.main.sync.WebDavSyncPage
import os.kei.ui.page.main.sync.rememberWebDavSyncDataPorts
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundHost
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyle
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyles
import os.kei.ui.page.main.widget.chrome.LocalSearchAutoFocusEnabled
import os.kei.ui.page.main.widget.glass.BindLiquidToastBridge
import os.kei.ui.page.main.widget.glass.LiquidToastHost
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.rememberLiquidToastState
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.sheet.LocalLiquidSheetEnabled
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride

@Composable
internal fun MainScreenNavHost(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    pagerCoordinator: MainScreenPagerCoordinator,
    prefsState: MainScreenUiPrefsState,
    appLabel: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    mcpServerManager: McpServerManager,
    appThemeMode: AppThemeMode,
    transientExternalLaunchActive: Boolean,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenGitHubActionsTrackFromHistory: (String) -> Unit,
    onRetryGitHubRefreshTargetsFromHistory: (List<String>) -> Unit,
) {
    val backCoordinator =
        rememberMainScreenBackCoordinator(
            backStack = backStack,
            navigator = navigator,
            pagerCoordinator = pagerCoordinator,
        )
    val onRouteBack =
        remember(backCoordinator) {
            { backCoordinator.onRouteBack() }
        }
    val predictiveBackPolicy =
        PredictiveBackOemCompat.currentPolicy(
            transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
            predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled,
        )
    val backRuntimeController = remember { BackNavigationRuntimeController() }
    SideEffect {
        backRuntimeController.updatePolicy(predictiveBackPolicy)
    }
    val routeAnimationsEnabled = prefsState.transitionAnimationsEnabled
    val routePredictiveBackAnimationsEnabled =
        routeAnimationsEnabled && predictiveBackPolicy.routePredictiveBackEnabled
    val isDarkTheme = isSystemInDarkTheme()
    val navEffects =
        remember(routeAnimationsEnabled, routePredictiveBackAnimationsEnabled, isDarkTheme) {
            if (routeAnimationsEnabled) {
                NavDisplayTransitionEffects(
                    enableCornerClip = true,
                    dimAmount = if (isDarkTheme) 0.54f else 0.34f,
                    blockInputDuringTransition = true,
                    popDirectionFollowsSwipeEdge = routePredictiveBackAnimationsEnabled,
                )
            } else {
                NavDisplayTransitionEffects.None
            }
        }
    val navTransitionSpec =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) {
                appNavTransitionSpec()
            } else {
                noNavContentTransform()
            }
        }
    val navPopTransitionSpec =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) {
                appNavPopTransitionSpec()
            } else {
                noNavContentTransform()
            }
        }
    val navPredictivePopTransitionSpec =
        remember(routePredictiveBackAnimationsEnabled) {
            if (routePredictiveBackAnimationsEnabled) {
                defaultPredictivePopTransitionSpec<NavKey>()
            } else {
                noNavPredictiveContentTransform()
            }
        }

    CompositionLocalProvider(
        LocalBackNavigationRuntimeController provides backRuntimeController,
        LocalBackNavigationRuntimeState provides backRuntimeController.state,
        LocalTransitionAnimationsEnabled provides prefsState.transitionAnimationsEnabled,
        LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled,
        LocalSearchAutoFocusEnabled provides prefsState.searchAutoFocusEnabled,
        LocalLiquidControlsEnabled provides prefsState.liquidSwitchEnabled,
        LocalLiquidSheetEnabled provides prefsState.liquidSheetEnabled,
        LocalTextCopyExpandedOverride provides prefsState.textCopyCapabilityExpanded,
    ) {
        val liquidToastState = rememberLiquidToastState()
        val liquidToastBackdrop = rememberLayerBackdrop()
        BindLiquidToastBridge(
            state = liquidToastState,
            liquidToastEnabled = prefsState.liquidToastEnabled,
            reduceToastInterruptionEnabled = prefsState.reduceToastInterruptionEnabled,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(liquidToastBackdrop)) {
                val navEntryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                        rememberViewModelStoreNavEntryDecorator<NavKey>(),
                    )
                NavDisplay(
                    backStack = backStack,
                    onBack = onRouteBack,
                    entryDecorators = navEntryDecorators,
                    transitionSpec = navTransitionSpec,
                    popTransitionSpec = navPopTransitionSpec,
                    predictivePopTransitionSpec = navPredictivePopTransitionSpec,
                    transitionEffects = navEffects,
                    modifier = Modifier.fillMaxSize(),
                    entryProvider = entryProvider<NavKey> {
                        entry<KeiosRoute.Main> {
                            MainPagerLayout(
                                rootBackHandlersEnabled = backStack.lastOrNull() is KeiosRoute.Main,
                                navigator = navigator,
                                settingsReturnToken = pagerCoordinator.settingsReturnToken,
                                liquidActionBarLayeredStyleEnabled = pagerCoordinator.liquidActionBarLayeredStyleEnabled,
                                gripAwareFloatingDockEnabled = pagerCoordinator.gripAwareFloatingDockEnabled,
                                homeIconHdrEnabled = pagerCoordinator.homeIconHdrEnabled,
                                homeDynamicFullEffectEnabled = pagerCoordinator.homeDynamicFullEffectEnabled,
                                preloadingEnabled = pagerCoordinator.preloadingEnabled,
                                nonHomeBackgroundEnabled = pagerCoordinator.nonHomeBackgroundEnabled,
                                nonHomeBackgroundUri = pagerCoordinator.nonHomeBackgroundUri,
                                nonHomeBackgroundOpacity = pagerCoordinator.nonHomeBackgroundOpacity,
                                nonHomeBackgroundContentScale = pagerCoordinator.nonHomeBackgroundContentScale,
                                nonHomeBackgroundAlignment = pagerCoordinator.nonHomeBackgroundAlignment,
                                nonHomeBackgroundPageStyle = pagerCoordinator.nonHomeBackgroundPageStyle,
                                nonHomeBackgroundScrim = pagerCoordinator.nonHomeBackgroundScrim,
                                nonHomeBackgroundDepthEnabled = pagerCoordinator.nonHomeBackgroundDepthEnabled,
                                nonHomeBackgroundSaturation = pagerCoordinator.nonHomeBackgroundSaturation,
                                visibleBottomPageNames = pagerCoordinator.visibleBottomPageNames,
                                onVisibleBottomPageNamesChange = pagerCoordinator.onVisibleBottomPageNamesChange,
                                shizukuStatus = pagerCoordinator.shizukuStatus,
                                shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                                mcpServerManager = pagerCoordinator.mcpServerManager,
                                onOpenGuideDetail = pagerCoordinator.onOpenGuideDetail,
                                onOpenBaGuideCatalog = pagerCoordinator.onBaGuideCatalogOpen,
                                requestedBottomPage = pagerCoordinator.requestedBottomPage,
                                requestedBottomPageToken = pagerCoordinator.requestedBottomPageToken,
                                requestedGitHubRefreshToken = pagerCoordinator.requestedGitHubRefreshToken,
                                requestedGitHubActionsTrackId = pagerCoordinator.requestedGitHubActionsTrackId,
                                requestedGitHubActionsSheetToken = pagerCoordinator.requestedGitHubActionsSheetToken,
                                requestedBaAccountId = pagerCoordinator.requestedBaAccountId,
                                requestedBaAccountToken = pagerCoordinator.requestedBaAccountToken,
                                transientExternalLaunchActive = transientExternalLaunchActive,
                                onRequestedBottomPageConsumed = pagerCoordinator.onRequestedBottomPageConsumed,
                            )
                        }
                        entry<KeiosRoute.Settings> {
                            MainScreenRouteBackgroundHost(prefsState = prefsState) {
                                SettingsPage(
                                    notificationPermissionGranted = notificationPermissionGranted,
                                    onRequestNotificationPermission = onRequestNotificationPermission,
                                    liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                                    onLiquidActionBarLayeredStyleChanged = prefsState::updateLiquidActionBarLayeredStyleEnabled,
                                    liquidSwitchEnabled = prefsState.liquidSwitchEnabled,
                                    onLiquidSwitchChanged = prefsState::updateLiquidSwitchEnabled,
                                    liquidToastEnabled = prefsState.liquidToastEnabled,
                                    onLiquidToastChanged = prefsState::updateLiquidToastEnabled,
                                    reduceToastInterruptionEnabled = prefsState.reduceToastInterruptionEnabled,
                                    onReduceToastInterruptionChanged = prefsState::updateReduceToastInterruptionEnabled,
                                    liquidSheetEnabled = prefsState.liquidSheetEnabled,
                                    onLiquidSheetChanged = prefsState::updateLiquidSheetEnabled,
                                    liquidDialogEnabled = prefsState.liquidDialogEnabled,
                                    onLiquidDialogChanged = prefsState::updateLiquidDialogEnabled,
                                    transitionAnimationsEnabled = prefsState.transitionAnimationsEnabled,
                                    onTransitionAnimationsChanged = prefsState::updateTransitionAnimationsEnabled,
                                    predictiveBackAnimationsEnabled = prefsState.predictiveBackAnimationsEnabled,
                                    onPredictiveBackAnimationsChanged = prefsState::updatePredictiveBackAnimationsEnabled,
                                    searchAutoFocusEnabled = prefsState.searchAutoFocusEnabled,
                                    onSearchAutoFocusChanged = prefsState::updateSearchAutoFocusEnabled,
                                    gripAwareFloatingDockEnabled = prefsState.gripAwareFloatingDockEnabled,
                                    onGripAwareFloatingDockChanged = prefsState::updateGripAwareFloatingDockEnabled,
                                    homeIconHdrEnabled = prefsState.homeIconHdrEnabled,
                                    onHomeIconHdrChanged = prefsState::updateHomeIconHdrEnabled,
                                    homeDynamicFullEffectEnabled = prefsState.homeDynamicFullEffectEnabled,
                                    onHomeDynamicFullEffectChanged = prefsState::updateHomeDynamicFullEffectEnabled,
                                    preloadingEnabled = prefsState.preloadingEnabled,
                                    onPreloadingEnabledChanged = prefsState::updatePreloadingEnabled,
                                    launcherIconDesign = prefsState.launcherIconDesign,
                                    onLauncherIconDesignChanged = prefsState::updateLauncherIconDesign,
                                    nonHomeBackgroundEnabled = prefsState.nonHomeBackgroundEnabled,
                                    onNonHomeBackgroundEnabledChanged = prefsState::updateNonHomeBackgroundEnabled,
                                    nonHomeBackgroundUri = prefsState.nonHomeBackgroundUri,
                                    onNonHomeBackgroundUriChanged = prefsState::updateNonHomeBackgroundUri,
                                    nonHomeBackgroundOpacity = prefsState.nonHomeBackgroundOpacity,
                                    onNonHomeBackgroundOpacityChanged = prefsState::updateNonHomeBackgroundOpacity,
                                    nonHomeBackgroundContentScale = prefsState.nonHomeBackgroundContentScale,
                                    onNonHomeBackgroundContentScaleChanged = prefsState::updateNonHomeBackgroundContentScale,
                                    nonHomeBackgroundAlignment = prefsState.nonHomeBackgroundAlignment,
                                    onNonHomeBackgroundAlignmentChanged = prefsState::updateNonHomeBackgroundAlignment,
                                    nonHomeBackgroundPageStyle = prefsState.nonHomeBackgroundPageStyle,
                                    onNonHomeBackgroundPageStyleChanged = prefsState::updateNonHomeBackgroundPageStyle,
                                    nonHomeBackgroundScrim = prefsState.nonHomeBackgroundScrim,
                                    onNonHomeBackgroundScrimChanged = prefsState::updateNonHomeBackgroundScrim,
                                    nonHomeBackgroundDepthEnabled = prefsState.nonHomeBackgroundDepthEnabled,
                                    onNonHomeBackgroundDepthEnabledChanged = prefsState::updateNonHomeBackgroundDepthEnabled,
                                    nonHomeBackgroundSaturation = prefsState.nonHomeBackgroundSaturation,
                                    onNonHomeBackgroundSaturationChanged = prefsState::updateNonHomeBackgroundSaturation,
                                    onResetNonHomeBackgroundRendering = prefsState::resetNonHomeBackgroundRendering,
                                    onApplyNonHomeBackgroundReadableSuggestion = prefsState::applyNonHomeBackgroundReadableSuggestion,
                                    superIslandNotificationEnabled = prefsState.superIslandNotificationEnabled,
                                    onSuperIslandNotificationChanged = prefsState::updateSuperIslandNotificationEnabled,
                                    superIslandFloatBehavior = prefsState.superIslandFloatBehavior,
                                    onSuperIslandFloatBehaviorChanged = prefsState::updateSuperIslandFloatBehavior,
                                    superIslandBypassRestrictionEnabled = prefsState.superIslandBypassRestrictionEnabled,
                                    onSuperIslandBypassRestrictionChanged = prefsState::updateSuperIslandBypassRestrictionEnabled,
                                    superIslandRestoreDelayMs = prefsState.superIslandRestoreDelayMs,
                                    onSuperIslandRestoreDelayMsChanged = prefsState::updateSuperIslandRestoreDelayMs,
                                    logLevel = prefsState.logLevel,
                                    onLogLevelChanged = prefsState::updateLogLevel,
                                    textCopyCapabilityExpanded = prefsState.textCopyCapabilityExpanded,
                                    onTextCopyCapabilityExpandedChanged = prefsState::updateTextCopyCapabilityExpanded,
                                    cacheDiagnosticsEnabled = prefsState.cacheDiagnosticsEnabled,
                                    onCacheDiagnosticsChanged = prefsState::updateCacheDiagnosticsEnabled,
                                    shizukuStatus = pagerCoordinator.shizukuStatus,
                                    onCheckOrRequestShizuku = onCheckOrRequestShizuku,
                                    shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                                    appThemeMode = appThemeMode,
                                    onAppThemeModeChanged = onAppThemeModeChanged,
                                    onBack = onRouteBack,
                                    onOpenWebDavSync = { navigator.pushSingleTop(KeiosRoute.WebDavSync) },
                                )
                            }
                        }
                        entry<KeiosRoute.McpSkill> {
                            MainScreenRouteBackgroundHost(prefsState = prefsState) {
                                McpSkillPage(
                                    mcpServerManager = mcpServerManager,
                                    onBack = onRouteBack,
                                )
                            }
                        }
                        entry<KeiosRoute.GitHubActionsNotificationHistory> {
                            MainScreenRouteBackgroundHost(prefsState = prefsState) {
                                GitHubActionsNotificationHistoryPage(
                                    onBack = onRouteBack,
                                    onOpenTrackActions = onOpenGitHubActionsTrackFromHistory,
                                    onRetryRefreshTargets = onRetryGitHubRefreshTargetsFromHistory,
                                )
                            }
                        }
                        entry<KeiosRoute.About> {
                            MainScreenRouteBackgroundHost(prefsState = prefsState) {
                                AboutPage(
                                    appLabel = appLabel,
                                    notificationPermissionGranted = notificationPermissionGranted,
                                    shizukuStatus = pagerCoordinator.shizukuStatus,
                                    shizukuApiUtils = pagerCoordinator.shizukuApiUtils,
                                    onCheckShizuku = onCheckOrRequestShizuku,
                                    onBack = onRouteBack,
                                )
                            }
                        }
                        entry<KeiosRoute.BaStudentGuide> {
                            BaStudentGuidePage(
                                liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                                preloadingEnabled = prefsState.preloadingEnabled,
                                onBack = onRouteBack,
                            )
                        }
                        entry<KeiosRoute.BaGuideCatalog> { route ->
                            BaGuideCatalogPage(
                                liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                                preloadingEnabled = prefsState.preloadingEnabled,
                                notificationPermissionGranted = notificationPermissionGranted,
                                onRequestNotificationPermission = onRequestNotificationPermission,
                                openBgmPlaybackToken = route.openBgmPlaybackToken,
                                onBack = onRouteBack,
                                onOpenGuide = pagerCoordinator.onOpenGuideDetail,
                            )
                        }
                        entry<KeiosRoute.WebDavSync> {
                            val dataPorts = rememberWebDavSyncDataPorts()
                            MainScreenRouteBackgroundHost(prefsState = prefsState) {
                                WebDavSyncPage(
                                    onBack = onRouteBack,
                                    dataPorts = dataPorts,
                                )
                            }
                        }
                    },
                )
            }
            LiquidToastHost(
                state = liquidToastState,
                backdrop = liquidToastBackdrop,
            )
        }
    }
}

private fun <T : Any> noNavContentTransform():
        AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
    )
}

private fun appNavTransitionSpec():
        AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (targetState.key is KeiosRoute.BaGuideCatalog) {
        baGuideCatalogEnterContentTransform()
    } else {
        defaultTransitionSpec<NavKey>().invoke(this)
    }
}

private fun appNavPopTransitionSpec():
        AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (initialState.key is KeiosRoute.BaGuideCatalog) {
        baGuideCatalogPopContentTransform()
    } else {
        defaultPopTransitionSpec<NavKey>().invoke(this)
    }
}

private fun baGuideCatalogEnterContentTransform(): ContentTransform =
    ContentTransform(
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = CatalogRouteEnterFadeMs)) +
                slideInHorizontally(
                    initialOffsetX = { width -> width / CatalogRouteEnterOffsetDivisor },
                    animationSpec = tween(durationMillis = CatalogRouteEnterSlideMs),
                ),
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = CatalogRouteExitFadeMs)) +
                slideOutHorizontally(
                    targetOffsetX = { width -> -width / CatalogRouteExitOffsetDivisor },
                    animationSpec = tween(durationMillis = CatalogRouteExitSlideMs),
                ),
    )

private fun baGuideCatalogPopContentTransform(): ContentTransform =
    ContentTransform(
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = CatalogRoutePopEnterFadeMs)) +
                slideInHorizontally(
                    initialOffsetX = { width -> -width / CatalogRoutePopEnterOffsetDivisor },
                    animationSpec = tween(durationMillis = CatalogRoutePopEnterSlideMs),
                ),
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = CatalogRoutePopExitFadeMs)) +
                slideOutHorizontally(
                    targetOffsetX = { width -> width / CatalogRoutePopExitOffsetDivisor },
                    animationSpec = tween(durationMillis = CatalogRoutePopExitSlideMs),
                ),
    )

private fun <T : Any> noNavPredictiveContentTransform():
        AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
    ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
    )
}

@Composable
private fun MainScreenRouteBackgroundHost(
    prefsState: MainScreenUiPrefsState,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
    content: @Composable () -> Unit,
) {
    AppManagedBackgroundHost(
        enabled = prefsState.nonHomeBackgroundEnabled,
        imageUri = prefsState.nonHomeBackgroundUri,
        opacity = prefsState.nonHomeBackgroundOpacity,
        saturation = prefsState.nonHomeBackgroundSaturation,
        contentScale = prefsState.nonHomeBackgroundContentScale,
        alignment = prefsState.nonHomeBackgroundAlignment,
        pageStyle = prefsState.nonHomeBackgroundPageStyle,
        scrim = prefsState.nonHomeBackgroundScrim,
        style = style,
        content = content,
    )
}

private const val CatalogRouteEnterFadeMs = 140
private const val CatalogRouteEnterSlideMs = 220
private const val CatalogRouteExitFadeMs = 110
private const val CatalogRouteExitSlideMs = 180
private const val CatalogRoutePopEnterFadeMs = 120
private const val CatalogRoutePopEnterSlideMs = 190
private const val CatalogRoutePopExitFadeMs = 120
private const val CatalogRoutePopExitSlideMs = 210
private const val CatalogRouteEnterOffsetDivisor = 7
private const val CatalogRouteExitOffsetDivisor = 18
private const val CatalogRoutePopEnterOffsetDivisor = 18
private const val CatalogRoutePopExitOffsetDivisor = 7
