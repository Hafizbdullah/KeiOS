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
