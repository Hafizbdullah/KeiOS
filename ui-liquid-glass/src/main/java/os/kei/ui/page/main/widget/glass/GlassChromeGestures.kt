package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * Keeps a press inside a piece of floating chrome instead of letting the surface underneath take it.
 *
 * The toolbar and the floating docks sit over a horizontally-swipeable pager. Without this, a finger
 * that drifts a few pixels while pressing an action hands the gesture to the pager: the click is
 * cancelled and the button appears not to work. Consuming position changes — and only those — leaves
 * the down and up events alone, so taps and press feedback still work while the pager never sees the
 * movement.
 *
 * The old action bar solved this by switching pager scrolling off for the whole page whenever the bar
 * was touched, threading an `onInteractionChanged` callback through four layers to do it. This is the
 * local form: nothing outside the chrome's own bounds changes behaviour.
 *
 * **Order matters, and getting it wrong is silent.** Chain this *before* any gesture that needs to
 * read the drag — the highlight's `gestureModifier`, for instance. Modifiers declared earlier are
 * outer, and the Main pass travels inner-to-outer, so an inner claim consumes position changes
 * before an outer gesture ever sees them. Put this last and the finger-tracking highlight and
 * drag-following deformation die without any error: the glass simply stops responding, which reads
 * as the component being dead rather than as a bug.
 *
 * Correct:  `.claimFloatingChromeDrags().then(highlight.gestureModifier)`
 * Wrong:    `.then(highlight.gestureModifier).claimFloatingChromeDrags()`
 *
 * **Only for chrome that floats above scrollable content.** Do not put this on buttons that live
 * inside a list — a list is scrolled by dragging, and starting that drag on a button is normal. That
 * is why [os.kei.ui.animation.InteractiveHighlight.consumeDragChanges] defaults to `false` and why
 * the generic Liquid surfaces leave it off.
 */
fun Modifier.claimFloatingChromeDrags(): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pressed: Boolean
            do {
                val event = awaitPointerEvent()
                pressed = false
                event.changes.fastForEach { change ->
                    if (change.positionChanged()) change.consume()
                    if (change.pressed) pressed = true
                }
            } while (pressed)
        }
    }

/**
 * The same leak-stopper, for a surface that *contains* vertically scrollable content.
 *
 * [claimFloatingChromeDrags] is documented as being only for chrome that floats *above* scrollable
 * content, and the sheet panel is the case that warning describes: it wraps a `verticalScroll`. Using
 * the unrestricted claim there breaks the content's scrolling, and it breaks it in a way that looks
 * like intermittent hardware failure rather than a bug.
 *
 * Measured on the Android 17 AVD, dragging the same 700px through a sheet's content:
 *
 * | gesture duration | travel with the unrestricted claim | travel with it removed |
 * |---|---|---|
 * | 100ms | 579px | — |
 * | 250ms | 598px | — |
 * | 400ms | **0px** | 585px |
 * | 600ms | **0px** | 659px |
 *
 * A quick flick crosses touch slop inside its first event or two, so the content's drag is already
 * active — and consuming — before this claim ever gets a look in. A slow, deliberate drag has to
 * accumulate slop across many small events, and every one of those events was being consumed out from
 * under it, so slop was never reached and the content never moved at all. Hence the symptom report:
 * scrolling "suddenly loses touch" and you have to stop and try again, harder.
 *
 * The fix is to claim only the axis that has no meaning inside the surface. A horizontal drag across a
 * modal sheet must not reach the pager underneath — verified: without any claim, horizontal swipes
 * across the panel dismissed the sheet. A vertical drag must be left entirely alone so the content and
 * the sheet's own nested-scroll connection can arbitrate it between them.
 *
 * Dominance is judged on the drag's *accumulated* vector, not per event: a single event of a slow drag
 * is a couple of pixels of noise and its axis means nothing. Accumulation deliberately uses
 * `positionChangeIgnoreConsumed`, so once a descendant scrollable starts consuming, this still tracks
 * where the finger is going and correctly concludes the gesture is not horizontal.
 */
fun Modifier.claimFloatingChromeCrossAxisDrags(): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pressed: Boolean
            var travel = Offset.Zero
            do {
                val event = awaitPointerEvent()
                pressed = false
                event.changes.fastForEach { change ->
                    travel += change.positionChangeIgnoreConsumed()
                    if (change.pressed) pressed = true
                }
                if (abs(travel.x) > abs(travel.y)) {
                    event.changes.fastForEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                }
            } while (pressed)
        }
    }
