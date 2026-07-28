package os.kei.core.privilege

import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrivilegeModeTest {
    @After
    fun resetRuntime() {
        PrivilegeModeRuntime.configure(PrivilegeMode.Default)
    }

    @Test
    fun `storage ids round trip`() {
        PrivilegeMode.entries.forEach { mode ->
            assertEquals(mode, PrivilegeMode.fromStorageId(mode.storageId))
        }
    }

    @Test
    fun `unknown storage id falls back to shizuku`() {
        assertEquals(PrivilegeMode.Shizuku, PrivilegeMode.Default)
        assertEquals(PrivilegeMode.Shizuku, PrivilegeMode.fromStorageId("magisk"))
        assertEquals(PrivilegeMode.Shizuku, PrivilegeMode.fromStorageId(null))
        assertEquals(PrivilegeMode.Shizuku, PrivilegeMode.fromStorageId("  "))
    }

    @Test
    fun `storage id tolerates surrounding whitespace`() {
        assertEquals(PrivilegeMode.Root, PrivilegeMode.fromStorageId("  root "))
        assertEquals(PrivilegeMode.Disabled, PrivilegeMode.fromStorageId("  disabled "))
    }

    @Test
    fun `switching modes notifies registered listeners`() {
        val observed = mutableListOf<PrivilegeMode>()
        val listener: (PrivilegeMode) -> Unit = { observed += it }
        PrivilegeModeRuntime.addListener(listener)
        try {
            PrivilegeModeRuntime.set(PrivilegeMode.Root)
            PrivilegeModeRuntime.set(PrivilegeMode.Root)
            PrivilegeModeRuntime.set(PrivilegeMode.Shizuku)
        } finally {
            PrivilegeModeRuntime.removeListener(listener)
        }

        assertEquals(listOf(PrivilegeMode.Root, PrivilegeMode.Shizuku), observed)
    }

    @Test
    fun `configure applies without notifying listeners`() {
        val observed = mutableListOf<PrivilegeMode>()
        val listener: (PrivilegeMode) -> Unit = { observed += it }
        PrivilegeModeRuntime.addListener(listener)
        try {
            PrivilegeModeRuntime.configure(PrivilegeMode.Root)
        } finally {
            PrivilegeModeRuntime.removeListener(listener)
        }

        assertEquals(PrivilegeMode.Root, PrivilegeModeRuntime.mode)
        assertTrue(observed.isEmpty())
    }
}
