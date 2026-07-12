package os.kei.ui.page.main.widget.glass

import kotlin.math.max
import kotlin.math.min

/**
 * Produces an ascending, finite range that is safe for Compose semantics and animations.
 *
 * A range with one finite endpoint collapses to that endpoint. A range with no finite endpoint
 * collapses to zero. Extremely wide ranges whose Float span overflows collapse to their lower
 * endpoint so downstream Float-based animation math remains finite.
 */
internal fun liquidFiniteRange(valueRange: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> {
    val start = valueRange.start
    val end = valueRange.endInclusive
    if (!start.isFinite() && !end.isFinite()) return 0f..0f
    if (!start.isFinite()) return end..end
    if (!end.isFinite()) return start..start

    val lower = min(start, end)
    val upper = max(start, end)
    return if ((upper - lower).isFinite()) lower..upper else lower..lower
}

internal fun liquidFiniteValue(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    fallback: Float = valueRange.start,
): Float {
    val safeRange = liquidFiniteRange(valueRange)
    val safeFallback =
        fallback
            .takeIf(Float::isFinite)
            ?.coerceIn(safeRange)
            ?: safeRange.start
    return value
        .takeIf(Float::isFinite)
        ?.coerceIn(safeRange)
        ?: safeFallback
}

/** Keeps transient NaN/Infinity values from resetting a live control to the start of its range. */
internal class LiquidFiniteValueResolver {
    private var lastFiniteValue: Float? = null

    fun resolve(
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
    ): Float {
        val safeRange = liquidFiniteRange(valueRange)
        if (value.isFinite()) {
            return value.coerceIn(safeRange).also { lastFiniteValue = it }
        }
        return lastFiniteValue?.coerceIn(safeRange) ?: safeRange.start
    }
}
