package os.kei.ui.page.main.ba.support

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

class BaCalendarPoolRenderHelpersTest {
    @Test
    fun decodeDimensionIsClampedToTheSupportedBudget() {
        assertEquals(128, normalizeGameKeeCoverDecodeDimension(1))
        assertEquals(384, normalizeGameKeeCoverDecodeDimension(384))
        assertEquals(2048, normalizeGameKeeCoverDecodeDimension(Int.MAX_VALUE))
    }

    @Test
    fun bitmapCacheSeparatesThumbnailAndBannerBudgets() {
        val thumbnail = gameKeeCoverBitmapCacheKey("https://example.test/cover.png", 384)
        val banner = gameKeeCoverBitmapCacheKey("https://example.test/cover.png", 1280)

        assertNotEquals(thumbnail, banner)
        assertEquals("384:https://example.test/cover.png", thumbnail)
        assertEquals("1280:https://example.test/cover.png", banner)
    }
}
