package os.kei.ui.pip

import org.junit.Test
import kotlin.test.assertEquals

class AppPictureInPictureSeekTest {
    @Test
    fun `seek position moves by delta inside known duration`() {
        assertEquals(
            60_000L,
            resolveAppPictureInPictureSeekPositionMs(
                currentPositionMs = 50_000L,
                durationMs = 120_000L,
                deltaMs = APP_PIP_SEEK_INTERVAL_10_SECONDS_MS,
            ),
        )
    }

    @Test
    fun `seek position clamps to start`() {
        assertEquals(
            0L,
            resolveAppPictureInPictureSeekPositionMs(
                currentPositionMs = 5_000L,
                durationMs = 120_000L,
                deltaMs = -APP_PIP_SEEK_INTERVAL_10_SECONDS_MS,
            ),
        )
    }

    @Test
    fun `seek position clamps to duration`() {
        assertEquals(
            120_000L,
            resolveAppPictureInPictureSeekPositionMs(
                currentPositionMs = 118_000L,
                durationMs = 120_000L,
                deltaMs = APP_PIP_SEEK_INTERVAL_10_SECONDS_MS,
            ),
        )
    }

    @Test
    fun `seek position keeps forward target when duration is unknown`() {
        assertEquals(
            128_000L,
            resolveAppPictureInPictureSeekPositionMs(
                currentPositionMs = 118_000L,
                durationMs = Long.MIN_VALUE + 1L,
                deltaMs = APP_PIP_SEEK_INTERVAL_10_SECONDS_MS,
            ),
        )
    }
}
