package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastForEach

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
