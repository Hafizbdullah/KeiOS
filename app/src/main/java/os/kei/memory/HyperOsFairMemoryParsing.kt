package os.kei.memory

import android.os.Bundle

/**
 * Reads a fair-memory notification out of the two nested bundles, or returns `null` if it is not one.
 *
 * Split from the receiver so it can be tested without a device: every value the app then acts on comes from
 * here, and the alternative is a broadcast that only a HyperOS build can deliver.
 *
 * Tolerant on purpose. The measurements are logging and policy inputs, not preconditions — a notification
 * that arrives with a missing `pssLimit` still means "release memory now", and refusing to handle it would
 * trade a working release for a pointless kill.
 */
internal fun parseHyperOsFairMemoryNotification(
    action: String?,
    extras: Bundle?,
): HyperOsFairMemoryNotification? {
    val kill =
        when (action) {
            HyperOsFairMemory.ACTION_KILL -> true
            HyperOsFairMemory.ACTION_TRIM -> false
            else -> return null
        }
    val common = extras?.let { bundle -> bundleOrNull(bundle, HyperOsFairMemory.KEY_COMMON) } ?: return null
    val extra = extras.let { bundle -> bundleOrNull(bundle, HyperOsFairMemory.KEY_EXTRA) }

    return HyperOsFairMemoryNotification(
        kill = kill,
        notifyType = common.getInt(HyperOsFairMemory.KEY_NOTIFY_TYPE),
        notifyId = common.getInt(HyperOsFairMemory.KEY_NOTIFY_ID),
        reason = common.getString(HyperOsFairMemory.KEY_REASON).orEmpty(),
        heapUsedKb = extra?.let(::readHeapUsedKb),
        heapCapacityKb = extra?.let { bundle -> intOrNull(bundle, HyperOsFairMemory.KEY_HEAP_CAPACITY) },
        pssKb = extra?.let { bundle -> intOrNull(bundle, HyperOsFairMemory.KEY_PSS) },
        pssLimitKb = extra?.let { bundle -> intOrNull(bundle, HyperOsFairMemory.KEY_PSS_LIMIT) },
    )
}

/**
 * The Java heap in use, under either spelling the documentation uses.
 *
 * `heapSize` is what the field table says; `heapAlloc` is what the same document's example code reads. Trying
 * the table's spelling first and the code's second costs one absent-key lookup and removes a guess about
 * which one the shipped system actually puts in the bundle.
 */
internal fun readHeapUsedKb(extra: Bundle): Int? =
    intOrNull(extra, HyperOsFairMemory.KEY_HEAP_SIZE)
        ?: intOrNull(extra, HyperOsFairMemory.KEY_HEAP_ALLOC)

/** Distinguishes "absent" from "present and zero", which `getInt` cannot. */
private fun intOrNull(
    bundle: Bundle,
    key: String,
): Int? = if (bundle.containsKey(key)) bundle.getInt(key) else null

private fun bundleOrNull(
    bundle: Bundle,
    key: String,
): Bundle? = runCatching { bundle.getBundle(key) }.getOrNull()

/**
 * What the app should do about [notification], as the two-level release policy.
 *
 * A KILL is always [AppMemoryReleaseLevel.Critical]: the process is going away, so nothing in a cache is
 * worth keeping and the only question is whether state got saved first.
 *
 * A TRIM is [AppMemoryReleaseLevel.Critical] too when the *physical* memory of the app is what tripped,
 * because the doc is explicit that the system kills first and notifies afterwards on that path — there is no
 * second warning to save the gentler response for. A Java-heap TRIM does get a second warning (the system
 * asks the user rather than killing), so that one takes the gentler level and keeps the on-screen images.
 */
internal fun releaseLevelFor(notification: HyperOsFairMemoryNotification): AppMemoryReleaseLevel =
    when {
        notification.kill -> AppMemoryReleaseLevel.Critical
        notification.physicalMemoryException -> AppMemoryReleaseLevel.Critical
        else -> AppMemoryReleaseLevel.Moderate
    }
