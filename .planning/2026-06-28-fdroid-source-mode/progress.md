# F-Droid Source Mode Progress

## 2026-06-28

- Read planning-with-files and Android UX design skill guidance.
- Searched official F-Droid documentation for repo APIs, v2 index, metadata, update processing, and custom repo setup.
- Probed live F-Droid main repo `entry.json`, package API, and v2 index package structure.
- Checked Maven Central metadata for `org.fdroid:index` and `org.fdroid:index-android`; latest observed version is `0.2.0`.
- Reviewed third-party clients:
  - Droid-ify
  - Neo Store
  - AuroraDroid
  - Foxy Droid
  - Obtainium
- Scanned existing KeiOS GitHub tracking source-mode, release-check, cache, serializer, and edit sheet touch points.
- Created `docs/fdroid-source-mode-plan.md`.
- Compared existing GitHub/Git/Direct APK feature surfaces:
  - tracked card more menu
  - header asset action
  - asset panel and APK trust pills
  - Direct APK health card
  - top-bar filter/sort/import menu
  - content filtering
  - MCP tracking filters
- Expanded `docs/fdroid-source-mode-plan.md` with:
  - existing source capability matrix
  - F-Droid-specific card/menu/detail sheet plan
  - data/cache/trust model refinements
  - top-bar and MCP integration plan
  - revised implementation phases
- Refined `docs/fdroid-source-mode-plan.md` with:
  - source surface parity across GitHub/Git/Direct APK/F-Droid
  - compact top-level F-Droid menu strategy
  - F-Droid metadata priority and warning severity
  - notification, history, and deep-link behavior
  - first-release acceptance criteria

## Next Verification

- `git diff --check` passed.
- New planning files passed trailing-whitespace scan.
- Only planning/docs files changed.

## 2026-06-29

- Started implementation on branch `codex/fdroid-source-mode`.
- Switched planning scope from research-only to backend-first implementation.
- Marked Backend P1 model/store/cache foundation as in progress.
- Completed Backend P1 model/store/cache foundation:
  - Added `GitHubTrackedSourceMode.FdroidRepository`.
  - Added F-Droid identity normalization and config models.
  - Added F-Droid source constraints, track ids, source counts, JSON round-trip, and cache signatures.
  - Added MCP source aliases/filter model support at the backend layer.
  - Kept add/edit dropdown from exposing F-Droid until the planned UI sheet bridge.
- Verification:
  - `./gradlew :feature-github:testDebugUnitTest --tests os.kei.feature.github.model.GitHubTrackModelsTest --tests os.kei.feature.github.model.GitHubCheckCachePolicyTest --tests os.kei.feature.github.data.local.GitHubTrackStoreTrackedItemJsonTest`
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :feature-github:testDebugUnitTest`
- Started Backend P2 data source foundation with a narrow package API and selection slice:
  - Added lightweight F-Droid package/version/anti-feature snapshot models.
  - Added package API client fast path for official and repo-scoped API-compatible hosts.
  - Added candidate selection by suggested version, highest compatible versionCode, highest versionCode, version regex, APK regex, SDK compatibility, and basic Anti-Feature policy.
- Verification:
  - `./gradlew :feature-github:testDebugUnitTest --tests os.kei.feature.github.data.remote.fdroid.FdroidPackageApiClientTest --tests os.kei.feature.github.domain.fdroid.FdroidCandidateSelectorTest`
- Completed Backend P2 data source foundation:
  - Added v2 index parser for repo metadata, mirrors, package metadata, version manifest, APK file metadata, localized release notes, release channels, signer SHA-256, and Anti-Features.
  - Added `FdroidRepositorySnapshot`, package lookup, repo cache key, and repo cache record freshness model.
  - Chose local structured v2 JSON parsing for the first cut; official F-Droid index libraries remain a later option for signature and diff support.
- Verification:
  - `./gradlew :feature-github:testDebugUnitTest --tests os.kei.feature.github.data.remote.fdroid.FdroidIndexV2ParserTest --tests os.kei.feature.github.data.local.fdroid.FdroidRepoCacheModelsTest`
  - `./gradlew :feature-github:testDebugUnitTest --tests os.kei.feature.github.data.remote.fdroid.FdroidPackageApiClientTest --tests os.kei.feature.github.domain.fdroid.FdroidCandidateSelectorTest --tests os.kei.feature.github.data.remote.fdroid.FdroidIndexV2ParserTest --tests os.kei.feature.github.data.local.fdroid.FdroidRepoCacheModelsTest`
