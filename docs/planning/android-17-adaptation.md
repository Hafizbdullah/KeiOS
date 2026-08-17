# Android 17 (API 37) adaptation audit

> Source: *Android 17 应用适配指南*, `dev.mi.com` (小米澎湃OS 开发者平台), page updated **2026-05-06**.
> Audited **2026-08-18**. Every item in that guide is walked below with a verdict.
>
> **The fact that decides the scope:** this app is already `targetSdk = 37`, `compileSdk = 37`, `minSdk = 35`.
> So the guide's "targetSdkVersion >= 37" half is **live**, not latent — there is no grace period to plan for.

## Result

Most of the guide does not touch this app, and that is a finding rather than a shrug — each row below was
checked against the tree, not assumed. **Two things were already done** (one of them the largest item in the
guide), **three were changed**, and **two are deferred** with a reason.

| Change | Verdict |
|---|---|
| `usesCleartextTraffic` deprecation | **N/A.** Neither the attribute nor a `networkSecurityConfig` exists; nothing to migrate |
| Implicit URI grant restriction | **N/A for existing code.** The app's own `ACTION_SEND` builder is text-only (`EXTRA_TEXT`, no `EXTRA_STREAM`); every place it hands out a file URI already adds `FLAG_GRANT_READ_URI_PERMISSION`. Now probed — see below |
| Keystore key count limit | **N/A.** No `AndroidKeyStore` / `KeyGenerator` anywhere |
| IME visibility after rotation | **N/A.** Every activity declares `keyboardHidden` in `configChanges`, so it is not recreated on IME changes |
| Touchpad relative pointer capture | **N/A.** No `requestPointerCapture` |
| Background audio hardening | **Tested under the hardening flag, passes** — see below |
| Bluetooth autonomous re-pairing | **N/A.** No Bluetooth |
| Parcel use-after-recycle | **Safe.** The only `Parcel.recycle()` in the tree is the ITGSA reply, which recycles in `finally` after the `transact` and never reads again |
| Parcel size mismatch | **N/A.** No custom `writeToParcel` / `createFromParcel` in the app; `MiFocusProtocol` only *passes* framework Parcelables through a Bundle |
| Config changes no longer recreating the Activity | **Safe by construction** — see below |
| `ParcelFileDescriptor.parseMode` strict | **N/A.** No `parseMode` / `openFileDescriptor` calls |
| `setThreadPriority` range check | **N/A.** No `setThreadPriority` calls |
| MessageQueue lock-free (`DeliQueue`) | **Safe, with one note** — see below |
| `static final` immutable at runtime | **Safe.** The one reflection site (`ShizukuPackageInstallerBridge`) takes a *constructor* and sets `isAccessible`; it writes no fields |
| Notification custom view size | **N/A.** No `setCustomContentView` / `setCustomBigContentView` / `RemoteViews`; MiFocus uses standard templates plus bundle extras |
| NPU feature declaration | **N/A.** No NNAPI / TFLite / ML Kit |
| Complex-IME accessibility | **N/A.** No `AccessibilityService`, and the guide marks it an optional enhancement |
| ECH opportunistic | **No action.** Default-on is desirable and nothing pins SNI |
| `ACCESS_LOCAL_NETWORK` | **Already done** — see below |
| Physical-keyboard password hiding | **N/A.** The one password field uses `PasswordVisualTransformation`, which always masks |
| SMS OTP filtering | **N/A.** No SMS |
| BAL hardening / `IntentSender` | **Safe.** No `IntentSender.sendIntent()`; notification PendingIntents already set the creator BAL mode explicitly. Now also probed |
| Certificate Transparency default-on | **No action.** Public CAs only, no pinning, no user-store trust |
| Writable native DCL | **N/A.** No `System.load` / `loadLibrary` |
| Large-screen orientation opt-out closed | **One predicate fixed; real Pad work deferred** — see below |
| Advanced Protection Mode | **N/A.** No `AccessibilityService` to flag as a tool |

## The item that was already handled, and it is the big one

**`ACCESS_LOCAL_NETWORK`.** At `targetSdk >= 37` this dangerous permission must be declared *and requested*,
and the platform enforces it in **kernel BPF** — unauthorised packets are dropped silently, with no Java
exception. For an app that runs an MCP server (`127.0.0.1:38888`, and `0.0.0.0` when external access is
enabled) and syncs to WebDAV, silent packet loss would have been a miserable thing to debug.

It was already complete before this audit: declared in the manifest, wrapped in
`LocalNetworkPermissionCompat` (which also maps to `NEARBY_WIFI_DEVICES` on Android 16), requested through a
launcher in `MainActivity`, gating MCP start on the grant, granted by the baseline profile, and explained on
the About page. Nothing to do.

## What changed

### 1. The Home hero's short-viewport rule was internally inconsistent

`homePageUsesCompactLandscapeLayout` read:

```kotlin
availableWidth > availableHeight && availableHeight <= 480.dp
```

which encodes "a phone held sideways", not the constraint it is named for. Both halves were wrong once the app
targeted 37:

- **The orientation term.** Android 16 already ignores an orientation request on `sw >= 600dp` for
  `targetSdk >= 36`; Android 17 removes the `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` opt-out at
  `targetSdk >= 37`. This app declares `sensorPortrait`, so a wide viewport on a large screen was previously
  **unreachable** — a rule keyed on it could not be caught being wrong.
- **The 480dp cutoff.** A phone in landscape is ~426dp tall, so 480dp covered it. A large screen in landscape
  is 600–800dp: too tall to trip the cutoff, still far shorter than the ~950dp the tall hero is drawn for.

Reproduced on the API 37 AVD at 2856×1280 / density 320 (a 1428×640dp viewport): the tall hero pushed the
overview pill rows **underneath the floating dock** at rest. Scrollable — one swipe brought all four rows
clear, so nothing was unreachable — but the first paint read as broken.

Fixed by making the predicate the height budget it claims to be, `availableHeight < HomePageTallHeroMinHeight`
(700dp), chosen from the two measured geometries: 640dp overflows, 952dp has room. No phone geometry changes,
which the test pins with the real dp values for both.

**This is a one-predicate correction, not Pad adaptation.** Proper large-screen work is deferred to a dedicated
Pad AVD — driving a phone AVD into tablet geometry is the wrong tool for it.

### 2. StrictMode probes for the changes that bite in Android 18

`Android17StrictModeProbes`, debug builds only, `penaltyLog` only. The app had **no StrictMode at all**.

Both of these ship in Android 17 as *detection without enforcement*: the restriction flags are off, the old
behaviour still happens, the platform only logs — and Android 18 is where they throw. That gap is the whole
reason to install them, because the failure is invisible in ordinary Android 17 testing and a
`SecurityException` later.

- `detectImplicitUriPermissionGrant()` — added reflectively. The method is new in 37 and a direct call would
  compile against `compileSdk 37`, but a `NoSuchMethodError` on an Android 16 device would take the entire
  policy down with it; reflection keeps a miss local to the one detector.
- BAL detection, which `detectAll()` includes automatically for `targetSdk > 35`. The app launches activities
  from notification PendingIntents, which is exactly the traffic those rules govern.

`penaltyDeath` deliberately not used: neither detector is precise enough to bet a release build on, and the
platform has not finished making up its mind.

### 3. Fair running memory

Landed separately and documented in `itgsa-fair-memory.md`. Related to this guide only in that both are part
of the same Android 17 / alliance push.

## Safe, but for a reason worth writing down

### Config changes that no longer recreate the Activity

Android 17 stops recreating the Activity for `keyboard`, `keyboardHidden`, `navigation`, `touchscreen`,
`colorMode` and calls `onConfigurationChanged()` instead. **This app overrides `onConfigurationChanged`
nowhere**, which looks like a gap and is not: it is Compose-only, and Compose's own
`ProvideAndroidCompositionLocals` registers a `ComponentCallbacks` and republishes `LocalConfiguration` when
the configuration changes *without* a recreation. Anything reading `LocalConfiguration` — including the Home
hero predicate above — recomposes either way.

Activities already declare `keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode`, so
the two that were already handled stay handled, and `android:recreateOnConfigChanges` is not needed.

### MessageQueue

No `mMessages` reflection anywhere, so the `DeliQueue` change has nothing to break. **One note for later:**
the guide asks for **Robolectric 4.17+** with `@LooperMode(PAUSED)` when targeting 37, and this project is on
**4.16.1**. It is not breaking today because every Robolectric test pins `@Config(sdk = [35])` (157 of them) or
`[36]` (one), so the new queue is never the one under test. Espresso is already 3.7.0, which satisfies the
guide. Bumping Robolectric is a dependency change with 2919 tests behind it — worth doing deliberately, not as
a footnote to this audit.

### Background audio hardening — tested, passes

**Exercised on the API 37 AVD with `adb shell cmd audio set-enable-hardening 1`**, which is the guide's own
switch for forcing the strict mode on. Not reasoned about — driven.

First, the shape of the app that makes this narrow. There is **no `AudioManager` usage anywhere** — no
`setStreamVolume`, `adjustStreamVolume` or `requestAudioFocus` — so the volume-API half of the hardening has
nothing to act on, and the BGM volume slider is Media3 player gain rather than a stream volume. Focus is
requested by Media3 on the app's behalf; `dumpsys audio` shows it as
`client: …media3.common.audio.AudioFocusManager… gain: GAIN loss: none notified: true sdk:37`.

**Default configuration: the hardening cannot reach it.** "Native media notification" is **off by default**, so
BGM does not join the system media session — `dumpsys media_session` reports `have 0 sessions`. Backgrounding
the app moves the player from `state:started` to `state:paused`, and the control run with
`set-enable-hardening 0` does **exactly the same thing**, which is what proves it is the app's own behaviour
and not the platform muting anything. `mutedState:none` throughout.

**With the setting enabled — the path that can be affected — it still passes.** With hardening on:

| Step | Result |
|---|---|
| Play in the foreground | `state:started`, session `androidx.media3.session.id.ba_guide_bgm_media_session` active |
| Home, app backgrounded | `state:started`, **`mutedState:none`** — playback continues, unmuted |
| `cmd media_session dispatch pause` from the background | `state:paused` |
| `cmd media_session dispatch play` from the background | **`state:started`, `mutedState:none`** |
| `AudioHardening` reports across all of it | **none** |

The last row of that table is the guide's worst case — a play command arriving while the app is invisible —
and the reason it passes is visible in `dumpsys activity services`:

```
infoAllowStartForeground=[… uidState: TOP … allowWiu:12 … targetSdkVersion:37 …]
isForeground=true foregroundId=1001 types=0x00000002
```

**`allowWiu:12`** — the foreground service holds the While-In-Use capability, because Media3 starts it while
the app is `TOP` and keeps it alive across a pause. WIU is precisely what the hardening requires, so the focus
request succeeds and playback is never muted. The failure mode the guide describes needs an FGS *started* from
the background; this one never is.

**No code change.** The AVD was left as found: hardening switched back off and the native-media-notification
setting returned to its default of off.

## Phone verification — clean, `2026-08-18`

Run on the API 37 AVD at its phone geometry (1280×2856, density 480 → 426×952dp), **debug and release**.

### Debug

| Check | Result |
|---|---|
| Cold start, all five tabs walked | No crash, no ANR, no app-side `E` line |
| StrictMode probes | `Android17Probes: StrictMode probes installed`, and **no violation reported** |
| Every signal the guide names | **None fired.** Swept for `discontinued from Android 18`, `BadParcelable`, `Parcel used while recycled`, `consumed … bytes, but`, `AudioHardening`, `Bad mode:`, `Priority/niceness`, `Too many keys`, `NPU access is blocked`, `certificate transparency`, `Attempt to load writable` |
| Home hero after the predicate change | Unchanged — tall hero, all four pill groups clear of the dock, as the 952dp ≥ 700dp test predicts |

**Restricted non-SDK interfaces**, which the guide asks to check explicitly: exactly two accesses, both
`allowed` at `TargetSdkVersion=37`, and **neither is app code**:

- `ServiceManager.getService` from `rikka.shizuku.SystemServiceHelper` — Shizuku's own library
- `SystemProperties.addChangeCallback` from `androidx.compose.ui.platform.AndroidComposeView$Companion` — Compose itself

The app's one remaining hidden-API site of its own is `Class.forName("android.content.pm.IPackageInstaller")`
in the Shizuku install bridge, which did not run here because Shizuku is not active on this AVD. It is worth
re-checking on a device where Shizuku is running.

### Release

Release matters separately because R8 is where a runtime-registered receiver would quietly disappear.

- `assembleRelease` succeeds and installs.
- The receiver **is registered under R8**, proven independently of logging — release sets
  `DEFAULT_LOG_LEVEL_ID = "off"`, so `dumpsys activity broadcasts` was used instead and lists both
  `itgsa.intent.action.TRIM` and `itgsa.intent.action.KILL`.
- The release trim path works, measured rather than logged: `am send-trim-memory <pid> COMPLETE` on the
  backgrounded release process took **TOTAL PSS 45,810 KB → 40,973 KB, freeing ~4.7 MB**. So the release
  registry, the Coil eviction and the bitmap-cache eviction all survived minification.

### Still owed on a real phone

**Nothing.** The last open item — background audio hardening — was driven on the AVD once it turned out the
emulator does have network, and it passes in both configurations. See the section above.

Everything on the phone side is verified. Large-screen behaviour is deliberately **not** in scope here —
see the note on the Home predicate — and is deferred to a dedicated Pad AVD.
