package os.kei.ui.page.main.settings.support

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import os.kei.core.privilege.PrivilegeMode

class SettingsPrivilegedAppListAccessStateTest {
    @Test
    fun `root package query keeps root mode in settings state`() {
        val state = resolvePrivilegedAppListAccessState(PrivilegeMode.Root, 317)

        assertEquals(SettingsAppListAccessMode.Privileged, state?.mode)
        assertEquals(PrivilegeMode.Root, state?.privilegeMode)
        assertEquals(317, state?.detectedCount)
    }

    @Test
    fun `shizuku package query keeps shizuku mode in settings state`() {
        val state = resolvePrivilegedAppListAccessState(PrivilegeMode.Shizuku, 241)

        assertEquals(SettingsAppListAccessMode.Privileged, state?.mode)
        assertEquals(PrivilegeMode.Shizuku, state?.privilegeMode)
        assertEquals(241, state?.detectedCount)
    }

    @Test
    fun `disabled and failed queries fall back to direct detection`() {
        assertNull(resolvePrivilegedAppListAccessState(PrivilegeMode.Disabled, 200))
        assertNull(resolvePrivilegedAppListAccessState(PrivilegeMode.Root, null))
        assertNull(resolvePrivilegedAppListAccessState(PrivilegeMode.Root, 0))
    }
}
