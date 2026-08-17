package os.kei.memory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import os.kei.core.log.AppLogger

/**
 * Answers HyperOS's fair-memory TRIM and KILL broadcasts.
 *
 * See [HyperOsFairMemory] for the transcribed contract and the source it came from. This class is the part
 * that touches the framework; the decisions live in `HyperOsFairMemoryParsing.kt` where they can be tested.
 *
 * ## Shape of the response
 *
 * TRIM releases caches through [AppMemoryRelease], the same path Android's own `onTrimMemory` uses, so the
 * OEM mechanism contributes a *trigger* rather than a second policy. KILL runs [onSaveState] first — the
 * process is going away regardless, and the only thing still in the app's control is whether reopening it
 * resumes — and then releases as well, because a KILL for a Java-heap exception only notifies the user and a
 * freed heap may still avoid the kill.
 *
 * ## Why a background thread, and why the reply is not deferred
 *
 * The receiver is registered with its own [HandlerThread], as the documentation's own sample does, because
 * `Debug.getPss()` alone can cost tens of milliseconds and the release runs several cache evictions. The
 * reply is sent from that same callback once the work returns rather than posted for later: the budget is
 * **3 seconds** ([HyperOsFairMemory.REPLY_TIMEOUT_MS]) and on the physical-memory path the system kills the
 * process before it notifies the user, so a reply that misses the window is a reply that never happened.
 *
 * ## On other devices
 *
 * Nothing broadcasts these actions off HyperOS, so [register] is inert there — but it still registers an
 * **exported** receiver, which any app on the device can then trigger. The worst that buys an attacker is
 * making this app drop caches it can rebuild, and [register] is gated on the device actually being HyperOS so
 * the surface does not exist elsewhere at all.
 */
object HyperOsFairMemoryReceiver : IBinder.DeathRecipient {
    private const val TAG = "HyperOsFairMemory"

    /** `IBinder.FIRST_CALL_TRANSACTION`, as the documented reply transaction code. */
    private const val TRANSACTION_EXCEPTION_REPLY = IBinder.FIRST_CALL_TRANSACTION

    private val lock = Any()
    private var handlerThread: HandlerThread? = null
    private var registered = false
    private var remote: IBinder? = null

    /**
     * Called on the receiver's background thread when a KILL arrives, before the caches are dropped.
     *
     * Must be fast and must not touch the UI. Anything held only in memory that the user would notice losing
     * belongs here; anything already written through to a store does not, which is most of this app.
     */
    private var onSaveState: (() -> Unit)? = null

    fun register(
        context: Context,
        onSaveState: () -> Unit,
    ) {
        synchronized(lock) {
            if (registered) return
            if (!isHyperOs()) {
                AppLogger.i(TAG) { "not HyperOS, fair-memory receiver not registered" }
                return
            }
            this.onSaveState = onSaveState
            val thread = HandlerThread(TAG).also(HandlerThread::start)
            handlerThread = thread
            val handler = Handler(thread.looper)
            val filter =
                IntentFilter().apply {
                    addAction(HyperOsFairMemory.ACTION_TRIM)
                    addAction(HyperOsFairMemory.ACTION_KILL)
                }
            val result =
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, null, handler, Context.RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(receiver, filter, null, handler)
                    }
                }
            result
                .onSuccess {
                    registered = true
                    AppLogger.i(TAG) { "fair-memory receiver registered" }
                }.onFailure { error ->
                    thread.quitSafely()
                    handlerThread = null
                    AppLogger.w(TAG, "fair-memory receiver registration failed", error)
                }
        }
    }

    override fun binderDied() {
        synchronized(lock) {
            runCatching { remote?.unlinkToDeath(this, 0) }
            remote = null
        }
    }

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                val notification =
                    parseHyperOsFairMemoryNotification(intent.action, intent.extras) ?: return
                val callback =
                    intent.extras
                        ?.getBundle(HyperOsFairMemory.KEY_COMMON)
                        ?.getBinder(HyperOsFairMemory.KEY_CALLBACK)
                handle(context, notification, callback)
            }
        }

    private fun handle(
        context: Context,
        notification: HyperOsFairMemoryNotification,
        callback: IBinder?,
    ) {
        val startedAtMs = System.currentTimeMillis()
        AppLogger.i(TAG) {
            "received ${if (notification.kill) "KILL" else "TRIM"} type=${notification.notifyType} " +
                "reason='${notification.reason}' pss=${notification.pssKb}/${notification.pssLimitKb} " +
                "heap=${notification.heapUsedKb}/${notification.heapCapacityKb} " +
                "usage=${notification.usageFraction}"
        }

        // Saving comes first on a KILL: the release is worth doing but the save is the part that cannot be
        // redone once the process is gone.
        val saved =
            if (notification.kill) {
                runCatching { onSaveState?.invoke() }
                    .onFailure { error -> AppLogger.w(TAG, "state save failed", error) }
                    .isSuccess
            } else {
                true
            }

        val freedKb =
            runCatching { AppMemoryRelease.release(context, releaseLevelFor(notification)) }
                .onFailure { error -> AppLogger.w(TAG, "release failed", error) }
                .getOrNull()

        val elapsedMs = System.currentTimeMillis() - startedAtMs
        // Reported even when inside the budget, because "we replied at 2.9s" is the warning that the next
        // cache added here will push it over.
        if (elapsedMs >= HyperOsFairMemory.REPLY_TIMEOUT_MS) {
            AppLogger.w(TAG, "handled in ${elapsedMs}ms, past the ${HyperOsFairMemory.REPLY_TIMEOUT_MS}ms budget")
        }

        if (callback == null) {
            AppLogger.w(TAG, "no callback binder in the notification; nothing to reply to")
            return
        }
        reply(
            callback = callback,
            notifyType = notification.notifyType,
            notifyId = notification.notifyId,
            result = if (saved) HyperOsFairMemory.RESULT_HANDLED else HyperOsFairMemory.RESULT_NOT_HANDLED,
            message = "freedKb=$freedKb elapsedMs=$elapsedMs",
        )
    }

    /**
     * Writes the documented reply parcel: `notifyType`, `notifyId`, `result`, `extra`, in that order.
     *
     * A raw `transact` rather than a generated stub because the system side exposes no AIDL to compile
     * against — so the *order* here is the contract, and `HyperOsFairMemoryReplyTest` pins it by reading the
     * parcel back.
     */
    private fun reply(
        callback: IBinder,
        notifyType: Int,
        notifyId: Int,
        result: Int,
        message: String,
    ) {
        synchronized(lock) {
            if (remote !== callback) {
                runCatching { remote?.unlinkToDeath(this, 0) }
                remote = callback
                runCatching { callback.linkToDeath(this, 0) }
                    .onFailure { error ->
                        remote = null
                        AppLogger.w(TAG, "callback binder already dead", error)
                        return
                    }
            }
        }
        val data = Parcel.obtain()
        val replyParcel = Parcel.obtain()
        try {
            writeHyperOsFairMemoryReply(
                data = data,
                notifyType = notifyType,
                notifyId = notifyId,
                result = result,
                extra = Bundle().apply { putString(HyperOsFairMemory.REPLY_KEY_MESSAGE, message) },
            )
            callback.transact(TRANSACTION_EXCEPTION_REPLY, data, replyParcel, IBinder.FLAG_ONEWAY)
            AppLogger.i(TAG) { "replied type=$notifyType id=$notifyId result=$result" }
        } catch (error: Exception) {
            AppLogger.w(TAG, "reply failed", error)
        } finally {
            replyParcel.recycle()
            data.recycle()
        }
    }

    private fun isHyperOs(): Boolean =
        HYPER_OS_PROPERTIES.any { key ->
            !readSystemProperty(key).isNullOrBlank()
        }

    private fun readSystemProperty(key: String): String? =
        runCatching {
            @Suppress("PrivateApi")
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            (get.invoke(null, key) as? String)?.trim()
        }.getOrNull()
}

/**
 * The reply parcel's field order, extracted so a test can write it and read it back.
 *
 * There is no AIDL to compile against, so nothing but this ordering makes the reply parse on the system side.
 */
internal fun writeHyperOsFairMemoryReply(
    data: Parcel,
    notifyType: Int,
    notifyId: Int,
    result: Int,
    extra: Bundle,
) {
    data.writeInt(notifyType)
    data.writeInt(notifyId)
    data.writeInt(result)
    data.writeBundle(extra)
}

/**
 * Properties that mean "this is HyperOS or MIUI".
 *
 * Same set the app already uses in `BaGuideBgmMediaOemCompat`, deliberately: two different answers to "is
 * this a Xiaomi build" in one app would be a bug waiting to happen.
 */
private val HYPER_OS_PROPERTIES =
    listOf(
        "ro.mi.os.version.name",
        "ro.mi.os.version.incremental",
        "ro.miui.ui.version.name",
        "ro.miui.ui.version.code",
    )
