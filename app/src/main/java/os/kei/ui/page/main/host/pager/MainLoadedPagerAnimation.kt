package os.kei.ui.page.main.host.pager

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween

internal suspend fun animateLoadedPagerPosition(
    start: Float,
    target: Float,
    durationMillis: Int,
    onFrame: (Float) -> Unit
) {
    animate(
        initialValue = start,
        targetValue = target,
        animationSpec =
            tween(
                durationMillis = durationMillis.coerceAtLeast(1),
                easing = EaseInOut,
            ),
    ) { value, _ ->
        onFrame(value)
    }
}
