package os.kei.feature.github.install

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Test
import os.kei.core.privilege.PrivilegeMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubLookupConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GitHubRootPackageInstallerTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `session id is read from the pm create line`() {
        assertEquals(1935741244, parseRootInstallSessionId("Success: created install session [1935741244]"))
    }

    @Test
    fun `session id survives vendor output without the success prefix`() {
        assertEquals(42, parseRootInstallSessionId("created session [42]\n"))
    }

    @Test
    fun `missing or unusable session id maps to null`() {
        assertNull(parseRootInstallSessionId("Error: java.lang.SecurityException"))
        assertNull(parseRootInstallSessionId("Success: created install session []"))
        assertNull(parseRootInstallSessionId("Success: created install session [0]"))
        assertNull(parseRootInstallSessionId(""))
    }

    @Test
    fun `success line reports success`() {
        val outcome = parseRootInstallOutcome("Success\n")

        assertTrue(outcome.succeeded)
        assertEquals("Success", outcome.message)
    }

    @Test
    fun `failure line outranks a success line in the same output`() {
        val outcome =
            parseRootInstallOutcome(
                """
                Success: streamed 4096 bytes
                Failure [INSTALL_FAILED_VERSION_DOWNGRADE]
                """.trimIndent(),
            )

        assertFalse(outcome.succeeded)
        assertEquals("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]", outcome.message)
    }

    @Test
    fun `output without a terminal line is not treated as success`() {
        val outcome = parseRootInstallOutcome("Exception occurred while executing 'install-commit'")

        assertFalse(outcome.succeeded)
        assertEquals("Exception occurred while executing 'install-commit'", outcome.message)
    }

    @Test
    fun `empty output is not treated as success`() {
        assertFalse(parseRootInstallOutcome("").succeeded)
    }

    @Test
    fun `commit follows the backend that created the session`() = runTest {
        val shizuku = RecordingInstaller(sessionId = 11)
        val root = RecordingInstaller(sessionId = 22)
        var mode = PrivilegeMode.Root
        val router =
            GitHubModeRoutedApkInstaller(
                shizukuInstaller = shizuku,
                rootInstaller = root,
                activeMode = { mode },
            )

        val staged = router.stage(context, REQUEST) {}
        mode = PrivilegeMode.Shizuku
        router.commit(
            context = context,
            request = REQUEST,
            sessionId = (staged as GitHubApkInstallResult.Staged).sessionId,
        )

        assertEquals(listOf("stage", "commit:22"), root.calls)
        assertTrue(shizuku.calls.isEmpty())
    }

    @Test
    fun `an unknown session falls back to the active backend`() = runTest {
        val shizuku = RecordingInstaller(sessionId = 11)
        val root = RecordingInstaller(sessionId = 22)
        val router =
            GitHubModeRoutedApkInstaller(
                shizukuInstaller = shizuku,
                rootInstaller = root,
                activeMode = { PrivilegeMode.Shizuku },
            )

        router.cancel(context, sessionId = 999)

        assertEquals(listOf("cancel:999"), shizuku.calls)
        assertTrue(root.calls.isEmpty())
    }

    @Test
    fun `disabled mode rejects managed install without touching a privileged backend`() = runTest {
        val shizuku = RecordingInstaller(sessionId = 11)
        val root = RecordingInstaller(sessionId = 22)
        val router =
            GitHubModeRoutedApkInstaller(
                shizukuInstaller = shizuku,
                rootInstaller = root,
                activeMode = { PrivilegeMode.Disabled },
            )

        val result = router.stage(context, REQUEST) {}

        assertEquals(
            GitHubApkInstallFailureReason.PrivilegeModeDisabled,
            (result as GitHubApkInstallResult.Failed).reason,
        )
        assertTrue(shizuku.calls.isEmpty())
        assertTrue(root.calls.isEmpty())
    }

    private class RecordingInstaller(
        private val sessionId: Int,
    ) : GitHubManagedApkInstaller {
        val calls = mutableListOf<String>()

        override suspend fun stage(
            context: Context,
            request: GitHubApkInstallRequest,
            onProgress: suspend (GitHubApkInstallProgress) -> Unit,
        ): GitHubApkInstallResult {
            calls += "stage"
            return GitHubApkInstallResult.Staged(requestId = request.requestId, sessionId = sessionId)
        }

        override suspend fun commit(
            context: Context,
            request: GitHubApkInstallRequest,
            sessionId: Int,
            downloadedBytes: Long,
            totalBytes: Long,
            onProgress: suspend (GitHubApkInstallProgress) -> Unit,
        ): GitHubApkInstallResult {
            calls += "commit:$sessionId"
            return GitHubApkInstallResult.Succeeded(
                requestId = request.requestId,
                sessionId = sessionId,
                packageName = "",
            )
        }

        override suspend fun cancel(context: Context, sessionId: Int) {
            calls += "cancel:$sessionId"
        }
    }

    private companion object {
        val REQUEST =
            GitHubApkInstallRequest(
                owner = "octocat",
                repo = "hello",
                releaseTag = "v1",
                projectUrl = "https://github.com/octocat/hello",
                asset =
                    GitHubReleaseAssetFile(
                        name = "app.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 128L,
                        downloadCount = 0,
                    ),
                lookupConfig = GitHubLookupConfig(),
            )
    }
}
