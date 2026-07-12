package os.kei.ui.page.main.widget.sheet

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Test
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotWindowListPopupPlacementTest {
    @Test
    fun safeWindowBoundsExcludeSystemInsets() {
        assertEquals(
            IntRect(left = 12, top = 96, right = 1056, bottom = 2260),
            calculateSnapshotPopupWindowBounds(
                windowSize = IntSize(width = 1080, height = 2400),
                safeInsets =
                    SnapshotPopupSafeInsets(
                        left = 12,
                        top = 96,
                        right = 24,
                        bottom = 140,
                    ),
            ),
        )
    }

    @Test
    fun actualProviderOffsetDrivesBelowRevealWhenBelowSpaceIsSmallerButSufficient() {
        val layout =
            layout(
                anchorBounds = IntRect(left = 300, top = 600, right = 500, bottom = 650),
                popupContentSize = IntSize(width = 180, height = 200),
                providerOffset = IntOffset(x = 300, y = 658),
            )

        assertEquals(IntOffset(x = 300, y = 658), layout.offset)
        assertTrue(layout.showBelow)
        assertFalse(layout.showAbove)
        assertEquals(0f, layout.transformOrigin.pivotFractionY)
    }

    @Test
    fun actualProviderOffsetDrivesAboveRevealNearWindowBottom() {
        val layout =
            layout(
                anchorBounds = IntRect(left = 300, top = 900, right = 500, bottom = 950),
                popupContentSize = IntSize(width = 180, height = 200),
                providerOffset = IntOffset(x = 300, y = 692),
            )

        assertTrue(layout.showAbove)
        assertFalse(layout.showBelow)
        assertEquals(1f, layout.transformOrigin.pivotFractionY)
    }

    @Test
    fun centeredProviderOffsetUsesMiddleRevealWhenBothSidesAreConstrained() {
        val layout =
            layout(
                anchorBounds = IntRect(left = 300, top = 475, right = 500, bottom = 525),
                popupContentSize = IntSize(width = 180, height = 600),
                providerOffset = IntOffset(x = 300, y = 200),
            )

        assertFalse(layout.showBelow)
        assertFalse(layout.showAbove)
        assertEquals(0.5f, layout.transformOrigin.pivotFractionY)
    }

    @Test
    fun providerOffsetIsClampedInsideSafeBounds() {
        val layout =
            calculateSnapshotPopupLayout(
                anchorBounds = IntRect(left = 900, top = 900, right = 1000, bottom = 960),
                windowBounds = IntRect(left = 20, top = 100, right = 1060, bottom = 920),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = IntSize(width = 240, height = 260),
                popupMargin = IntRect(left = 8, top = 8, right = 8, bottom = 8),
                alignment = PopupPositionProvider.Align.End,
                placement = SnapshotPopupPlacement.Dropdown,
                providerOffset = IntOffset(x = 980, y = 968),
            )

        assertEquals(IntOffset(x = 812, y = 652), layout.offset)
        assertTrue(layout.showAbove)
    }

    @Test
    fun buttonEndAndActionBarCenterUseStableHorizontalOrigins() {
        val anchor = IntRect(left = 400, top = 400, right = 600, bottom = 460)
        val endLayout =
            layout(
                anchorBounds = anchor,
                popupContentSize = IntSize(width = 160, height = 180),
                providerOffset = IntOffset(x = 432, y = 468),
                placement = SnapshotPopupPlacement.ButtonEnd,
            )
        val centeredLayout =
            layout(
                anchorBounds = anchor,
                popupContentSize = IntSize(width = 160, height = 180),
                providerOffset = IntOffset(x = 420, y = 468),
                placement = SnapshotPopupPlacement.ActionBarCenter,
            )

        assertEquals(1f, endLayout.transformOrigin.pivotFractionX)
        assertEquals(0.5f, centeredLayout.transformOrigin.pivotFractionX)
    }

    private fun layout(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        providerOffset: IntOffset,
        placement: SnapshotPopupPlacement = SnapshotPopupPlacement.Dropdown,
    ): SnapshotPopupLayoutInfo =
        calculateSnapshotPopupLayout(
            anchorBounds = anchorBounds,
            windowBounds = IntRect(left = 0, top = 0, right = 1080, bottom = 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize,
            popupMargin = IntRect(left = 0, top = 8, right = 0, bottom = 8),
            alignment = PopupPositionProvider.Align.Start,
            placement = placement,
            providerOffset = providerOffset,
        )
}
