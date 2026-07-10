# BA AP Dismiss Snooze Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate explicit BA AP acknowledgement from swipe dismissal while giving swipe dismissal a one-hour account-scoped snooze.

**Architecture:** Add a dedicated delete PendingIntent to the shared live-notification payload, then route BA AP deletion to a dismiss broadcast while retaining the mark-read broadcast on visible actions and Xiaomi Focus JSON. Persist a separate dismissed-until timestamp beside local read anchors and evaluate both states in foreground and background reminder policy.

**Tech Stack:** Kotlin 2.3, Android notifications and PendingIntent, Jetpack Compose state, MMKV-backed BA account local state, Robolectric, JUnit, Gradle 9.5.1.

## Global Constraints

- `Mark read` is the only interaction that writes the read suppression anchor.
- Swipe dismissal and `Clear all` snooze the matching account and AP kind for exactly one hour.
- Recovery below threshold or disabling the reminder clears read and dismissal state.
- Legacy, Live Updates, Xiaomi Magic, and Super Island use the same action semantics.
- Existing non-BA payloads retain stop-on-dismiss through the default payload value.
- Do not change the Xiaomi Focus JSON template structure.
- Run Gradle with `gtimeout`, `--no-daemon`, `--console=plain`, and logs under `.tmp/codex-gradle/`.

---

### Task 1: Notification Action Separation

**Files:**
- Modify: `core-notification/src/main/java/os/kei/core/notification/live/LiveNotificationPayload.kt`
- Modify: `core-notification/src/main/java/os/kei/core/notification/live/builder/LegacyNotificationBuilder.kt`
- Modify: `core-notification/src/main/java/os/kei/core/notification/live/builder/ModernNotificationBuilder.kt`
- Modify: `core-notification/src/main/java/os/kei/core/notification/live/builder/MiIslandNotificationBuilder.kt`
- Modify: `feature-mcp/src/main/java/os/kei/mcp/notification/McpAppIntentContract.kt`
- Modify: `feature-mcp/src/main/java/os/kei/mcp/notification/McpNotificationHelper.kt`
- Test: `core-notification/src/test/java/os/kei/core/notification/live/builder/NotificationActionRoutingTest.kt`
- Test: `feature-mcp/src/test/java/os/kei/mcp/notification/McpNotificationMarkReadIntentTest.kt`

**Interfaces:**
- Consumes: existing `stopPendingIntent` visible-action contract.
- Produces: `LiveNotificationPayload.deletePendingIntent: PendingIntent`, `McpNotificationDismissContract`, and `McpNotificationHelper.dismissPendingIntent(...)`.

- [ ] **Step 1: Write failing builder routing tests**

Create separate mark-read and dismiss PendingIntents in
`NotificationActionRoutingTest`, pass both into the payload, and assert:

```kotlin
assertEquals(markReadPendingIntent, notification.actions[1].actionIntent)
assertEquals(dismissPendingIntent, notification.deleteIntent)
assertEquals(markReadPendingIntent, island.focusAction("mcp_action_stop").actionIntent)
```

- [ ] **Step 2: Write failing production PendingIntent identity tests**

Assert the delete intent uses `ACTION_DISMISS`, retains notification/server/
account metadata, uses a request code distinct from mark-read, and updates
account metadata for the same notification ID.

- [ ] **Step 3: Verify RED**

Run:

```bash
mkdir -p .tmp/codex-gradle
gtimeout 8m ./gradlew --no-daemon --console=plain \
  :core-notification:testDebugUnitTest \
  :feature-mcp:testDebugUnitTest \
  > .tmp/codex-gradle/ba-dismiss-routing-red.log 2>&1
```

Expected: failures show `deleteIntent` still equals the mark-read/stop intent
and the dismiss contract is absent.

- [ ] **Step 4: Implement the separate delete action**

Add the payload field with a compatibility default:

```kotlin
val stopPendingIntent: PendingIntent,
val deletePendingIntent: PendingIntent = stopPendingIntent,
```

Route each builder's `setDeleteIntent` to `deletePendingIntent`. Add a public
dismiss contract and construct the BA dismiss PendingIntent with its own
request-code range. Pass it only when `SecondaryActionMode.MARK_READ`; all
other payloads inherit the existing default.

- [ ] **Step 5: Verify GREEN and commit**

Run the two module test tasks again, inspect the complete logs, then commit:

```bash
git add core-notification feature-mcp
git commit -m "fix(notification): separate dismiss from mark read"
```

### Task 2: Account-Scoped Dismissal Snooze

**Files:**
- Modify: `app/src/main/java/os/kei/feature/notification/MiFocusNotificationActionReceiver.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStore.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementRuntimeRepository.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaAccountSnapshotMapper.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaPageModels.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicy.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaReminderCoordinator.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinator.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeController.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageEffects.kt`
- Modify: `app/src/main/java/os/kei/core/background/AppBackgroundSchedulePolicy.kt`
- Modify: `app/src/main/java/os/kei/core/background/AppForegroundInfoHandler.kt`
- Modify: `feature-mcp/src/main/java/os/kei/mcp/notification/McpNotificationHelper.kt`
- Test: focused existing BA policy, store, receiver, coordinator, scheduler, and snapshot-mapper tests.

**Interfaces:**
- Consumes: `McpNotificationDismissContract.ACTION` and validated `BaApMarkReadTarget` metadata.
- Produces: `dismissedUntilAtMs` state per account/AP kind and policy decisions that schedule the next reminder boundary.

- [ ] **Step 1: Write failing policy and store tests**

Add tests proving:

```kotlin
val decision = BaApAcknowledgementPolicy.evaluate(
    notificationEnabled = true,
    currentDisplay = 131,
    thresholdDisplay = 120,
    keepReadUntilBelowThreshold = true,
    suppressionAnchorAtMs = 0L,
    dismissedUntilAtMs = NOW_MS + 30 * 60 * 1000L,
    nowMs = NOW_MS,
)
assertTrue(decision.suppressed)
assertEquals(NOW_MS + 30 * 60 * 1000L, decision.nextEligibleAtMs)
```

Also prove expiry bypasses level deduplication, recovery resets both fields,
ordinary/cafe/account keys stay isolated, and account deletion clears both key
families.

- [ ] **Step 2: Write failing receiver and integration tests**

Assert mark-read and dismiss actions differ:

```kotlin
assertEquals(0L, store.loadSuppressionAnchor(accountId, kind))
assertEquals(NOW_MS + BA_AP_DISMISS_SNOOZE_INTERVAL_MS,
    store.loadDismissedUntil(accountId, kind))
```

Seed the MCP snapshot and active cache before dismissal and assert both are
cleared. Add coordinator/scheduler tests showing AP `+1` stays quiet before
the deadline and becomes eligible at the deadline.

- [ ] **Step 3: Verify RED**

Run focused test classes with `:app:testDebugUnitTest --tests ...`; expected
failures identify the missing dismissed-until state and receiver action.

- [ ] **Step 4: Implement local state and policy propagation**

Add `apDismissedUntilAtMs` and `cafeApDismissedUntilAtMs` to local snapshots and
controller state. Extend the acknowledgement store/repository with a
`ba_ap_dismissed_until` key family. Thread the selected timestamp through
reminder coordinator requests and background scheduling.

Policy precedence:

```kotlin
if (permanentRead) return suppressedWithoutDeadline
val nextEligibleAtMs = maxOf(hourlyReadDeadline, dismissedUntilAtMs)
if (nowMs < nextEligibleAtMs) return suppressedAt(nextEligibleAtMs)
return eligibleWithLevelDedupeBypass
```

Recovery and disabled-reminder paths request persistence resets for both local
fields.

- [ ] **Step 5: Implement receiver mutation semantics**

Expose a runtime-state invalidation helper from `McpNotificationHelper`.
Handle `ACTION_DISMISS` by invalidating runtime state and saving saturated
`now + BA_AP_DISMISS_SNOOZE_INTERVAL_MS`. Handle `ACTION_MARK_READ` by
cancelling, clearing dismissal state, writing the read anchor, and scheduling
the next BA alarm.

- [ ] **Step 6: Verify GREEN and commit**

Run all focused app tests and module routing tests, then commit:

```bash
git add app feature-mcp core-notification
git commit -m "feat(ba): snooze dismissed AP reminders"
```

### Task 3: Full Verification And Artifact Refresh

**Files:**
- Verify: all changed source and test files.
- Preserve: untracked `app/benchmarkRelease/`.
- Produce: `app/build/outputs/apk/benchmark/app-benchmark.apk`.

**Interfaces:**
- Consumes: completed Task 1 and Task 2 behavior.
- Produces: verified benchmark APK and clean tracked worktree.

- [ ] **Step 1: Run full tests and compilation**

```bash
gtimeout 20m ./gradlew --no-daemon --console=plain \
  :core-notification:testDebugUnitTest \
  :feature-mcp:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:compileDebugKotlin \
  > .tmp/codex-gradle/ba-dismiss-full-tests.log 2>&1
```

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 2: Build and inspect benchmark APK**

```bash
gtimeout 20m ./gradlew --no-daemon --console=plain :app:assembleBenchmark \
  > .tmp/codex-gradle/ba-dismiss-benchmark.log 2>&1
"$ANDROID_HOME/build-tools/37.0.0/apksigner" verify --verbose \
  app/build/outputs/apk/benchmark/app-benchmark.apk
"$ANDROID_HOME/build-tools/37.0.0/aapt" dump badging \
  app/build/outputs/apk/benchmark/app-benchmark.apk | head -1
```

Expected: signature verification succeeds and badging reports the current
version name/code.

- [ ] **Step 3: Audit repository and Gradle processes**

```bash
git diff --check
git status --short --branch
pgrep -af 'GradleDaemon|org.gradle.launcher.daemon' || true
```

Expected: no tracked changes remain after commits, `app/benchmarkRelease/`
remains untouched, and no task-owned Gradle process remains running.
