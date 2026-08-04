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
    fun `pager requests an adaptive category without fixing a display mode`() {
        val frameRateSource = sourceFile(MAIN_PAGER_FRAME_RATE_SOURCE)
        val pagerSource = sourceFile(MAIN_LOADED_PAGER_SOURCE)

        assertTrue("preferredFrameRate(FrameRateCategory.High)" in frameRateSource)
        assertTrue("preferHighFrameRateForPagerMotion(highFrameRateMotionActive)" in pagerSource)
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
