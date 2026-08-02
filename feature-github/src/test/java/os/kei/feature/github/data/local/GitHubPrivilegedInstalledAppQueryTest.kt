package os.kei.feature.github.data.local

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.system.AppCommandResult

class GitHubPrivilegedInstalledAppQueryTest {
    @Test
    fun `parser separates user and system package scopes`() {
        val inventory =
            parseGitHubPrivilegedPackageInventory(
                """
                __keios_user_packages__
                package:com.example.alpha
                package:com.example.beta
                __keios_system_packages__
                package:android
                package:com.android.settings
                """.trimIndent(),
            )

        assertEquals(setOf("com.example.alpha", "com.example.beta"), inventory.userPackages)
        assertEquals(setOf("android", "com.android.settings"), inventory.systemPackages)
        assertEquals(
            setOf("com.example.alpha", "com.example.beta", "android", "com.android.settings"),
            inventory.allPackages,
        )
    }

    @Test
    fun `parser ignores diagnostics outside marked package scopes`() {
        val inventory =
            parseGitHubPrivilegedPackageInventory(
                """
                package:before.marker
                permission warning
                __keios_user_packages__
                package:com.example.visible uid:10123
                package:invalid-package-name
                invalid-line
                """.trimIndent(),
            )

        assertEquals(setOf("com.example.visible"), inventory.userPackages)
        assertTrue(inventory.systemPackages.isEmpty())
    }

    @Test
    fun `system classification wins when package appears in both scopes`() {
        val inventory =
            parseGitHubPrivilegedPackageInventory(
                """
                __keios_user_packages__
                package:Com.Example.Shared
                __keios_system_packages__
                package:com.example.shared
                """.trimIndent(),
            )

        assertTrue(inventory.userPackages.isEmpty())
        assertEquals(setOf("com.example.shared"), inventory.systemPackages)
    }

    @Test
    fun `system classification comparison is locale independent and case insensitive`() {
        val inventory =
            GitHubPrivilegedPackageInventory(
                userPackages = emptySet(),
                systemPackages = setOf("Com.Example.System"),
            )

        assertTrue(inventory.isSystemPackage("com.example.system"))
        assertFalse(inventory.isSystemPackage("com.example.user"))
    }

    @Test
    fun `inventory accepts only a complete successful stdout result`() {
        val stdout =
            """
            __keios_user_packages__
            package:com.example.visible
            """.trimIndent()

        assertEquals(
            setOf("com.example.visible"),
            resolveGitHubPrivilegedPackageInventory(commandResult(stdout = stdout))?.userPackages,
        )
        assertNull(
            resolveGitHubPrivilegedPackageInventory(
                commandResult(stdout = stdout, exitCode = 1),
            ),
        )
        assertNull(
            resolveGitHubPrivilegedPackageInventory(
                commandResult(stdout = stdout, stdoutTruncated = true),
            ),
        )
    }

    @Test
    fun `root and shizuku query inventory while disabled skips privileged shell`() {
        assertTrue(
            shouldQueryGitHubPrivilegedPackageInventory(
                mode = PrivilegeMode.Root,
                supportsShellCommand = true,
            ),
        )
        assertTrue(
            shouldQueryGitHubPrivilegedPackageInventory(
                mode = PrivilegeMode.Shizuku,
                supportsShellCommand = true,
            ),
        )
        assertFalse(
            shouldQueryGitHubPrivilegedPackageInventory(
                mode = PrivilegeMode.Disabled,
                supportsShellCommand = false,
            ),
        )
    }
}

private fun commandResult(
    stdout: String,
    exitCode: Int = 0,
    stdoutTruncated: Boolean = false,
): AppCommandResult =
    AppCommandResult(
        stdout = stdout,
        stderr = "",
        exitCode = exitCode,
        timedOut = false,
        stdoutTruncated = stdoutTruncated,
    )
