// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package os.kei.core.ui.effect.background

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val BG_EFFECT_HIGH_FPS = 60L
private const val BG_EFFECT_LOW_FPS = 30L
private const val BG_EFFECT_LOW_ALPHA_THRESHOLD = 0.5f
private const val BG_EFFECT_TIME_WRAP_SECONDS = 62.831852f
private const val BG_EFFECT_VISIBLE_ALPHA_THRESHOLD = 0.001f

internal fun Modifier.bgEffectDraw(
    painter: BgEffectPainter,
    preset: BgEffectConfig.Config,
    isDark: Boolean,
    surface: Color,
    effectBackground: Boolean,
    isFullSize: Boolean,
    playing: Boolean,
    colorStage: () -> Float,
    alpha: () -> Float,
): Modifier = this then BgEffectElement(
    painter = painter,
    preset = preset,
    isDark = isDark,
    surface = surface,
    effectBackground = effectBackground,
    isFullSize = isFullSize,
    playing = playing,
    colorStage = colorStage,
    alpha = alpha,
)

private data class BgEffectElement(
    val painter: BgEffectPainter,
    val preset: BgEffectConfig.Config,
    val isDark: Boolean,
    val surface: Color,
    val effectBackground: Boolean,
    val isFullSize: Boolean,
    val playing: Boolean,
    val colorStage: () -> Float,
    val alpha: () -> Float,
) : ModifierNodeElement<BgEffectNode>() {
    override fun create(): BgEffectNode = BgEffectNode(
        painter = painter,
        preset = preset,
        isDark = isDark,
        surface = surface,
        effectBackground = effectBackground,
        isFullSize = isFullSize,
        playing = playing,
        colorStage = colorStage,
        alpha = alpha,
    )

    override fun update(node: BgEffectNode) {
        node.update(
            painter = painter,
            preset = preset,
            isDark = isDark,
            surface = surface,
            effectBackground = effectBackground,
            isFullSize = isFullSize,
            playing = playing,
            colorStage = colorStage,
            alpha = alpha,
        )
    }
}

private class BgEffectNode(
    private var painter: BgEffectPainter,
    private var preset: BgEffectConfig.Config,
    private var isDark: Boolean,
    private var surface: Color,
    private var effectBackground: Boolean,
    private var isFullSize: Boolean,
    private var playing: Boolean,
    private var colorStage: () -> Float,
    private var alpha: () -> Float,
) : Modifier.Node(),
    DrawModifierNode {

    private var animationJob: Job? = null
    private var animTime: Float = 0f
    private var startOffset: Float = 0f
    private var alphaActive: Boolean = false

    override fun onAttach() {
        syncAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: BgEffectPainter,
        preset: BgEffectConfig.Config,
        isDark: Boolean,
        surface: Color,
        effectBackground: Boolean,
        isFullSize: Boolean,
        playing: Boolean,
        colorStage: () -> Float,
        alpha: () -> Float,
    ) {
        val visualChanged =
            this.painter !== painter ||
                this.preset !== preset ||
                this.isDark != isDark ||
                this.surface != surface ||
                this.effectBackground != effectBackground ||
                this.isFullSize != isFullSize ||
                this.alpha !== alpha
        val playbackChanged = this.playing != playing || this.effectBackground != effectBackground
        this.painter = painter
        this.preset = preset
        this.isDark = isDark
        this.surface = surface
        this.effectBackground = effectBackground
        this.isFullSize = isFullSize
        this.colorStage = colorStage
        this.alpha = alpha
        if (playbackChanged) {
            this.playing = playing
            syncAnimation()
        }
        if (visualChanged || playbackChanged) {
            invalidateDraw()
        }
    }

    private fun startAnimation() {
        if (animationJob != null) return
        animationJob?.cancel()
        startOffset = animTime
        animationJob = coroutineScope.launch {
            val origin = withFrameNanos { it }
            var lastEmit = origin
            var lastFrame = origin
            while (isActive) {
                val now = withFrameNanos { it }
                // Keep waking on every delivered VSYNC so 120/90/60 Hz and thermal transitions
                // still share a single phase -- that is why this loop follows Choreographer.
                // But only invalidate at the capped cadence. Measured on a 120 Hz panel
                // (5eea1f50): an untouched Home held a flat 120 fps indefinitely, redrawing the
                // whole Liquid Glass page twice as often as the drift needs.
                //
                // animTime derives from real elapsed time, so the drift runs at exactly the same
                // speed whatever the update rate -- only how often it is sampled changes. On a
                // 60 Hz display (and the AVD, which has no LTPO) the cap matches VSYNC, so
                // behaviour there is unchanged.
                val currentAlpha = alpha()
                val targetFps =
                    if (currentAlpha < BG_EFFECT_LOW_ALPHA_THRESHOLD) {
                        BG_EFFECT_LOW_FPS
                    } else {
                        BG_EFFECT_HIGH_FPS
                    }
                val minDeltaNanos = 1_000_000_000L / targetFps
                // Compare against the midpoint to the *next* VSYNC, not this one. A bare
                // `elapsed < minDelta` test makes a 60 fps cap on a 120 Hz panel land on every
                // third VSYNC (~40 fps) instead of every second, because the second VSYNC arrives
                // at exactly the threshold and jitter pushes it under.
                val framePeriodNanos = (now - lastFrame).coerceAtLeast(0L)
                lastFrame = now
                if (now - lastEmit + framePeriodNanos / 2 < minDeltaNanos) continue
                lastEmit = now
                animTime = (startOffset + (now - origin) / 1_000_000_000f) % BG_EFFECT_TIME_WRAP_SECONDS
                invalidateDraw()
            }
        }
    }

    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun syncAnimation() {
        // Match MIUIX OS3 behavior: keep the coroutine alive while the node is
        // playing and visible. The draw() method's effectBackground gate still
        // suppresses the actual shader draw when the effect is off, so there is
        // no visual leak — but when the effect comes back on the animation is
        // already ticking instead of needing a coroutine restart, removing the
        // visible startup stutter.
        if (playing && alphaActive) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    override fun ContentDrawScope.draw() {
        drawRect(surface)
        if (effectBackground) {
            val alphaValue = alpha()
            val nextAlphaActive = alphaValue > BG_EFFECT_VISIBLE_ALPHA_THRESHOLD
            if (alphaActive != nextAlphaActive) {
                alphaActive = nextAlphaActive
                syncAnimation()
            }
            if (nextAlphaActive) {
                val drawHeight = if (isFullSize) size.height * 0.8f else size.height * 0.5f

                painter.updateResolution(size.width, size.height)
                painter.updateBoundIfNeeded(drawHeight, size.height, size.width)
                painter.updatePresetIfNeeded(isDark)
                painter.updateColors(preset, colorStage())
                painter.updateAnimTime(animTime)
                painter.updatePointsAnim(animTime, preset)

                drawRect(painter.brush, alpha = alphaValue)
            }
        }
        drawContent()
    }
}
