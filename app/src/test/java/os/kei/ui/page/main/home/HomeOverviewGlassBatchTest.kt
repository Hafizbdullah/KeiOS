package os.kei.ui.page.main.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class HomeOverviewGlassBatchTest {
    @Test
    fun idleCardTransformPreservesGeometry() {
        val transform =
            resolveHomeOverviewCardVisualTransform(
                width = 1208f,
                height = 216f,
                deformationProgress = 0f,
                dragOffset = Offset.Zero,
                pressExpansionPx = 12f,
            )

        assertEquals(1f, transform.scaleX)
        assertEquals(1f, transform.scaleY)
        assertEquals(0f, transform.translationX)
        assertEquals(0f, transform.translationY)
    }

    @Test
    fun pressedCardTransformKeepsLiquidExpansionFinite() {
        val transform =
            resolveHomeOverviewCardVisualTransform(
                width = 1208f,
                height = 216f,
                deformationProgress = 1f,
                dragOffset = Offset(96f, -48f),
                pressExpansionPx = 12f,
            )

        assertTrue(transform.scaleX > 1f)
        assertTrue(transform.scaleY > 1f)
        assertTrue(transform.translationX > 0f)
        assertTrue(transform.translationY < 0f)
    }

    @Test
    fun batchUniformsPackDisconnectedCardsAndClampLens() {
        val uniforms =
            resolveHomeOverviewCardBatchUniforms(
                cards =
                    listOf(
                        HomeOverviewCardBatchRect(
                            bounds = Rect(12f, 0f, 1220f, 216f),
                            radius = 60f,
                            highlightScale = 1.1f,
                        ),
                        HomeOverviewCardBatchRect(
                            bounds = Rect(12f, 228f, 1220f, 336f),
                            radius = 54f,
                        ),
                    ),
                refractionHeight = 80f,
                refractionAmount = 180f,
            )

        assertNotNull(uniforms)
        assertEquals(2, uniforms.cardCount)
        assertEquals(8, uniforms.bounds.size)
        assertEquals(2, uniforms.radii.size)
        assertEquals(2, uniforms.highlightScales.size)
        assertEquals(60f, uniforms.radii[0])
        assertEquals(54f, uniforms.radii[1])
        assertEquals(54f, uniforms.refractionHeight)
        assertEquals(108f, uniforms.refractionAmount)
        assertEquals(1.1f, uniforms.highlightScales[0])
    }

    @Test
    fun batchUniformsRejectEmptyOrDegenerateGeometry() {
        assertNull(
            resolveHomeOverviewCardBatchUniforms(
                cards = emptyList(),
                refractionHeight = 24f,
                refractionAmount = 24f,
            ),
        )
        assertNull(
            resolveHomeOverviewCardBatchUniforms(
                cards =
                    listOf(
                        HomeOverviewCardBatchRect(
                            bounds = Rect.Zero,
                            radius = 20f,
                        ),
                    ),
                refractionHeight = 24f,
                refractionAmount = 24f,
            ),
        )
    }
}
