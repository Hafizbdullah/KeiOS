package os.kei.ui.page.main.widget.glass

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The menu panel's anchored reveal.
 *
 * The pivot compensation is the reason this is a pure function. `drawBackdrop` inverse-transforms its
 * sample using `InverseLayerScope.inverseTransformAtTopLeft`, which reads only `rotationZ`, `scaleX` and
 * `scaleY` and inverts about the element's top-left — it never looks at `transformOrigin`. So the panel
 * cannot express "grow from the button's corner" as an origin; it has to scale about the top-left and
 * translate by `p(1 - s)`, which is the same transform and one the inverse handles exactly. Get that
 * translation wrong and the refraction slides for the length of the animation while the panel itself
 * looks perfectly fine — invisible to a screenshot test, visible to a user.
 */
class LiquidMenuRevealTest {
    private val width = 300f
    private val height = 400f

    private fun transform(
        progress: Float,
        pivotX: Float = 0f,
        pivotY: Float = 0f,
        directionalOffsetPx: Float = 0f,
    ) = liquidMenuTransform(
        progress = progress,
        widthPx = width,
        heightPx = height,
        pivot =
            LiquidMenuPivot(
                pivotX = pivotX,
                pivotY = pivotY,
                directionalOffsetPx = directionalOffsetPx,
            ),
    )

    @Test
    fun aSettledPanelIsUntransformed() {
        val settled = transform(progress = 1f, pivotX = 1f, pivotY = 1f, directionalOffsetPx = -20f)

        assertEquals(1f, settled.scale, 0.0001f)
        assertEquals(1f, settled.alpha, 0.0001f)
        assertEquals(0f, settled.translationX, 0.0001f)
        assertEquals(0f, settled.translationY, 0.0001f)
    }

    @Test
    fun aTopLeftPivotNeedsNoCompensation() {
        val opening = transform(progress = 0f, pivotX = 0f, pivotY = 0f)

        assertTrue(opening.scale < 1f)
        assertEquals(0f, opening.translationX, 0.0001f)
        assertEquals(0f, opening.translationY, 0.0001f)
    }

    @Test
    fun theCompensationPutsThePivotWhereItWasAsked() {
        // The whole point: whatever the pivot, that point must not move while the panel scales.
        // Under scale-about-top-left plus translation, the pivot maps to s*p + t, and t = p(1-s),
        // so it maps back to p exactly.
        listOf(0f, 0.25f, 0.5f, 1f).forEach { fraction ->
            listOf(0f, 0.4f, 0.85f).forEach { progress ->
                val values = transform(progress = progress, pivotX = fraction, pivotY = fraction)
                val pivotPxX = fraction * width
                val pivotPxY = fraction * height

                assertEquals(
                    pivotPxX,
                    values.scale * pivotPxX + values.translationX,
                    0.01f,
                    "pivot x moved at fraction=$fraction progress=$progress",
                )
                assertEquals(
                    pivotPxY,
                    values.scale * pivotPxY + values.translationY,
                    0.01f,
                    "pivot y moved at fraction=$fraction progress=$progress",
                )
            }
        }
    }

    @Test
    fun theDirectionalNudgeDecaysToNothing() {
        val below = transform(progress = 0f, directionalOffsetPx = -24f)
        val settledBelow = transform(progress = 1f, directionalOffsetPx = -24f)

        assertEquals(-24f, below.translationY, 0.0001f)
        assertEquals(0f, settledBelow.translationY, 0.0001f)
    }

    @Test
    fun aPanelAboveItsAnchorNudgesTheOtherWay() {
        val above = transform(progress = 0f, directionalOffsetPx = 24f)

        assertTrue(above.translationY > 0f)
    }

    @Test
    fun scaleAndAlphaAreMonotonicSoTheExitNeverStutters() {
        var previousScale = transform(progress = 0f).scale
        var previousAlpha = transform(progress = 0f).alpha
        (1..20).forEach { step ->
            val values = transform(progress = step / 20f)
            assertTrue(values.scale >= previousScale, "scale regressed at step $step")
            assertTrue(values.alpha >= previousAlpha, "alpha regressed at step $step")
            previousScale = values.scale
            previousAlpha = values.alpha
        }
    }

    @Test
    fun alphaReachesFullBeforeTheScaleDoes() {
        assertEquals(0f, transform(progress = 0f).alpha, 0.0001f)
        assertTrue(
            transform(progress = 0.6f).alpha >= 1f,
            "the panel must be solid before it stops growing, or the rows fade in separately",
        )
    }

    @Test
    fun degenerateSizesAndProgressDoNotProduceNaN() {
        val zeroSized =
            liquidMenuTransform(
                progress = 0.5f,
                widthPx = 0f,
                heightPx = 0f,
                pivot = LiquidMenuPivot(pivotX = 1f, pivotY = 1f),
            )
        assertEquals(0f, zeroSized.translationX, 0.0001f)
        assertEquals(0f, zeroSized.translationY, 0.0001f)

        val notMeasured =
            liquidMenuTransform(
                progress = 0.5f,
                widthPx = Float.NaN,
                heightPx = Float.NaN,
                pivot = LiquidMenuPivot(pivotX = 0.5f, pivotY = 0.5f),
            )
        assertTrue(notMeasured.translationX.isFinite())
        assertTrue(notMeasured.translationY.isFinite())

        val overshoot = transform(progress = 1.7f, pivotX = 1f)
        assertEquals(1f, overshoot.scale, 0.0001f)
        val undershoot = transform(progress = -0.5f)
        assertEquals(0f, undershoot.alpha, 0.0001f)
    }

    @Test
    fun anOutOfRangePivotIsClamped() {
        val clamped = transform(progress = 0f, pivotX = 4f, pivotY = -3f)
        val atEdges = transform(progress = 0f, pivotX = 1f, pivotY = 0f)

        assertEquals(atEdges.translationX, clamped.translationX, 0.0001f)
        assertEquals(atEdges.translationY, clamped.translationY, 0.0001f)
    }
}
