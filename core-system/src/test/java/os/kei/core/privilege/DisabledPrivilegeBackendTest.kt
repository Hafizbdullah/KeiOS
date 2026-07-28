package os.kei.core.privilege

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisabledPrivilegeBackendTest {
    @Test
    fun `disabled backend exposes no privileged capability or access request`() = runTest {
        var attachedStatus: PrivilegeStatus? = null

        DisabledPrivilegeBackend.attach { attachedStatus = it }
        DisabledPrivilegeBackend.requestAccess()
        val result =
            DisabledPrivilegeBackend.execute(
                command = "id",
                timeoutMs = 1_000L,
                onSnapshot = null,
            )

        assertEquals(
            PrivilegeStatus(PrivilegeMode.Disabled, PrivilegeStatusCode.Disabled),
            attachedStatus,
        )
        assertTrue(DisabledPrivilegeBackend.capabilities.isEmpty())
        assertFalse(DisabledPrivilegeBackend.canUseCommand())
        assertFalse(result.succeeded)
        assertEquals("Privilege mode: disabled", result.stderr)
    }
}
