package os.kei.core.shizuku.service

import kotlin.test.assertEquals
import org.junit.Test

class KeiShizukuUserServiceTest {
    @Test
    fun processTableParserIgnoresHeaderAndMalformedRows() {
        val result =
            parseProcessParentMap(
                """
                PID PPID
                100 1
                malformed
                101 100 command
                """.trimIndent(),
            )

        assertEquals(mapOf(1 to listOf(100), 100 to listOf(101)), result)
    }

    @Test
    fun descendantTraversalUsesParentBeforeChildDiscoveryOrder() {
        val result =
            collectDescendantPids(
                rootPid = 10,
                childPidsByParent =
                    mapOf(
                        10 to listOf(11, 12),
                        11 to listOf(13),
                        13 to listOf(14),
                    ),
                limit = 10,
            )

        assertEquals(listOf(11, 12, 13, 14), result)
    }

    @Test
    fun descendantTraversalHonorsSafetyLimitAndCycles() {
        val result =
            collectDescendantPids(
                rootPid = 20,
                childPidsByParent = mapOf(20 to listOf(21), 21 to listOf(20, 22)),
                limit = 2,
            )

        assertEquals(listOf(21, 22), result)
    }
}
