package os.kei.core.privilege

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.system.AppCommandExecutor
import os.kei.core.system.AppCommandResult
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs privileged work by spawning `su` from the app process.
 *
 * Readiness is probed off the caller's thread and cached, because a probe launches a process and can
 * block on the superuser manager's grant dialog. Callers that only render status get the cached
 * answer immediately and a refresh is scheduled behind them.
 *
 * A `su` child cannot hand a binder back to the app process, so the binder capabilities stay unset.
 * The surfaces that use them already carry shell fallbacks.
 */
internal class RootPrivilegeBackend(
    private val commandDispatcher: CoroutineDispatcher,
) : PrivilegeBackend {

    private data class RootState(
        val probed: Boolean,
        val suPath: String?,
        val code: PrivilegeStatusCode,
        val detail: String,
    ) {
        val status: PrivilegeStatus
            get() = PrivilegeStatus(mode = PrivilegeMode.Root, code = code, detail = detail)

        val commandReady: Boolean
            get() = suPath != null && code == PrivilegeStatusCode.Ready
    }

    private val runner = RootShellCommandRunner()

    private val probeExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "KeiOS-RootProbe").apply { isDaemon = true }
        }

    private val probeInFlight = AtomicBoolean(false)

    @Volatile
    private var state = INITIAL_STATE

    @Volatile
    private var statusCallback: ((PrivilegeStatus) -> Unit)? = null

    override val mode: PrivilegeMode = PrivilegeMode.Root

    override val capabilities: Set<PrivilegeCapability> = setOf(PrivilegeCapability.ShellCommand)

    override fun attach(onStatusChanged: (PrivilegeStatus) -> Unit) {
        statusCallback = onStatusChanged
        scheduleProbe(RootShellLocator.PASSIVE_PROBE_TIMEOUT_MS)
    }

    override fun detach() {
        statusCallback = null
    }

    override fun requestAccess() {
        publish(PrivilegeStatus(PrivilegeMode.Root, PrivilegeStatusCode.RequestingPermission))
        scheduleProbe(RootShellLocator.INTERACTIVE_PROBE_TIMEOUT_MS)
    }

    override fun currentStatus(forceRefresh: Boolean): PrivilegeStatus {
        val snapshot = state
        if (forceRefresh || !snapshot.probed) {
            scheduleProbe(RootShellLocator.PASSIVE_PROBE_TIMEOUT_MS)
        }
        return snapshot.status
    }

    override fun canUseCommand(): Boolean = state.commandReady

    override suspend fun execute(
        command: String,
        timeoutMs: Long,
        onSnapshot: (suspend (PrivilegeCommandOutputSnapshot) -> Unit)?,
    ): AppCommandResult {
        val ready = withContext(commandDispatcher) { ensureReadyState() }
        val suPath = ready.suPath
        if (suPath == null || !ready.commandReady) {
            return AppCommandResult(
                stdout = "",
                stderr = ready.status.text,
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        }
        return runner.execute(
            suPath = suPath,
            command = command,
            timeoutMs = timeoutMs,
            maxOutputBytes = AppCommandExecutor.DEFAULT_MAX_OUTPUT_BYTES,
            dispatcher = commandDispatcher,
            onSnapshot = onSnapshot?.let { callback ->
                { snapshot ->
                    callback(
                        PrivilegeCommandOutputSnapshot(
                            stdout = snapshot.stdout,
                            stderr = snapshot.stderr,
                        ),
                    )
                }
            },
        )
    }

    override suspend fun diagnosticRows(): List<Pair<String, String>> {
        val snapshot = withContext(commandDispatcher) { ensureReadyState() }
        return buildList {
            add("Root Binary Installed" to RootShellLocator.hasInstalledBinary().toString())
            add("Root Activated" to snapshot.commandReady.toString())
            add("Root Command Backend" to "su")
            snapshot.suPath?.let { add("Root Su Path" to it) }
            if (snapshot.detail.isNotBlank()) add("Root Status Detail" to snapshot.detail)
        }
    }

    /** Probes inline when the cached answer is still unknown, so the first command does not fail blind. */
    private fun ensureReadyState(): RootState {
        val snapshot = state
        if (snapshot.probed) return snapshot
        return runProbe(RootShellLocator.PASSIVE_PROBE_TIMEOUT_MS)
    }

    private fun scheduleProbe(timeoutMs: Long) {
        if (!probeInFlight.compareAndSet(false, true)) return
        runCatching {
            probeExecutor.execute {
                try {
                    runProbe(timeoutMs)
                } finally {
                    probeInFlight.set(false)
                }
            }
        }.onFailure { probeInFlight.set(false) }
    }

    private fun runProbe(timeoutMs: Long): RootState {
        val resolved = when (val outcome = RootShellLocator.probe(timeoutMs)) {
            is RootProbeOutcome.Granted ->
                RootState(
                    probed = true,
                    suPath = outcome.suPath,
                    code = PrivilegeStatusCode.Ready,
                    detail = "root",
                )

            is RootProbeOutcome.Rejected ->
                RootState(
                    probed = true,
                    suPath = null,
                    code = if (outcome.uid == null) {
                        PrivilegeStatusCode.PermissionNotGranted
                    } else {
                        PrivilegeStatusCode.UnsupportedIdentity
                    },
                    detail = outcome.uid?.toString() ?: outcome.message,
                )

            is RootProbeOutcome.Unavailable ->
                RootState(
                    probed = true,
                    suPath = null,
                    code = PrivilegeStatusCode.ServiceUnavailable,
                    detail = outcome.reason,
                )
        }
        val changed = resolved != state
        state = resolved
        if (changed) publish(resolved.status)
        return resolved
    }

    private fun publish(status: PrivilegeStatus) {
        statusCallback?.invoke(status)
    }

    private companion object {
        val INITIAL_STATE =
            RootState(
                probed = false,
                suPath = null,
                code = PrivilegeStatusCode.Initializing,
                detail = "",
            )
    }
}
