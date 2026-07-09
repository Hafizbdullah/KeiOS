package os.kei.core.background

import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Test
import os.kei.feature.github.domain.GitHubRefreshRuntimePhase
import os.kei.feature.github.domain.GitHubRefreshRuntimeState
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshSchedulerDiagnostics

class GitHubBackgroundRefreshJobStopStateTest {
    @Test
    fun `stopped job keeps its captured runtime after a newer session starts`() {
        val stoppedRuntime = backgroundRuntime(sessionId = 8L, completedCount = 50)
        val activeRuntime = backgroundRuntime(sessionId = 9L, completedCount = 4)
        val startDiagnostics = GitHubRefreshSchedulerDiagnostics(jobId = GITHUB_BACKGROUND_REFRESH_JOB_ID)
        val stopDiagnostics =
            startDiagnostics.copy(
                stopReason = "timeout",
                rescheduled = true,
            )
        val state = GitHubBackgroundRefreshJobStopState()
        state.bindSession(stoppedRuntime.sessionId)
        state.capture(
            diagnostics = stopDiagnostics,
            runtime = stoppedRuntime,
        )

        val resolved =
            state.resolve(
                fallbackDiagnostics = startDiagnostics,
                fallbackRuntime = activeRuntime,
            )

        assertSame(stopDiagnostics, resolved.diagnostics)
        assertSame(stoppedRuntime, resolved.runtime)
    }

    @Test
    fun `ordinary cancellation uses the current runtime fallback`() {
        val runtime = backgroundRuntime(sessionId = 9L, completedCount = 4)
        val diagnostics = GitHubRefreshSchedulerDiagnostics(jobId = GITHUB_BACKGROUND_REFRESH_JOB_ID)
        val state = GitHubBackgroundRefreshJobStopState()
        state.bindSession(runtime.sessionId)

        val resolved =
            state.resolve(
                fallbackDiagnostics = diagnostics,
                fallbackRuntime = runtime,
            )

        assertSame(diagnostics, resolved.diagnostics)
        assertSame(runtime, resolved.runtime)
    }

    @Test
    fun `stop before runtime creation adopts the session bound by the same job`() {
        val runtime = backgroundRuntime(sessionId = 8L, completedCount = 0)
        val startDiagnostics = GitHubRefreshSchedulerDiagnostics(jobId = GITHUB_BACKGROUND_REFRESH_JOB_ID)
        val stopDiagnostics =
            startDiagnostics.copy(
                stopReason = "quota",
                rescheduled = true,
            )
        val state = GitHubBackgroundRefreshJobStopState()
        state.capture(
            diagnostics = stopDiagnostics,
            runtime = null,
        )
        state.bindSession(runtime.sessionId)

        val resolved =
            state.resolve(
                fallbackDiagnostics = startDiagnostics,
                fallbackRuntime = runtime,
            )

        assertSame(stopDiagnostics, resolved.diagnostics)
        assertSame(runtime, resolved.runtime)
    }

    @Test
    fun `stopped job does not adopt a newer job runtime`() {
        val stoppedRuntime = backgroundRuntime(sessionId = 8L, completedCount = 50)
        val newerRuntime = backgroundRuntime(sessionId = 9L, completedCount = 2)
        val startDiagnostics = GitHubRefreshSchedulerDiagnostics(jobId = GITHUB_BACKGROUND_REFRESH_JOB_ID)
        val stopDiagnostics =
            startDiagnostics.copy(
                stopReason = "battery_saver",
                rescheduled = true,
            )
        val state = GitHubBackgroundRefreshJobStopState()
        state.bindSession(stoppedRuntime.sessionId)
        state.capture(
            diagnostics = stopDiagnostics,
            runtime = null,
        )

        val resolved =
            state.resolve(
                fallbackDiagnostics = startDiagnostics,
                fallbackRuntime = newerRuntime,
        )

        assertSame(stopDiagnostics, resolved.diagnostics)
        assertNull(resolved.runtime)
    }

    private fun backgroundRuntime(
        sessionId: Long,
        completedCount: Int,
    ): GitHubRefreshRuntimeState =
        GitHubRefreshRuntimeState(
            sessionId = sessionId,
            phase = GitHubRefreshRuntimePhase.Running,
            source = GitHubRefreshSource.BackgroundTick,
            running = true,
            totalTrackedCount = 74,
            targetCount = 74,
            completedCount = completedCount,
        )
}
