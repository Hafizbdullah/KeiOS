@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import os.kei.ui.liquidglass.R
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/*
 * The Liquid Glass toast: an iOS-style HUD pill in the upper third of the screen.
 *
 * Sits high rather than at an edge so it obscures neither the navigation chrome nor the content the
 * user is working in, while staying inside their natural focal area.
 */

/** Vertical anchor of the toast stack: fraction from the top of the screen (iOS HUD zone). */
private const val TOAST_TOP_FRACTION = 0.28f

/**
 * The pill scales up from here on enter and back down to it on exit.
 *
 * One value for both directions because there is one motion driver, and the springs give the
 * asymmetry: the enter overshoots, the exit does not.
 */
private const val TOAST_MIN_SCALE = 0.82f

/** Reaches full opacity before the scale settles, so the pill never reads as two separate fades. */
private const val TOAST_ALPHA_GAIN = 1.7f

private const val TOAST_ENTER_DAMPING = 0.72f
private const val TOAST_ENTER_RESPONSE = 0.34f
private const val TOAST_EXIT_DAMPING = 1f
private const val TOAST_EXIT_RESPONSE = 0.22f

/**
 * Host composable that displays Liquid Glass toasts in the upper third of the screen.
 *
 * Mount this once, at the app root, alongside [BindLiquidToastBridge]. It portals itself into the
 * overlay host's notification layer, so it draws above sheets, alerts and action sheets no matter what
 * order they opened in, and — because the portal is a sibling of the scene backdrop's producer, not a
 * child — it is not captured into the backdrop those presentations sample. Without that, an open sheet
 * would paint a blurred ghost of the toast into its own surface.
 *
 * @param state the [LiquidToastState] that owns the queue.
 * @param backdrop overrides what the pill refracts. Leave null in the app: the toast then samples
 *   `LocalSceneBackdrop`, which already holds the whole app pre-painted over an opaque base. Passing
 *   one explicitly exists for harnesses that compose the host outside a `SceneBackdropHost`, and for
 *   the settings preview, which shows a toast inside a card.
 * @param portalToOverlay whether to lift the stack to the top of the window. False keeps it inside the
 *   caller's layout, which is what an inline preview wants — a portalled preview would fly out of its
 *   card and cover the screen.
 */
@Composable
fun LiquidToastHost(
    state: LiquidToastState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    portalToOverlay: Boolean = true,
) {
    val slots = state.visibleSlots
    if (slots.isEmpty()) return

    val stack: @Composable () -> Unit = {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val topPadding = maxHeight * TOAST_TOP_FRACTION
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // key() per token keeps each toast's own motion driver and timer stable as the list
                // mutates, so promoting a queued toast never disturbs the one still on screen.
                slots.forEach { slot ->
                    key(slot.token) {
                        LiquidToastStackItem(
                            slot = slot,
                            backdrop = backdrop,
                            // Read backlog live inside the timer so a backlog appearing mid-display can
                            // expedite this toast (shorten only, never extend).
                            hasBacklog = { state.hasBacklog },
                            onDismiss = { state.dismiss(slot.token) },
                        )
                    }
                }
            }
        }
    }

    if (portalToOverlay) {
        LiquidOverlayPortal(layer = LiquidOverlayLayer.Notification) { stack() }
    } else {
        stack()
    }
}

/**
 * One toast within the stack. Owns its enter, its timed hold and its exit, and calls [onDismiss] only
 * once the exit has finished so the slot is released cleanly — which is what lets the state promote a
 * queued toast into its place.
 *
 * Two effects, one shared `visible` flag, exactly like the modal presentations: the timer decides
 * *when* to leave, the motion effect decides *how*. That split is why a tap dismisses instantly
 * instead of waiting out the current poll tick, and why the previous "stuck toast" bug cannot recur —
 * there is no animation target being re-asserted on every recomposition to fight the timer with.
 */
@Composable
private fun LiquidToastStackItem(
    slot: LiquidToastSlot,
    backdrop: Backdrop?,
    hasBacklog: () -> Boolean,
    onDismiss: () -> Unit,
) {
    val isDark = isAppInDarkTheme()
    val transitionsEnabled = LocalTransitionAnimationsEnabled.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentHasBacklog by rememberUpdatedState(hasBacklog)

    var visible by remember { mutableStateOf(true) }
    val progress = remember { mutableFloatStateOf(0f) }

    // Accessibility-aware base duration. Mirrors what Compose's own Snackbar does: when a screen
    // reader / accessibility service is active, calculateRecommendedTimeoutMillis EXTENDS the time so
    // low-vision and TalkBack users can finish reading.
    val accessibilityManager = LocalAccessibilityManager.current
    val baseDisplay =
        remember(slot.token, accessibilityManager) {
            val original = slot.data.duration.duration
            val recommendedMs =
                accessibilityManager?.calculateRecommendedTimeoutMillis(
                    originalTimeoutMillis = original.inWholeMilliseconds,
                    containsIcons = slot.data.icon != null,
                    containsText = true,
                    containsControls = true,
                )
            recommendedMs?.milliseconds ?: original
        }
    // Only accelerate a backlog when no accessibility service is driving the timeout — never rush a
    // toast away from a user who relies on assistive tech.
    val accelerationAllowed =
        remember(accessibilityManager) {
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = toastAccessibilityProbeDuration.inWholeMilliseconds,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) == toastAccessibilityProbeDuration.inWholeMilliseconds
        }

    // Display timer. Keyed only on the unique token (always distinct), so a repeated identical message
    // can never collide here. Elapsed time accumulates from the delay() ticks themselves (the coroutine
    // clock), NOT System.currentTimeMillis(): mixing wall-clock elapsed with coroutine-clock delays
    // desyncs under background suspension / doze and is untestable. Re-checking the backlog each tick
    // lets a late backlog shorten — but never extend — the remaining time, because
    // resolveToastDisplayLimit only ever returns a smaller limit.
    LaunchedEffect(slot.token) {
        var elapsed = Duration.ZERO
        while (true) {
            val limit =
                resolveToastDisplayLimit(
                    base = baseDisplay,
                    expedited = accelerationAllowed && currentHasBacklog(),
                )
            val remaining = limit - elapsed
            if (remaining <= Duration.ZERO) break
            val tick = minOf(remaining, TOAST_TIMER_TICK)
            delay(tick)
            elapsed += tick
        }
        visible = false
    }

    // Motion. Restarting on `visible` is what makes a tap immediate: it cancels an enter still in
    // flight and animates out from wherever the pill actually got to, rather than snapping.
    LaunchedEffect(visible, transitionsEnabled) {
        suspend fun animateTo(
            target: Float,
            spec: AnimationSpec<Float>,
        ) {
            animate(
                initialValue = progress.floatValue,
                targetValue = target,
                animationSpec = spec,
            ) { value, _ -> progress.floatValue = value }
            progress.floatValue = target
        }

        if (visible) {
            if (transitionsEnabled) {
                animateTo(
                    1f,
                    folmeSpring(damping = TOAST_ENTER_DAMPING, response = TOAST_ENTER_RESPONSE),
                )
            } else {
                progress.floatValue = 1f
            }
        } else {
            if (transitionsEnabled && progress.floatValue > 0f) {
                animateTo(
                    0f,
                    folmeSpring(damping = TOAST_EXIT_DAMPING, response = TOAST_EXIT_RESPONSE),
                )
            } else {
                progress.floatValue = 0f
            }
            currentOnDismiss()
        }
    }

    LiquidToastPill(
        data = slot.data,
        isDark = isDark,
        backdrop = backdrop,
        progressProvider = { progress.floatValue },
        onDismissRequest = { visible = false },
    )
}

/**
 * The pill. Reads [progressProvider] only inside the layer transform, so the whole enter/exit runs on
 * the render thread without recomposing or relaying out the text.
 */
@Composable
private fun LiquidToastPill(
    data: LiquidToastData,
    isDark: Boolean,
    backdrop: Backdrop?,
    progressProvider: () -> Float,
    onDismissRequest: () -> Unit,
) {
    val paneTitle = stringResource(R.string.liquid_toast_pane_title)
    val dismissLabel = stringResource(R.string.liquid_toast_dismiss)
    val interactionSource = remember { MutableInteractionSource() }

    val surface =
        rememberLiquidToastSurface(
            isDark = isDark,
            explicitBackdrop = backdrop,
            transformProvider = {
                val value = progressProvider()
                val scale = liquidToastScale(value)
                scaleX = scale
                scaleY = scale
                presentationFade(liquidToastAlpha(value))
            },
        )

    Box(
        modifier =
            Modifier
                .widthIn(min = 140.dp, max = 300.dp)
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp)
                .then(surface.modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = dismissLabel,
                    onClick = onDismissRequest,
                ).semantics {
                    contentDescription = data.message
                    liveRegion = LiveRegionMode.Polite
                    this.paneTitle = paneTitle
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    tint =
                        if (data.iconTint.isSpecified) {
                            data.iconTint
                        } else {
                            MiuixTheme.colorScheme.onBackground.copy(alpha = 0.90f)
                        },
                )
            }
            Text(
                text = data.message,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Kept out of the composable so the enter/exit shape can be unit-tested. */
internal fun liquidToastScale(progress: Float): Float {
    val eased = progress.coerceIn(0f, 1f)
    return TOAST_MIN_SCALE + (1f - TOAST_MIN_SCALE) * eased
}

internal fun liquidToastAlpha(progress: Float): Float = (progress.coerceIn(0f, 1f) * TOAST_ALPHA_GAIN).coerceIn(0f, 1f)
