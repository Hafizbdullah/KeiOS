# Liquid component inventory

> Scanned: 2026-08-09, at `fb6aa0b02`.
> Purpose: track which Liquid components have been rebuilt and which have not, so the remaining work
> can be planned rather than rediscovered.

**94 files** in `ui-liquid-glass`, plus **13** more Liquid surfaces defined in `:app`. Grouped by what
they are as components rather than by file: roughly **48 component kinds** plus ~20 token, material and
infrastructure units.

The rebuild campaign is a three-day window, **2026-08-07 → 08-09**. It touched **39 of 94** module
files, but 6 of those were a single narrow pass, so **33 were genuinely rebuilt** and **~55 remain
untouched**.

To refresh the dates in this table:

```bash
for f in $(find ui-liquid-glass/src/main -name '*.kt' | sort); do printf '%s  %s\n' "$(git log -1 --format=%ad --date=short -- "$f")" "$f"; done | sort -r
```

## Rebuilt or rewritten (2026-08-07 → 08-09)

| Component | Files | What was done |
|---|---|---|
| Toolbar / action bar | `LiquidToolbar`, `LiquidActionBarStyle`, `AppLiquidNavigationButton` | rebuilt as an actual toolbar |
| Bottom bar | `LiquidGlassBottomBar` | press-theft fix only; material **not** rebuilt |
| Floating dock surface | `AppFloatingLiquidDockSurface`, `GlassChromeGestures` | glass follows the finger again |
| Bottom sheet | `LiquidSheet`, `LiquidSheetLayout`, `LiquidSheetChrome`, `LiquidSheetScrim`, `LiquidSheetSurface`, `LiquidGlassBottomSheet`, `SceneBackdropScope` | full in-window rewrite; miuix window machinery deleted |
| Sheet content vocabulary | `SheetStyles`, `SheetContentColumn`, `SheetCardOptics` | reworked onto the new sheet |
| Alert | `LiquidAlert` (+ `LiquidGlassDialog` collapsed into it) | rebuilt on real glass |
| Action sheet | `LiquidActionSheet` | **new** |
| Modal plumbing | `LiquidModalPresentation`, `LiquidModalSurface`, `LiquidPresentationAction` | new shared host |
| Toast | `LiquidToastHost`, `LiquidToastState`, `LiquidToastSurface` | full rewrite, split out of one file |
| Overlay host / portal | `LiquidOverlayHost` | **new**, presentation + notification layers |
| Shared presentation material | `LiquidPresentationMaterial` | **new**, extracted from four verbatim copies |
| Dropdown | `LiquidGlassDropdown` | 690 → 348 lines, hosted in-window |
| Action menu | `LiquidGlassActionMenu`, `LiquidMenuSurface`, `LiquidMenuPresentation`, `LiquidMenuLayout` | rewritten, plus Apple's Small/Medium/Large layouts |
| Snapshot popup adapters | `MiuixSnapshotAdapters` | 717 → ~375 lines |
| Stacked cards | `AppEdgeStackedCards` | rewritten twice (`e4b722136`, `fb6aa0b02`) |
| Card surfaces | `AppSurfaceBox`, `AppFeatureCards`, `BaLiquidSurfaces`, `GuideLiquidCard` | stack slot; BA glass restored |
| Core liquid surface | `LiquidSurfaces` | gained the card-pile plumbing |
| Unsaved-changes confirm | `UnsavedSheetDismiss` | moved onto the action sheet |

## Touched narrowly, not rebuilt

`53eb6510b` (08-07) only dropped identity `graphicsLayer` nodes — 5-20 line diffs, and its own message
records it as "a simplification, not a perf win". Treat these as untouched when planning.

`AppLiquidButtons` · `AppSwitch` · `LiquidSliderVariants` · `AppFloatingSearchDock` · `AppInteractiveTokens`

## Not touched

| Last touched | Components |
|---|---|
| 2026-07-30 | **Dropdown items** `LiquidGlassDropdownItems` — the rows inside the rewritten dropdown |
| 2026-07-29 | **Expandable / accordion cards** `AppLiquidExpandableCards` |
| 2026-07-17 | **Status pills** `StatusPill`, `StatusIconPill` · **Info block** `LiquidInfoBlock` · **Text input** `AppTextInputContent` · **Search field** `AppLiquidSearchField` · **Standalone buttons** `AppStandaloneLiquidButtons` · **Dropdown selector controls** `AppDropdownControls` · **Card headers** `AppCardHeaders` · **Icon actions** `AppIconActions` · **Supporting blocks** `AppStatusPrimitives` · `LiquidBackdropWindowBoundary` |
| 2026-07-16 | **Progress bars** `LiquidProgressBars` · **Checkbox** `AppLiquidCheckbox` · **Dialog action buttons** `AppLiquidDialogActions` · **Bottom bar material** `LiquidGlassBottomBarMaterial` · **Overview cards** `AppOverviewCards` · **Shell panel** `ShellLiquidPanelSurface` · `GlassEffectRuntime` · `AppThemeAppearance` |
| 2026-07-13 | **Badges** `AppLiquidBadges` · **Grip-aware dock** `AppGripAwareDock`, `AppGripAwareDockState` · **Floating action button** `AppFloatingLiquidActionButton` · **Search material** `AppLiquidSearchMaterial` · `AppToastBridge` · `InteractiveHighlight` · foundations `GlassStyle`, `LiquidGlassShaders`, `BackdropLensSafety`, `LiquidFiniteValues`, `GlassContentContrast` |
| ≤ 2026-07-01 | `SheetSurfaceColors` · `AppSquircle` · `ChromePixelSnapping` · `AppFloatingSearchDockMotion`, `AppFloatingDockMetrics` · module-extraction era (06-01): `AppMotionTokens`, `AppExpandTransitions`, `UiPerformanceBudget`, `LiquidSliderInteractionLock`, `CardLayoutRhythm`, `AppTypographyTokens`, `AppControlRows`, `AppCardBodyLayouts`, `AppWindowMetrics`, `AppSearchBehavior`, `AppStatusColors` |
| `:app` surfaces | BGM mini-player, track list and hero visuals · `GuideProfileUi` · GitHub tracked-item info and health cards · `AppOverviewPillBatch` · `HomeOverviewGlassBatch` · `DebugLiquidCatalogSamples` |

## The three root causes the campaign kept finding

Each one was silent: it made configured optical values dead code without any visible error. Worth
checking against any component before rebuilding it.

**1. Glass hosted in a real platform window.** `LiquidBackdropWindowBoundary` blanks `LocalSceneBackdrop`
to `emptyBackdrop()` across a window boundary, so a blur there draws nothing and the surface silently
takes its opaque fallback. Cost when found on menus: 20 configured values dead.

*Status: essentially closed.* `AppWindowDialogHost` routes `Card` presentations to `LiquidAlert`; only
three call sites still open a raw `Dialog(` — both gallery-fullscreen viewers and a debug card — and a
fullscreen viewer has no page behind it to sample, so that is correct. `GitHubShareImportWindowChrome`
(last touched 2026-05-18) is the one still worth checking.

**2. A transform applied outside `drawBackdrop`.** `LayerBackdrop.drawBackdrop` inverse-transforms the
sampled backdrop only by the `layerBlock` it is handed; anything applied around the modifier falls into
the library's `// TODO: outer transformations lead to wrong position calculation` path and the
refraction slides. Note `InverseLayerScope.inverseTransformAtTopLeft` reads only `rotationZ`, `scaleX`
and `scaleY` and inverts about the top-left — it ignores `transformOrigin`, so any pivot has to be
expressed as a translation.

*Remaining candidates:* `AppFloatingSearchDockMotion` + `AppFloatingSearchDock`,
`LiquidGlassDropdownItems`, `AppLiquidExpandableCards`, `AppIconActions`.

**3. A plain `alpha` on a surface that owns a backdrop shadow.** Promotes the layer to an offscreen
buffer — the expensive kind on a surface that also draws a blurred backdrop. `presentationFade`
(`CompositingStrategy.ModulateAlpha`) is the family-wide answer.

**4. A shadow sized for the wrong surface.** *Fixed for the LiquidSurface family; see below.*

## The square-cornered shadow

Diagnosed and fixed across `004bb8f9b` and its follow-up. It was **geometry, not clipping of the ring by
an enclosing layer** — the first theory — and then, on a second report, **the container clip after all**,
just not where that theory put it. Both wrong turns are recorded because each looked convincing.

`Shadow.Default` is a fixed **24dp blur with a 4dp drop**, and `ShadowNode` spreads `radius * 2` in every
direction — a ring 48dp past its surface. That is:

- **Four times the size of a 30dp checkbox.** A blurred silhouette that large keeps no visible corner
  rounding, so what shows under a rounded corner is a right angle.
- **Far enough on a card to reach the enclosing scroll container's clip**, which bounds it on the scroll
  axis only. Measured on the BA account card: the ring stepped 247 → 242 *in one pixel* at the card's top
  edge, where a 24dp blur should have faded in over ~20dp, and cut dead again at the bottom edge, while
  still spilling sideways. Every clip leaves a straight edge; a straight edge beside a rounded corner is
  the artifact.

Two fixes were tried and measured before the third:

| attempt | result |
|---|---|
| tighten the blur to 10dp | wedge shrank (step 5 → 3 levels) but survived |
| drop the shadow fully downward so nothing spreads up | moved the wedge to the **bottom** corners and made it worse (230 vs 247), and below the card it rendered nothing at all |
| **no outer drop shadow on the card family** | both corners flat 247 — clean |

So `LiquidSurface.shadow` and `AppSurfaceBox.shadow` now default to **false**, with `BaLiquidSurfaces`,
`GuideLiquidCard`, `GitHubActionsPrimitives`, `LiquidRoundedCard` and the sheet choice cards following.
Nothing of value is lost: the ring rendered nothing below a card anyway, and these cards separate from the
page by being brighter than it, with the rim highlight carrying the edge. Turn it back on only for a
surface that is **not** inside a scroll container.

Surfaces that still cast one — the sheet, alert, toast, action bar, bottom bar, dock and home overview —
are not in scroll containers and resolve correctly, so they keep `Shadow.Default`.

For the small controls that call `drawBackdrop` directly, `liquidGlassShadow(color)` in `LiquidSurfaces.kt`
is the shared tight shadow (`LiquidShadowRadius = 10.dp`). A *proportional* radius — half the surface's
shorter side, measured with `onSizeChanged` — was built and then removed: once the ceiling came down to
10dp every remaining caller landed on the ceiling, so the machinery bought only the illusion of scaling.
`AppSwitch`, `LiquidSliderVariants` and `LiquidProgressBars` already used explicit small radii and were
correct all along.

Separately, `AppLiquidSearchField` was casting a **hard rectangle** on focus: it set `shadowElevation` in
a `graphicsLayer` without setting `shape`, and a platform elevation shadow is derived from the layer's
outline, which defaults to `RectangleShape`. `LiquidGlassDropdownItems` does the same thing correctly and
is the reference — if you add `shadowElevation` anywhere, set `shape` and `clip = false` with it.

## The sheet's dead scroll ("断触")

Reported as: scrolling a sheet's content suddenly stops responding, and you have to stop touching for
a moment before it works again. Diagnosed 2026-08-16 by instrumenting the real nested-scroll callbacks
and reading the trace off an Android 17 AVD, then confirmed by controlled before/after measurement.
**Two independent causes**, either of which alone produces the symptom.

### 1. The drag claim was starving the content's touch-slop detector

`claimFloatingChromeDrags` consumes every position change on the Main pass. Its own KDoc already warned
*"only for chrome that floats above scrollable content"* — and the sheet panel is the opposite: it
*wraps* a `verticalScroll`. Dragging the same 700px through a sheet's content:

| gesture duration | with the unrestricted claim | with it removed |
|---|---|---|
| 100ms | 579px | — |
| 250ms | 598px | — |
| 400ms | **0px** | 585px |
| 600ms | **0px** | 659px |

A flick crosses touch slop inside its first event or two, so the content's drag is already active — and
consuming — before the claim gets a look in. A slow drag has to accumulate slop over many small events
and every one was being consumed out from under it. Hence "scroll slowly and it dies; flick and it
works", which reads as flaky hardware rather than a bug.

The claim is still needed: with it gone entirely, horizontal swipes across the panel **dismissed the
sheet**. Fix is `claimFloatingChromeCrossAxisDrags`, which claims only when the accumulated drag is
horizontal-dominant. Verified: all durations 100–900ms scroll, and six horizontal swipes neither
dismiss the sheet nor switch the pager behind it.

### 2. The nested-scroll arbitration

Traced directly, for a sheet whose maximum height is 2700px:

```
preScroll DIVERT delta=-19.2  hidden=0.0 canScrollUp=false h=2157 max=2700 consumed=-19.2
preScroll DIVERT delta=-243.2 hidden=0.0 canScrollUp=false h=2700 max=2700 consumed=-198.8
preFling  EAT   v=0.0 sheetConsumed=true hidden=0.0
```

- **The fling was eaten on every release.** `sheetConsumedScroll` latched for the whole gesture, so
  *drag up → sheet grows → content scrolls → flick* ended with the sheet returning the entire velocity
  as consumed while `hidden` was exactly zero. Now tracks the **most recent** delta only.
- **`contentCanScrollUp` was latched false.** It arrived via `snapshotFlow → reporter → state write`,
  and `SheetContentColumn`'s `DisposableEffect` was keyed on the reporter *lambdas* — so any
  recomposition of the host published "cannot scroll up" mid-scroll, and `distinctUntilChanged` meant
  the `true` never came back. The tracking sheet recomposes on every keystroke. Replaced by
  `LiquidSheetContentScroll`, which holds the `ScrollState` and is read outside composition.
- **The sheet inflated when growing revealed nothing**, leaving 500–700px of blank glass under content
  that already fitted. Expand-to-scroll now requires `contentScroll.overflows`.
- **No epsilon on "can grow"**, so a sub-pixel shortfall re-armed the diversion forever.
- **`resizeTo` was outside the generation counter** — two overlapping resizes wrote `resizedHeightPx`
  from two spring trajectories. It now has `heightGeneration`.

**Rejected: snapping the height to a detent on release.** It looked right — a sheet resting below its
maximum re-arms expand-to-scroll — but it broke three existing tests that pin the documented behaviour
that the grabber is a *continuous* control and tapping it is what cycles detents. The residual cost is
bounded anyway: `applyDrag` reports only what it used and the remainder reaches the content in the same
event.

## The dock badge

Reported 2026-08-16: the number in a vertical dock's top-right corner means something different
expanded than collapsed, it has no Liquid material, and it spills out of the dock.

**The meaning.** Collapsed, one button stands in for every action, so its badge owes their total. The
BA dock did that by hand (`compactBadgeLabel = totalCount`, a real sum of calendar + pool). The GitHub
dock passed only its *refresh* count while its expanded history action showed the *unread history*
count — same corner, same size, same colour, two unrelated numbers. The collapsed label is now
**derived** by the dock from the actions it conceals (`appFloatingDockCollapsedBadgeLabel`), and the
`compactBadgeLabel` parameter is gone, so the mistake is structurally impossible rather than merely
fixed. Verified on the BA page: 33 and 15 expanded, **48** collapsed.

Already-capped inputs stay capped ("99+" plus anything is still "at least 99"), and a non-numeric
badge collapses to a dot rather than borrowing one action's label — that would be the same
inconsistency again.

**The overflow.** Placement was `align(TopEnd).offset(x = -5.dp, y = 6.dp)` — an offset inside the
host's *bounding box*, which for a capsule is the one place a badge is guaranteed to escape. Measured
at 3x against a 62dp dock, the badge's right edge tracked the capsule's curve exactly (210px at one
row, 218px eight rows down, 222px further still) because the dock's capsule clip was shaving it: the
badge lost its own rounded end and read as bleeding off the edge. `appFloatingDockBadgeOffsetPx` now
solves against the inscribed circle, putting the badge's outer corner on the rim at 45° so nothing
extends past it. After: 19–21px of clearance at every row.

**The material.** The badge was a flat opaque squircle — the only pasted-on element in a glass dock.
It now takes `drawBackdrop` with vibrancy, a small blur and lens, an ambient rim highlight and an inner
shadow. Two constraints shaped it:

- It samples the **dock's own exported backdrop**, not the page. Sampling the page from inside the
  dock would be glass on glass; `exportedBackdrop` is the library's answer and `AppFloatingLiquidVerticalDockSurface`
  now publishes one through `LocalAppFloatingDockBackdrop`.
- The tint stays at 90% opacity. Apple's Materials guidance is that Liquid Glass exists to reveal what
  is beneath it, but a badge's job is the opposite — legible at 10sp against anything. So the glass
  supplies the optics and the colour stays a colour. The same guidance ("use Liquid Glass effects
  sparingly") is why `badgeBackdrop` is opt-in per call site rather than switched on for every badge.

Not `Shadow.Default` here either — its 24dp blur spreads 48dp around an 18dp badge. See the
square-cornered shadow section above; `liquidGlassShadow` is the shared tight one.

## The custom background on secondary pages

Issue: "二级菜单的透明度在白色背景下生效" — a secondary page had *two* backgrounds, one pure white and
one the semi-transparent custom image, so the opacity slider behaved differently depending on how deep
the page was. Half-fixed between 1.12.0 and 1.13.0. **Two independent causes; the second one is the
half that was left.**

### 1. Routes that export a backdrop had no opaque base at all

`Modifier.layerBackdrop` draws only `drawContent()` to the screen and records the
`rememberLayerBackdrop { ... }` block into an offscreen layer *separately*:

```kotlin
override fun ContentDrawScope.draw() {          // LayerBackdropModifier.kt
    drawContent()
    recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
}
```

`AppManagedBackgroundHost` wrote its `drawRect(baseColor)` **inside that recording block**, and chose
between `layerBackdrop(...)` and `background(baseColor)` as alternatives. So every route with
`exportBackdropToContent = true` — Settings, McpSkill, GitHubActionsNotificationHistory, OsShellRunner —
painted nothing opaque on screen and was transparent down to the main pager behind it. The page
underneath and the custom image composited together into what looks like two stacked backgrounds. Only
the `else` branch ever painted a base, which is why About and WebDavSync already looked right.

Fixed by painting the base unconditionally (`appManagedBackgroundBaseModifier`). The recording still
needs its own `drawRect`, because it runs at the `layerBackdrop` node and cannot see an outer modifier's
background.

### 2. The two paths composited over different tokens

miuix's tokens are not interchangeable:

| | `background` | `surface` |
|---|---|---|
| light | **White** | `#F7F7F7` |
| dark | `#242424` | **Black** |

`MainPagerLayout` already declared `.background(colorScheme.background)`, but `AppScaffold` defaults to
`colorScheme.surface` and painted straight over it — dead paint. So main pages composited the image over
`surface` while routes used `background`. Measured in dark theme at 16% opacity: a main page read
rgb(30,36,40) where the same pixel of the same image on a secondary page read rgb(59,64,69) — a delta of
29 against the 36 that `#242424` contributes at (1 − 0.19). Fixed by making the pager scaffold
transparent so its declared base is the only one, and the same one the routes use.

**Order matters:** doing (2) first regresses hard — the pager then shows *through* the routes, because
they had no base of their own. Confirmed on device before (1) was in place.

After both, the same pixel of the same image is byte-identical on a main page and a secondary page:
(59,64,69) dark, (244,250,255) light, across four sample points.

### 3. It did not actually apply everywhere except Home

The feature's contract is its name: everywhere except Home, which has its own background. Four routes
were missing it — `BaStudentGuide`, `BaGuideCatalog`, `BaActivityCalendar`, `BaPool` — and each also
painted an opaque plate of its own that would have covered the image anyway.

Those plates are not dead weight, so they were kept rather than deleted: the calendar and pool use theirs
for a designed accent wash *and* as their glass backdrop producer, and the catalog uses `#10141B`. Each
now asks `appManagedPageBackgroundActive()` and drops only the opaque part — the calendar and pool keep
the accent wash with transparent ends, the catalog and guide go fully transparent. One signal, the same
one that already makes scaffolds transparent, so there is no second definition of "a background is
active".

Verified on device: all four routes now sample identically to a main page and to the About route —
(59,64,69) / (67,59,63) / (70,67,69) / (68,61,63) — except the calendar and pool, which sit slightly
warmer because they still lay their accent wash over the top, as designed.

### Readability is now a guarantee rather than a preset

The image is user-supplied, so the worst case has to be assumed: a white image in dark theme, a black one
in light. Composited strength is `opacity × (1 − overlay)`, and solving WCAG contrast for primary text
against that worst case gives a ceiling of **0.357** in dark and 0.527 in light — dark binds, because
`#242424` sits far closer to white than White does to black.

Measured worst-case primary-text contrast before any ceiling existed:

| opacity | dark | light |
|---|---|---|
| 16% (default) | 9.29:1 | 14.48:1 |
| 25% | 6.82:1 | 11.45:1 |
| 40% (maximum) | **4.20:1** | 7.37:1 |

So the maximum breached the 4.5:1 AA line in dark theme, and nothing in the default configuration
prevented it: `AppManagedBackgroundStyles.Standard` has a zero flat overlay and the reading-overlay
preference defaults to 0, so readability depended on the user finding the "Readable" page style.
`appManagedBackgroundRender` now derives a minimum overlay from the chosen opacity. It is **zero up to
~36%**, so the default look is untouched and the slider keeps its full range; it only trims the top,
where AA was actually failing.

**Defaults were left alone deliberately.** 16% measures at 9.29:1 / 14.48:1 — comfortably above AA — so
there was nothing to fix there, and changing it would only have altered the look on taste. What was
missing was the floor, not a different default.

**Known limitation:** `onBackgroundVariant` is only 3.04:1 on plain White and 3.86:1 on plain `#242424` —
already at or under large-text AA *before* any image, dropping to ~2.1:1 / ~2.3:1 with the default
background. No realistic overlay rescues it (14% only reaches ~2.2:1 / ~2.5:1). Fixing it means not
putting secondary text on the raw page background, which is a change across many pages rather than a
tuning of this one.

## Gaps worth doing early

- **`LiquidGlassDropdownItems`** — the dropdown container was rewritten on 08-09 but the rows inside it
  still date from 07-30. Cheapest high-value follow-up.
- **`LiquidGlassBottomBarMaterial`** — the bottom bar got a press fix but its material predates every
  material lesson from the campaign, sitting directly beside the freshly rebuilt toolbar.

## Still open from the campaign

- **The card pile is about one card deep.** A pinned card is still disposed on its *layout* position, so
  it dies roughly one card-height after crossing the stack line. Fixing it needs keep-alive headroom on
  the lazy container (measure taller than the viewport extending upward, place at `-headroom`, clip the
  parent, add `headroom` to `contentPadding.top`), which changes list wiring on all eight host pages.
- **A destructive menu item should confirm through an action sheet**, per the pull-down-buttons
  guidance. Needs a confirmation host that outlives the menu, since the menu unmounts on dismiss.
