package os.kei.core.privilege

import os.kei.core.system.AppCommandResult

/**
 * Explicit opt-out backend.
 *
 * Keeping this as a regular backend lets every caller continue through [PrivilegedShell] while
 * guaranteeing that selecting "Disabled" never probes Shizuku, starts `su`, or requests access.
 */
internal object DisabledPrivilegeBackend : PrivilegeBackend {
    private val status =
        PrivilegeStatus(
            mode = PrivilegeMode.Disabled,
            code = PrivilegeStatusCode.Disabled,
        )

    override val mode: PrivilegeMode = PrivilegeMode.Disabled
    override val capabilities: Set<PrivilegeCapability> = emptySet()

    override fun attach(onStatusChanged: (PrivilegeStatus) -> Unit) {
        onStatusChanged(status)
    }

    override fun detach() = Unit
    override fun requestAccess() = Unit
    override fun currentStatus(forceRefresh: Boolean): PrivilegeStatus = status
    override fun canUseCommand(): Boolean = false

    override suspend fun execute(
        command: String,
        timeoutMs: Long,
        onSnapshot: (suspend (PrivilegeCommandOutputSnapshot) -> Unit)?,
    ): AppCommandResult =
        AppCommandResult(
            stdout = "",
            stderr = status.text,
            exitCode = null,
            timedOut = false,
            cancelled = false,
        )

    override suspend fun diagnosticRows(): List<Pair<String, String>> =
        listOf("Privilege Enabled" to "false")
}
