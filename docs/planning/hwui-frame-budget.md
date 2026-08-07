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
