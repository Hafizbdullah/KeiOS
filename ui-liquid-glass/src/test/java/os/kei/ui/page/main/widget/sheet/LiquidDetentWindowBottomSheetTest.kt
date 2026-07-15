package os.kei.ui.page.main.widget.sheet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidDetentWindowBottomSheetTest {
    @Test
    fun detectsComposeImeInsets() {
        assertTrue(liquidSheetImeVisible(composeImeBottomPx = 1, platformImeVisible = false))
    }

    @Test
    fun detectsPlatformImeInsetsForDialogWindows() {
        assertTrue(liquidSheetImeVisible(composeImeBottomPx = 0, platformImeVisible = true))
    }

    @Test
    fun reportsHiddenImeWhenBothSourcesAreClear() {
        assertFalse(liquidSheetImeVisible(composeImeBottomPx = 0, platformImeVisible = false))
    }
}
