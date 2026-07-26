package os.kei.ui.page.main.widget.sheet

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidDetentWindowBottomSheetTest {
    @Test
    fun detectsComposeImeInsets() {
        assertTrue(liquidSheetImeVisible(composeImeBottomPx = 1, platformImeVisible = false))
    }

    @Test
    fun dialogWindowContentRebindsBackToTheSheetWindowDispatcher() {
        // The host composition provides miuix-nav's entry-scoped navigation-event owner; it is
        // inherited across the dialog-window boundary and would swallow back unless the window
        // scope re-resolves the dialog's own dispatcher around the sheet content.
        val source = detentSheetSource()
        val dialogHost = source.indexOf("LiquidBackdropWindowDialog(")
        assertTrue("Detent sheet must host through LiquidBackdropWindowDialog", dialogHost >= 0)
        val windowScope = source.indexOf("WindowNavigationEventScope {", dialogHost)
        assertTrue("Dialog window content must rebind via WindowNavigationEventScope", windowScope >= 0)
        val hostContent = source.indexOf("hostContent()", windowScope)
        assertTrue("hostContent must render inside WindowNavigationEventScope", hostContent >= 0)
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

private fun detentSheetSource(): String {
    val relativePath =
        "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/LiquidDetentWindowBottomSheet.kt"
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}
