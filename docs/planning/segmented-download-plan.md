# Segmented Download Plan

## Goal

Add a clean-room segmented HTTP downloader for high-value KeiOS download paths.

The first delivery targets large files where HTTP Range is usually available:

- GitHub managed install direct APK downloads.
- GitHub Actions APK artifact ZIP downloads.
- GameKee large media file downloads.

## Scope

| Area | Status | Target |
| --- | --- | --- |
| GitHub managed install direct APK | Planned | Download to app-private temp APK with segmented Range, then stream the completed file into `PackageInstaller.Session`. |
| GitHub Actions artifact ZIP | Planned | Download archive temp ZIP with segmented Range, then reuse current ZIP selection and session-writing flow. |
| GameKee large media | Planned | Download large media to `.part` with segmented Range and atomic rename. |
| WebDAV sync text downloads | Later independent topic | Current text sync path stays with dav4jvm single request. |
| Coil/image decode cache | Later independent topic | Current Coil/GameKee image flow stays focused on cache and decode behavior. |

## Module Boundary

Implement the high-speed downloader as an independent Gradle module.

Proposed module:

`include(":core-download")`

Module identity:

| Item | Decision |
| --- | --- |
| Module name | `:core-download` |
| Namespace | `os.kei.core.download` |
| Role | Reusable high-throughput download engine and download-domain models. |
| Placement | `core-download/src/main/java/os/kei/core/download/segmented/` |
| Test placement | `core-download/src/test/java/os/kei/core/download/segmented/` |

Dependency direction:

| Module | Dependency |
| --- | --- |
| `:core-download` | `:core-io`, OkHttp, kotlinx-coroutines-core |
| `:feature-github` | `:core-download` for managed install downloads |
| `:app` | `:core-download` for current app-owned GameKee media code |
| Future `:feature-ba` migration | `:feature-ba` can depend on `:core-download` after BA media code moves out of `:app` |

Boundary rules:

- `:core-download` owns Range probe, segmented scheduling, retry, progress aggregation, temp-file lifecycle, and single-stream fallback.
- `:core-download` receives `OkHttpClient`, headers, options, and dispatcher from callers.
- `:core-download` depends only on core transport primitives and keeps app, feature, notification, PackageInstaller, GameKee, and UI concerns in adapters.
- Feature adapters translate `SegmentedDownloadProgress` into existing notification/UI progress models.
- Feature adapters own business headers such as GitHub `Accept`, GitHub token, GameKee `Referer`, and User-Agent selection.
- The module keeps GPL clean-room implementation constraints local and reviewable.

## Dependency Version Policy

Use the latest stable dependency versions verified from Maven metadata before implementation.

Maven metadata checked on 2026-07-09:

| Dependency | Planned Version | Evidence | Source |
| --- | ---: | --- | --- |
| `com.squareup.okhttp3:okhttp` | `5.4.0` | Maven metadata `latest` and `release` are `5.4.0`; project already uses `5.4.0`. | `https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml` |
| `com.squareup.okhttp3:mockwebserver` | `5.4.0` | Maven metadata `latest` and `release` are `5.4.0`; keep tests aligned with OkHttp runtime. | `https://repo1.maven.org/maven2/com/squareup/okhttp3/mockwebserver/maven-metadata.xml` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.11.0` | Maven metadata `latest` and `release` are `1.11.0`; project already uses `1.11.0`. | `https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.11.0` | Maven metadata `latest` and `release` are `1.11.0`; keep tests aligned with coroutines runtime. | `https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml` |
| `org.jetbrains.kotlin:kotlin-test` | `2.4.0` | Maven metadata has newer prerelease entries such as `2.4.10-RC2` and `2.4.20-Beta1`; use the latest stable that matches the root Kotlin plugin. | `https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml` |

Version rules:

- Re-check Maven metadata at the start of P1 and update the plan if a newer stable release exists.
- Keep OkHttp runtime and MockWebServer test versions identical.
- Keep `kotlin-test` aligned with the root Kotlin plugin version.
- Keep coroutines runtime and coroutines test versions identical.
- Inherit root Android Gradle Plugin and Kotlin plugin versions in `:core-download`.
- Prefer shared version constants or a later version-catalog cleanup if the project centralizes dependency versions.

Metadata check commands:

```bash
curl -fsSL https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml | rg '<latest>|<release>|<lastUpdated>'
curl -fsSL https://repo1.maven.org/maven2/com/squareup/okhttp3/mockwebserver/maven-metadata.xml | rg '<latest>|<release>|<lastUpdated>'
curl -fsSL https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml | rg '<latest>|<release>|<lastUpdated>'
curl -fsSL https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml | rg '<latest>|<release>|<lastUpdated>'
curl -fsSL https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml | rg '<version>2\\.' | tail -40
```

## Current Baseline

| Path | Current Behavior | File |
| --- | --- | --- |
| GitHub direct APK managed install | Single OkHttp stream writes into install session and temp APK copy. | `feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt` |
| GitHub Actions ZIP managed install | Single OkHttp stream writes full ZIP to temp file, then `ZipFile` extracts APK. | `feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt` |
| GameKee media download | Single OkHttp stream writes `.part`, then renames to target file. | `app/src/main/java/os/kei/feature/ba/data/remote/GameKeeFetchHelper.kt` |
| Remote APK metadata scan | Uses precise HTTP Range requests and validates `Content-Range`. | `feature-github/src/main/java/os/kei/feature/github/data/apk/RemoteZipEntryReader.kt` |
| User-visible GitHub asset download | Uses system `DownloadManager` or external downloader package. | `app/src/main/java/os/kei/core/download/AppPrivateDownloadManager.kt` |

## Benchmark Fixtures

Benchmark both discovery modes for each GitHub download family. The optimizer
must improve the paths real users hit in Atom/NightlyLink mode and in
GitHubApiToken mode.

Release APK fixed sample:

| Field | Value |
| --- | --- |
| Repository | `getpaseo/paseo` |
| Release | `https://github.com/getpaseo/paseo/releases/tag/v0.1.104` |
| Asset name | `paseo-v0.1.104-android.apk` |
| Asset ID | `470196887` |
| Asset API URL | `https://api.github.com/repos/getpaseo/paseo/releases/assets/470196887` |
| Atom/direct URL | `https://github.com/getpaseo/paseo/releases/download/v0.1.104/paseo-v0.1.104-android.apk` |
| Size | `183037443` bytes, about `174.56 MiB` |
| Digest | `sha256:f98520a1d8c9df9fb54c11505fa5af32cd108b0b3581927a596f40d9fc5191d5` |
| Header check | Direct URL redirects to `release-assets.githubusercontent.com` and the final response advertises `Accept-Ranges: bytes`. |

Release benchmark matrix:

| Mode | URL resolution | Required comparison |
| --- | --- | --- |
| AtomFeed | Use the `browser_download_url` / direct release asset URL available from Atom or HTML parsing. | Single-stream direct URL vs segmented direct URL. |
| GitHubApiToken | Resolve the asset through the GitHub release asset API using `Accept: application/octet-stream` and the configured token, then follow the signed redirect. | Single-stream API-resolved URL vs segmented API-resolved URL. |

Actions benchmark matrix:

| Mode | URL resolution | Required comparison |
| --- | --- | --- |
| NightlyLink | Use the current `GitHubActionsLookupStrategyOption.NightlyLink` path backed by `nightly.build` public pages. | Single-stream nightly artifact ZIP vs segmented nightly artifact ZIP. |
| GitHubApiToken | Use the current `GitHubActionsLookupStrategyOption.GitHubApiToken` path backed by GitHub Actions artifact API URLs. | Single-stream API artifact ZIP vs segmented API artifact ZIP. |

Actions benchmark selection rules:

- Select one non-expired Android-like artifact ZIP from the same repository,
  workflow, run, and artifact name for both modes whenever both modes can
  resolve it.
- Prefer artifacts at least `50 MiB`; prefer `100 MiB+` when available.
- Record run ID, workflow name, artifact ID, artifact name, artifact size,
  strategy, resolved final URL host, and expiration state before downloading.
- Re-resolve the artifact URL immediately before each timed run because GitHub
  Actions artifact and release-asset redirect URLs are short-lived.
- Treat NightlyLink and GitHubApiToken as separate benchmark rows even when they
  eventually redirect to the same storage host.

Benchmark metrics:

| Metric | Reason |
| --- | --- |
| Total elapsed time | Primary user-visible result. |
| Average throughput | Main before/after speed signal. |
| Tail time after 90% | Captures end-of-download slowdown. |
| Resolved content length | Confirms both modes download the same file size. |
| Range support result | Confirms segmented activation or fallback reason. |
| Final URL host | Distinguishes `github.com`, `release-assets.githubusercontent.com`, and Actions storage redirects. |
| Active connections | Confirms effective parallelism. |
| Retry count and status codes | Captures 429/5xx/range failures. |
| Temp-file cleanup result | Keeps managed install and benchmark reruns safe. |

Benchmark run rules:

- Use the same device, network, app build, dispatcher settings, and connection
  defaults for all rows in one comparison set.
- Run each row at least three times after one untimed warm-up resolution.
- Keep raw per-run rows; compare medians for the decision.
- Clear only the downloader temp files between runs; leave HTTP/TLS connection
  behavior representative of normal app usage.
- Capture both current single-stream baseline and new segmented downloader
  results before changing default rollout behavior.

## Reference Summary

`piko` is a GPL-3.0 Go downloader in `.tmp/piko`, currently at commit `4f5ba27`.

Useful behavior-level ideas:

- Probe Range support with `HEAD` and `GET Range: bytes=0-0`.
- Enable parallel workers only when `Content-Length > 0` and byte ranges return `206`.
- Adapt each worker's part size from observed throughput.
- Allocate parts from both file head and file tail.
- Let idle workers split remaining bytes from active slow parts.
- Requeue failed parts from the last written offset.
- Use delayed retry for HTTP 429 and bounded retry budgets for failed parts.
- Detect persistently slow range connections against active peer average.

Clean-room rule:

- Use the piko repository only as behavioral research.
- Implement Kotlin data models, scheduler, error types, constants, tests, and docs independently inside KeiOS.
- Write KeiOS-specific tests from expected HTTP behavior and product requirements.

## Range Constraint

The high-speed path is Range-only.

Activation requirements:

| Requirement | Rule |
| --- | --- |
| URL scheme | `https` for GitHub managed install and external asset safety. |
| Size | Positive remote size from `Content-Length` or `Content-Range`. |
| Range probe | `GET Range: bytes=0-0` returns `206` with valid `Content-Range`. |
| File size threshold | Start with segmented download only for files at least 8 MiB. |
| Target storage | App-private temp file supports random-access writes. |

Fallback behavior:

| Probe Result | Behavior |
| --- | --- |
| Range supported | Segmented download writes temp file and validates final byte count. |
| Range unavailable | Existing single-stream download path handles the file. |
| Range response invalid | Existing single-stream download path handles the file and logs probe failure. |
| Unknown size | Existing single-stream download path handles the file. |

## Proposed API

Create a Kotlin downloader owned by `:core-download`, with a small reusable core and feature adapters.

Suggested package:

`core-download/src/main/java/os/kei/core/download/segmented/`

Core models:

```kotlin
data class SegmentedDownloadRequest(
    val url: String,
    val outputFile: File,
    val headers: Map<String, String> = emptyMap(),
    val fileNameHint: String = "",
)

data class SegmentedDownloadOptions(
    val minParallelSizeBytes: Long = 8L * 1024L * 1024L,
    val initialPartSizeBytes: Long = 4L * 1024L * 1024L,
    val maxConnections: Int = 4,
    val maxRetriesPerPart: Int = 3,
    val stallTimeoutMs: Long = 15_000L,
)

data class SegmentedDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val activeConnections: Int,
    val parallel: Boolean,
)

data class SegmentedDownloadResult(
    val outputFile: File,
    val totalBytes: Long,
    val parallel: Boolean,
    val rangeSupported: Boolean,
    val finalUrl: String,
)
```

Primary entry:

```kotlin
suspend fun downloadToFile(
    request: SegmentedDownloadRequest,
    options: SegmentedDownloadOptions,
    onProgress: suspend (SegmentedDownloadProgress) -> Unit,
): SegmentedDownloadResult
```

Client shape:

```kotlin
class SegmentedDownloadClient(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
)
```

Feature adapters choose dispatchers:

| Adapter | Dispatcher |
| --- | --- |
| GitHub managed install | `AppDispatchers.githubNetwork` |
| GameKee media | `AppDispatchers.baFetch` or a future media-download dispatcher after evidence shows contention |

Keep OkHttp ownership explicit. The downloader receives an `OkHttpClient` from the feature caller so GitHub downloads can keep GitHub headers, timeouts, redirect policy, and shared connection pool behavior.

Build file shape:

```kotlin
plugins {
    id("com.android.library")
}

android {
    namespace = "os.kei.core.download"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    val okhttpVersion = "5.4.0"
    val coroutinesVersion = "1.11.0"
    val kotlinVersion = "2.4.0"

    implementation(project(":core-io"))
    api("com.squareup.okhttp3:okhttp:$okhttpVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
}
```

## Scheduler Design

| Component | Responsibility |
| --- | --- |
| `RangeProbe` | Resolve final URL, size, file name hint, Range support, and validation errors. |
| `PartScheduler` | Hand out byte ranges, adapt part size, maintain active parts, and split slow tail work. |
| `RangeWorker` | Execute one byte-range request at a time, validate `206` and `Content-Range`, write bytes into `RandomAccessFile` or `FileChannel`. |
| `RetryPolicy` | Requeue retryable part failures, delay 429, cap retry budget, and fail the download after exhausted retries. |
| `ProgressAggregator` | Aggregate atomic completed bytes and emit throttled progress for notifications/UI. |
| `SegmentedDownloadClient` | Coordinate probe, file lifecycle, workers, fallback, and cleanup. |

Scheduler rules:

- Start with `initialPartSizeBytes`.
- Alternate head and tail allocations while unassigned bytes remain.
- In the tail window, split remaining bytes into at least `connections * 4` smaller parts.
- When workers become idle and no unassigned bytes remain, split the largest active remaining part.
- Requeue failed bytes from the last confirmed written offset.
- Clamp active connections by file size and part size.
- Emit final progress after all ranges finish and final byte count matches expected size.

## Kotlin Coroutine Execution Model

Kotlin coroutines fit the segmented downloader coordinator role. The actual
OkHttp response reads and file writes still occupy a bounded set of platform IO
threads when using synchronous OkHttp and `FileChannel`/`RandomAccessFile`.

```kotlin
suspend fun runWorkers(
    connections: Int,
    scheduler: PartScheduler,
) = coroutineScope {
    repeat(connections) { workerId ->
        launch {
            while (isActive) {
                val part = scheduler.nextPart(workerId) ?: break
                downloadPart(workerId, part)
            }
        }
    }
}
```

Execution rules:

| Topic | Plan |
| --- | --- |
| Worker lifecycle | Start workers inside `coroutineScope`; one parent job owns probe, temp file, workers, progress, and cleanup. |
| Scheduler state | Protect part assignment, active-part mutation, retry queues, and tail splitting with `Mutex`. |
| Progress state | Use `AtomicLong` for completed bytes and a throttled progress emitter for UI/notification callbacks. |
| OkHttp cancellation | Use the existing cancellable call pattern so coroutine cancellation invokes `Call.cancel()`. |
| File writes | Use `RandomAccessFile` or `FileChannel` with per-part offsets; serialize scheduler state while each worker writes its own assigned range. |
| Thread control | Keep connection count tied to effective HTTP connections because each synchronous OkHttp stream occupies a real IO thread. |
| Structured cleanup | Parent cancellation closes calls, closes the file channel, deletes `.part`, and returns control through existing failure paths. |

Implementation guidance:

- Prefer `coroutineScope` and `launch` over ad-hoc scopes.
- Prefer `withContext(dispatcher)` at the downloader boundary.
- Keep `PartScheduler` deterministic and unit-testable without OkHttp.
- Keep `RangeWorker` thin: build request, validate response, write bytes, report offset.
- Keep progress emission separate from worker loops so notification cadence stays stable.
- Keep P1 public API suspend-first; feature code calls it from existing coroutine flows.
- Evaluate a suspending OkHttp call wrapper for response acquisition; keep response body streaming and random-access file writes on the bounded download dispatcher.
- Keep worker count near effective connection count; hundreds of range workers would mainly increase scheduler, server, and retry pressure.

Runtime notes:

| Topic | Plan Impact |
| --- | --- |
| Kotlin coroutines | Coroutines compile into continuations/state machines and run through dispatchers, which gives structured concurrency, cancellation, and testable scheduling. |
| Blocking streams | Synchronous OkHttp reads and file writes still need real carrier threads, so the module must own worker limits and dispatcher injection. |
| Go goroutines | Go's runtime-managed goroutines and network poller make cheap blocking-style network workers ergonomic; this is useful inspiration for scheduler shape, retry, and cancellation ergonomics. |
| Java virtual threads | Java virtual threads are the JVM conceptual match for cheap blocking tasks. They are a future backend research item after Android device/runtime verification. |
| Android SDK status | Local `android-37.1` and `android-36.1` sources include `Thread.startVirtualThread`, `Thread.ofVirtual`, `Thread.isVirtual`, and hidden `VirtualThread` scheduler code behind `Flags.virtualThreadImplV1()`. Local `android-35` source has no `VirtualThread.java`, so P1 stays compatible with min SDK 35. |

Runtime references:

| Topic | Source |
| --- | --- |
| Kotlin coroutine basics | `https://kotlinlang.org/docs/coroutines-basics.html` |
| Java virtual threads | `https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html` |
| Android 37.1 virtual-thread API gate | `/Users/voyager/Library/Android/sdk/sources/android-37.1/java/lang/Thread.java` |
| Android 37.1 hidden virtual-thread scheduler | `/Users/voyager/Library/Android/sdk/sources/android-37.1/java/lang/VirtualThread.java` |
| epoll/io_uring discussion reference | `https://sibexi.co/posts/epoll-vs-io_uring/` |

## Kernel I/O Scope

Keep P1 on portable JVM/Android APIs.

| Topic | Decision |
| --- | --- |
| Network polling | Treat epoll and platform network polling as OkHttp/Android runtime details. Measure observable download behavior instead of choosing kernel polling APIs directly. |
| File IO | Use blocking `FileChannel` or `RandomAccessFile` on the injected bounded dispatcher. |
| io_uring | Leave native io_uring integration as a later research path after Kotlin implementation metrics show a clear file-IO bottleneck. |
| Native backend | Leave Go/Rust/C native downloader backends out of P1 because they add ABI packaging, capability checks, cancellation bridging, GPL review, and test complexity. |

Expected bottlenecks for the first implementation are server/CDN Range behavior,
rate limits, conservative connection counts, retry policy, and tail scheduling.
File IO should be measured before adding native kernel-API work.

## Integration Plan

### P0 Plan Setup

| Status | Output |
| --- | --- |
| Done | `docs/planning/segmented-download-plan.md` |

### P1 Core Downloader Foundation

Add `:core-download` and implement the clean-room segmented downloader behind tests.

Files:

- `settings.gradle.kts`
- `core-download/build.gradle.kts`
- `core-download/src/main/java/os/kei/core/download/segmented/*`
- `core-download/src/test/java/os/kei/core/download/segmented/*`

Tests:

- Range probe succeeds with `206` and valid `Content-Range`.
- Probe falls back when server ignores Range and returns `200`.
- Probe falls back on missing or malformed `Content-Range`.
- Segmented download writes all bytes in correct order.
- Worker retries after EOF mid-range and resumes from offset.
- 429 requeues with delay policy.
- Cancellation removes temp file.
- Final file rename is atomic from `.part`.
- Progress reaches total bytes once.
- Parent coroutine cancellation cancels active OkHttp calls.
- Worker count never exceeds effective connection count.
- Occupied IO thread count stays near active connection count in a large-file test run.
- Virtual-thread backend research is documented separately after SDK 36.1/37.1 runtime verification.
- `PartScheduler` tail splitting stays deterministic under a fake clock.
- `PartScheduler` can be tested without network or file IO.

### P2 GitHub Actions ZIP Download Bridge

Replace `downloadToTempFile()` in `GitHubInstallSessionWriter` with the segmented downloader adapter.

Reason:

Actions ZIP already downloads to a temp file before session staging, so it is the lowest-risk integration path.

Acceptance:

- `:feature-github` depends on `:core-download`.
- Range-supported ZIP uses segmented downloader.
- Range-unavailable ZIP uses existing single-stream behavior through the downloader fallback.
- NightlyLink and GitHubApiToken Actions artifact downloads both route through the same `:core-download` adapter after each mode resolves its own URL.
- Benchmark logging records Actions strategy, workflow run ID, artifact ID, artifact name, artifact size, resolved final URL host, parallel/fallback mode, elapsed time, average throughput, tail time, and retry count.
- Existing `ZipFile` APK selection flow remains unchanged.
- Notifications continue using `GitHubInstallProgressEmitter`.
- Existing managed install result types remain unchanged.

Tests:

- Actions archive install stages progress through downloading and staging.
- ZIP content remains readable after segmented download.
- Invalid Range response falls back to single stream.
- Cancellation cleans temp ZIP.
- NightlyLink and GitHubApiToken modes can each produce a benchmark row for the same selected artifact when both modes resolve it.

### P3 GitHub Direct APK Managed Install Bridge

Change direct APK managed install to download into app-private temp APK first, then stream completed file into `PackageInstaller.Session`.

Reason:

Segmented download needs random-access temp storage. The current path already creates a temp APK for metadata scanning, so this phase consolidates download and scan around the completed temp file.

Acceptance:

- Direct APK with Range uses segmented downloader.
- Direct APK without Range uses single-stream temp download.
- AtomFeed and GitHubApiToken release downloads both route through the same `:core-download` adapter after each mode resolves its own URL.
- Paseo v0.1.104 Android APK is available as a fixed release benchmark sample for both Atom/direct and GitHubApiToken API-resolved downloads.
- Benchmark logging records release strategy, asset ID, asset name, expected size, resolved final URL host, parallel/fallback mode, elapsed time, average throughput, tail time, and retry count.
- Temp APK is reused for archive info reading.
- Session write runs from completed temp APK.
- Session progress keeps current notification wording and stage model.

Tests:

- Direct APK install writes expected session bytes.
- Archive info scan reads the downloaded temp APK.
- Download failure deletes temp APK and abandons session through existing failure path.
- Paseo v0.1.104 Android APK benchmark produces rows for AtomFeed single-stream, AtomFeed segmented, GitHubApiToken single-stream, and GitHubApiToken segmented.

### P4 GameKee Large Media Bridge

Use the segmented downloader for large GameKee media downloads.

Activation:

- `:app` depends on `:core-download` while GameKee media code still lives in the app module.
- Only media downloads that use `GameKeeNetworkClient.downloadToFile`.
- Use existing URL normalization, Referer, Accept-Language, and User-Agent selection.
- Keep current `.part` temp naming and final rename behavior.

Acceptance:

- Large media with Range uses segmented downloader.
- Small media and failed Range probe use current single-stream behavior.
- Progress callbacks keep `(downloadedBytes, totalBytes)`.
- Existing GameKee logging records parallel/fallback mode.

Tests:

- Media file bytes match expected content.
- Referer/User-Agent headers are preserved.
- Small file path uses single-stream fallback.

### P5 Runtime Tuning

Tune defaults from local evidence.

Initial defaults:

| Setting | Value |
| --- | ---: |
| GitHub max connections | 4 |
| GameKee max connections | 3 |
| Initial part size | 4 MiB |
| Minimum parallel size | 8 MiB |
| Stall timeout | 15 seconds |
| Max retries per part | 3 |

Evidence to capture:

- Average throughput for single-stream vs segmented on the same URL.
- Paseo v0.1.104 Android APK benchmark rows for AtomFeed direct URL and GitHubApiToken API-resolved URL.
- GitHub Actions artifact benchmark rows for NightlyLink/nightly.build and GitHubApiToken API modes.
- Tail duration after 90% complete.
- 429 and 5xx frequency.
- Cancellation cleanup success.
- Memory footprint while downloading 100-300 MiB APK/ZIP files.
- Active coroutine count and occupied IO thread count during large downloads.
- Whether Android 36.1/37.1 devices expose usable virtual-thread runtime behavior for app code.

## Verification Checklist

- Targeted unit tests for segmented downloader pass.
- Existing `RemoteZipEntryReaderTest` stays green.
- Targeted GitHub install tests pass.
- Targeted GameKee media download tests pass.
- `:app:testDebugUnitTest` relevant test filters pass.
- `:app:compileDebugKotlin` passes.
- `git diff --check` passes.
- Manual managed-install smoke confirms notification progress and successful install on a Range-supported APK.
- Cancellation smoke confirms notification cleanup, temp-file deletion, and session failure handling.

## Risks

| Risk | Mitigation |
| --- | --- |
| GPL contamination | Keep implementation clean-room, use independent Kotlin code and KeiOS-owned tests. |
| CDN rate limits | Cap default GitHub connections at 4, delay 429 retries, and fallback to single stream when segmented attempts fail early. |
| Install session complexity | Download to temp file first, then use the existing session write flow. |
| Temp storage pressure | Check available cache space before large downloads and clean temp files on cancellation/failure. |
| Progress noise | Throttle progress emissions through the existing progress emitter cadence. |
| Server Range quirks | Validate every `Content-Range` and fallback on probe mismatch. |
| Coroutine thread pressure | Keep connection counts conservative and run workers on existing bounded dispatchers. |
| Virtual-thread availability | Treat virtual threads as optional research until min-SDK and device-runtime evidence proves app-level support. |
| Native IO complexity | Keep io_uring/native download backends outside P1 and require file-IO bottleneck evidence before revisiting. |
| Scheduler race conditions | Keep scheduler state behind `Mutex` and cover allocation, requeue, stealing, and cancellation with deterministic unit tests. |

## First Implementation Recommendation

Start with P1 and P2.

P2 gives the strongest safety profile because Actions ZIP already lands in a temp file before package staging. After that path is stable, direct APK managed install can move to the same temp-file-first model.
