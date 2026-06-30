# MIUIX Modernization Landing Plan

> Reference: `.tmp/miuix` at `b09d5deb`.
> Scope: migrate KeiOS temporary MIUIX-era adapters to current MIUIX behavior cores while preserving KeiOS Liquid Glass visuals, custom sheet guard rules, user-facing motion richness, and existing app architecture.

## Goals

- Keep public KeiOS UI helper interfaces stable where possible.
- Move shared behavior cores to current MIUIX implementations or MIUIX-shaped thin adapters.
- Preserve custom Liquid Glass rendering, Backdrop visual depth, adaptive sheet sizing, and unsaved-dismiss checks.
- Reduce long-term maintenance load in popup, sheet, squircle, slider, switch, chrome, and predictive-back code.
- Verify each phase with focused unit/compile checks before broader AVD validation.

## Priority Matrix

| Priority | Area | Landing Target | Status |
| --- | --- | --- | --- |
| P0 | Nav residual cleanup | Keep route-level `NavDisplay` as the owner of route predictive back; narrow custom runtime to pager/local/fullscreen/activity paths. | Done |
| P0 | Temporary source residue | Remove the old ignored `.tmp/miuix-nav-v1` reference cache after migrating to the latest `.tmp/miuix` navigation source. | Done |
| P1 | Squircle | Convert `AppSquircle` into a thin wrapper over `top.yukonga.miuix.kmp.squircle` while retaining existing `appSquircle*` call sites. | Done |
| P1 | Popup/Menu | Replace custom popup back-dismiss behavior with a MIUIX `NavigationBackHandler` adapter, keeping KeiOS glass row visuals and adaptive width policy. | Done |
| P1 | Bottom sheet | Rebase Liquid Glass sheet behavior onto current MIUIX bottom-sheet interaction patterns: `NavigationBackHandler`, single transition collector, drag snap channel, and nested-scroll dismissal semantics. | Done |
| P2 | Slider/Switch | Adopt MIUIX interaction semantics for drag, press, haptic, hover, and snap, while keeping Backdrop glass track/thumb visuals. | Done |
| P2 | Chrome primitives | Audit MIUIX badge/tooltip/press/indication overlap. Keep existing KeiOS chrome primitives where they already wrap Miuix badge/tooltip and custom glass press visuals. | Done |
| P2 | Back runtime | Keep OEM policy and local fullscreen/activity handling; route-level predictive back is owned by Miuix `NavDisplay`. | Done |

## Implementation Order

1. Document and baseline scan. Done.
2. P0 route-level back cleanup and stale nav reference handling. Done.
3. P1 squircle wrapper migration. Done.
4. P1 popup/menu adapter migration. Done.
5. P1 bottom-sheet behavior alignment. Done.
6. P2 slider/switch interaction alignment. Done.
7. P2 chrome/back runtime cleanup. Done.
8. Compile, focused unit test, R8, and release art-profile pass. Done.

## Guardrails

- Preserve visual quality and interaction richness.
- Keep data and UI flow architecture unchanged unless a touched module requires a local cleanup.
- Keep public function names such as `appSquircleBackground`, `SnapshotWindowListPopup`, and `SnapshotWindowBottomSheet` during the first pass to avoid broad call-site churn.
- Prefer layout/draw-phase reads for hot drag/animation state.
- Keep sheet dismiss guard behavior intact: blocked dismiss should surface the existing prompt path.
- Keep AVD validation for a later visual QA pass after compile/test stability.

## Verification Checklist

- `./gradlew :ui-liquid-glass:compileDebugKotlin`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `git diff --check`
- `./gradlew :app:compileReleaseArtProfile`
- Optional visual pass: AVD sheet, dropdown, action bar, bottom bar, slider, switch, and route back smoke.

## Progress Log

| Date | Phase | Notes |
| --- | --- | --- |
| 2026-06-30 | Plan | Added plan for P0/P1/P2 MIUIX modernization landing work. |
| 2026-06-30 | P0 | Renamed custom route source to `StandaloneRoute`; route-level predictive back remains owned by Miuix `NavDisplay`. Removed ignored `.tmp/miuix-nav-v1` cache. |
| 2026-06-30 | P1 | `AppSquircle` now delegates to `miuix-squircle` public APIs while keeping non-composable draw-path helpers for hot-path callers. Popup and Liquid Glass sheet now use `NavigationBackHandler` predictive-back progress instead of framework back-only dismissal. |
| 2026-06-30 | P2 | Liquid sliders gained MIUIX-style edge/key-point haptics; Liquid switch gained toggle haptics on drag and tap. Existing action bar/bottom bar/floating dock chrome already uses shared KeiOS glass primitives plus Miuix badge/tooltip pieces, so this pass preserved those visuals. |
| 2026-06-30 | Verification | Passed `:ui-liquid-glass:compileDebugKotlin`, focused app back tests, `:ui-liquid-glass:testDebugUnitTest`, `:app:compileReleaseArtProfile`, and `git diff --check`. Removed stale Squircle SDF entries from release baseline profiles. |
