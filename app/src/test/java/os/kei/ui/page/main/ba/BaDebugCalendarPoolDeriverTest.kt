package os.kei.ui.page.main.ba

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import os.kei.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaDebugCalendarPoolDeriverTest {
    @Test
    fun `calendar debug entries fall back to sample when real data is missing`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val entries =
            resolveCalendarDebugEntries(
                context = context,
                entries = emptyList(),
                useRealData = true,
                upcoming = true,
                nowMs = NOW_MS,
            )

        assertEquals(1, entries.size)
        assertEquals(context.getString(R.string.ba_debug_sample_calendar_title), entries.single().title)
        assertTrue(entries.single().beginAtMs > NOW_MS)
    }

    @Test
    fun `pool debug entries fall back to sample when real data is missing`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val entries =
            resolvePoolDebugEntries(
                context = context,
                entries = emptyList(),
                useRealData = true,
                upcoming = true,
                nowMs = NOW_MS,
            )

        assertEquals(1, entries.size)
        assertEquals(context.getString(R.string.ba_debug_sample_pool_title), entries.single().name)
        assertTrue(entries.single().startAtMs > NOW_MS)
    }

    private companion object {
        private const val NOW_MS = 1_783_557_600_000L
    }
}
