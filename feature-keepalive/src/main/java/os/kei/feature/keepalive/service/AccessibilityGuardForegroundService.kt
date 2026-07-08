package os.kei.feature.keepalive.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRestoreReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRuntime
import os.kei.feature.keepalive.accessibility.AccessibilityGuardStateStore
import os.kei.feature.keepalive.notification.AccessibilityGuardNotificationHelper

class AccessibilityGuardForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + AppDispatchers.osOperations)
    private val stateStore: AccessibilityGuardStateStore by lazy { AccessibilityGuardRuntime.newStateStore() }
    private val restoreRunner by lazy {
        AccessibilityGuardRuntime.restoreRunner(
            context = this,
            stateStore = stateStore,
        )
    }
    private var settingsObserver: ContentObserver? = null
    private var screenOnReceiver: BroadcastReceiver? = null
    private var restoreJob: Job? = null
    private var foregroundPromoted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP_ACCESSIBILITY_GUARD -> {
                stopGuard(removeNotification = true)
                return START_NOT_STICKY
            }

            ACTION_CHECK_ACCESSIBILITY_GUARD -> {
                if (promoteForegroundIfNeeded(force = true)) {
                    refreshRegistrations()
                    scheduleRestore(
                        reason = AccessibilityGuardRestoreReason.Manual,
                        triggerAction = ACTION_CHECK_ACCESSIBILITY_GUARD,
                    )
                    return START_STICKY
                }
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_ACCESSIBILITY_GUARD,
            null,
            -> {
                val settings = stateStore.loadSettings()
                if (!settings.daemonEnabled) {
                    stopGuard(removeNotification = true)
                    return START_NOT_STICKY
                }
                if (promoteForegroundIfNeeded(force = false)) {
                    refreshRegistrations()
                    scheduleRestore(
                        reason = AccessibilityGuardRestoreReason.ForegroundServiceStart,
                        triggerAction = ACTION_START_ACCESSIBILITY_GUARD,
                    )
                    return START_STICKY
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopGuard(removeNotification = true)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promoteForegroundIfNeeded(force: Boolean): Boolean {
        if (foregroundPromoted && !force) return true
        return runCatching {
            AccessibilityGuardNotificationHelper.ensureChannel(this)
            startForeground(
                AccessibilityGuardNotificationHelper.FOREGROUND_NOTIFICATION_ID,
                AccessibilityGuardNotificationHelper.buildForegroundNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            foregroundPromoted = true
            true
        }.getOrElse { error ->
            AppLogger.w(TAG, "start accessibility guard foreground failed", error)
            false
        }
    }

    private fun refreshRegistrations() {
        registerSettingsObserver()
        refreshScreenOnReceiver()
    }

    private fun registerSettingsObserver() {
        if (settingsObserver != null) return
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    scheduleRestore(
                        reason = AccessibilityGuardRestoreReason.SecureSettingChanged,
                        triggerAction = "secure_settings_observer",
                        debounceMs = SETTINGS_OBSERVER_DEBOUNCE_MS,
                    )
                }
            }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer,
        )
        settingsObserver = observer
    }

    private fun refreshScreenOnReceiver() {
        val enabled = stateStore.loadSettings().screenOnCheckEnabled
        if (enabled && screenOnReceiver == null) {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent?,
                    ) {
                        if (intent?.action != Intent.ACTION_SCREEN_ON) return
                        scheduleRestore(
                            reason = AccessibilityGuardRestoreReason.ScreenOn,
                            triggerAction = Intent.ACTION_SCREEN_ON,
                        )
                    }
                }
            ContextCompat.registerReceiver(
                this,
                receiver,
                IntentFilter(Intent.ACTION_SCREEN_ON),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            screenOnReceiver = receiver
        } else if (!enabled) {
            unregisterScreenOnReceiver()
        }
    }

    private fun unregisterSettingsObserver() {
        settingsObserver?.let { observer ->
            runCatching { contentResolver.unregisterContentObserver(observer) }
        }
        settingsObserver = null
    }

    private fun unregisterScreenOnReceiver() {
        screenOnReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        screenOnReceiver = null
    }

    private fun scheduleRestore(
        reason: AccessibilityGuardRestoreReason,
        triggerAction: String,
        debounceMs: Long = 0L,
    ) {
        restoreJob?.cancel()
        restoreJob =
            serviceScope.launch {
                if (debounceMs > 0L) delay(debounceMs)
                restoreRunner.restoreAndRecord(
                    reason = reason,
                    triggerAction = triggerAction,
                )
            }
    }

    private fun stopGuard(removeNotification: Boolean) {
        restoreJob?.cancel()
        restoreJob = null
        unregisterSettingsObserver()
        unregisterScreenOnReceiver()
        if (foregroundPromoted || removeNotification) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            foregroundPromoted = false
        }
        stopSelf()
    }

    companion object {
        const val ACTION_START_ACCESSIBILITY_GUARD = "os.kei.keepalive.action.START_ACCESSIBILITY_GUARD"
        const val ACTION_STOP_ACCESSIBILITY_GUARD = "os.kei.keepalive.action.STOP_ACCESSIBILITY_GUARD"
        const val ACTION_CHECK_ACCESSIBILITY_GUARD = "os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD"

        private const val TAG = "AccessibilityGuardService"
        private const val SETTINGS_OBSERVER_DEBOUNCE_MS = 2_000L

        fun start(context: Context): Boolean =
            startForegroundService(
                context = context,
                action = ACTION_START_ACCESSIBILITY_GUARD,
            )

        fun check(context: Context): Boolean =
            startForegroundService(
                context = context,
                action = ACTION_CHECK_ACCESSIBILITY_GUARD,
            )

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, AccessibilityGuardForegroundService::class.java).apply {
                action = ACTION_STOP_ACCESSIBILITY_GUARD
            }
            runCatching { appContext.startService(intent) }
                .onFailure { error -> AppLogger.w(TAG, "stop accessibility guard service failed", error) }
        }

        private fun startForegroundService(
            context: Context,
            action: String,
        ): Boolean {
            val appContext = context.applicationContext
            val intent = Intent(appContext, AccessibilityGuardForegroundService::class.java).apply {
                this.action = action
            }
            return runCatching {
                ContextCompat.startForegroundService(appContext, intent)
                true
            }.getOrElse { error ->
                AppLogger.w(TAG, "start accessibility guard service failed", error)
                false
            }
        }
    }
}
