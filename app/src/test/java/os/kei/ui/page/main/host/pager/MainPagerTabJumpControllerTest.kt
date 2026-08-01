package os.kei.ui.page.main.host.pager

import org.junit.Test
import kotlin.test.assertEquals

class MainPagerTabJumpControllerTest {
    @Test
    fun `tab switch keeps the established minimum motion duration`() {
        assertEquals(300, mainPagerTabSwitchDurationMillis(distance = 0))
        assertEquals(300, mainPagerTabSwitchDurationMillis(distance = 1))
        assertEquals(300, mainPagerTabSwitchDurationMillis(distance = 2))
    }

    @Test
    fun `tab switch scales established motion duration for farther pages`() {
        assertEquals(400, mainPagerTabSwitchDurationMillis(distance = 3))
        assertEquals(500, mainPagerTabSwitchDurationMillis(distance = 4))
    }
}
