# KeepAlive Self-Guard Plan

**Goal:** KeepAlive should help KeiOS stay reliable in the background through self diagnostics, foreground service policy, boot/update checks, screen-on checks, Shizuku capability detection, and local history. The feature scope serves KeiOS itself.

**Current direction:** B scope reduction. The earlier selected-accessibility-service recovery design has been retired. KeiOS now reads secure settings as a capability signal, records self-check history, and avoids writing `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.

## Scope

| Area | Decision |
| --- | --- |
| Target | KeiOS self keep-alive and diagnostics |
| Shizuku use | Read secure settings capability; no accessibility secure-setting mutation |
| Accessibility services | Parse enabled service IDs only for diagnostics and legacy history compatibility |
| History | Local JSONL, schema v2, excluded from WebDAV sync |
| Settings entry | Existing Settings -> KeepAlive bottom tab |
| Notification | Existing notification framework and channel ownership stay unchanged |
| Compatibility | Android 35 minimum; API 36 and API 37 behavior must be validated |

## Architecture

| Layer | Files | Responsibility |
| --- | --- | --- |
| Feature module | `feature-keepalive/src/main/java/os/kei/feature/keepalive/accessibility` | Self-check models, state store, Shizuku read bridge, coordinator, runner, history |
| Runtime service | `feature-keepalive/src/main/java/os/kei/feature/keepalive/service` | Low-priority foreground self-guard runtime |
| Event receiver | `feature-keepalive/src/main/java/os/kei/feature/keepalive/receiver` | Boot, app update, explicit check, timeout history |
| Settings state | `app/src/main/java/os/kei/ui/page/main/settings/state` | Loads self-guard state, exports history, runs manual checks |
| Settings UI | `app/src/main/java/os/kei/ui/page/main/settings/section` | Compact policy and history cards |
| Docs | `docs/qa/keepalive-accessibility-guard-validation.md` | API 36/37 validation evidence |

## Priority Table

| Priority | Deliverable | Status | Verification |
| --- | --- | --- | --- |
| P0 | Read-only Shizuku / secure-settings capability | Done | Unit tests, AVD missing-Shizuku path |
| P0 | Self-check coordinator and runner timeout | Done | `feature-keepalive:testDebugUnitTest` |
| P0 | Local guard history schema v2 | Done | history encode/decode/export tests |
| P1 | Settings KeepAlive self-guard policy card | Done | `app:testDebugUnitTest`, AVD screenshots |
| P1 | Foreground self-guard service | Done | debug build, AVD launch |
| P1 | Boot/update and explicit check receiver | Done | receiver unit tests, explicit AVD broadcast |
| P2 | API 36 and API 37 AVD validation | In progress | `docs/qa/keepalive-accessibility-guard-validation.md` |
| P2 | Legacy cleanup | Done | old service-list and write paths removed |
| P3 | Accessibility/Shizuku advanced keep-alive | Planned | separate clean-room design before code |

## Implementation Notes

- `AccessibilityGuardSettings` stores `daemonEnabled`, `bootCheckEnabled`, and `screenOnCheckEnabled`.
- MMKV keeps the old `boot_restore_enabled` key value for compatibility while code uses boot-check naming.
- `AccessibilitySecureSettingsBridge` exposes `readEnabledServiceIds()` only.
- `ShizukuAccessibilitySecureSettingsBridge` runs a bounded `settings get secure enabled_accessibility_services` command.
- `AccessibilityGuardCoordinator.checkSelf()` checks secure-settings readability and enabled policy count.
- `AccessibilityGuardCheckRunner` wraps checks with a 12-second timeout and always attempts local history recording.
- `AccessibilityGuardHistoryStore` writes bounded JSONL under `filesDir/keepalive/accessibility-guard-history.jsonl`.
- History export format is `keios.keepalive.accessibility-guard-history`, schema version `2`, `syncScope = local_only`.
- Schema v1 restore records are decoded into schema v2 check records for compatibility.

## Clean-Room Boundary

ThemeStore dev branch was used as behavior-level research for background watch shape, secure-settings observation, Shizuku capability, and service lifecycle ideas. KeiOS implementation uses its own module boundary, model names, resource strings, UI structure, tests, and existing Shizuku shell helper.

The current B-scope implementation excludes:

- selecting other apps' accessibility services;
- writing `enabled_accessibility_services`;
- writing `accessibility_enabled`;
- per-service cooldowns;
- AppOps mutation;
- broad "guard other apps" UI.

## Validation Commands

```bash
./gradlew :feature-keepalive:testDebugUnitTest :app:testDebugUnitTest --continue
./gradlew :app:assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n os.kei.debug/os.kei.LauncherAndroidDesigns
adb -s emulator-5556 shell am start -n os.kei.debug/os.kei.LauncherAndroidDesigns
```

Explicit receiver check:

```bash
adb shell am broadcast \
  -n os.kei.debug/os.kei.feature.keepalive.receiver.AccessibilityGuardEventReceiver \
  -a os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD
```

History inspection:

```bash
adb shell run-as os.kei.debug cat files/keepalive/accessibility-guard-history.jsonl
```

## Completion Criteria

- Settings KeepAlive tab shows Android/background status, self-guard policy, and self-guard history as separate compact cards.
- The self-guard policy card contains Shizuku capability, foreground service, boot/update check, screen-on check, scope disclosure, manual check, and export.
- The UI keeps KeiOS self-policy and self-history cards as the active KeepAlive scope.
- Manual check records `Healthy`, `Checked`, `MissingPrivilege`, `Failed`, or `TimedOut` history.
- API 36 and API 37 AVDs can open the KeepAlive page after a debug install.
- AVD history after explicit broadcast contains schema v2 fields: `checkCount`, `healthyCount`, `warningCount`.
- Unit tests and debug build pass.
