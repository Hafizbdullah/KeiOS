package os.kei.ui.page.main.home

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeInfoCardMaterialTest {
    @Test
    fun overviewCardsStayAiryAndCompact() {
        assertTrue(HOME_INFO_CARD_SURFACE_ALPHA <= 0.30f)
        assertEquals(4.dp, HOME_INFO_CARD_GAP)
        assertEquals(4.dp, HOME_INFO_CARD_VERTICAL_CONTENT_PADDING)
        assertEquals(8.dp, HOME_INFO_CARD_HORIZONTAL_CONTENT_PADDING)
    }
}
