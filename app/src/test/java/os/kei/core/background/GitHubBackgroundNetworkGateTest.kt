package os.kei.core.background

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubBackgroundNetworkGateTest {
    @Test
    fun `ready validated network resolves both github hosts`() = runTest {
        val resolvedHosts = mutableListOf<String>()
        val gate =
            GitHubBackgroundNetworkGate(
                stateProvider = { readyState() },
                resolveHost = { host -> resolvedHosts += host; true },
                delayBetweenAttempts = {},
            )

        val result = gate.awaitReady()

        assertTrue(result.ready)
        assertEquals(1, result.attempts)
        assertEquals(listOf("github.com", "api.github.com"), resolvedHosts)
    }

    @Test
    fun `unvalidated network retries without resolving hosts`() = runTest {
        var stateReads = 0
        var resolveCalls = 0
        val gate =
            GitHubBackgroundNetworkGate(
                stateProvider = { stateReads += 1; readyState(validated = false) },
                resolveHost = { resolveCalls += 1; true },
                delayBetweenAttempts = {},
            )

        val result = gate.awaitReady(maxAttempts = 3)

        assertFalse(result.ready)
        assertEquals(3, result.attempts)
        assertEquals(3, stateReads)
        assertEquals(0, resolveCalls)
    }

    @Test
    fun `temporary DNS failure recovers on a later attempt`() = runTest {
        var apiAttempts = 0
        val gate =
            GitHubBackgroundNetworkGate(
                stateProvider = { readyState() },
                resolveHost = { host ->
                    host != "api.github.com" || ++apiAttempts >= 2
                },
                delayBetweenAttempts = {},
            )

        val result = gate.awaitReady(maxAttempts = 3)

        assertTrue(result.ready)
        assertEquals(2, result.attempts)
        assertEquals(2, apiAttempts)
    }

    @Test
    fun `persistent DNS failure requests job retry`() = runTest {
        val gate =
            GitHubBackgroundNetworkGate(
                stateProvider = { readyState() },
                resolveHost = { host -> host != "github.com" },
                delayBetweenAttempts = {},
            )

        val result = gate.awaitReady(maxAttempts = 2)

        assertFalse(result.ready)
        assertEquals("github.com", result.failedHost)
        assertTrue(result.state.ready)
    }
}

private fun readyState(validated: Boolean = true) =
    GitHubBackgroundNetworkState(
        present = true,
        internet = true,
        validated = validated,
        notSuspended = true,
    )
