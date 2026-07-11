package os.kei.core.shizuku

import os.kei.core.system.AppCommandResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShizukuApiUtilsTest {
    @Test
    fun `command output keeps stderr status when command result has no exit code`() {
        val result =
            AppCommandResult(
                stdout = "",
                stderr = "Shizuku command unavailable: unsupported service uid 1000",
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )

        assertEquals(
            "Shizuku command unavailable: unsupported service uid 1000",
            shizukuCommandOutputOrNull(result),
        )
    }

    @Test
    fun `command output prefers stdout over stderr`() {
        val result =
            AppCommandResult(
                stdout = "shell",
                stderr = "ignored",
                exitCode = 0,
                timedOut = false,
                cancelled = false,
            )

        assertEquals("shell", shizukuCommandOutputOrNull(result))
    }

    @Test
    fun `command output stays null for blank command result`() {
        val result =
            AppCommandResult(
                stdout = "",
                stderr = "",
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )

        assertNull(shizukuCommandOutputOrNull(result))
    }

    @Test
    fun `command ready status text accepts shell or root granted states`() {
        assertEquals(
            true,
            ShizukuApiUtils.isCommandReadyStatusText("Shizuku permission: granted (shell)"),
        )
        assertEquals(
            true,
            ShizukuApiUtils.isCommandReadyStatusText("Shizuku permission: granted (root)"),
        )
    }

    @Test
    fun `command ready status text rejects not granted and denied states`() {
        assertEquals(
            false,
            ShizukuApiUtils.isCommandReadyStatusText("Shizuku permission: not granted"),
        )
        assertEquals(
            false,
            ShizukuApiUtils.isCommandReadyStatusText("Shizuku permission: denied"),
        )
    }

    @Test
    fun `detailed probe parses one batched command and keeps equals in values`() {
        val rows =
            parseShizukuDetailedCommandRows(
                """
                ignored=value
                __keios_shizuku_id=uid=2000(shell) gid=2000(shell)
                __keios_shizuku_whoami=shell
                __keios_shizuku_uname=Linux localhost 6.6
                __keios_shizuku_getenforce=Enforcing
                __keios_shizuku_process_count=321
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                "Shizuku id" to "uid=2000(shell) gid=2000(shell)",
                "Shizuku whoami" to "shell",
                "Shizuku uname" to "Linux localhost 6.6",
                "Shizuku getenforce" to "Enforcing",
                "Shizuku process count" to "321",
            ),
            rows,
        )
    }
}
