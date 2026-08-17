# Liquid component inventory

> Scanned: 2026-08-09 at `fb6aa0b02`; **rescanned 2026-08-17 at `f343f30e6`**.
> Purpose: track which Liquid components have been rebuilt and which have not, so the remaining work
> can be planned rather than rediscovered.

**97 files** in `ui-liquid-glass` (94 at the first scan), plus **13** more Liquid surfaces defined in
`:app`. Grouped by what they are as components rather than by file: roughly **48 component kinds** plus
~20 token, material and infrastructure units.

The rebuild campaign is a three-day window, **2026-08-07 → 08-09**. It touched **39 of 94** module
files, but 6 of those were a single narrow pass, so **33 were genuinely rebuilt** and **~55 remained
untouched**.

Since then, **08-16 → 08-17** added a second wave of 14 file-touches across four commits — the dock
badge, the scroll edge, the sheet's content scroll and the theme/press fixes. Those are folded into the
tables below, so the counts above describe the campaign and the tables describe today.

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

## Second wave (2026-08-16 → 08-17)

Four commits after the campaign. Listed separately because they were driven by specific defects rather
than by the rebuild plan, and because three of them move entries out of the "not touched" table below.

| Component | Files | Churn | What was done |
|---|---|---|---|
| Dock badge | **`AppFloatingDockBadge`** (new, 136 lines) · `AppLiquidBadges` (+121/-6) · `AppFloatingLiquidDockSurface` (+26/-2) · `AppFloatingSearchDock` (+21/-8) | `cfb23e352` | badge means one thing across expand/collapse, stays inside the dock, gains glass |
| Scroll edge | **`AppScrollEdgeEffect`** (new, 134 lines) | `6f6e75912` | replaced the catalog's black curtain with a real Apple scroll edge |
| Sheet content scroll | **`LiquidSheetContentScroll`** (new, 67 lines) · `LiquidSheet` (+201/-52) · `GlassChromeGestures` (+58) · `SheetContentColumn` (+12/-16) | `1111f5201` | the sheet stopped eating its own content's touch slop |
| Press bloom / theme | `LiquidGlassBottomBar` (+5) | `294ad3ab8` | press bloom keys off the app's theme, not the device's |
| Overlay tag publication | `SceneBackdropScope` (+12/-1) | `746a8e4b8` | overlay layer publishes `testTagsAsResourceId`, so sheets are reachable by baseline-profile journeys |

## Touched narrowly, not rebuilt

`53eb6510b` (08-07) only dropped identity `graphicsLayer` nodes — 5-20 line diffs, and its own message
records it as "a simplification, not a perf win". Treat these as untouched when planning.

`AppSwitch` · `LiquidSliderVariants` · `AppInteractiveTokens` · `AppLiquidButtons` (+8 more on 08-16) ·
`AppFloatingLiquidActionButton` (+3 on 08-16) · `LiquidGlassBottomBar` (+5 on 08-16)

`AppFloatingSearchDock` has **left** this list — the dock-badge commit reworked it for real.

## Not touched

| Last touched | Components |
|---|---|
| 2026-07-30 | **Dropdown items** `LiquidGlassDropdownItems` — the rows inside the rewritten dropdown |
| 2026-07-29 | **Expandable / accordion cards** `AppLiquidExpandableCards` |
| 2026-07-17 | **Status pills** `StatusPill`, `StatusIconPill` · **Info block** `LiquidInfoBlock` · **Text input** `AppTextInputContent` · **Search field** `AppLiquidSearchField` · **Standalone buttons** `AppStandaloneLiquidButtons` · **Dropdown selector controls** `AppDropdownControls` · **Card headers** `AppCardHeaders` · **Icon actions** `AppIconActions` · **Supporting blocks** `AppStatusPrimitives` · `LiquidBackdropWindowBoundary` |
| 2026-07-16 | **Progress bars** `LiquidProgressBars` · **Checkbox** `AppLiquidCheckbox` · **Dialog action buttons** `AppLiquidDialogActions` · **Bottom bar material** `LiquidGlassBottomBarMaterial` · **Overview cards** `AppOverviewCards` · **Shell panel** `ShellLiquidPanelSurface` · `GlassEffectRuntime` · `AppThemeAppearance` |
| 2026-07-13 | **Grip-aware dock** `AppGripAwareDock`, `AppGripAwareDockState` · **Search material** `AppLiquidSearchMaterial` · `AppToastBridge` · `InteractiveHighlight` · foundations `GlassStyle`, `LiquidGlassShaders`, `BackdropLensSafety`, `LiquidFiniteValues`, `GlassContentContrast` — *`AppLiquidBadges` and `AppFloatingLiquidActionButton` moved out on 08-16* |
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

**Defaults were left alone at this point** — 16% measures at 9.29:1 / 14.48:1, comfortably above AA, so
the missing piece looked like the floor rather than the number. That reading was incomplete: the reason
16% was safe is that the chrome was not sampling the page at all, which the next section is about. The
range was re-derived from this same ceiling once the chrome started carrying the wallpaper.

**Known limitation:** `onBackgroundVariant` is only 3.04:1 on plain White and 3.86:1 on plain `#242424` —
already at or under large-text AA *before* any image. No realistic overlay rescues it. Fixing it means not
putting secondary text on the raw page background, which is a change across many pages rather than a
tuning of this one.

Measured on the Archive route at the 35% default, where the rows go fully transparent so the wallpaper
applies (dark theme, four sampled row backgrounds):

| | primary | secondary |
|---|---|---|
| plain `#242424` | 14.87:1 | 3.86:1 |
| over the wallpaper | 6.09–7.14:1 | **1.58–1.85:1** |

Primary text is protected exactly as designed; secondary text is not, and the stronger default makes the
pre-existing weakness plainer. **The default is not the lever, though.** Solving for a panel fill that
would lift `onBackgroundVariant` to even 3.0:1 over a worst-case bright image gives α ≥ **0.92** — which
is just covering the wallpaper again — and at the old 16% default it was still only ~2.3:1. The quantity
that is wrong is the token, not the opacity, so picking a lower default or a hand-tuned panel alpha would
buy the appearance of a fix without the fact of one. Archive and the student guide are simply where it
shows, being the two routes whose content panels go fully transparent.

## The chrome was not sampling the page at all

Reported as "the custom background's defaults badly degrade the Liquid look". The defaults were a symptom;
the cause was that **no glass surface in the app had ever sampled the page's background image.**

Every `LayerBackdrop` producer recorded `drawRect(colorScheme.surface)` before `drawContent()`, and the
background image is a *sibling* drawn behind the recorded subtree — so the image could not reach the layer,
and the opaque rect would have covered it if it had. Measured on the BA page with an image at 16%:

| | before | after | page 1px away |
|---|---|---|---|
| title capsule | (14,14,14) | **(46,52,58)** | (61,54,60) |
| toolbar icon capsule | (8,8,8) | **(51,50,57)** | — |
| bottom dock | (6,6,6) | **(63,61,64)** | (65,65,66) |

Note the *neutrality* of the before column: exactly equal channels, so the sample carried no trace of a
reddish image sitting right beside it. The chrome was a black hole punched into a photograph, which
inverts what the material is for — Apple's Materials guidance has Liquid Glass "allow content to scroll
and peek through from beneath these elements". A stronger wallpaper only made the mismatch louder, which
is why the opacity default had drifted down to 16% to hide it.

Two token bugs sat underneath, both live even with the feature off: producers recorded `surface` while a
non-Home main page's visible base is `background` (Black against `#242424` in dark — 36 levels), and the
pager's producer did the same. `appPageBackdropBaseColor()` now derives it from the scaffold container.

### How the composite reaches the glass

`LocalAppManagedSceneBackdrop` publishes the page composite — base colour, image, readability overlay — as
a `LayerBackdrop`, non-null only while a background is actually painting. Chrome consumers get
`rememberCombinedBackdrop(scene, ownLayer)`, so the scene is drawn *under* their own layer and each
positions itself from its own coordinates; the producers correspondingly stop painting a base of their
own, since the scene is now the base. `appManagedPageBackgroundActive()` cannot answer this question —
`MainPagerPageHost` makes every non-Home page's scaffold transparent whether or not a background exists.

`MainPageBackdropSet` also splits producers from consumers by name. They were the same objects, and
`MainPageContentBackdropScene` decided whether to record by casting its argument to a layer — so any
consumer value that happened to be one would be silently re-recorded and blanked. That is exactly the trap
the scene backdrop would have walked into.

### Cards are translucent without sampling anything

Reported as the cards looking opaque over a wallpaper. They were: their material is a flat
`drawRect(baseColor)`, fully opaque, so the page behind them was simply gone.

The first instinct — give them the scene — is the one the next section rejects, and re-measuring at
**steady state** (rather than on BA first entry, where the earlier 8 fps figure came from) confirmed the
rejection rather than overturning it:

| BA page, six-swipe scroll | AVG | 1% low |
|---|---|---|
| canvas fill | 36 | **13** |
| scene-sampled cards | 33 | **6** |

But a card does not need to sample anything to show what is behind it. `drawBackdrop` composites its
sampled layer *over* what is already on screen, so drawing the fill below full opacity reveals the page
underneath for the price of the same single rect. Zero measured cost: 120 fps / 1% low 120 idle after.

The alpha is a floor derived from the weakest text a card carries, `onBackgroundVariant`, against a
worst-case image, keeping **80%** of the contrast that same text has on a plain page (3.86:1 dark,
3.04:1 light):

| | alpha | worst-case secondary | show-through |
|---|---|---|---|
| dark | 0.80 | 3.11:1 | 20% |
| light | 0.82 | 2.45:1 | 18% |

Light needs the thicker fill because `onBackgroundVariant` has no headroom there at all — 3.04:1 on pure
white — so any darkening costs it immediately. That asymmetry is why the value is per-theme.

Both round *up* from the solved minimum (0.794 / 0.813); rounding down put light at 2.41:1 against a
2.43:1 floor, and the test caught it. Primary text cannot be the casualty here: the page ceiling already
holds it at AA, and a translucent card is no worse than the page it reveals — asserted at alpha 0.

This does trade secondary-text contrast, deliberately. A card that reads as an opaque slab on a
photograph was the defect being fixed.

### Cards stay off the scene, on Apple's advice and a measurement

Composing the scene under the content material as well looked right and was wrong twice over. Apple:
"Don't use Liquid Glass in the content layer... Instead, use standard materials for elements in the
content layer." And it turns every one of a page's ~20 cards from a single `drawRect` into a real blur of
a screen-sized layer: the BA page's 1% low frame rate fell to **8 fps**. Cards keep the canvas fill; what
they needed was the right colour, which the base-token fix gave them.

The scene itself is free. A/B on the same six-swipe scroll, background on both times: scene composed in
= AVG 43 / 1%L 22, scene suppressed = AVG 44 / 1%L 15. ~43 fps is this page's pre-existing AVD floor.

### The range, re-derived from Apple's dimming rule

Apple handles legibility over a rich background with a *local* dimming layer: "If the underlying content
is bright, consider adding a dark dimming layer of 35% opacity. If the underlying content is sufficiently
dark... you don't need to apply a dimming layer." KeiOS's readability ceiling is the same quantity, so
both ends of the slider now come from it rather than from taste:

- **default = the ceiling itself, 0.35** — the strongest wallpaper that needs no dimming at all.
- **maximum = ceiling / (1 − 0.35) = 0.55** — where Apple's 35% figure is reached.

`AppManagedBackgroundReadabilityTest` checks both as properties, so widening the range without
re-deriving it fails rather than quietly costing legibility. The slider's own copy of the range was a
separate set of literals with nothing checking that they agreed; it now aliases the store.

Verified at 35% on the AVD, both themes. Dark: title capsule (59,72,84) against page (90,76,88), dock
(95,90,97) against (102,100,102). Light: (240,249,255) against (236,220,234), dock (254,251,255) against
(246,244,248) — the glass carries the wallpaper's cast in both.

### The catalog's edges were a curtain, not a scroll edge

Follow-on from the same session, and self-inflicted: the catalog page draws a 150dp top and 196dp bottom
band, each a gradient of its `panelBackground` at alpha 0.96/0.98. That colour became `Color.Transparent`
when the page was taught to let the background through — and **`Color.Transparent` is transparent
*black*, so `.copy(alpha = 0.96f)` is 96% black, not "the panel at 96%".** Measured on the AVD:
`rgb(1,2,3)` at the top of the screen and `rgb(5,5,5)` at the bottom, the wallpaper surviving only in the
strip between them. The pager-switch veil, `drawRect(panelBackground.copy(alpha = veilAlpha))`, had it
too.

Both are now `AppScrollEdgeEffect`, shared and living beside `liquidSheetScrollEdge`. It does what Apple
describes — "blurring and reducing the opacity of background content" — rather than painting in front:
a 16dp blur of the page composite, masked out toward the content with `BlendMode.DstIn`, plus a tint
capped at Apple's 35%. The tint comes from `appPageBackdropBaseColor()`, so it is White in light theme
and brightens rather than darkens.

| | before | after |
|---|---|---|
| top, y=20 | (1,2,3) | **(20,24,30)** |
| top, y=250 | (30,37,45) | **(55,68,84)** |
| bottom, y=2750 | (10,9,10) | **(96,91,107)** |
| bottom, y=2840 | (5,5,5) | **(80,81,88)** |

The two ramps are deliberately asymmetric. A bottom edge separates chrome that floats *inside* the band —
the catalog's playback bar starts about 14% down it — so a symmetric ramp is still near zero exactly where
it is needed, and the bar's own title collided with a list row until the rise moved to 8%.

Worth knowing: the black curtain had been hiding that collision. Removing a curtain exposes whatever it
was covering, which is a reason to check the layout underneath rather than to keep the curtain.

### Light-mode acceptance, and the one page it failed on

Run on the AVD's *system* light mode with the app on Follow-system and a cold start, not the in-app theme
switch — that switch leaves components stale, which is its own open issue.

Home, OS, MCP, GitHub, BA, Settings, Event Calendar and Archive all passed: chrome glass carries the
wallpaper, cards keep their fill, the scroll edges brighten with White instead of darkening.

**About failed.** Its cards are `0x22` tints — 13% alpha of an accent and nothing else — so with the
wallpaper behind them the illustration *was* the card and the labels sat on raw imagery. Settings has the
same kind of card and looked right because it fills with `surfaceContainer` at 64% before tinting. About
now composites over the same surface at the same alpha, keeping its per-card hues:

| `onBackgroundVariant` label, light | before | after | Settings | plain page |
|---|---|---|---|---|
| "Package name" | 2.09:1 | 2.43:1 | ~2.72:1 | 3.04:1 |
| "Build Type" | 2.14:1 | 2.44:1 | | |
| "Commit time" | 2.23:1 | 2.48:1 | | |

Note the numbers understate the change: most of what made those labels unreadable was the illustration's
high-frequency detail behind them, which a luminance ratio cannot see. About is now within ~0.3 of
Settings, and the residual gap to a plain page is the `onBackgroundVariant` limitation recorded above,
not this card.

Left as an asymmetry rather than changed: Settings passes `exportBackdropToContent = true` so its cards
get a real glass material, and About does not, so About's cards are tinted fills. Worth unifying, but it
changes what every nested component inside About samples.

### With no background, the page was painting the elevated token

Apple, Dark Mode: *"the system uses two sets of background colors — called base and elevated… The base
colors are dimmer, making background interfaces appear to recede, and the elevated colors are brighter,
making foreground interfaces appear to advance."* miuix ships exactly that pair:

| | light | dark |
|---|---|---|
| `surface` (base) | `#F7F7F7` | Black |
| `background` / `surfaceContainer` (elevated) | White | `#242424` |

Non-Home main pages were painting the **elevated** token as their page base, and their cards were
sampling that same colour. `AppFeatureCard` fills with `surfaceContainer` at 64%, and 64% of a colour over
itself changes nothing — so the card had nothing to stand on. Routes never had the problem, because their
scaffold paints `surface`.

Measured page vs card interior, no background:

| | before | after | Apple's own |
|---|---|---|---|
| dark | 36 / 42 — **Δ6** | **0 / 42 — Δ42** | `#000` / `#1C1C1E` — Δ28 |
| light | 255 / 255 — **Δ0** | **247 / 255 — Δ8** | `#F2F2F7` / `#FFF` — Δ8–13 |

Δ0 in light is the striking one: the cards were the page, held together only by their rim.

Two changes, both narrow. The pager paints `surface` unless a managed background is active — with one, the
base has to stay `background`, which is what the image composites over and what the readability ceiling is
solved for. And `mainPagerPageContainerColorOverride` is now conditional on a background actually
painting, rather than transparent for every non-Home page; that also makes
`appManagedPageBackgroundActive()` mean what its name says, retiring the caveat recorded against it above.

`appManagedPageCardMaterialColor` names the choice: elevated when unbacked, the page base at
[the card alpha](#cards-are-translucent-without-sampling-anything) when backed. Its test caught a real
confusion while being written — the wallpaper's base (`background`) and the unbacked page's base
(`surface`) are *different pairings*, and the first version compared the wrong two.

## What to rewrite next (re-derived 2026-08-17)

The 08-09 list named `LiquidGlassDropdownItems` and `LiquidGlassBottomBarMaterial`. Re-derived against
Apple's *Materials* HIG page and the Backdrop docs, that ordering **survives but for different reasons
than were recorded**, and two things it implied turn out to be false. Both corrections are below,
because acting on either would have wasted a rewrite.

### The rule that decides the list

Apple, *Materials*: Liquid Glass "forms a distinct functional layer for controls and navigation
elements … that floats above the content layer". And, explicitly:

> **Don't use Liquid Glass in the content layer.** … including it in the content layer can result in
> unnecessary complexity and a confusing visual hierarchy. Instead, use standard materials for elements
> in the content layer, such as app backgrounds.
>
> **Use Liquid Glass effects sparingly.** … overusing this material in multiple custom controls can
> provide a subpar user experience by distracting from that content. Limit these effects to the most
> important functional elements in your app.

So "untouched" is not the same as "owed a rewrite". A content-layer component that carries no glass is
*finished*, not stale.

### Correction 1 — most of the "not touched" table is not debt

Checked, not assumed: `StatusPill`, `LiquidInfoBlock` and `AppLiquidExpandableCards` contain **zero**
`drawBackdrop` and zero `glassStyle` calls. They are flat fills and `AppSurfaceCard`, which is exactly
what Apple asks for in the content layer. The same holds for the card headers, text input, dialog
actions, progress bars and status primitives sitting beside them. Roughly **20 of the ~44 untouched
files are untouched because they are already right**, and rewriting them as glass would move the app
*away* from the guidance while adding a per-frame cost.

Only the ~7 functional-layer entries below are candidates at all.

### Correction 2 — the bottom bar is not missing its lens

`grep 'lens('` on `LiquidGlassBottomBar` returns nothing, which reads as a missing refraction on the
app's most-visible glass. It is not: the bar calls **`safeLiquidLens(...)`**, the project's clamped
wrapper, at three sites, alongside `vibrancy()` and a `layerBlock` for its transforms. Its effect stack
is the complete color-filter ⇒ blur ⇒ lens order the Backdrop docs prescribe.

Its real debt is narrower and worth stating precisely, because it is what makes it rank at all:
`LiquidGlassBottomBarMaterial` is a **private 84-line material** — `surfaceAlpha` 0.40/0.18,
`lensHeight`, `lensAmount`, `highlightAlpha` — duplicating a job something else already does, plus
hardcoded `10.dp`/`14.dp` interaction-lens numbers inline at the third call site that bypass even the
private material.

**Correction 2b, found while doing the work:** the thing it duplicates is *not* `GlassVariant.Bar`, as
first written here. `GlassVariant.Bar` serves the controls that sit *inside* bars — `AppLiquidButtons`,
`AppFloatingLiquidActionButton`, `AppInteractiveTokens` — and the rebuilt toolbar does not use it
either. The toolbar carries **`LiquidActionBarMaterial`** in `LiquidActionBarStyle`, a data class with
the *same four fields* as the bottom bar's private one and different numbers (0.30/0.22 surface against
0.40/0.18; a light-mode highlight of 0.66 against a full 1.0). That is the real duplication: one visual
role, two definitions, on the two surfaces a teacher sees simultaneously. So the fold target is the
toolbar's material, not the variant enum — see `LiquidActionBarMaterialTest`.

### The ranking

| # | Component | Why now | Cost |
|---|---|---|---|
| 1 | ~~**`LiquidGlassBottomBarMaterial` → `LiquidActionBarMaterial`**~~ **— done, `2026-08-17`** | The bar is the one surface on *every* screen, and Apple names navigation as where Liquid Glass belongs, so it is the "most important functional element" the guidance says to spend the effect on. Landed as consolidation, not new design: the private material and palette deleted, both bars now on `liquidActionBarMaterial` / `rememberLiquidActionBarPalette`, the inline highlight and shadow replaced by the toolbar's own `liquidActionBarBaseHighlight` / `liquidActionBarBaseShadow` helpers, the flat 10% selection film replaced by the accent-aware `liquidChromeSelectionIndicatorColor` the toolbar already used, and the press lens named. `blur` moved from `UiPerformanceBudget.backdropBlur` to `material.blur` — both 4.dp, so that half is a no-op that removes a second source. Verified on the AVD in both themes. | small — 84 lines out |
| 2 | ~~**`LiquidGlassDropdownItems`**~~ **— done, `2026-08-17`** | Rows inside a container rewritten on 08-09. Apple decides the shape: the *rows* must **not** become glass — the menu container is the functional-layer surface and the rows are vibrant content on it, which the file's own comment already said. So it was a code-health job. **The "judge it on the 818 lines" instruction below was wrong — see the note.** What landed: one press progress instead of two springs on the same boolean; the darkening overlay folded from a `matchParentSize` sibling `Box` into a draw pass (one fewer node and measure per row); one `LiquidGlassDropdownCheck` instead of two implementations of the same icon, which also fixed a drift where the leading copy ran its enter springs backwards on hide; the colour ladder derived once for the row and the sizing pass instead of twice; the four nested shadow ternaries moved to `liquidGlassDropdownPressShadow`; a three-branch `when` whose last two branches both returned `23.dp` collapsed. | medium — done |
| 3 | ~~**`LiquidSliderVariants`**~~ **— decided and done, `2026-08-18`** | Apple's exception for content-layer controls says a slider "takes on a Liquid Glass appearance to emphasize its interactivity **when a person activates it**", while the slider lenses at rest. **Decision: keep glass at rest**, which Backdrop's own slider does and which the rest of this app's language expects — a 20dp capsule turning opaque at rest would read as foreign — and deliver Apple's emphasis *as light* instead of as a material swap. Which is also the "go past the tutorial" half: the thumb's fixed `vibrancy()` became a progress-driven `colorControls(brightness, saturation)`. Resting values reproduce `vibrancy()` exactly (the docs define it as `colorControls(saturation = 1.5f)`), so idle is unchanged and only the active end is new. Note the thumb was *already* past the tutorial elsewhere — `depthEffect = true`, a combined backdrop, `Highlight.Ambient`, `InnerShadow` and a velocity stretch in `layerBlock`. | small in the end |
| 4 | **`AppGripAwareDock`** · **`AppLiquidSearchMaterial`** | Functional-layer, still on 07-13 materials, and neither has a shared-system equivalent yet. Lower because the grip dock is one screen and the search material is already consistent with the search field. | small each |

Not ranked, deliberately: `GlassStyle`, `LiquidGlassShaders`, `BackdropLensSafety`, `GlassContentContrast`
and the token files. They are foundations the campaign built *on* — old dates, current designs.

### Do not rank a component by its line count

Written after doing #2, because the entry above set "judge it on the 818 lines" and that was the wrong
target. The file came out of the work at **881 lines — 63 more than it went in with**, while every actual
defect in it got fixed. Each extraction traded roughly fifteen duplicated lines for twenty-five of
function signature plus the note explaining the drift it closed. One round of that was over-abstraction
and got reverted: seven named spring constants existed only to keep two copies of the check in step, and
once there was one copy they were pure indirection.

What is actually left of the length is the public API surface: six public composables with fourteen to
seventeen parameters each is roughly 250 lines of parameter list, spread over 32 call sites. That is the
app's dropdown vocabulary, not fat, and shrinking it means changing all 32.

So the metric for a row-level component is duplication and per-row runtime — nodes, springs, draw passes
— not the line count. A file can get longer and better in the same commit.

~~**Dead code found while ranking:** `LiquidSliderInteractionLock` has no consumers outside its own file.~~
**Retracted 08-18 — that was wrong.** There is no class by that name; the file declares
`Modifier.liquidSliderInteractionLock`, which the slider calls, and grepping the *filename* found nothing
because nothing refers to a file. Verified: one declaration, one use.

### What the AVD cannot check on a slider

Recorded so the next person does not spend the time again. The active state of a settings slider is
**not reachable with synthetic input** on the API 37 AVD: `adb shell input swipe` and a hand-built
`motionevent DOWN`/`MOVE` are both claimed by the settings pager, which is also horizontal, so the page
slides and `pressProgress` never leaves zero. A bare `DOWN` with no movement does not ramp it either —
the press progress follows a *claimed drag*, past touch slop.

So the resting appearance is verifiable by screenshot and the active appearance is not. The numeric
contract is pinned by `LiquidSliderThumbColorControlsTest` instead, which is the half a screenshot could
never have guarded anyway — a drift of the resting saturation from 1.5 to 1.4 is invisible to the eye and
would leave the thumb permanently duller than every surface that still calls `vibrancy()`. A real-finger
pass on the active state is still owed.

## Still open from the campaign

- **The card pile is about one card deep.** *Mechanism landed 2026-08-18; 1 of 8 hosts converted.*
  See "The card pile" below.
- ~~**A destructive menu item should confirm through an action sheet**, per the pull-down-buttons
  guidance. Needs a confirmation host that outlives the menu, since the menu unmounts on dismiss.~~
  **Done for the GitHub track delete, `2026-08-18`.** See below.

## The destructive menu item (2026-08-18)

The plan's one-line item was right, and I nearly retracted it on the wrong page. Recording both, because
the two Apple pages say different things and only one of them governs this case.

**Action sheets** says an alert is acceptable: *"Although an alert can also help people confirm or cancel
an action that has destructive consequences, it doesn't provide additional choices related to the
action."* Read alone, that makes the existing centred alert compliant and the item phantom work.

**Pull-down buttons** is the page that governs, because the delete is chosen from a menu: *"Let people
know when a pull-down button's menu item is destructive, and ask them to confirm their intent … the
system displays an action sheet (iOS) or popover (iPadOS) … Because an action sheet appears in a
different location from the menu and requires deliberate dismissal, it can help people avoid losing data
by mistake."*

The different location is the whole reason, and it is exactly what the old alert did not give: the More
menu opens at the item's trailing edge and the alert opened centred over roughly the same area, so a
second tap landing where the first one did could confirm a delete that was never read.

**What changed.** `GitHubDeleteTrackDialog` renders `LiquidActionSheet` instead of `AppWindowDialogHost`.
No new confirmation host was needed after all — the plan expected one, but the delete already hoisted its
`pendingDeleteItem` to page state, so the confirmation already outlived the menu and this was a
presentation swap. `LiquidActionSheet` already orders destructive-first and cancel-last, so Apple's
"make destructive choices visually prominent … place these buttons at the top" came for free.
`dismissible = !deleteInProgress` so a delete in flight cannot be walked away from.

`GitHubDeleteTrackDialogContent` is deleted — it had no production caller left, only the test.

**Verified on the API 37 AVD:** More menu → Delete tracking → the sheet rises from the bottom edge with
a red Delete above a Cancel; Cancel dismisses and the tracked item survives.

### Still open here

- **The import confirmation stays an alert, deliberately.** It is not menu-originated, so the Action
  sheets page's allowance applies. `GitHubTrackDialogsTest` now pins that the two use different
  presentations, and why.
- **BGM favourite removal still does not confirm.** It is also a menu item with a red row
  (`BaGuideBgmFavoriteCards`), so the same pull-down rule applies, but it deletes immediately through
  `requestRemoveBgmFavorite`. Unlike the track delete it has no pending state to hang a sheet on, so it
  does need the hoisting the plan described. Worth weighing first: un-favouriting is nearly reversible,
  one of its two call paths already toasts, and Apple also says to use action sheets sparingly.


## The card pile (2026-08-18)

The plan described this as a lazy-disposal problem needing keep-alive headroom, and that was half of it.
**The other half was in the transform, and it is the half that actually capped the depth.**

`computeAppEdgeStackTransform` clamped the pile's extent to the disposal point:

```kotlin
val disposalOvershoot = (stackLinePx + itemHeightPx) * APP_EDGE_STACK_RETIRE_MARGIN
val extent = minOf(APP_EDGE_STACK_LEVELS * stepPx, disposalOvershoot)
```

with a comment saying exactly why — *"a pinned card is still disposed on its LAYOUT position … so the pile
can never outlast the card's own height"*. So the depth was bounded twice: once by the container disposing
the item, and once by the engine deliberately retiring it before that happened. Adding headroom alone
changes nothing, because the clamp still retires the plate on schedule. Raising
`APP_EDGE_STACK_LEVELS` alone would also have changed nothing, for the same reason.

**What landed.**

- `AppEdgeStackKeepAlive` — a wrapper that measures its child `headroom` taller, places it at `-headroom`,
  clips back to the visible bounds, and takes over as the stack container so the stack line stays measured
  from the *visible* top edge.
- `appEdgeStackKeepAliveTopPadding` — the content inset a list inside it needs, so the shift cannot be
  adopted without absorbing it.
- `AppEdgeStackState.keepAliveHeadroomPx`, published by the wrapper and read by the probe.
- `computeAppEdgeStackTransform(keepAliveHeadroomPx = …)`, which adds the headroom to the disposal bound.
  **Defaults to zero, so the seven unconverted hosts behave exactly as before** and each gains depth only
  when it adopts the wrapper. That is what makes this safe to roll out one page at a time.

**Converted: `BaCalendarPoolStackedLayout`** — one host, two routes (activity calendar and pool). Picked
because it is the only stacking host with a plain `AppPageLazyColumn`: the other seven wrap theirs in
`PullToRefresh`, where shifting the list up interacts with the refresh indicator's placement and wants its
own look.

**What the AVD could and could not show.** A plate mid-pile renders correctly on the converted routes —
dimmed, blurred, inset, pinned under the server panel. A *deep* pile cannot be shown there and it is not a
bug: those cards are roughly 400dp tall against a 504dp pile extent, so two plates can never coexist on
that page whatever the bound allows. The pages that would show three plates are the card-dense ones with
short rows — the OS page above all. So the depth claim is pinned by arithmetic in
`AppEdgeStackedCardsTest` instead: without headroom a short row saturates and retires at the disposal
bound well before its level budget; with headroom it saturates exactly at the budget and a half-depth
plate is still fully present.

### Remaining

Seven hosts, all mechanical now that the primitive exists, all needing the same three edits (wrap the
list, drop `appEdgeStackContainer` from it, run the top inset through the helper): `OsPageMainList`,
`McpPageContent`, `GitHubMainContentSection`, `GitHubActionsNotificationHistoryPage`,
`BaGuideCatalogV2ListContent`, `BaGuideStudentBgmTabContent`, `BaGuideMemoryLobbyTabContent`.

The open question for all seven is `PullToRefresh`: the shifted list's top is no longer the visible top, so
the refresh indicator's anchor and the pull threshold need checking on each. Worth doing one of them and
looking hard before doing the rest.

Cost to weigh: every card inside the headroom is a real composed, measured card. 530dp of headroom is
roughly three extra tall cards or eight short rows kept alive per stacking page.
