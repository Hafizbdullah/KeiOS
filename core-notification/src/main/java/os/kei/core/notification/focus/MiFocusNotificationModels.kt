package os.kei.core.notification.focus

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import os.kei.core.notification.R

const val MI_FOCUS_DEFAULT_BUSINESS = "keios"

data class MiFocusNotificationSpec(
    val title: String,
    val content: String,
    @param:DrawableRes val displayIconResId: Int,
    @param:DrawableRes val expandedIconResId: Int = displayIconResId,
    @param:DrawableRes val actionIconResId: Int = displayIconResId,
    @param:DrawableRes val tickerIconResId: Int = R.drawable.ic_notification_logo,
    val displayPictureSource: MiFocusPictureSource =
        MiFocusPictureSource.Resource(displayIconResId),
    val expandedPictureSource: MiFocusPictureSource =
        MiFocusPictureSource.Resource(expandedIconResId),
    val extraPictures: List<MiFocusPictureAsset> = emptyList(),
    val island: MiFocusIslandSpec? = MiFocusIslandSpec.summaryText(),
    val expanded: MiFocusExpandedSpec = MiFocusExpandedSpec.base(title, content),
    val allowFloat: Boolean = true,
    val islandFirstFloat: Boolean = true,
    val updatable: Boolean = true,
    val showNotification: Boolean? = null,
    val showSmallIcon: Boolean? = null,
    val timeoutMinutes: Int? = null,
    val outerGlow: Boolean = false,
    val outEffectSrc: String? = null,
    val outEffectColor: String? = null,
    val aodTitle: String = title,
    val aodPic: MiFocusPictureRef? = MiFocusPictureRef.TickerLight,
    val ticker: String? = null,
    val compactTicker: String? = null,
    val tickerLightPic: MiFocusPictureRef? = MiFocusPictureRef.TickerLight,
    val tickerDarkPic: MiFocusPictureRef? = MiFocusPictureRef.TickerDark,
    val reopen: String? = null,
    val filterWhenNoPermission: Boolean? = null,
    val hideDeco: Boolean? = null,
    val sequence: Long? = null,
    val privateOverrides: MiFocusPrivateOverrides? = null,
    val business: String? = MI_FOCUS_DEFAULT_BUSINESS,
    val notifyId: String? = null,
    val orderId: String? = null
)

data class MiFocusIslandSpec(
    val bigTemplates: List<MiFocusIslandBigTemplate>,
    val smallTemplate: MiFocusIslandSmallTemplate? =
        MiFocusIslandSmallTemplate.Picture(),
    val property: Int = 1,
    val priority: Int? = null,
    val expandedTimeSeconds: Int? = null,
    val timeoutSeconds: Int? = null,
    val dismissIsland: Boolean? = null,
    val reorderWhenHidden: Boolean? = null,
    val maxSize: Boolean? = null,
    val needCloseAnimation: Boolean? = null,
    val business: String? = null,
    val highlightColor: String? = null,
    val shareData: MiFocusIslandShareData? = null
) {
    companion object {
        fun iconOnlySummary(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic)
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic)
        )

        fun textOnlySummary(
            title: String,
            content: String? = null,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            showHighlightColor: Boolean? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.Text(
                    MiFocusIslandText(
                        title = title,
                        content = content,
                        showHighlightColor = showHighlightColor
                    )
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic)
        )

        fun iconTextSummary(
            rightTitle: String,
            rightFrontTitle: String? = null,
            rightContent: String? = null,
            rightPic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
            leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
            highlightColor: String? = null,
            narrowFont: Boolean? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = leftPic),
                MiFocusIslandBigTemplate.officialIconTextRight(
                    text = MiFocusIslandText(
                        frontTitle = rightFrontTitle,
                        title = rightTitle,
                        content = rightContent,
                        narrowFont = narrowFont,
                        showHighlightColor = highlightColor != null
                    ),
                    pic = MiFocusIslandPic.officialStatic(pic = rightPic)
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = leftPic),
            highlightColor = highlightColor
        )

        fun summaryText(
            title: String = "",
            content: String? = null,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.officialTerminalTextRight(
                    text = MiFocusIslandText(title = title, content = content)
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic)
        )

        fun progressSummary(
            progressPercent: Int,
            content: String,
            progressText: String = "$progressPercent%",
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            colorReach: String,
            colorUnReach: String,
            highlightColor: String? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.ProgressText(
                    text = MiFocusIslandText(title = progressText, content = content),
                    progress = MiFocusIslandProgress(
                        progressPercent = progressPercent,
                        colorReach = colorReach,
                        colorUnReach = colorUnReach
                    )
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialProgressPicture(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorUnReach = colorUnReach,
                pic = pic
            ),
            highlightColor = highlightColor
        )

        fun countdownSummary(
            content: String,
            deadlineAtMs: Long,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            nowMs: Long = System.currentTimeMillis(),
            highlightColor: String? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.SameWidthDigit(
                    content = content,
                    timer = MiFocusTimer.countdown(deadlineAtMs = deadlineAtMs, nowMs = nowMs),
                    showHighlightColor = highlightColor != null
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic),
            highlightColor = highlightColor
        )

        fun terminalSummary(
            title: String,
            content: String? = null,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            highlightColor: String? = null,
            showHighlightColor: Boolean = highlightColor != null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.officialTerminalTextRight(
                    text = MiFocusIslandText(
                        title = title,
                        content = content,
                        showHighlightColor = showHighlightColor
                    )
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic),
            highlightColor = highlightColor
        )

        fun terminalIconSummary(
            title: String,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            highlightColor: String? = null,
            narrowFont: Boolean? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.officialTerminalTextRight(
                    text = MiFocusIslandText(
                        title = title,
                        narrowFont = narrowFont,
                        showHighlightColor = highlightColor != null
                    ),
                    pic = MiFocusIslandPic.officialStatic(pic = pic)
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic),
            highlightColor = highlightColor
        )

        fun fixedDigitSummary(
            digit: String,
            content: String? = null,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            highlightColor: String? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = pic),
                MiFocusIslandBigTemplate.FixedWidthDigit(
                    digit = digit,
                    content = content,
                    showHighlightColor = highlightColor != null
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = pic),
            highlightColor = highlightColor
        )

        fun largePictureSummary(
            leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
            rightPic: MiFocusPictureRef = MiFocusPictureRef.Expanded
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialImageTextLeft(pic = leftPic),
                MiFocusIslandBigTemplate.officialPicture(pic = rightPic)
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialPicture(pic = leftPic)
        )

        fun dualImageTextSummary(
            rightTitle: String? = null,
            rightContent: String? = null,
            rightPic: MiFocusPictureRef? = null,
            leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
            highlightColor: String? = null
        ) = MiFocusIslandSpec(
            bigTemplates = listOf(
                MiFocusIslandBigTemplate.officialDualImageTextLeft(
                    pic = leftPic
                ),
                MiFocusIslandBigTemplate.officialDualImageTextRight(
                    text = MiFocusIslandText(
                        title = rightTitle,
                        content = rightContent,
                        showHighlightColor = highlightColor != null
                    ),
                    pic = rightPic
                )
            ),
            smallTemplate = MiFocusIslandSmallTemplate.officialImageTextRight(
                text = MiFocusIslandText(
                    title = rightTitle,
                    showHighlightColor = highlightColor != null
                ),
                pic = rightPic ?: leftPic
            ),
            highlightColor = highlightColor
        )
    }
}

sealed interface MiFocusIslandBigTemplate {
    companion object {
        fun officialImageTextLeft(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            text: MiFocusIslandText? = null,
            progress: MiFocusIslandProgress? = null
        ) = ImageTextLeft(
            type = 1,
            text = text,
            pic = MiFocusIslandPic.officialStatic(pic = pic),
            progress = progress
        )

        fun officialPicture(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display
        ) = Picture(
            pic = MiFocusIslandPic.officialStatic(pic = pic)
        )

        fun officialIconTextRight(
            text: MiFocusIslandText,
            pic: MiFocusIslandPic? = null,
            progress: MiFocusIslandProgress? = null
        ) = ImageTextRight(
            type = 2,
            text = text,
            pic = pic,
            progress = progress
        )

        fun officialTerminalTextRight(
            text: MiFocusIslandText,
            pic: MiFocusIslandPic? = null,
            progress: MiFocusIslandProgress? = null
        ) = ImageTextRight(
            type = 3,
            text = text,
            pic = pic,
            progress = progress
        )

        fun officialDualImageTextLeft(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            text: MiFocusIslandText? = null,
            progress: MiFocusIslandProgress? = null
        ) = ImageTextLeft(
            type = 5,
            text = text,
            pic = MiFocusIslandPic.officialCompact(pic = pic),
            progress = progress
        )

        fun officialDualImageTextRight(
            text: MiFocusIslandText,
            pic: MiFocusPictureRef? = null,
            progress: MiFocusIslandProgress? = null
        ) = ImageTextRight(
            type = 6,
            text = text,
            pic = pic?.let { MiFocusIslandPic.officialCompact(pic = it) },
            progress = progress
        )
    }

    data class Text(val text: MiFocusIslandText) : MiFocusIslandBigTemplate

    data class Picture(
        val pic: MiFocusIslandPic = MiFocusIslandPic.officialStatic()
    ) : MiFocusIslandBigTemplate

    data class ImageTextLeft(
        override val type: Int = 1,
        override val text: MiFocusIslandText? = null,
        override val pic: MiFocusIslandPic? = MiFocusIslandPic.officialStatic(),
        override val progress: MiFocusIslandProgress? = null
    ) : MiFocusIslandImageTextTemplate, MiFocusIslandBigTemplate

    data class ImageTextRight(
        override val type: Int = 3,
        override val text: MiFocusIslandText? = null,
        override val pic: MiFocusIslandPic? = null,
        override val progress: MiFocusIslandProgress? = null
    ) : MiFocusIslandImageTextTemplate, MiFocusIslandBigTemplate

    data class ProgressText(
        val text: MiFocusIslandText,
        val progress: MiFocusIslandProgress
    ) : MiFocusIslandBigTemplate

    data class FixedWidthDigit(
        val digit: String,
        val content: String? = null,
        val showHighlightColor: Boolean? = null,
        val turnAnim: Boolean? = null
    ) : MiFocusIslandBigTemplate

    data class SameWidthDigit(
        val digit: String? = null,
        val content: String? = null,
        val timer: MiFocusTimer? = null,
        val showHighlightColor: Boolean? = null,
        val turnAnim: Boolean? = null
    ) : MiFocusIslandBigTemplate
}

sealed interface MiFocusIslandImageTextTemplate {
    val type: Int
    val text: MiFocusIslandText?
    val pic: MiFocusIslandPic?
    val progress: MiFocusIslandProgress?
}

sealed interface MiFocusIslandSmallTemplate {
    companion object {
        fun officialPicture(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display
        ) = Picture(
            pic = MiFocusIslandPic.officialStatic(pic = pic)
        )

        fun officialProgressPicture(
            progressPercent: Int,
            colorReach: String,
            colorUnReach: String,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            smallPic: MiFocusPictureRef? = null
        ) = CombinePic(
            pic = MiFocusIslandPic.officialStatic(pic = pic),
            progress = MiFocusIslandProgress(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorUnReach = colorUnReach
            ),
            smallPic = smallPic?.let { MiFocusIslandPic.officialStatic(pic = it) }
        )

        fun officialImageTextRight(
            text: MiFocusIslandText,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display
        ) = ImageTextRight(
            text = text,
            pic = MiFocusIslandPic.officialCompact(pic = pic)
        )
    }

    data class Picture(
        val pic: MiFocusIslandPic = MiFocusIslandPic.officialStatic()
    ) : MiFocusIslandSmallTemplate

    data class CombinePic(
        val pic: MiFocusIslandPic = MiFocusIslandPic.officialStatic(),
        val progress: MiFocusIslandProgress,
        val smallPic: MiFocusIslandPic? = null
    ) : MiFocusIslandSmallTemplate

    data class ImageTextRight(
        val type: Int = 6,
        val text: MiFocusIslandText,
        val pic: MiFocusIslandPic = MiFocusIslandPic.officialCompact()
    ) : MiFocusIslandSmallTemplate {
        init {
            require(type == 6) { "Small island image text template requires type = 6" }
        }
    }
}

data class MiFocusIslandText(
    val title: String? = null,
    val frontTitle: String? = null,
    val content: String? = null,
    val showHighlightColor: Boolean? = null,
    val narrowFont: Boolean? = null,
    val isTitleDigit: Boolean? = null,
    val turnAnim: Boolean? = null
)

data class MiFocusIslandPic(
    val type: Int = 1,
    val pic: MiFocusPictureRef = MiFocusPictureRef.Display,
    val contentDescription: String? = null,
    val number: Int? = null,
    val effectSrc: String? = null,
    val effectColor: String? = null,
    val autoplay: Boolean? = null,
    val loop: Boolean? = null
) {
    companion object {
        fun officialStatic(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            contentDescription: String? = null,
            number: Int? = null,
            effectSrc: String? = null,
            effectColor: String? = null,
            autoplay: Boolean? = null,
            loop: Boolean? = null
        ) = MiFocusIslandPic(
            type = 1,
            pic = pic,
            contentDescription = contentDescription,
            number = number,
            effectSrc = effectSrc,
            effectColor = effectColor,
            autoplay = autoplay,
            loop = loop
        )

        fun officialCompact(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            contentDescription: String? = null,
            number: Int? = null,
            effectSrc: String? = null,
            effectColor: String? = null,
            autoplay: Boolean? = null,
            loop: Boolean? = null
        ) = MiFocusIslandPic(
            type = 4,
            pic = pic,
            contentDescription = contentDescription,
            number = number,
            effectSrc = effectSrc,
            effectColor = effectColor,
            autoplay = autoplay,
            loop = loop
        )
    }
}

data class MiFocusIslandProgress(
    val progressPercent: Int,
    val colorReach: String? = null,
    val colorUnReach: String? = null,
    val isClockwiseFromTop: Boolean = true
)

data class MiFocusIslandShareData(
    val title: String? = null,
    val content: String? = null,
    val pic: MiFocusPictureRef? = null,
    val shareContent: String? = null,
    val sharePic: MiFocusPictureRef? = null
)

data class MiFocusExpandedSpec(
    val components: List<MiFocusExpandedComponent>
) {
    companion object {
        fun base(title: String, content: String) = MiFocusExpandedSpec(
            components = listOf(
                MiFocusExpandedComponent.officialBaseSecondary(
                    text = MiFocusExpandedText(title = title, content = content.ifBlank { " " })
                )
            )
        )

        fun dualTextButtons(
            text: MiFocusExpandedText,
            secondaryAction: MiFocusNotificationAction,
            primaryAction: MiFocusNotificationAction,
            picture: MiFocusPictureRef? = null
        ) = MiFocusExpandedSpec(
            components = buildList {
                add(MiFocusExpandedComponent.officialBaseSecondary(text = text))
                picture?.let { add(MiFocusExpandedComponent.officialPictureAppIcon(pic = it)) }
                add(
                    MiFocusExpandedComponent.officialTextButtons(
                        actions = listOf(secondaryAction, primaryAction)
                    )
                )
            }
        )

        fun hintAction(
            text: MiFocusExpandedText,
            action: MiFocusNotificationAction,
            titleLineCount: Int? = null,
            colorContentBg: String? = null,
            picContent: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = MiFocusExpandedSpec(
            components = listOf(
                MiFocusExpandedComponent.officialHintSecondary(
                    text = text,
                    action = action,
                    titleLineCount = titleLineCount,
                    colorContentBg = colorContentBg,
                    picContent = picContent,
                    timer = timer
                )
            )
        )

        fun highlightCapsuleAction(
            text: MiFocusExpandedText,
            primaryText: String,
            action: MiFocusNotificationAction,
            secondaryText: String? = null,
            label: String? = null,
            labelColor: String? = null,
            labelColorDark: String? = null,
            labelBgColor: String? = null,
            labelBgColorDark: String? = null,
            primaryColor: String? = null,
            primaryColorDark: String? = null,
            secondaryColor: String? = null,
            secondaryColorDark: String? = null,
            showSecondaryLine: Boolean? = null
        ) = MiFocusExpandedSpec(
            components = listOf(
                MiFocusExpandedComponent.officialHighlightCapsule(
                    text = text,
                    primaryText = primaryText,
                    action = action,
                    secondaryText = secondaryText,
                    label = label,
                    labelColor = labelColor,
                    labelColorDark = labelColorDark,
                    labelBgColor = labelBgColor,
                    labelBgColorDark = labelBgColorDark,
                    primaryColor = primaryColor,
                    primaryColorDark = primaryColorDark,
                    secondaryColor = secondaryColor,
                    secondaryColorDark = secondaryColorDark,
                    showSecondaryLine = showSecondaryLine
                )
            )
        )
    }
}

data class MiFocusExpandedText(
    val title: String? = null,
    val subTitle: String? = null,
    val extraTitle: String? = null,
    val specialTitle: String? = null,
    val content: String? = null,
    val subContent: String? = null,
    val colorTitle: String? = null,
    val colorTitleDark: String? = null,
    val colorSubTitle: String? = null,
    val colorSubTitleDark: String? = null,
    val colorExtraTitle: String? = null,
    val colorExtraTitleDark: String? = null,
    val colorSpecialTitle: String? = null,
    val colorSpecialTitleDark: String? = null,
    val colorSpecialBg: String? = null,
    val colorContent: String? = null,
    val colorContentDark: String? = null,
    val colorSubContent: String? = null,
    val colorSubContentDark: String? = null
)

data class MiFocusExpandedProgress(
    val progressPercent: Int,
    val colorReach: String? = null,
    val colorEnd: String? = null
)

data class MiFocusActionProgress(
    val progressPercent: Int,
    val colorReach: String? = null,
    val colorEnd: String? = null,
    val isClockwiseFromTop: Boolean? = null,
    val autoProgress: Boolean? = null
)

data class MiFocusAnimIcon(
    val src: MiFocusPictureRef? = MiFocusPictureRef.Expanded,
    val srcDark: MiFocusPictureRef? = null,
    val type: Int = 0,
    val number: Int? = null,
    val effectSrc: String? = null,
    val effectColor: String? = null,
    val autoplay: Boolean? = null,
    val loop: Boolean? = null,
    val text: MiFocusExpandedText = MiFocusExpandedText()
)

data class MiFocusNotificationAction(
    val key: String,
    val title: String,
    val pendingIntent: PendingIntent,
    @param:DrawableRes val iconResId: Int? = null,
    val type: MiFocusActionType = MiFocusActionType.Text,
    val isHighlighted: Boolean = false,
    val collapsePanel: Boolean? = null,
    val backgroundColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_BG,
    val backgroundColorDark: String? = null,
    val pressedBackgroundColor: String? = null,
    val pressedBackgroundColorDark: String? = null,
    val titleColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_TITLE,
    val titleColorDark: String? = null,
    val progress: MiFocusActionProgress? = null
)

fun MiFocusNotificationAction.asSecondaryTextButton(): MiFocusNotificationAction {
    return copy(
        isHighlighted = false,
        type = MiFocusActionType.Text
    )
}

fun MiFocusNotificationAction.asPrimaryTextButton(
    backgroundColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_BG,
    titleColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_TITLE,
    backgroundColorDark: String? = null,
    titleColorDark: String? = null,
    pressedBackgroundColor: String? = null,
    pressedBackgroundColorDark: String? = null
): MiFocusNotificationAction {
    return copy(
        type = MiFocusActionType.Text,
        isHighlighted = true,
        backgroundColor = backgroundColor,
        backgroundColorDark = backgroundColorDark,
        titleColor = titleColor,
        titleColorDark = titleColorDark,
        pressedBackgroundColor = pressedBackgroundColor,
        pressedBackgroundColorDark = pressedBackgroundColorDark
    )
}

fun MiFocusNotificationAction.asHighlightCapsuleButton(
    backgroundColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_BG,
    titleColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_TITLE,
    backgroundColorDark: String? = null,
    titleColorDark: String? = null,
    pressedBackgroundColor: String? = null,
    pressedBackgroundColorDark: String? = null
): MiFocusNotificationAction {
    return copy(
        type = MiFocusActionType.Circle,
        isHighlighted = true,
        backgroundColor = backgroundColor,
        backgroundColorDark = backgroundColorDark,
        titleColor = titleColor,
        titleColorDark = titleColorDark,
        pressedBackgroundColor = pressedBackgroundColor,
        pressedBackgroundColorDark = pressedBackgroundColorDark
    )
}

fun MiFocusNotificationAction.asProgressButton(
    progressPercent: Int,
    colorReach: String? = null,
    colorEnd: String? = null,
    isClockwiseFromTop: Boolean? = null,
    autoProgress: Boolean? = null,
    backgroundColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_BG,
    titleColor: String = MiFocusNotificationTemplate.PRIMARY_ACTION_TITLE,
    backgroundColorDark: String? = null,
    titleColorDark: String? = null
): MiFocusNotificationAction {
    return copy(
        type = MiFocusActionType.Progress,
        isHighlighted = true,
        backgroundColor = backgroundColor,
        backgroundColorDark = backgroundColorDark,
        titleColor = titleColor,
        titleColorDark = titleColorDark,
        progress = MiFocusActionProgress(
            progressPercent = progressPercent,
            colorReach = colorReach,
            colorEnd = colorEnd,
            isClockwiseFromTop = isClockwiseFromTop,
            autoProgress = autoProgress
        )
    )
}

enum class MiFocusActionType(val rawValue: Int) {
    Circle(0),
    Progress(1),
    Text(2)
}

data class MiFocusTimer(
    val type: MiFocusTimerType,
    val whenAtMs: Long,
    val totalMs: Long,
    val systemCurrentMs: Long
) {
    companion object {
        fun countdown(deadlineAtMs: Long, nowMs: Long = System.currentTimeMillis()) = MiFocusTimer(
            type = MiFocusTimerType.CountdownStart,
            whenAtMs = deadlineAtMs,
            totalMs = (deadlineAtMs - nowMs).coerceAtLeast(0L),
            systemCurrentMs = nowMs
        )
    }
}

enum class MiFocusTimerType(val rawValue: Int) {
    CountdownPause(-2),
    CountdownStart(-1),
    None(0),
    CountUpStart(1),
    CountUpPause(2)
}

data class MiFocusPictureRef(val key: String) {
    companion object {
        val TickerLight = MiFocusPictureRef("mi_focus_ticker_light")
        val TickerDark = MiFocusPictureRef("mi_focus_ticker_dark")
        val Display = MiFocusPictureRef("mi_focus_display")
        val Expanded = MiFocusPictureRef("mi_focus_expanded")
    }
}

data class MiFocusPictureAsset(
    val ref: MiFocusPictureRef,
    val source: MiFocusPictureSource
)

data class MiFocusPrivateOverrides(
    val contentMode: MiFocusPrivateContentMode = MiFocusPrivateContentMode.Default,
    val splitRule: MiFocusPrivateSplitRule? = null,
    val actionIcons: MiFocusPrivateActionIcons? = null,
    val labelChips: List<MiFocusPrivateLabelChip> = emptyList(),
    val backgroundArtwork: MiFocusPrivateBackgroundArtwork? = null,
    val notes: String? = null
)

enum class MiFocusPrivateContentMode {
    Default,
    ContentOnly,
    SubtitleRightOnly,
    LabelChips,
    SplitContentTwoLines
}

data class MiFocusPrivateSplitRule(
    val delimiter: String,
    val surfaces: Set<MiFocusPrivateSurface> = setOf(MiFocusPrivateSurface.Default)
)

data class MiFocusPrivateActionIcons(
    val leftDefault: MiFocusPictureRef? = null,
    val leftIsland: MiFocusPictureRef? = null,
    val rightDefault: MiFocusPictureRef? = null,
    val rightIsland: MiFocusPictureRef? = null,
    val rightNight: MiFocusPictureRef? = null
)

data class MiFocusPrivateLabelChip(
    val text: String,
    val backgroundColor: String,
    val textColor: String? = null
)

data class MiFocusPrivateBackgroundArtwork(
    val source: MiFocusPictureSource,
    val roundedCornerDp: Int? = null,
    val cropWidthDp: Int? = null,
    val cropHeightDp: Int? = null,
    val surfaces: Set<MiFocusPrivateSurface> = setOf(MiFocusPrivateSurface.Default)
)

enum class MiFocusPrivateSurface {
    Default,
    Island,
    Night,
    Tiny,
    DecoPort,
    Flip
}

sealed interface MiFocusPictureSource {
    data class Resource(
        @param:DrawableRes val resId: Int,
        val tintColor: Int? = null
    ) : MiFocusPictureSource

    data class IconValue(
        val icon: Icon
    ) : MiFocusPictureSource

    data class BitmapValue(
        val bitmap: Bitmap
    ) : MiFocusPictureSource
}
