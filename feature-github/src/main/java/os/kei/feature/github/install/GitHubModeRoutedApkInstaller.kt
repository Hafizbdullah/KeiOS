package os.kei.feature.github.install

import android.content.Context
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.privilege.PrivilegeModeRuntime
import java.util.concurrent.ConcurrentHashMap

/**
 * Dispatches managed installs to the backend the user selected.
 *
 * Staging and committing are separate user actions, so the backend that created a session is
 * remembered against its id. Switching modes between the two steps then still commits through the
 * process that owns the session instead of handing a foreign id to the other backend.
 */
class GitHubModeRoutedApkInstaller(
    private val shizukuInstaller: GitHubManagedApkInstaller = GitHubShizukuPackageInstaller(),
    private val rootInstaller: GitHubManagedApkInstaller = GitHubRootPackageInstaller(),
    private val activeMode: () -> PrivilegeMode = { PrivilegeModeRuntime.mode },
) : GitHubManagedApkInstaller {
    private val sessionOwners = ConcurrentHashMap<Int, PrivilegeMode>()

    override suspend fun stage(
        context: Context,
        request: GitHubApkInstallRequest,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubApkInstallResult {
        val mode = activeMode()
        if (mode == PrivilegeMode.Disabled) return disabledFailure()
        val result = installerFor(mode).stage(context, request, onProgress)
        if (result is GitHubApkInstallResult.Staged && result.sessionId > 0) {
            sessionOwners[result.sessionId] = mode
        }
        return result
    }

    override suspend fun commit(
        context: Context,
        request: GitHubApkInstallRequest,
        sessionId: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubApkInstallResult {
        val owner = ownerOf(sessionId)
        if (owner == PrivilegeMode.Disabled) return disabledFailure(sessionId)
        val result =
            installerFor(owner).commit(
                context = context,
                request = request,
                sessionId = sessionId,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                onProgress = onProgress,
            )
        sessionOwners.remove(sessionId)
        return result
    }

    override suspend fun cancel(context: Context, sessionId: Int) {
        val owner = ownerOf(sessionId)
        if (owner != PrivilegeMode.Disabled) {
            installerFor(owner).cancel(context, sessionId)
        }
        sessionOwners.remove(sessionId)
    }

    private fun ownerOf(sessionId: Int): PrivilegeMode = sessionOwners[sessionId] ?: activeMode()

    private fun installerFor(mode: PrivilegeMode): GitHubManagedApkInstaller = when (mode) {
        PrivilegeMode.Disabled -> error("Disabled privilege mode has no managed installer")
        PrivilegeMode.Shizuku -> shizukuInstaller
        PrivilegeMode.Root -> rootInstaller
    }

    private fun disabledFailure(sessionId: Int = -1): GitHubApkInstallResult.Failed =
        GitHubApkInstallResult.Failed(
            reason = GitHubApkInstallFailureReason.PrivilegeModeDisabled,
            message = "Privilege mode is disabled",
            sessionId = sessionId,
        )
}
