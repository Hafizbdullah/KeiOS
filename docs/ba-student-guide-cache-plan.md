# BA Student Guide Implemented Student Cache Plan

## Goal

Build a Student-only long-term cache model for implemented student guide detail pages.

The model uses GameKee catalog `created_at`, detail-page release date, and local first-seen time to classify implemented students into freshness tiers. Newly implemented students stay in a hot-update period, while mature students move to stable and long-term cache tiers. The page should open quickly from local cache, remain usable offline, and still make it clear when the visible content was cached and last validated.

NPC and satellite entries keep the existing timely-refresh behavior because those entries can change at any time and users usually open them for fresh information.

## Current Baseline

| Area | Current State | Main File |
| --- | --- | --- |
| Catalog cache | 12/24 hour incremental refresh, 3 day full refresh, manual full refresh | `app/src/main/java/os/kei/ui/page/main/student/catalog/BaGuideCatalogRefreshPolicy.kt` |
| Catalog entry model | Student entries already keep `createdAtSec`, `releaseDateSec`, `detailUrl`, `contentId`, and tab | `app/src/main/java/os/kei/ui/page/main/student/catalog/BaGuideCatalogFetch.kt` |
| Detail cache | V2 split MMKV payload, source URL index, memory LRU, legacy migration | `app/src/main/java/os/kei/ui/page/main/student/BaStudentGuideStore.kt` |
| Detail loading | Uses a single refresh interval currently tied to BA calendar settings | `app/src/main/java/os/kei/ui/page/main/student/page/state/BaStudentGuideRepository.kt` |
| Detail prefetch | Static image prefetch is stage-based and dispatcher-isolated | `app/src/main/java/os/kei/ui/page/main/student/page/state/BaStudentGuidePrefetchController.kt` |
| Cache diagnostics | BA Guide cache is already visible as a settings cache item | `app/src/main/java/os/kei/ui/page/main/settings/cache/BaCacheProviders.kt` |

## Product Rules

| Rule | Decision |
| --- | --- |
| Scope | Long-term cache policy applies only to `BaGuideCatalogTab.Student`. |
| Hot-update period | Implemented students with catalog creation age up to 7 days use cache as first-screen fallback and run background validation. |
| Local cache value | Detail cache remains written during hot-update period for offline reading, weak-network fallback, and smooth first paint. |
| User visibility | Detail page exposes cached time, last validation time, next automatic validation time, and freshness tier. |
| Manual refresh | Manual refresh always performs a network validation for the current student detail. |
| Failure behavior | Network failures keep the current cached content visible and record retry metadata. |
| Media behavior | Detail refresh should keep still-referenced media cache entries and clean only stale media when feasible. |
| Catalog behavior | Catalog refresh continues to discover new students and maintain `createdAtSec` / `releaseDateSec` signals. |

## Freshness Tiers

Use the most conservative age signal available from `createdAtSec`, `releaseDateSec`, and `firstSeenAtMs`.

| Tier | Age Signal | Automatic Validation | Cache Treatment |
| --- | ---: | ---: | --- |
| Hot update | 0-7 days | Open triggers background validation, with 30-60 minute automatic debounce | Cache is first-screen fallback and failure fallback |
| Completion | 8-30 days | 12 hours | Cache-first, validate after expiry |
| Stable | 31-90 days | 3 days | Cache-first, low-frequency validation |
| Long-term | 91-180 days | 14 days | Local-first, bandwidth-saving validation |
| Archived | 180+ days and multiple unchanged validations | 30 days | Long-lived local cache with manual refresh available |
| Unknown | Missing catalog and detail dates | 24 hours | Refresh detail, then reclassify after dates are known |

## Detail Cache Metadata

Add a lightweight metadata index beside the detail payload. This index supports policy decisions and settings diagnostics without decoding every large detail payload.

Storage decision after dependency audit:

| Data Type | Storage | Reason |
| --- | --- | --- |
| Current selected guide URL, small settings, favorites | Existing MMKV stores | Small key-value state, existing sync/import code already uses this model. |
| Implemented student detail payload | App-private files under `filesDir` | Long-term cache can grow across many students; file IO avoids large MMKV values and keeps payload reads on demand. |
| Implemented student detail metadata/index | File-backed JSON index plus per-entry meta file | Structured scans, cache diagnostics, and future migration to SQLite/Room can sit behind one store interface. |
| Temporary media files | Existing cache-dir file store plus index | Regenerable media belongs in cache storage and can be trimmed independently. |

No new database dependency is planned for P0-P2. The current dependency set already includes `kotlinx.serialization-json` through `core-json` and the app module, and has no Room/SQLDelight/DataStore baseline. A file-backed repository keeps the dependency surface small while fitting this cache size. If metadata queries become complex after P2, the same store interface can move to Room/SQLite.

```kotlin
data class BaGuideStudentDetailCacheMeta(
    val schema: Int,
    val sourceUrl: String,
    val contentId: Long,
    val tab: BaGuideCatalogTab,
    val catalogCreatedAtSec: Long,
    val releaseDateSec: Long,
    val firstSeenAtMs: Long,
    val cachedAtMs: Long,
    val lastValidatedAtMs: Long,
    val lastChangedAtMs: Long,
    val nextAutoRefreshAtMs: Long,
    val freshnessTier: BaGuideStudentDetailFreshnessTier,
    val contentHash: String,
    val unchangedValidationCount: Int,
    val failureCount: Int,
    val lastFailureAtMs: Long,
    val nextRetryAtMs: Long,
)
```

### Field Use

| Field | Use |
| --- | --- |
| `sourceUrl` | Main key, aligned with current detail cache. |
| `contentId` | Links detail cache to catalog entries and release date index. |
| `tab` | Guards Student-only policy. |
| `catalogCreatedAtSec` | Strong signal for newly created GameKee WIKI pages. |
| `releaseDateSec` | Stable signal for student implementation age. |
| `firstSeenAtMs` | Local fallback when remote dates are incomplete. |
| `cachedAtMs` | User-visible cache write time. |
| `lastValidatedAtMs` | User-visible latest network validation time. |
| `lastChangedAtMs` | Detects content that has settled over time. |
| `nextAutoRefreshAtMs` | Drives page hints and automatic validation. |
| `contentHash` | Detects unchanged content without unnecessary payload churn. |
| `failureCount` / `nextRetryAtMs` | Controls retry backoff under weak network. |

## Runtime Flow

### Opening a Student Detail

1. Normalize the source URL.
2. Resolve catalog context by `contentId` or normalized `detailUrl`.
3. Load cached detail snapshot and lightweight metadata.
4. Display complete cached detail immediately when available.
5. Evaluate freshness tier and retry metadata.
6. Launch background validation when the tier requires validation.
7. On success:
   - Same content hash: update validation time and next validation time.
   - Changed content hash: save new detail, update cache time, changed time, and validation time.
8. On failure:
   - Keep visible cached content.
   - Update failure count, failure time, and next retry time.
   - Emit a lightweight UI event if the user explicitly requested refresh.

### Opening an NPC or Satellite Detail

1. Keep current timely refresh behavior.
2. Keep existing cache fallback behavior.
3. Keep Student-only metadata out of the NPC/satellite path.

### Catalog Refresh Linkage

| Trigger | Work |
| --- | --- |
| Catalog incremental refresh | Update known Student catalog metadata and keep existing detail meta aligned. |
| Catalog full refresh | Detect new implemented students and assign `firstSeenAtMs`. |
| Detail refresh success | Extract release date from detail rows and upsert catalog release date index. |
| Entry URL change | Preserve old cache as orphan candidate, then link the new URL to the same `contentId` when safe. |

## UI Plan

### Detail Page Cache Status

Expose a compact cache status action from the detail page action area or a small details sheet.

| State | Example Copy |
| --- | --- |
| Hot update | `缓存于 2026-07-01 10:24 · 热更新期` |
| Validated recently | `上次校验 12 分钟前` |
| Next validation | `下次自动校验 42 分钟后` |
| Refresh failed | `当前显示缓存内容 · 上次校验失败 5 分钟前` |
| Long-term | `长期缓存 · 上次校验 6 天前` |

Actions:

- `刷新当前学生`
- `清除此学生缓存`
- `查看缓存说明`

### Catalog Page Settings

Add an implemented-student cache sheet from the catalog page dropdown.

| Section | Content |
| --- | --- |
| Strategy | Explain hot-update, stable, long-term, and archived tiers. |
| Counts | Implemented detail cache count, hot-update count, long-term count, archived count. |
| Freshness | Latest cache write, latest validation, next scheduled validation. |
| Cleanup | Clear implemented student detail cache, clear orphan detail cache. |

### Settings Cache Diagnostics

Extend the BA Guide cache summary.

| Metric | Example |
| --- | --- |
| Implemented student details | `128 项` |
| Hot-update details | `3 项` |
| Long-term details | `94 项` |
| Offline-readable details | `128 项` |
| Latest validation | `12 分钟前` |
| Media cache | `45 个文件 / 120 MB` |

## Priority Table

| Priority | ID | Status | Work | Validation |
| --- | --- | --- | --- | --- |
| P0 | S0 | Done | Add `BaGuideStudentDetailFreshnessTier` and `BaGuideStudentDetailCachePolicy` with Student-only guards. | Unit tests for tier selection and NPC/satellite guard passed. |
| P0 | S1 | Done | Add file-backed detail cache metadata index with lazy migration from existing V2 detail cache. | File-backed meta read/write, tier counts, Student-only guard, existing snapshot meta-builder, and repository lazy migration tests passed. |
| P0 | S2 | Done | Route `BaStudentGuideRepository.loadGuide()` through Student detail policy while keeping NPC/satellite timely refresh. | Repository tests cover Student fresh cache, manual refresh, and NPC/satellite legacy interval. |
| P0 | S3 | Done | Implement hot-update behavior: cached first paint, background validation, 30-60 minute automatic debounce. | Repository tests cover expired hot-update cache first paint and forced background validation; ViewModel now starts silent background validation. |
| P0 | S4 | Done | Expose detail cache status in ViewModel/UI state for current student. | Repository meta is now surfaced as structured `BaStudentGuideCacheStatusUiState`; targeted tests and `:app:compileDebugKotlin` passed. |
| P1 | S5 | Planned | Update catalog refresh linkage: maintain `firstSeenAtMs`, sync `createdAtSec`, and update existing meta. | Catalog repository/store tests for new Student discovery and metadata alignment. |
| P1 | S6 | Planned | Update detail refresh write path: compute hash, update validation/change timestamps, and backfill release date. | Tests for unchanged hash, changed hash, release date extraction, catalog index upsert. |
| P1 | S7 | Planned | Add retry backoff and singleflight per source URL. | Coroutine tests for concurrent loads, retry delay, cancellation. |
| P1 | S8 | Planned | Add detail page cache status sheet and manual clear current-student cache action. | Compose smoke test or AVD screenshot pass. |
| P1 | S9 | Done | Extend settings cache diagnostics with Student detail tier counts. | BA Guide cache summary now includes implemented detail counts, hot-update/long-term/archived tier counts, file-cache bytes, and clears the file-backed detail metadata. |
| P2 | S10 | Planned | Add media cache differential cleanup for detail refreshes. | Tests for retained referenced media and removed stale media. |
| P2 | S11 | Planned | Add orphan Student detail cache detection and cleanup. | Store tests for URL changes and orphan cleanup. |
| P2 | S12 | Planned | Add optional low-priority validation for favorites and recently viewed implemented students. | Scheduler tests for bounded concurrency and cancellation. |
| P2 | S13 | Planned | Run AVD validation: cached old student, hot-update student, manual refresh failure, offline opening. | AVD log, screenshot, and no app error logs. |
| P3 | S14 | Planned | Consider offline cache pack or pre-cache for selected favorites. | Product review after P0-P2 land. |
| P3 | S15 | Planned | Consider WebDAV/import-export coverage for detail cache metadata. | Sync design review after metadata stabilizes. |

## Implementation Notes

- Keep detail data loading main-safe. Repository suspend functions can be called from the main thread and switch work onto injected dispatchers.
- Keep heavy parsing on `AppDispatchers.uiDerivation`.
- Keep network and MMKV work off the main thread.
- Use `StateFlow` for persistent UI state and `SharedFlow` for refresh/clear result events.
- Use `CancellationException` rethrow in every broad catch block.
- Add `ensureActive()` or `yield()` in any future large metadata scan.
- Keep UI strings in resources when the implementation lands.
- Keep the existing V2 detail payload format unless metadata work proves a schema bump is necessary.

## Test Plan

| Test Group | Required Cases |
| --- | --- |
| Policy | Tier by created age, release age, first-seen fallback, unknown age, Student-only guard. |
| Store | Meta persistence, lazy migration, cache counts, latest validation, orphan detection. |
| Repository | Cache hit, stale background validation, manual refresh, failure fallback, singleflight. |
| Catalog | New Student discovery, metadata alignment, release date backfill. |
| UI state | Cache status labels, next validation display, explicit refresh result. |
| Media | Differential cleanup and cache retention. |
| AVD | Offline old student, hot-update student, weak-network refresh failure, settings diagnostics. |

## Progress Log

| Date | Status | Notes |
| --- | --- | --- |
| 2026-07-01 | Planned | Initial plan written. Scope is limited to implemented students. Hot-update period uses cache as first-screen fallback and runs background validation. |
| 2026-07-01 | Storage refined | Dependency audit found MMKV, app-private file IO, `kotlinx.serialization-json`, Coil 3, OkHttp/Ktor, and existing cache-dir media stores. Long-term implemented-student details should use app-private files with file-backed metadata/index; MMKV stays for small settings and existing lightweight stores. |
| 2026-07-01 | P0 S0 done, S1 started | Added Student-only detail freshness policy, file-backed metadata store, content hash, and storage tests. Targeted unit tests passed: `BaGuideStudentDetailCachePolicyTest`, `BaStudentGuideStoreDetailMetaTest`. |
| 2026-07-01 | P0 S1-S3 done | `BaStudentGuideRepository.loadGuide()` now resolves catalog context, lazily writes Student metadata from existing cache, keeps NPC/satellite on legacy cadence, returns cached Student first paint when validation is due, and lets ViewModel run silent background validation. Targeted unit tests passed with `:app:compileDebugKotlin`. |
| 2026-07-01 | P0 S4 done | Added structured cache status UI state for current student detail: cached time, validation time, next auto-refresh, freshness tier, background validation flag, and retry/failure metadata. |
| 2026-07-01 | P1 S9 done | Settings cache diagnostics now include the file-backed implemented-student detail cache, tier counts, latest cache/validation time, and file-cache bytes. |

## Open Decisions

| Topic | Proposed Default | Decision Needed During Implementation |
| --- | --- | --- |
| Hot-update debounce | 60 minutes | Tune after AVD and real usage evidence. |
| Archived tier threshold | 180 days and multiple unchanged validations | Decide exact unchanged count, likely 3. |
| Detail status placement | Action sheet | Confirm visual fit after first UI pass. |
| Meta storage location | `filesDir/ba_student_guide_detail_cache/v1` | Use versioned file-backed JSON index and per-entry `meta.json`; keep MMKV only for existing small guide settings/current URL. |
