package os.kei.core.privilege

import os.kei.core.system.AppCommandResult

internal data class PrivilegeCommandOutputSnapshot(
    val stdout: String,
    val stderr: String,
)

/**
 * One privileged execution backend.
 *
 * Implementations own their own readiness probing and publish status changes through the callback
 * handed to [attach]. Command rewriting, status caching for the UI, and mode switching all live in
 * [PrivilegedShell] so every backend behaves the same from a caller's point of view.
 */
internal interface PrivilegeBackend {
    val mode: PrivilegeMode

    val capabilities: Set<PrivilegeCapability>

    fun attach(onStatusChanged: (PrivilegeStatus) -> Unit)

    fun detach()

    /** Triggers whatever grant flow the backend needs: a Shizuku permission dialog, or a `su` call. */
    fun requestAccess()

    fun currentStatus(forceRefresh: Boolean = false): PrivilegeStatus

    fun canUseCommand(): Boolean

    suspend fun execute(
        command: String,
        timeoutMs: Long,
        onSnapshot: (suspend (PrivilegeCommandOutputSnapshot) -> Unit)? = null,
    ): AppCommandResult

    /** Backend-specific diagnostic rows for the About page. */
    suspend fun diagnosticRows(): List<Pair<String, String>>
}
