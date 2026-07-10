# GitHub Managed Install Download Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make GitHub page managed installs finish ranged downloads reliably, report exact progress, expose correct page-install actions, and cancel the real install session.

**Architecture:** `:core-download` moves to immutable range ownership, progressive concurrency, category-specific retries, progress-aware leases, final-CDN reuse, and a bounded single-writer queue. The app notification adapter adds an explicit page-install source and a token-safe cancellation registry while keeping `:core-notification` template behavior stable.

**Tech Stack:** Kotlin 2.4, Kotlin Coroutines 1.11, OkHttp/MockWebServer 5.4, JUnit 4, Robolectric, Android PackageInstaller, Xiaomi Focus Notification V3.

## Global Constraints

- Keep the implementation clean-room from GPL piko source; use behavior and invariants only.
- Keep existing `SegmentedDownloadResult.stealCount` and `handoffCount` fields, with both remaining zero.
- Preserve the existing Xiaomi `param_v2.protocol = 1` changes as a separate commit.
- Keep page-install content at 28 characters or fewer before it reaches the Focus builder.
- `maxRetriesPerPart = 0` disables every retry category.
- Use structured concurrency; every worker, writer, and lease monitor remains a child of the download call.
- Keep all new user-facing labels in string resources.

---

### Task 1: Immutable Range Scheduler and Progressive Concurrency

**Files:**
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/PartScheduler.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/SegmentedDownloadModels.kt`
- Modify: `core-download/src/test/java/os/kei/core/download/segmented/PartSchedulerTest.kt`

**Interfaces:**
- Produces: `PartScheduler.nextPart(workerId): ActiveDownloadPart?`
- Produces: `PartScheduler.requeueFailed(active, failureKind, delayMs): Boolean`
- Produces: `PartScheduler.recordSuccess(workerId, active, bytes, elapsedMs)`
- Produces: `PartScheduler.recordRateLimit(active, delayMs)`

- [ ] **Step 1: Replace steal tests with failing immutable-ownership tests**

```kotlin
@Test
fun `idle worker waits while remaining bytes belong to an active part`() = runBlocking {
    val scheduler = scheduler(totalBytes = 500, concurrency = 2, startupActive = 2)
    val active = assertNotNull(scheduler.nextPart(workerId = 0))
    active.advanceTo(100)
    assertNull(scheduler.nextPart(workerId = 1))
    assertEquals(499, active.currentEndInclusive())
}
```

- [ ] **Step 2: Add failing progressive-concurrency tests**

```kotlin
@Test
fun `scheduler starts four workers and grows after successful parts`() = runBlocking {
    val scheduler = scheduler(totalBytes = 1_000, concurrency = 6, startupActive = 4)
    val active = (0 until 4).map { assertNotNull(scheduler.nextPart(it)) }
    assertNull(scheduler.nextPart(4))
    scheduler.finish(0, active[0])
    scheduler.recordSuccess(0, active[0], active[0].part.length, 100)
    assertNotNull(scheduler.nextPart(4))
}
```

- [ ] **Step 3: Run RED tests**

Run: `./gradlew :core-download:testDebugUnitTest --tests '*PartSchedulerTest*'`

Expected: compilation or assertion failures for the new scheduler API and immutable ownership.

- [ ] **Step 4: Implement immutable ownership and startup gating**

Remove active end mutation and split/handoff code. Allocate and activate a part in one scheduler lock. Add `activeCount`, `maxActive`, and startup growth after successful range completion.

- [ ] **Step 5: Run GREEN tests**

Run: `./gradlew :core-download:testDebugUnitTest --tests '*PartSchedulerTest*'`

Expected: PASS with all scheduler tests and zero steal/handoff statistics.

- [ ] **Step 6: Commit**

```bash
git add core-download/src/main/java/os/kei/core/download/segmented/PartScheduler.kt core-download/src/main/java/os/kei/core/download/segmented/SegmentedDownloadModels.kt core-download/src/test/java/os/kei/core/download/segmented/PartSchedulerTest.kt
git commit -m "fix(download): keep range ownership immutable"
```

### Task 2: Independent Retry Budgets, Rate-limit Recovery, and Tail Lease

**Files:**
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/PartScheduler.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/RangeLease.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/SegmentedDownloadClient.kt`
- Modify: `core-download/src/test/java/os/kei/core/download/segmented/PartSchedulerTest.kt`
- Modify: `core-download/src/test/java/os/kei/core/download/segmented/RangeLeaseTest.kt`
- Modify: `core-download/src/test/java/os/kei/core/download/segmented/SegmentedDownloadClientTest.kt`

**Interfaces:**
- Produces: `RangeFailureKind`
- Produces: `RangeRetryBudgets.fromBase(maxRetriesPerPart)`
- Produces: `rangeLeaseMs(remainingBytes, retryCount, minExpectedBytesPerSecond)`

- [ ] **Step 1: Add failing retry-category tests**

Requeue one partial EOF and one HTTP 429 against the same logical suffix with a budget of one for each category. Assert both requeues succeed and a second requeue in either category fails.

- [ ] **Step 2: Add failing 429 admission tests**

Drive two rate-limit strikes, assert the active limit drops, advance injected time through cooldown, and assert exactly one probe connection is admitted before normal recovery.

- [ ] **Step 3: Add failing lease tests**

```kotlin
assertEquals(8_000L, rangeLeaseMs(512L * 1024L, retryCount = 0, minExpectedBytesPerSecond = 64L * 1024L))
assertEquals(4_000L, rangeLeaseMs(128L * 1024L, retryCount = 1, minExpectedBytesPerSecond = 64L * 1024L))
```

- [ ] **Step 4: Run RED tests**

Run: `./gradlew :core-download:testDebugUnitTest --tests '*PartSchedulerTest*' --tests '*RangeLeaseTest*' --tests '*SegmentedDownloadClientTest*'`

Expected: failures for missing failure categories, rate-limit recovery, and progress-aware lease behavior.

- [ ] **Step 5: Implement retry policy and lease monitor**

Classify partial EOF, timeout/lease expiry, socket reset, 429, and transient I/O independently. Reset the lease deadline after every accepted chunk and cancel only the current OkHttp call after continuous no-progress expiry.

- [ ] **Step 6: Run GREEN tests and commit**

Run: `./gradlew :core-download:testDebugUnitTest`

```bash
git add core-download/src/main core-download/src/test
git commit -m "fix(download): adapt retries and tail leases"
```

### Task 3: Bounded Async Writer, Final URL Reuse, and Exact Coverage

**Files:**
- Create: `core-download/src/main/java/os/kei/core/download/segmented/BoundedAsyncFileWriter.kt`
- Create: `core-download/src/test/java/os/kei/core/download/segmented/BoundedAsyncFileWriterTest.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/RangeProbe.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/SegmentedDownloadClient.kt`
- Modify: `core-download/src/main/java/os/kei/core/download/segmented/SegmentedDownloadModels.kt`
- Modify: `core-download/src/test/java/os/kei/core/download/segmented/SegmentedDownloadClientTest.kt`

**Interfaces:**
- Produces: `BoundedAsyncFileWriter.enqueue(position, source, byteCount)`
- Produces: `BoundedAsyncFileWriter.closeAndJoin()`
- Produces: `SegmentedDownloadOptions.writeQueueCapacity`

- [ ] **Step 1: Write failing writer tests**

Test that close drains queued positioned writes in order, producer suspension bounds the queue, cancellation closes the writer, and a positioned-write exception reaches the producer or close call.

- [ ] **Step 2: Write failing final-URL and coverage tests**

Use two MockWebServer instances: the probe server redirects once to the CDN server, and every data range must hit only the CDN server. Add a writer coverage test that preallocates the full file but reports one byte missing and expects failure.

- [ ] **Step 3: Run RED tests**

Run: `./gradlew :core-download:testDebugUnitTest --tests '*BoundedAsyncFileWriterTest*' --tests '*SegmentedDownloadClientTest*'`

Expected: missing writer API, repeated redirect requests, and missing byte-coverage validation.

- [ ] **Step 4: Implement bounded writer and final checks**

Create a bounded `Channel` owned by the current `coroutineScope`. The consumer performs positioned `FileChannel` writes and advances progress after success. Drain it before validating written bytes, file length, SHA-256, and atomic replacement.

- [ ] **Step 5: Run GREEN tests and commit**

Run: `./gradlew :core-download:testDebugUnitTest`

```bash
git add core-download/src/main core-download/src/test
git commit -m "perf(download): pipeline bounded file writes"
```

### Task 4: Exact GitHub Download Percentage

**Files:**
- Modify: `feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt`
- Create: `feature-github/src/test/java/os/kei/feature/github/install/GitHubInstallSessionWriterProgressTest.kt`

**Interfaces:**
- Produces: `internal fun downloadProgressPercent(downloadedBytes, totalBytes): Int`

- [ ] **Step 1: Write failing percentage tests**

```kotlin
assertEquals(99, downloadProgressPercent(98_160_000, 98_560_000))
assertEquals(99, downloadProgressPercent(99, 100))
assertEquals(100, downloadProgressPercent(100, 100))
```

- [ ] **Step 2: Run RED test**

Run: `./gradlew :feature-github:testDebugUnitTest --tests '*GitHubInstallSessionWriterProgressTest*'`

Expected: the near-complete case currently returns `100`.

- [ ] **Step 3: Implement integer floor and completion clamp**

Use overflow-safe integer arithmetic or floor-based `Double` conversion. Return `100` only when `downloadedBytes >= totalBytes`; clamp positive incomplete progress to `1..99`.

- [ ] **Step 4: Run GREEN test and commit**

```bash
./gradlew :feature-github:testDebugUnitTest
git add feature-github/src/main/java/os/kei/feature/github/install/GitHubInstallSessionWriter.kt feature-github/src/test/java/os/kei/feature/github/install/GitHubInstallSessionWriterProgressTest.kt
git commit -m "fix(github): report exact install download progress"
```

### Task 5: Page-install Notification Source and Real Cancellation

**Files:**
- Create: `app/src/main/java/os/kei/feature/github/notification/GitHubPageManagedInstallCancelRegistry.kt`
- Create: `app/src/test/java/os/kei/feature/github/notification/GitHubPageManagedInstallCancelRegistryTest.kt`
- Modify: `app/src/main/java/os/kei/feature/github/notification/GitHubShareImportNotificationModels.kt`
- Modify: `app/src/main/java/os/kei/feature/github/notification/GitHubShareImportNotificationActions.kt`
- Modify: `app/src/main/java/os/kei/feature/github/notification/GitHubShareImportActionReceiver.kt`
- Modify: `app/src/main/java/os/kei/feature/github/notification/GitHubShareImportNotificationHelper.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/github/page/action/GitHubPageManagedInstallRunner.kt`
- Modify: `app/src/test/java/os/kei/feature/github/notification/GitHubShareImportNotificationHelperTest.kt`
- Modify: `app/src/main/res/values/strings_github.xml`
- Modify localized `strings_github.xml` files containing the same notification action family.

**Interfaces:**
- Produces: `GitHubShareImportNotificationSource.PageInstall`
- Produces: `GitHubPageManagedInstallCancelRegistry.register(cancel)`
- Produces: `GitHubPageManagedInstallCancelRegistry.cancelActive()`

- [ ] **Step 1: Write failing action and content tests**

Create a page-install downloading state and assert: primary action is `View progress`, secondary action is `Cancel install`, primary opens `MainActivity` on GitHub, secondary targets the page-install cancel receiver action, and Focus base content has length at most 28 with no `...`.

- [ ] **Step 2: Write failing registry tests**

Assert active cancellation invokes the callback once, clearing an old token preserves a newer registration, and clearing the current token removes it.

- [ ] **Step 3: Run RED tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*GitHubShareImportNotificationHelperTest*' --tests '*GitHubPageManagedInstallCancelRegistryTest*'`

Expected: current action opens share import, uses `Cancel linkage`, and has no page-install cancellation callback.

- [ ] **Step 4: Implement source-aware helpers and cancellation**

Register the runner's current Job and atomic session ID. On cancel, cancel the Job and call `managedApkInstaller.cancel(context, sessionId)` when positive. Add dedicated page-install notify wrappers and bounded localized content composition.

- [ ] **Step 5: Run GREEN tests and commit**

Run: `./gradlew :app:testDebugUnitTest :feature-github:testDebugUnitTest`

```bash
git add app/src/main app/src/test
git commit -m "fix(github): separate page install notification actions"
```

### Task 6: Focus Protocol Compatibility Commit

**Files:**
- Existing modification: `core-notification/src/main/java/os/kei/core/notification/focus/MiFocusProtocol.kt`
- Existing modification: `core-notification/src/test/java/os/kei/core/notification/focus/MiFocusProtocolEncoderTest.kt`

- [ ] **Step 1: Re-run focused protocol tests**

Run: `./gradlew :core-notification:testDebugUnitTest --tests '*MiFocusProtocolEncoderTest*'`

Expected: PASS and encoded `param_v2.protocol` equals numeric `1`.

- [ ] **Step 2: Commit separately**

```bash
git add core-notification/src/main/java/os/kei/core/notification/focus/MiFocusProtocol.kt core-notification/src/test/java/os/kei/core/notification/focus/MiFocusProtocolEncoderTest.kt
git commit -m "fix(notification): encode Xiaomi Focus protocol version"
```

### Task 7: Benchmark, Build, and Device Acceptance

**Files:**
- Update when measured: `docs/planning/segmented-download-plan.md`

- [ ] **Step 1: Run all module tests**

```bash
./gradlew :core-download:testDebugUnitTest :feature-github:testDebugUnitTest :core-notification:testDebugUnitTest :app:testDebugUnitTest
```

Expected: all tasks PASS.

- [ ] **Step 2: Run the Image Toolbox live benchmark**

```bash
./gradlew :core-download:testDebugUnitTest --tests '*SegmentedDownloadLiveBenchmarkTest*' -Dkeios.download.liveBenchmark=true -Dkeios.download.liveUrl=https://github.com/T8RIN/ImageToolbox/releases/download/4.2.0-alpha01/image-toolbox-4.2.0-alpha01-arm64-v8a.apk -Dkeios.download.liveBytes=98557185 -Dkeios.download.liveSha256=88199de76a991973829294b8cca6f393cc816045bed2058f163d58a1e51cf4a8 -Dkeios.download.maxConnections=8 -Dkeios.download.partMiB=4
```

Expected: full size, matching SHA-256, `steal=0`, `handoff=0`, and no tail stall.

- [ ] **Step 3: Build Benchmark APK**

Run: `./gradlew :app:assembleBenchmark`

Expected: successful minified Benchmark artifact.

- [ ] **Step 4: Install and verify on Xiaomi device**

Abandon stale session `529915666`, install the new Benchmark APK, then verify page-install download, notification wording, cancellation, install confirmation, completed state, Super Island display, and absence of truncation.

- [ ] **Step 5: Record measured results and final status**

Update the existing segmented-download plan with the current piko reference commit, measured before/after rows, and the final immutable-ownership rules.
