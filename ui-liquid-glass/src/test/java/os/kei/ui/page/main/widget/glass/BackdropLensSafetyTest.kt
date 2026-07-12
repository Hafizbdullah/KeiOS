package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.shapes.RoundedRectangle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackdropLensSafetyTest {
    private val density = Density(1f)
    private val size = Size(width = 100f, height = 48f)

    @Test
    fun roundedRectangleClampsHeightToCornerAndAmountToShortestSide() {
        val parameters =
            resolveLiquidLensParameters(
                shape = RoundedRectangle(12.dp),
                size = size,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 28f,
                requestedAmount = 80f,
            )

        assertEquals(12f, parameters?.refractionHeight)
        assertEquals(48f, parameters?.refractionAmount)
    }

    @Test
    fun capsuleClampsHeightToHalfOfShortestSide() {
        val parameters =
            resolveLiquidLensParameters(
                shape = ContinuousCapsule,
                size = Size(width = 100f, height = 40f),
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 28f,
                requestedAmount = 54f,
            )

        assertEquals(20f, parameters?.refractionHeight)
        assertEquals(40f, parameters?.refractionAmount)
    }

    @Test
    fun circleUsesItsResolvedRadius() {
        val parameters =
            resolveLiquidLensParameters(
                shape = CircleShape,
                size = Size(width = 32f, height = 32f),
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 24f,
                requestedAmount = 48f,
            )

        assertEquals(16f, parameters?.refractionHeight)
        assertEquals(32f, parameters?.refractionAmount)
    }

    @Test
    fun zeroRadiusAndUnsupportedShapesDisableLens() {
        assertNull(
            resolveLiquidLensParameters(
                shape = RectangleShape,
                size = size,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 12f,
                requestedAmount = 24f,
            ),
        )
        assertNull(
            resolveLiquidLensParameters(
                shape = UnsupportedShape,
                size = size,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 12f,
                requestedAmount = 24f,
            ),
        )
    }

    @Test
    fun invalidRequestsDisableLens() {
        assertNull(
            resolveLiquidLensParameters(
                shape = CircleShape,
                size = size,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = Float.NaN,
                requestedAmount = 24f,
            ),
        )
        assertNull(
            resolveLiquidLensParameters(
                shape = CircleShape,
                size = Size.Zero,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                requestedHeight = 12f,
                requestedAmount = 24f,
            ),
        )
    }

    private data object UnsupportedShape : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline = Outline.Rectangle(Rect(offset = androidx.compose.ui.geometry.Offset.Zero, size = size))
    }
}
