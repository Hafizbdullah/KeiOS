package os.kei.core.notification.focus

import org.json.JSONObject

internal class MiFocusProtocolIslandTemplate {
    var islandPriority: Int? = null
    var islandProperty: Int? = null
    var expandedTime: Int? = null
    var islandTimeout: Int? = null
    var dismissIsland: Boolean? = null
    var islandOrder: Boolean? = null
    var maxSize: Boolean? = null
    var needCloseAnimation: Boolean? = null
    var business: String? = null
    var highlightColor: String? = null
    var smallIslandArea: MiFocusProtocolSmallIslandArea? = null
    var bigIslandArea: MiFocusProtocolBigIslandArea? = null
    var shareData: MiFocusProtocolIslandShareData? = null

    fun smallIslandArea(block: MiFocusProtocolSmallIslandArea.() -> Unit) {
        (smallIslandArea ?: MiFocusProtocolSmallIslandArea().also { smallIslandArea = it }).apply(block)
    }

    fun bigIslandArea(block: MiFocusProtocolBigIslandArea.() -> Unit) {
        (bigIslandArea ?: MiFocusProtocolBigIslandArea().also { bigIslandArea = it }).apply(block)
    }

    fun shareData(block: MiFocusProtocolIslandShareData.() -> Unit) {
        (shareData ?: MiFocusProtocolIslandShareData().also { shareData = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("islandPriority", islandPriority)
        putNullable("islandProperty", islandProperty)
        putNullable("expandedTime", expandedTime)
        putNullable("islandTimeout", islandTimeout)
        putNullable("dismissIsland", dismissIsland)
        putNullable("islandOrder", islandOrder)
        putNullable("maxSize", maxSize)
        putNullable("needCloseAnimation", needCloseAnimation)
        putNullable("business", business)
        putNullable("highlightColor", highlightColor)
        smallIslandArea?.let { put("smallIslandArea", it.toJson()) }
        bigIslandArea?.let { put("bigIslandArea", it.toJson()) }
        shareData?.let { put("shareData", it.toJson()) }
    }
}

internal class MiFocusProtocolBigIslandArea {
    var textInfo: MiFocusProtocolIslandTextInfo? = null
    var picInfo: MiFocusProtocolIslandPicInfo? = null
    var imageTextInfoLeft: MiFocusProtocolIslandImageTextInfo? = null
    var imageTextInfoRight: MiFocusProtocolIslandImageTextInfo? = null
    var progressTextInfo: MiFocusProtocolIslandProgressTextInfo? = null
    var fixedWidthDigitInfo: MiFocusProtocolFixedWidthDigitInfo? = null
    var sameWidthDigitInfo: MiFocusProtocolSameWidthDigitInfo? = null

    fun picInfo(block: MiFocusProtocolIslandPicInfo.() -> Unit) {
        (picInfo ?: MiFocusProtocolIslandPicInfo().also { picInfo = it }).apply(block)
    }

    fun imageTextInfoLeft(block: MiFocusProtocolIslandImageTextInfo.() -> Unit) {
        (imageTextInfoLeft ?: MiFocusProtocolIslandImageTextInfo().also { imageTextInfoLeft = it }).apply(block)
    }

    fun imageTextInfoRight(block: MiFocusProtocolIslandImageTextInfo.() -> Unit) {
        (imageTextInfoRight ?: MiFocusProtocolIslandImageTextInfo().also { imageTextInfoRight = it }).apply(block)
    }

    fun progressTextInfo(block: MiFocusProtocolIslandProgressTextInfo.() -> Unit) {
        (progressTextInfo ?: MiFocusProtocolIslandProgressTextInfo().also { progressTextInfo = it }).apply(block)
    }

    fun fixedWidthDigitInfo(block: MiFocusProtocolFixedWidthDigitInfo.() -> Unit) {
        (fixedWidthDigitInfo ?: MiFocusProtocolFixedWidthDigitInfo().also { fixedWidthDigitInfo = it }).apply(block)
    }

    fun sameWidthDigitInfo(block: MiFocusProtocolSameWidthDigitInfo.() -> Unit) {
        (sameWidthDigitInfo ?: MiFocusProtocolSameWidthDigitInfo().also { sameWidthDigitInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        textInfo?.let { put("textInfo", it.toJson()) }
        picInfo?.let { put("picInfo", it.toJson()) }
        imageTextInfoLeft?.let { put("imageTextInfoLeft", it.toJson()) }
        imageTextInfoRight?.let { put("imageTextInfoRight", it.toJson()) }
        progressTextInfo?.let { put("progressTextInfo", it.toJson()) }
        fixedWidthDigitInfo?.let { put("fixedWidthDigitInfo", it.toJson()) }
        sameWidthDigitInfo?.let { put("sameWidthDigitInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolSmallIslandArea {
    var picInfo: MiFocusProtocolIslandPicInfo? = null
    var combinePicInfo: MiFocusProtocolCombinePicInfo? = null
    var imageTextInfoRight: MiFocusProtocolIslandImageTextInfo? = null

    fun picInfo(block: MiFocusProtocolIslandPicInfo.() -> Unit) {
        (picInfo ?: MiFocusProtocolIslandPicInfo().also { picInfo = it }).apply(block)
    }

    fun combinePicInfo(block: MiFocusProtocolCombinePicInfo.() -> Unit) {
        (combinePicInfo ?: MiFocusProtocolCombinePicInfo().also { combinePicInfo = it }).apply(block)
    }

    fun imageTextInfoRight(block: MiFocusProtocolIslandImageTextInfo.() -> Unit) {
        (imageTextInfoRight ?: MiFocusProtocolIslandImageTextInfo().also { imageTextInfoRight = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        picInfo?.let { put("picInfo", it.toJson()) }
        combinePicInfo?.let { put("combinePicInfo", it.toJson()) }
        imageTextInfoRight?.let { put("imageTextInfoRight", it.toJson()) }
    }
}

internal class MiFocusProtocolIslandImageTextInfo {
    var type: Int? = null
    var textInfo: MiFocusProtocolIslandTextInfo? = null
    var picInfo: MiFocusProtocolIslandPicInfo? = null
    var progressInfo: MiFocusProtocolIslandProgressInfo? = null

    fun textInfo(block: MiFocusProtocolIslandTextInfo.() -> Unit) {
        (textInfo ?: MiFocusProtocolIslandTextInfo().also { textInfo = it }).apply(block)
    }

    fun picInfo(block: MiFocusProtocolIslandPicInfo.() -> Unit) {
        (picInfo ?: MiFocusProtocolIslandPicInfo().also { picInfo = it }).apply(block)
    }

    fun progressInfo(block: MiFocusProtocolIslandProgressInfo.() -> Unit) {
        (progressInfo ?: MiFocusProtocolIslandProgressInfo().also { progressInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("type", type)
        textInfo?.let { put("textInfo", it.toJson()) }
        picInfo?.let { put("picInfo", it.toJson()) }
        progressInfo?.let { put("progressInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolCombinePicInfo {
    var smallPicInfo: MiFocusProtocolIslandPicInfo? = null
    var picInfo: MiFocusProtocolIslandPicInfo? = null
    var progressInfo: MiFocusProtocolIslandProgressInfo? = null

    fun smallPicInfo(block: MiFocusProtocolIslandPicInfo.() -> Unit) {
        (smallPicInfo ?: MiFocusProtocolIslandPicInfo().also { smallPicInfo = it }).apply(block)
    }

    fun picInfo(block: MiFocusProtocolIslandPicInfo.() -> Unit) {
        (picInfo ?: MiFocusProtocolIslandPicInfo().also { picInfo = it }).apply(block)
    }

    fun progressInfo(block: MiFocusProtocolIslandProgressInfo.() -> Unit) {
        (progressInfo ?: MiFocusProtocolIslandProgressInfo().also { progressInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        smallPicInfo?.let { put("smallPicInfo", it.toJson()) }
        picInfo?.let { put("picInfo", it.toJson()) }
        progressInfo?.let { put("progressInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolIslandTextInfo {
    var title: String? = null
    var frontTitle: String? = null
    var content: String? = null
    var showHighlightColor: Boolean? = null
    var narrowFont: Boolean? = null
    var isTitleDigit: Boolean? = null
    var turnAnim: Boolean? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("title", title)
        putNullable("frontTitle", frontTitle)
        putNullable("content", content)
        putNullable("showHighlightColor", showHighlightColor)
        putNullable("narrowFont", narrowFont)
        putNullable("isTitleDigit", isTitleDigit)
        putNullable("turnAnim", turnAnim)
    }
}

internal class MiFocusProtocolIslandPicInfo {
    var type: Int? = null
    var contentDescription: String? = null
    var number: Int? = null
    var pic: String? = null
    var effectSrc: String? = null
    var effectColor: String? = null
    var autoplay: Boolean? = null
    var loop: Boolean? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("type", type)
        putNullable("contentDescription", contentDescription)
        putNullable("number", number)
        putNullable("pic", pic)
        putNullable("effectSrc", effectSrc)
        putNullable("effectColor", effectColor)
        putNullable("autoplay", autoplay)
        putNullable("loop", loop)
    }
}

internal class MiFocusProtocolIslandProgressInfo {
    var progress: Int? = null
    var isCCW: Boolean? = null
    var colorReach: String? = null
    var colorUnReach: String? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("progress", progress)
        putNullable("isCCW", isCCW)
        putNullable("colorReach", colorReach)
        putNullable("colorUnReach", colorUnReach)
    }
}

internal class MiFocusProtocolIslandProgressTextInfo {
    var textInfo: MiFocusProtocolIslandTextInfo? = null
    var progressInfo: MiFocusProtocolIslandProgressInfo? = null

    fun textInfo(block: MiFocusProtocolIslandTextInfo.() -> Unit) {
        (textInfo ?: MiFocusProtocolIslandTextInfo().also { textInfo = it }).apply(block)
    }

    fun progressInfo(block: MiFocusProtocolIslandProgressInfo.() -> Unit) {
        (progressInfo ?: MiFocusProtocolIslandProgressInfo().also { progressInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        textInfo?.let { put("textInfo", it.toJson()) }
        progressInfo?.let { put("progressInfo", it.toJson()) }
    }
}

internal open class MiFocusProtocolDigitInfo {
    var content: String? = null
    var digit: String? = null
    var showHighlightColor: Boolean? = null
    var turnAnim: Boolean? = null

    fun putFields(json: JSONObject) {
        json.putNullable("content", content)
        json.putNullable("digit", digit)
        json.putNullable("showHighlightColor", showHighlightColor)
        json.putNullable("turnAnim", turnAnim)
    }
}

internal class MiFocusProtocolFixedWidthDigitInfo : MiFocusProtocolDigitInfo() {
    fun toJson(): JSONObject = JSONObject().also(::putFields)
}

internal class MiFocusProtocolSameWidthDigitInfo : MiFocusProtocolDigitInfo() {
    var timerInfo: MiFocusProtocolTimerInfo? = null

    fun timerInfo(block: MiFocusProtocolTimerInfo.() -> Unit) {
        (timerInfo ?: MiFocusProtocolTimerInfo().also { timerInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putFields(json)
        timerInfo?.let { json.put("timerInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolIslandShareData {
    var title: String? = null
    var content: String? = null
    var pic: String? = null
    var shareContent: String? = null
    var sharePic: String? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("title", title)
        putNullable("content", content)
        putNullable("pic", pic)
        putNullable("shareContent", shareContent)
        putNullable("sharePic", sharePic)
    }
}
