# Why scrolling a Liquid sheet is not smooth

Reported as "the frame rate looks a bit low when scrolling up and down in a liquid sheet". Measured
on the API 37 AVD (`KeiOS_API37_Validation`, 1280x2856 @480dpi, 120Hz, vsync interval 8.33ms) against
the installed `os.kei` 1.13.0 — no rebuild, so this is the shipped build's behaviour. Harness:
`scripts/perf/frame_stages.py` over `dumpsys gfxinfo <pkg> framestats`, per
`hwui-frame-budget.md`'s rule of reading only the per-frame stage columns.

Every row below is two independent runs. They agree to within a few tenths of a millisecond, so the
differences here are far outside the noise this instrument is known to have.

## The headline: scrolling is not the cause

| state | frames / 3s | total p50 | RT issue->swap p50 | gpu p50 |
|---|---|---|---|---|
| Home at rest, no sheet | >=119 (capped) | 16.19 / 16.78 | 3.40 / 3.22 | 11.16 / 11.57 |
| Home page scroll | >=119 | 17.42 / 17.49 | 3.25 / 3.29 | 11.96 / 12.14 |
| **sheet open, at rest, zero input** | 72 / 72 | **124.52 / 124.89** | **36.14 / 36.29** | 6.97 / 7.04 |
| **sheet open, scrolling** | >=119 | **132.99 / 132.87** | **38.55 / 38.34** | 6.94 / 6.29 |
| sheet scrolling, Liquid Glass controls **off** | >=119 | 16.82 / 17.34 | 2.58 / 2.41 | 12.70 / 12.94 |

The first four rows are at a forced 1280x2000 window, because the sheet under test (Home's "Bottom
pages" control sheet) only overflows in a shorter window. `frames / 3s` saturates at ~119 because
the gfxinfo ring buffer holds 120 frames, so ">=119" means "producing as fast as it can".

**An open sheet costs 124ms per frame while the user is doing nothing at all.** Scrolling it takes
that to 133ms — about 6% more. The scroll is not what makes it slow; the sheet is already slow, and
scrolling is merely when the eye expects motion and therefore notices.

At the native 1280x2856 window the same at-rest sheet measures **133.87 / 141.14ms total, 38.18 /
39.20ms RT** — so on the real geometry an open sheet sits at roughly 7fps, untouched.

## Four things that are not the cause

**Not Compose.** Every UI-thread stage stays trivial in the expensive case: input 0.00, animation
0.19, measure+layout **0.02**, record draw 0.18ms. This matches `hwui-frame-budget.md` exactly —
"optimising Compose here would buy nothing". Nothing about recomposition, `derivedStateOf`, or lazy
vs eager composition is in play, and the drag-arbitration reads of `contentScroll.canScrollUp`
happen inside `onPreScroll`, outside composition, so they are not causing per-pixel invalidation
either.

**Not the GPU.** `swap->completed` *falls* from ~12ms to ~7ms in the expensive case. The pipeline is
starved of frames rather than fill-bound.

**Not the sheet's own glass area.** Between the 1280x2000 window and native, the sheet's glass grows
from 1136x1437 to 1136x2586 — 1.8x the area — and RT moves only 36.2 -> 38.7ms, +7%. A cost
dominated by the sheet's own full-area blur would have scaled with the area. It does not.

**Not the scroll container.** `SheetContentColumn` is a `Column` + `verticalScroll` rather than a
lazy list, which is a real thing to fix eventually, but it cannot explain a cost that is already
fully present with no scrolling at all.

## Cost is set by how many glass controls are *composed*, not how many are visible

Varying the window changes both how much of the sheet is on screen and how big its glass is, while the
composed control count stays at nine. Baseline, at rest:

| window | switches in view | sheet glass | total p50 | RT p50 |
|---|---|---|---|---|
| 1280x1600 | 5 | 1136x1137 | 124.51 | 36.07 |
| 1280x2000 | 7 | 1136x1437 | 124.44 | 36.55 |
| 1280x2856 | 9 | 1136x2586 | 141.27 | 40.40 |

Sheet glass area grows 2.3x and visible switches 1.8x, but the frame cost moves only 1.12x. Nothing
that scales with area or with visibility can explain that; what is flat is the number of glass
consumers in the composition. Against the glass-off floor of 2.5ms, nine controls costing ~36ms puts
each one at roughly **3.7ms of RenderThread per frame, on screen or not**. Clipping is not culling —
the effect layer is recorded and rasterized either way.

## The cause: cost is linear in the number of glass consumers on screen

The decisive control is the last row of the table. Turning off Settings -> **Liquid Glass controls**
takes the identical journey from 133ms to 17ms, and RenderThread CPU from 38.4ms to 2.5ms — a 15x
reduction that makes the sheet scroll exactly as smoothly as a page.

That toggle does **not** disable the sheet's own glass. The app's own description says so ("ActionBar,
Sheet, and Dialog are always Liquid Glass"), and a screenshot with it off confirms it: the page
behind is still visibly blurred and refracted through the sheet surface. What it does disable is the
glass on the *controls*. So the sheet's one big blur is affordable; the controls inside it are not.

Counted on screen in that one sheet: **9 glass switches and 4 glass pills**, plus the close button
and four glass cards. Each `AppSwitch` is not one cheap element — it is

- a `Modifier.layerBackdrop(trackBackdrop)` **producer**, recording its track into a GraphicsLayer,
- plus a thumb `drawBackdrop` **consumer** sampling a `rememberCombinedBackdrop(backdrop,
  trackBackdrop)` — two sources, not one,
- with `vibrancy()` + `blur()` + `safeLiquidLens(..., chromaticAberration = true, depthEffect =
  true)`, an ambient highlight, a drop shadow and an inner shadow.

`chromaticAberration = true` is the most expensive refraction mode available, and it is on every
switch thumb in the app.

The scaling holds across surfaces, which is why this is not really a sheet bug:

| surface | total p50 | RT issue->swap p50 |
|---|---|---|
| Home (few glass elements) | 16.2 | 3.4 |
| Settings at rest | 16.4 | 11.3 |
| Settings **scrolling** | 65.9 | 19.7 |
| sheet | 124.5-141.1 | 36.1-39.2 |

Settings scrolling is ~15fps by the same mechanism. The sheet is simply the densest glass surface in
the app, so it is where the effect became impossible to miss.

## Why it redraws continuously even at rest

Something must be invalidating for an untouched sheet to produce 72 frames in three seconds. It is
not the sheet: Home *by itself* already produces >=119 frames in three seconds at rest. That is the
dynamic background driving `invalidateDraw` at its 60fps cap, already documented in
`hwui-frame-budget.md` — "the pipeline is already saturated before the user touches anything".

For contrast, MCP at rest produces almost nothing (20 rows of dump against Home's 140), confirming
that the continuous invalidation is Home's and not universal.

So the sheet does not create the redraw. It multiplies the cost of a redraw that was already
happening, from 16ms to 124ms, because every invalidation re-draws the whole glass stack above the
background.

## The library mechanism, from the AAR

Decompiled `io.github.kyant0:backdrop-android:2.0.0` (jadx), `DrawBackdropNode`:

`draw(ContentDrawScope)` runs, in order: `onDrawBehind`, `drawBackdropLayer`, `onDrawSurface`,
`drawContent()`, `onDrawFront`, and then — only when `exportedBackdrop != null` — a `recordLayer`
into the exported layer whose block invokes `onDrawBehind`, **`drawBackdropLayer` a second time**,
`onDrawSurface` and `onDrawFront` (notably *not* `drawContent`, which is what the docs mean by
"exportedBackdrop will skip drawing the content").

And `drawBackdropLayer` is not a cheap replay: it calls `recordLayer(...)` to **re-record** the
node's own effect layer every time, then `drawLayer`s it. The layer is forced to
`CompositingStrategy.Offscreen` in `layoutLayerBlock`, so it is a genuine offscreen rasterization —
the same cost named in `ba-first-entry-frame-floor`.

This is worth knowing, but note what the measurements say about it: the double record applies to the
**sheet surface** (which sets `exportedBackdrop`), and the sheet surface is demonstrably *not* the
dominant term. It is a real inefficiency, not the one to fix first.

## What I got wrong first

Reading the source before measuring, the obvious hypothesis was that the sheet's own large
`drawBackdrop` — sampling the scene, blurring and lensing an area covering 80% of the screen, and
doing it twice per frame because of `exportedBackdrop` — was the cost, with the content being a
descendant that re-triggers it. That hypothesis is wrong, disproved twice over: independently by the
glass-controls toggle (which leaves the sheet's blur fully on and still recovers 15x) and by the area
scaling (1.8x area, +7% cost). Recorded because it is a plausible-looking dead end that someone
reading `LiquidSheetSurface.kt` will arrive at again.

## Candidate fixes, none measured

In rough order of expected payoff per unit of risk. All of these need an A/B before being believed —
`backdrop-reduced-resolution.md` is the cautionary tale of a direction whose measured upper bound
turned out to be exactly zero.

1. **Make the switch thumb cheaper.** `chromaticAberration = true` on every thumb, over a
   two-source combined backdrop, is the most expensive configuration in the library applied to the
   most-repeated control in the app. Try `chromaticAberration = false`, and try whether the thumb
   needs to sample its own track at all rather than just the parent backdrop. This is a per-element
   constant on a term that multiplies by 13.
2. **Stop the idle invalidation reaching a covered stack.** While a modal sheet is up, the page
   behind it is not meaningfully visible — it is blurred beyond recognition. Freezing the scene
   backdrop for the duration of a modal presentation would cut the redraw rate to zero at rest.
   Note there is no existing mechanism for this: `SnapshotWindowBottomSheet` and
   `AppSnapshotFlowManager` are Compose `snapshotFlow` state plumbing, nothing to do with freezing a
   backdrop.
3. **Cap the number of simultaneously-glass controls.** A table of 13 glass elements is past the
   point where the material reads as material; the HIG line KeiOS already follows elsewhere is "use
   Liquid Glass effects sparingly", and "use the regular variant when components have a significant
   amount of text".
4. **Make `SheetContentColumn` lazy.** Correct regardless, and it bounds the count in 1 and 3, but
   on its own it fixes nothing here — the at-rest measurement proves the cost does not need scrolling.

Not a candidate: reducing backdrop capture resolution. Already measured at an upper bound of zero in
`backdrop-reduced-resolution.md`.

## Reproducing

Enable `debug.hwui.profile true`, restart the app, then for the at-rest case simply open Home's
"Bottom pages" sheet, `dumpsys gfxinfo os.kei reset`, wait three seconds without touching the
screen, and dump `framestats`. The at-rest measurement needs no window override and no gesture
scripting, which makes it the cheapest possible regression check for any of the fixes above.
