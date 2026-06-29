# F-Droid Source Mode Tracking Plan

## Goal

Add an F-Droid repository source mode to the existing GitHub tracking page so users can track app updates from `f-droid.org`, IzzyOnDroid, and compatible third-party F-Droid repositories by package name.

This plan keeps the first product shape close to KeiOS tracking:

- Add/edit tracked app sheet can choose `F-Droid 仓库`.
- A tracked card can show repo name, package name, latest version, versionCode, anti-features, signer state, and release notes.
- Refresh, cache, ignore-current-version, notification, import/export, WebDAV sync, and managed-install flows reuse the existing GitHub tracking infrastructure.
- Full F-Droid store browsing, category browsing, repository marketplace, and bulk install flows are later phases.

## Research Summary

### Official F-Droid model

| Reference | Relevant Point |
|---|---|
| [All our APIs](https://f-droid.org/en/docs/All_our_APIs/) | F-Droid exposes a per-app package API for the main repo, and the repository itself is a signed JSON index. v2 uses `entry.json`, `index-v2.json`, and diff files. v1 uses signed JAR index files. |
| [New repo format](https://f-droid.org/2023/03/01/new-repo-format-faster-smaller-updates.html) | v2 diff files use JSON Merge Patch so clients can update the repo with much smaller downloads after the first full index. The official client moved toward streaming/index processing for bandwidth and memory. |
| [Setup an F-Droid App Repo](https://f-droid.org/en/docs/Setup_an_F-Droid_App_Repo/) | Third parties can publish simple binary APK repos with `fdroid update`; custom repos are part of the expected F-Droid ecosystem. |
| [Build Metadata Reference](https://f-droid.org/en/docs/Build_Metadata_Reference/) | Metadata can include changelog, anti-features, update modes, current version, current version code, and archive policy. |
| [Update Processing](https://f-droid.org/en/docs/Update_Processing/) | F-Droid update processing maintains current version/current version code metadata, which maps well to version tracking. |
| [Anti-Features](https://f-droid.org/en/docs/Anti-Features/) | F-Droid has a user-facing Anti-Features taxonomy. Tracking clients can expose these labels as install/readiness warnings and filters. |
| [F-Droid index library docs](https://fdroid.gitlab.io/fdroidclient/libs/index/) | Official reusable libraries expose index v1/v2 packages; `org.fdroid:index` and `org.fdroid:index-android` latest observed Maven version is `0.2.0` on 2026-06-28. |

Live probe on 2026-06-28:

- `https://f-droid.org/repo/entry.json` points to `/index-v2.json`, declares about 51 MB and 3998 packages, and lists 10 diff files.
- The v2 record for `org.fdroid.fdroid` includes manifest versionCode/versionName, SDK range, native ABIs, signer SHA-256, APK hash/size/path, localized release notes, and release channels.
- The main repo package API `https://f-droid.org/api/v1/packages/org.fdroid.fdroid` exposes `suggestedVersionCode`, which is useful as a fast path for official/API-compatible repos.
- `https://f-droid.org/repo/signer-index.json` maps package names to official signer SHA-256 values for `f-droid.org`.
- `entry.json`, `index-v2.json`, `index-v1.json`, and signer index files have signature options through JAR or GPG signatures.
- Official F-Droid exposes search, app build metadata, binary transparency, build status/logs, and fdroiddata links that can enrich details for the main repo.

### Third-party client lessons

| Client | Reference | Useful Pattern |
|---|---|---|
| Droid-ify | [GitHub](https://github.com/Droid-ify/client) | Custom repos, background updates, offline behavior after sync, Session/Root/Shizuku installation. |
| Neo Store | [GitHub](https://github.com/NeoApplications/Neo-Store) | Fast repo sync, many built-in repos, filters, reproducible-build labels, privacy panels, Room-backed repo cache, Kotlin serialization index v2 model. |
| AuroraDroid | [GitLab](https://gitlab.com/AuroraOSS/AuroraDroid) | Repo manager expectations and multiple install methods from an older mature client. |
| Foxy Droid | [GitHub](https://github.com/kitsunyan/foxy-droid) | Lean fast-sync approach with minimal dependency posture. |
| Obtainium | [GitHub](https://github.com/ImranR98/Obtainium) | Closest tracking analogue: F-Droid, third-party F-Droid repos, IzzyOnDroid, suggested version code, highest versionCode selection, version regex filtering, APK regex filtering, and source URL normalization. |

## Existing KeiOS Touch Points

| Area | File |
|---|---|
| Source mode enum and identity helpers | `feature-github/src/main/java/os/kei/feature/github/model/GitHubTrackModels.kt` |
| Cache source signature | `feature-github/src/main/java/os/kei/feature/github/model/GitHubCheckCachePolicy.kt` |
| Release-check dispatch | `feature-github/src/main/java/os/kei/feature/github/domain/GitHubReleaseCheckService.kt` |
| Direct APK fallback behavior | `feature-github/src/main/java/os/kei/feature/github/domain/GitHubDirectApkReleaseCheckSource.kt` |
| Generic Git release assets | `feature-github/src/main/java/os/kei/feature/github/domain/GitRepositoryReleaseAssetSource.kt` |
| APK manifest fallback | `feature-github/src/main/java/os/kei/feature/github/data/remote/GitHubApkManifestReader.kt` |
| JSON import/export | `feature-github/src/main/java/os/kei/feature/github/data/local/GitHubTrackStoreSerializers.kt` |
| Add/edit track sheet | `app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditFormContent.kt` |
| Source mode labels | `app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditLabels.kt` |
| Tracked card more menu | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemMoreMenu.kt` |
| Header install/asset action | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHeaderActions.kt` |
| Asset panel and APK trust pills | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemAssetPanel.kt`, `GitHubTrackedItemAssetPanelAssetRow.kt` |
| Direct APK remote health card | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHealthCards.kt` |
| Top bar sort/filter/import menu | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTopBarSection.kt` |
| Content filtering/sorting | `app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageContentStateDeriver.kt` |

## Existing Source Capability Matrix

| Capability | GitHub Repository | Git Repository | Direct APK | F-Droid Repository Target |
|---|---|---|---|---|
| Primary identity | owner/repo + package | repo URL + package | APK URL/directory + package | repo URL + package |
| Version source | Releases Atom/API | platform releases | APK manifest / JSON / directory | signed repo index / package API |
| Pre-release model | GitHub prerelease + tag heuristics | platform releases + tag heuristics | stable/pre-release target split | release channels + version-name heuristics |
| Precise APK version | remote APK manifest scan | remote APK manifest scan for supported platforms | native core path | index-native versionCode/versionName, manifest scan as verification |
| Actions updates | supported | skipped | skipped | skipped |
| Release notes | Decision Assist / releases | supported on GitHub/Gitee/GitLab/Gitea | direct metadata releaseNotes | localized `whatsNew`, changelog URL, fdroiddata metadata |
| Asset panel | release assets | release assets for supported platforms | single resolved APK asset | selected repo APK as an asset bundle |
| Managed install | release APK assets | release APK assets | resolved APK | APK hash/signer verified asset |
| Source health | repo profile and health score | platform fetch health | remote health card | repo sync health, index format, signature, mirror, package presence |
| More menu | refresh, Actions, release notes, ignore, delete | refresh, release notes where supported, ignore, delete | refresh, release notes where present, ignore, delete | refresh, package detail, repo detail, release notes, ignore, trust detail, delete |
| Top-bar filters | source filter, update, installed, failed, actions | source filter, update, installed, failed | source filter, update, installed, failed | source filter, update, installed, failed, anti-feature/trust filters later |
| Import | Star/import/share links | generic URL attach | direct APK/share links | package URL, repo URL, `fdroidrepo://`, preset repo search |

### Source-specific gaps to address

| Existing Pattern | F-Droid Adaptation |
|---|---|
| GitHub Actions menu appears only for GitHub repository source. | F-Droid keeps Actions hidden and uses package/repo detail actions in the same more-menu position. |
| Release notes action depends on Decision Assist or Direct APK releaseNotes. | F-Droid release notes action should read `whatsNew`, `metadata.changelog`, package page, and fdroiddata metadata in that priority order. |
| Header action toggles release APK asset panel for GitHub and Direct APK. | F-Droid header action toggles a repo-index APK asset panel for the selected candidate. |
| Direct APK health card describes remote availability/degraded state. | F-Droid health card describes repo sync state, index format, signature state, package count, selected package presence, and last successful repo refresh. |
| Asset trust currently evaluates APK-like name, ABI, debug/source/archive hints. | F-Droid asset trust adds repo signature, APK SHA-256 match, signer-index match, versionCode compatibility, ABI, anti-features, and signer-change continuity. |
| Top-bar source filter has GitHub/Git/Direct APK. | Add `F-Droid 仓库`; later add subfilters for `有反特性`, `签名需确认`, `仓库异常`, and `官方 F-Droid`. |

## Source Surface Parity Plan

F-Droid should inherit the source-agnostic parts of the tracking surface and replace only the parts whose meaning depends on GitHub releases, Git platform releases, or Direct APK probing.

| Surface | GitHub Repository | Git Repository | Direct APK | F-Droid Repository |
|---|---|---|---|---|
| Add/edit source fields | owner/repo, package, Actions, release strategy, precise APK version | repo URL, platform release support, precise APK version | APK URL/feed, package, APK manifest mode | repo URL, package, repo probe, versionCode strategy, trust policy, Anti-Feature policy |
| Header action | release page or APK asset panel | release page or platform asset panel | direct APK asset panel | selected repo APK asset panel with hash/signer gates |
| More menu top level | refresh, Actions, release notes, skip version, edit, delete | refresh, release notes where supported, skip version, edit, delete | refresh, release notes when present, skip version, edit, delete | refresh, F-Droid detail, release notes, skip version, edit, delete |
| Source details | repository profile, health, Actions | platform release detail | remote health and resolved APK detail | package, version, repo, trust, Anti-Features |
| Health signal | archived/fork/activity/release/assets/Actions | platform fetch and release parse health | remote availability and last good result | repo sync, index format, signature, package presence, diff support, last successful refresh |
| Notification text | repo/app and release tag | platform repo/app and release tag | app and resolved APK version | app, repo name, versionName/versionCode, highest trust or Anti-Feature warning |
| History surface | Actions history for GitHub-only builds | release checks only | release checks only | release checks plus source-specific trust/update metadata |
| Import/export/WebDAV | owner/repo/package/source fields | repo URL/package/source fields | APK URL/package/source fields | repo URL/package/source/trust/selection/Anti-Feature fields |
| MCP | GitHub source filters and Actions details | generic Git source filter | Direct APK source filter | F-Droid source filter, repo inspection, Anti-Feature/trust summaries |

Top-level menu density rule:

- Keep F-Droid top-level actions to `刷新`, `F-Droid 详情`, `发行说明`, `跳过本版`, `编辑`, and `删除`.
- Put `包详情`, `版本详情`, `仓库详情`, `信任与签名`, and `反特性` inside one F-Droid detail sheet with tabs or segmented controls.
- Put secondary copy/open actions inside detail sheets: `打开源页面`, `复制仓库地址`, `复制包名`, `打开源码`, `打开问题反馈`, and official-only `打开 fdroiddata`.
- Keep GitHub `Actions` hidden for F-Droid at the source-model constraint layer and at the UI menu layer.
- Keep labels short enough for the current dropdown width. Longer explanatory text belongs in the detail sheet body.

## F-Droid-specific Metadata Priority

F-Droid has richer package and repository metadata than GitHub/Git/Direct APK sources. The collapsed card should expose only decision-making signals and move raw data into details.

| Priority | Metadata | Card/Sheet Treatment |
|---|---|---|
| P0 identity | app name, package name, repo display name | card title, package subtitle, `F-Droid · repo` source badge |
| P0 version | `versionName`, `versionCode`, selected channel, `suggestedVersionCode` | local/remote version row; compare by `versionCode`, display both name and code |
| P0 trust | repo fingerprint, APK SHA-256, signer SHA-256, signer-index match for official F-Droid | one compact trust pill on card; full chain in trust tab |
| P0 warnings | `Tracking`, `KnownVuln`, `DisabledAlgorithm`, `NoSourceSince`, signer change, hash mismatch | one highest-severity warning pill on card; full list in Anti-Features/trust tabs |
| P1 app metadata | icon, summary, description, license, categories, author | detail sheet header and package tab; icon can be a remote fallback when local icon is missing |
| P1 links | source code, website, issue tracker, changelog, translation, donation | package tab actions; collapsed card stays clean |
| P1 repo state | mirrors, index format, package count, timestamp, diff availability, ETag/Last-Modified, last refresh | repo tab and health card |
| P2 ecosystem evidence | fdroiddata metadata, build status, reproducible-build result, binary transparency | official F-Droid detail only; useful for trust investigation |

Recommended warning severity:

| Severity | Signals |
|---|---|
| Block install until confirmation | hash mismatch, signer change, signature verification failure |
| Strong warning | `KnownVuln`, `DisabledAlgorithm`, `NoSourceSince` |
| User policy warning | `Tracking`, `NonFreeNet`, `NonFreeDep`, `NonFreeAssets`, `Ads` |
| Informational | release channel, ABI, target SDK, archive repo, mirror fallback |

## Product Shape

### Add/edit tracked app sheet

Add `F-Droid 仓库` to the source mode dropdown.

Fields:

| Field | First Version Behavior |
|---|---|
| 仓库地址 | Default `https://f-droid.org/repo`; accept repo base URL and normalized package page URLs such as `https://f-droid.org/packages/<package>/`. |
| 包名 | Required primary identity, can be filled from installed app picker. |
| 版本选择 | Default `推荐版本优先`; options can include `最高兼容 versionCode`, `最高 versionCode`, and `按版本名正则过滤`. |
| APK 过滤 | Optional regex for APK file name or ABI suffix, matching Obtainium’s useful tracking pattern. |
| 仓库信任 | Read-only summary after first probe: repo name, fingerprint/signature state, index format, last sync time. |
| 反特性策略 | Default `显示并提醒`; later options can include `隐藏含 Tracking`, `隐藏安全风险`, and custom Anti-Feature filters. |
| 签名策略 | Default `跟随仓库`; options can include `记录首次 signer`, `要求 signer-index 匹配`, and `变更时提醒`. |
| 镜像策略 | Default `跟随仓库镜像`; first version can display mirrors and use primary address for downloads. |

Sheet behavior:

- Package scan from APK is hidden for F-Droid because package name is the lookup key.
- Repository scan becomes repo probe: fetch `entry.json`, validate shape, show repo name and package count if available.
- Precise APK version mode displays as `索引原生版本号`; APK manifest inspection remains a diagnostic fallback.
- Actions update settings remain GitHub-only and stay hidden for this source.
- Official package page URLs can fill both repo URL and package name.
- IzzyOnDroid URLs should normalize to its F-Droid package API and repo APK URL shape when available.
- The selected installed app card should display install source when available, helping users match F-Droid/Droid-ify/Neo Store installed apps.

### Tracked card UI

Show compact F-Droid-specific signals:

- Source badge: `F-Droid` plus repo display name.
- Version line: `versionName · versionCode`.
- APK line: file size, ABI match if known, target/min SDK if useful.
- Trust line: signer fingerprint state, repo fingerprint state, and signer-change warning.
- Metadata line: anti-features and release channel labels.
- Release notes: localized `whatsNew` first, changelog URL second.

More menu:

- Shared actions: `刷新`, `忽略本版本追踪`, `编辑`, `删除`.
- F-Droid actions:
  - `包详情`: opens a package detail sheet.
  - `仓库详情`: opens repo sync/trust/mirror/index detail.
  - `发行说明`: opens `whatsNew` or changelog detail.
  - `打开源页面`: opens official package page, Izzy package page, or repo package URL.
  - `复制仓库地址`: copies normalized repo URL.
  - `信任与签名`: opens signer/hash/fingerprint detail when trust data exists.

### F-Droid detail sheets

| Detail Surface | Content |
|---|---|
| Package detail | app name, package name, summary, description, license, categories, source code, website, issue tracker, translation, donation links, author. |
| Version detail | versionName, versionCode, added time, target/min SDK, native ABIs, release channel, localized `whatsNew`, APK file name, APK size, APK SHA-256. |
| Repo detail | repo name, address, mirrors, index format, timestamp, package count, diff availability, last refresh, ETag/Last-Modified, signature state. |
| Trust detail | repo fingerprint, entry/index signature state, APK hash match, signer SHA-256, signer-index match for official F-Droid, signer change history. |
| Anti-Features detail | package-level and version-level Anti-Features, localized descriptions, severity grouping, user policy applied to this track. |

## Data Model Proposal

Extend existing tracking models with source-owned configuration while keeping old tracks stable.

```kotlin
enum class GitHubTrackedSourceMode(val storageId: String) {
    GitHubRepository("github_repository"),
    GitRepository("git_repository"),
    DirectApk("direct_apk"),
    FdroidRepository("fdroid_repository"),
}

data class FdroidRepositoryTrackIdentity(
    val repoBaseUrl: String,
    val normalizedRepoUrl: String,
    val host: String,
    val repoDisplayName: String,
    val packageName: String,
)

enum class FdroidVersionSelectionMode(val storageId: String) {
    SuggestedVersionCode("suggested_version_code"),
    HighestCompatibleVersionCode("highest_compatible_version_code"),
    HighestVersionCode("highest_version_code"),
    VersionNameRegex("version_name_regex"),
}

enum class FdroidTrustPolicy(val storageId: String) {
    TrackOnlyWarn("track_only_warn"),
    RequireRepoFingerprint("require_repo_fingerprint"),
    RequireApkHash("require_apk_hash"),
    RequireOfficialSignerIndex("require_official_signer_index"),
}

enum class FdroidAntiFeaturePolicy(val storageId: String) {
    ShowAndWarn("show_and_warn"),
    HideTracking("hide_tracking"),
    HideSecurityRisk("hide_security_risk"),
    Custom("custom"),
}
```

Storage additions:

| JSON Field | Purpose |
|---|---|
| `source.mode = "fdroid_repository"` | Source mode. |
| `source.url` | Repo base URL, normalized to a repo endpoint. |
| `source.packageName` | Package identity, duplicated from top-level `packageName` for structured import/export. |
| `source.fdroid.repoFingerprint` | Optional trusted repo fingerprint after probe. |
| `source.fdroid.selectionMode` | Version selection strategy. |
| `source.fdroid.versionNameRegex` | Optional filter. |
| `source.fdroid.apkNameRegex` | Optional APK file filter. |
| `source.fdroid.indexFormat` | Last known `v2`, `v1`, or `package_api`. |
| `source.fdroid.trustPolicy` | Trust behavior for this track. |
| `source.fdroid.antiFeaturePolicy` | Anti-Feature display/filter behavior. |
| `source.fdroid.blockedAntiFeatures` | Optional custom Anti-Feature ids. |
| `source.fdroid.packagePageUrl` | Optional package detail URL for opening source page. |
| `source.fdroid.repoPresetId` | Optional preset id such as `fdroid`, `fdroid_archive`, `izzyondroid`. |

F-Droid-specific cache models:

```kotlin
data class FdroidPackageSummary(
    val repoUrl: String,
    val packageName: String,
    val name: String,
    val summary: String,
    val license: String,
    val sourceCodeUrl: String,
    val changelogUrl: String,
    val categories: List<String>,
    val antiFeatures: List<FdroidAntiFeatureSummary>,
    val selectedVersion: FdroidVersionSummary?,
    val candidateVersions: List<FdroidVersionSummary>,
)

data class FdroidVersionSummary(
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val apkSha256: String,
    val apkSizeBytes: Long,
    val addedAtMillis: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val nativeAbis: List<String>,
    val signerSha256: List<String>,
    val releaseChannels: List<String>,
    val whatsNew: String,
    val antiFeatures: List<FdroidAntiFeatureSummary>,
)
```

Compatibility:

- Existing JSON readers keep unknown fields.
- Existing source counts gain `fdroidRepository`.
- Existing WebDAV sync receives the new fields through the same tracking JSON payload.
- Existing track IDs should include source mode and normalized repo URL to avoid collisions with GitHub/Git entries.

## Backend Architecture

Recommended package layout inside `feature-github`:

| Package | Classes |
|---|---|
| `data/remote/fdroid` | `FdroidRepositoryIndexClient`, `FdroidPackageApiClient`, `FdroidRepoUrlNormalizer`, `FdroidIndexFormatDetector` |
| `data/local/fdroid` | `FdroidRepoCacheStore`, `FdroidPackageCacheStore`, `FdroidRepoTrustStore` |
| `domain/fdroid` | `FdroidReleaseCheckSource`, `FdroidCandidateSelector`, `FdroidCompatibilityFilter`, `FdroidTrustEvaluator`, `FdroidReleaseNotesMapper` |
| `model/fdroid` | `FdroidRepositoryModels`, `FdroidIndexModels`, `FdroidTrustModels`, `FdroidSelectionModels` |
| `domain/fdroid/detail` | `FdroidPackageDetailService`, `FdroidRepoDetailService`, `FdroidTrustDetailService` |

### Release check flow

1. `GitHubReleaseCheckService` detects `GitHubTrackedSourceMode.FdroidRepository`.
2. Build `FdroidRepositoryTrackIdentity` from repo URL and package name.
3. Load local package version through existing `GitHubVersionUtils.localVersionInfoOrNull`.
4. Ask `FdroidReleaseCheckSource.evaluate(...)`.
5. Source loads repo snapshot through `FdroidRepoCacheStore`.
6. Source selects candidate with `FdroidCandidateSelector`.
7. Source maps selected candidate to existing `GitHubTrackedReleaseCheck`:
   - `stableRelease` from selected F-Droid version.
   - `preRelease` from release channels or version-name heuristics when present.
   - `remoteApkVersionInfo` from index file path/hash/versionCode.
   - `sourceConfigSignature` from repo URL, package name, selection mode, regexes, and trust fingerprint.
   - F-Droid metadata sidecar cached by track id for package detail and trust detail sheets.

### Candidate selection

Default strategy:

1. Prefer package API `suggestedVersionCode` when available and matching the package’s candidate list.
2. Filter by device compatibility:
   - `usesSdk.minSdkVersion <= current SDK`.
   - Native ABI contains installed/current ABI when APK is ABI-specific.
   - Optional target SDK display, with no hard rejection.
3. Filter by user regexes.
4. Choose highest compatible `versionCode`.
5. Display `versionName`; compare updates primarily by `versionCode`.

Pre-release mapping:

- v2 `releaseChannels` can map `Beta`, `Alpha`, `Preview`, `RC` to existing pre-release states.
- Version-name heuristics can reuse existing GitHub version utilities as fallback.
- Existing ignore keys can use `repoUrl|packageName|versionCode|versionName|channel`.

F-Droid-specific signal mapping:

| F-Droid Field | KeiOS Use |
|---|---|
| `repo.name`, `repo.address`, `repo.mirrors` | repo detail, source badge, mirror policy. |
| `repo.antiFeatures` | anti-feature dictionary for localized labels. |
| package `metadata.name/summary/description` | card title fallback, package detail. |
| `metadata.license/sourceCode/webSite/issueTracker/changelog` | detail sheet and external links. |
| version `manifest.versionCode/versionName` | update comparison and display. |
| version `manifest.usesSdk` | device compatibility and install detail. |
| version `manifest.nativecode` | ABI compatibility badge. |
| version `manifest.signer.sha256` | signer continuity and trust detail. |
| version `file.name/sha256/size` | asset panel, hash verification, download card. |
| version `whatsNew` | release notes action. |
| version `releaseChannels` | pre-release mapping. |
| version/package `antiFeatures` | warning pills and filters. |
| official `signer-index.json` | official signer verification. |

## Cache, Concurrency, And Bandwidth

F-Droid must be repo-first:

| Layer | Behavior |
|---|---|
| Entry cache | Fetch `entry.json` with ETag/Last-Modified where available. |
| Repo snapshot cache | Cache full parsed index metadata per normalized repo URL. |
| Diff cache | Apply v2 diffs when the cached timestamp matches an available diff. |
| Package cache | Store extracted per-package summaries so list refreshes avoid reparsing the full index. |
| Single-flight | Collapse concurrent refreshes for the same repo URL. |
| Batch refresh | Group tracked items by repo URL, refresh each repo once, fan out package checks. |
| Force refresh | Refresh button refreshes repo entry and package summary for all matching tracks. |
| Source-aware progress | Progress reports should count repo refresh once and package fan-out separately for cleaner notification text. |

Performance guardrails:

- Network and JSON work must run off the main thread.
- Use bounded concurrency consistent with existing GitHub refresh batch scheduling.
- Parse large indexes through streaming or official index library APIs where feasible.
- Store compact package summaries for UI and cache comparisons.
- Keep UI state flowing through repositories into ViewModels as immutable snapshots.
- Extend fair refresh scheduling with an F-Droid lane grouped by repo URL. Repository refresh should run at low bounded concurrency, while per-package fan-out can run in memory after the repo snapshot lands.

## Security And Trust

Trust levels:

| Level | Meaning | Allowed Actions |
|---|---|---|
| Unknown | Repo parsed but signature/fingerprint is not confirmed. | Tracking and display with warning. |
| Repo verified | Entry/index signature or trusted fingerprint verified. | Tracking, download preview, clear trust badge. |
| APK verified | APK hash and signer match index/trust expectations. | Managed install and direct install confirmation. |

Implementation direction:

- Store repo fingerprint after first trusted confirmation.
- Compare signer SHA-256 across updates and warn on changes.
- Verify APK hash before managed install when index provides SHA-256.
- Show anti-features as user-visible metadata before install.
- Keep Shizuku managed install framework unchanged; F-Droid provides a new asset source feeding the existing install confirmation surface.
- Use official signer-index only for `f-droid.org` official repo trust. Third-party repos should use their own repo fingerprint and APK hash continuity.
- Persist first-seen signer per package/repo pair and show a signer-change warning before install.
- Treat `DisabledAlgorithm`, `KnownVuln`, `NoSourceSince`, and `Tracking` as high-signal warning classes in the default UI.
- Show binary transparency links for official F-Droid as informational trust evidence in detail sheets.

## UI And UX Direction

Design direction: `源模式表单 + 仓库健康摘要`.

Add/edit sheet layout:

1. Source row with `F-Droid 仓库`.
2. Repo card with URL input, probe action, repo status, fingerprint/trust summary.
3. Package card with package input, installed-app binding, optional open F-Droid package URL.
4. Version selection card with concise dropdown and optional regex fields hidden until needed.
5. Check options card reuses update interval and ignore mode.

Tracked card additions:

- Use existing card density and action menu style.
- Add only high-signal metadata on collapsed cards.
- Put anti-features, signer, APK hash, mirrors, and raw index details behind expansion/detail sheet.

Collapsed card priority:

1. Title and package name.
2. Source badge with repo name.
3. Local version and selected remote version.
4. One compact trust pill and one compact metadata pill:
   - trust examples: `签名已记录`, `签名变更`, `哈希可校验`.
   - metadata examples: `Tracking`, `KnownVuln`, `Beta`, `arm64`.
5. Asset/install button follows the existing header action position.

String/i18n:

- Add localized strings for source labels, trust states, selection modes, and error messages.
- Keep user-visible strings in resources.

## Import, Share, And Discovery

First version:

- Paste/share URLs:
  - `https://f-droid.org/packages/<package>/`
  - `https://apt.izzysoft.de/fdroid/index/apk/<package>`
  - Direct repo base URL plus package name from sheet.
- Installed app picker fills package name and app label.
- Repo probe can infer official F-Droid package page URL from package name.

Later:

- `fdroidrepo://` QR/link handling.
- Import from known F-Droid clients by install source package names where Android exposes installer package.
- Built-in repo presets: F-Droid, F-Droid Archive, IzzyOnDroid.
- Search integration through official search API where available.
- Optional migration helper: list installed apps whose install source is F-Droid, Droid-ify, Neo Store, AuroraDroid, or Foxy Droid and offer bulk package-name tracking.

## Top Bar And Page-level UI Changes

| Existing Control | F-Droid Plan |
|---|---|
| Source filter menu | Add `F-Droid 仓库`. Later add trust/Anti-Feature subfilters after first source mode lands. |
| Sort by update/name/pre-release/changed/added | Keep all modes; pre-release sort should use F-Droid release channels and version-name heuristic. |
| Refresh interval menu | Keep per-track refresh override; F-Droid repo cache can refresh less aggressively through source config. |
| Export/import tracks | Extend counts and preview summaries with F-Droid item count, repo count, official repo count, and unknown-trust count. |
| Star import quick action | Keep GitHub-specific wording. Add a separate later quick action `导入 F-Droid 包` when URL/package import is ready. |
| Check logic sheet | Add F-Droid section for repo cache TTL, trust policy, package API fast path, and Anti-Feature default policy. |
| Strategy sheet | Keep GitHub Atom/API strategy isolated. Add F-Droid index strategy only if users need package API vs repo index control. |

## Notifications, History, And Deep Links

F-Droid update notifications should stay on the existing tracking notification framework and only add source-specific content builders.

| Notification Surface | F-Droid Behavior |
|---|---|
| Release update title | Use app label or F-Droid app name: `%1$s 有新版本`. |
| Release update content | Use repo and version: `%1$s · %2$s (%3$d)`, for example `F-Droid · 1.23.0 (123000)`. |
| Expanded content | Add one concise warning line when needed: `签名变更`, `KnownVuln`, `Tracking`, or `哈希待确认`. |
| Click target | Open the tracking page with `trackId` and `sourceMode=fdroid_repository`; expand or scroll to the item when route state supports it. |
| Install action | Open the existing install confirmation path only after F-Droid asset trust data is loaded. |
| History row | Store source mode, repo display name, package name, versionName, versionCode, trust summary, and Anti-Feature summary. |
| Live Updates / island text | Use short text: app name + version; repo/trust goes into expanded notification text. |

History should stay source-aware:

- Actions history remains GitHub-only because it represents build runs and artifacts.
- Release update history can become a broader `更新历史` later. F-Droid rows should already store enough metadata for that future surface.
- Deep links should carry `trackId` and source mode so a GitHub, Git, Direct APK, or F-Droid notification opens the correct card.

## MCP And Automation Surface

Existing MCP tracking filters and summaries know GitHub/Git/Direct APK. F-Droid should extend:

- `sourceMode` parser aliases: `fdroid`, `f_droid`, `fdroid_repository`, `izzy`, `izzyondroid`.
- filter mode: `FdroidRepository`.
- summary counts: `fdroidRepositoryCount`, `fdroidRepoCount`, `fdroidUnknownTrustCount`, `fdroidAntiFeatureCount`.
- inspect tool: `inspectFdroidRepo(url)` for repo health and package count.
- tracking tool result rows: repo display name, package name, versionCode, Anti-Features, trust state.
- MCP guidance text: explain that F-Droid source tracks package updates from a repository index by package name.

## Test Plan

Unit tests:

| Test Area | Examples |
|---|---|
| URL normalization | Official package URL, repo URL, Izzy URL, trailing slash, invalid URL. |
| Index parser | v2 package fixture, v2 entry fixture, v1 fallback fixture if adopted. |
| Candidate selection | Suggested version, highest compatible versionCode, ABI filter, SDK filter, regex filter. |
| Cache signature | Repo URL/package/selection/trust changes invalidate cache. |
| Release mapping | Candidate maps to `GitHubTrackedReleaseCheck` with versionCode and release notes. |
| Store migration | Existing tracks parse unchanged; new F-Droid fields round-trip. |
| Batch scheduler | Multiple F-Droid tracks sharing one repo trigger one repo refresh. |
| Trust evaluator | Fingerprint match, signer change, hash mismatch. |
| Anti-Feature policy | Tracking/KnownVuln/DisabledAlgorithm/NoSourceSince map to warning levels and filters. |
| More-menu state | F-Droid item shows package/repo/trust detail actions and hides Actions. |
| Header asset action | F-Droid selected candidate builds a single APK asset bundle with hash and signer data. |
| Top-bar filter | F-Droid filter returns F-Droid items and source counts round-trip. |
| MCP filter | `fdroid` sourceMode returns only F-Droid tracks. |

UI state tests:

- Source dropdown includes F-Droid.
- F-Droid mode hides GitHub Actions controls.
- F-Droid mode shows repo probe and version selection.
- Tracked card displays compact source metadata.

Integration / AVD after implementation:

- Add official F-Droid track for `org.fdroid.fdroid`.
- Add IzzyOnDroid track for `dev.imranr.obtainium`.
- Refresh both tracks and verify source-specific card state.
- Kill/reopen app and verify cache age and tracked cards.
- Run release build/R8 and targeted baseline profile refresh if UI or startup path changes.

Implemented verification on 2026-06-29:

- Targeted app and `feature-github` unit tests passed for editor state, asset bridge, MCP registration, release-check dispatch, F-Droid source evaluation, candidate selection, package API, index v2 parsing, repo cache models, sidecar JSON, and tracked-item JSON.
- `:app:assembleRelease` passed with `minifyReleaseWithR8`, `lintVitalRelease`, `optimizeReleaseResources`, `packageRelease`, and `assembleRelease`.
- `:app:assembleDebug` passed and the current debug APK was installed on `Pixel_10_Pro`.
- AVD smoke covered GitHub page entry, `新增跟踪`, source dropdown exposure, selecting `F-Droid 仓库`, and F-Droid field rendering.
- Longer AVD/manual QA remains useful for live repo refresh, card detail sheet, notification deep link, and cold-start cache display with real F-Droid tracks.

## Phased Implementation

| Phase | Scope | Files |
|---|---|---|
| P1 Model and store | Add source mode, identity parser, storage round-trip, source counts, cache signature. | `feature-github/model`, `feature-github/data/local` |
| P2 F-Droid data source | Add repo normalizer, package API client, v2 index parser/library adapter, repo cache, candidate selector. | `feature-github/data/remote/fdroid`, `feature-github/domain/fdroid` |
| P3 Release-check bridge | Dispatch from `GitHubReleaseCheckService`, map F-Droid candidates to existing release-check models. | `GitHubReleaseCheckService`, new source classes |
| P4 F-Droid metadata sidecar | Cache package/version/repo/trust/Anti-Feature metadata for detail sheets and card pills. | `feature-github/data/local/fdroid`, state mappers |
| P5 UI sheet bridge | Add source dropdown option, repo probe, F-Droid-specific field visibility, localized labels. | `app/src/main/.../github/sheet`, strings |
| P6 Card/menu/detail bridge | Show metadata, source-specific more menu, package/repo/trust/Anti-Feature detail sheets. | tracked card, more menu, detail sheets |
| P7 Asset and install bridge | Feed selected F-Droid APK into existing asset/install confirmation path with hash/signer gates. | asset panel, managed install prep |
| P8 Import/export/WebDAV/MCP | Extend transfer payloads, source filters, MCP tracking tools, and share import parser. | stores, MCP helpers, share import |
| P9 Verification | Unit tests, compile, release build/R8, AVD add-sheet smoke, baseline profile review. | tests and verification scripts |

## First Release Acceptance Criteria

| Area | Acceptance |
|---|---|
| Existing sources | GitHub Atom/API, Git platform releases, Direct APK checks, Actions updates, ignore modes, import/export, and managed install keep their current behavior. |
| F-Droid add/edit | Official package URL, repo URL, Izzy-style package URL, and installed-app picker can create a track with normalized repo URL and package name. |
| Refresh/cache | Multiple F-Droid tracks sharing one repo perform one repo refresh, then package fan-out runs from the cached repo snapshot. |
| Version selection | Default uses `suggestedVersionCode` when available and falls back to highest compatible `versionCode`. |
| Card/menu | F-Droid card shows compact repo/version/trust/warning signals; top-level more menu has no Actions entry and keeps labels short on narrow devices. |
| Details | One F-Droid detail sheet exposes package, version, repo, trust, and Anti-Features without overcrowding the card. |
| Release notes | `whatsNew` is preferred; changelog URL and fdroiddata metadata are fallback detail sources. |
| Notifications | Update notifications identify repo and versionCode, and deep-link to the correct tracked card. |
| Transfer/WebDAV | Import/export preview shows F-Droid item count, repo count, official repo count, and unknown-trust count. |
| MCP | `fdroid` source aliases filter F-Droid tracks and expose repo/trust/Anti-Feature summaries. |
| Build/runtime | Unit tests pass, release build/R8 pass, and AVD smoke covers add, refresh, card menu, detail sheet, notification deep link, and cold restart cache display. |

## Open Decisions Before Coding

1. Trust depth for first release: tracking-only warning state, full entry signature verification, or install-gated verification first.
2. Package API fast path scope: official F-Droid only, official plus IzzyOnDroid, or user-configurable API template.
3. Built-in repo presets: official repo only in P1, with IzzyOnDroid as P2.
4. Archive handling: separate source mode/option for F-Droid archive repo, or treat archive as a normal repo URL.
5. Anti-Feature policy scope: display-only first, or display plus filter controls in the first release.
6. Detail sheet layout: one top-level `F-Droid 详情` entry with internal tabs by default; final tab order should follow the rendered AVD layout.

Resolved during P2:

- Start with local kotlinx.serialization JSON models and defer `org.fdroid:index-android:0.2.0` until signature and diff support need a deeper library spike.

## Recommended First Cut

Implement P1-P6 first with tracking, refresh, detail display, source filter, import/export, and card/menu integration. Gate managed install behind APK hash and signer confirmation in P7.

Use `org.fdroid:index-android:0.2.0` only after a small spike verifies API shape, APK size impact, R8 behavior, and compatibility with the project’s kotlinx serialization version. If the library pulls storage/database expectations that conflict with KeiOS custom stores, keep a narrow local v2 JSON model for repo tracking and revisit the library for signature/diff support.

Default user-facing selection should be `推荐版本优先`, falling back to `最高兼容 versionCode`. This matches official package API behavior when available and produces reliable tracking for generic v2 repos.

The first UI should expose F-Droid’s strongest unique value: versionCode precision, Anti-Features, repo trust, signer continuity, APK hash, and localized release notes. These signals make F-Droid feel like a first-class source instead of a Direct APK variant.
