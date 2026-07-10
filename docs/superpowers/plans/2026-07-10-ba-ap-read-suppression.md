# BA AP Read Suppression Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make ordinary BA AP and cafe stored-AP `Mark read` actions persist locally per account and suppress repeat notifications until AP falls below threshold or, in hourly mode, until the next one-hour reminder window.

**Architecture:** Keep the user-facing behavior setting in the existing synchronized global/custom BA reminder model. Store acknowledgement anchors in a dedicated device-local MMKV accessor, then route foreground notification sync, background reminder evaluation, and Alarm scheduling through one pure eligibility policy before Android Live Updates, Legacy notifications, or Xiaomi Magic render the notification.

**Tech Stack:** Kotlin 2.4, Kotlin serialization, Jetpack Compose, AndroidX NotificationCompat/Live Updates, MMKV, coroutines, Robolectric, JUnit 4, Gradle, R8 benchmark build.

## Global Constraints

- Preserve the existing notification templates, Xiaomi Focus JSON, notification IDs, renderer selection, and Xiaomi Magic dispatcher.
- Apply the mechanism to ordinary AP and cafe stored AP independently for every BA account.
- Keep acknowledgement anchors device-local and outside BA transfer JSON, WebDAV fingerprints, and conflict merge state.
- Synchronize `keepApRemindersReadUntilBelowThreshold` through the existing global/custom BA reminder settings; its default is `true`.
- Hourly mode permits at most one successful repeat delivery per hour after acknowledgement.
- Android Live Updates, Legacy notifications, Xiaomi Magic, and Super Island share the same mark-read `PendingIntent` and local policy.
- Keep min SDK 35 and the current Compose, Navigation 3, Miuix, StateFlow, MMKV, and coroutine architecture.
- Add no dependency.
- Add Chinese, English, and Japanese strings through resources.
- Keep production files focused and below 1000 lines where practical.
- Run Gradle through `gtimeout`, `--no-daemon`, `--console=plain`, and redirected files under `.tmp/codex-gradle/`.
- Preserve the existing untracked `app/benchmarkRelease/` directory and unrelated worktree changes.

## File Structure

**New production files**

- `app/src/main/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStore.kt`: device-local key schema and per-account/per-kind anchor operations.
- `app/src/main/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicy.kt`: pure suppression and hourly eligibility decisions.
- `app/src/main/java/os/kei/feature/notification/BaApMarkReadTarget.kt`: pure validation of BA AP mark-read metadata.

**New test files**

- `app/src/test/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStoreTest.kt`
- `app/src/test/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicyTest.kt`
- `app/src/test/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinatorTest.kt`
- `app/src/test/java/os/kei/feature/notification/BaApMarkReadTargetTest.kt`
- `app/src/test/java/os/kei/ui/page/main/sync/WebDavSyncDataPortsTest.kt`
- `feature-mcp/src/test/java/os/kei/mcp/notification/McpNotificationMarkReadIntentTest.kt`
- `core-notification/src/test/java/os/kei/core/notification/live/builder/NotificationActionRoutingTest.kt`

**Primary modified files**

- BA model and persistence: `BaAccountModels.kt`, `BaAccountStore.kt`, `BaSettingsStoreKeys.kt`, `BaSettingsSnapshotLoader.kt`, `BaSettingsStore.kt`, `BaPageModels.kt`, `BaAccountSnapshotMapper.kt`, `WebDavSyncDataPorts.kt`.
- Reminder decisions: `BaReminderCoordinator.kt`, `AppForegroundInfoHandler.kt`, `AppBackgroundSchedulePolicy.kt`, `AppBackgroundScheduler.kt`.
- Foreground state: `BaOfficeController.kt`, `BaApNotificationSyncCoordinator.kt`, `BaPageEffects.kt`, `BaOfficeActionCoordinator.kt`, `BaPageSheetHost.kt`.
- Mark-read routing: `McpAppIntentContract.kt`, `McpNotificationHelper.kt`, `MiFocusNotificationActionReceiver.kt`.
- Settings UI: `BaPageDraftState.kt`, `BaNotificationSettingsSheet.kt`, `BaPageBindings.kt`, `BaSettingsPersistenceRepository.kt`, `BaOfficePageRepository.kt`, `BaOfficeViewModel.kt`, `BaAccountManagementSheet.kt`, and localized `strings_ba.xml` files.

---

### Task 1: Add the synchronized behavior setting

**Files:**
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaAccountModels.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaAccountStore.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStoreKeys.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsSnapshotLoader.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStore.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaPageModels.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaAccountSnapshotMapper.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncDataPorts.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountModelsTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountStoreTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountTransferTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountSnapshotMapperTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/sync/WebDavSyncDataPortsTest.kt`

**Interfaces:**
- Consumes: existing `BaGlobalReminderSettings`, `BaAccountReminderOverride`, account transfer serialization, and effective reminder resolution.
- Produces: `BaGlobalReminderSettings.keepApRemindersReadUntilBelowThreshold: Boolean`, the matching override field, `BaPageSnapshot.keepApRemindersReadUntilBelowThreshold`, and `BASettingsStore.saveGlobalReminderSettings(settings)`.

- [ ] **Step 1: Write failing model and transfer tests**

Add these tests and assertions:

~~~kotlin
@Test
fun `global reminder settings default persistent AP read suppression on`() {
    assertTrue(BaGlobalReminderSettings().keepApRemindersReadUntilBelowThreshold)
}

@Test
fun `account reminder override preserves AP read suppression mode`() {
    val accountId = BaAccountId("cn-main")
    val override =
        BaGlobalReminderSettings(
            keepApRemindersReadUntilBelowThreshold = false,
        ).toAccountReminderOverride(accountId)

    assertFalse(override.keepApRemindersReadUntilBelowThreshold)
}
~~~

Extend the transfer round-trip setup and assertion:

~~~kotlin
globalReminderSettings =
    BaGlobalReminderSettings(
        apNotifyEnabled = true,
        keepApRemindersReadUntilBelowThreshold = false,
    )

assertFalse(parsed.globalReminderSettings.keepApRemindersReadUntilBelowThreshold)
~~~

Extend the snapshot mapper test with a custom account override and:

~~~kotlin
assertFalse(snapshot.keepApRemindersReadUntilBelowThreshold)
~~~

Add a data-port revision test:

~~~kotlin
@Test
fun `BA account fingerprint revision changes with reminder setting schema`() {
    assertEquals(3, BA_ACCOUNTS_FINGERPRINT_REVISION)
}
~~~

- [ ] **Step 2: Run focused tests and verify RED**

~~~bash
mkdir -p .tmp/codex-gradle
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests 'os.kei.ui.page.main.ba.support.BaAccountModelsTest' --tests 'os.kei.ui.page.main.ba.support.BaAccountStoreTest' --tests 'os.kei.ui.page.main.ba.support.BaAccountTransferTest' --tests 'os.kei.ui.page.main.ba.support.BaAccountSnapshotMapperTest' --tests 'os.kei.ui.page.main.sync.WebDavSyncDataPortsTest' > .tmp/codex-gradle/ba-ap-setting-red.log 2>&1
~~~

Expected: compilation fails because `keepApRemindersReadUntilBelowThreshold` does not exist.

- [ ] **Step 3: Add the setting to synchronized models and mappings**

Add the property to both serializable setting models:

~~~kotlin
@Serializable
internal data class BaGlobalReminderSettings(
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val keepApRemindersReadUntilBelowThreshold: Boolean = true,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
)

@Serializable
internal data class BaAccountReminderOverride(
    val accountId: BaAccountId,
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val keepApRemindersReadUntilBelowThreshold: Boolean = true,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
)
~~~

Copy the property in `effectiveReminderSettings()` and `toAccountReminderOverride()`.

Add and use this MMKV key in `BaAccountStore.loadGlobalReminderSettings()`, `saveGlobalReminderSettings()`, and `saveGlobalReminderSettingsFromSync()`:

~~~kotlin
internal const val KEY_KEEP_AP_REMINDERS_READ_UNTIL_BELOW_THRESHOLD =
    "keep_ap_reminders_read_until_below_threshold"
~~~

~~~kotlin
keepApRemindersReadUntilBelowThreshold =
    store.decodeBool(KEY_KEEP_AP_REMINDERS_READ_UNTIL_BELOW_THRESHOLD, true)
~~~

~~~kotlin
store.encode(
    KEY_KEEP_AP_REMINDERS_READ_UNTIL_BELOW_THRESHOLD,
    normalized.keepApRemindersReadUntilBelowThreshold,
)
~~~

Expose one atomic save entry point:

~~~kotlin
fun saveGlobalReminderSettings(settings: BaGlobalReminderSettings) {
    migratedAccountStore().saveGlobalReminderSettings(settings)
    notifyChanged()
}
~~~

Add the setting to `BaPageSnapshot` with default `true`, load it in `BaSettingsSnapshotLoader`, and map the effective account value in `BaAccountSnapshotMapper.withBaAccount()`.

Define and use the BA account data-port revision:

~~~kotlin
internal const val BA_ACCOUNTS_FINGERPRINT_REVISION = 3

fingerprintRevision = BA_ACCOUNTS_FINGERPRINT_REVISION,
~~~

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the Step 2 command again.

Expected: all four test classes pass.

- [ ] **Step 5: Commit**

~~~bash
git add app/src/main/java/os/kei/ui/page/main/ba/support app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncDataPorts.kt app/src/test/java/os/kei/ui/page/main/ba/support
git commit -m "feat(ba): model AP read suppression setting"
~~~

---

### Task 2: Add the device-local acknowledgement store

**Files:**
- Create: `app/src/main/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStore.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStore.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaPageModels.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeController.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStoreTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountTransferTest.kt`

**Interfaces:**
- Consumes: `BaAccountKeyValueStore`, `BaAccountId`, `BaPageSnapshot`, and `InMemoryBaAccountKeyValueStore`.
- Produces: `BaApReminderKind`, `BaApAcknowledgementStore.loadSuppressionAnchor()`, `setSuppressionAnchor()`, `clear()`, `clearAccount()`, and matching `BASettingsStore` wrappers.

- [ ] **Step 1: Write failing local-store tests**

~~~kotlin
class BaApAcknowledgementStoreTest {
    private val backing = InMemoryBaAccountKeyValueStore()
    private val store = BaApAcknowledgementStore(backing)

    @Test
    fun `anchors are isolated by account and AP kind`() {
        val first = BaAccountId("cn-main")
        val second = BaAccountId("jp-main")

        store.setSuppressionAnchor(first, BaApReminderKind.Ap, 1_000L)
        store.setSuppressionAnchor(first, BaApReminderKind.CafeAp, 2_000L)
        store.setSuppressionAnchor(second, BaApReminderKind.Ap, 3_000L)

        assertEquals(1_000L, store.loadSuppressionAnchor(first, BaApReminderKind.Ap))
        assertEquals(2_000L, store.loadSuppressionAnchor(first, BaApReminderKind.CafeAp))
        assertEquals(3_000L, store.loadSuppressionAnchor(second, BaApReminderKind.Ap))
    }

    @Test
    fun `clear account removes both AP anchors`() {
        val accountId = BaAccountId("cn-main")
        store.setSuppressionAnchor(accountId, BaApReminderKind.Ap, 1_000L)
        store.setSuppressionAnchor(accountId, BaApReminderKind.CafeAp, 2_000L)

        assertTrue(store.clearAccount(accountId))
        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.Ap))
        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.CafeAp))
    }

    @Test
    fun `negative anchors normalize to zero`() {
        val accountId = BaAccountId("cn-main")

        store.setSuppressionAnchor(accountId, BaApReminderKind.Ap, -10L)

        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.Ap))
    }
}
~~~

Add a transfer assertion:

~~~kotlin
assertFalse(raw.contains("suppression_anchor", ignoreCase = true))
~~~

- [ ] **Step 2: Run store tests and verify RED**

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests 'os.kei.ui.page.main.ba.support.BaApAcknowledgementStoreTest' --tests 'os.kei.ui.page.main.ba.support.BaAccountTransferTest' > .tmp/codex-gradle/ba-ap-store-red.log 2>&1
~~~

Expected: compilation fails because `BaApAcknowledgementStore` and `BaApReminderKind` do not exist.

- [ ] **Step 3: Implement the local store**

~~~kotlin
package os.kei.ui.page.main.ba.support

internal enum class BaApReminderKind(
    val keyPart: String,
) {
    Ap("ap"),
    CafeAp("cafe_ap"),
}

internal class BaApAcknowledgementStore(
    private val store: BaAccountKeyValueStore,
) {
    fun loadSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long = store.decodeLong(key(accountId, kind), 0L).coerceAtLeast(0L)

    fun setSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ): Boolean {
        val key = key(accountId, kind)
        val normalized = anchorAtMs.coerceAtLeast(0L)
        if (store.decodeLong(key, 0L).coerceAtLeast(0L) == normalized) return false
        if (normalized == 0L) {
            store.removeValueForKey(key)
        } else {
            store.encode(key, normalized)
        }
        return true
    }

    fun clear(accountId: BaAccountId, kind: BaApReminderKind): Boolean =
        setSuppressionAnchor(accountId, kind, 0L)

    fun clearAccount(accountId: BaAccountId): Boolean =
        BaApReminderKind.entries
            .map { kind -> clear(accountId, kind) }
            .any { it }

    private fun key(accountId: BaAccountId, kind: BaApReminderKind): String =
        "ba_ap_read_anchor:${kind.keyPart}:${accountId.value}"
}
~~~

- [ ] **Step 4: Expose local operations and enrich snapshots**

Add `BaPageSnapshot.apSuppressionAnchorAtMs` and `cafeApSuppressionAnchorAtMs`, both defaulting to `0L`.

Add these wrappers:

~~~kotlin
private fun apAcknowledgementStore(): BaApAcknowledgementStore =
    BaApAcknowledgementStore(accountKeyValueStore())

fun loadAccountApSuppressionAnchor(
    accountId: BaAccountId,
    kind: BaApReminderKind,
): Long = apAcknowledgementStore().loadSuppressionAnchor(accountId, kind)

fun saveAccountApSuppressionAnchor(
    accountId: BaAccountId,
    kind: BaApReminderKind,
    anchorAtMs: Long,
): Boolean =
    apAcknowledgementStore()
        .setSuppressionAnchor(accountId, kind, anchorAtMs)
        .also { changed ->
            if (changed) notifyChanged(notifyHomeOverview = false)
        }

fun clearAccountApAcknowledgements(accountId: BaAccountId): Boolean =
    apAcknowledgementStore()
        .clearAccount(accountId)
        .also { changed ->
            if (changed) notifyChanged(notifyHomeOverview = false)
        }
~~~

After resolving an active or reminder account, copy local anchors into its snapshot:

~~~kotlin
private fun BaPageSnapshot.withLocalApAcknowledgements(accountId: BaAccountId): BaPageSnapshot =
    copy(
        apSuppressionAnchorAtMs =
            loadAccountApSuppressionAnchor(accountId, BaApReminderKind.Ap),
        cafeApSuppressionAnchorAtMs =
            loadAccountApSuppressionAnchor(accountId, BaApReminderKind.CafeAp),
    )
~~~

Call `clearAccountApAcknowledgements(accountId)` when an account is deleted.
Add `keepApRemindersReadUntilBelowThreshold` plus matching mutable long anchor
state to `BaOfficeController`, including `matchesSnapshot()` and
`applySnapshot()`.

- [ ] **Step 5: Run tests and verify GREEN**

Run the Step 2 command again.

Expected: tests pass and exported BA JSON contains no local anchor field.

- [ ] **Step 6: Commit**

~~~bash
git add app/src/main/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStore.kt app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStore.kt app/src/main/java/os/kei/ui/page/main/ba/support/BaPageModels.kt app/src/main/java/os/kei/ui/page/main/ba/BaOfficeController.kt app/src/test/java/os/kei/ui/page/main/ba/support/BaApAcknowledgementStoreTest.kt app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountTransferTest.kt
git commit -m "feat(ba): persist AP read state locally"
~~~

---

### Task 3: Implement the shared policy and background scheduling

**Files:**
- Create: `app/src/main/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicy.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaReminderCoordinator.kt`
- Modify: `app/src/main/java/os/kei/core/background/AppForegroundInfoHandler.kt`
- Modify: `app/src/main/java/os/kei/core/background/AppBackgroundSchedulePolicy.kt`
- Modify: `app/src/main/java/os/kei/core/background/AppBackgroundScheduler.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStore.kt`
- Create: `app/src/test/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicyTest.kt`
- Modify: `app/src/test/java/os/kei/ui/page/main/ba/BaReminderCoordinatorTest.kt`
- Modify: `app/src/test/java/os/kei/core/background/AppBackgroundSchedulePolicyTest.kt`

**Interfaces:**
- Consumes: the setting and local anchors from Tasks 1-2.
- Produces: `BaApAcknowledgementPolicy.evaluate()`, `BaApAcknowledgementDecision`, plan reset/advance flags, and `BASettingsStore.reconcileApAcknowledgements(nowMs)`.

- [ ] **Step 1: Write failing pure-policy tests**

~~~kotlin
private const val NOW_MS = 20_000_000L

@Test
fun `persistent read suppresses while AP remains above threshold`() {
    val decision =
        BaApAcknowledgementPolicy.evaluate(
            notificationEnabled = true,
            currentDisplay = 130,
            thresholdDisplay = 120,
            keepReadUntilBelowThreshold = true,
            suppressionAnchorAtMs = NOW_MS - 10_000L,
            nowMs = NOW_MS,
        )

    assertTrue(decision.suppressed)
    assertNull(decision.nextEligibleAtMs)
    assertFalse(decision.resetSuppressionAnchor)
}

@Test
fun `hourly read schedules exact one hour boundary`() {
    val anchor = NOW_MS - 30L * 60L * 1000L
    val decision =
        BaApAcknowledgementPolicy.evaluate(
            notificationEnabled = true,
            currentDisplay = 130,
            thresholdDisplay = 120,
            keepReadUntilBelowThreshold = false,
            suppressionAnchorAtMs = anchor,
            nowMs = NOW_MS,
        )

    assertTrue(decision.suppressed)
    assertEquals(anchor + BA_AP_READ_REPEAT_INTERVAL_MS, decision.nextEligibleAtMs)
}

@Test
fun `expired hourly read bypasses level dedupe and advances after delivery`() {
    val decision =
        BaApAcknowledgementPolicy.evaluate(
            notificationEnabled = true,
            currentDisplay = 130,
            thresholdDisplay = 120,
            keepReadUntilBelowThreshold = false,
            suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
            nowMs = NOW_MS,
        )

    assertFalse(decision.suppressed)
    assertTrue(decision.bypassLastLevelDeduplication)
    assertTrue(decision.advanceSuppressionAnchorAfterDelivery)
}

@Test
fun `below threshold resets read state`() {
    val decision =
        BaApAcknowledgementPolicy.evaluate(
            notificationEnabled = true,
            currentDisplay = 119,
            thresholdDisplay = 120,
            keepReadUntilBelowThreshold = true,
            suppressionAnchorAtMs = NOW_MS,
            nowMs = NOW_MS,
        )

    assertTrue(decision.resetSuppressionAnchor)
    assertFalse(decision.eligible)
}
~~~

- [ ] **Step 2: Extend coordinator and scheduler tests for RED**

Add persistent, hourly, expired-hourly, and below-threshold cases for ordinary AP and cafe AP. Assert:

~~~kotlin
assertNull(plan.notification)
assertTrue(plan.resetSuppressionAnchor)
assertTrue(expiredPlan.advanceSuppressionAnchorAfterDelivery)
assertNull(persistentSchedule)
assertEquals(anchor + BA_AP_READ_REPEAT_INTERVAL_MS, hourlySchedule?.triggerAtMillis)
assertEquals(NOW_MS, expiredSchedule?.triggerAtMillis)
~~~

- [ ] **Step 3: Run tests and verify RED**

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests 'os.kei.ui.page.main.ba.BaApAcknowledgementPolicyTest' --tests 'os.kei.ui.page.main.ba.BaReminderCoordinatorTest' --tests 'os.kei.core.background.AppBackgroundSchedulePolicyTest' > .tmp/codex-gradle/ba-ap-policy-red.log 2>&1
~~~

Expected: compilation fails because the policy and plan fields do not exist.

- [ ] **Step 4: Implement the pure policy**

~~~kotlin
package os.kei.ui.page.main.ba

internal const val BA_AP_READ_REPEAT_INTERVAL_MS = 60L * 60L * 1000L

internal data class BaApAcknowledgementDecision(
    val eligible: Boolean,
    val suppressed: Boolean = false,
    val resetSuppressionAnchor: Boolean = false,
    val nextEligibleAtMs: Long? = null,
    val bypassLastLevelDeduplication: Boolean = false,
    val advanceSuppressionAnchorAfterDelivery: Boolean = false,
)

internal object BaApAcknowledgementPolicy {
    fun evaluate(
        notificationEnabled: Boolean,
        currentDisplay: Int,
        thresholdDisplay: Int,
        keepReadUntilBelowThreshold: Boolean,
        suppressionAnchorAtMs: Long,
        nowMs: Long,
    ): BaApAcknowledgementDecision {
        val anchor = suppressionAnchorAtMs.coerceAtLeast(0L)
        if (!notificationEnabled || currentDisplay < thresholdDisplay) {
            return BaApAcknowledgementDecision(
                eligible = false,
                resetSuppressionAnchor = anchor > 0L,
            )
        }
        if (anchor <= 0L) return BaApAcknowledgementDecision(eligible = true)
        if (keepReadUntilBelowThreshold) {
            return BaApAcknowledgementDecision(eligible = false, suppressed = true)
        }
        val nextEligibleAtMs =
            if (anchor > Long.MAX_VALUE - BA_AP_READ_REPEAT_INTERVAL_MS) {
                Long.MAX_VALUE
            } else {
                anchor + BA_AP_READ_REPEAT_INTERVAL_MS
            }
        if (nowMs < nextEligibleAtMs) {
            return BaApAcknowledgementDecision(
                eligible = false,
                suppressed = true,
                nextEligibleAtMs = nextEligibleAtMs,
            )
        }
        return BaApAcknowledgementDecision(
            eligible = true,
            bypassLastLevelDeduplication = true,
            advanceSuppressionAnchorAfterDelivery = true,
        )
    }
}
~~~

- [ ] **Step 5: Integrate background reminder plans**

Add to both AP plan types:

~~~kotlin
val resetSuppressionAnchor: Boolean = false,
val advanceSuppressionAnchorAfterDelivery: Boolean = false,
~~~

Evaluate the policy after AP projection and before last-level dedupe. A reset decision returns the existing disabled/below-threshold plan with `resetSuppressionAnchor = true` when an anchor exists. A suppressed decision returns no notification. An expired hourly decision bypasses the equality check and marks the plan to advance after delivery.

In `AppForegroundInfoHandler`, persist kind-specific resets as anchor `0L`. After successful expired-hour delivery, save `nowMs` as the new anchor. Preserve current last-notified-level writes after successful delivery.

- [ ] **Step 6: Integrate Alarm scheduling and reconciliation**

For above-threshold AP, map decisions exactly:

~~~kotlin
when {
    decision.suppressed && decision.nextEligibleAtMs == null -> null
    decision.suppressed -> userReminderWindow(requireNotNull(decision.nextEligibleAtMs))
    decision.bypassLastLevelDeduplication -> promptUserReminder(nowMs)
    currentDisplay != snapshot.apLastNotifiedLevel -> promptUserReminder(nowMs)
    else -> nextPointSchedule
}
~~~

Apply the same mapping to cafe AP.

Add `BASettingsStore.reconcileApAcknowledgements(nowMs)`. It resolves every account, including disabled accounts, projects ordinary/cafe AP through existing regeneration functions, evaluates `BaReminderCoordinator`, and clears anchors requested by the plans. Call it at the start of `AppBackgroundScheduler.scheduleBaApThreshold()`, then reload reminder snapshots before calculating the Alarm.

- [ ] **Step 7: Run tests and verify GREEN**

Run the Step 3 command again.

Expected: policy, coordinator, and scheduler tests pass.

- [ ] **Step 8: Commit**

~~~bash
git add app/src/main/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicy.kt app/src/main/java/os/kei/ui/page/main/ba/BaReminderCoordinator.kt app/src/main/java/os/kei/core/background/AppForegroundInfoHandler.kt app/src/main/java/os/kei/core/background/AppBackgroundSchedulePolicy.kt app/src/main/java/os/kei/core/background/AppBackgroundScheduler.kt app/src/main/java/os/kei/ui/page/main/ba/support/BaSettingsStore.kt app/src/test/java/os/kei/ui/page/main/ba/BaApAcknowledgementPolicyTest.kt app/src/test/java/os/kei/ui/page/main/ba/BaReminderCoordinatorTest.kt app/src/test/java/os/kei/core/background/AppBackgroundSchedulePolicyTest.kt
git commit -m "feat(ba): apply AP read suppression policy"
~~~

---

### Task 4: Apply policy to foreground updates and AP mutations

**Files:**
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinator.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageEffects.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeController.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeActionCoordinator.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageSheetHost.kt`
- Create: `app/src/test/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinatorTest.kt`
- Modify: `app/src/test/java/os/kei/ui/page/main/ba/BaPageActionsTest.kt`

**Interfaces:**
- Consumes: `BaApAcknowledgementPolicy`, local anchor store wrappers, and existing AP dispatchers.
- Produces: foreground plans carrying local anchor reset/advance results and persistence-before-reschedule mutation ordering.

- [ ] **Step 1: Write failing foreground-plan tests**

~~~kotlin
@Test
fun `foreground persistent read suppresses regenerated AP`() {
    val plan =
        planBaApNotificationSync(
            request =
                request(
                    currentDisplay = 121,
                    lastNotifiedLevel = 120,
                    keepReadUntilBelowThreshold = true,
                    suppressionAnchorAtMs = NOW_MS - 1_000L,
                ),
            nowMs = NOW_MS,
        )

    assertFalse(plan.shouldSendThresholdNotification)
    assertFalse(plan.shouldRefreshActiveNotification)
}

@Test
fun `foreground expired hourly read sends and advances anchor`() {
    val plan =
        planBaApNotificationSync(
            request =
                request(
                    currentDisplay = 120,
                    lastNotifiedLevel = 120,
                    keepReadUntilBelowThreshold = false,
                    suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
                ),
            nowMs = NOW_MS,
        )

    assertTrue(plan.shouldSendThresholdNotification)
    assertTrue(plan.advanceSuppressionAnchorAfterDelivery)
}

@Test
fun `foreground below threshold clears local read state`() {
    val plan =
        planBaApNotificationSync(
            request = request(currentDisplay = 119, suppressionAnchorAtMs = NOW_MS),
            nowMs = NOW_MS,
        )

    assertEquals(0L, plan.nextSuppressionAnchorAtMs)
    assertEquals(-1, plan.nextLastNotifiedLevel)
}
~~~

- [ ] **Step 2: Run tests and verify RED**

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests 'os.kei.ui.page.main.ba.BaApNotificationSyncCoordinatorTest' --tests 'os.kei.ui.page.main.ba.BaPageActionsTest' > .tmp/codex-gradle/ba-ap-foreground-red.log 2>&1
~~~

Expected: compilation fails because foreground requests and plans have no suppression fields.

- [ ] **Step 3: Extend foreground request, plan, and result**

Add to `BaApNotificationSyncRequest`:

~~~kotlin
val keepReadUntilBelowThreshold: Boolean = true,
val suppressionAnchorAtMs: Long = 0L,
~~~

Add to `BaApNotificationSyncPlan`:

~~~kotlin
val nextSuppressionAnchorAtMs: Long? = null,
val advanceSuppressionAnchorAfterDelivery: Boolean = false,
~~~

Add to `BaApNotificationSyncResult`:

~~~kotlin
val suppressionAnchorAtMs: Long? = null,
~~~

Change `planBaApNotificationSync(request, nowMs)` to evaluate the shared policy before last-level dedupe. A successful expired-hour notification returns `suppressionAnchorAtMs = nowMs`. A reset returns `0L`.

- [ ] **Step 4: Persist foreground anchor changes**

Include the setting and ordinary AP anchor in the `snapshotFlow` request. After `sync()`:

~~~kotlin
result.suppressionAnchorAtMs?.let { anchorAtMs ->
    request.accountId?.let { accountId ->
        withContext(AppDispatchers.baFetch) {
            BASettingsStore.saveAccountApSuppressionAnchor(
                accountId = accountId,
                kind = BaApReminderKind.Ap,
                anchorAtMs = anchorAtMs,
            )
        }
    }
    office.apSuppressionAnchorAtMs = anchorAtMs
}
~~~

Keep last-notified-level persistence in `BaRuntimePersistenceCoordinator`.

- [ ] **Step 5: Persist AP mutations before rescheduling**

Replace immediate schedule calls after asynchronous persistence with:

~~~kotlin
private fun persistRuntimeAndReschedule(update: BaRuntimePersistenceUpdate?) {
    if (update == null) return
    scope.launch {
        update.withCurrentAccount().persistAsync()
        AppBackgroundScheduler.scheduleBaApThreshold(context)
    }
}
~~~

Use it for ordinary AP edits, cafe AP edits, and cafe AP claims. Apply the same ordering in `BaPageSheetHost` for AP-limit and cafe-calibration writes so reconciliation reads persisted values.

- [ ] **Step 6: Run tests and verify GREEN**

Run the Step 2 command again.

Expected: tests pass.

- [ ] **Step 7: Commit**

~~~bash
git add app/src/main/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinator.kt app/src/main/java/os/kei/ui/page/main/ba/BaPageEffects.kt app/src/main/java/os/kei/ui/page/main/ba/BaOfficeController.kt app/src/main/java/os/kei/ui/page/main/ba/BaOfficeActionCoordinator.kt app/src/main/java/os/kei/ui/page/main/ba/BaPageSheetHost.kt app/src/test/java/os/kei/ui/page/main/ba/BaApNotificationSyncCoordinatorTest.kt app/src/test/java/os/kei/ui/page/main/ba/BaPageActionsTest.kt
git commit -m "fix(ba): honor AP read state in foreground"
~~~

---

### Task 5: Route mark-read metadata through every renderer

**Files:**
- Modify: `feature-mcp/src/main/java/os/kei/mcp/notification/McpAppIntentContract.kt`
- Modify: `feature-mcp/src/main/java/os/kei/mcp/notification/McpNotificationHelper.kt`
- Modify: `app/src/main/java/os/kei/feature/notification/MiFocusNotificationActionReceiver.kt`
- Create: `app/src/main/java/os/kei/feature/notification/BaApMarkReadTarget.kt`
- Create: `feature-mcp/src/test/java/os/kei/mcp/notification/McpNotificationMarkReadIntentTest.kt`
- Create: `app/src/test/java/os/kei/feature/notification/BaApMarkReadTargetTest.kt`
- Modify: `app/src/test/java/os/kei/feature/notification/MiFocusNotificationActionsTest.kt`
- Create: `core-notification/src/test/java/os/kei/core/notification/live/builder/NotificationActionRoutingTest.kt`

**Interfaces:**
- Consumes: account/kind local store APIs, `BaAccountNotificationKind`, BA server names, and `BackgroundAsyncReceiverRunner`.
- Produces: public `McpNotificationMarkReadContract` extras, immutable BA metadata in the existing action, and validated local writes.

- [ ] **Step 1: Write failing metadata and target tests**

Assert the helper intent contains:

~~~kotlin
assertEquals(
    243_220,
    intent.getIntExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, -1),
)
assertEquals(
    LiveNotificationPayload.BA_AP_SERVER_NAME,
    intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME),
)
assertEquals(
    "cn-main",
    intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID),
)
~~~

In the app target test, assert a known account plus its deterministic ordinary AP ID resolves to `BaApReminderKind.Ap`; another account's ID and an unsupported server name return `null`. Retain the old-intent cancellation test in `MiFocusNotificationActionsTest`.

- [ ] **Step 2: Write failing renderer tests**

Build a payload with distinct open and mark-read pending intents, then assert:

~~~kotlin
assertEquals(markReadPendingIntent, modern.actions[1].actionIntent)
assertEquals(markReadPendingIntent, legacy.actions[1].actionIntent)
assertEquals(markReadPendingIntent, island.focusAction("mcp_action_stop").actionIntent)
~~~

Use SDK 36 for `ModernNotificationBuilder`, SDK 35 for `LegacyNotificationBuilder`, and the existing Xiaomi Focus extras helper for `MiIslandNotificationBuilder`.

- [ ] **Step 3: Run tests and verify RED**

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :feature-mcp:testDebugUnitTest --tests '*McpNotificationMarkReadIntentTest' > .tmp/codex-gradle/ba-ap-actions-feature-mcp-red.log 2>&1
gtimeout 300s ./gradlew --no-daemon --console=plain :core-notification:testDebugUnitTest --tests '*NotificationActionRoutingTest' > .tmp/codex-gradle/ba-ap-actions-core-red.log 2>&1
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests '*BaApMarkReadTargetTest' --tests '*MiFocusNotificationActionsTest' > .tmp/codex-gradle/ba-ap-actions-app-red.log 2>&1
~~~

Expected: compilation fails because the metadata contract and target resolver do not exist.

- [ ] **Step 4: Add shared metadata and helper intent**

~~~kotlin
object McpNotificationMarkReadContract {
    const val ACTION = "os.kei.focus.notification.action.MARK_READ"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_SERVER_NAME = "server_name"
    const val EXTRA_TARGET_BA_ACCOUNT_ID = "target_ba_account_id"
}
~~~

Add a testable helper:

~~~kotlin
internal fun buildMarkReadIntent(
    context: Context,
    notificationId: Int,
    serverName: String,
    targetBaAccountId: String?,
): Intent =
    Intent().apply {
        setClassName(
            context.packageName,
            McpNotificationActionContract.MI_FOCUS_ACTION_RECEIVER_CLASS_NAME,
        )
        action = McpNotificationMarkReadContract.ACTION
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        putExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME, serverName)
        targetBaAccountId?.let {
            putExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID, it)
        }
    }
~~~

Use it inside the existing private `markReadPendingIntent()`, passing the normalized account ID and server name from `buildForegroundNotificationResult()`.

- [ ] **Step 5: Validate and persist BA AP targets**

Create:

~~~kotlin
internal data class BaApMarkReadTarget(
    val accountId: BaAccountId,
    val kind: BaApReminderKind,
)

internal fun resolveBaApMarkReadTarget(
    notificationId: Int,
    serverName: String?,
    rawAccountId: String?,
    knownAccountIds: Set<BaAccountId>,
): BaApMarkReadTarget? {
    val accountId = BaAccountId(rawAccountId?.trim().orEmpty())
    if (accountId.value.isBlank() || accountId !in knownAccountIds) return null
    val kind =
        when (serverName?.trim()) {
            LiveNotificationPayload.BA_AP_SERVER_NAME -> BaApReminderKind.Ap
            LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME -> BaApReminderKind.CafeAp
            else -> return null
        }
    val notificationKind =
        when (kind) {
            BaApReminderKind.Ap -> BaAccountNotificationKind.Ap
            BaApReminderKind.CafeAp -> BaAccountNotificationKind.CafeAp
        }
    if (notificationKind.notificationId(accountId) != notificationId) return null
    return BaApMarkReadTarget(accountId, kind)
}
~~~

In `MiFocusNotificationActionReceiver`, cancel first, then use:

~~~kotlin
BackgroundAsyncReceiverRunner.launch(
    receiver = this,
    context = context,
    tag = TAG,
) { appContext ->
    val accountIds =
        BASettingsStore.loadAccountState().accounts
            .map { it.profile.id }
            .toSet()
    val target =
        resolveBaApMarkReadTarget(
            notificationId = notificationId,
            serverName =
                intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME),
            rawAccountId =
                intent.getStringExtra(
                    McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID,
                ),
            knownAccountIds = accountIds,
        ) ?: return@launch
    BASettingsStore.saveAccountApSuppressionAnchor(
        accountId = target.accountId,
        kind = target.kind,
        anchorAtMs = System.currentTimeMillis(),
    )
    AppBackgroundScheduler.scheduleBaApThreshold(appContext)
}
~~~

Keep aliases for existing receiver constants so old notifications and current callers remain compatible.

- [ ] **Step 6: Run tests and verify GREEN**

Run the Step 3 command again.

Expected: all action and renderer tests pass.

- [ ] **Step 7: Commit**

~~~bash
git add feature-mcp/src/main/java/os/kei/mcp/notification feature-mcp/src/test/java/os/kei/mcp/notification/McpNotificationMarkReadIntentTest.kt app/src/main/java/os/kei/feature/notification app/src/test/java/os/kei/feature/notification core-notification/src/test/java/os/kei/core/notification/live/builder/NotificationActionRoutingTest.kt
git commit -m "feat(notification): route BA AP read acknowledgements"
~~~

---

### Task 6: Add setting UI, persistence, and localization

**Files:**
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageDraftState.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaNotificationSettingsSheet.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageBindings.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaPageSheetHost.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaSettingsPersistenceRepository.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficePageRepository.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeViewModel.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaAccountManagementSheet.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/ba/BaOfficeUiState.kt`
- Modify: `app/src/main/res/values/strings_ba.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings_ba.xml`
- Modify: `app/src/main/res/values-en/strings_ba.xml`
- Modify: `app/src/main/res/values-ja/strings_ba.xml`
- Modify: `app/src/test/java/os/kei/ui/page/main/ba/BaPagePresentationDeriverTest.kt`
- Modify: `app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountTransferTest.kt`

**Interfaces:**
- Consumes: synchronized setting from Task 1 and the existing notification draft/save architecture.
- Produces: one global/custom switch, localized copy, atomic global reminder persistence, and scheduler refresh after save.

- [ ] **Step 1: Write failing draft and custom-setting tests**

Extend presentation tests:

~~~kotlin
assertTrue(
    presentation.notificationSettingsSheetState
        .keepApRemindersReadUntilBelowThreshold,
)
~~~

Add a saved-draft case with the property `false` and assert both current and saved sheet states retain `false`. Extend account transfer/custom override tests to retain `false` through editor input conversion and export round-trip.

- [ ] **Step 2: Run tests and verify RED**

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests 'os.kei.ui.page.main.ba.BaPagePresentationDeriverTest' --tests 'os.kei.ui.page.main.ba.support.BaAccountTransferTest' > .tmp/codex-gradle/ba-ap-ui-red.log 2>&1
~~~

Expected: compilation fails because draft and sheet state lack the property.

- [ ] **Step 3: Thread the property through UI state and atomic persistence**

Add this property to `BaPageNotificationDraftState`, `BaNotificationSettingsSheetState`, and `BaNotificationSettingsPersistenceResult`:

~~~kotlin
val keepApRemindersReadUntilBelowThreshold: Boolean,
~~~

Map it in `toNotificationDraftState()`, `buildBaNotificationSettingsSheetState()`, ViewModel snapshot application, and saved draft construction.

When `BaOfficeUiState` converts a `BaAccountReminderOverride` back to
`BaGlobalReminderSettings`, copy the property explicitly:

~~~kotlin
keepApRemindersReadUntilBelowThreshold =
    override.keepApRemindersReadUntilBelowThreshold,
~~~

Replace separate global AP/cafe/daily save calls with:

~~~kotlin
BASettingsStore.saveGlobalReminderSettings(
    BaGlobalReminderSettings(
        apNotifyEnabled = sheetState.apNotifyEnabled,
        apNotifyThreshold = savedThreshold,
        cafeApNotifyEnabled = sheetState.cafeApNotifyEnabled,
        cafeApNotifyThreshold = savedCafeApThreshold,
        keepApRemindersReadUntilBelowThreshold =
            sheetState.keepApRemindersReadUntilBelowThreshold,
        arenaRefreshNotifyEnabled = sheetState.arenaRefreshNotifyEnabled,
        cafeVisitNotifyEnabled = sheetState.cafeVisitNotifyEnabled,
    ),
)
~~~

Keep calendar and pool notification fields in their current independent stores.

- [ ] **Step 4: Add global and custom switch rows**

Bind the global callback in `BaPageSheetHost`:

~~~kotlin
onKeepApRemindersReadUntilBelowThresholdChange = { enabled ->
    viewModel.updateNotificationDraft { draft ->
        draft.copy(keepApRemindersReadUntilBelowThreshold = enabled)
    }
}
~~~

Place this row after the cafe AP controls:

~~~kotlin
SheetControlRow(
    label = stringResource(R.string.ba_settings_label_ap_read_suppression),
    summary =
        stringResource(
            if (state.keepApRemindersReadUntilBelowThreshold) {
                R.string.ba_settings_summary_ap_read_suppression_persistent
            } else {
                R.string.ba_settings_summary_ap_read_suppression_hourly
            },
        ),
) {
    AppSwitch(
        checked = state.keepApRemindersReadUntilBelowThreshold,
        onCheckedChange = onKeepApRemindersReadUntilBelowThresholdChange,
    )
}
~~~

Add the same row to `BaAccountCustomReminderEditor` by copying `BaGlobalReminderSettings`.

- [ ] **Step 5: Add localized strings**

~~~xml
<!-- values and values-zh-rCN -->
<string name="ba_settings_label_ap_read_suppression">已读后持续静默</string>
<string name="ba_settings_summary_ap_read_suppression_persistent">已读后，直到对应 AP 低于阈值前保持静默</string>
<string name="ba_settings_summary_ap_read_suppression_hourly">已读后仍达到阈值时，每小时最多再次提醒一次</string>
~~~

~~~xml
<!-- values-en -->
<string name="ba_settings_label_ap_read_suppression">Keep read AP reminders silent</string>
<string name="ba_settings_summary_ap_read_suppression_persistent">After marking as read, stays silent until the corresponding AP drops below its threshold.</string>
<string name="ba_settings_summary_ap_read_suppression_hourly">After marking as read, reminds at most once per hour while AP remains above its threshold.</string>
~~~

~~~xml
<!-- values-ja -->
<string name="ba_settings_label_ap_read_suppression">既読後は継続して通知を抑制</string>
<string name="ba_settings_summary_ap_read_suppression_persistent">既読にすると、対象の AP がしきい値を下回るまで通知を抑制します。</string>
<string name="ba_settings_summary_ap_read_suppression_hourly">既読後も AP がしきい値以上の場合、再通知は最大 1 時間に 1 回です。</string>
~~~

- [ ] **Step 6: Run tests and compile resources**

Run Step 2 again, then:

~~~bash
gtimeout 300s ./gradlew --no-daemon --console=plain :app:compileDebugKotlin > .tmp/codex-gradle/ba-ap-ui-compile.log 2>&1
~~~

Expected: tests pass and Kotlin/resource compilation succeeds.

- [ ] **Step 7: Commit**

~~~bash
git add app/src/main/java/os/kei/ui/page/main/ba app/src/main/res/values/strings_ba.xml app/src/main/res/values-zh-rCN/strings_ba.xml app/src/main/res/values-en/strings_ba.xml app/src/main/res/values-ja/strings_ba.xml app/src/test/java/os/kei/ui/page/main/ba app/src/test/java/os/kei/ui/page/main/ba/support/BaAccountTransferTest.kt
git commit -m "feat(ba): expose AP read suppression setting"
~~~

---

### Task 7: Run integrated verification and prepare the device build

**Files:**
- Verify: all files changed in Tasks 1-6.
- Preserve: `app/benchmarkRelease/` as an unrelated untracked directory.

**Interfaces:**
- Consumes: complete implementation from Tasks 1-6.
- Produces: passing unit suites, a minified benchmark APK, renderer evidence, and a clean task-specific diff.

- [ ] **Step 1: Run all focused suites**

~~~bash
gtimeout 600s ./gradlew --no-daemon --console=plain :feature-mcp:testDebugUnitTest --tests '*McpNotificationMarkReadIntentTest' > .tmp/codex-gradle/ba-ap-focused-feature-mcp.log 2>&1
gtimeout 600s ./gradlew --no-daemon --console=plain :core-notification:testDebugUnitTest --tests '*NotificationActionRoutingTest' > .tmp/codex-gradle/ba-ap-focused-core-notification.log 2>&1
gtimeout 600s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest --tests '*BaApAcknowledgementStoreTest' --tests '*BaApAcknowledgementPolicyTest' --tests '*BaReminderCoordinatorTest' --tests '*AppBackgroundSchedulePolicyTest' --tests '*BaApNotificationSyncCoordinatorTest' --tests '*BaApMarkReadTargetTest' --tests '*MiFocusNotificationActionsTest' --tests '*BaAccountTransferTest' --tests '*BaPagePresentationDeriverTest' --tests '*WebDavSyncDataPortsTest' > .tmp/codex-gradle/ba-ap-focused-app.log 2>&1
~~~

Expected: exit code `0` and no failed tests.

- [ ] **Step 2: Run the full app unit suite**

~~~bash
gtimeout 1200s ./gradlew --no-daemon --console=plain :app:testDebugUnitTest > .tmp/codex-gradle/ba-ap-full-app-tests.log 2>&1
~~~

Expected: exit code `0`. Poll the log and process state at bounded intervals; diagnose when `gtimeout` expires.

- [ ] **Step 3: Build the minified benchmark APK**

~~~bash
gtimeout 1800s ./gradlew --no-daemon --console=plain :app:assembleBenchmark > .tmp/codex-gradle/ba-ap-benchmark-build.log 2>&1
~~~

Expected: exit code `0`, R8 completes, and an APK exists under `app/build/outputs/apk/benchmark/`.

- [ ] **Step 4: Inspect diff and APK metadata**

~~~bash
git status --short --branch
git diff --check
git log -7 --oneline
~~~

Expected: task changes and the pre-existing `app/benchmarkRelease/` remain; `git diff --check` reports no whitespace errors.

Run `apksigner verify --verbose --print-certs` and `aapt dump badging` on the generated APK. Expected: signature verification succeeds and package metadata matches the benchmark variant.

- [ ] **Step 5: Validate both renderer families**

On the connected Xiaomi API 36 device:

1. Install the benchmark APK.
2. Enable ordinary AP and cafe AP threshold notifications.
3. Verify Super Island shows `Mark read` and each AP kind suppresses independently.
4. Verify persistent mode survives process death and stays silent until AP drops below threshold.
5. Verify hourly mode repeats once after one hour and advances the next boundary after successful delivery.
6. Verify lowering AP below threshold clears the episode and allows a later threshold crossing.

On a non-Xiaomi device/emulator or renderer-forced generic path:

1. Trigger Android Live Updates for both AP kinds.
2. Tap the same `Mark read` action.
3. Repeat persistent and hourly checks.
4. Confirm the Legacy builder path on SDK 35.

Expected: all renderers share device-local state, and mark-read interaction creates no BA WebDAV dirty state or automatic sync.

- [ ] **Step 6: Confirm verification introduced no uncommitted source changes**

~~~bash
git status --short
~~~

Expected: only the pre-existing untracked `app/benchmarkRelease/` directory is
reported. A verification defect starts a new focused RED-GREEN cycle against
the concrete production and test files that exposed it before this final check
is repeated.
