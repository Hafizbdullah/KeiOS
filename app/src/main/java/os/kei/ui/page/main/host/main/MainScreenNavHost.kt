@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
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
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.sheet.LocalLiquidSheetEnabled
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import kotlin.math.roundToInt

@Composable
internal fun MainScreenNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
    pagerCoordinator: MainScreenPagerCoordinator,
    prefsState: MainScreenUiPrefsState,
    appLabel: String,
    onCheckOrRequestPrivilege: () -> Unit,
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
    val isDarkTheme = isAppInDarkTheme()
    // miuix-nav keeps one visual contract for push/pop/predictive back (visual = f(depth)), so the
    // former transitionSpec/popTransitionSpec/predictivePopTransitionSpec trio collapses into a
    // single NavTransition: MiuixDefault when route animations are on, None for an instant swap.
    val navTransition =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) NavTransitions.MiuixDefault else NavTransitions.None
        }
    val catalogTransition =
        remember(routeAnimationsEnabled) {
            if (routeAnimationsEnabled) baGuideCatalogNavTransition() else null
        }
    val navEffects =
        remember(routeAnimationsEnabled, isDarkTheme) {
            if (routeAnimationsEnabled) {
                NavDisplayEffects(
                    enableCornerClip = true,
                    dimAmount = if (isDarkTheme) 0.54f else 0.34f,
                    blockInputDuringTransition = true,
                )
            } else {
                NavDisplayEffects.None
            }
        }
    // No route opts into navSwipeDismiss. Miuix attaches that gesture to the display container and
    // watches PointerEventPass.Initial, which dispatches parent-first, so it claims any horizontal
    // drag past touch slop before a descendant sees it — sliders, AppSwitch, the bottom-bar tab
    // drag and text fields all lose their gesture (issue #21). Descendants cannot pre-empt an
    // Initial-pass ancestor, and Miuix exposes no edge band or nested veto, so back stays with the
    // system predictive gesture that NavDisplay already drives.
    // See docs/planning/miuix-nav-swipe-dismiss-gap.md before enabling it again.

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
            val toastBackdropProducer =
                if (liquidToastState.isVisible) {
                    Modifier.layerBackdrop(liquidToastBackdrop)
                } else {
                    Modifier
                }
            Box(modifier = Modifier.fillMaxSize().then(toastBackdropProducer)) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = onRouteBack,
                    transition = navTransition,
                    effects = navEffects,
                ) {
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
                            privilegeStatus = pagerCoordinator.privilegeStatus,
                            privilegedShell = pagerCoordinator.privilegedShell,
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
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
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
                                privilegeStatus = pagerCoordinator.privilegeStatus,
                                onCheckOrRequestPrivilege = onCheckOrRequestPrivilege,
                                privilegeMode = prefsState.privilegeMode,
                                onPrivilegeModeChanged = prefsState::updatePrivilegeMode,
                                privilegedShell = pagerCoordinator.privilegedShell,
                                appThemeMode = appThemeMode,
                                onAppThemeModeChanged = onAppThemeModeChanged,
                                onBack = onRouteBack,
                                onOpenWebDavSync = { navigator.pushSingleTop(KeiosRoute.WebDavSync) },
                            )
                        }
                    }
                    entry<KeiosRoute.McpSkill> {
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
                            McpSkillPage(
                                mcpServerManager = mcpServerManager,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.GitHubActionsNotificationHistory> {
                        MainScreenRouteBackgroundHost(
                            prefsState = prefsState,
                            exportBackdropToContent = true,
                        ) {
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
                                privilegeStatus = pagerCoordinator.privilegeStatus,
                                privilegedShell = pagerCoordinator.privilegedShell,
                                onCheckPrivilege = onCheckOrRequestPrivilege,
                                onBack = onRouteBack,
                            )
                        }
                    }
                    entry<KeiosRoute.BaStudentGuide> { route ->
                        BaStudentGuidePage(
                            warmStartId = route.nonce,
                            liquidActionBarLayeredStyleEnabled = prefsState.liquidActionBarLayeredStyleEnabled,
                            preloadingEnabled = prefsState.preloadingEnabled,
                            onBack = onRouteBack,
                        )
                    }
                    entry<KeiosRoute.BaGuideCatalog>(transition = catalogTransition) { route ->
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
                }
            }
            LiquidToastHost(
                state = liquidToastState,
                backdrop = liquidToastBackdrop,
            )
        }
    }
}

/**
 * Preserves the pre-migration BaGuideCatalog feel — a light fade + shallow slide instead of the
 * full-width Miuix slide. The same depth function drives push, pop, and predictive back: the
 * entering/leaving top fades over a width/7 offset, the covered layer parallaxes width/18.
 */
private fun baGuideCatalogNavTransition(): NavTransition =
    navGraphicsTransition(opaqueDepth = 1f) { scope ->
        val width = scope.layoutSize.width.toFloat()
        val d = scope.relativeDepth
        val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
        if (d <= 0f) {
            val progress = (-d).coerceIn(0f, 1f)
            translationX =
                (direction * progress * width / CatalogRouteTopOffsetDivisor).roundToInt().toFloat()
            alpha = 1f - progress
        } else {
            val progress = d.coerceIn(0f, 1f)
            translationX = -direction * progress * width / CatalogRouteCoverOffsetDivisor
            alpha = 1f - CatalogRouteCoverAlphaFalloff * progress
        }
    }

@Composable
private fun MainScreenRouteBackgroundHost(
    prefsState: MainScreenUiPrefsState,
    style: AppManagedBackgroundStyle = AppManagedBackgroundStyles.Standard,
    exportBackdropToContent: Boolean = false,
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
        exportBackdropToContent = exportBackdropToContent,
        content = content,
    )
}

private const val CatalogRouteTopOffsetDivisor = 7
private const val CatalogRouteCoverOffsetDivisor = 18
private const val CatalogRouteCoverAlphaFalloff = 0.1f
