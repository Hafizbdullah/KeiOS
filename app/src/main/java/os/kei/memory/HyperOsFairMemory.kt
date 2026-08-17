package os.kei.memory

/**
 * The wire contract of HyperOS's *公平运行内存* (fair running memory) mechanism.
 *
 * Transcribed from the HyperOS developer documentation, *公平运行内存适配：开发者文档*
 * (`dev.mi.com/xiaomihyperos/documentation/detail?pId=2304`, updated 2026-04-28). Kept in one file with the
 * doc's own field names so a future reader can diff it against the page rather than infer it from usage.
 *
 * ## What the mechanism does
 *
 * The system watches two numbers per app: the summed **PSS of its high-priority processes**, and each
 * process's **Java heap**. As either approaches its limit the app gets a TRIM broadcast and is expected to
 * free memory; if it keeps growing it gets a KILL broadcast and is expected to save enough state to resume,
 * because the process is going to be killed either way. Both broadcasts carry a callback `IBinder`, and the
 * app has **3 seconds** to report back through it before the system stops waiting.
 *
 * ## The one discrepancy in the source
 *
 * The doc's field table names the Java-heap size key **`heapSize`**; the doc's own example code reads
 * **`heapAlloc`**. They cannot both be right and there is no way to tell which ships, so
 * [readHeapUsedKb] tries both. See [KEY_HEAP_SIZE] and [KEY_HEAP_ALLOC].
 */
object HyperOsFairMemory {
    /** Warning: free memory now. */
    const val ACTION_TRIM = "itgsa.intent.action.TRIM"

    /** Kill imminent: save state for continuation. */
    const val ACTION_KILL = "itgsa.intent.action.KILL"

    /** `BUNDLE_KEY_COMMON` — the bundle holding the notification's identity and the callback. */
    const val KEY_COMMON = "common"

    /** `BUNDLE_KEY_EXTRA` — the bundle holding the measurements. */
    const val KEY_EXTRA = "extra"

    const val KEY_NOTIFY_TYPE = "notifyType"
    const val KEY_NOTIFY_ID = "notifyId"

    /** `"Excessive PSS Usage"` or `"Excessive Java Heap Usage"`. */
    const val KEY_REASON = "reason"

    /** `"trim"` or `"kill"` — the same information as the intent action, restated in the bundle. */
    const val KEY_ACTION = "action"

    const val KEY_CALLBACK = "callback"

    /** Java heap in use, KB. The field table's spelling. */
    const val KEY_HEAP_SIZE = "heapSize"

    /** Java heap in use, KB. The example code's spelling for the same value. */
    const val KEY_HEAP_ALLOC = "heapAlloc"

    const val KEY_HEAP_CAPACITY = "heapCapacity"
    const val KEY_PSS = "pss"
    const val KEY_PSS_LIMIT = "pssLimit"

    /** `notifyType` for a physical-memory (PSS) exception. */
    const val NOTIFY_TYPE_PHYSICAL = 1000

    /** `notifyType` for a Java-heap exception. */
    const val NOTIFY_TYPE_JAVA_HEAP = 2000

    /** `result` — handled: memory released, or state saved. */
    const val RESULT_HANDLED = 0

    /** `result` — not handled. */
    const val RESULT_NOT_HANDLED = 1

    /** The `extra` bundle sent back with the reply carries one string under this key. */
    const val REPLY_KEY_MESSAGE = "reply"

    /**
     * The system stops waiting after this long.
     *
     * The app must have replied by then; on a physical-memory exception the system kills the process first
     * and notifies the user afterwards, so a late reply means the save never happened.
     */
    const val REPLY_TIMEOUT_MS = 3_000L
}

/**
 * One parsed TRIM or KILL notification.
 *
 * [heapUsedKb] resolves the `heapSize`/`heapAlloc` disagreement described on [HyperOsFairMemory]. All sizes
 * are kilobytes, and `null` means the field was absent rather than zero — worth distinguishing, because a
 * PSS notification carries no heap numbers and vice versa.
 */
data class HyperOsFairMemoryNotification(
    val kill: Boolean,
    val notifyType: Int,
    val notifyId: Int,
    val reason: String,
    val heapUsedKb: Int?,
    val heapCapacityKb: Int?,
    val pssKb: Int?,
    val pssLimitKb: Int?,
) {
    val physicalMemoryException: Boolean
        get() = notifyType == HyperOsFairMemory.NOTIFY_TYPE_PHYSICAL

    val javaHeapException: Boolean
        get() = notifyType == HyperOsFairMemory.NOTIFY_TYPE_JAVA_HEAP

    /**
     * How full the offending pool is, 0..1, or `null` when the notification carried no usable pair.
     *
     * Only used for the log line. It is the one number that makes a report actionable after the fact — "we
     * were at 0.94 of the PSS limit" says something a raw kilobyte count does not.
     */
    val usageFraction: Float?
        get() {
            val (used, limit) =
                if (javaHeapException) heapUsedKb to heapCapacityKb else pssKb to pssLimitKb
            if (used == null || limit == null || limit <= 0) return null
            return (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }
}
