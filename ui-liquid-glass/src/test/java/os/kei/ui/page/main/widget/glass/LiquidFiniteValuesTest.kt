package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidFiniteValuesTest {
    @Test
    fun rangesAreAscendingFiniteAndSafeForFloatMath() {
        assertEquals(0f..1f, liquidFiniteRange(1f..0f))
        assertEquals(3f..3f, liquidFiniteRange(3f..3f))
        assertEquals(2f..2f, liquidFiniteRange(2f..Float.POSITIVE_INFINITY))
        assertEquals(4f..4f, liquidFiniteRange(Float.NEGATIVE_INFINITY..4f))
        assertEquals(0f..0f, liquidFiniteRange(Float.NaN..Float.POSITIVE_INFINITY))
        assertEquals(
            -Float.MAX_VALUE..-Float.MAX_VALUE,
            liquidFiniteRange(-Float.MAX_VALUE..Float.MAX_VALUE),
        )
    }

    @Test
    fun resolverKeepsTheLatestFiniteValueAcrossTransientInvalidValues() {
        val resolver = LiquidFiniteValueResolver()

        assertEquals(0.65f, resolver.resolve(0.65f, 0f..1f))
        assertEquals(0.65f, resolver.resolve(Float.NaN, 0f..1f))
        assertEquals(0.65f, resolver.resolve(Float.POSITIVE_INFINITY, 0f..1f))
        assertEquals(0.65f, resolver.resolve(Float.NEGATIVE_INFINITY, 0f..1f))
        assertEquals(2f, resolver.resolve(Float.NaN, 2f..4f))
    }

    @Test
    fun resolverUsesRangeStartUntilAValidValueArrives() {
        val resolver = LiquidFiniteValueResolver()

        assertEquals(2f, resolver.resolve(Float.NaN, 2f..4f))
        assertEquals(-4f, resolver.resolve(Float.POSITIVE_INFINITY, -4f..-2f))
        assertEquals(3.5f, resolver.resolve(3.5f, 2f..4f))
        assertEquals(3.5f, resolver.resolve(Float.POSITIVE_INFINITY, 2f..4f))
    }

    @Test
    fun progressFractionAlwaysReturnsAFiniteBoundedValue() {
        assertEquals(0.25f, liquidProgressFraction(0.25f, 1f..0f))
        assertEquals(0f, liquidProgressFraction(Float.NaN, 2f..4f))
        assertEquals(0f, liquidProgressFraction(Float.POSITIVE_INFINITY, 2f..4f))
        assertEquals(0f, liquidProgressFraction(3f, 3f..3f))
        assertEquals(0f, liquidProgressFraction(3f, Float.NaN..Float.POSITIVE_INFINITY))

        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
            val fraction = liquidProgressFraction(value, 1f..0f)
            assertTrue(fraction.isFinite())
            assertTrue(fraction in 0f..1f)
        }
    }

    @Test
    fun sliderValueProgressHandlesReversedEqualAndInvalidInputs() {
        assertEquals(0.25f, valueProgress(0.25f, 1f..0f))
        assertEquals(0f, valueProgress(3f, 3f..3f))
        assertEquals(0f, valueProgress(Float.NaN, 0f..1f))
        assertEquals(0f, valueProgress(Float.POSITIVE_INFINITY, 0f..1f))
    }

    @Test
    fun invalidKeyPointsAreIgnoredAndValidValuesAreClamped() {
        val sanitized =
            sanitizeLiquidSliderKeyPoints(
                keyPoints =
                    listOf(
                        LiquidSliderKeyPoint(value = 0.25f, size = 5.dp),
                        LiquidSliderKeyPoint(value = Float.NaN, size = 5.dp),
                        LiquidSliderKeyPoint(value = 0.50f, size = Float.NaN.dp),
                        LiquidSliderKeyPoint(value = 0.75f, size = (-1).dp),
                        LiquidSliderKeyPoint(value = 2f, size = 4.dp),
                    ),
                valueRange = 0f..1f,
            )

        assertEquals(
            listOf(
                LiquidSliderKeyPoint(value = 0.25f, size = 5.dp),
                LiquidSliderKeyPoint(value = 1f, size = 4.dp),
            ),
            sanitized,
        )
    }
}
