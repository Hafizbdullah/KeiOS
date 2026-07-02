@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.page

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration

@Stable
internal class BaGuideCatalogPagerSwitchMotion internal constructor(
    private val farJumpAlpha: Animatable<Float, AnimationVector1D>,
) {
    val contentAlpha: Float
        get() = farJumpAlpha.value

    suspend fun runSwitch(
        distance: Int,
        animationsEnabled: Boolean,
        switch: suspend () -> Unit,
    ) {
        val farJump = distance > 1 && animationsEnabled
        var dimmed = false
        try {
            if (farJump) {
                farJumpAlpha.snapTo(1f)
                farJumpAlpha.animateTo(
                    targetValue = BaGuideCatalogFarJumpDimAlpha,
                    animationSpec =
                        tween(
                            durationMillis =
                                resolvedMotionDuration(
                                    AppMotionTokens.farJumpDimMs,
                                    animationsEnabled,
                                ),
                        ),
                )
                dimmed = true
            }
            switch()
        } finally {
            if (dimmed) {
                farJumpAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis =
                                resolvedMotionDuration(
                                    AppMotionTokens.farJumpRestoreMs,
                                    animationsEnabled,
                                ),
                        ),
                )
            } else if (farJumpAlpha.value != 1f) {
                farJumpAlpha.snapTo(1f)
            }
        }
    }
}

@Composable
internal fun rememberBaGuideCatalogPagerSwitchMotion(): BaGuideCatalogPagerSwitchMotion {
    val farJumpAlpha = remember { Animatable(1f) }
    return remember(farJumpAlpha) {
        BaGuideCatalogPagerSwitchMotion(farJumpAlpha)
    }
}

private const val BaGuideCatalogFarJumpDimAlpha = 0.92f
