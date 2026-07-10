# Segmented Download Plan

## Goal

Maintain a clean-room segmented HTTP downloader for high-value KeiOS download paths.

The first delivery targets large files where HTTP Range is usually available:

- GitHub managed install direct APK downloads.
- GitHub Actions APK artifact ZIP downloads.
- GameKee large media file downloads.

## Scope

| Area | Status | Target |
| --- | --- | --- |
| GitHub managed install direct APK | Implemented | Download to app-private temp APK with segmented Range, then stream the completed file into `PackageInstaller.Session`. |
| GitHub Actions artifact ZIP | Implemented | Download archive temp ZIP with segmented Range, then reuse current ZIP selection and session-writing flow. |
| GameKee large media | Implemented | Download large media to `.part` with segmented Range and atomic rename. |
| WebDAV sync text downloads | Later independent topic | Current text sync path stays with dav4jvm single request. |
| Coil/image decode cache | Later independent topic | Current Coil/GameKee image flow stays focused on cache and decode behavior. |

## Module Boundary

Implement the high-speed downloader as an independent Gradle module.

Implemented module:

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

Use the latest stable dependency versions verified from Maven metadata.

Maven metadata checked on 2026-07-11:

| Dependency | Current Version | Evidence | Source |
| --- | ---: | --- | --- |
| `com.squareup.okhttp3:okhttp` | `5.4.0` | Maven metadata `latest` and `release` are `5.4.0`; project already uses `5.4.0`. | `https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml` |
| `com.squareup.okhttp3:mockwebserver` | `5.4.0` | Maven metadata `latest` and `release` are `5.4.0`; keep tests aligned with OkHttp runtime. | `https://repo1.maven.org/maven2/com/squareup/okhttp3/mockwebserver/maven-metadata.xml` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.11.0` | Maven metadata `latest` and `release` are `1.11.0`; project already uses `1.11.0`. | `https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.11.0` | Maven metadata `latest` and `release` are `1.11.0`; keep tests aligned with coroutines runtime. | `https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml` |
| `org.jetbrains.kotlin:kotlin-test` | `2.4.0` | Maven metadata `latest`/`release` point to prerelease `2.4.20-Beta1`; the newest stable entry is `2.4.0`, matching the root Kotlin plugin. | `https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml` |

Version rules:

- Re-check Maven metadata before dependency updates and adopt newer stable releases after API and target-SDK verification.
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

## Current Implementation

| Path | Current Behavior | File |
| --- | --- | --- |
| GitHub direct APK managed install | Large Range-capable APKs use `:core-download`, then the completed temp APK streams into `PackageInstaller.Session`; Range-unavailable files use the module's single-stream fallback. | `feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt` |
| GitHub Actions ZIP managed install | Large Range-capable artifact ZIPs use `:core-download`, then `ZipFile` selects and stages the APK. | `feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt` |
| GameKee media download | Large video/archive-like media uses `:core-download`; common image cache traffic keeps the existing single-stream path. | `app/src/main/java/os/kei/feature/ba/data/remote/GameKeeFetchHelper.kt` |
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
| Release | `https://github.com/getpaseo/paseo/releases/tag/v0.1.105` |
| Asset name | `paseo-v0.1.105-android.apk` |
| Asset ID | `472420162` |
| Asset API URL | `https://api.github.com/repos/getpaseo/paseo/releases/assets/472420162` |
| Atom/direct URL | `https://github.com/getpaseo/paseo/releases/download/v0.1.105/paseo-v0.1.105-android.apk` |
| Size | `183051959` bytes, about `174.57 MiB` |
| Digest | `sha256:88e28ce20ca0a8bb0a0a0e2f455f2c66ee0f0bd8cb50ec70476ff699683e7065` |
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
| Connection strategy | Distinguishes one shared HTTP/2 pool from one pool per worker. |
| Negotiated protocol | Separates HTTP/1.1, HTTP/2, and fallback behavior. |
| Physical connections | Confirms whether concurrent HTTP/2 streams share one socket or use isolated worker sockets. |
| Request count | Detects tail-splitting amplification and CDN pressure. |
| Retry count and status codes | Captures 429/5xx/range failures. |
| Temp-file cleanup result | Keeps managed install and benchmark reruns safe. |

Benchmark run rules:

- Use the same device, network, app build, dispatcher settings, and connection
  defaults for all rows in one comparison set.
- Run each row at least three times after one untimed warm-up resolution.
- Keep raw per-run rows; compare medians for the decision.
- Clear only the downloader temp files between runs; leave HTTP/TLS connection
  behavior representative of normal app usage.
- Use a direct ordinary GET plus sequential file write for the single-stream
  baseline. Keep Range probing and Range recovery out of that baseline.
- Stop elapsed-time measurement after file flush/sync. Run length and SHA-256
  verification after the timed section for every mode.
- Capture `plain_get`, `segmented_shared`, and `segmented_isolated` rows before
  changing the default connection strategy.
- Rotate row order between repeated runs to reduce connection warm-up and
  transient network bias.

## Reference Summary

`piko` is a GPL-3.0 Go downloader in `.tmp/piko`, currently at commit `42d8928`.

Useful behavior-level ideas:

- Probe Range support with `HEAD` and `GET Range: bytes=0-0`.
- Enable parallel workers only when `Content-Length > 0` and byte ranges return `206`.
- Adapt each worker's part size from observed throughput.
- Allocate parts from both file head and file tail.
- Keep active ranges immutable and requeue failed suffixes from the last accepted offset.
- Start with limited concurrency, grow after successful parts, and reduce concurrency after repeated HTTP 429 responses.
- Recover rate-limited concurrency through delayed probe connections.
- Use independent retry budgets for EOF, timeouts, connection resets, rate limits, and other transient failures.
- Queue positioned file writes through a bounded asynchronous writer.
- Reuse the final CDN URL resolved by the range probe.
- Detect persistently slow range connections against active peer average.
- Keep a fixed, nondecreasing fresh-part size after entering the tail window so
  repeated allocation does not create a geometric series of tiny requests.
- Isolate worker connection pools when physical HTTP/2 connections are needed,
  while retaining a shared-pool strategy for measured comparison.
- Cancel a recovered-concurrency probe after one second without byte progress
  and feed that result back into rate-limit concurrency control.

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
    val expectedSha256: String = "",
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
    val retryCount: Int = 0,
    val stealCount: Int = 0,
    val handoffCount: Int = 0,
    val fallbackReason: String? = null,
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
| `PartScheduler` | Hand out immutable byte ranges, adapt part size, gate progressive concurrency, and requeue failed suffixes. |
| `RangeWorker` | Execute one byte-range request at a time, validate `206` and `Content-Range`, write bytes into `RandomAccessFile` or `FileChannel`. |
| `RetryPolicy` | Requeue retryable part failures, delay 429, cap retry budget, and fail the download after exhausted retries. |
| `ProgressAggregator` | Aggregate atomic completed bytes and emit throttled progress for notifications/UI. |
| `SegmentedDownloadClient` | Coordinate probe, file lifecycle, workers, fallback, and cleanup. |

Scheduler rules:

- Start with `initialPartSizeBytes`.
- Alternate head and tail allocations while unassigned bytes remain.
- On first entry to the normal tail window, derive a part size from
  `configured connections * 4`, then keep that size nondecreasing until fresh
  allocation completes.
- Under rate limiting, derive the tail target from reduced active connections,
  use two parts per active connection, and raise normal fresh-part size toward
  the 16 MiB rate-limited floor.
- When workers become idle and all remaining bytes belong to active workers, wait for completion or explicit failure requeue.
- Start at four active connections and grow toward the configured limit after successful range completion.
- Reduce active concurrency after repeated HTTP 429 responses and recover with delayed probe connections.
- Give recovered-concurrency probes a one-second continuous-idle timeout.
- Requeue failed bytes from the last confirmed written offset.
- Clamp active connections by file size and part size.
- Drain bounded asynchronous positioned writes before final validation.
- Emit final progress after written-byte coverage and file length both match the expected size.
- Reuse the range probe's final CDN URL for all data requests in the same download.
- Support shared and isolated-per-worker OkHttp connection pools; keep isolated
  pools bounded to one idle connection and evict them after all workers finish.
- Validate optional `expectedSha256` on the completed `.part` file before replacing the previous output.
- Keep the previous output file intact when length, range, or hash validation fails.

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
| Scheduler state | Protect part assignment, active counts, retry queues, rate-limit state, and recovery probes with `Mutex`. |
| Progress state | Use `AtomicLong` for completed bytes and a throttled progress emitter for UI/notification callbacks. |
| OkHttp cancellation | Use the existing cancellable call pattern so coroutine cancellation invokes `Call.cancel()`. |
| File writes | Feed copied chunks into a bounded channel; one child coroutine performs positioned `FileChannel` writes and reports successful bytes. |
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

### Implementation Status 2026-07-11

| Priority | Status | Notes |
| --- | --- | --- |
| P1 Core downloader foundation | Implemented and hardened | Added redirect-safe probing, resource validators, expected-size/SHA validation, bounded writes, resumable single-Range recovery, categorized retries, rate-limit recovery, slow-tail recovery, and connection strategies. |
| P2 GitHub Actions ZIP bridge | Implemented | `GitHubInstallSessionWriter` downloads Actions ZIP assets through `:core-download`, then keeps the existing `ZipFile` APK selection and staging flow. |
| P3 GitHub direct APK bridge | Implemented | Direct APK install now downloads to an app-private temp APK through `:core-download`, then streams the completed file into `PackageInstaller.Session`. |
| P4 GameKee large media bridge | Implemented with Kotlin-side scope guard | Video/archive-like media extensions (`mp4`, `m4v`, `mov`, `webm`, `mkv`, `zip`) use `:core-download`; common image cache downloads keep the previous single-stream path to avoid doubling request count with Range probes during image prefetch. |
| P5 Runtime tuning | Controlled evidence complete; live evidence pending | Controlled HTTP/1.1 and HTTP/2 matrices validate request bounding, speed profiles, and physical connection behavior. Paseo Atom/API and Actions Nightly/API rows remain required for final default-strategy tuning. |

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
- Paseo v0.1.105 Android APK is available as a fixed release benchmark sample for both Atom/direct and GitHubApiToken API-resolved downloads.
- Benchmark logging records release strategy, asset ID, asset name, expected size, resolved final URL host, parallel/fallback mode, elapsed time, average throughput, tail time, and retry count.
- Temp APK is reused for archive info reading.
- Session write runs from completed temp APK.
- Session progress keeps current notification wording and stage model.

Tests:

- Direct APK install writes expected session bytes.
- Archive info scan reads the downloaded temp APK.
- Download failure deletes temp APK and abandons session through existing failure path.
- Paseo v0.1.105 Android APK benchmark produces rows for AtomFeed single-stream, AtomFeed segmented, GitHubApiToken single-stream, and GitHubApiToken segmented.

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

Current defaults:

| Setting | Value |
| --- | ---: |
| GitHub balanced max connections | 4 |
| GitHub foreground boost hard limit | 12 |
| GitHub useful bytes per connection | 16 MiB |
| Foreground boost startup active connections | 8 |
| Foreground boost dispatcher capacity | 20 |
| Low-budget fallback threshold | 4 connections |
| GameKee max connections | 3 |
| GitHub balanced initial part size | 8 MiB |
| GitHub foreground boost initial part size | 4 MiB |
| GameKee initial part size | 4 MiB |
| Balanced minimum parallel size | 8 MiB |
| Foreground boost minimum parallel size | 4 MiB |
| Max retries per part | 3 |
| Default connection strategy | `IsolatedPerWorker` |
| Foreground boost dynamic part target | 16 seconds |
| Foreground boost tail parts per connection | 3 |
| Recovered probe idle timeout | 1 second |
| Progress-aware Range lease | 4-30 seconds when applicable |

Controlled benchmark evidence from 2026-07-11:

| Scenario | Mode | Requests | Physical connections | Average MiB/s | Speedup |
| --- | --- | ---: | ---: | ---: | ---: |
| Per-connection cap HTTP/1.1 | Plain GET | 1 | 1 | 2.31 | 1.00x |
| Per-connection cap HTTP/1.1 | Balanced isolated, 3 workers | 13 | 4 | 7.09 | 3.07x |
| Per-connection cap HTTP/1.1 | Foreground boost isolated, 6 workers | 19 | 7 | 14.04 | 6.07x |
| Slow head HTTP/1.1 | Plain GET | 1 | 1 | 0.36 | 1.00x |
| Slow head HTTP/1.1 | Balanced isolated, 4 workers | 17 | 5 | 5.72 | 15.95x |
| Slow head HTTP/1.1 | Foreground boost isolated, 8 workers | 25 | 9 | 8.62 | 24.05x |
| Per-stream cap HTTP/2 | Plain GET | 1 | 1 | 2.32 | 1.00x |
| Per-stream cap HTTP/2 | Balanced shared | 17 | 1 | 9.70 | 4.19x |
| Per-stream cap HTTP/2 | Balanced isolated | 17 | 5 | 9.74 | 4.20x |

The connection budget now scales worker count from file size before the
scheduler starts. The 24 MiB controlled fixture selects 3 Balanced workers and
6 ForegroundBoost workers, while the 30 MiB slow-head fixture selects 4 and 8.
This keeps request count proportional to useful work and preserves the expected
speedup under deterministic per-connection caps. The HTTP/2 fixture continues
to confirm one physical connection for Shared and five for Isolated.

Paseo Atom/direct device evidence from 2026-07-11:

- Device: Xiaomi `25098PN5AC`, Android 16 / API 36, HyperOS 3.0.
- Network: 5 GHz Wi-Fi, 432 Mbps link rate, Android-estimated downstream
  bandwidth 132 Mbps, Data Saver disabled, device idle state ACTIVE.
- Sample: Paseo `v0.1.104`, `183037443` bytes (`174.56 MiB`), direct release URL.
- Each combination ran three times with a cold Debug process. Every run used
  Range, completed with zero retries, and matched SHA-256.
- Foreground means process importance `100`. Background means the benchmark
  started from the Activity and immediately moved its task behind the Home/app
  stack; process importance remained `400` for the entire timed download.

| Foreground boost setting | App state | Runtime profile | Run speeds MiB/s | Mean MiB/s | Median MiB/s | Mean elapsed | Median elapsed |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: |
| Off | Foreground | Balanced, 4 workers, 8 MiB parts | 14.706 / 15.212 / 13.704 | 14.541 | 14.706 | 12.028 s | 11.870 s |
| Off | Background | Balanced, 4 workers, 8 MiB parts | 13.936 / 17.445 / 13.532 | 14.971 | 13.936 | 11.811 s | 12.526 s |
| On | Foreground | ForegroundBoost, 8 workers, 4 MiB parts | 18.225 / 13.709 / 18.659 | 16.864 | 18.225 | 10.555 s | 9.578 s |
| On | Background | ForegroundBoost, 8 workers, 4 MiB parts | 17.843 / 13.319 / 14.307 | 15.156 | 14.307 | 11.697 s | 12.201 s |

Device interpretation:

- ForegroundBoost in the foreground improved mean throughput by `15.98%` and
  median throughput by `23.93%`. Mean elapsed time fell by `12.24%`; median
  elapsed time fell by `19.31%`, saving about `2.29` seconds on this 174.56 MiB
  sample.
- ForegroundBoost after moving the app to the background improved mean
  throughput by `1.24%` and median throughput by `2.66%`. Mean elapsed time
  improved by `0.97%`, about `0.11` seconds.
- Paired foreground/background comparisons showed a median background effect
  of `-1.26%` for Balanced and `-2.84%` for ForegroundBoost. One Boost pair had
  a `-23.32%` background outlier, showing greater sensitivity to CDN and
  scheduler variance with eight physical worker connections.
- The speed profile is captured when a managed-install request is created. A
  foreground Boost download keeps the Boost profile after the task moves to the
  background. Background refresh checks metadata and does not download APKs.
- Keep the user-controlled foreground boost for manual foreground installs.
  Keep background-originated work on Balanced. A lifecycle-driven mid-download
  downgrade has low expected benefit for 10-13 second APK transfers and would
  add cancellation or dynamic-scheduler complexity.

Adaptive foreground profile revision from 2026-07-11:

- ForegroundBoost has a 12-connection hard limit and starts with at most 8
  active workers.
- GitHub managed downloads reserve at least 16 MiB of artifact data per useful
  connection. Effective worker count is `ceil(size / 16 MiB)`, clamped by the
  selected profile limit.
- A connection budget of 4 or fewer uses the Balanced scheduler tuning and an
  8 MiB effective initial part size. Small APKs therefore avoid the extra
  request churn of the aggressive large-file profile.
- Large downloads grow one connection only after a complete wave of distinct
  workers has succeeded. A 174.57 MiB Paseo run received an 11-worker budget,
  started at 8, peaked at 9, and completed before the remaining budget opened.
- Repeated 429 responses continue to reduce active concurrency, enlarge
  rate-limited parts, honor retry delay, and recover through one probe slot.
- Foreground managed downloads use the dedicated 20-slot
  `AppDispatchers.githubManagedDownload` view. The capacity covers up to 12
  blocking readers plus the bounded writer and cancellation work.

The clean-room review used piko `v0.1.2` (`723abd6`) only as a behavioral
reference. Relevant observations were a useful-bytes-per-connection cap, an
8-connection startup probe, small concurrency-probe ranges, and aggressive
backoff when startup probing encounters rate limits. KeiOS uses independently
written Kotlin models, scheduler state, retry policy, tests, and Android
dispatcher integration.

Candidate screening before the adaptive profile:

| Candidate | Sample/mode | Balanced mean | Boost mean | Result |
| --- | --- | ---: | ---: | --- |
| Immediate 16 workers | Paseo direct, API 36 device | 11.531 MiB/s | 11.249 MiB/s | Mean `-2.45%`; median `-12.79%` |
| Immediate 12 workers | Paseo API-signed, API 36 device | 14.558 MiB/s | 14.498 MiB/s | Mean `-0.41%`; median `-5.39%` |
| Immediate 10 workers | Paseo API-signed, API 36 device | 13.458 MiB/s | 13.925 MiB/s | Mean `+3.47%`; median `+6.42%` |

These candidates established 8 as the strong startup tier and 12 as a useful
hard ceiling. Size budgeting and wave-gated growth let large APK/ZIP transfers
approach the ceiling while common APKs stay at 2-8 workers.

Android 17 AVD size matrix:

Current tuning and regression work targets `emulator-5556` on API 37. Direct
and API-signed samples run through the same AVD so connection-budget changes,
progressive growth, retries, integrity, and obvious throughput regressions can
be compared on a stable test target. The API 36 Xiaomi device keeps the current
Debug build for the final foreground/background and OEM install-flow pass after
the algorithm settles.

| Sample | Actual size | Balanced/Boost budget | Direct mean MiB/s | Direct delta | API-signed mean MiB/s | API delta |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| KeiOS 1.11.0 | 19.27 MiB | 2 / 2 | 5.056 / 4.037 | `-20.14%` | 4.278 / 2.385 | `-44.25%` |
| Keyguard r20260616 | 37.13 MiB | 3 / 3 | 4.010 / 5.869 | `+46.38%` | 4.604 / 9.663 | `+109.88%` |
| Momogram 12.8.1-1 | 60.45 MiB | 4 / 4 | 8.811 / 8.282 | `-6.00%` | 7.361 / 12.396 | `+68.40%` |
| ImageToolbox 4.1.0 | 94.77 MiB | 4 / 6 | 4.301 / 10.048* | `+133.65%` | 9.532 / 7.473 | `-21.60%` |
| Termux 0.119 beta 3 | 113.74 MiB | 4 / 8 | 9.430 / 6.532 | `-30.72%` | 7.147 / 9.930 | `+38.95%` |
| Paseo 0.1.105 | 174.57 MiB | 4 / 11 | 5.532 / 11.831 | `+113.86%` | 7.357 / 11.999 | `+63.11%` |

The matrix used two passes with reversed profile order. One ImageToolbox Direct
Boost run hit the local 90-second harness timeout, so its Boost mean contains
one completed sample. The other 47 transfers completed with Range support and
matching per-asset SHA-256; five retries occurred across the four Paseo Boost
or Direct runs. AVD throughput varied from roughly 1 to 14 MiB/s, so this matrix
serves as correctness and regression screening. Final release-speed claims stay
gated on the API 36 physical-device pass.

AVD background screening for Paseo API-signed produced 9.546 MiB/s for Balanced
and 7.357 MiB/s for ForegroundBoost in one paired run. Process importance moved
from 100 to 125. This sample confirms lifecycle completion and provides a
follow-up signal for the final physical-device foreground/background matrix.

Size-matrix fixtures:

| Approximate tier | Release asset | Actual bytes | Balanced budget | Boost budget |
| --- | --- | ---: | ---: | ---: |
| 20 MiB | [KeiOS 1.11.0](https://github.com/hosizoraru/KeiOS/releases/download/v1.11.0/KeiOS_1.11.0.apk) | 20,206,796 | 2 | 2 |
| 40 MiB | [Keyguard r20260616](https://github.com/AChep/keyguard-app/releases/download/r20260616/androidApp-none-release.apk) | 38,929,150 | 3 | 3 |
| 60 MiB | [Momogram 12.8.1-1](https://github.com/im030/Momogram/releases/download/v12.8.1-1/Momo-v12.8.1-8453086bc7-arm64-v8a.apk) | 63,384,858 | 4 | 4 |
| 90 MiB | [ImageToolbox 4.1.0](https://github.com/T8RIN/ImageToolbox/releases/download/4.1.0/image-toolbox-4.1.0-arm64-v8a.apk) | 99,376,167 | 4 | 6 |
| 110 MiB | [Termux 0.119 beta 3](https://github.com/termux/termux-app/releases/download/v0.119.0-beta.3/termux-app_v0.119.0-beta.3+apt-android-7-github-debug_universal.apk) | 119,267,555 | 4 | 8 |
| 170 MiB | [Paseo 0.1.105](https://github.com/getpaseo/paseo/releases/download/v0.1.105/paseo-v0.1.105-android.apk) | 183,051,959 | 4 | 11 |

Each fixture is measured twice: the public release URL used by Atom/direct mode
and a fresh API-token asset URL resolved immediately before the run. Expiring
signed URLs stay in ignored local evidence and never enter tracked docs.

Live benchmark command template:

```bash
gtimeout 20m ./gradlew :core-download:testDebugUnitTest \
  --tests 'os.kei.core.download.segmented.SegmentedDownloadLiveBenchmarkTest' \
  -Dkeios.download.liveBenchmark=true \
  -Dkeios.download.liveRuns=1 \
  -Dkeios.download.liveUrl='<fresh direct or signed URL>' \
  -Dkeios.download.liveBytes='<expected bytes>' \
  -Dkeios.download.liveSha256='<expected sha256>' \
  -Dkeios.download.maxConnections=4 \
  -Dkeios.download.partMiB=4 \
  -Dkeios.download.protocol=auto \
  --no-daemon --console=plain \
  > .tmp/codex-gradle/core-download-live.log 2>&1
```

Each Live run downloads three copies: `plain_get`, `segmented_shared`, and
`segmented_isolated`. Repeat the same resolved asset with
`-Dkeios.download.protocol=http1` for the forced HTTP/1.1 control. Run separate
comparison sets for Paseo Atom/direct, Paseo API-resolved signed URL,
NightlyLink/nightly.build, and GitHub Actions API-resolved signed URL.

Evidence to capture:

- Average throughput for single-stream vs segmented on the same URL.
- Paseo v0.1.105 Android APK benchmark rows for AtomFeed direct URL and GitHubApiToken API-resolved URL.
- GitHub Actions artifact benchmark rows for NightlyLink/nightly.build and GitHubApiToken API modes.
- Tail duration after 90% complete.
- 429 and 5xx frequency.
- Cancellation cleanup success.
- Memory footprint while downloading 100-300 MiB APK/ZIP files.
- Active coroutine count and occupied IO thread count during large downloads.
- Whether Android 36.1/37.1 devices expose usable virtual-thread runtime behavior for app code.

## Verification Checklist

- `:core-download:testDebugUnitTest --rerun-tasks` passes 66 tests with the two
  opt-in benchmark tests skipped by default.
- Existing `RemoteZipEntryReaderTest` stays green.
- `:feature-github:testDebugUnitTest --offline` passes 534 tests with one skip.
- Targeted GameKee media download tests pass.
- `:app:testDebugUnitTest` relevant test filters pass.
- `:app:compileDebugKotlin --offline` passes.
- `git diff --check` passes.
- Manual managed-install smoke confirms notification progress and successful install on a Range-supported APK.
- Cancellation smoke confirms notification cleanup, temp-file deletion, and session failure handling.

## Risks

| Risk | Mitigation |
| --- | --- |
| GPL contamination | Keep implementation clean-room, use independent Kotlin code and KeiOS-owned tests. |
| CDN rate limits | Cap balanced GitHub connections at 4, honor `Retry-After`, reduce active concurrency after repeated 429 responses, enlarge rate-limited parts, and recover through one probe slot. |
| Install session complexity | Download to temp file first, then use the existing session write flow. |
| Temp storage pressure | Check available cache space before large downloads and clean temp files on cancellation/failure. |
| Progress noise | Throttle progress emissions through the existing progress emitter cadence. |
| Server Range quirks | Validate every `Content-Range` and fallback on probe mismatch. |
| Coroutine thread pressure | Keep connection counts conservative and run workers on existing bounded dispatchers. |
| Virtual-thread availability | Treat virtual threads as optional research until min-SDK and device-runtime evidence proves app-level support. |
| Native IO complexity | Keep io_uring/native download backends outside P1 and require file-IO bottleneck evidence before revisiting. |
| Scheduler race conditions | Keep scheduler state behind `Mutex` and cover allocation, requeue, stealing, and cancellation with deterministic unit tests. |

## Next Validation Recommendation

Keep the active tuning loop on the Android 17 `emulator-5556` AVD. Run the four
real-source comparison sets there: Paseo Atom/direct, Paseo API-resolved,
Actions NightlyLink/nightly.build, and Actions API-resolved. Use the three-mode
Live matrix for each source and repeat winning candidates three times. Once the
parameters settle, run the compact foreground/background matrix and managed
install smoke on the API 36 Xiaomi device before choosing the release default.
