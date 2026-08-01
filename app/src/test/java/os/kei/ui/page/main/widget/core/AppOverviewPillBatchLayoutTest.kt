package os.kei.ui.page.main.widget.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

class AppOverviewPillBatchLayoutTest {
    @Test
    fun keepsPillsOnOneRowWhenTheyFit() {
        val layout =
            calculateOverviewPillFlowLayout(
                childSizes = listOf(IntSize(80, 28), IntSize(100, 28), IntSize(60, 28)),
                maxWidth = 260,
                horizontalGap = 10,
                verticalGap = 8,
            )

        assertEquals(listOf(IntOffset(0, 0), IntOffset(90, 0), IntOffset(200, 0)), layout.placements)
        assertEquals(260, layout.width)
        assertEquals(28, layout.height)
    }

    @Test
    fun wrapsWholePillAndPreservesVerticalRhythm() {
        val layout =
            calculateOverviewPillFlowLayout(
                childSizes = listOf(IntSize(100, 28), IntSize(100, 28), IntSize(80, 28)),
                maxWidth = 220,
                horizontalGap = 10,
                verticalGap = 8,
            )

        assertEquals(listOf(IntOffset(0, 0), IntOffset(110, 0), IntOffset(0, 36)), layout.placements)
        assertEquals(210, layout.width)
        assertEquals(64, layout.height)
    }

    @Test
    fun batchUniformsMatchActivePillCount() {
        val uniforms =
            resolveOverviewPillBatchUniforms(
                bounds =
                    listOf(
                        Rect(0f, 0f, 120f, 56f),
                        Rect(140f, 0f, 300f, 56f),
                        Rect(0f, 72f, 180f, 128f),
                    ),
                refractionHeight = 48f,
                refractionAmount = 80f,
            )

        assertNotNull(uniforms)
        assertEquals(3, uniforms.pillCount)
        assertEquals(12, uniforms.bounds.size)
        assertEquals(28f, uniforms.pillRadius)
        assertEquals(28f, uniforms.refractionHeight)
        assertEquals(56f, uniforms.refractionAmount)
    }

}
