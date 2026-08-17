package os.kei.memory

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Debug
import coil3.SingletonImageLoader
import os.kei.ui.page.main.student.BaGuideImageCache
import os.kei.ui.page.main.student.catalog.BaGuideCatalogIconCache
import os.kei.core.log.AppLogger

/**
 * How hard to release, in the two steps this app actually distinguishes.
 *
 * Deliberately two levels rather than a mirror of Android's five `TRIM_MEMORY_*` constants. Those
 * describe the *process's* standing (visible, background, one-from-the-end of the LRU list), not how much
 * to drop, and mapping five inputs onto two behaviours in one place is clearer than five call sites each
 * deciding again.
 */
enum class AppMemoryReleaseLevel {
    /**
     * Something is pressing. Drop what can be rebuilt cheaply — decoded bitmaps above all, since a
     * bitmap cache is the largest thing this app holds and its disk copy survives.
     */
    Moderate,

    /** Nothing on screen needs its caches, or a kill is imminent. Drop every in-memory cache. */
    Critical,
}

/**
 * One in-memory cache the app can be asked to drop.
 *
 * [release] must be **memory-only and idempotent**, and it runs off the main thread. Anything reachable
 * again from disk or the network belongs here; anything that would have to be re-downloaded to be restored
 * does not.
 */
fun interface AppReleasableCache {
    fun release(
        context: Context,
        level: AppMemoryReleaseLevel,
    )
}

/**
 * The app's single answer to memory pressure, whatever asked.
 *
 * There was no answer before this: nothing in the tree overrode `onTrimMemory`, implemented
 * [ComponentCallbacks2], or handled `onLowMemory`, so every cache the app held survived until the process
 * died. Both callers now come through here — Android's own `onTrimMemory` and the ITGSA alliance's
 * fair-memory TRIM broadcast — so the release behaviour is defined once and the OEM path adds a trigger rather
 * than a second policy. That split is what makes the OEM work worth doing: the alliance mechanism gives an
 * *earlier and better-informed* trigger (it arrives with the app's PSS and its limit) for a response the app
 * needed on every device anyway.
 *
 * ## Never disk
 *
 * The two bitmap caches expose `clearAll(context)` / `clear(context)` which evict memory **and delete the
 * disk cache**. Calling those here would be wrong twice over: disk is not the resource under pressure, and
 * throwing it away converts a memory problem into a network one the moment the user scrolls back. Every
 * registration below evicts memory and leaves the files alone, and `release never touches disk caches` in
 * `ItgsaFairMemoryTest` pins that.
 */
object AppMemoryRelease {
    private const val TAG = "AppMemoryRelease"

    private val caches = mutableListOf<Pair<String, AppReleasableCache>>()

    /** [name] appears in the log line, so a release that frees nothing can be attributed. */
    @Synchronized
    fun register(
        name: String,
        cache: AppReleasableCache,
    ) {
        if (caches.any { (registered, _) -> registered == name }) return
        caches += name to cache
    }

    @Synchronized
    private fun snapshot(): List<Pair<String, AppReleasableCache>> = caches.toList()

    /**
     * Runs every registered cache's release, then reports how much came back.
     *
     * Never throws: a cache that fails is logged and the rest still run, because this is called from a
     * broadcast with a 3-second reply deadline and from `onTrimMemory`, and neither caller can do anything
     * useful with an exception.
     *
     * @return kilobytes of PSS recovered, or `null` if it could not be measured. Measured with
     *   [Debug.getPss] rather than `getMemoryInfo`, per the HyperOS guidance that the latter costs tens of
     *   milliseconds; even so this only runs on an actual pressure event, never per frame.
     */
    fun release(
        context: Context,
        level: AppMemoryReleaseLevel,
    ): Long? {
        val before = runCatching { Debug.getPss() }.getOrNull()
        snapshot().forEach { (name, cache) ->
            runCatching { cache.release(context, level) }
                .onFailure { error -> AppLogger.w(TAG, "release failed for $name", error) }
        }
        // Only at Critical: `System.gc()` is a hint, it costs a pause, and at Moderate the point is to make
        // the objects collectable rather than to force the collection. At Critical the caller is about to be
        // judged on its PSS, so asking is worth the pause.
        if (level == AppMemoryReleaseLevel.Critical) {
            runCatching { System.gc() }
        }
        val after = runCatching { Debug.getPss() }.getOrNull()
        val freed = if (before != null && after != null) (before - after).coerceAtLeast(0L) else null
        AppLogger.i(
            TAG,
            "released level=$level caches=${caches.size} pssBeforeKb=$before pssAfterKb=$after freedKb=$freed",
        )
        return freed
    }

    /**
     * Translates one of Android's `TRIM_MEMORY_*` constants, or returns `null` for the ones that must not
     * release anything.
     *
     * `RUNNING_MODERATE` and `RUNNING_LOW` arrive while the app is **foreground and visible**. Dropping the
     * decoded bitmaps of the list the user is looking at would cause a visible re-decode, so those two are
     * deliberately ignored — the app answers them by having its caches bounded, not by emptying them under
     * the user's eyes. `RUNNING_CRITICAL` is different: the alternative to releasing there is being killed.
     */
    fun levelForTrimMemory(trimLevel: Int): AppMemoryReleaseLevel? =
        when (trimLevel) {
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            -> AppMemoryReleaseLevel.Critical

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            -> AppMemoryReleaseLevel.Moderate

            // UI_HIDDEN says the UI went away, not that memory is short; the process is still whole and the
            // user is one tap from coming back to the same list.
            else -> null
        }

    /**
     * Registers the caches this app actually holds. Called once from the Application.
     *
     * Coil is first on purpose: it is configured with a memory cache of 25% of the heap, which makes it the
     * largest single reclaimable allocation in the process by a wide margin.
     */
    fun registerAppCaches() {
        register("ba-guide-image") { _, _ ->
            // Memory only. `clearAll(context)` would also `deleteRecursively()` the disk cache, which is the
            // exact mistake this object exists to avoid.
            BaGuideImageCache.evictMemory()
        }
        register("ba-catalog-icon") { _, _ ->
            // A null context is this cache's own memory-only path; passing one deletes the files.
            BaGuideCatalogIconCache.clear(context = null)
        }
        register("coil") { _, level ->
            val loader = SingletonImageLoader.get(os.kei.KeiOSApp.appContext)
            loader.memoryCache?.let { cache ->
                if (level == AppMemoryReleaseLevel.Critical) {
                    cache.clear()
                } else {
                    // Halve rather than empty: the images on screen are the most recently used, so trimming
                    // to half keeps them and drops the scrollback.
                    cache.trimToSize(cache.maxSize / 2)
                }
            }
        }
    }
}
