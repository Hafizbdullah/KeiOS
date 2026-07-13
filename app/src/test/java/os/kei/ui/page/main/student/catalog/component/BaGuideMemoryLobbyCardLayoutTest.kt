package os.kei.ui.page.main.student.catalog.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaGuideMemoryLobbyCardLayoutTest {
    @Test
    fun headerActionsFitWithoutShrinkingTheTouchTarget() {
        val moreActionTouchWidth = 48.dp
        val expandStateIconWidth = 24.dp

        assertEquals(72.dp, MemoryLobbyHeaderActionsWidth)
        assertTrue(MemoryLobbyHeaderActionsWidth >= moreActionTouchWidth + expandStateIconWidth)
    }
}
