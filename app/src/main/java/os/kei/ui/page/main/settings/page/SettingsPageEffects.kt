@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import os.kei.core.log.AppLogLevel
import os.kei.ui.page.main.settings.state.SettingsPageViewModel
import os.kei.ui.page.main.settings.support.SettingsBatteryOptimizationController
import os.kei.ui.page.main.settings.support.SettingsPermissionKeepAliveController
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun BindSettingsPageEffects(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    settingsPageViewModel: SettingsPageViewModel,
    batteryOptimizationController: SettingsBatteryOptimizationController,
    permissionKeepAliveController: SettingsPermissionKeepAliveController,
    notificationPermissionGranted: Boolean,
    shizukuStatus: String,
    cacheDiagnosticsEnabled: Boolean,
    logLevel: AppLogLevel,
    shizukuRefreshToken: Int,
    keepAliveActive: Boolean,
) {
    val latestContext = rememberUpdatedState(context)
    val latestNotificationPermissionGranted = rememberUpdatedState(notificationPermissionGranted)
    val latestShizukuStatus = rememberUpdatedState(shizukuStatus)
    val latestCacheDiagnosticsEnabled = rememberUpdatedState(cacheDiagnosticsEnabled)
    val latestLogLevel = rememberUpdatedState(logLevel)

    LaunchedEffect(context, settingsPageViewModel) {
        settingsPageViewModel.bindSearchTargets(context)
    }

    DisposableEffect(
        lifecycleOwner,
        settingsPageViewModel,
        batteryOptimizationController,
        permissionKeepAliveController,
        keepAliveActive,
    ) {
        fun refreshSupportState() {
            settingsPageViewModel.refreshBatteryOptimization(batteryOptimizationController)
            settingsPageViewModel.refreshPermissionKeepAlive(
                controller = permissionKeepAliveController,
                notificationPermissionGranted = latestNotificationPermissionGranted.value,
                shizukuStatus = latestShizukuStatus.value,
            )
            if (keepAliveActive) {
                settingsPageViewModel.refreshAccessibilityGuard(latestContext.value)
            }
        }

        fun bindDiagnostics(active: Boolean) {
            settingsPageViewModel.bindDiagnostics(
                context = latestContext.value,
                active = active,
                cacheDiagnosticsEnabled = latestCacheDiagnosticsEnabled.value,
                logLevel = latestLogLevel.value,
            )
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            refreshSupportState()
            bindDiagnostics(active = true)
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                    -> {
                        refreshSupportState()
                        bindDiagnostics(active = true)
                    }

                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                    -> {
                        bindDiagnostics(active = false)
                    }

                    else -> {
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(context, cacheDiagnosticsEnabled, logLevel) {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return@LaunchedEffect
        }
        settingsPageViewModel.bindDiagnostics(
            context = context,
            active = true,
            cacheDiagnosticsEnabled = cacheDiagnosticsEnabled,
            logLevel = logLevel,
        )
    }
    LaunchedEffect(context, notificationPermissionGranted, shizukuStatus, keepAliveActive) {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return@LaunchedEffect
        }
        settingsPageViewModel.refreshPermissionKeepAliveNow(
            controller = permissionKeepAliveController,
            notificationPermissionGranted = notificationPermissionGranted,
            shizukuStatus = shizukuStatus,
        )
        if (keepAliveActive) {
            settingsPageViewModel.refreshAccessibilityGuardNow(context)
        }
    }
    LaunchedEffect(context, keepAliveActive) {
        if (!keepAliveActive) return@LaunchedEffect
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return@LaunchedEffect
        }
        settingsPageViewModel.refreshAccessibilityGuardNow(context)
    }
    LaunchedEffect(shizukuRefreshToken) {
        if (shizukuRefreshToken <= 0) return@LaunchedEffect
        repeat(8) {
            settingsPageViewModel.refreshPermissionKeepAliveNow(
                controller = permissionKeepAliveController,
                notificationPermissionGranted = latestNotificationPermissionGranted.value,
                shizukuStatus = latestShizukuStatus.value,
            )
            if (keepAliveActive) {
                settingsPageViewModel.refreshAccessibilityGuardNow(latestContext.value)
            }
            delay(400.milliseconds)
        }
    }
}
