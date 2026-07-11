package os.kei.core.background

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.ui.page.main.sync.WebDavAutoSync
import os.kei.ui.page.main.sync.WebDavAutoSyncStatus
import os.kei.ui.page.main.sync.WebDavSyncStore

internal const val WEBDAV_AUTO_SYNC_JOB_ID = 42102

class WebDavAutoSyncJobService : JobService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + AppDispatchers.webDavNetwork)

    @Volatile
    private var activeJob: ActiveJob? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (params.jobId != WEBDAV_AUTO_SYNC_JOB_ID) return false
        if (activeJob?.worker?.isActive == true) return false

        val appContext = applicationContext
        lateinit var execution: ActiveJob
        val worker = serviceScope.launch {
            var shouldRetry = false
            try {
                val summary = WebDavAutoSync.handleScheduledTick(appContext)
                shouldRetry = summary?.status == WebDavAutoSyncStatus.Failed
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                shouldRetry = true
                AppLogger.w(TAG, "WebDAV auto-sync job failed", error)
            } finally {
                if (activeJob === execution) activeJob = null
                if (!execution.stopped) {
                    if (!shouldRetry) {
                        AppBackgroundScheduler.scheduleWebDavAutoSync(appContext)
                    }
                    AppLogger.i(
                        TAG,
                        "finish WebDAV auto-sync job retry=$shouldRetry " +
                            "queued=${params.extras.getLong(EXTRA_ENQUEUED_AT_MS, 0L)}",
                    )
                    jobFinished(params, shouldRetry)
                }
            }
        }
        execution = ActiveJob(params = params, worker = worker)
        activeJob = execution
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val current = activeJob?.takeIf { it.params == params } ?: return false
        current.stopped = true
        current.worker.cancel(
            CancellationException("WebDAV auto-sync job stopped: ${params.stopReason}"),
        )
        activeJob = null
        val reschedule = WebDavSyncStore.loadConfig() != null && WebDavSyncStore.isAutoSyncEnabled()
        AppLogger.i(
            TAG,
            "stop WebDAV auto-sync job reason=${jobStopReasonLabel(params.stopReason)} " +
                "code=${params.stopReason} reschedule=$reschedule",
        )
        return reschedule
    }

    override fun onDestroy() {
        serviceJob.cancel()
        activeJob = null
        super.onDestroy()
    }

    private data class ActiveJob(
        val params: JobParameters,
        val worker: Job,
        @Volatile var stopped: Boolean = false,
    )

    companion object {
        private const val TAG = "WebDavAutoSyncJob"
        private const val BACKOFF_DELAY_MS = 15L * 60L * 1000L
        private const val EXTRA_ENQUEUED_AT_MS = "webdav_auto_sync_enqueued_at_ms"
        private const val EXTRA_DUE_AT_MS = "webdav_auto_sync_due_at_ms"

        fun scheduleAt(
            context: Context,
            dueAtMillis: Long,
            replacePending: Boolean = false,
        ): Boolean {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(JobScheduler::class.java) ?: return false
            val normalizedDueAt = dueAtMillis.coerceAtLeast(System.currentTimeMillis())
            val existing = scheduler.getPendingJob(WEBDAV_AUTO_SYNC_JOB_ID)
            val existingDueAt = existing?.extras?.getLong(EXTRA_DUE_AT_MS, Long.MAX_VALUE)
                ?: Long.MAX_VALUE
            if (existing != null && !replacePending && existingDueAt <= normalizedDueAt) return true
            if (existing != null) scheduler.cancel(WEBDAV_AUTO_SYNC_JOB_ID)
            return scheduler.schedule(
                buildJobInfo(
                    context = appContext,
                    dueAtMillis = normalizedDueAt,
                ),
            ) == JobScheduler.RESULT_SUCCESS
        }

        fun enqueueNow(context: Context, replacePending: Boolean = true): Boolean =
            scheduleAt(
                context = context,
                dueAtMillis = System.currentTimeMillis(),
                replacePending = replacePending,
            )

        fun cancel(context: Context) {
            context.applicationContext
                .getSystemService(JobScheduler::class.java)
                ?.cancel(WEBDAV_AUTO_SYNC_JOB_ID)
        }

        internal fun buildJobInfo(
            context: Context,
            dueAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis(),
        ): JobInfo =
            JobInfo.Builder(
                WEBDAV_AUTO_SYNC_JOB_ID,
                ComponentName(context.applicationContext, WebDavAutoSyncJobService::class.java),
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency((dueAtMillis - nowMillis).coerceAtLeast(0L))
                .setBackoffCriteria(BACKOFF_DELAY_MS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setPersisted(true)
                .setExtras(
                    PersistableBundle().apply {
                        putLong(EXTRA_ENQUEUED_AT_MS, nowMillis.coerceAtLeast(0L))
                        putLong(EXTRA_DUE_AT_MS, dueAtMillis.coerceAtLeast(0L))
                    },
                )
                .build()

        private fun jobStopReasonLabel(reason: Int): String =
            when (reason) {
                JobParameters.STOP_REASON_UNDEFINED -> "undefined"
                JobParameters.STOP_REASON_CANCELLED_BY_APP -> "cancelled_by_app"
                JobParameters.STOP_REASON_PREEMPT -> "preempt"
                JobParameters.STOP_REASON_TIMEOUT -> "timeout"
                JobParameters.STOP_REASON_DEVICE_STATE -> "device_state"
                JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "connectivity"
                JobParameters.STOP_REASON_QUOTA -> "quota"
                JobParameters.STOP_REASON_BACKGROUND_RESTRICTION -> "background_restriction"
                JobParameters.STOP_REASON_APP_STANDBY -> "app_standby"
                JobParameters.STOP_REASON_USER -> "user"
                JobParameters.STOP_REASON_SYSTEM_PROCESSING -> "system_processing"
                17 -> "thermal"
                18 -> "battery_saver"
                19 -> "network_unavailable"
                20 -> "metered_restricted"
                else -> "unknown_$reason"
            }
    }
}
