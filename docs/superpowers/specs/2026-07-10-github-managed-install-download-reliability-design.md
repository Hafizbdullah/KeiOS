# GitHub Managed Install Download Reliability Design

## Context

The GitHub page managed-install flow can stop near the end of a ranged APK
download while the PackageInstaller session remains open and empty. Device
evidence from `192.168.31.208:5555` showed the Image Toolbox download stopping
at `98.16 MB / 98.56 MB`; the install session never received a file descriptor
or progress update.

The current downloader lets an idle worker split or hand off the remaining
bytes of an active range. That changes a live request's end boundary and can
cancel the request while another worker starts the same tail. This creates a
fragile ownership transition at the exact point where GitHub downloads are
most likely to expose a slow final range. Synchronous positioned writes also
serialize network workers on file I/O.

The same managed-install flow publishes progress through the share-import
notification state. Its open and cancel actions therefore resume or cancel a
share-import flow, and its secondary action is labelled `Cancel linkage`.
Long install content is then shortened by the generic Focus builder, which
writes a literal `...` into Xiaomi Focus JSON.

## Goals

- Complete large GitHub release APK and Actions artifact downloads without a
  tail handoff stall.
- Preserve one immutable owner for every assigned byte interval.
- Scale connection count gradually and react to HTTP 429 pressure.
- Retry EOF, timeouts, connection resets, rate limits, and other transient
  failures with independent budgets.
- Keep file writes bounded in memory and outside network-worker critical paths.
- Verify exact written-byte coverage before accepting a preallocated file.
- Reuse the final CDN URL resolved by the range probe.
- Keep incomplete byte progress at `99%` or below.
- Give GitHub page installs their own open, cancel, and content semantics.
- Make the cancel action cancel the active install job and abandon its active
  PackageInstaller session.
- Keep page-install Focus content within the local Xiaomi template limit so
  the JSON contains no generated ellipsis.
- Preserve the GPL clean-room boundary: behavior and invariants may inform the
  design; no Go source is copied into Kotlin.

## Non-goals

- Resume downloads across process death.
- Persist segmented range state.
- Add a second HTTP stack or native `.so` downloader.
- Change Xiaomi notification templates or the general notification framework.
- Run multiple page-install notifications concurrently under separate IDs.

## Download Architecture

### Immutable range ownership

`PartScheduler` owns fresh and retry queues. A range is assigned once and its
`endInclusive` remains fixed for the lifetime of the HTTP request. Idle workers
wait while all remaining bytes belong to active workers. A failed worker
requeues only the suffix after its last accepted byte.

The public result keeps `stealCount` and `handoffCount` for compatibility and
benchmark continuity. Both values remain zero after this migration.

### Progressive concurrency

The scheduler starts at `min(4, effectiveConnections)`. Every successful range
completion permits one additional active worker until the configured maximum
is reached. All workers remain children of the download `coroutineScope`; the
scheduler only gates range acquisition.

### Rate-limit adaptation

HTTP 429 records a rate-limit strike and requeues the remaining suffix after a
delay. Repeated strikes lower the active connection limit to a floor of two.
After a cooldown, the scheduler admits one probe connection. A successful
probe keeps the higher limit and schedules the next recovery step. A failed
probe lowers the limit again and extends the cooldown.

### Retry classification

Each `DownloadPart` carries retry counters by failure kind:

- partial EOF
- lease or socket timeout
- connection reset
- HTTP 429
- other transient I/O

Each category has its own budget derived from `maxRetriesPerPart`; setting the
base value to zero disables all retries. The result's `retryCount` remains the
sum of all accepted requeues.

### Progress-aware range lease

Only retry ranges and small tail ranges receive a lease. Lease duration is
derived from remaining bytes and a minimum expected speed, with lower and
upper bounds. The monitor tracks the last accepted network progress. Each
accepted chunk resets the no-progress interval. Continuous no-progress expiry
cancels the current OkHttp call, classifies the failure as a timeout, and
requeues the remaining suffix.

### Bounded asynchronous file writer

Parallel workers copy each accepted network chunk into a bounded `Channel`.
A single child coroutine drains the channel and performs positioned writes on
the `.part` file. The channel capacity bounds retained byte arrays. Writer
failure closes the queue with the original cause; producers fail promptly and
the parent download deletes the temporary file.

Progress advances after a positioned write succeeds. Closing the writer drains
all queued writes before integrity checks and file replacement.

### Final URL and integrity

The range probe's final URL becomes the URL for every subsequent range request.
This avoids repeating GitHub redirects and reuses the same signed CDN target
during one download.

The downloader validates all of the following before replacing the output:

1. The asynchronous writer closed successfully.
2. Successfully written bytes equal the probed total exactly.
3. The `.part` file length equals the probed total.
4. The optional SHA-256 digest matches.

Preallocation alone never establishes completeness.

## Managed-install Notification Architecture

### Source-aware state

`GitHubShareImportNotificationState` gains an explicit source with two values:
`ShareImport` and `PageInstall`. Existing helpers default to `ShareImport`.
Page-install wrappers create `PageInstall` states for preparing, downloading,
committing, cancelled, and failed phases.

An ongoing page install opens the GitHub page and exposes these actions:

- Primary: `View progress`
- Secondary: `Cancel install`

Share-import phases retain their current flow actions and wording.

### Real cancellation

`GitHubPageManagedInstallCancelRegistry` holds the latest active page-install
cancel callback. The runner registers the current coroutine `Job` plus an
atomic session ID. The notification receiver invokes the callback, which
cancels the Job and calls `GitHubManagedApkInstaller.cancel()` when a session
has already been created. Existing installer cancellation paths also abandon
the session, making cancellation idempotent.

The runner clears only its own registration token in `finally`, so an older
completion cannot erase a newer install registration.

### Focus-safe content

Page-install progress content is composed from the localized phase short text,
percentage, and a bounded target label. It is kept at 28 characters or fewer
before entering `MiIslandNotificationBuilder`. The app helper performs this
composition; the shared Xiaomi builder remains unchanged.

## Exact Percentage Rule

For known totals:

- `downloadedBytes <= 0` returns `0`.
- `downloadedBytes >= totalBytes` returns `100`.
- every incomplete positive value uses integer floor division and clamps to
  `1..99`.

This prevents `98.16 MB / 98.56 MB` from being rounded to `100%`.

## Verification

Unit and Robolectric tests cover:

- no active range splitting or handoff
- progressive connection admission
- 429 reduction and probe recovery
- independent retry budgets
- progress-aware lease calculations
- final CDN URL reuse
- bounded writer drain and failure propagation
- exact byte coverage validation
- incomplete percentage clamping
- page-install open/cancel actions and labels
- cancel registry token safety and callback invocation
- page-install Focus JSON content without `...`

The live benchmark uses Image Toolbox `4.2.0-alpha01`:

- URL: `https://github.com/T8RIN/ImageToolbox/releases/download/4.2.0-alpha01/image-toolbox-4.2.0-alpha01-arm64-v8a.apk`
- Size: `98557185`
- SHA-256: `88199de76a991973829294b8cca6f393cc816045bed2058f163d58a1e51cf4a8`

Acceptance requires full byte count, matching SHA-256, `steal=0`,
`handoff=0`, and continuous completion through the final range. The final
Benchmark APK is installed on the connected Xiaomi API 36 device for download,
cancel, commit, completion, and Super Island validation.

## Commit Boundaries

1. Design and implementation plan.
2. Download scheduler and retry reliability.
3. Bounded asynchronous writing, final URL reuse, and integrity checks.
4. Exact install progress and page-install notification cancellation semantics.
5. Existing Xiaomi Focus `protocol=1` compatibility fix.
