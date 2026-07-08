package os.kei.feature.keepalive.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.system.AppCommandResult

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ShizukuAccessibilitySecureSettingsBridgeTest {
    @Test
    fun `read enabled services parses successful settings output`() = kotlinx.coroutines.test.runTest {
        val runner =
            RecordingCommandRunner(
                result = commandResult(stdout = "com.example/.First:com.other/com.other.Second"),
            )
        val bridge = ShizukuAccessibilitySecureSettingsBridge(runner, timeoutMs = 1234L)

        val read = bridge.readEnabledServiceIds()

        assertEquals(true, read.success)
        assertEquals(
            setOf(
                AccessibilityServiceId("com.example", "com.example.First"),
                AccessibilityServiceId("com.other", "com.other.Second"),
            ),
            read.ids,
        )
        assertEquals(listOf("settings get secure enabled_accessibility_services" to 1234L), runner.commands)
    }

    @Test
    fun `read enabled services treats null output as empty success`() = kotlinx.coroutines.test.runTest {
        val runner = RecordingCommandRunner(result = commandResult(stdout = "null"))
        val bridge = ShizukuAccessibilitySecureSettingsBridge(runner)

        val read = bridge.readEnabledServiceIds()

        assertEquals(true, read.success)
        assertEquals("", read.rawValue)
        assertEquals(emptySet(), read.ids)
    }

    @Test
    fun `timeout maps to read failure reason`() = kotlinx.coroutines.test.runTest {
        val runner =
            RecordingCommandRunner(
                result = commandResult(
                    exitCode = null,
                    timedOut = true,
                ),
            )
        val bridge = ShizukuAccessibilitySecureSettingsBridge(runner)

        val read = bridge.readEnabledServiceIds()

        assertEquals(false, read.success)
        assertEquals("timeout", read.reason)
    }

    @Test
    fun `permission denied maps stderr to read failure reason`() = kotlinx.coroutines.test.runTest {
        val runner =
            RecordingCommandRunner(
                result = commandResult(
                    stderr = "Permission denial",
                    exitCode = 1,
                ),
            )
        val bridge = ShizukuAccessibilitySecureSettingsBridge(runner)

        val read = bridge.readEnabledServiceIds()

        assertEquals(false, read.success)
        assertEquals("Permission denial", read.reason)
        assertEquals(1, runner.commands.size)
    }

    private class RecordingCommandRunner(
        private val result: AppCommandResult,
    ) : AccessibilitySecureSettingsCommandRunner {
        val commands = mutableListOf<Pair<String, Long>>()

        override suspend fun run(
            command: String,
            timeoutMs: Long,
        ): AppCommandResult {
            commands += command to timeoutMs
            return result
        }
    }

    private fun commandResult(
        stdout: String = "",
        stderr: String = "",
        exitCode: Int? = 0,
        timedOut: Boolean = false,
        cancelled: Boolean = false,
    ): AppCommandResult =
        AppCommandResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            timedOut = timedOut,
            cancelled = cancelled,
        )
}
