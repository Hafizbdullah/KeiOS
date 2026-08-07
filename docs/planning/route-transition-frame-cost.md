# Route transition frame cost

Where the frames go during a nav route push, measured on 5eea1f50 (120Hz, HyperOS, API 36).

## Why this needed a benchmark

`dumpsys gfxinfo <pkg> framestats` keeps a 120-frame ring. A route transition is 560ms — well
under 120 frames at 120Hz — so the ring always holds the transition *plus* whatever came before and
after it, in proportions that change run to run. Three attempts to read the transition out of
`gfxinfo` each caught a different mix and disagreed with each other.

`MainNavigationFrameBenchmarks` bounds the measurement with a trace section around the push and
another around the pop, so the reported percentiles describe the slide and nothing else.

## Running it

The macrobenchmark rules are disabled in the baseline-profile variant, and Gradle reinstalls the
app on every run — which resets HyperOS's `GET_INSTALLED_APPS` grant and puts
`com.miui.securitycenter/.InstalledPermissionDialog` on top of the app, so every UI tap lands on
the dialog and the journey times out. Install once by hand, clear the dialog once, then drive the
instrumentation directly:

```bash
adb -s 5eea1f50 install -r -g app/build/outputs/apk/nonMinifiedRelease/app-nonMinifiedRelease.apk
adb -s 5eea1f50 install -r -g baselineprofile/build/outputs/apk/nonMinifiedRelease/baselineprofile-nonMinifiedRelease.apk
# launch once, tap 始终允许 on any HyperOS dialog, then:
adb -s 5eea1f50 shell am instrument -w -r \
  -e class 'os.kei.baselineprofile.MainNavigationFrameBenchmarks#settingsRoutePushAndPop' \
  -e androidx.benchmark.enabledRules Macrobenchmark \
  -e targetAppId os.kei \
  -e additionalTestOutputDir /sdcard/Download \
  os.kei.baselineprofile/androidx.test.runner.AndroidJUnitRunner
```

`ANDROID_SERIAL` does confine a Gradle-driven run to one device, so the Gradle path works for the
baseline-profile journeys — it is only these UI-tapping macrobenchmarks that need the manual
install, because of the reinstall/permission interaction above.

## What the numbers say

Settings push, `CompilationMode.Partial(Require)`, 5 iterations:

| | P50 | P90 | P95 | P99 |
|---|---|---|---|---|
| `frameDurationCpuMs` | 10.7 | 24.0 | 27.8 | 44.5 |
| `frameOverrunMs` | -1.9 | 23.2 | 29.6 | 56.7 |

RenderThread slice breakdown over the same journey (median of 5):

| slice | sum | max |
|---|---|---|
| `DrawFrame` | 1346ms | 35.8ms |
| ├ `Drawing` | 1196ms | 34.0ms |
| ├ **`flush layers`** | **674ms** | **28.4ms** |
| └ `flush commands` | 178ms | 2.9ms |
| `prepareTree` | 125ms | 2.2ms |
| `syncFrameState` | 130ms | 2.3ms |
| `Compose:recompose` | 153ms | **3.3ms** |

Three things follow directly.

**It is not composition.** `Compose:recompose` never exceeds 3.3ms in a frame. Every long frame is
long on the RenderThread.

**It is offscreen-layer rasterization.** `flush layers` is half of `DrawFrame`'s total and 79% of
its worst frame. That is HWUI rasterizing the offscreen RenderNode layers Liquid Glass needs — and
during a transition there are two full-screen pages' worth of them on screen at once.

**It is not a cold-start effect.** The second push in the same process measures 9.6 / 23.3 / 24.9 /
38.9 against the first push's 10.7 / 24.0 / 27.8 / 44.5. Within noise. No amount of baseline-profile
or pre-warm work can reach this cost, because there is nothing cold about it.

## Candidates measured and rejected

**`enableCornerClip = false`.** `flush layers` 674 → 628ms (-7%), `DrawFrame` 1346 → 1253ms (-7%),
overrun P90 23.2 → 20.3. Real but small, and it costs the rounded corners on both layers for the
whole slide. Rejected: the visual is worth more than 7% of RenderThread time.

An earlier attempt to A/B this through `gfxinfo` returned p50 13 → 22 across two identical runs and
was abandoned as inconclusive. It was not the change that was unmeasurable, it was the instrument.

**Closing the covered page's backdrop gate synchronously at push** (`backStack.size <= 1` instead of
waiting for the lifecycle to leave RESUMED). Overrun P90 23.2 → 16.4 and P95 29.6 → 23.6, but P50
went -1.9 → **+4.4** — the median frame stops making its deadline — and the RenderThread sums moved
the wrong way (`flush layers` 674 → 710ms). Trading the median for the tail is the wrong trade for
perceived smoothness. Rejected.

## Where this leaves it

The remaining cost is two Liquid Glass pages compositing at once, which is what the transition
is. Reducing it means reducing the material — suppressing the covered page's glass while it
parallaxes under the 0.54 scrim is the obvious lever, and it is exactly the visual downgrade this
work is not allowed to spend. Anyone picking this up should start by re-running the numbers above,
not by re-running the two rejected experiments.
