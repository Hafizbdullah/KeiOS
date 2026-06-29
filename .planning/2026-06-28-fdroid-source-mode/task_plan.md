# F-Droid Source Mode Planning

## Goal

Plan how to add an F-Droid repository source mode to the existing GitHub tracking page while preserving the current GitHub, Git repository, Direct APK, Actions, cache, ignore-release, import/export, and managed-install chains.

## Current Scope

- Implement the plan in vertical backend-first slices.
- Start with model/store/cache/test foundations in `feature-github`.
- Keep UI integration behind backend contracts until source identity, serialization, cache signatures, and release-check models are stable.
- Update this plan after each implementation slice and commit in segments.

## Phases

| Phase | Status | Output |
|---|---|---|
| P0 Planning setup | Done | `.planning/2026-06-28-fdroid-source-mode/*` |
| P1 Official F-Droid research | Done | `findings.md` official-doc section |
| P2 Third-party client research | Done | `findings.md` client section |
| P3 KeiOS source-tracking map | Done | Existing source-mode, cache, release-check, sheet touch points identified |
| P4 Plan document | Done | `docs/fdroid-source-mode-plan.md` |
| P5 Verification | Done | `git diff --check`, trailing-whitespace scan, and final status |
| P6 Source capability refinement | Done | Added source capability matrix, F-Droid detail/menu plan, top-bar/MCP plan |
| P7 Source parity and acceptance refinement | Done | Added source-surface parity, metadata priority, notification/deep-link plan, and first-release acceptance criteria |
| P8 Backend P1 model/store/cache foundation | Done | Added F-Droid source mode, identity normalization, config models, JSON round-trip, source counts, cache signature, tests |
| P9 Backend P2 data source foundation | Done | Added package API client, v2 index parser, repo cache models, package snapshots, and candidate selector tests |
| P10 Backend P3 release-check bridge | Pending | Dispatch F-Droid tracks through release-check service and map selected candidates into existing check models |
| P11 Backend P4 metadata sidecar | Pending | Persist package/version/repo/trust/Anti-Feature summary data for UI/detail sheets |
| P12 UI P5 sheet bridge | Pending | Add source dropdown option, repo probe state, F-Droid field visibility, localized labels |
| P13 UI P6 card/menu/detail bridge | Pending | Add compact card metadata, F-Droid detail entry, detail sheets, source filter |
| P14 P7 asset/install bridge | Pending | Feed selected F-Droid APK into existing asset/install path with hash/signer checks |
| P15 P8 transfer/WebDAV/MCP bridge | Pending | Extend import/export, WebDAV payload, MCP aliases/counts/filters |
| P16 P9 verification | Pending | Unit tests, compile, release build/R8, AVD smoke, baseline-profile review when affected |

## Decisions

| Decision | Rationale |
|---|---|
| Add F-Droid as a fourth tracked source mode | The data source is repository-index based, distinct from GitHub releases, generic Git releases, and Direct APK directories. |
| Put backend ownership in `feature-github` first | Current tracking, caching, release checks, app import/export, and MCP tracking tools already live there. |
| Use repo-level cache plus package-level lookup | Official F-Droid index v2 is large enough that per-track full downloads would waste bandwidth and CPU. |
| Prefer official F-Droid index library evaluation before custom parsing | The library already targets v1/v2 index and Android compatibility concepts; the project can still keep its own store/cache shape. |
| Treat installation trust as a separate gate from tracking trust | Version checks can start with signed-index awareness; download/install actions should require stronger signature/fingerprint confidence. |
| Give F-Droid its own details instead of treating it as Direct APK | F-Droid has Anti-Features, signer index, repo mirrors, release channels, fdroiddata metadata, APK hashes, and localized `whatsNew`. |
| Keep F-Droid source UX close to current GitHub card/menu rhythm | Existing tracked cards already handle source-specific Actions, release notes, health, asset panel, ignore current version, and delete actions. |
| Keep F-Droid top-level menu compact | Package, repo, trust, and Anti-Feature data are too dense for a narrow dropdown; one `F-Droid 详情` entry with internal tabs keeps the card/menu readable. |
| Start P2 with local structured v2 JSON parsing | It keeps the first tracking path small and avoids dependency/R8 risk; official index libraries can be revisited for signature and diff support. |

## Risks

| Risk | Mitigation |
|---|---|
| Index v2 full file size is large | Fetch `entry.json` first, use diff/conditional requests, cache repo snapshots by base URL. |
| Third-party repos differ in metadata quality | Build a tolerant parser with clear source health states and a Direct APK-style degraded path. |
| Official package API covers only some repos | Use it as a fast path for f-droid.org/compatible services; use repo index as the universal path. |
| Install trust can be misunderstood | Surface repo fingerprint, signer changes, anti-features, and confirmation before managed install. |
| F-Droid card can become too dense | Keep collapsed card to source, version, trust, and one metadata pill; move raw repo/package/version/trust data into detail sheets. |
| F-Droid notification text can become too long | Use app name, repo name, versionName/versionCode, and one highest-severity warning; put full trust and Anti-Feature data in history/detail views. |

## Errors Encountered

| Error | Attempt | Resolution |
|---|---|---|
| `GitHubTrackStoreSerializersTest.kt` lookup failed because the serializer tests live in `GitHubTrackStoreTrackedItemJsonTest.kt`. | Initial source exploration | Used `find` to locate existing test files and added serializer coverage in the correct test file. |
| `app:compileDebugKotlin` reported a missing `FdroidRepository` branch in Home overview source counts. | P8 app compile verification | Added `fdroidRepositoryCount` to `HomeGitHubOverview` and Home overview derivation. |
| First P9 package API test had an unclosed `server.enqueue(...)` call. | Initial red test run | Closed the test fixture call and reran; remaining failures were the expected missing implementation types. |

## Verification Checklist

- `docs/fdroid-source-mode-plan.md` exists.
- Planning files capture sources, findings, and progress.
- Production source files remain unchanged.
- `git diff --check` passes.
- New planning files pass trailing-whitespace scan.
