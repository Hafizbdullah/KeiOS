package os.kei.ui.page.main.host.pager

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MainPagerFrameRateTest {
    @Test
    fun `high frame rate follows pager and predictive back motion lifecycle`() {
        assertFalse(
            shouldPreferHighFrameRateForPagerMotion(
                pagerScrollInProgress = false,
                additionalMotionInProgress = false,
            ),
        )
        assertTrue(
            shouldPreferHighFrameRateForPagerMotion(
                pagerScrollInProgress = true,
                additionalMotionInProgress = false,
            ),
        )
        assertTrue(
            shouldPreferHighFrameRateForPagerMotion(
                pagerScrollInProgress = false,
                additionalMotionInProgress = true,
            ),
        )
    }

    @Test
    fun `pager asks for the panel peak without fixing a display mode`() {
        val frameRateSource = sourceFile(MAIN_PAGER_FRAME_RATE_SOURCE)
        val pagerSource = sourceFile(MAIN_LOADED_PAGER_SOURCE)

        // FrameRateCategory.High resolves through the display's frameRateCategoryRate, which is
        // {normal = 60, high = 90} on 5eea1f50 — so the category asked a 120Hz panel for 90 and
        // capped the very motion it was meant to smooth. Ask for the peak mode instead.
        assertFalse("preferredFrameRate(FrameRateCategory" in frameRateSource)
        assertTrue("display.supportedModes.maxOfOrNull { mode -> mode.refreshRate }" in frameRateSource)
        assertTrue("preferredFrameRate(peakFrameRate)" in frameRateSource)
        assertTrue("peakFrameRate = peakFrameRate," in pagerSource)

        // The rate is still read from the display rather than pinned, and the app never selects a
        // mode outright — ARR and thermal policy keep ownership of the actual cadence.
        assertFalse("preferredFrameRate(120" in frameRateSource)
        assertFalse("preferredDisplayModeId" in frameRateSource)
    }
}

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val MAIN_PAGER_FRAME_RATE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/pager/MainPagerFrameRate.kt"
private const val MAIN_LOADED_PAGER_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/pager/MainLoadedPager.kt"
