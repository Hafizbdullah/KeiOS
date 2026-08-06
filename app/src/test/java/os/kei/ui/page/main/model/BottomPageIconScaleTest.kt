package os.kei.ui.page.main.model

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomPageIconScaleTest {
    @Test
    fun defaultScalePagesOmitIdentityLayer() {
        val unscaled = BottomPage.entries.filter { it.iconScale == 1f }

        assertEquals(listOf(BottomPage.Os, BottomPage.Mcp, BottomPage.GitHub), unscaled)
        unscaled.forEach { page ->
            assertEquals(0, Modifier.bottomPageIconScale(page).elementCount())
        }
    }

    @Test
    fun scaledPagesKeepSingleLayer() {
        listOf(BottomPage.Home, BottomPage.Ba).forEach { page ->
            assertEquals(1, Modifier.bottomPageIconScale(page).elementCount())
        }
    }

    /** Starting from a bare Modifier cannot tell a chained layer from a dropped chain. */
    @Test
    fun upstreamChainSurvivesBothBranches() {
        val upstream = Modifier.graphicsLayer { }

        assertEquals(1, upstream.elementCount())
        assertEquals(2, upstream.bottomPageIconScale(BottomPage.Ba).elementCount())
        assertEquals(1, upstream.bottomPageIconScale(BottomPage.Os).elementCount())
    }
}

private fun Modifier.elementCount(): Int = foldIn(0) { count, _ -> count + 1 }
