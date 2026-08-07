# Where the frame time goes

Measured on 5eea1f50 (1220x2656, 120Hz LTPO, HyperOS) with Developer options ->
Profile HWUI rendering -> "In adb shell dumpsys gfxinfo". Harness: `scripts/perf/`.

## Reading the instrument

`dumpsys gfxinfo` reports two things that look authoritative and are not. Across three
back-to-back runs of one unchanged build, `Janky frames (legacy)` came out 9.6% / 11.8% / 59.0%,
and `99th gpu percentile` alternates between a real value and the 4950ms overflow bucket. Neither
can support an A/B.

The CPU and GPU **percentiles** are stable to +/-1-2ms across the same runs. Use only those.

The per-frame `PROFILEDATA` block carries 24 columns, not the 14 that older docs describe, and
`Flags` is routinely 32 rather than 0 — filtering on `Flags == 0` silently discards every frame.
`scripts/perf/frame_stages.py` parses by header name and does not filter.

## The stage that matters is not the one the totals suggest

Per-frame decomposition, p50, from the columns rather than the summary:

| journey | total | start_delay | ui_work | sync_wait | rt_cpu | gpu |
|---|---|---|---|---|---|---|
| home dwell (no input) | 20.71 | 0.43 | 0.42 | 0.10 | 8.62 | 11.08 |
| home scroll | 13.52 | 0.29 | 1.25 | 0.04 | 4.74 | 6.54 |
| section switch | 19.25 | 0.38 | 0.45 | 0.10 | 7.52 | 10.58 |
| route push | 12.33 | 0.25 | 0.45 | 0.03 | 4.73 | 6.07 |

`start_delay` is IntendedVsync -> HandleInputStart; `ui_work` is HandleInputStart -> SyncQueued
(input, animation, measure, layout, record draw — all of Compose); `rt_cpu` is
SyncStart -> CommandSubmissionCompleted; `gpu` is CommandSubmissionCompleted -> GpuCompleted.

**The UI thread is not the problem.** Everything Compose does — recomposition, measure, layout,
recording draw commands — totals 0.4-1.3ms. Measure+layout alone is consistently under 0.1ms.
Optimising Compose here would buy nothing.

The cost is entirely RenderThread CPU plus GPU. `swap->completed` equals `gpu` exactly, and
`DequeueBufferDuration` is ~0.01ms, so there is no buffer starvation: the app is doing the work,
not waiting for a buffer.

## Per-page steady state

| page | total p50 | rt_cpu | gpu |
|---|---|---|---|
| home | 13.07 | 3.94 | 6.54 |
| github | 11.70 | 3.55 | 6.13 |
| mcp | 13.13 | 3.84 | 5.12 |
| os | 14.12 | 3.77 | 5.15 |
| ba | 33.23 | 9.61 | 4.69 |

BA looks like the outlier and mostly is not. Its list holds three cards, which do not fill the
screen, so a swipe lands in overscroll rather than scrolling — the stretch is what costs 33ms.
**Left alone, BA renders zero frames in a three-second dwell**, which is the best result of any
page: nothing on it redraws when nothing changes.

Home is the opposite. Idle, untouched, it produces ~40 frames per second at 20.71ms each. That is
the dynamic background driving `invalidateDraw` at its 60fps cap, and every invalidation re-blurs
the whole Liquid Glass stack above it. The shader itself is cheap — it already renders at
`DYNAMIC_BACKGROUND_RENDER_SCALE = 0.25f` and upscales — but the re-blur it forces is not.

The consequence worth naming: **the pipeline is already saturated before the user touches
anything.** Every interaction starts from a full queue.

## Measured and rejected

**`BG_EFFECT_HIGH_FPS` 60 -> 30.** The obvious lever, and it makes things worse, not better:

| | 60fps (shipped) | 30fps |
|---|---|---|
| home idle total p50 | 20.71 | **23.52** |
| home idle gpu p50 | 11.08 | **13.34** |
| section switch `Slow issue draw commands` | 4-5 | **25-26** |
| section switch `Frame deadline missed` | 1-2 | **40-79** |

Consistent across runs. Halving the invalidation rate does not halve the work — it spaces
redraws far enough apart that each one arrives colder, and the irregular cadence fights the LTPO
panel. The cost is not per-frame overhead that fewer frames would avoid; it is that each frame is
expensive. Do not retry this as a frame-time fix.

## What is left

The remaining cost is full-screen Liquid Glass blur re-running whenever the background drifts.
Making it cheaper means either fewer glass surfaces sampling a full-screen backdrop, or sampling
a lower-resolution one — the second is appearance-neutral in principle, because a blur discards
the detail a full-resolution capture preserves, but it needs support from the Backdrop library
rather than a change here.

There is no free frame-time win in the paths measured above. Anyone picking this up should start
by reproducing the table in "The stage that matters", not by re-running the rejected experiment.

## Why Home reads differently at different moments

The panel is LTPO and moves between 120Hz and 60Hz on its own. Home idle, measured twice on the
same build, with nothing changed but when the sample was taken:

| Home idle | panel | vsync budget | frame cost p50 | gpu p50 | over deadline |
|---|---|---|---|---|---|
| within ~1s of a scroll | 120Hz | 8.31ms | 19.65 | 11.27 | 100% |
| ~10s after last touch | 60Hz | 16.62ms | 21.29 | 11.25 | 100% |

The work is the same — GPU 11.27 vs 11.25. What changes is the deadline it is scored against, and
the panel crosses between the two a second or so after the last touch. `dumpsys gfxinfo`
accumulates both regimes into one jank figure, so the same build reads well or badly depending on
how the app was being used while the counter was open. Six consecutive settled samples, by
contrast, land within 20.85-22.15ms — the variance is not in the app.

The part that is a real problem: **Home misses the deadline 100% of the time in both regimes.**
A ~20ms frame does not fit an 8.31ms budget and does not fit a 16.62ms one either. Home never
keeps up while its background animates. That is the full-screen glass blur, deferred to a separate
Backdrop investigation.

When comparing builds, reset the counter and drive a fixed journey (`scripts/perf/`) rather than
reading the accumulated figure — otherwise the refresh-rate mix is the variable, not the change.

## Switching into BA

Ranked by cost of Home -> tab, three passes pooled, every tab a first entry:

| switch to | total p50 | p90 | p99 | ui p99 | rt p99 | gpu p99 |
|---|---|---|---|---|---|---|
| github | 17.04 | 33.90 | 48.44 | 24.01 | 22.49 | 28.63 |
| mcp | 18.48 | 44.75 | 59.34 | 25.28 | 38.84 | 31.36 |
| os | 15.34 | 45.96 | 76.96 | 19.44 | 18.46 | 37.99 |
| **ba** | **15.26** | **58.79** | **83.76** | **38.38** | **57.72** | 26.81 |

BA's median is the *best* of the four and its GPU p99 is the *lowest*. The entire complaint is the
tail, and the tail is first activation. Bouncing Home <-> BA repeatedly:

| Home -> BA | p50 | p90 | p99 | ui p99 | rt p99 |
|---|---|---|---|---|---|
| entry #1 | 13.13 | 55.78 | 74.65 | 32.15 | 62.63 |
| entry #2 | 12.74 | 41.26 | 55.78 | 14.32 | 35.94 |
| entry #3 | 12.84 | 43.19 | 59.20 | 11.81 | 36.68 |
| entry #4 | 13.19 | 38.06 | 56.48 | 13.93 | 29.80 |

`MainPageActivationState.hasActivated` keeps a page composed once it has been reached, so a page
composes exactly once per process — which is why only entry #1 pays. First entry costs ~19ms of
extra UI-thread work (composing BA's tree) and ~26ms of extra RenderThread (first rasterization of
its glass layers) over a repeat.

### The candidate worth trying

`MainPageActivationState` marks a page activated from two `LaunchedEffect`s: when it becomes the
settled page, and when it is the scroll target *while `isScrollInProgress` is true*. A tab tap
reaches the second one, so BA's tree composes on the **first frame of the switch animation** —
the worst possible frame to spend 19ms on.

`MainPagerTabJumpController.onPageSelected` already knows the target index synchronously, at tap
time, before `animateToPage` is launched. Marking the target activated there would move that
composition into the touch-response window, ahead of any motion. Same work, same tap, one frame
earlier.

This is **not** the layer pre-warm recorded in the BA first-entry notes, which rendered glass off
the click path and made the other half worse. Nothing is rendered early here; only the ordering of
composition versus the start of the animation changes. It does need care: `activationState` is
built after the coordinator in `MainPagerLayout`, so the target index has to be threaded out of
`MainPagerTabJumpControllerState` first.
