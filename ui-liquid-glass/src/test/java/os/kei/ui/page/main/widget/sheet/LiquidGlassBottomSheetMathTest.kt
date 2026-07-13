@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidGlassBottomSheetMathTest {
    @Test
    fun blockedDismissGateCoalescesCallbacksFromOneUserAction() {
        val gate = BlockedDismissRequestGate(deduplicationWindowNanos = 200L)
        var dispatchCount = 0

        assertTrue(gate.dispatch(nowNanos = 1_000L) { dispatchCount++ })
        assertFalse(gate.dispatch(nowNanos = 1_050L) { dispatchCount++ })
        assertFalse(gate.dispatch(nowNanos = 1_199L) { dispatchCount++ })
        assertTrue(gate.dispatch(nowNanos = 1_200L) { dispatchCount++ })
        assertEquals(2, dispatchCount)
    }

    @Test
    fun managedContentUsesOpeningHeightUntilUserResizes() {
        assertEquals(
            720,
            liquidSheetManagedContentMaxHeightPx(
                openingContentHeightPx = 720,
                resizedContentHeightPx = 0,
            ),
        )
        assertEquals(
            420,
            liquidSheetManagedContentMaxHeightPx(
                openingContentHeightPx = 720,
                resizedContentHeightPx = 420,
            ),
        )
        assertEquals(
            960,
            liquidSheetManagedContentMaxHeightPx(
                openingContentHeightPx = 720,
                resizedContentHeightPx = 960,
            ),
        )
    }

    @Test
    fun maxVisibleHeightLeavesSafeTopInset() {
        assertEquals(
            1_040f,
            liquidSheetMaxVisibleHeightPx(
                windowHeightPx = 1_120f,
                topInsetPx = 80f,
            ),
        )
        assertEquals(
            1_024f,
            liquidSheetMaxVisibleHeightPx(
                windowHeightPx = 1_120f,
                topInsetPx = 96f,
            ),
        )
    }

    @Test
    fun backgroundBlurLayerExtendsBehindTopCorners() {
        assertEquals(
            504f,
            liquidSheetBackgroundBlurLayerHeightPx(
                sheetTopOffsetPx = 420f,
                cornerRadiusPx = 84f,
                windowHeightPx = 1_120f,
            ),
        )
        assertEquals(
            1_120f,
            liquidSheetBackgroundBlurLayerHeightPx(
                sheetTopOffsetPx = 1_080f,
                cornerRadiusPx = 84f,
                windowHeightPx = 1_120f,
            ),
        )
    }

    @Test
    fun visibleHeightFractionTracksResizableSheetHeight() {
        assertEquals(
            0f,
            liquidSheetVisibleHeightFraction(
                visibleHeightPx = 0f,
                maxVisibleHeightPx = 1_000f,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            liquidSheetVisibleHeightFraction(
                visibleHeightPx = 500f,
                maxVisibleHeightPx = 1_000f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            liquidSheetVisibleHeightFraction(
                visibleHeightPx = 1_200f,
                maxVisibleHeightPx = 1_000f,
            ),
            0.0001f,
        )
    }

    @Test
    fun visualDetentFractionUsesSmallStableSteps() {
        assertEquals(
            0f,
            liquidSheetQuantizedVisualDetentFraction(-0.5f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            liquidSheetQuantizedVisualDetentFraction(0.5f),
            0.0001f,
        )
        assertTrue(
            liquidSheetQuantizedVisualDetentFraction(0.751f) in 0.74f..0.77f,
            "Expected visual detent quantization to preserve smooth height readability",
        )
        assertEquals(
            1f,
            liquidSheetQuantizedVisualDetentFraction(1.2f),
            0.0001f,
        )
    }

    @Test
    fun glassSurfaceTintGainsReadabilityBeforeFullHeight() {
        val shortLightAlpha =
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f / 3f,
            ).alpha
        val tallLightAlpha =
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 0.75f,
            ).alpha
        val fullLightAlpha =
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f,
            ).alpha
        val tallDarkAlpha =
            liquidSheetGlassSurfaceColor(
                isDark = true,
                detentFraction = 0.75f,
            ).alpha

        assertTrue(
            tallLightAlpha > shortLightAlpha + 0.06f,
            "Expected 3/4 detent to add readable tint before full height",
        )
        assertTrue(
            fullLightAlpha > tallLightAlpha,
            "Expected full detent to keep gaining readable tint",
        )
        assertTrue(
            tallDarkAlpha > 0.40f,
            "Expected dark sheet tint to remain readable at 3/4 detent",
        )
        assertTrue(
            liquidSheetSolidness(0.75f) in 0.30f..0.45f,
            "Expected readability curve to engage before full height",
        )
    }

    @Test
    fun glassSurfaceTintStaysInsideAiryOpticalEnvelope() {
        assertEquals(
            0.28f,
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f / 3f,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.50f,
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.34f,
            liquidSheetGlassSurfaceColor(
                isDark = true,
                detentFraction = 1f / 3f,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.58f,
            liquidSheetGlassSurfaceColor(
                isDark = true,
                detentFraction = 1f,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.58f,
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f / 3f,
                surfaceTone = LiquidSheetSurfaceTone.Readable,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.74f,
            liquidSheetGlassSurfaceColor(
                isDark = false,
                detentFraction = 1f,
                surfaceTone = LiquidSheetSurfaceTone.Readable,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.60f,
            liquidSheetGlassSurfaceColor(
                isDark = true,
                detentFraction = 1f / 3f,
                surfaceTone = LiquidSheetSurfaceTone.Readable,
            ).alpha,
            0.002f,
        )
        assertEquals(
            0.78f,
            liquidSheetGlassSurfaceColor(
                isDark = true,
                detentFraction = 1f,
                surfaceTone = LiquidSheetSurfaceTone.Readable,
            ).alpha,
            0.002f,
        )
    }

    @Test
    fun backgroundDepthCurvesIncreaseAfterSheetGrows() {
        assertEquals(
            0.28f,
            liquidSheetBackgroundDimDepth(0f),
            0.0001f,
        )
        assertTrue(
            liquidSheetBackgroundDimDepth(0.5f) > 0.60f,
            "Expected mid-height background dim to support sheet readability",
        )
        assertEquals(
            1f,
            liquidSheetBackgroundDimDepth(1f),
            0.0001f,
        )

        assertEquals(
            0f,
            liquidSheetBackgroundBlurLayerAlpha(0.25f),
            0.0001f,
        )
        assertTrue(
            liquidSheetBackgroundBlurLayerAlpha(0.84f) > 0.85f,
            "Expected high sheet depth to restore strong background blur after settling",
        )
    }

    @Test
    fun adaptiveInitialDetentPromotesOnlyThreeQuarterOverflow() {
        assertEquals(
            LiquidSheetInitialDetent.Full,
            liquidSheetAdaptedInitialDetent(
                initialDetent = LiquidSheetInitialDetent.ThreeQuarter,
                contentOverflowsOpeningDetent = true,
            ),
        )
        assertEquals(
            LiquidSheetInitialDetent.Half,
            liquidSheetAdaptedInitialDetent(
                initialDetent = LiquidSheetInitialDetent.Half,
                contentOverflowsOpeningDetent = true,
            ),
        )
    }
}
