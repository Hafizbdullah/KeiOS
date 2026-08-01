# Main navigation performance audit — 2026-07-30

## Scope and acceptance boundary

This audit investigates the HWUI regression observed after the 1.11.0 release,
with emphasis on Liquid Glass, Backdrop, the retained main pager, miuix-nav route
transitions, and edge-stacked cards.

The physical Xiaomi device supplies the problem baseline. Iteration, builds,
traces, frame benchmarks, and visual checks run on the single visible Android 17
AVD (`KeiOS_API37_Validation`, `emulator-5554`). A final physical-device
`benchRelease` pass remains the release acceptance gate.

Visual fidelity is a hard constraint. The fixes preserve blur, refraction,
highlight, shadow, animation duration, page-switch semantics, sheet detents, and
Toast entry/exit behavior.

## Physical-device problem baseline

Device: Xiaomi `25098PN5AC`, Android 16 / API 36, 1220 × 2656, density 520.

Measured interaction:

1. Home → GitHub → MCP → Home.
2. Repeat five times.

Observed result:

| Metric | Baseline |
| --- | ---: |
| Total frames | 742 |
| Modern jank | 0 |
| Legacy jank | 736 / 99.19% |
| CPU P50 / P90 / P95 / P99 | 48 / 85 / 97 / 105 ms |
| GPU P50 / P90 / P95 | 8 / 16 / 19 ms |
| Scratch `GrVkTextureRenderTarget` | 508.31 MB / 745 entries |
| Total GPU memory | 610.07 MB |

The low GPU frame percentiles and very large RenderTarget cache point to
RenderNode submission, Backdrop captures, and offscreen raster work rather than
shader execution alone.

## Android 17 AVD benchmark

The benchmark module now contains four `benchmarkRelease` Macrobenchmarks:

- `homeRestingDynamicBackground`: three seconds of the complete animated Home
  background and HDR sweep.
- `homeScrollWithFullEffects`: repeated Home scrolling with every visual effect
  enabled.
- `homeGitHubMcpHome`: Home → GitHub → MCP → Home.
- `mcpStackedCardsScroll`: three upward and three downward MCP list swipes.

All four use:

- `FrameTimingMetric`
- `CompilationMode.Partial(BaselineProfileMode.Require)`
- warm startup
- five iterations
- R8-enabled `benchmarkRelease`

The AVD is not CPU-locked and remains a relative signal. Emulator suppression is
passed only on the command line; the project configuration still rejects
emulator performance results by default.

After the code-level comparisons were complete, the persistent AVD profile was
raised from 4 CPU cores / 2 GB / automatic graphics to 6 CPU cores / 6 GB /
explicit host graphics. A cold boot confirmed gfxstream, Metal-backed GLES,
MoltenVK on Apple M2 Pro, and a 512 MB configured VM heap (resolved to 576 MB by
the Pixel 10 Pro device profile). Measurements across that configuration
boundary are treated as separate diagnostic baselines.

## Perfetto diagnosis

The initial navigation trace recorded 106 app frames, including 77 missed app
frames and 74 `App Deadline Missed` frames.

| Work | Initial trace |
| --- | ---: |
| Main thread `Running` | 265.18 ms |
| RenderThread `Running` | 2817.83 ms |
| RenderThread `Drawing full screen` | 2887.13 ms total / 28.87 ms average |
| Main thread `postAndWait` | 2723.59 ms total / 27.24 ms average |
| RenderThread `flush commands` | 1269.34 ms total |
| RenderThread `flush layers` | 979.55 ms total / 10.00 ms average |
| Ganesh `FillRectOp` | 82,522 |

Compose recomposition accounted for a much smaller share:
`Recomposer:recompose` was approximately 83.56 ms and `Compose:recompose`
approximately 68.72 ms.

The main thread is mostly waiting for RenderThread. Native raster, layer flush,
and full-screen Backdrop capture are the primary optimization targets.

## Implemented changes

### 1. Sheet Backdrop producers are demand-driven

GitHub and MCP previously retained separate full-page content and Sheet
producers while every Sheet was closed. The content and Sheet identities now
collapse while no Sheet is visible, then separate in the same composition that
opens a Sheet.

The GitHub visibility gate includes all overview, strategy, check-logic,
Droid-source, debug, Actions, add/edit, artifact, APK information, install
confirmation, F-Droid, and decision-assist Sheet requests.

### 2. Liquid Toast producer follows the live Toast stack

The NavDisplay previously lived inside a permanent full-screen Toast Backdrop
producer. `LiquidToastState.isVisible` now tracks live Toast slots.

The producer is attached before a visible Toast consumer and remains attached
through its exit animation. It detaches after the final Toast slot leaves.

### 3. Retained inactive pages release full-page producers

The custom main pager intentionally keeps Home, OS, BA, MCP, and GitHub
compositions alive to preserve state. OS, BA, MCP, and GitHub also created
full-size content and Sheet producers inside those retained compositions.

`MainPageContentBackdropScene` now accepts `producerActive`. Each page connects
it to `MainPageRuntime.isPageActive`, which is true for the settled and target
pages during a transition. Inactive retained pages keep their state and data
contracts while detaching their full-page producers.

The target page restores its producer in the same runtime-state update that
starts the transition. Outgoing and incoming glass therefore remain available
during page motion.

### 4. Resting edge-stacked cards avoid a dedicated transform layer

The previous resting fast path reset a permanently allocated `graphicsLayer` to
identity for every visible card. The card modifier now uses normal placement
below the stack line and `placeRelativeWithLayer` only for cards actively
entering the top pile.

The translation, scale, alpha, transform origin, disposal fade curve, and stack
depth formulas are unchanged. Scroll-position reads remain in the layout
placement phase and do not invalidate composition.

### 5. Benchmark permission setup is deterministic

The Macrobenchmark setup grants the target package notification and local
network runtime permissions before launch. A freshly installed benchmark APK
therefore reaches `home_page_root` without permission-sheet timeouts.

### 6. Passive surfaces omit zero-contribution shadow nodes

`LiquidSurface` now passes no outer-shadow producer when shadow rendering is
disabled or alpha resolves to zero. It also omits the interactive inner-shadow
producer for passive or disabled surfaces. Visible outer shadows, press-driven
inner shadows, blur, refraction, highlight, border, content clipping, and the
enabled-state composition boundary keep their existing formulas and timing.

### 7. Overview pills share one Backdrop draw pass

Home, GitHub, and OS overview pills opt into a batched Liquid Glass renderer.
The custom flow layout preserves the 28 dp height, horizontal and vertical
rhythm, RTL placement, typography, color, border, blur, vibrancy, lens
refraction, and highlight. Up to 24 pill bounds are supplied to one lens and
highlight shader, replacing one Backdrop consumer per pill with one consumer
for the complete flow. Empty, unsupported, and oversized cases fall back to the
original `FlowRow` implementation.

The isolated Home overview experiment reduced its selected AVD rendering signal
by approximately 59%. This is a component-level relative result; complete frame
timing remains the release acceptance metric.

### 8. Pager state is visible in Perfetto

The runtime now emits counters for current, target, and settled page indices,
scroll state, programmatic navigation, active navigation, active page Backdrop
producer count, and full-effect page count. The existing
`PerformanceMetricsState` values remain available for JankStats while Perfetto
can align the real pager window with RenderThread `Drawing`, `flush commands`,
and `flush layers` slices.

## Measured result

Navigation frame timing:

| Metric | Initial AVD | After retained-producer gating | Change |
| --- | ---: | ---: | ---: |
| Frame count median | 71 | 72 | +1 |
| CPU P50 | 55.43 ms | 56.75 ms | within AVD noise |
| CPU P90 | 92.74 ms | 89.43 ms | -3.6% |
| CPU P95 | 126.60 ms | 109.27 ms | -13.7% |
| CPU P99 | 165.80 ms | 149.13 ms | -10.1% |
| Overrun P50 | 62.85 ms | 64.94 ms | within AVD noise |
| Overrun P90 | 123.70 ms | 122.78 ms | -0.7% |
| Overrun P95 | 159.10 ms | 140.95 ms | -11.4% |
| Overrun P99 | 188.05 ms | 172.52 ms | -8.3% |

In the optimized iteration-zero trace, `flush layers` was approximately
7.96 ms per submitted frame, compared with approximately 9.80 ms per frame in
the initial trace.

After the AVD was left available for visual review, the same
`benchmarkRelease` suite was run again on `KeiOS_API37_Validation` with
`androidx.benchmark.suppressErrors=EMULATOR`. The run completed successfully;
the suppression only converts the expected emulator-environment warning into a
warning so FrameTiming data can be collected.

| Benchmark | Frame count median | CPU P50 / P90 / P95 / P99 |
| --- | ---: | ---: |
| `homeGitHubMcpHome` | 51 | 59.07 / 96.85 / 131.65 / 155.73 ms |
| `mcpStackedCardsScroll` | 235 | 16.93 / 50.00 / 58.77 / 67.78 ms |

Raw result:
`baselineprofile/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/KeiOS_API37_Validation(AVD) - 17/os.kei.baselineprofile-benchmarkData.json`

This run is a fresh AVD signal rather than a physical-device acceptance
claim. Its navigation tail is higher than the previous AVD run, while the
scroll distribution is lower; the variation reinforces the decision to use
the Xiaomi device for final performance sign-off.

The dedicated scroll benchmark showed substantial AVD variance. Its repeated
optimized run measured CPU P50/P90/P95/P99 of
21.72/53.50/62.22/73.58 ms. The old permanent stack-layer run measured
24.52/52.58/61.77/75.60 ms. This supports visual safety and lower median work,
while the tail difference remains too small for a standalone performance claim.

### Exact v1.11.8 comparison

The reference APK was verified as version `1.11.8` (`11108999`) with certificate
SHA-256 `c5c4c43ba6d03268773122bc99d1e870908609ae5251c7c8ad4b6029b573c82a`.
Both builds were measured with R8, required Baseline Profiles, the same AVD
profile, and five iterations before the later AVD resource change.

Home dynamic resting:

| Build | CPU P50 / P90 / P95 / P99 | Overrun P50 / P90 / P95 / P99 |
| --- | ---: | ---: |
| v1.11.8 | 20.4 / 33.7 / 34.2 / 40.6 ms | 4.9 / 20.5 / 21.0 / 29.7 ms |
| Current stable source | 19.7 / 32.5 / 33.2 / 34.7 ms | 4.3 / 21.0 / 21.8 / 22.8 ms |

Home → GitHub → MCP → Home:

| Build | CPU P50 / P90 / P95 / P99 | Overrun P50 / P90 / P95 / P99 |
| --- | ---: | ---: |
| v1.11.8 | 32.4 / 60.0 / 77.7 / 111.0 ms | 22.6 / 59.9 / 74.5 / 102.7 ms |
| Current stable source | 21.0 / 60.8 / 79.1 / 92.2 ms | 5.3 / 56.4 / 90.3 / 106.5 ms |

The current build improves Home and the transition median while preserving all
visual material and animation paths. Transition-tail values still vary enough
to require the Xiaomi acceptance pass.

RenderThread decomposition explains the structural tradeoff:

| Build | Drawing | `eglSwapBuffers` | `flush commands` | `flush layers` | `drawLayer` calls/frame |
| --- | ---: | ---: | ---: | ---: | ---: |
| v1.11.8 | 16.380 ms/frame | 13.760 ms/frame | 1.449 ms/frame | 0.541 ms/frame | 8 |
| Current stable source | 16.234 ms/frame | 9.412 ms/frame | 3.738 ms/frame | 1.768 ms/frame | 29 |

The newer material system performs more layer and flush work while spending
less time waiting in buffer swap; total Drawing is slightly lower. Layer count
alone therefore remains an insufficient retention criterion.

With the permanent 6-core / 6-GB / host-GPU AVD profile, the current batched
build measured Home CPU P50/P90/P95/P99 of
`19.9/33.5/35.7/38.4 ms` and overrun P50/P90/P95/P99 of
`4.5/20.9/23.8/27.1 ms`. This establishes the new configuration baseline rather
than a cross-configuration performance claim.

## Rejected experiment

Removing the enabled-state identity `graphicsLayer` from the shared
`LiquidSurface` looked structurally attractive because Backdrop already owns an
offscreen layer. Two AVD scroll measurements showed a consistent regression:

| Build | CPU P50 / P90 / P95 / P99 |
| --- | ---: |
| Current stack-layer optimization | 21.72 / 53.50 / 62.22 / 73.58 ms |
| Identity-layer removal, first run | 22.80 / 57.10 / 67.77 / 93.55 ms |
| Identity-layer removal, repeat | 25.22 / 61.56 / 73.44 / 99.95 ms |

The existing layer acts as a useful composition boundary around Backdrop and
card content on this renderer. The experiment was fully reverted; the shared
surface implementation and visual behavior remain unchanged.

Gating the four retained-page `topBarBackdrop` producers by
`isPageActive` was measured twice after an AVD restart. CPU P95/P99 were
`132.66/156.53 ms` and `129.82/166.39 ms`, compared with the
`109.27/149.13 ms` retained-producer baseline. That producer boundary is
also reverted; top-bar sampling remains continuously available for the active
composition contract.

Four additional single-variable experiments were measured and reverted:

- A local HDR transparent-period `saveLayer` did not reduce the retained
  RenderThread layer work.
- Specializing the AGSL lens shader to the active pill count did not reduce the
  real shader/layer submission cost.
- Removing the Home producer's two fallback fills did not improve tail latency.
- Merging the Hero transform layer with the HDR offscreen layer changed layer
  ownership without a measurable RenderThread gain.

Each revert restores the accepted visual result and keeps the optimized source
at an independently measurable checkpoint.

## Backdrop tooling boundary

The Backdrop MCP server was requested during this audit and was not exposed in
the active tool catalog. Diagnosis therefore used the project-locked Backdrop
2.0.0 source, runtime trace counters, R8 Macrobenchmark, Perfetto, and visual
screenshots. The missing MCP surface is recorded so future runs can repeat the
analysis with it when available.

## GPU cache interpretation

After visiting all main pages, both the temporary always-active producer build
and the optimized build reported 279 Scratch RenderTarget cache entries and
153.31 MB total GPU cache on the AVD. Skia retains released scratch targets for
reuse, so `dumpsys gfxinfo` cache totals do not immediately fall when a
producer detaches.

The runtime trace still shows fewer layer-flush costs and lower high-percentile
frame duration. Physical-device peak cache growth remains an explicit final
acceptance measurement.

## Visual validation

The optimized `benchmarkRelease` was validated on the visible
`KeiOS_API37_Validation` AVD and installed on Xiaomi `25098PN5AC` at source
commit `ca4cb236d`. The physical package is R8-minified, profileable,
`debuggable=false`, and signed with the same release certificate as the
previously installed build.

Validated in dark mode:

- Home, GitHub, MCP, OS, and BA page switching.
- MCP stacked-card scroll in both directions.
- GitHub add-tracking Sheet with independent glass background sampling.
- MCP edit-service Sheet.
- Liquid Toast first visible frame, glass material, and completed exit.
- Bottom dock, page overview anchors, card shape, shadow, alpha, and stack order.

The physical-device smoke check used the user's enabled Apple launcher alias,
confirmed `os.kei/.LauncherAppleDesigns` as the resumed activity, and showed the
complete light-mode Home surface at 1220 × 2656 / density 520. No process-local
fatal exception or ANR appeared after launch. Manual scrolling, transitions,
HWUI bars, thermal behavior, and repeated-loop timing remain with the user for
interactive acceptance.

Saved local evidence lives under
`artifacts/performance-audit-2026-07-30/`,
`artifacts/performance-audit-2026-08-01-home-segmented/`, and
`artifacts/performance-physical-2026-08-01/`.

## Remaining acceptance work

1. The targeted backdrop and Liquid Glass unit tests pass. The full app suite
   still contains the pre-existing `GitHubDetailInfoRowReuseTest` contract
   failure, and `lintDebug` still reports the project backlog; neither was
   expanded by this performance patch.
2. The signed `benchmarkRelease` is installed on Xiaomi `25098PN5AC` for user
   review. Repeat the five-loop navigation scenario and capture frame timing,
   Perfetto, GPU memory, HWUI bars, and a visual recording.
3. Treat the physical-device run as the release decision. The fixed AVD remains
   the fast, reproducible diagnostic loop for the next single-variable change.
