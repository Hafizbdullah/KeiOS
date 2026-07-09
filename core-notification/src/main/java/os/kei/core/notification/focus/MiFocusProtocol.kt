package os.kei.core.notification.focus

import android.os.Bundle
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONObject

internal const val MI_FOCUS_PARAM_KEY = "miui.focus.param"
internal const val MI_FOCUS_PARAM_V3_KEY = "param_v2"
internal const val MI_FOCUS_ISLAND_KEY = "param_island"
internal const val MI_FOCUS_PICTURES_KEY = "miui.focus.pics"
internal const val MI_FOCUS_ACTIONS_KEY = "miui.focus.actions"

/**
 * Local encoder for the public HyperOS Focus Notification V3 Bundle contract.
 *
 * The payload remains a regular notification extra. Xiaomi Magic only decides the dispatch path
 * after this encoder has produced the notification, so its lifecycle stays outside this class.
 */
internal object MiFocusProtocolNotification {
    fun buildV3(block: MiFocusProtocolTemplateV3.() -> Unit): Bundle =
        MiFocusProtocolTemplateV3().apply(block).toBundle()
}

internal class MiFocusProtocolTemplateV3 {
    private val pictures = linkedMapOf<String, Parcelable>()
    private val actions = linkedMapOf<String, Parcelable>()

    var cancel: Boolean? = null
    var enableFloat: Boolean? = null
    var updatable: Boolean? = null
    var showSmallIcon: Boolean? = null
    var timeout: Int? = null
    var aodTitle: String? = null
    var aodPic: String? = null
    var reopen: String? = null
    var filterWhenNoPermission: Boolean? = null
    var ticker: String? = null
    var tickerPic: String? = null
    var tickerPicDark: String? = null
    var isShowNotification: Boolean? = null
    var islandFirstFloat: Boolean? = null
    var hideDeco: Boolean? = null
    var outEffectColor: String? = null
    var outEffectSrc: String? = null
    var sequence: Long? = null
    var business: String? = null
    var notifyId: String? = null
    var orderId: String? = null

    var island: MiFocusProtocolIslandTemplate? = null
    var baseInfo: MiFocusProtocolBaseInfo? = null
    var chatInfo: MiFocusProtocolChatInfo? = null
    var highlightInfo: MiFocusProtocolHighlightInfo? = null
    var hintInfo: MiFocusProtocolHintInfo? = null
    var progressInfo: MiFocusProtocolExpandedProgressInfo? = null
    var picInfo: MiFocusProtocolExpandedPicInfo? = null
    var bgInfo: MiFocusProtocolBgInfo? = null
    var actionsInfo: MutableList<MiFocusProtocolActionInfo>? = null
    var coverInfo: MiFocusProtocolCoverInfo? = null
    var highlightInfoV3: MiFocusProtocolHighlightInfoV3? = null
    var iconTextInfo: MiFocusProtocolIconTextInfo? = null
    var multiProgressInfo: MiFocusProtocolMultiProgressInfo? = null
    var animTextInfo: MiFocusProtocolAnimTextInfo? = null
    var textButton: MutableList<MiFocusProtocolActionInfo>? = null

    fun createPicture(key: String, value: Parcelable): String = key.also { pictures[it] = value }

    fun createAction(key: String, value: Parcelable): String = key.also { actions[it] = value }

    fun island(block: MiFocusProtocolIslandTemplate.() -> Unit) {
        (island ?: MiFocusProtocolIslandTemplate().also { island = it }).apply(block)
    }

    fun baseInfo(block: MiFocusProtocolBaseInfo.() -> Unit) {
        (baseInfo ?: MiFocusProtocolBaseInfo().also { baseInfo = it }).apply(block)
    }

    fun chatInfo(block: MiFocusProtocolChatInfo.() -> Unit) {
        (chatInfo ?: MiFocusProtocolChatInfo().also { chatInfo = it }).apply(block)
    }

    fun highlightInfo(block: MiFocusProtocolHighlightInfo.() -> Unit) {
        (highlightInfo ?: MiFocusProtocolHighlightInfo().also { highlightInfo = it }).apply(block)
    }

    fun hintInfo(block: MiFocusProtocolHintInfo.() -> Unit) {
        (hintInfo ?: MiFocusProtocolHintInfo().also { hintInfo = it }).apply(block)
    }

    fun progressInfo(block: MiFocusProtocolExpandedProgressInfo.() -> Unit) {
        (progressInfo ?: MiFocusProtocolExpandedProgressInfo().also { progressInfo = it }).apply(block)
    }

    fun picInfo(block: MiFocusProtocolExpandedPicInfo.() -> Unit) {
        (picInfo ?: MiFocusProtocolExpandedPicInfo().also { picInfo = it }).apply(block)
    }

    fun bgInfo(block: MiFocusProtocolBgInfo.() -> Unit) {
        (bgInfo ?: MiFocusProtocolBgInfo().also { bgInfo = it }).apply(block)
    }

    fun actions(block: MutableList<MiFocusProtocolActionInfo>.() -> Unit) {
        (actionsInfo ?: mutableListOf<MiFocusProtocolActionInfo>().also { actionsInfo = it }).apply(block)
    }

    fun highlightInfoV3(block: MiFocusProtocolHighlightInfoV3.() -> Unit) {
        (highlightInfoV3 ?: MiFocusProtocolHighlightInfoV3().also { highlightInfoV3 = it }).apply(block)
    }

    fun iconTextInfo(block: MiFocusProtocolIconTextInfo.() -> Unit) {
        (iconTextInfo ?: MiFocusProtocolIconTextInfo().also { iconTextInfo = it }).apply(block)
    }

    fun multiProgressInfo(block: MiFocusProtocolMultiProgressInfo.() -> Unit) {
        (multiProgressInfo ?: MiFocusProtocolMultiProgressInfo().also { multiProgressInfo = it }).apply(block)
    }

    fun animTextInfo(block: MiFocusProtocolAnimTextInfo.() -> Unit) {
        (animTextInfo ?: MiFocusProtocolAnimTextInfo().also { animTextInfo = it }).apply(block)
    }

    fun textButton(block: MutableList<MiFocusProtocolActionInfo>.() -> Unit) {
        (textButton ?: mutableListOf<MiFocusProtocolActionInfo>().also { textButton = it }).apply(block)
    }

    fun MutableList<MiFocusProtocolActionInfo>.addActionInfo(block: MiFocusProtocolActionInfo.() -> Unit) {
        add(MiFocusProtocolActionInfo().apply(block))
    }

    fun toBundle(): Bundle = Bundle().apply {
        putString(MI_FOCUS_PARAM_KEY, JSONObject().apply {
            put(MI_FOCUS_PARAM_V3_KEY, toJson())
        }.toString())
        if (pictures.isNotEmpty()) {
            putBundle(MI_FOCUS_PICTURES_KEY, Bundle().apply {
                pictures.forEach { (key, value) -> putParcelable(key, value) }
            })
        }
        if (actions.isNotEmpty()) {
            putBundle(MI_FOCUS_ACTIONS_KEY, Bundle().apply {
                actions.forEach { (key, value) -> putParcelable(key, value) }
            })
        }
    }

    private fun toJson(): JSONObject = JSONObject().apply {
        putNullable("cancel", cancel)
        putNullable("enableFloat", enableFloat)
        putNullable("updatable", updatable)
        putNullable("showSmallIcon", showSmallIcon)
        putNullable("timeout", timeout)
        putNullable("aodTitle", aodTitle)
        putNullable("aodPic", aodPic)
        putNullable("reopen", reopen)
        putNullable("filterWhenNoPermission", filterWhenNoPermission)
        putNullable("ticker", ticker)
        putNullable("tickerPic", tickerPic)
        putNullable("tickerPicDark", tickerPicDark)
        putNullable("isShowNotification", isShowNotification)
        putNullable("islandFirstFloat", islandFirstFloat)
        putNullable("hideDeco", hideDeco)
        putNullable("outEffectColor", outEffectColor)
        putNullable("outEffectSrc", outEffectSrc)
        putNullable("sequence", sequence)
        putNullable("business", business)
        putNullable("notifyId", notifyId)
        putNullable("orderId", orderId)
        island?.let { put(MI_FOCUS_ISLAND_KEY, it.toJson()) }
        baseInfo?.let { put("baseInfo", it.toJson()) }
        chatInfo?.let { put("chatInfo", it.toJson()) }
        highlightInfo?.let { put("highlightInfo", it.toJson()) }
        hintInfo?.let { put("hintInfo", it.toJson()) }
        progressInfo?.let { put("progressInfo", it.toJson()) }
        picInfo?.let { put("picInfo", it.toJson()) }
        bgInfo?.let { put("bgInfo", it.toJson()) }
        actionsInfo?.takeIf { it.isNotEmpty() }?.let { items ->
            put("actions", JSONArray().apply { items.forEach { put(it.toJson()) } })
        }
        coverInfo?.let { put("coverInfo", it.toJson()) }
        highlightInfoV3?.let { put("highlightInfoV3", it.toJson()) }
        iconTextInfo?.let { put("iconTextInfo", it.toJson()) }
        multiProgressInfo?.let { put("multiProgressInfo", it.toJson()) }
        animTextInfo?.let { put("animTextInfo", it.toJson()) }
        textButton?.takeIf { it.isNotEmpty() }?.let { items ->
            put("textButton", JSONArray().apply { items.forEach { put(it.toJson()) } })
        }
    }
}

internal fun JSONObject.putNullable(key: String, value: Any?) {
    if (value != null) put(key, value)
}
