package os.kei.core.notification.focus

sealed interface MiFocusExpandedComponent {
    companion object {
        fun officialChat(
            text: MiFocusExpandedText,
            picProfile: MiFocusPictureRef? = null,
            picProfileDark: MiFocusPictureRef? = null,
            appIconPkg: String? = null,
            timer: MiFocusTimer? = null
        ) = Chat(
            text = text,
            picProfile = picProfile,
            picProfileDark = picProfileDark,
            appIconPkg = appIconPkg,
            timer = timer
        )

        fun officialHighlight(
            text: MiFocusExpandedText,
            type: Int? = null,
            picFunction: MiFocusPictureRef? = null,
            picFunctionDark: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = Highlight(
            text = text,
            type = type,
            picFunction = picFunction,
            picFunctionDark = picFunctionDark,
            timer = timer
        )

        fun officialHintSecondary(
            payload: OfficialHintPayload
        ): OfficialHint {
            require(payload.type == 2) {
                "officialHintSecondary requires an OfficialHintPayload with type 2"
            }
            return OfficialHint(payload)
        }

        fun officialHintSecondary(
            text: MiFocusExpandedText,
            action: MiFocusNotificationAction,
            titleLineCount: Int? = null,
            colorContentBg: String? = null,
            picContent: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = officialHintSecondary(
            payload = OfficialHintPayload(
                text = text,
                type = 2,
                titleLineCount = titleLineCount,
                colorContentBg = colorContentBg,
                picContent = picContent,
                timer = timer,
                action = action
            )
        )

        fun officialHintPrimary(
            payload: OfficialHintPayload
        ): OfficialHint {
            require(payload.type == 1) {
                "officialHintPrimary requires an OfficialHintPayload with type 1"
            }
            return OfficialHint(payload)
        }

        fun officialHintPrimary(
            text: MiFocusExpandedText,
            action: MiFocusNotificationAction,
            titleLineCount: Int? = null,
            colorContentBg: String? = null,
            picContent: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = officialHintPrimary(
            payload = OfficialHintPayload(
                text = text,
                type = 1,
                titleLineCount = titleLineCount,
                colorContentBg = colorContentBg,
                picContent = picContent,
                timer = timer,
                action = action
            )
        )

        fun button2Hint(
            text: MiFocusExpandedText,
            action: MiFocusNotificationAction,
            type: Int? = 2,
            titleLineCount: Int? = null,
            colorContentBg: String? = null,
            picContent: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = Hint(
            text = text,
            type = type,
            titleLineCount = titleLineCount,
                colorContentBg = colorContentBg,
                picContent = picContent,
                timer = timer,
                action = action
            )

        fun button3Hint(
            text: MiFocusExpandedText,
            action: MiFocusNotificationAction,
            titleLineCount: Int? = null,
            colorContentBg: String? = null,
            picContent: MiFocusPictureRef? = null,
            timer: MiFocusTimer? = null
        ) = Hint(
            text = text,
            type = 1,
            titleLineCount = titleLineCount,
            colorContentBg = colorContentBg,
            picContent = picContent,
            timer = timer,
            action = action
        )

        fun officialActions(
            payload: OfficialActionsPayload
        ) = OfficialActions(
            payload = payload
        )

        fun officialActions(actions: List<MiFocusNotificationAction>) = officialActions(
            payload = OfficialActionsPayload(actions = actions)
        )

        fun button3IconText(
            text: MiFocusExpandedText,
            icon: MiFocusAnimIcon? = null,
            type: Int? = null
        ) = IconText(
            text = text,
            type = type,
            icon = icon
        )

        fun officialTextButtons(
            payload: OfficialTextButtonsPayload
        ) = OfficialTextButtons(
            payload = payload
        )

        fun officialTextButtons(actions: List<MiFocusNotificationAction>) = officialTextButtons(
            payload = OfficialTextButtonsPayload(actions = actions)
        )

        fun button4TextButtons(actions: List<MiFocusNotificationAction>) = TextButtons(actions)

        fun button1Actions(actions: List<MiFocusNotificationAction>) = Actions(actions)

        fun officialBasePrimary(
            text: MiFocusExpandedText,
            picFunction: MiFocusPictureRef? = null,
            showDivider: Boolean? = null,
            showContentDivider: Boolean? = null,
            setMarginTop: Boolean? = null,
            setMarginBottom: Boolean? = null
        ) = Base(
            text = text,
            type = 1,
            picFunction = picFunction,
            showDivider = showDivider,
            showContentDivider = showContentDivider,
            setMarginTop = setMarginTop,
            setMarginBottom = setMarginBottom
        )

        fun officialBaseSecondary(
            text: MiFocusExpandedText,
            picFunction: MiFocusPictureRef? = null,
            showDivider: Boolean? = null,
            showContentDivider: Boolean? = null,
            setMarginTop: Boolean? = null,
            setMarginBottom: Boolean? = null
        ) = Base(
            text = text,
            type = 2,
            picFunction = picFunction,
            showDivider = showDivider,
            showContentDivider = showContentDivider,
            setMarginTop = setMarginTop,
            setMarginBottom = setMarginBottom
        )

        fun officialPictureAppIcon(
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            picDark: MiFocusPictureRef? = null,
            action: MiFocusNotificationAction? = null
        ) = OfficialPicture(
            pic = pic,
            picDark = picDark,
            type = 1,
            action = action
        )

        fun officialPictureMiddle(
            pic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
            picDark: MiFocusPictureRef? = null,
            action: MiFocusNotificationAction? = null
        ) = OfficialPicture(
            pic = pic,
            picDark = picDark,
            type = 2,
            action = action
        )

        fun officialPictureLarge(
            pic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
            picDark: MiFocusPictureRef? = null,
            action: MiFocusNotificationAction? = null
        ) = OfficialPicture(
            pic = pic,
            picDark = picDark,
            type = 3,
            action = action
        )

        fun officialPictureCountdown(
            payload: OfficialPictureType5Payload,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            picDark: MiFocusPictureRef? = null,
            action: MiFocusNotificationAction? = null
        ) = OfficialPicture(
            pic = pic,
            picDark = picDark,
            type = 5,
            type5Payload = payload,
            action = action
        )

        fun officialPictureCountdown(
            title: String,
            colorTitle: String,
            pic: MiFocusPictureRef = MiFocusPictureRef.Display,
            picDark: MiFocusPictureRef? = null,
            action: MiFocusNotificationAction? = null
        ) = officialPictureCountdown(
            payload = OfficialPictureType5Payload(
                title = title,
                colorTitle = colorTitle
            ),
            pic = pic,
            picDark = picDark,
            action = action
        )

        fun officialProgressNodes(
            progressPercent: Int,
            picForward: MiFocusPictureRef,
            picMiddle: MiFocusPictureRef,
            picMiddleUnselected: MiFocusPictureRef,
            picEnd: MiFocusPictureRef,
            picEndUnselected: MiFocusPictureRef,
            colorReach: String? = null,
            colorEnd: String? = null
        ) = Progress(
            progress = MiFocusExpandedProgress(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            ),
            picForward = picForward,
            picMiddle = picMiddle,
            picMiddleUnselected = picMiddleUnselected,
            picEnd = picEnd,
            picEndUnselected = picEndUnselected
        )

        fun officialProgressBar(
            progressPercent: Int,
            colorReach: String? = null,
            colorEnd: String? = null
        ) = Progress(
            progress = MiFocusExpandedProgress(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )

        fun officialCover(
            text: MiFocusExpandedText,
            pic: MiFocusPictureRef
        ) = Cover(
            text = text,
            pic = pic
        )

        fun officialNewImageText(
            payload: OfficialNewImageTextPayload
        ) = OfficialNewImageText(
            payload = payload
        )

        fun officialNewImageText(
            text: MiFocusExpandedText,
            icon: MiFocusAnimIcon? = null,
            type: Int? = null
        ) = officialNewImageText(
            payload = OfficialNewImageTextPayload(
                text = text,
                type = type,
                icon = icon
            )
        )

        fun officialMultiProgress(
            payload: OfficialMultiProgressPayload
        ) = OfficialMultiProgress(
            payload = payload
        )

        fun officialMultiProgress(
            progressPercent: Int,
            color: String? = null,
            points: Int? = null,
            text: MiFocusExpandedText? = null
        ) = officialMultiProgress(
            payload = OfficialMultiProgressPayload(
                progressPercent = progressPercent,
                color = color,
                points = points,
                text = text
            )
        )

        fun officialHighlightCapsule(
            payload: OfficialHighlightCapsulePayload
        ) = OfficialHighlightCapsule(
            payload = payload
        )

        fun officialHighlightCapsule(
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
        ) = officialHighlightCapsule(
            payload = OfficialHighlightCapsulePayload(
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

        fun button5Highlight(
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
        ) = HighlightV3(
            text = text,
            label = label,
            labelColor = labelColor,
            labelColorDark = labelColorDark,
            labelBgColor = labelBgColor,
            labelBgColorDark = labelBgColorDark,
            primaryText = primaryText,
            primaryColor = primaryColor,
            primaryColorDark = primaryColorDark,
            secondaryText = secondaryText,
            secondaryColor = secondaryColor,
            secondaryColorDark = secondaryColorDark,
            showSecondaryLine = showSecondaryLine,
            action = action
        )
    }

    data class Base(
        val text: MiFocusExpandedText,
        val type: Int = 2,
        val showDivider: Boolean? = null,
        val showContentDivider: Boolean? = null,
        val picFunction: MiFocusPictureRef? = null,
        val setMarginTop: Boolean? = null,
        val setMarginBottom: Boolean? = null
    ) : MiFocusExpandedComponent

    data class Chat(
        val text: MiFocusExpandedText,
        val picProfile: MiFocusPictureRef? = null,
        val picProfileDark: MiFocusPictureRef? = null,
        val appIconPkg: String? = null,
        val timer: MiFocusTimer? = null
    ) : MiFocusExpandedComponent

    data class Highlight(
        val text: MiFocusExpandedText,
        val type: Int? = null,
        val picFunction: MiFocusPictureRef? = null,
        val picFunctionDark: MiFocusPictureRef? = null,
        val timer: MiFocusTimer? = null
    ) : MiFocusExpandedComponent

    data class Hint(
        val text: MiFocusExpandedText,
        val type: Int? = null,
        val titleLineCount: Int? = null,
        val colorContentBg: String? = null,
        val picContent: MiFocusPictureRef? = null,
        val timer: MiFocusTimer? = null,
        val action: MiFocusNotificationAction? = null
    ) : MiFocusExpandedComponent

    data class OfficialHintPayload(
        val text: MiFocusExpandedText,
        val type: Int,
        val action: MiFocusNotificationAction,
        val titleLineCount: Int? = null,
        val colorContentBg: String? = null,
        val picContent: MiFocusPictureRef? = null,
        val timer: MiFocusTimer? = null
    ) {
        init {
            require(type in 1..2) { "OfficialHintPayload type must be 1 or 2" }
        }
    }

    data class OfficialHint(
        val payload: OfficialHintPayload
    ) : MiFocusExpandedComponent {
        val text: MiFocusExpandedText
            get() = payload.text

        val type: Int
            get() = payload.type

        val action: MiFocusNotificationAction
            get() = payload.action

        val titleLineCount: Int?
            get() = payload.titleLineCount

        val colorContentBg: String?
            get() = payload.colorContentBg

        val picContent: MiFocusPictureRef?
            get() = payload.picContent

        val timer: MiFocusTimer?
            get() = payload.timer
    }

    data class Progress(
        val progress: MiFocusExpandedProgress,
        val picForward: MiFocusPictureRef? = null,
        val picMiddle: MiFocusPictureRef? = null,
        val picMiddleUnselected: MiFocusPictureRef? = null,
        val picEnd: MiFocusPictureRef? = null,
        val picEndUnselected: MiFocusPictureRef? = null
    ) : MiFocusExpandedComponent

    data class Picture(
        val pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        val picDark: MiFocusPictureRef? = null,
        val type: Int = 0,
        val action: MiFocusNotificationAction? = null
    ) : MiFocusExpandedComponent

    data class OfficialPictureType5Payload(
        val title: String,
        val colorTitle: String? = null
    )

    data class OfficialPicture(
        val pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        val picDark: MiFocusPictureRef? = null,
        val type: Int,
        val type5Payload: OfficialPictureType5Payload? = null,
        val action: MiFocusNotificationAction? = null
    ) : MiFocusExpandedComponent {
        init {
            require(type in 1..5) { "OfficialPicture type must be between 1 and 5" }
            require(type5Payload == null || type == 5) {
                "OfficialPicture type5Payload is only supported for type 5"
            }
        }

        val title: String?
            get() = type5Payload?.title

        val colorTitle: String?
            get() = type5Payload?.colorTitle
    }

    @Deprecated(
        message = "Use OfficialPicture via officialPictureCountdown for public countdown-picture semantics",
        replaceWith = ReplaceWith(
            expression = "MiFocusExpandedComponent.officialPictureCountdown(title = title, colorTitle = colorTitle ?: \"\", pic = pic, picDark = picDark, action = action)"
        )
    )
    data class CountdownPicture(
        val pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        val picDark: MiFocusPictureRef? = null,
        val title: String,
        val colorTitle: String? = null,
        val action: MiFocusNotificationAction? = null
    ) : MiFocusExpandedComponent

    data class Background(
        val type: Int = 1,
        val color: String? = null,
        val pic: MiFocusPictureRef? = null
    ) : MiFocusExpandedComponent

    data class Cover(
        val text: MiFocusExpandedText,
        val pic: MiFocusPictureRef
    ) : MiFocusExpandedComponent

    data class HighlightV3(
        val text: MiFocusExpandedText,
        val label: String? = null,
        val labelColor: String? = null,
        val labelColorDark: String? = null,
        val labelBgColor: String? = null,
        val labelBgColorDark: String? = null,
        val primaryText: String? = null,
        val primaryColor: String? = null,
        val primaryColorDark: String? = null,
        val secondaryText: String? = null,
        val secondaryColor: String? = null,
        val secondaryColorDark: String? = null,
        val showSecondaryLine: Boolean? = null,
        val action: MiFocusNotificationAction? = null
    ) : MiFocusExpandedComponent

    data class OfficialHighlightCapsulePayload(
        val text: MiFocusExpandedText,
        val primaryText: String,
        val action: MiFocusNotificationAction,
        val secondaryText: String? = null,
        val label: String? = null,
        val labelColor: String? = null,
        val labelColorDark: String? = null,
        val labelBgColor: String? = null,
        val labelBgColorDark: String? = null,
        val primaryColor: String? = null,
        val primaryColorDark: String? = null,
        val secondaryColor: String? = null,
        val secondaryColorDark: String? = null,
        val showSecondaryLine: Boolean? = null
    )

    data class OfficialHighlightCapsule(
        val payload: OfficialHighlightCapsulePayload
    ) : MiFocusExpandedComponent {
        val text: MiFocusExpandedText
            get() = payload.text

        val primaryText: String
            get() = payload.primaryText

        val action: MiFocusNotificationAction
            get() = payload.action

        val secondaryText: String?
            get() = payload.secondaryText

        val label: String?
            get() = payload.label

        val labelColor: String?
            get() = payload.labelColor

        val labelColorDark: String?
            get() = payload.labelColorDark

        val labelBgColor: String?
            get() = payload.labelBgColor

        val labelBgColorDark: String?
            get() = payload.labelBgColorDark

        val primaryColor: String?
            get() = payload.primaryColor

        val primaryColorDark: String?
            get() = payload.primaryColorDark

        val secondaryColor: String?
            get() = payload.secondaryColor

        val secondaryColorDark: String?
            get() = payload.secondaryColorDark

        val showSecondaryLine: Boolean?
            get() = payload.showSecondaryLine
    }

    data class IconText(
        val text: MiFocusExpandedText,
        val type: Int? = null,
        val icon: MiFocusAnimIcon? = null
    ) : MiFocusExpandedComponent

    data class OfficialNewImageTextPayload(
        val text: MiFocusExpandedText,
        val type: Int? = null,
        val icon: MiFocusAnimIcon? = null
    )

    data class OfficialNewImageText(
        val payload: OfficialNewImageTextPayload
    ) : MiFocusExpandedComponent {
        val text: MiFocusExpandedText
            get() = payload.text

        val type: Int?
            get() = payload.type

        val icon: MiFocusAnimIcon?
            get() = payload.icon
    }

    data class MultiProgress(
        val progressPercent: Int,
        val color: String? = null,
        val points: Int? = null,
        val text: MiFocusExpandedText? = null
    ) : MiFocusExpandedComponent

    data class OfficialMultiProgressPayload(
        val progressPercent: Int,
        val color: String? = null,
        val points: Int? = null,
        val text: MiFocusExpandedText? = null
    )

    data class OfficialMultiProgress(
        val payload: OfficialMultiProgressPayload
    ) : MiFocusExpandedComponent {
        val progressPercent: Int
            get() = payload.progressPercent

        val color: String?
            get() = payload.color

        val points: Int?
            get() = payload.points

        val text: MiFocusExpandedText?
            get() = payload.text
    }

    data class AnimText(
        val text: MiFocusExpandedText,
        val icon: MiFocusAnimIcon? = null,
        val timer: MiFocusTimer? = null
    ) : MiFocusExpandedComponent

    data class TextButtons(
        val actions: List<MiFocusNotificationAction>
    ) : MiFocusExpandedComponent {
        init {
            require(actions.isNotEmpty()) { "TextButtons requires at least one action" }
            require(actions.size <= 2) { "TextButtons supports at most two actions" }
        }
    }

    data class OfficialTextButtonsPayload(
        val actions: List<MiFocusNotificationAction>
    ) {
        init {
            require(actions.isNotEmpty()) { "OfficialTextButtonsPayload requires at least one action" }
            require(actions.size <= 2) {
                "OfficialTextButtonsPayload supports at most two actions"
            }
        }
    }

    data class OfficialTextButtons(
        val payload: OfficialTextButtonsPayload
    ) : MiFocusExpandedComponent {
        val actions: List<MiFocusNotificationAction>
            get() = payload.actions
    }

    data class Actions(
        val actions: List<MiFocusNotificationAction>
    ) : MiFocusExpandedComponent {
        init {
            require(actions.isNotEmpty()) { "Actions requires at least one action" }
            require(actions.size <= 3) { "Actions supports at most three actions" }
            val hasTextAction = actions.any { it.type == MiFocusActionType.Text }
            if (hasTextAction) {
                require(actions.size == 1) { "Text actions must be used alone in actions" }
                require(actions.all { it.type == MiFocusActionType.Text }) {
                    "Text actions cannot be mixed with other action types"
                }
            }
            actions
                .filter { it.type == MiFocusActionType.Progress }
                .forEach { action ->
                    require(action.progress != null) {
                        "Progress actions require progress metadata"
                    }
                }
        }
    }

    data class OfficialActionsPayload(
        val actions: List<MiFocusNotificationAction>
    ) {
        init {
            require(actions.isNotEmpty()) { "OfficialActionsPayload requires at least one action" }
            require(actions.size <= 3) { "OfficialActionsPayload supports at most three actions" }
            val hasTextAction = actions.any { it.type == MiFocusActionType.Text }
            if (hasTextAction) {
                require(actions.size == 1) {
                    "Text actions must be used alone in official actions payload"
                }
                require(actions.all { it.type == MiFocusActionType.Text }) {
                    "Text actions cannot be mixed with other action types in official actions payload"
                }
            }
            actions
                .filter { it.type == MiFocusActionType.Progress }
                .forEach { action ->
                    require(action.progress != null) {
                        "Progress actions require progress metadata in official actions payload"
                    }
                }
        }
    }

    data class OfficialActions(
        val payload: OfficialActionsPayload
    ) : MiFocusExpandedComponent {
        val actions: List<MiFocusNotificationAction>
            get() = payload.actions
    }
}
