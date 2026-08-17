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
| Background audio hardening | **Analysed, not exercised** — see below |
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

### Background audio hardening

Analysed, **not exercised**. The app has **no `AudioManager` usage at all** — no `setStreamVolume`,
`adjustStreamVolume` or `requestAudioFocus` — so the volume-API half of the hardening cannot touch it. Playback
is Media3 `MediaSessionService` with `foregroundServiceType="mediaPlayback"` and
`setAudioAttributes(handleAudioFocus = true)`, so focus is requested by the library, and the per-track volume
slider is player gain rather than a stream volume.

The residual risk is narrow and real: an FGS **started from the background** does not get the While-In-Use
capability, so a focus request made then returns `AUDIOFOCUS_REQUEST_FAILED` and Media3 will decline to play —
silently. Concretely: pause, background the app, press play in the notification.

`adb shell cmd audio set-enable-hardening 1` is accepted on the API 37 AVD, but the flow could not be driven
there: BGM playback needs favourites, and populating them needs a network lookup this emulator has none for.

**Owed on the phone**, alongside the other physical-device items:

1. `adb shell cmd audio set-enable-hardening 1`.
2. Play a BGM favourite, pause, background the app, then press **play from the notification**.
3. Pass = it plays. Fail = nothing happens and logcat shows `AudioHardening focus request ... ignored`.
4. `adb shell cmd audio set-enable-hardening 0` afterwards.

If it fails, the fix is to surface the denial rather than swallow it — Media3 reports it as
`playWhenReady = false` with `PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS` — and to keep the session's FGS
alive across short pauses so playback resumes from a foreground-started service.
