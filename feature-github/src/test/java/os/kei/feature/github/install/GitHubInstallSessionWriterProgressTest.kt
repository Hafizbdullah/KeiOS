package os.kei.feature.github.install

import org.junit.Test
import kotlin.test.assertEquals

class GitHubInstallSessionWriterProgressTest {
    @Test
    fun `incomplete download never rounds to one hundred percent`() {
        assertEquals(99, downloadProgressPercent(98_160_000L, 98_560_000L))
        assertEquals(99, downloadProgressPercent(99L, 100L))
    }

    @Test
    fun `exact or greater byte count reports one hundred percent`() {
        assertEquals(100, downloadProgressPercent(100L, 100L))
        assertEquals(100, downloadProgressPercent(101L, 100L))
    }

    @Test
    fun `unknown and empty progress remain zero`() {
        assertEquals(0, downloadProgressPercent(0L, 100L))
        assertEquals(0, downloadProgressPercent(50L, -1L))
    }
}
