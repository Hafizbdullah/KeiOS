# F-Droid Source Mode Findings

## Official F-Droid References

| Source | Finding |
|---|---|
| [All our APIs](https://f-droid.org/en/docs/All_our_APIs/) | The main site exposes a per-app API at `/api/v1/packages/<package>`, and the repository model centers on signed JSON indexes. v2 adds `entry.json`, `index-v2.json`, and diff files. |
| [New repo format](https://f-droid.org/2023/03/01/new-repo-format-faster-smaller-updates.html) | v2 uses JSON Merge Patch diffs so clients can fetch much smaller updates after the initial full index. The official blog calls out bandwidth, processing, hash verification, and low-RAM improvements. |
| [Setup an F-Droid App Repo](https://f-droid.org/en/docs/Setup_an_F-Droid_App_Repo/) | Any publisher can create a simple binary repo by placing APKs in a repo directory and running `fdroid update`; custom repos are first-class. |
| [Build Metadata Reference](https://f-droid.org/en/docs/Build_Metadata_Reference/) | Metadata includes changelog, anti-features, update check mode, current version, current version code, and archive policy concepts. |
| [Update Processing](https://f-droid.org/en/docs/Update_Processing/) | `fdroid checkupdates` updates metadata fields such as current version and version code; that informs what clients should consider recommended. |
| [Anti-Features](https://f-droid.org/en/docs/Anti-Features/) | F-Droid Anti-Features are explicitly user-facing labels. The client can expose them as warning pills, filters, and install-confirmation context. |
| [F-Droid index library docs](https://fdroid.gitlab.io/fdroidclient/libs/index/) | The official client publishes reusable index libraries with `org.fdroid.index`, `org.fdroid.index.v1`, and `org.fdroid.index.v2` packages. |
| [F-Droid download library docs](https://fdroid.gitlab.io/fdroidclient/libs/download/) | The official client publishes reusable download helpers; useful for later install/download trust work. |
| Maven Central `org.fdroid:index` / `org.fdroid:index-android` | Latest observed version on 2026-06-28 is `0.2.0`. |

## Live Repo Probe

Observed on 2026-06-28:

- `https://f-droid.org/repo/entry.json` points to `/index-v2.json`.
- The declared v2 index size is about 51 MB with 3998 packages.
- The entry file listed 10 diffs.
- The v2 package record for `org.fdroid.fdroid` includes manifest version code/name, SDK limits, native ABIs, signer SHA-256, APK file hash/size/path, localized release notes, and release channels.
- The F-Droid main repo metadata includes mirrors, categories, and anti-features.
- The package API for `org.fdroid.fdroid` returns `suggestedVersionCode` and package versions; this is a useful fast path for official or API-compatible repos.
- `signer-index.json` maps package names to official signer SHA-256 values.
- Official docs also point to search API, fdroiddata build metadata, build status/log APIs, and binary transparency logs.

## Third-Party Client References

| Client | Source | Relevant Lessons |
|---|---|---|
| Droid-ify | [Droid-ify/client](https://github.com/Droid-ify/client) | Popular F-Droid client with custom repos, background updates, offline sync after initial sync, and multiple install methods including Shizuku. |
| Neo Store | [NeoApplications/Neo-Store](https://github.com/NeoApplications/Neo-Store) | Feature-rich F-Droid client with fast repo sync, many built-in repos, filters, reproducible-build labels, privacy panels, Room-backed local data, and a Kotlin serialization v2 index model. |
| AuroraDroid | [AuroraOSS/AuroraDroid](https://gitlab.com/AuroraOSS/AuroraDroid) | Older but popular F-Droid client with repo management and multiple install methods; useful as historical confirmation of repo manager expectations. |
| Foxy Droid | [kitsunyan/foxy-droid](https://github.com/kitsunyan/foxy-droid) | Lean client focused on fast repository syncing, standard Android components, and minimal dependencies. |
| Obtainium | [ImranR98/Obtainium](https://github.com/ImranR98/Obtainium) | Most relevant tracking analogue: supports F-Droid, third-party F-Droid repos, IzzyOnDroid, suggested version code, highest version code selection, version regex filtering, APK regex filtering, and source-specific URL normalization. |

## KeiOS Existing Touch Points

| Area | Current File |
|---|---|
| Source mode enum and identity helpers | `feature-github/src/main/java/os/kei/feature/github/model/GitHubTrackModels.kt` |
| Cache source signatures | `feature-github/src/main/java/os/kei/feature/github/model/GitHubCheckCachePolicy.kt` |
| Release-check dispatch | `feature-github/src/main/java/os/kei/feature/github/domain/GitHubReleaseCheckService.kt` |
| Direct APK check source | `feature-github/src/main/java/os/kei/feature/github/domain/GitHubDirectApkReleaseCheckSource.kt` |
| Generic Git release asset source | `feature-github/src/main/java/os/kei/feature/github/domain/GitRepositoryReleaseAssetSource.kt` |
| APK manifest parsing fallback | `feature-github/src/main/java/os/kei/feature/github/data/remote/GitHubApkManifestReader.kt` |
| Track import/export serializers | `feature-github/src/main/java/os/kei/feature/github/data/local/GitHubTrackStoreSerializers.kt` |
| Track edit sheet | `app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditFormContent.kt` |
| Source-mode labels | `app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditLabels.kt` |
| More menu | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemMoreMenu.kt` |
| Header asset action | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHeaderActions.kt` |
| Asset panel | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemAssetPanel.kt` |
| Health card | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHealthCards.kt` |
| Top-bar action menu | `app/src/main/java/os/kei/ui/page/main/github/section/GitHubTopBarSection.kt` |
| Page filtering | `app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageContentStateDeriver.kt` |

## Planning Conclusions

- F-Droid should use a repo identity, package name, repo fingerprint/trust metadata, and optional selection strategy.
- The first implementation should refresh each repo once, then serve all tracked packages from the same repo snapshot.
- The package API can accelerate official F-Droid and Izzy-style sources, while repo index parsing remains the generic path.
- F-Droid version comparison should use `versionCode` as the primary ordering signal and `versionName` as display text.
- Anti-features, signer SHA-256, target/min SDK, native ABI, release channels, and localized release notes are valuable UI signals.
- Existing ignore modes can work by mapping F-Droid candidates to stable/pre-release style channels.
- Existing JSON import/export and WebDAV sync should gain new source fields in a backward-compatible shape.
- F-Droid should receive package/repo/trust/Anti-Feature detail sheets because its index metadata is much richer than GitHub release metadata.
- The existing source-specific menu model is a good fit: GitHub has Actions, Direct APK has remote health, F-Droid should have package detail, repo detail, trust/signature detail, and release notes.
- Top-bar filter and MCP filter surfaces need a `FdroidRepository` mode to keep source counts and automation aligned.
- Current KeiOS more-menu implementation already branches by source mode: GitHub shows Actions, Git platforms can show release notes, Direct APK reads release notes from resolved APK metadata. F-Droid should follow the same branching style and add one compact `F-Droid 详情` entry instead of several long top-level entries.
- Current header action already maps each source to the asset panel or external status URL. F-Droid should add a selected repo APK asset source with hash/signer gates rather than overloading GitHub release-asset loading.
- Current top-bar filters enumerate source modes directly. F-Droid needs a first-class `FdroidRepository` filter in app UI and MCP models.
- F-Droid notifications need source metadata in the record: repo display name, package, versionName, versionCode, trust state, and highest Anti-Feature warning. Actions history remains GitHub-specific; release update history can evolve into a broader source-aware update history.
