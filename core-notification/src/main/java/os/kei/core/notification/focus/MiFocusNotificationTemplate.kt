package os.kei.core.notification.focus

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.annotation.DrawableRes
import os.kei.core.notification.R
import os.kei.core.notification.identity.NotificationAppIconResolver

/**
 * Xiaomi HyperOS Super Island/Focussed Notification JSON builder facade.
 *
 * Project usage:
 * 1. Build a [MiFocusNotificationSpec].
 * 2. Pick one summary-state big-island template with [MiFocusIslandBigTemplate].
 * 3. Pick one summary-state small-island template with [MiFocusIslandSmallTemplate].
 * 4. Add one or more expanded-state components with [MiFocusExpandedComponent].
 * 5. Pass the returned [Bundle] to `NotificationCompat.Builder.addExtras`.
 * 6. Keep [MiFocusNotificationSpec.privateOverrides] as host-side render intent metadata when
 *    private RemoteViews tweaks are desired later.
 *
 * This facade keeps the project on the public client-side route:
 * `Notification.extras["miui.focus.param"]` + the local Focus V3 encoder.
 * MIPUSH permission flow and Xiaomi Magic dispatch stay outside this file.
 *
 * Summary-state design notes:
 * - Big-island text should stay compact: 2-4 CJK chars, 1-3 digits, or a short percent.
 * - Put full titles and long details in expanded-state components such as [MiFocusExpandedComponent.Base].
 * - Use [MiFocusIslandBigTemplate.ImageTextLeft] plus
 *   [MiFocusIslandBigTemplate.ProgressText] for icon + progress text scenes.
 * - Use [MiFocusIslandBigTemplate.ImageTextLeft] plus
 *   [MiFocusIslandBigTemplate.SameWidthDigit] for countdown scenes.
 * - Use [MiFocusIslandSmallTemplate.CombinePic] when the small island must show progress.
 * - Use [MiFocusIslandSmallTemplate.ImageTextRight] for the official small-island icon+text pattern.
 * - Use [MiFocusIslandBigTemplate.ImageTextRight] with `type = 3` for finished, failed,
 *   cancelled, read, or other terminal short states.
 *
 * Expanded-state design notes:
 * - [MiFocusExpandedComponent.Base] is the safest default for full title/body text.
 * - [MiFocusExpandedComponent.Actions] maps to the official `actions` array and supports
 *   1-3 circle/progress actions or one standalone text action through a final JSON patch.
 * - [MiFocusExpandedComponent.TextButtons] maps to the official `textButton` component and
 *   should contain 1-2 actions in this project: primary action highlighted, secondary action plain.
 * - [MiFocusExpandedComponent.HighlightV3] maps to the official `highlightInfoV3` component and
 *   accepts a single highlighted capsule action.
 * - [MiFocusExpandedComponent.MultiProgress] mirrors summary progress for expanded details.
 * - Keep icons square. Resource/vector icons and bitmap app icons are both supported.
 *
 * APK evidence notes from `com.xiaomi.aicr` 4.0.6:
 * - Medication reminder cards still use the public `Notification.extras` route.
 * - The special-looking left/right buttons are produced by internal RemoteViews icon overrides in
 *   `TakeMedicineHelper`, layered on top of regular focus action wiring.
 * - `MetroHelper` adds colored label chips and a QR action icon.
 * - `MovieReminderHelper` adds cropped rounded background artwork plus a QR action icon.
 * - `HyperMindHabitHelper` and `PrepareCarReminderHelper` switch from content text to subtitle-right
 *   emphasis and reuse island/default action icon pairs across surfaces.
 * - Project-side code can reproduce the public structure and most color/icon treatment. Fully
 *   matching private medicine-button chrome depends on system or host resources.
 * - The local encoder writes small-island `imageTextInfoRight(type = 6)` directly.
 * - Expanded `actions(button1)` uses the official `actions` array.
 * - Expanded `picInfo(type = 5)` includes the official title/color extension fields.
 *
 * JSON composition:
 * - `MiFocusProtocolNotification.buildV3` serializes to `miui.focus.param`.
 * - Pictures are registered with `createPicture(key, Icon/Bitmap)` and referenced by string keys.
 * - Actions are registered with `createAction(key, Notification.Action)` and referenced by string keys.
 * - JSON field names live in local protocol models and stay explicit under R8.
 */
object MiFocusNotificationTemplate {
    const val OUTER_GLOW_SRC = "outer_glow"
    const val PRIMARY_ACTION_BG = "#006EFF"
    const val PRIMARY_ACTION_TITLE = "#FFFFFF"

    fun build(context: Context, spec: MiFocusNotificationSpec): Bundle {
        var pictures: MiFocusPictureRegistry? = null
        var expandedActions: List<MiFocusProtocolActionInfo> = emptyList()
        val bundle = MiFocusProtocolNotification.buildV3 {
            val registry = registerPictures(context, spec)
            pictures = registry
            applyBaseFlags(spec, registry)
            expandedActions = registerExpandedActions(
                context = context,
                components = spec.expanded.components,
                defaultActionIconResId = spec.actionIconResId
            )

            spec.island?.let { islandSpec ->
                island {
                    islandPriority = islandSpec.priority
                    islandProperty = islandSpec.property
                    expandedTime = islandSpec.expandedTimeSeconds
                    islandTimeout = islandSpec.timeoutSeconds
                    dismissIsland = islandSpec.dismissIsland
                    islandOrder = islandSpec.reorderWhenHidden
                    maxSize = islandSpec.maxSize
                    needCloseAnimation = islandSpec.needCloseAnimation
                    business = islandSpec.business
                    highlightColor = islandSpec.highlightColor

                    bigIslandArea {
                        islandSpec.bigTemplates.forEach { template ->
                            applyBigIslandTemplate(template, registry)
                        }
                    }
                    islandSpec.smallTemplate?.let { smallTemplate ->
                        smallIslandArea {
                            applySmallIslandTemplate(smallTemplate, registry)
                        }
                    }
                    islandSpec.shareData?.let { data ->
                        shareData {
                            title = data.title
                            content = data.content
                            pic = registry.keyOf(data.pic)
                            shareContent = data.shareContent
                            sharePic = registry.keyOf(data.sharePic)
                        }
                    }
                }
            }

            spec.expanded.components.forEach { component ->
                applyExpandedComponent(context, component, registry, spec.actionIconResId)
            }
            if (expandedActions.isNotEmpty()) {
                actions { addAll(expandedActions) }
            }
        }
        return bundle
    }

    private fun MiFocusProtocolTemplateV3.registerPictures(
        context: Context,
        spec: MiFocusNotificationSpec
    ): MiFocusPictureRegistry {
        val tickerIconResId =
            if (spec.tickerIconResId == R.drawable.ic_notification_logo) {
                NotificationAppIconResolver.smallIconResId()
            } else {
                spec.tickerIconResId
            }
        val assets = buildList {
            add(
                MiFocusPictureAsset(
                    ref = MiFocusPictureRef.TickerLight,
                    source = MiFocusPictureSource.Resource(
                        resId = tickerIconResId,
                        tintColor = Color.BLACK
                    )
                )
            )
            add(
                MiFocusPictureAsset(
                    ref = MiFocusPictureRef.TickerDark,
                    source = MiFocusPictureSource.Resource(
                        resId = tickerIconResId,
                        tintColor = Color.WHITE
                    )
                )
            )
            add(
                MiFocusPictureAsset(
                    ref = MiFocusPictureRef.Display,
                    source = spec.displayPictureSource
                )
            )
            add(
                MiFocusPictureAsset(
                    ref = MiFocusPictureRef.Expanded,
                    source = spec.expandedPictureSource
                )
            )
            addAll(spec.extraPictures)
        }
        val keys = assets.associate { asset ->
            asset.ref to createPicture(asset.ref.key, asset.source.toParcelable(context))
        }
        return MiFocusPictureRegistry(keys)
    }

    private fun MiFocusProtocolTemplateV3.applyBaseFlags(
        spec: MiFocusNotificationSpec,
        pictures: MiFocusPictureRegistry
    ) {
        islandFirstFloat = spec.islandFirstFloat
        enableFloat = spec.allowFloat
        updatable = spec.updatable
        isShowNotification = spec.showNotification
        showSmallIcon = spec.showSmallIcon
        timeout = spec.timeoutMinutes
        aodTitle = spec.aodTitle
        aodPic = pictures.keyOf(spec.aodPic)
        reopen = spec.reopen
        filterWhenNoPermission = spec.filterWhenNoPermission
        hideDeco = spec.hideDeco
        sequence = spec.sequence
        business = spec.business?.ifBlank { MI_FOCUS_DEFAULT_BUSINESS } ?: MI_FOCUS_DEFAULT_BUSINESS
        notifyId = spec.notifyId
        orderId = spec.orderId
        ticker = spec.ticker ?: spec.compactTicker ?: spec.title
        tickerPic = pictures.keyOf(spec.tickerLightPic)
        tickerPicDark = pictures.keyOf(spec.tickerDarkPic)

        if (spec.outerGlow) {
            outEffectSrc = OUTER_GLOW_SRC
        }
        spec.outEffectColor?.let { outEffectColor = it }
        spec.outEffectSrc?.let { outEffectSrc = it }
    }

    private fun MiFocusProtocolBigIslandArea.applyBigIslandTemplate(
        template: MiFocusIslandBigTemplate,
        pictures: MiFocusPictureRegistry
    ) {
        when (template) {
            is MiFocusIslandBigTemplate.Text -> {
                textInfo = template.text.toIslandTextInfo()
            }

            is MiFocusIslandBigTemplate.Picture -> {
                picInfo = template.pic.toIslandPicInfo(pictures)
            }

            is MiFocusIslandBigTemplate.ImageTextLeft -> {
                imageTextInfoLeft {
                    applyImageTextInfo(template, pictures)
                }
            }

            is MiFocusIslandBigTemplate.ImageTextRight -> {
                imageTextInfoRight {
                    applyImageTextInfo(template, pictures)
                }
            }

            is MiFocusIslandBigTemplate.ProgressText -> {
                progressTextInfo {
                    progressInfo = template.progress.toIslandProgressInfo()
                    textInfo = template.text.toIslandTextInfo()
                }
            }

            is MiFocusIslandBigTemplate.FixedWidthDigit -> {
                fixedWidthDigitInfo {
                    content = template.content
                    digit = template.digit
                    showHighlightColor = template.showHighlightColor
                    turnAnim = template.turnAnim
                }
            }

            is MiFocusIslandBigTemplate.SameWidthDigit -> {
                sameWidthDigitInfo {
                    content = template.content
                    digit = template.digit
                    showHighlightColor = template.showHighlightColor
                    turnAnim = template.turnAnim
                    template.timer?.let { timer ->
                        timerInfo {
                            timerType = timer.type.rawValue
                            timerWhen = timer.whenAtMs
                            timerTotal = timer.totalMs
                            timerSystemCurrent = timer.systemCurrentMs
                        }
                    }
                }
            }
        }
    }

    private fun MiFocusProtocolSmallIslandArea.applySmallIslandTemplate(
        template: MiFocusIslandSmallTemplate,
        pictures: MiFocusPictureRegistry
    ) {
        when (template) {
            is MiFocusIslandSmallTemplate.Picture -> {
                picInfo = template.pic.toIslandPicInfo(pictures)
            }

            is MiFocusIslandSmallTemplate.CombinePic -> {
                combinePicInfo {
                    picInfo = template.pic.toIslandPicInfo(pictures)
                    progressInfo = template.progress.toIslandProgressInfo()
                    smallPicInfo = template.smallPic?.toIslandPicInfo(pictures)
                }
            }

            is MiFocusIslandSmallTemplate.ImageTextRight -> {
                imageTextInfoRight {
                    type = template.type
                    textInfo = template.text.toIslandTextInfo()
                    picInfo = template.pic.toIslandPicInfo(pictures)
                }
            }
        }
    }

    private fun MiFocusProtocolIslandImageTextInfo.applyImageTextInfo(
        template: MiFocusIslandImageTextTemplate,
        pictures: MiFocusPictureRegistry
    ) {
        type = template.type
        textInfo = template.text?.toIslandTextInfo()
        picInfo = template.pic?.toIslandPicInfo(pictures)
        progressInfo = template.progress?.toIslandProgressInfo()
    }

    @Suppress("DEPRECATION")
    private fun MiFocusProtocolTemplateV3.applyExpandedComponent(
        context: Context,
        component: MiFocusExpandedComponent,
        pictures: MiFocusPictureRegistry,
        @DrawableRes defaultActionIconResId: Int
    ) {
        when (component) {
            is MiFocusExpandedComponent.Base -> {
                baseInfo {
                    type = component.type
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    showDivider = component.showDivider
                    showContentDivider = component.showContentDivider
                    picFunction = pictures.keyOf(component.picFunction)
                    setMarginTop = component.setMarginTop
                    setMarginBottom = component.setMarginBottom
                    applyExpandedTextColors(component.text)
                }
            }

            is MiFocusExpandedComponent.Chat -> {
                chatInfo {
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    picProfile = pictures.keyOf(component.picProfile)
                    picProfileDark = pictures.keyOf(component.picProfileDark)
                    appIconPkg = component.appIconPkg
                    component.timer?.let { timer ->
                        timerInfo { applyFocusTimer(timer) }
                    }
                }
            }

            is MiFocusExpandedComponent.Highlight -> {
                highlightInfo {
                    type = component.type
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    picFunction = pictures.keyOf(component.picFunction)
                    picFunctionDark = pictures.keyOf(component.picFunctionDark)
                    component.timer?.let { timer ->
                        timerInfo { applyFocusTimer(timer) }
                    }
                }
            }

            is MiFocusExpandedComponent.Hint -> {
                hintInfo {
                    type = component.type
                    titleLineCount = component.titleLineCount
                    colorContentBg = component.colorContentBg
                    picContent = pictures.keyOf(component.picContent)
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    component.timer?.let { timer ->
                        timerInfo { applyFocusTimer(timer) }
                    }
                    component.action?.let { focusAction ->
                        actionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.OfficialHint -> {
                hintInfo {
                    type = component.type
                    titleLineCount = component.titleLineCount
                    colorContentBg = component.colorContentBg
                    picContent = pictures.keyOf(component.picContent)
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    component.timer?.let { timer ->
                        timerInfo { applyFocusTimer(timer) }
                    }
                    actionInfo {
                        this@applyExpandedComponent.applyAction(
                            target = this,
                            context = context,
                            focusAction = component.action,
                            defaultActionIconResId = defaultActionIconResId
                        )
                    }
                }
            }

            is MiFocusExpandedComponent.Progress -> {
                progressInfo {
                    progress = component.progress.progressPercent.coerceIn(0, 100)
                    colorProgress = component.progress.colorReach
                    colorProgressEnd = component.progress.colorEnd
                    picForward = pictures.keyOf(component.picForward)
                    picMiddle = pictures.keyOf(component.picMiddle)
                    picMiddleUnselected = pictures.keyOf(component.picMiddleUnselected)
                    picEnd = pictures.keyOf(component.picEnd)
                    picEndUnselected = pictures.keyOf(component.picEndUnselected)
                }
            }

            is MiFocusExpandedComponent.Picture -> {
                picInfo {
                    type = component.type
                    pic = pictures.keyOf(component.pic)
                    picDark = pictures.keyOf(component.picDark)
                    component.action?.let { focusAction ->
                        actionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.OfficialPicture -> {
                picInfo {
                    type = component.type
                    pic = pictures.keyOf(component.pic)
                    picDark = pictures.keyOf(component.picDark)
                    component.type5Payload?.let { payload ->
                        title = payload.title
                        colorTitle = payload.colorTitle
                    }
                    component.action?.let { focusAction ->
                        actionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.CountdownPicture -> {
                picInfo {
                    type = 5
                    pic = pictures.keyOf(component.pic)
                    picDark = pictures.keyOf(component.picDark)
                    title = component.title
                    colorTitle = component.colorTitle
                    component.action?.let { focusAction ->
                        actionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.Background -> {
                bgInfo {
                    type = component.type
                    colorBg = component.color
                    picBg = pictures.keyOf(component.pic)
                }
            }

            is MiFocusExpandedComponent.Cover -> {
                coverInfo = MiFocusProtocolCoverInfo().apply {
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    picCover = pictures.keyOf(component.pic)
                }
            }

            is MiFocusExpandedComponent.HighlightV3 -> {
                highlightInfoV3 {
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    highLightText = component.label
                    highLightTextColor = component.labelColor
                    highLightTextColorDark = component.labelColorDark
                    highLightbgColor = component.labelBgColor
                    highLightbgColorDark = component.labelBgColorDark
                    primaryColor = component.primaryColor
                    primaryColorDark = component.primaryColorDark
                    primaryText = component.primaryText
                    secondaryColor = component.secondaryColor
                    secondaryColorDark = component.secondaryColorDark
                    secondaryText = component.secondaryText
                    showSecondaryLine = component.showSecondaryLine
                    component.action?.let { focusAction ->
                        actionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.OfficialHighlightCapsule -> {
                highlightInfoV3 {
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    highLightText = component.label
                    highLightTextColor = component.labelColor
                    highLightTextColorDark = component.labelColorDark
                    highLightbgColor = component.labelBgColor
                    highLightbgColorDark = component.labelBgColorDark
                    primaryColor = component.primaryColor
                    primaryColorDark = component.primaryColorDark
                    primaryText = component.primaryText
                    secondaryColor = component.secondaryColor
                    secondaryColorDark = component.secondaryColorDark
                    secondaryText = component.secondaryText
                    showSecondaryLine = component.showSecondaryLine
                    actionInfo {
                        this@applyExpandedComponent.applyAction(
                            target = this,
                            context = context,
                            focusAction = component.action,
                            defaultActionIconResId = defaultActionIconResId
                        )
                    }
                }
            }

            is MiFocusExpandedComponent.IconText -> {
                iconTextInfo {
                    applyExpandedIconText(
                        text = component.text,
                        type = component.type,
                        icon = component.icon,
                        pictures = pictures
                    )
                }
            }

            is MiFocusExpandedComponent.OfficialNewImageText -> {
                iconTextInfo {
                    applyExpandedIconText(
                        text = component.text,
                        type = component.type,
                        icon = component.icon,
                        pictures = pictures
                    )
                }
            }

            is MiFocusExpandedComponent.MultiProgress -> {
                multiProgressInfo {
                    applyExpandedMultiProgress(
                        progressPercent = component.progressPercent,
                        color = component.color,
                        points = component.points,
                        text = component.text
                    )
                }
            }

            is MiFocusExpandedComponent.OfficialMultiProgress -> {
                multiProgressInfo {
                    applyExpandedMultiProgress(
                        progressPercent = component.progressPercent,
                        color = component.color,
                        points = component.points,
                        text = component.text
                    )
                }
            }

            is MiFocusExpandedComponent.AnimText -> {
                animTextInfo {
                    applyExpandedTextColors(component.text)
                    title = component.text.title
                    subTitle = component.text.subTitle
                    extraTitle = component.text.extraTitle
                    specialTitle = component.text.specialTitle
                    content = component.text.content
                    subContent = component.text.subContent
                    component.icon?.let { icon ->
                        animIconInfo {
                            applyAnimIcon(icon, pictures)
                        }
                    }
                    component.timer?.let { timer ->
                        timerInfo { applyFocusTimer(timer) }
                    }
                }
            }

            is MiFocusExpandedComponent.TextButtons -> {
                textButton {
                    component.actions.take(2).forEach { focusAction ->
                        addActionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.OfficialTextButtons -> {
                textButton {
                    component.actions.take(2).forEach { focusAction ->
                        addActionInfo {
                            this@applyExpandedComponent.applyAction(
                                target = this,
                                context = context,
                                focusAction = focusAction,
                                defaultActionIconResId = defaultActionIconResId
                            )
                        }
                    }
                }
            }

            is MiFocusExpandedComponent.Actions -> Unit
            is MiFocusExpandedComponent.OfficialActions -> Unit
        }
    }

    private fun MiFocusProtocolTextAndColorInfo.applyExpandedTextColors(
        text: MiFocusExpandedText
    ) {
        colorTitle = text.colorTitle
        colorTitleDark = text.colorTitleDark
        colorSubTitle = text.colorSubTitle
        colorSubTitleDark = text.colorSubTitleDark
        colorExtraTitle = text.colorExtraTitle
        colorExtraTitleDark = text.colorExtraTitleDark
        colorSpecialTitle = text.colorSpecialTitle
        colorSpecialTitleDark = text.colorSpecialTitleDark
        colorSpecialBg = text.colorSpecialBg
        colorContent = text.colorContent
        colorContentDark = text.colorContentDark
        colorSubContent = text.colorSubContent
        colorSubContentDark = text.colorSubContentDark
    }

    private fun MiFocusProtocolIconTextInfo.applyExpandedIconText(
        text: MiFocusExpandedText,
        type: Int?,
        icon: MiFocusAnimIcon?,
        pictures: MiFocusPictureRegistry
    ) {
        this.type = type
        applyExpandedTextColors(text)
        title = text.title
        subTitle = text.subTitle
        extraTitle = text.extraTitle
        specialTitle = text.specialTitle
        content = text.content
        subContent = text.subContent
        icon?.let { animIcon ->
            animIconInfo {
                applyAnimIcon(animIcon, pictures)
            }
        }
    }

    private fun MiFocusProtocolMultiProgressInfo.applyExpandedMultiProgress(
        progressPercent: Int,
        color: String?,
        points: Int?,
        text: MiFocusExpandedText?
    ) {
        progress = progressPercent.coerceIn(0, 100)
        this.color = color
        this.points = points?.coerceIn(0, 4)
        title = text?.title
        subTitle = text?.subTitle
        content = text?.content
        subContent = text?.subContent
        text?.let { applyExpandedTextColors(it) }
    }

    private fun MiFocusProtocolTemplateV3.applyAction(
        target: MiFocusProtocolActionInfo,
        context: Context,
        focusAction: MiFocusNotificationAction,
        @DrawableRes defaultActionIconResId: Int
    ) {
        target.type = focusAction.type.rawValue
        val nativeAction = Notification.Action.Builder(
            Icon.createWithResource(context, focusAction.iconResId ?: defaultActionIconResId),
            focusAction.title,
            focusAction.pendingIntent
        ).build()
        target.action = createAction(focusAction.key, nativeAction)
        target.actionTitle = focusAction.title
        target.clickWithCollapse = focusAction.collapsePanel
        if (focusAction.isHighlighted) {
            target.actionBgColor = focusAction.backgroundColor
            target.actionBgColorDark =
                focusAction.backgroundColorDark ?: focusAction.backgroundColor
            target.actionBgPressColor = focusAction.pressedBackgroundColor
            target.actionBgPressColorDark = focusAction.pressedBackgroundColorDark
            target.actionTitleColor = focusAction.titleColor
            target.actionTitleColorDark = focusAction.titleColorDark ?: focusAction.titleColor
        }
    }

    private fun MiFocusProtocolTemplateV3.registerExpandedActions(
        context: Context,
        components: List<MiFocusExpandedComponent>,
        @DrawableRes defaultActionIconResId: Int
    ): List<MiFocusProtocolActionInfo> {
        return components
            .asSequence()
            .mapNotNull { component ->
                when (component) {
                    is MiFocusExpandedComponent.Actions -> component.actions
                    is MiFocusExpandedComponent.OfficialActions -> component.actions
                    else -> null
                }
            }
            .flatMap { actions ->
                actions.map { focusAction ->
                    val patchedAction = focusAction.copy(
                        key = officialPatchedActionKey(focusAction.key)
                    )
                    val actionInfo = MiFocusProtocolActionInfo()
                    applyAction(
                        target = actionInfo,
                        context = context,
                        focusAction = patchedAction,
                        defaultActionIconResId = defaultActionIconResId
                    )
                    focusAction.progress?.let { progress ->
                        actionInfo.progressInfo =
                            MiFocusProtocolExpandedProgressInfo().also {
                                it.progress = progress.progressPercent.coerceIn(0, 100)
                                it.isCCW = progress.isClockwiseFromTop
                                it.isAutoProgress = progress.autoProgress
                                it.colorProgress = progress.colorReach
                                it.colorProgressEnd = progress.colorEnd
                            }
                    }
                    actionInfo
                }
            }
            .toList()
    }

    private fun MiFocusProtocolAnimIconInfo.applyAnimIcon(
        icon: MiFocusAnimIcon,
        pictures: MiFocusPictureRegistry
    ) {
        type = icon.type
        number = icon.number
        src = pictures.keyOf(icon.src)
        srcDark = pictures.keyOf(icon.srcDark)
        effectSrc = icon.effectSrc
        effectColor = icon.effectColor
        autoplay = icon.autoplay
        loop = icon.loop
        title = icon.text.title
        subTitle = icon.text.subTitle
        content = icon.text.content
        subContent = icon.text.subContent
        applyExpandedTextColors(icon.text)
    }

    private fun MiFocusProtocolTimerInfo.applyFocusTimer(
        timer: MiFocusTimer
    ) {
        timerType = timer.type.rawValue
        timerWhen = timer.whenAtMs
        timerTotal = timer.totalMs
        timerSystemCurrent = timer.systemCurrentMs
    }

    private fun officialPatchedActionKey(key: String): String {
        return if (key.startsWith("miui.focus.action_")) {
            key
        } else {
            "miui.focus.action_$key"
        }
    }
}

private class MiFocusPictureRegistry(
    private val keys: Map<MiFocusPictureRef, String>
) {
    fun keyOf(ref: MiFocusPictureRef?): String? {
        return ref?.let { keys[it] }
    }
}

private fun MiFocusPictureSource.toParcelable(context: Context): android.os.Parcelable {
    return when (this) {
        is MiFocusPictureSource.Resource -> {
            Icon.createWithResource(context, resId).also { icon ->
                tintColor?.let(icon::setTint)
            }
        }

        is MiFocusPictureSource.IconValue -> icon
        is MiFocusPictureSource.BitmapValue -> Icon.createWithBitmap(bitmap)
    }
}

private fun MiFocusIslandText.toIslandTextInfo() =
    MiFocusProtocolIslandTextInfo().also { info ->
        info.title = title
        info.frontTitle = frontTitle
        info.content = content
        info.showHighlightColor = showHighlightColor
        info.narrowFont = narrowFont
        info.isTitleDigit = isTitleDigit
        info.turnAnim = turnAnim
    }

private fun MiFocusIslandPic.toIslandPicInfo(
    pictures: MiFocusPictureRegistry
) = MiFocusProtocolIslandPicInfo().also { info ->
    info.type = type
    info.pic = pictures.keyOf(pic)
    info.contentDescription = contentDescription
    info.number = number
    info.effectSrc = effectSrc
    info.effectColor = effectColor
    info.autoplay = autoplay
    info.loop = loop
}

private fun MiFocusIslandProgress.toIslandProgressInfo() =
    MiFocusProtocolIslandProgressInfo().also { info ->
        info.progress = progressPercent.coerceIn(0, 100)
        info.isCCW = isClockwiseFromTop
        info.colorReach = colorReach
        info.colorUnReach = colorUnReach
    }
