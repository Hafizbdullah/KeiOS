package os.kei.ui.page.main.student

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GuideVideoPictureInPictureLaunchBoundsTest {
    @Test
    fun `small preview expands to adaptive wide source rect`() {
        val source = GuidePictureInPictureLaunchBounds(850, 1200, 1010, 1290)
        val result = resolveGuidePictureInPictureLaunchBounds(
            windowBounds = GuidePictureInPictureLaunchBounds(0, 0, 1080, 2400),
            sourceRectHint = source,
        )
        assertNotNull(result)

        assertTrue(result.width() > source.width())
        assertTrue(result.height() > source.height())
        assertEquals(16f / 9f, result.width().toFloat() / result.height().toFloat(), 0.02f)
        assertTrue(GuidePictureInPictureLaunchBounds(0, 0, 1080, 2400).contains(result))
    }

    @Test
    fun `large preview keeps existing source rect when it already matches video size`() {
        val source = GuidePictureInPictureLaunchBounds(96, 420, 984, 920)
        val result = resolveGuidePictureInPictureLaunchBounds(
            windowBounds = GuidePictureInPictureLaunchBounds(0, 0, 1080, 2400),
            sourceRectHint = source,
        )

        assertEquals(source, result)
    }

    @Test
    fun `edge source rect clamps adaptive bounds inside window`() {
        val result = resolveGuidePictureInPictureLaunchBounds(
            windowBounds = GuidePictureInPictureLaunchBounds(0, 0, 1080, 2400),
            sourceRectHint = GuidePictureInPictureLaunchBounds(1010, 2200, 1070, 2270),
        )
        assertNotNull(result)

        assertTrue(result.left >= 0)
        assertTrue(result.top >= 0)
        assertTrue(result.right <= 1080)
        assertTrue(result.bottom <= 2400)
        assertEquals(16f / 9f, result.width().toFloat() / result.height().toFloat(), 0.02f)
    }

    @Test
    fun `landscape window uses available height for adaptive video bounds`() {
        val result = resolveGuidePictureInPictureLaunchBounds(
            windowBounds = GuidePictureInPictureLaunchBounds(0, 0, 2400, 1080),
            sourceRectHint = GuidePictureInPictureLaunchBounds(1000, 500, 1120, 568),
        )
        assertNotNull(result)

        assertTrue(result.width() <= 2400)
        assertTrue(result.height() <= 1080)
        assertTrue(result.width() > 120)
        assertEquals(16f / 9f, result.width().toFloat() / result.height().toFloat(), 0.02f)
    }
}
