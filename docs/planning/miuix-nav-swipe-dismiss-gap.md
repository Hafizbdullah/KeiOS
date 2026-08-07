# miuix-nav swipeDismiss vs. horizontal page content

Prepared for an upstream report against `compose-miuix-ui/miuix`. Held back while miuix-nav is
still iterating; file it if a stable release still behaves this way.

Observed on `0.9.3-c6d7d6dd-SNAPSHOT` (miuix-nav `NavSwipeDismiss.kt` unchanged since
`df78b0f4`, 2026-07-13). Tracked locally as [#21](https://github.com/hosizoraru/KeiOS/issues/21).

## The gap

`entry(swipeDismiss = …)` turns on a swipe that no page content can share. `NavDisplay` attaches
`navSwipeDismiss` to the **display container**, and the gesture watches `PointerEventPass.Initial`:

```kotlin
val event = awaitPointerEvent(PointerEventPass.Initial)
…
if (toward > slop && toward >= abs(crossTravel)) {
    claimed = true
    change.consume()          // "claim before nested scroll can"
}
```

Initial dispatches parent-first, so the container sees every pointer before any descendant. Once
travel toward the dismiss direction passes touch slop and beats the cross axis, the container
consumes it — and the follow phase then consumes **every** change on **both** axes until lift.

The kdoc presents this as the fix for a real defect: orientation-locked detectors used to yield the
pointer to nested scrollables, so a dismiss died the moment a finger drifted cross-axis. Claiming on
Initial solves that. The cost is that it also claims from horizontal *controls*, which are not
competing scroll containers and have no way to object.

A descendant cannot pre-empt an Initial-pass ancestor. `awaitFirstDown(requireUnconsumed = false)`
means consuming the down does not help either. There is no edge band, no nested veto, and no
per-subtree opt-out — only the per-route `swipeDismiss` switch.

## What it costs a real app

Every horizontal control on a swipe-enabled route stops working. In KeiOS that is sliders,
`AppSwitch` (drag to toggle), the bottom-bar tab drag, and text fields (drag to move the caret or
select). Measured on a Xiaomi 25098PN5AC, 1220x2656 @3.25:

| gesture | result |
| --- | --- |
| drag a Settings slider thumb right, 239dp from the left edge | page dismisses, slider value unchanged |
| drag the Settings bottom-bar tabs right | page dismisses, tab does not change |

The 239dp start point matters: it rules out the system back gesture and any app-side
`PredictiveBackHandler`, leaving `navSwipeDismiss` as the only claimant.

An app-side workaround has to flip `swipeDismiss` off from a pointer-down listener, which needs a
recomposition to remove the modifier node. That loses whenever the DOWN and a past-slop MOVE land in
the same input batch, and it rebuilds the nav graph on every touch-down on a control.

## What would close it

Any one of these:

1. **Edge band.** `swipeDismiss = NavSwipeDirection.LeftToRight(edgeWidth = 24.dp)` — engage only
   when the down lands in the band. Matches how Android and iOS both scope back gestures, and fixes
   every control at once, present and future.
2. **Nested veto.** A `LocalNavSwipeDismissEnabled` or `Modifier.navSwipeDismissExclusion()` that a
   subtree can set, read inside the engagement phase rather than through recomposition.
3. **Honour a consumed down.** Skip engagement when the down was already consumed by a descendant,
   so a control that claims its own press keeps the gesture.

Option 1 is the smallest change and needs no coordination from callers.

## Local decision

KeiOS enables `swipeDismiss` on no route. Back stays with the system predictive gesture that
`NavDisplay` already drives, which works on every route and never competes with content. Re-enabling
is a per-entry argument in `MainScreenNavHost.kt`; wait for one of the three APIs above first.
