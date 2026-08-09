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

Diagnosed and fixed on 2026-08-09. It was **geometry, not clipping**, which is worth recording because
the first theory (that an enclosing layer was bounding the `ShadowNode` ring to the element rect) was
wrong — measured pixel scans show the ring escaping the card's edge normally.

`Shadow.Default` is a fixed **24dp blur with a 4dp drop**, and `ShadowNode` spreads `radius * 2` in
every direction. On a card that is correct. On a 30dp checkbox it draws a silhouette four times the
control's size, and a blurred silhouette that large has no visible corner rounding left — so what
appears under a rounded corner is a right angle. Nearly every glass control in the app was wearing a
card's shadow.

The fix scales the blur to the surface's shorter side, `0.5x` clamped to 5-24dp:

- `liquidSurfaceShadowRadius` / `liquidGlassShadow` in `LiquidSurfaces.kt` are the shared helpers.
- `LiquidSurface` measures itself with `onSizeChanged` (only when a shadow is enabled) and reads the
  result in the shadow lambda at draw time — so the whole family (cards, pills, status blocks, GitHub,
  BA and guide surfaces) is proportioned without per-call-site work.
- `AppLiquidFloatingSurface` measures itself the same way, since it spans a round button to a tall dock.
- `AppLiquidCheckbox`, both `AppLiquidButtons` sites and both `AppLiquidSearchField` sites pass their
  own height explicitly.
- The 24dp ceiling is the old constant, so **anything card-sized is pixel-identical**.

Separately, `AppLiquidSearchField` was casting a **hard rectangle** on focus: it set `shadowElevation`
in a `graphicsLayer` without setting `shape`, and a platform elevation shadow is derived from the
layer's outline, which defaults to `RectangleShape`. `LiquidGlassDropdownItems` does the same thing
correctly and is the reference — if you add `shadowElevation` anywhere, set `shape` and `clip = false`
with it.

Deliberately left alone, because 24dp is right at their size: `LiquidModalSurface`, `LiquidToastSurface`,
`LiquidSheetSurface`, `LiquidActionBarStyle`, `LiquidGlassBottomBar`, `AppFloatingLiquidDockSurface`,
`HomeOverviewGlassBatch`. `AppSwitch`, `LiquidSliderVariants` and `LiquidProgressBars` already used
explicit small radii and were already correct.

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
