package os.kei.feature.home.model

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeOverviewModelsTest {
    @Test
    fun `ba activation accepts server-specific friend code formats`() {
        assertFalse(isHomeBaActivated("ARISUKEI"))
        assertFalse(isHomeBaActivated("arisukei"))
        assertFalse(isHomeBaActivated(""))
        assertFalse(isHomeBaActivated("A1B2"))
        assertTrue(isHomeBaActivated("ab12cd34"))
        assertTrue(isHomeBaActivated("GL12CD34"))
    }
}
