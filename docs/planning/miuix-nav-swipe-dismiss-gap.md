# miuix-nav swipeDismiss vs. horizontal page content

> **Closed upstream in `0.9.4-4a6b750b-SNAPSHOT` (2026-08-13); do not file the report.** Everything
> below described `0.9.3` and is kept as the record of what was wrong and how it was measured. See
> [What 0.9.4 changed](#what-0-9-4-changed) for the fix, and for the *new* open question that keeps
> the gesture disabled in KeiOS anyway.

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

<a id="what-0-9-4-changed"></a>

## What 0.9.4 changed

`0.9.4-4a6b750b-SNAPSHOT` rewrites the engagement phase. It is option **3** above — *honour a
consumed gesture* — in a stronger form than asked for, since it arbitrates on consumed **movement**
rather than a consumed down:

- engagement now watches `PointerEventPass.Final`, so descendants get the Main pass first. Only the
  *follow* phase still consumes from `Initial`, which is where that behaviour was always wanted;
- a new pure arbiter, `NavSwipeArbitrator`, holds the decision. The first consumed non-zero position
  change opens a one-move confirmation window; a second consumed change locks `ChildOwned`
  permanently, while an unconsumed one releases the evidence and lets navigation claim if travel has
  crossed touch slop and dominates the cross axis. Ownership is terminal either way;
- the arbiter separates the two signals deliberately: child ownership is **not** gated on
  navigation's touch slop, so a lower-slop child still wins. It also distinguishes a real recognizer
  from a `clickable` merely cancelling its press — the single most likely false positive;
- cross-axis-dominant and opposite-direction travel yield to content outright, a second pointer
  cancels engagement, and one joining mid-follow force-cancels the dismiss with zero release
  velocity;
- claimed gestures no longer restart at the claim event: `initialDismissTravelPx` carries the
  slop-exceeding pre-claim travel forward, so the page catches up instead of jumping.

miuix's own comment in `NavDisplay.kt` now states the intent in as many words: *"without stealing
slider or scroll gestures."*

## Why it stays off here regardless

A probe against this build — `dismissDirection = NavSwipeDirection.LeftToRight` on
`keiosNavTransition()`, with `routeAnimationsEnabled=true dismissDirection=LeftToRight` logged from
the running APK to prove the argument reached `NavDisplay` — **could not make the gesture engage at
all** on the API 37 AVD, on the `Settings` route (so `enabled = topIndex > 0` held):

| start | travel | pacing | result |
| --- | --- | --- | --- |
| page content, y=1400 | 420–750px | held mid-drag, and 400/700ms swipes | no translation, no dismiss |
| inert top-bar band, y=252 | 420px | 120ms and 400ms | nothing |
| inert top-bar band, y=252 | 1060px (83% of width) | 150ms and 600ms | nothing |

Whole-framebuffer tile diffs, not eyeballing. Injection itself was verified working in the same
session: the launcher's app drawer opens on a synthesised swipe (2143/2144 tiles), and the OS page
scrolls at 150/300/700/1400ms once end-of-list confounds are removed.

So the original defect is gone and nothing replaced it in the *stealing* direction — but "does not
steal" and "does not work" are indistinguishable from outside, and this probe cannot tell them
apart. Two candidates worth checking before enabling:

1. **A real finger.** The arbiter's confirmation window keys off consumption timing across passes,
   which `adb shell input` may not reproduce. The 0.9.3 measurements in this doc were taken with a
   finger on a physical Xiaomi; the fix deserves the same.
2. **`externalGestureOwnership`.** `navSwipeDismissImpl` blocks the whole sequence from the down when
   `initialExternalOwnership and 1L != 0L` — an odd generation on `NavDisplay`'s internal
   `PredictiveBackOwnership`, meaning "an external back gesture owns this". KeiOS drives its own back
   runtime (`BackNavigationRuntimeController`), and a lease acquired but never released would pin
   that token odd and silently disable every swipe. This is a guess, not a finding: confirming it
   needs instrumentation inside miuix.

Until one of those resolves, enabling would ship a gesture that is either dead or untested.
