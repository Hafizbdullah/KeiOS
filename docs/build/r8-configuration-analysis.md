# R8 Configuration Analysis

## Build Configuration

- Analysis snapshot: `2026-07-10`
- Android Gradle Plugin: `9.3.0-rc01`
- Release optimization: `optimization.enable = true`
- R8 full mode: AGP default active; `android.enableR8.fullMode` override absent
- Benchmark optimization: `benchmark` uses the same release-like optimization path and package name
  for pre-release R8 / Baseline Profile verification.
- VersionCode slots: release uses semver slot `999`; benchmark/debug use the same commit-count
  suffix for versionName and versionCode, with local git metadata fallback when CI metadata is absent.
- R8 Gradle property: `android.r8.gradual.support=true`
- Release build result: `./gradlew :app:assembleRelease` passed
- Debug build result: `./gradlew :app:assembleDebug` passed
- Core notification tests: 68 passed, 0 failures, 0 errors
- MCP feature tests: 33 passed, 0 failures, 0 errors
- Benchmark build result: `./gradlew :app:assembleBenchmark` passed
- Release analyzer and mapping outputs present: `configanalyzer.pb`, `configanalyzer.html`,
  `configuration.txt`, `mapping.txt`, `seeds.txt`, `usage.txt`, `resources.txt`
- Missing-rule output: no release missing-rule file was produced

The app uses the Compose compiler plugin and has no app-level `androidx.compose.**` keep rules. Compose and Kotlin serialization rules are supplied by dependency/plugin consumer rules in the merged release configuration.

## Applied Rule Changes

| Area | Current Rule | Finding | Recommended Action |
| --- | --- | --- | --- |
| GitHub managed install binder bridge | Removed `-keep public interface ** extends android.os.IInterface { *; }` | The code reflects platform hidden interfaces by exact names in `ShizukuPackageInstallerBridge`; the release build succeeds with only the `ShizukuBinderWrapper` constructor and `queryLocalInterface` kept. | Keep the narrowed bridge rule. |
| Shizuku provider | Removed local `moe.shizuku.api.BinderContainer` and `rikka.shizuku.ShizukuProvider` rules | `dev.rikka.shizuku:provider:13.1.5` contributes the needed `BinderContainer` rules in the merged release configuration. | Rely on dependency consumer rules. |
| GitHub install package readability | Replaced `-keepnames class os.kei.feature.github.install.**` with five class-level keepnames rules | GitHub install seeds dropped from 75 to 10 after the enum rule was narrowed. The remaining install seeds are the five diagnostic entry classes and required constructors/companions. | Keep receiver, commit registry, confirm registry, installer, and bridge names readable. |
| Enum member names | Replaced `-keepclassmembernames enum os.kei.** { *; }` with explicit enum keep rules | Enum member seeds dropped from 402 to 155. The retained set covers persisted/wire-name enums for app theme, visible pages/cards, OS cards, GitHub profile/star-import/share-import state, BA catalog cache, BGM queue mode, and MCP execution profile. | Keep the explicit enum list and add future enum-name persistence to this list deliberately. |
| Navigation route keys | Replaced broad route class/member keeps with interface, serializer, `Companion`, `INSTANCE`, and `serializer(...)` anchors using `allowoptimization,allowobfuscation` | Main navigation owns an in-memory `remember` back stack, and `entry<T>` references route classes directly. Route seeds dropped from 109 to 37; route classes are now obfuscated in `mapping.txt` while serializer anchors remain available. | Keep the narrowed route rule. Add explicit `@SerialName` values before introducing persisted route snapshots across app updates. |
| Local Focus V3 protocol engine | Removed `-keepnames class os.kei.core.notification.focus.**` and the `MiIslandNotificationBuilder` class-name rule | Configuration Analyzer attributed 103 class names to the package rule and one class name to the builder rule. The encoder writes protocol keys through `JSONObject`, and callers use direct typed references. | Keep the module fully shrinkable, optimizable, and obfuscatable. Add future rules around a demonstrated reflection or JNI boundary. |

## Configuration Analyzer Snapshot

| Metric | Before | After | Change |
| --- | ---: | ---: | ---: |
| Optimization score | 98.71% | 98.74% | +0.03 percentage points |
| Obfuscation score | 98.84% | 98.88% | +0.04 percentage points |
| Shrinking score | 98.94% | 98.94% | 0.00 percentage points |
| Kept items | 4,113 | 4,009 | -104 |
| Live items | 300,393 | 300,393 | unchanged |

The release mapping confirms normal obfuscation for the local implementation. Examples include
`MiFocusCapabilityAdapter -> qw7`, `MiFocusProtocolTemplateV3 -> cy7`, and
`MiIslandNotificationBuilder -> ry7`. R8 also removed several protocol helper classes after
inlining. Explicit JSON protocol keys remain stable independently of JVM class and member names.

## Rules To Keep

- Manifest component `keepnames` rules are low risk and preserve names for About/runtime introspection.
- `rikka.shizuku.Shizuku` member rules match `ShizukuApiUtils` reflective static method access.
- `android.util.Log` side-effect stripping for `v/d/i` is release-only and keeps warning/error paths intact.
- Ktor JDK management `dontwarn` entries are harmless and scoped to Android-incompatible debug probe references.

## Validation Completed After Rule Edits

- `./gradlew :app:assembleRelease`
- `./gradlew :app:assembleRelease -Dcom.android.tools.r8.dumpkeepradiustodirectory="$PWD/tmp/r8analysis"`
- `./gradlew :app:assembleDebug`
- `./gradlew :core-notification:testDebugUnitTest :feature-mcp:testDebugUnitTest --rerun-tasks`
- `./gradlew :feature-github:testDebugUnitTest`
- `./gradlew :app:assembleBenchmark`
- `./gradlew :app:assembleRelease` after narrowing Navigation route rules
- `./gradlew :app:dependencyInsight --configuration releaseRuntimeClasspath --dependency com.xzakota.hyper.notification:focus-api` returned zero matches.
- Tracked-source scan returned zero matches for `com.xzakota`, `focus-api`, `FocusUtils`, and
  `hyper.notification` after removing four stale `FocusUtils` Baseline Profile entries.
- `apkanalyzer dex packages --defined-only` returned zero old Focus library symbols from the
  minified release APK.
- Pixel_10_Pro AVD smoke test target for the current build matrix: install
  `app/build/outputs/apk/benchmark/app-benchmark.apk`, launch package `os.kei`, and confirm the
  Home screen UI dump exposes Home, OS, MCP, GitHub, and BA tabs.
- Benchmark process logcat showed no startup `FATAL EXCEPTION`, `ClassNotFoundException`, `NoSuchMethodError`, `NoSuchFieldError`, or `VerifyError` entries after the R8 rule changes.
- Startup logcat still shows first-frame Davey/Skipped-frame entries while existing MMKV-backed page stores load; this is runtime page-loading pressure and remains part of the broader Compose/page-load performance track.
- Compared `app/build/outputs/mapping/release/seeds.txt` after edits: GitHub install seeds remain 10, and Navigation route seeds are 37.
