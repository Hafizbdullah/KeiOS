package os.kei.platform

import android.os.Build
import android.os.StrictMode
import os.kei.core.log.AppLogger

/**
 * Debug-only StrictMode detectors for the two Android 17 changes that are **warnings now and errors later**.
 *
 * Android 17 ships both of these as detection without enforcement: the restriction flags are off, the old
 * behaviour still happens, and the platform only logs. Android 18 is where they start throwing. That gap is
 * the entire reason to install these — the failure they catch is invisible in normal testing on Android 17 and
 * a `SecurityException` on Android 18.
 *
 * Debug builds only, and `penaltyLog` only. A `penaltyDeath` here would turn a future-facing probe into a
 * crash on a platform that has not made up its mind yet, and neither detector is precise enough to bet a
 * release build on.
 *
 * ## What each one is for
 *
 * **Implicit URI permission grants.** `ACTION_SEND`, `ACTION_SEND_MULTIPLE` and `ACTION_IMAGE_CAPTURE` have
 * always had read/write access to their `EXTRA_STREAM` granted automatically. Android 18 discontinues that;
 * senders must add `FLAG_GRANT_READ_URI_PERMISSION` themselves. This app's own share builder is text-only and
 * the places it does hand out file URIs already add the flag explicitly, so the detector is here to catch a
 * *new* call site rather than an existing one — and to catch it in the window where the platform still lets it
 * work.
 *
 * **Blocked background activity launches.** `detectAll()` includes BAL detection automatically for
 * `targetSdk > 35`, which this app is, so this is mostly about making the reports visible rather than opting
 * in. The app launches activities from notification `PendingIntent`s, which is exactly the traffic the BAL
 * rules govern.
 */
object Android17StrictModeProbes {
    private const val TAG = "Android17Probes"

    /**
     * Installs the probes. Call once, from the Application, in debug builds only.
     *
     * [isDebugBuild] is a parameter rather than a `BuildConfig.DEBUG` read so the decision stays at the call
     * site and this object is testable.
     */
    fun install(isDebugBuild: Boolean) {
        if (!isDebugBuild) return
        // Below Android 17 the detector does not exist, and `detectAll` already covers what does. Guarding
        // keeps the reflection-free path on older platforms rather than relying on the builder to no-op.
        val policy =
            StrictMode.VmPolicy
                .Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .apply { addImplicitUriGrantDetection(this) }
                .build()
        runCatching { StrictMode.setVmPolicy(policy) }
            .onSuccess { AppLogger.i(TAG) { "StrictMode probes installed" } }
            .onFailure { error -> AppLogger.w(TAG, "StrictMode probes not installed", error) }
    }

    /**
     * Adds `detectImplicitUriPermissionGrant()` where it exists.
     *
     * Called reflectively on purpose. The method is new in Android 17 and this module compiles against
     * `compileSdk 37`, so a direct call would compile — but it is also `@FlaggedApi`-adjacent and absent from
     * earlier platforms, and a `NoSuchMethodError` on an Android 16 device would take the whole policy down
     * with it. Reflection keeps the failure to "this one detector is missing".
     */
    private fun addImplicitUriGrantDetection(builder: StrictMode.VmPolicy.Builder) {
        if (Build.VERSION.SDK_INT < ANDROID_17_SDK) return
        runCatching {
            StrictMode.VmPolicy.Builder::class.java
                .getMethod("detectImplicitUriPermissionGrant")
                .invoke(builder)
        }.onFailure {
            AppLogger.i(TAG) { "detectImplicitUriPermissionGrant unavailable on this build" }
        }
    }

    /** `Build.VERSION_CODES.CINNAMON_BUN`, spelled numerically so this compiles on any SDK. */
    private const val ANDROID_17_SDK = 37
}
