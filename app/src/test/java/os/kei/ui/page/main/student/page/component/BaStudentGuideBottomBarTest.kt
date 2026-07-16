package os.kei.ui.page.main.student.page.component

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BaStudentGuideBottomBarTest {
    @Test
    fun denseSixTabGuideUsesIconOnlyBottomBarAtEveryFontScale() {
        assertFalse(guideBottomBarShowsLabels(tabCount = 6, fontScale = 1f))
        assertFalse(guideBottomBarShowsLabels(tabCount = 6, fontScale = 1.5f))
    }

    @Test
    fun compactBottomBarsKeepLabelsOnlyAtReadableFontScales() {
        assertTrue(guideBottomBarShowsLabels(tabCount = 5, fontScale = 1f))
        assertTrue(guideBottomBarShowsLabels(tabCount = 5, fontScale = 1.2f))
        assertFalse(guideBottomBarShowsLabels(tabCount = 5, fontScale = 1.21f))
    }
}
