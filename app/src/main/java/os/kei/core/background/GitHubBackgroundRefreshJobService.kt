package os.kei.core.background

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.feature.github.domain.GitHubRefreshRuntimeStore
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshSchedulerDiagnostics

internal const val GITHUB_BACKGROUND_REFRESH_JOB_ID = 42101

class GitHubBackgroundRefreshJobService : JobService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + AppDispatchers.githubNetwork)
    private var activeJob: ActiveJob? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (params.jobId != GITHUB_BACKGROUND_REFRESH_JOB_ID) return false
        val current = activeJob
        if (current?.worker?.isActive == true) {
            AppLogger.i(TAG, "skip github background refresh job because another job is running")
            return false
        }
        jobRunning.set(true)

        val appContext = applicationContext
        val schedulerDiagnostics = buildSchedulerDiagnostics(
            params = params,
            startedAtMillis = System.currentTimeMillis(),
        )
        val stopped = AtomicBoolean(false)
        val worker =
            serviceScope.launch(start = CoroutineStart.LAZY) {
                try {
                    AppForegroundInfoHandler.handleGitHubTick(
                        context = appContext,
                        schedulerDiagnostics = schedulerDiagnostics,
                        suppressQuietBackgroundCompletion = true,
                    )
                } catch (error: CancellationException) {
                    val stopDiagnostics = activeJob
                        ?.takeIf { it.params == params }
                        ?.stopDiagnostics
                    if (stopped.get() && stopDiagnostics != null) {
                        withContext(NonCancellable) {
                            AppForegroundInfoHandler.handleGitHubTickTimeout(
                                context = appContext,
                                schedulerDiagnostics = stopDiagnostics,
                            )
                        }
                    } else {
                        withContext(NonCancellable) {
                            AppForegroundInfoHandler.handleGitHubTickTimeout(
                                context = appContext,
                                schedulerDiagnostics = schedulerDiagnostics,
                            )
                        }
                    }
                    throw error
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "github background refresh job failed", error)
                } finally {
                    if (activeJob?.params == params) {
                        activeJob = null
                    }
                    jobRunning.set(false)
                    if (!stopped.get()) {
                        AppBackgroundScheduler.onTickHandled(
                            context = appContext,
                            action = AppBackgroundTickReceiver.ACTION_GITHUB_TICK,
                        )
                        jobFinished(params, false)
                    }
                }
            }
        activeJob = ActiveJob(
            params = params,
            worker = worker,
            stopped = stopped,
            startedAtMillis = schedulerDiagnostics.startedAtMillis,
        )
        worker.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val current = activeJob?.takeIf { it.params == params } ?: return false
        current.stopped.set(true)
        val schedulerDiagnostics = buildSchedulerDiagnostics(
            params = params,
            startedAtMillis = current.startedAtMillis,
            stopReason = jobStopReasonLabel(params.stopReason),
            rescheduled = true,
        )
        current.stopDiagnostics = schedulerDiagnostics
        current.worker.cancel(
            CancellationException("GitHub background refresh job stopped: ${params.stopReason}")
        )
        jobRunning.set(false)
        return true
    }

    override fun onDestroy() {
        serviceJob.cancel()
        activeJob = null
        jobRunning.set(false)
        super.onDestroy()
    }

    private data class ActiveJob(
        val params: JobParameters,
        val worker: Job,
        val stopped: AtomicBoolean,
        val startedAtMillis: Long,
        @Volatile
        var stopDiagnostics: GitHubRefreshSchedulerDiagnostics? = null,
    )

    companion object {
        private const val TAG = "GitHubBackgroundJob"
        private const val BACKOFF_DELAY_MS = 10L * 60L * 1000L
        private const val EXTRA_ENQUEUED_AT_MS = "github_background_enqueued_at_ms"
        private val jobRunning = AtomicBoolean(false)

        fun enqueueNow(context: Context): Boolean {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(JobScheduler::class.java)
                ?: return false
            val runtime = GitHubRefreshRuntimeStore.state.value
            if (runtime.running && runtime.source == GitHubRefreshSource.BackgroundTick) {
                return true
            }
            val existing = scheduler.getPendingJob(GITHUB_BACKGROUND_REFRESH_JOB_ID)
            if (existing != null) return true
            if (jobRunning.get()) return true
            val jobInfo = buildJobInfo(
                context = appContext,
                enqueuedAtMillis = System.currentTimeMillis(),
            )
            val scheduled = scheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS
            if (!scheduled) {
                AppLogger.w(TAG, "failed to enqueue github background refresh job")
            }
            return scheduled
        }

        internal fun buildJobInfo(
            context: Context,
            enqueuedAtMillis: Long = System.currentTimeMillis(),
        ): JobInfo {
            val appContext = context.applicationContext
            return JobInfo.Builder(
                GITHUB_BACKGROUND_REFRESH_JOB_ID,
                ComponentName(appContext, GitHubBackgroundRefreshJobService::class.java),
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0L)
                .setBackoffCriteria(BACKOFF_DELAY_MS, JobInfo.BACKOFF_POLICY_LINEAR)
                .setExtras(
                    PersistableBundle().apply {
                        putLong(EXTRA_ENQUEUED_AT_MS, enqueuedAtMillis.coerceAtLeast(0L))
                    },
                )
                .build()
        }

        private fun buildSchedulerDiagnostics(
            params: JobParameters,
            startedAtMillis: Long,
            stopReason: String = "",
            rescheduled: Boolean = false,
        ): GitHubRefreshSchedulerDiagnostics =
            GitHubRefreshSchedulerDiagnostics(
                jobId = params.jobId,
                enqueuedAtMillis = params.extras.getLong(EXTRA_ENQUEUED_AT_MS, 0L),
                startedAtMillis = startedAtMillis.coerceAtLeast(0L),
                stopReason = stopReason,
                rescheduled = rescheduled,
            )

        private fun jobStopReasonLabel(reason: Int): String =
            when (reason) {
                JobParameters.STOP_REASON_UNDEFINED -> "undefined"
                JobParameters.STOP_REASON_CANCELLED_BY_APP -> "cancelled_by_app"
                JobParameters.STOP_REASON_PREEMPT -> "preempt"
                JobParameters.STOP_REASON_TIMEOUT -> "timeout"
                JobParameters.STOP_REASON_DEVICE_STATE -> "device_state"
                JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> "battery_not_low"
                JobParameters.STOP_REASON_CONSTRAINT_CHARGING -> "charging"
                JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "connectivity"
                JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> "device_idle"
                JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> "storage_not_low"
                JobParameters.STOP_REASON_QUOTA -> "quota"
                JobParameters.STOP_REASON_BACKGROUND_RESTRICTION -> "background_restriction"
                JobParameters.STOP_REASON_APP_STANDBY -> "app_standby"
                JobParameters.STOP_REASON_USER -> "user"
                JobParameters.STOP_REASON_SYSTEM_PROCESSING -> "system_processing"
                JobParameters.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED -> "estimated_launch_time_changed"
                JobParameters.STOP_REASON_TIMEOUT_ABANDONED -> "timeout_abandoned"
                17 -> "thermal"
                18 -> "battery_saver"
                19 -> "network_unavailable"
                20 -> "metered_restricted"
                else -> "unknown_$reason"
            }
    }
}
