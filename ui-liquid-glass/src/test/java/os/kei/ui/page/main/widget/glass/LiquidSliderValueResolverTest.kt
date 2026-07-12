package os.kei.ui.page.main.widget.glass

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiquidSliderValueResolverTest {
    private val keyPoints =
        listOf(
            LiquidSliderKeyPoint(0.25f),
            LiquidSliderKeyPoint(0.50f),
            LiquidSliderKeyPoint(0.75f),
        )

    @Test
    fun accessibilityProgressUsesTheSameKeyPointSnapAsTouch() {
        val next =
            resolveSliderProgressChange(
                currentValue = 0.25f,
                target = 0.53f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = 0.05f,
            )

        assertEquals(0.50f, next)
    }

    @Test
    fun accessibilityProgressKeepsTargetOutsideTheMagnetThreshold() {
        val next =
            resolveSliderProgressChange(
                currentValue = 0.25f,
                target = 0.57f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = 0.05f,
            )

        assertEquals(0.57f, next)
    }

    @Test
    fun accessibilityProgressKeepsMovingOutsideTheMagnetThreshold() {
        assertEquals(
            0.51f,
            resolveSliderProgressChange(
                currentValue = 0.50f,
                target = 0.51f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = 0.05f,
            ),
        )
    }

    @Test
    fun fullyDiscreteAccessibilityProgressMovesToTheNextKeyPoint() {
        assertEquals(
            0.50f,
            resolveSliderProgressChange(
                currentValue = 0.25f,
                target = 0.26f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun fullyDiscreteAccessibilityProgressMovesToThePreviousKeyPoint() {
        assertEquals(
            0.25f,
            resolveSliderProgressChange(
                currentValue = 0.50f,
                target = 0.49f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun fullyDiscreteAccessibilityProgressStopsAtTheFirstAndLastKeyPoints() {
        assertNull(
            resolveSliderProgressChange(
                currentValue = 0.25f,
                target = 0.24f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
        assertNull(
            resolveSliderProgressChange(
                currentValue = 0.75f,
                target = 0.76f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun fullyDiscreteAccessibilityProgressKeepsDirectionBetweenKeyPoints() {
        assertEquals(
            0.25f,
            resolveSliderProgressChange(
                currentValue = 0.40f,
                target = 0.39f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
        assertEquals(
            0.50f,
            resolveSliderProgressChange(
                currentValue = 0.40f,
                target = 0.41f,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun progressTargetsAreClampedToTheRange() {
        assertEquals(
            1f,
            resolveSliderProgressChange(
                currentValue = 0.50f,
                target = 4f,
                valueRange = 0f..1f,
                keyPoints = emptyList(),
                snapToKeyPoints = false,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun reversedRangesAreNormalizedBeforeResolvingTargets() {
        assertEquals(
            0.25f,
            resolveSliderTarget(
                target = 0.25f,
                valueRange = 1f..0f,
                keyPoints = emptyList(),
                snapToKeyPoints = false,
                snapThreshold = null,
            ),
        )
    }

    @Test
    fun invalidAccessibilityTargetKeepsTheCurrentFiniteValue() {
        assertNull(
            resolveSliderProgressChange(
                currentValue = 0.40f,
                target = Float.NaN,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = 0.05f,
            ),
        )
        assertNull(
            resolveSliderProgressChange(
                currentValue = 0.40f,
                target = Float.POSITIVE_INFINITY,
                valueRange = 0f..1f,
                keyPoints = keyPoints,
                snapToKeyPoints = true,
                snapThreshold = 0.05f,
            ),
        )
    }

    @Test
    fun invalidKeyPointsDoNotParticipateInSnapping() {
        assertEquals(
            0.52f,
            resolveSliderTarget(
                target = 0.52f,
                valueRange = 0f..1f,
                keyPoints =
                    listOf(
                        LiquidSliderKeyPoint(Float.NaN),
                        LiquidSliderKeyPoint(Float.POSITIVE_INFINITY),
                    ),
                snapToKeyPoints = true,
                snapThreshold = null,
            ),
        )
    }
}
