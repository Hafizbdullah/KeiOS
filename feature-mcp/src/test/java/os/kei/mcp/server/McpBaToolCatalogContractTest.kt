package os.kei.mcp.server

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The BA tools' advertised behaviour, which is what a client reads before deciding whether it may call
 * something without asking the user first.
 */
class McpBaToolCatalogContractTest {
    private fun meta(name: String): McpToolMeta =
        McpToolCatalog.metaForName(name, Locale.ENGLISH) ?: error("$name is not registered")

    @Test
    fun `reading tools stay read-only`() {
        listOf("keios.ba.snapshot", "keios.ba.accounts").forEach { name ->
            assertEquals(true, meta(name).readOnly, name)
            assertEquals(false, meta(name).destructive, name)
        }
    }

    @Test
    fun `the dailies tool is a destructive write that is still safe to retry`() {
        val daily = meta("keios.ba.daily.done")

        assertEquals(false, daily.readOnly)
        // It zeroes AP and restarts cooldowns — it overwrites, it does not append.
        assertEquals(true, daily.destructive)
        // And it is genuinely idempotent: only elapsed cooldowns restart, only free craft slots load.
        // A client may retry a timed-out call without re-asking.
        assertEquals(true, daily.idempotent)
        assertEquals(false, daily.openWorld)
        assertEquals(McpToolExecutionProfile.NormalWrite, daily.executionProfile)
    }

    @Test
    fun `the dailies tool documents that it previews by default`() {
        val apply =
            meta("keios.ba.daily.done").arguments.firstOrNull { it.name == "apply" }
                ?: error("apply argument missing")

        // A caller must be able to learn from the schema alone that the bare call is safe.
        assertTrue("preview" in apply.description.lowercase(Locale.ROOT), apply.description)
    }

    @Test
    fun `both account-scoped tools take an account id`() {
        listOf("keios.ba.snapshot", "keios.ba.daily.done").forEach { name ->
            val arg =
                meta(name).arguments.firstOrNull { it.name == "accountId" }
                    ?: error("$name is missing accountId")
            assertTrue(arg.description.isNotBlank(), name)
            // Every account owns its own AP and craft slots, so the fallback has to be stated.
            assertTrue("Empty" in arg.description, "$name: ${arg.description}")
        }
    }

    @Test
    fun `the new tools join the BA group and its workflow`() {
        listOf("keios.ba.accounts", "keios.ba.daily.done").forEach { name ->
            assertTrue(name in McpToolCatalog.baToolNames, name)
            assertEquals(listOf("ba-daily-brief"), meta(name).workflowTags, name)
        }
    }
}
