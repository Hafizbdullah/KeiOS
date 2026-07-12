package os.kei.ui.page.main.sync

import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import kotlinx.serialization.Serializable
import os.kei.core.background.WEBDAV_AUTO_SYNC_JOB_ID

@Serializable
internal data class WebDavSyncRuntimeDiagnostics(
    val interactive: Boolean,
    val deviceIdle: Boolean,
    val lightDeviceIdle: Boolean,
    val powerSave: Boolean,
    val lowPowerStandbyEnabled: Boolean,
    val lowPowerStandbyExempt: Boolean,
    val batteryOptimizationExempt: Boolean,
    val backgroundDataRestricted: Boolean,
    val networkPresent: Boolean,
    val networkValidated: Boolean,
    val networkNotSuspended: Boolean,
    val appStandbyBucket: String,
    val queuedDurationMs: Long = 0L,
    val pendingReasons: List<String> = emptyList(),
    val previousStopReason: String? = null,
) {
    val powerRestricted: Boolean
        get() = deviceIdle || lightDeviceIdle || powerSave ||
            (lowPowerStandbyEnabled && !lowPowerStandbyExempt)

    val networkRestricted: Boolean
        get() = backgroundDataRestricted || !networkPresent || !networkValidated || !networkNotSuspended

    companion object {
        fun capture(
            context: Context,
            params: JobParameters? = null,
            nowMillis: Long = System.currentTimeMillis(),
        ): WebDavSyncRuntimeDiagnostics {
            val appContext = context.applicationContext
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
            val usageStatsManager = appContext.getSystemService(UsageStatsManager::class.java)
            val scheduler = appContext.getSystemService(JobScheduler::class.java)
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
            val enqueuedAtMs = params?.extras?.getLong("webdav_auto_sync_enqueued_at_ms", 0L) ?: 0L
            return WebDavSyncRuntimeDiagnostics(
                interactive = powerManager?.isInteractive == true,
                deviceIdle = powerManager?.isDeviceIdleMode == true,
                lightDeviceIdle = powerManager?.isDeviceLightIdleMode == true,
                powerSave = powerManager?.isPowerSaveMode == true,
                lowPowerStandbyEnabled = powerManager?.isLowPowerStandbyEnabled == true,
                lowPowerStandbyExempt = powerManager?.isExemptFromLowPowerStandby == true,
                batteryOptimizationExempt = powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true,
                backgroundDataRestricted =
                    connectivityManager?.restrictBackgroundStatus ==
                        ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
                networkPresent = activeNetwork != null,
                networkValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                networkNotSuspended =
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED) == true,
                appStandbyBucket = appStandbyBucketLabel(usageStatsManager?.appStandbyBucket),
                queuedDurationMs =
                    if (enqueuedAtMs > 0L) (nowMillis - enqueuedAtMs).coerceAtLeast(0L) else 0L,
                pendingReasons = pendingJobReasonLabels(scheduler),
                previousStopReason = WebDavSyncStore.getLastJobStopReason(),
            )
        }

        private fun pendingJobReasonLabels(scheduler: JobScheduler?): List<String> {
            if (scheduler == null) return emptyList()
            return runCatching {
                if (Build.VERSION.SDK_INT >= 36) {
                    scheduler.getPendingJobReasonsHistory(WEBDAV_AUTO_SYNC_JOB_ID)
                        .flatMap { info -> info.pendingJobReasons.toList() }
                        .distinct()
                        .map(::pendingJobReasonLabel)
                } else {
                    listOf(scheduler.getPendingJobReason(WEBDAV_AUTO_SYNC_JOB_ID))
                        .filter { it != JobScheduler.PENDING_JOB_REASON_UNDEFINED }
                        .map(::pendingJobReasonLabel)
                }
            }.getOrDefault(emptyList())
        }

        private fun pendingJobReasonLabel(reason: Int): String =
            when (reason) {
                JobScheduler.PENDING_JOB_REASON_EXECUTING -> "executing"
                JobScheduler.PENDING_JOB_REASON_APP_STANDBY -> "app_standby"
                JobScheduler.PENDING_JOB_REASON_BACKGROUND_RESTRICTION -> "background_restriction"
                JobScheduler.PENDING_JOB_REASON_CONSTRAINT_CONNECTIVITY -> "connectivity"
                JobScheduler.PENDING_JOB_REASON_DEVICE_STATE -> "device_state"
                JobScheduler.PENDING_JOB_REASON_JOB_SCHEDULER_OPTIMIZATION -> "scheduler_optimization"
                JobScheduler.PENDING_JOB_REASON_QUOTA -> "quota"
                17 -> "thermal"
                18 -> "battery_saver"
                19 -> "network_unavailable"
                20 -> "metered_restricted"
                else -> "reason_$reason"
            }

        private fun appStandbyBucketLabel(bucket: Int?): String =
            when (bucket) {
                UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "active"
                UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "working_set"
                UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "frequent"
                UsageStatsManager.STANDBY_BUCKET_RARE -> "rare"
                UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "restricted"
                STANDBY_BUCKET_NEVER -> "never"
                else -> "unknown"
            }

        // Present in framework source, omitted from the public SDK stub.
        private const val STANDBY_BUCKET_NEVER = 50
    }
}
