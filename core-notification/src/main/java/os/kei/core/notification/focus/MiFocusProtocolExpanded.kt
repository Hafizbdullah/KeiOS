package os.kei.core.notification.focus

import org.json.JSONObject

internal open class MiFocusProtocolTextAndColorInfo {
    var title: String? = null
    var colorTitle: String? = null
    var colorTitleDark: String? = null
    var subTitle: String? = null
    var colorSubTitle: String? = null
    var colorSubTitleDark: String? = null
    var extraTitle: String? = null
    var colorExtraTitle: String? = null
    var colorExtraTitleDark: String? = null
    var specialTitle: String? = null
    var colorSpecialTitle: String? = null
    var colorSpecialTitleDark: String? = null
    var colorSpecialBg: String? = null
    var content: String? = null
    var colorContent: String? = null
    var colorContentDark: String? = null
    var subContent: String? = null
    var colorSubContent: String? = null
    var colorSubContentDark: String? = null

    fun putTextFields(json: JSONObject) {
        json.putNullable("title", title)
        json.putNullable("colorTitle", colorTitle)
        json.putNullable("colorTitleDark", colorTitleDark)
        json.putNullable("subTitle", subTitle)
        json.putNullable("colorSubTitle", colorSubTitle)
        json.putNullable("colorSubTitleDark", colorSubTitleDark)
        json.putNullable("extraTitle", extraTitle)
        json.putNullable("colorExtraTitle", colorExtraTitle)
        json.putNullable("colorExtraTitleDark", colorExtraTitleDark)
        json.putNullable("specialTitle", specialTitle)
        json.putNullable("colorSpecialTitle", colorSpecialTitle)
        json.putNullable("colorSpecialTitleDark", colorSpecialTitleDark)
        json.putNullable("colorSpecialBg", colorSpecialBg)
        json.putNullable("content", content)
        json.putNullable("colorContent", colorContent)
        json.putNullable("colorContentDark", colorContentDark)
        json.putNullable("subContent", subContent)
        json.putNullable("colorSubContent", colorSubContent)
        json.putNullable("colorSubContentDark", colorSubContentDark)
    }
}

internal class MiFocusProtocolBaseInfo : MiFocusProtocolTextAndColorInfo() {
    var type: Int? = null
    var showDivider: Boolean? = null
    var showContentDivider: Boolean? = null
    var picFunction: String? = null
    var setMarginTop: Boolean? = null
    var setMarginBottom: Boolean? = null

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("type", type)
        json.putNullable("showDivider", showDivider)
        json.putNullable("showContentDivider", showContentDivider)
        json.putNullable("picFunction", picFunction)
        json.putNullable("setMarginTop", setMarginTop)
        json.putNullable("setMarginBottom", setMarginBottom)
    }
}

internal class MiFocusProtocolChatInfo : MiFocusProtocolTextAndColorInfo() {
    var picProfile: String? = null
    var picProfileDark: String? = null
    var appIconPkg: String? = null
    var timerInfo: MiFocusProtocolTimerInfo? = null

    fun timerInfo(block: MiFocusProtocolTimerInfo.() -> Unit) {
        (timerInfo ?: MiFocusProtocolTimerInfo().also { timerInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("picProfile", picProfile)
        json.putNullable("picProfileDark", picProfileDark)
        json.putNullable("appIconPkg", appIconPkg)
        timerInfo?.let { json.put("timerInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolHighlightInfo : MiFocusProtocolTextAndColorInfo() {
    var type: Int? = null
    var picFunction: String? = null
    var picFunctionDark: String? = null
    var timerInfo: MiFocusProtocolTimerInfo? = null

    fun timerInfo(block: MiFocusProtocolTimerInfo.() -> Unit) {
        (timerInfo ?: MiFocusProtocolTimerInfo().also { timerInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("type", type)
        json.putNullable("picFunction", picFunction)
        json.putNullable("picFunctionDark", picFunctionDark)
        timerInfo?.let { json.put("timerInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolHintInfo : MiFocusProtocolTextAndColorInfo() {
    var type: Int? = null
    var titleLineCount: Int? = null
    var colorContentBg: String? = null
    var picContent: String? = null
    var timerInfo: MiFocusProtocolTimerInfo? = null
    var actionInfo: MiFocusProtocolActionInfo? = null

    fun timerInfo(block: MiFocusProtocolTimerInfo.() -> Unit) {
        (timerInfo ?: MiFocusProtocolTimerInfo().also { timerInfo = it }).apply(block)
    }

    fun actionInfo(block: MiFocusProtocolActionInfo.() -> Unit) {
        (actionInfo ?: MiFocusProtocolActionInfo().also { actionInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("type", type)
        json.putNullable("titleLineCount", titleLineCount)
        json.putNullable("colorContentBg", colorContentBg)
        json.putNullable("picContent", picContent)
        timerInfo?.let { json.put("timerInfo", it.toJson()) }
        actionInfo?.let { json.put("actionInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolExpandedProgressInfo {
    var progress: Int? = null
    var isCCW: Boolean? = null
    var isAutoProgress: Boolean? = null
    var colorProgress: String? = null
    var colorProgressEnd: String? = null
    var picForward: String? = null
    var picMiddle: String? = null
    var picMiddleUnselected: String? = null
    var picEnd: String? = null
    var picEndUnselected: String? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("progress", progress)
        putNullable("isCCW", isCCW)
        putNullable("isAutoProgress", isAutoProgress)
        putNullable("colorProgress", colorProgress)
        putNullable("colorProgressEnd", colorProgressEnd)
        putNullable("picForward", picForward)
        putNullable("picMiddle", picMiddle)
        putNullable("picMiddleUnselected", picMiddleUnselected)
        putNullable("picEnd", picEnd)
        putNullable("picEndUnselected", picEndUnselected)
    }
}

internal class MiFocusProtocolExpandedPicInfo {
    var type: Int? = null
    var pic: String? = null
    var picDark: String? = null
    var title: String? = null
    var colorTitle: String? = null
    var actionInfo: MiFocusProtocolActionInfo? = null

    fun actionInfo(block: MiFocusProtocolActionInfo.() -> Unit) {
        (actionInfo ?: MiFocusProtocolActionInfo().also { actionInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("type", type)
        putNullable("pic", pic)
        putNullable("picDark", picDark)
        putNullable("title", title)
        putNullable("colorTitle", colorTitle)
        actionInfo?.let { put("actionInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolBgInfo {
    var type: Int? = null
    var colorBg: String? = null
    var picBg: String? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("type", type)
        putNullable("colorBg", colorBg)
        putNullable("picBg", picBg)
    }
}

internal class MiFocusProtocolCoverInfo : MiFocusProtocolTextAndColorInfo() {
    var picCover: String? = null

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("picCover", picCover)
    }
}

internal class MiFocusProtocolHighlightInfoV3 : MiFocusProtocolTextAndColorInfo() {
    var highLightText: String? = null
    var highLightTextColor: String? = null
    var highLightTextColorDark: String? = null
    var highLightbgColor: String? = null
    var highLightbgColorDark: String? = null
    var primaryColor: String? = null
    var primaryColorDark: String? = null
    var primaryText: String? = null
    var secondaryColor: String? = null
    var secondaryColorDark: String? = null
    var secondaryText: String? = null
    var showSecondaryLine: Boolean? = null
    var actionInfo: MiFocusProtocolActionInfo? = null

    fun actionInfo(block: MiFocusProtocolActionInfo.() -> Unit) {
        (actionInfo ?: MiFocusProtocolActionInfo().also { actionInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("highLightText", highLightText)
        json.putNullable("highLightTextColor", highLightTextColor)
        json.putNullable("highLightTextColorDark", highLightTextColorDark)
        json.putNullable("highLightbgColor", highLightbgColor)
        json.putNullable("highLightbgColorDark", highLightbgColorDark)
        json.putNullable("primaryColor", primaryColor)
        json.putNullable("primaryColorDark", primaryColorDark)
        json.putNullable("primaryText", primaryText)
        json.putNullable("secondaryColor", secondaryColor)
        json.putNullable("secondaryColorDark", secondaryColorDark)
        json.putNullable("secondaryText", secondaryText)
        json.putNullable("showSecondaryLine", showSecondaryLine)
        actionInfo?.let { json.put("actionInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolIconTextInfo : MiFocusProtocolTextAndColorInfo() {
    var type: Int? = null
    var animIconInfo: MiFocusProtocolAnimIconInfo? = null

    fun animIconInfo(block: MiFocusProtocolAnimIconInfo.() -> Unit) {
        (animIconInfo ?: MiFocusProtocolAnimIconInfo().also { animIconInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("type", type)
        animIconInfo?.let { json.put("animIconInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolMultiProgressInfo : MiFocusProtocolTextAndColorInfo() {
    var progress: Int? = null
    var color: String? = null
    var points: Int? = null

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("progress", progress)
        json.putNullable("color", color)
        json.putNullable("points", points)
    }
}

internal class MiFocusProtocolAnimTextInfo : MiFocusProtocolTextAndColorInfo() {
    var animIconInfo: MiFocusProtocolAnimIconInfo? = null
    var timerInfo: MiFocusProtocolTimerInfo? = null

    fun animIconInfo(block: MiFocusProtocolAnimIconInfo.() -> Unit) {
        (animIconInfo ?: MiFocusProtocolAnimIconInfo().also { animIconInfo = it }).apply(block)
    }

    fun timerInfo(block: MiFocusProtocolTimerInfo.() -> Unit) {
        (timerInfo ?: MiFocusProtocolTimerInfo().also { timerInfo = it }).apply(block)
    }

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        animIconInfo?.let { json.put("animIconInfo", it.toJson()) }
        timerInfo?.let { json.put("timerInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolAnimIconInfo : MiFocusProtocolTextAndColorInfo() {
    var type: Int? = null
    var number: Int? = null
    var src: String? = null
    var srcDark: String? = null
    var effectSrc: String? = null
    var effectColor: String? = null
    var autoplay: Boolean? = null
    var loop: Boolean? = null

    fun toJson(): JSONObject = JSONObject().also { json ->
        putTextFields(json)
        json.putNullable("type", type)
        json.putNullable("number", number)
        json.putNullable("src", src)
        json.putNullable("srcDark", srcDark)
        json.putNullable("effectSrc", effectSrc)
        json.putNullable("effectColor", effectColor)
        json.putNullable("autoplay", autoplay)
        json.putNullable("loop", loop)
    }
}

internal class MiFocusProtocolActionInfo {
    var type: Int? = null
    var action: String? = null
    var actionIntent: String? = null
    var actionIntentType: Int? = null
    var actionIcon: String? = null
    var actionIconDark: String? = null
    var actionTitle: String? = null
    var actionTitleColor: String? = null
    var actionTitleColorDark: String? = null
    var actionBgColor: String? = null
    var actionBgColorDark: String? = null
    var actionBgPressColor: String? = null
    var actionBgPressColorDark: String? = null
    var clickWithCollapse: Boolean? = null
    var progressInfo: MiFocusProtocolExpandedProgressInfo? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("type", type)
        putNullable("action", action)
        putNullable("actionIntent", actionIntent)
        putNullable("actionIntentType", actionIntentType)
        putNullable("actionIcon", actionIcon)
        putNullable("actionIconDark", actionIconDark)
        putNullable("actionTitle", actionTitle)
        putNullable("actionTitleColor", actionTitleColor)
        putNullable("actionTitleColorDark", actionTitleColorDark)
        putNullable("actionBgColor", actionBgColor)
        putNullable("actionBgColorDark", actionBgColorDark)
        putNullable("actionBgPressColor", actionBgPressColor)
        putNullable("actionBgPressColorDark", actionBgPressColorDark)
        putNullable("clickWithCollapse", clickWithCollapse)
        progressInfo?.let { put("progressInfo", it.toJson()) }
    }
}

internal class MiFocusProtocolTimerInfo {
    var timerType: Int? = null
    var timerWhen: Long? = null
    var timerTotal: Long? = null
    var timerSystemCurrent: Long? = null

    fun toJson(): JSONObject = JSONObject().apply {
        putNullable("timerType", timerType)
        putNullable("timerWhen", timerWhen)
        putNullable("timerTotal", timerTotal)
        putNullable("timerSystemCurrent", timerSystemCurrent)
    }
}
