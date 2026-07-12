package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedRectangularShape

internal data class LiquidLensParameters(
    val refractionHeight: Float,
    val refractionAmount: Float,
)

/**
 * Applies Backdrop refraction inside the limits documented by the library.
 *
 * Backdrop's lens shader supports rounded rectangular shapes. Its refraction height must stay
 * within the smallest corner radius, and its refraction amount must stay within the shortest side.
 * Keeping those constraints here protects every liquid component from unsupported-shape crashes
 * and corner discontinuities.
 */
internal fun BackdropEffectScope.safeLiquidLens(
    refractionHeight: Float,
    refractionAmount: Float = refractionHeight,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
) {
    val parameters =
        resolveLiquidLensParameters(
            shape = shape,
            size = size,
            layoutDirection = layoutDirection,
            density = this,
            requestedHeight = refractionHeight,
            requestedAmount = refractionAmount,
        ) ?: return

    lens(
        refractionHeight = parameters.refractionHeight,
        refractionAmount = parameters.refractionAmount,
        depthEffect = depthEffect,
        chromaticAberration = chromaticAberration,
    )
}

internal fun resolveLiquidLensParameters(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    requestedHeight: Float,
    requestedAmount: Float,
): LiquidLensParameters? {
    if (
        !requestedHeight.isFinite() ||
        !requestedAmount.isFinite() ||
        requestedHeight <= 0f ||
        requestedAmount <= 0f ||
        !size.width.isFinite() ||
        !size.height.isFinite() ||
        size.width <= 0f ||
        size.height <= 0f
    ) {
        return null
    }

    val cornerRadii = shape.liquidLensCornerRadii(size, layoutDirection, density) ?: return null
    val minCornerRadius = cornerRadii.minOrNull()?.takeIf { it.isFinite() && it > 0f } ?: return null
    val shortestSide = size.minDimension.takeIf { it.isFinite() && it > 0f } ?: return null
    val resolvedHeight = requestedHeight.coerceAtMost(minCornerRadius)
    val resolvedAmount = requestedAmount.coerceAtMost(shortestSide)

    return LiquidLensParameters(
        refractionHeight = resolvedHeight,
        refractionAmount = resolvedAmount,
    )
}

private fun Shape.liquidLensCornerRadii(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): List<Float>? =
    when (this) {
        is RoundedRectangularShape -> {
            val corners = corners(size, layoutDirection, density)
            listOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
        }

        is AbsoluteRoundedCornerShape -> {
            val maxRadius = size.minDimension / 2f
            listOf(
                topStart.toPx(size, density).coerceAtMost(maxRadius),
                topEnd.toPx(size, density).coerceAtMost(maxRadius),
                bottomEnd.toPx(size, density).coerceAtMost(maxRadius),
                bottomStart.toPx(size, density).coerceAtMost(maxRadius),
            )
        }

        is CornerBasedShape -> {
            val maxRadius = size.minDimension / 2f
            val isLtr = layoutDirection == LayoutDirection.Ltr
            listOf(
                (if (isLtr) topStart else topEnd).toPx(size, density).coerceAtMost(maxRadius),
                (if (isLtr) topEnd else topStart).toPx(size, density).coerceAtMost(maxRadius),
                (if (isLtr) bottomEnd else bottomStart).toPx(size, density).coerceAtMost(maxRadius),
                (if (isLtr) bottomStart else bottomEnd).toPx(size, density).coerceAtMost(maxRadius),
            )
        }

        else -> {
            null
        }
    }
