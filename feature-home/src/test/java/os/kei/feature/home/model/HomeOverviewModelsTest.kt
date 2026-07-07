package os.kei.feature.home.model

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeOverviewModelsTest {
    @Test
    fun `ba activation accepts server-specific friend code formats`() {
        assertFalse(isHomeBaActivated("ARISUKEI"))
        assertFalse(isHomeBaActivated("arisuke"))
        assertFalse(isHomeBaActivated("arisukei"))
        assertFalse(isHomeBaActivated(""))
        assertFalse(isHomeBaActivated("A1B2"))
        assertFalse(isHomeBaActivated("GL12CD34", serverIndex = 1))
        assertTrue(isHomeBaActivated("ab12cd3"))
        assertTrue(isHomeBaActivated("ab12cd3", serverIndex = 0))
        assertTrue(isHomeBaActivated("GLOBALAB"))
        assertTrue(isHomeBaActivated("GLOBALAB", serverIndex = 1))
    }
}
