# KeepAlive Clean-Room Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a clean-room keep-alive and accessibility-guard capability for KeiOS, using ThemeStore dev branch only as behavior-level reference and fitting KeiOS settings, Shizuku, notification, background scheduling, and history architecture.

**Architecture:** Add a focused `feature-keepalive` module for diagnostics, guard state, privileged secure-setting access, foreground guard service, receiver entry points, and audit history. Keep UI integration in the existing Settings `KeepAlive` bottom tab so the app preserves its Route/ViewModel/Screen split and current settings search, expansion, i18n, and bottom chrome behavior.

**Tech Stack:** Android SDK 35-37, Kotlin Coroutines, StateFlow, MMKV for small guard preferences, bounded JSONL/file storage for append-only history, existing `core-system` Shizuku shell bridge, existing `core-concurrency` dispatchers, Compose Settings UI, Miuix components, existing notification framework.

## Global Constraints

- Project min SDK is 35 and compile/target SDK is 37.
- Local Android framework source priority: `/Users/voyager/Library/Android/sdk/sources/android-37.1`, then `/Users/voyager/Library/Android/sdk/sources/android-36.1`, then `/Users/voyager/Library/Android/sdk/sources/android-35`.
- ThemeStore is AGPL and can serve as behavior-level reference only; KeiOS implementation uses new names, new module boundaries, new code, and existing KeiOS helpers.
- Existing notification framework remains the source of truth for notification channels and rendering.
- New strings live in `values`, `values-zh-rCN`, `values-en`, and `values-ja`.
- Settings UI keeps existing card expansion memory, search index, bottom tab, and adaptive spacing conventions.
- Privileged actions require explicit user opt-in, Shizuku command readiness, history logging, and rate limiting.

---

## Clean-Room Boundary

ThemeStore dev branch demonstrated this behavior shape:

- a foreground service can watch selected accessibility service state;
- secure setting changes can be observed through `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`;
- Shizuku can provide a shell/root route to read and write secure settings;
- boot, package replacement, screen-on, and settings changes are useful recovery triggers;
- a settings page can show Shizuku capability, selected services, and restore results.

KeiOS implementation should use these as requirements and design inputs. The implementation should avoid ThemeStore class/file structure, method signatures, hidden API wrappers, UI wording, and resource names. The first implementation should favor the existing `ShizukuApiUtils.execCommandCancellableResult()` shell path over a Binder wrapper.

Reference files used for analysis:

- `.tmp/ThemeStore/app/src/main/AndroidManifest.xml`
- `.tmp/ThemeStore/app/src/main/java/com/merak/service/AccessibilityDaemonService.kt`
- `.tmp/ThemeStore/app/src/main/java/com/merak/core/accessibility/AccessibilityServiceManager.kt`
- `.tmp/ThemeStore/app/src/main/java/com/merak/core/os/shizuku/PrivilegedManager.kt`
- `.tmp/ThemeStore/app/src/main/java/com/merak/core/os/shizuku/service/PrivilegedService.kt`

## Current KeiOS Anchors

| Area | Existing path | Reuse plan |
| --- | --- | --- |
| Settings bottom tabs | `app/src/main/java/os/kei/ui/page/main/settings/page/SettingsCategory.kt` | Keep `SettingsCategory.KeepAlive` as the user entry |
| Keep-alive settings card | `app/src/main/java/os/kei/ui/page/main/settings/section/SettingsPermissionKeepAliveSection.kt` | Split into more focused cards and add accessibility guard cards |
| Settings state | `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageViewModel.kt` | Add a `SettingsKeepAliveGuardUiState` flow |
| Settings support controller | `app/src/main/java/os/kei/ui/page/main/settings/support/SettingsPermissionKeepAliveSupport.kt` | Keep Android/OEM/background diagnostics here and delegate new guard data to feature module |
| Shizuku shell | `core-system/src/main/java/os/kei/core/shizuku/ShizukuApiUtils.kt` | Reuse command readiness, timeout, cancellation, and bounded output |
| Background system receiver | `app/src/main/java/os/kei/core/background/AppBackgroundSystemEventReceiver.kt` | Keep app-wide reschedule logic; feature module gets its own narrow guard receiver |
| MCP FGS | `feature-mcp/src/main/java/os/kei/mcp/service/McpKeepAliveService.kt` | Use as local FGS style reference while keeping MCP notification behavior isolated |
| Manifest permissions | `app/src/main/AndroidManifest.xml` | Reuse existing notification, FGS, battery, and boot permissions; add feature-specific declarations in module manifest |

## Priority Table

| Priority | Deliverable | User-visible result | Verification |
| --- | --- | --- | --- |
| P0 | Read-only diagnostics and history | User sees current background, Shizuku, and accessibility capability clearly | Unit tests plus Settings screen AVD check |
| P1 | Explicit accessibility guard configuration | User can select services to guard and see selected target state | Robolectric/domain tests plus API 36/37 AVD checks |
| P1 | Foreground daemon and recovery triggers | Selected services can be checked after boot/package replacement/screen-on/settings change | adb secure setting tests with Shizuku-ready device |
| P1 | Guard history and export | User can see restore attempts, failures, cooldowns, and reasons | Unit tests plus export file inspection |
| P2 | Settings UI refinement and i18n | KeepAlive tab becomes a clean control center | Screenshot review on phone and compact DPI |
| P2 | Android 16/17 behavior validation | API 36 daily-device behavior and API 37 target behavior are documented | AVD API 36/37 logs and screenshots |
| P3 | Quick Settings tile and notification actions | User gets fast manual check/restore entry points | Manual device check |
| P3 | Optional self-protection accessibility overlay | Advanced users get stronger keep-alive with clear disclosure | Separate design review and privacy review |

## File Structure

### Create

- `feature-keepalive/build.gradle.kts`  
  Android library module depending on `core-concurrency`, `core-log`, `core-prefs`, `core-system`, `core-json`, AndroidX Core, MMKV, and coroutines.

- `feature-keepalive/src/main/AndroidManifest.xml`  
  Starts as a minimal library manifest in Task 1. Task 6 adds `AccessibilityGuardForegroundService` with `foregroundServiceType="specialUse"`, a service-level `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, and `AccessibilityGuardEventReceiver` for explicit internal actions, boot completed, package replaced, and optional screen-on receiver enablement.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardModels.kt`  
  Owns stable public models:
  - `AccessibilityServiceId(packageName: String, serviceName: String)`
  - `AccessibilityServiceSnapshot(id, label, packageLabel, enabled, guarded, installed, system)`
  - `AccessibilityGuardCapability(shizukuReady, canReadSecureSettings, canWriteSecureSettings, notificationReady, foregroundServiceAllowed)`
  - `AccessibilityGuardRestoreReason`
  - `AccessibilityGuardRestoreResult`
  - `AccessibilityGuardHistoryEntry`

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityServiceRepository.kt`  
  Lists installed accessibility services through `AccessibilityManager`, reads enabled secure setting, normalizes component IDs, and derives snapshots.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilitySecureSettingsBridge.kt`  
  Defines the clean interface for secure setting access:
  - `suspend fun readEnabledServiceIds(): AccessibilitySecureSettingRead`
  - `suspend fun writeEnabledServiceIds(ids: Set<AccessibilityServiceId>): AccessibilitySecureSettingWrite`
  - `suspend fun setAccessibilityEnabled(enabled: Boolean): AccessibilitySecureSettingWrite`

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/ShizukuAccessibilitySecureSettingsBridge.kt`  
  Implements the interface through `ShizukuApiUtils.execCommandCancellableResult()` using `settings get secure enabled_accessibility_services`, `settings put secure enabled_accessibility_services`, and `settings put secure accessibility_enabled`.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardStore.kt`  
  Stores selected guarded service IDs, daemon enabled state, boot restore enabled state, screen-on check enabled state, and per-service cooldown timestamps.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardHistoryStore.kt`  
  Stores bounded JSONL history in app-private files, rotates by count and file size, and exposes latest entries plus export.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardCoordinator.kt`  
  Coordinates capability checks, target selection, restore decisions, rate limiting, and history writes.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/service/AccessibilityGuardForegroundService.kt`  
  Foreground service that observes secure setting changes while active and runs lightweight restore checks.

- `feature-keepalive/src/main/java/os/kei/feature/keepalive/receiver/AccessibilityGuardEventReceiver.kt`  
  Handles boot, package replacement, and explicit check actions with `goAsync()` and `BackgroundAsyncReceiverRunner`-style timeout behavior implemented inside the module.

- `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityServiceIdTest.kt`
- `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardCoordinatorTest.kt`
- `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardHistoryStoreTest.kt`
- `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/ShizukuAccessibilitySecureSettingsBridgeTest.kt`

### Modify

- `settings.gradle.kts`  
  Add `include(":feature-keepalive")`.

- `app/build.gradle.kts`  
  Add `implementation(project(":feature-keepalive"))`.

- `app/src/main/AndroidManifest.xml`  
  Keep app-level permissions as the central declaration. Add only missing permissions if feature validation proves they are required on API 36/37.

- `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageViewModel.kt`  
  Add guard state flow and user actions.

- `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageRepository.kt`  
  Add a small repository wrapper for keep-alive guard state and export actions.

- `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsSectionContractAssembler.kt`  
  Add `SettingsKeepAliveGuardSectionState` and actions to the settings contract.

- `app/src/main/java/os/kei/ui/page/main/settings/section/SettingsSectionContracts.kt`  
  Add immutable UI models for diagnostics, selected services, restore policy, and history summary.

- `app/src/main/java/os/kei/ui/page/main/settings/section/SettingsPermissionKeepAliveSection.kt`  
  Split existing keep-alive card into smaller cards:
  - Android background state
  - OEM and battery state
  - Accessibility guard
  - Guard history

- `app/src/main/java/os/kei/ui/page/main/settings/page/SettingsSearchIndex.kt`  
  Add search targets for accessibility guard, guarded services, recovery history, and Shizuku secure settings.

- `app/src/main/res/values/strings_settings.xml`
- `app/src/main/res/values-zh-rCN/strings_settings.xml`
- `app/src/main/res/values-en/strings_settings.xml`
- `app/src/main/res/values-ja/strings_settings.xml`  
  Add localized strings for capability states, guard actions, history labels, risk disclosure, and export labels.

- `app/src/test/java/os/kei/ui/page/main/settings/page/SettingsSearchIndexTest.kt`  
  Add coverage for new search targets.

- `app/src/test/java/os/kei/ui/page/main/settings/state/SettingsPageRepositoryTest.kt`  
  Add state derivation tests.

## Task 1: Scaffold `feature-keepalive`

**Files:**
- Create: `feature-keepalive/build.gradle.kts`
- Create: `feature-keepalive/src/main/AndroidManifest.xml`
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardModels.kt`
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces `AccessibilityServiceId`, `AccessibilityGuardCapability`, `AccessibilityGuardRestoreReason`, `AccessibilityGuardRestoreResult`, and `AccessibilityGuardHistoryEntry`.
- Later tasks consume these models from app UI and service logic.

- [x] Add module include and app dependency.
- [x] Add `build.gradle.kts` with Android library config, namespace `os.kei.feature.keepalive`, compile SDK 37, min SDK 35, Java 21.
- [x] Add a minimal module manifest that compiles before service classes exist.
- [x] Add model file with immutable data classes and enums.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest`.
- [x] Commit with `feat: scaffold keepalive feature module`.

## Task 2: Implement Read-Only Accessibility Diagnostics

**Files:**
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityServiceRepository.kt`
- Test: `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityServiceIdTest.kt`

**Interfaces:**
- Consumes `AccessibilityServiceId`.
- Produces:
  - `fun AccessibilityServiceId.flatten(): String`
  - `fun parseAccessibilityServiceIds(raw: String): Set<AccessibilityServiceId>`
  - `suspend fun listInstalledServices(context: Context): List<AccessibilityServiceSnapshot>`
  - `fun deriveEnabledState(installed, enabledIds, guardedIds): List<AccessibilityServiceSnapshot>`

- [x] Add tests for parsing empty values, colon-separated component names, duplicate entries, malformed entries, and legacy slash formats.
- [x] Implement parser with `ComponentName.unflattenFromString()` and stable sorting by package then service.
- [x] Add repository method using `AccessibilityManager.installedAccessibilityServiceList`.
- [x] Keep all heavy derivation on `AppDispatchers.uiDerivation` or `AppDispatchers.fileIo`.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest`.
- [x] Commit with `feat: add accessibility diagnostics model`.

## Task 3: Add Narrow Shizuku Secure Settings Bridge

**Files:**
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilitySecureSettingsBridge.kt`
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/ShizukuAccessibilitySecureSettingsBridge.kt`
- Test: `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/ShizukuAccessibilitySecureSettingsBridgeTest.kt`

**Interfaces:**
- Consumes `ShizukuApiUtils.execCommandCancellableResult()`.
- Produces:
  - `AccessibilitySecureSettingRead(rawValue, ids, success, reason)`
  - `AccessibilitySecureSettingWrite(success, changed, reason)`

- [x] Add a fake command runner wrapper for tests so shell outputs can be injected.
- [x] Test successful read from `settings get secure enabled_accessibility_services`.
- [x] Test write command quoting for flattened component names.
- [x] Test timeout and permission-denied result mapping.
- [x] Implement read/write using existing Shizuku timeout and cancellation behavior.
- [x] Set `accessibility_enabled` to `1` only when the final selected service set is non-empty.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest :core-system:testDebugUnitTest`.
- [x] Commit with `feat: add shizuku accessibility settings bridge`.

## Task 4: Add Guard Store, Coordinator, and Rate Limits

**Files:**
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardStore.kt`
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardCoordinator.kt`
- Test: `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardCoordinatorTest.kt`

**Interfaces:**
- Consumes diagnostics and secure settings bridge.
- Produces:
  - `suspend fun loadSnapshot(context): AccessibilityGuardSnapshot`
  - `suspend fun setGuarded(id, guarded): AccessibilityGuardSnapshot`
  - `suspend fun restoreMissing(reason): AccessibilityGuardRestoreResult`

- [x] Store selected targets and guard toggles in MMKV with stable keys.
- [x] Implement per-service cooldown: default 5 minutes after success, 30 minutes after repeated failure.
- [x] Implement restore decision:
  - empty selected set returns `SkippedNoTargets`;
  - Shizuku unavailable returns `SkippedMissingPrivilege`;
  - all selected services already enabled returns `SkippedAlreadyEnabled`;
  - missing selected services triggers secure setting write.
- [x] Preserve unrelated currently enabled services when writing the merged enabled-service set.
- [x] Record the before and after service-id sets in result objects.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest`.
- [x] Commit with `feat: add accessibility guard coordinator`.

## Task 5: Add Guard History and Export

**Files:**
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardHistoryStore.kt`
- Test: `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardHistoryStoreTest.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageRepository.kt`

**Interfaces:**
- Consumes `AccessibilityGuardRestoreResult`.
- Produces:
  - `suspend fun append(entry: AccessibilityGuardHistoryEntry)`
  - `suspend fun latest(limit: Int): List<AccessibilityGuardHistoryEntry>`
  - `suspend fun exportToUri(context, uri): ExportJobResult`

- [x] Store history as bounded JSONL in `filesDir/keepalive/accessibility-guard-history.jsonl`.
- [x] Cap history at 500 entries and 1 MiB.
- [x] Include timestamp, reason, trigger action, selected count, restored count, skipped count, duration, Shizuku state, failure reason, and service IDs.
- [x] Add export through Settings data/log export conventions.
- [x] Keep history out of WebDAV sync.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest :app:testDebugUnitTest`.
- [x] Commit with `feat: add accessibility guard history`.

## Task 6: Add Foreground Guard Service and Receivers

**Files:**
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/service/AccessibilityGuardForegroundService.kt`
- Create: `feature-keepalive/src/main/java/os/kei/feature/keepalive/receiver/AccessibilityGuardEventReceiver.kt`
- Modify: `feature-keepalive/src/main/AndroidManifest.xml`
- Test: `feature-keepalive/src/test/java/os/kei/feature/keepalive/accessibility/AccessibilityGuardCoordinatorTest.kt`

**Interfaces:**
- Consumes `AccessibilityGuardCoordinator.restoreMissing(reason)`.
- Produces internal actions:
  - `os.kei.keepalive.action.START_ACCESSIBILITY_GUARD`
  - `os.kei.keepalive.action.STOP_ACCESSIBILITY_GUARD`
  - `os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD`

- [x] Build a foreground notification through existing app notification helper entry points after confirming the proper channel owner.
  - Implementation note: MCP/GitHub notification channel owners stay isolated; `feature-keepalive` owns a low-importance foreground-service channel for accessibility guard runtime status.
- [x] Register a `ContentObserver` for `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` while service is running.
- [x] On observer change, call restore through coordinator with debounce of 2 seconds.
- [x] Receiver handles `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, and explicit check action.
- [x] Enable screen-on checks only when user has turned on the screen-on policy.
- [x] Use a 12-second receiver timeout and always record timeout as history.
- [x] Run `./gradlew :feature-keepalive:testDebugUnitTest`.
- [x] Commit with `feat: add accessibility guard service`.

## Task 7: Integrate Settings UI

**Files:**
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageViewModel.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsPageRepository.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/state/SettingsSectionContractAssembler.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/section/SettingsSectionContracts.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/section/SettingsPermissionKeepAliveSection.kt`
- Modify: `app/src/main/java/os/kei/ui/page/main/settings/page/SettingsSearchIndex.kt`
- Modify: `app/src/main/res/values*/strings_settings.xml`
- Test: `app/src/test/java/os/kei/ui/page/main/settings/page/SettingsSearchIndexTest.kt`
- Test: `app/src/test/java/os/kei/ui/page/main/settings/state/SettingsPageRepositoryTest.kt`

**Interfaces:**
- Consumes feature module snapshots.
- Produces Settings UI cards:
  - capability summary card;
  - Android/OEM/battery card;
  - accessibility guard service list card;
  - restore policy card;
  - latest history card.

- [ ] Add state to load guard snapshot when KeepAlive tab becomes active and when Shizuku status changes.
- [ ] Use existing `SettingsGroupCard`, `SettingsButtonActionItem`, and expansion memory.
- [ ] Keep list rendering lazy and stable-keyed by flattened service ID.
- [ ] Add user actions: select service, enable daemon, enable boot restore, enable screen-on check, run manual check, export history.
- [ ] Add concise disclosure text explaining Shizuku secure setting writes.
- [ ] Add search tokens for all new settings entries.
- [ ] Add Chinese, English, and Japanese strings.
- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Commit with `feat: surface accessibility guard in settings`.

## Task 8: Add Android 16/17 Validation Pass

**Files:**
- Modify: `docs/planning/keepalive-clean-room-plan.md`
- Create or update: `docs/qa/keepalive-accessibility-guard-validation.md`

**Interfaces:**
- Consumes final implementation.
- Produces validation evidence for API 36 and API 37.

- [ ] Confirm local SDK constants:
  - `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
  - `Settings.Secure.ACCESSIBILITY_ENABLED`
  - `Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE`
  - `PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
  - `AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE`
- [ ] Build debug and signed release variants as needed.
- [ ] Run API 36 AVD: open Settings > KeepAlive, capture capability card, service list, and history card.
- [ ] Run API 37 AVD: repeat API 36 checks.
- [ ] On a Shizuku-ready device, run:

```bash
adb shell settings get secure enabled_accessibility_services
adb shell settings get secure accessibility_enabled
adb shell dumpsys activity services | grep -i AccessibilityGuard
adb logcat -d -s KeiOS AccessibilityGuard ShizukuApiUtils
```

- [ ] Verify manual check records history for success, skipped, missing privilege, and timeout cases.
- [ ] Verify background receivers reschedule cleanly after app update and boot.
- [ ] Commit with `docs: add keepalive validation evidence`.

## Task 9: P3 Advanced Entry Points

**Files:**
- Create in P3: `feature-keepalive/src/main/java/os/kei/feature/keepalive/tile/AccessibilityGuardTileService.kt`
- Create in P3: optional self accessibility service files after privacy review

**Interfaces:**
- Consumes Task 1-8 capability and history model.
- Produces optional entry points after the core guard is stable.

- [ ] Add Quick Settings tile only after foreground daemon and settings UI pass device validation.
- [ ] Add notification actions for manual check and stop daemon only after notification channel ownership is confirmed.
- [ ] Add self accessibility watchdog only after a separate product/privacy review.
- [ ] Add direct-boot support only after selected-target storage is moved to device-protected storage.
- [ ] Commit advanced entry points separately.

## Risk Controls

- Secure setting writes preserve all currently enabled services and only add selected missing targets.
- Restore actions use explicit user-selected targets and history logging.
- Repeated failure enters cooldown and surfaces the reason in Settings.
- The first implementation uses Shizuku shell commands with bounded output and timeout.
- Broad AppOps mutation stays out of the first implementation.
- Accessibility overlay watchdog remains a P3 opt-in capability.
- Foreground service notification uses existing notification framework ownership.
- WebDAV sync excludes guard history and guard runtime state.

## Validation Commands

```bash
./gradlew :feature-keepalive:testDebugUnitTest
./gradlew :core-system:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests '*Settings*'
./gradlew :app:assembleDebug
```

Release-mode validation after implementation:

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -n os.kei/.MainActivity
adb logcat -d -s KeiOS AccessibilityGuard ShizukuApiUtils
```

## Completion Criteria

- Settings `保活` bottom tab shows Android/OEM/battery, Shizuku, accessibility guard, policy, and recent history as separate compact cards.
- Service list loads without blocking UI and remains responsive on API 36 and API 37.
- Manual guard check produces deterministic history in success, skipped, failure, and timeout paths.
- Foreground daemon observes secure setting changes only while enabled by the user.
- Boot/package replacement checks respect stored user policy.
- Shizuku unavailable state gives actionable UI and history output.
- Clean-room audit confirms ThemeStore AGPL code, names, strings, hidden API wrappers, and UI structure stayed outside the KeiOS implementation.
- All new strings are localized in Chinese, English, and Japanese.
- AVD screenshots and logs are saved in `docs/qa/keepalive-accessibility-guard-validation.md`.
