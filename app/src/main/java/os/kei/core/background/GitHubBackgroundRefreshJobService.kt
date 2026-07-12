package os.kei.core.background

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
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
import os.kei.feature.github.domain.GitHubRefreshRuntimeState
import os.kei.feature.github.domain.GitHubRefreshRuntimeStore
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshSchedulerDiagnostics

internal const val GITHUB_BACKGROUND_REFRESH_JOB_ID = 42101

internal data class GitHubBackgroundRefreshStopContext(
    val diagnostics: GitHubRefreshSchedulerDiagnostics,
    val runtime: GitHubRefreshRuntimeState?,
)

internal class GitHubBackgroundRefreshJobStopState {
    private val boundSessionId = AtomicLong(0L)
    private val stopContext = AtomicReference<GitHubBackgroundRefreshStopContext?>(null)

    val isStopped: Boolean
        get() = stopContext.get() != null

    fun bindSession(sessionId: Long) {
        if (sessionId > 0L) {
            boundSessionId.compareAndSet(0L, sessionId)
        }
    }

    fun capture(
        diagnostics: GitHubRefreshSchedulerDiagnostics,
        runtime: GitHubRefreshRuntimeState?,
    ) {
        stopContext.compareAndSet(
            null,
            GitHubBackgroundRefreshStopContext(
                diagnostics = diagnostics,
                runtime = runtime?.takeIf(::isBoundBackgroundRuntime),
            ),
        )
    }

    fun resolve(
        fallbackDiagnostics: GitHubRefreshSchedulerDiagnostics,
        fallbackRuntime: GitHubRefreshRuntimeState?,
    ): GitHubBackgroundRefreshStopContext {
        val captured = stopContext.get()
        return if (captured != null) {
            captured.copy(
                runtime = captured.runtime ?: fallbackRuntime?.takeIf(::isBoundBackgroundRuntime),
            )
        } else {
            GitHubBackgroundRefreshStopContext(
                diagnostics = fallbackDiagnostics,
                runtime = fallbackRuntime?.takeIf(::isBoundBackgroundRuntime),
            )
        }
    }

    private fun isBoundBackgroundRuntime(runtime: GitHubRefreshRuntimeState): Boolean =
        runtime.sessionId > 0L &&
            runtime.sessionId == boundSessionId.get() &&
            runtime.source == GitHubRefreshSource.BackgroundTick
}

class GitHubBackgroundRefreshJobService : JobService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + AppDispatchers.githubNetwork)

    @Volatile
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
        val stopState = GitHubBackgroundRefreshJobStopState()
        lateinit var execution: ActiveJob
        val worker =
            serviceScope.launch(start = CoroutineStart.LAZY) {
                var retry = false
                try {
                    val networkGateResult =
                        GitHubBackgroundNetworkGate
                            .forJob(appContext, params.network)
                            .awaitReady()
                    if (!networkGateResult.ready) {
                        retry = true
                        AppLogger.i(
                            TAG,
                            "defer github background refresh because network is not ready " +
                                "attempts=${networkGateResult.attempts} " +
                                "present=${networkGateResult.state.present} " +
                                "internet=${networkGateResult.state.internet} " +
                                "validated=${networkGateResult.state.validated} " +
                                "notSuspended=${networkGateResult.state.notSuspended} " +
                                "failedHost=${networkGateResult.failedHost.ifBlank { "none" }}",
                        )
                        return@launch
                    }
                    AppForegroundInfoHandler.handleGitHubTick(
                        context = appContext,
                        schedulerDiagnostics = schedulerDiagnostics,
                        suppressQuietBackgroundCompletion = true,
                        cleanupCancellationLocally = false,
                        onRefreshSessionStarted = { session ->
                            execution.stopState.bindSession(session.id)
                        },
                    )
                } catch (error: CancellationException) {
                    val stopContext =
                        execution.stopState.resolve(
                            fallbackDiagnostics = schedulerDiagnostics,
                            fallbackRuntime = GitHubRefreshRuntimeStore.state.value,
                        )
                    withContext(NonCancellable) {
                        AppForegroundInfoHandler.handleGitHubTickStopped(
                            context = appContext,
                            schedulerDiagnostics = stopContext.diagnostics,
                            stoppedRuntime = stopContext.runtime,
                        )
                    }
                    throw error
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "github background refresh job failed", error)
                } finally {
                    if (activeJob === execution) {
                        activeJob = null
                        jobRunning.set(false)
                    }
                    if (!execution.stopState.isStopped) {
                        if (!retry) {
                            AppBackgroundScheduler.onTickHandled(
                                context = appContext,
                                action = AppBackgroundTickReceiver.ACTION_GITHUB_TICK,
                            )
                        }
                        jobFinished(params, retry)
                    }
                }
            }
        execution =
            ActiveJob(
                params = params,
                worker = worker,
                stopState = stopState,
                startedAtMillis = schedulerDiagnostics.startedAtMillis,
            )
        activeJob = execution
        worker.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val current = activeJob?.takeIf { it.params == params } ?: return false
        val stopReason = jobStopReasonLabel(params.stopReason)
        val schedulerDiagnostics = buildSchedulerDiagnostics(
            params = params,
            startedAtMillis = current.startedAtMillis,
            stopReason = stopReason,
            rescheduled = true,
        )
        val runtime = GitHubRefreshRuntimeStore.state.value
        AppLogger.i(
            TAG,
            "stop github background refresh job reason=$stopReason code=${params.stopReason} " +
                "session=${runtime.sessionId} phase=${runtime.phase} " +
                "progress=${runtime.completedCount}/${runtime.targetCount} reschedule=true",
        )
        current.stopState.capture(
            diagnostics = schedulerDiagnostics,
            runtime =
                runtime.takeIf {
                    it.sessionId > 0L && it.source == GitHubRefreshSource.BackgroundTick
                },
        )
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
        val stopState: GitHubBackgroundRefreshJobStopState,
        val startedAtMillis: Long,
    )

    companion object {
        private const val TAG = "GitHubBackgroundJob"
        private const val BACKOFF_DELAY_MS = 10L * 60L * 1000L
        private const val EXTRA_ENQUEUED_AT_MS = "github_background_enqueued_at_ms"
        private val jobRunning = AtomicBoolean(false)

        fun enqueueNow(
            context: Context,
            replacePending: Boolean = false,
        ): Boolean {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(JobScheduler::class.java)
                ?: return false
            val runtime = GitHubRefreshRuntimeStore.state.value
            if (runtime.running && runtime.source == GitHubRefreshSource.BackgroundTick) {
                return true
            }
            val existing = scheduler.getPendingJob(GITHUB_BACKGROUND_REFRESH_JOB_ID)
            if (existing != null && !replacePending) return true
            if (jobRunning.get()) return true
            if (existing != null) {
                scheduler.cancel(GITHUB_BACKGROUND_REFRESH_JOB_ID)
            }
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
