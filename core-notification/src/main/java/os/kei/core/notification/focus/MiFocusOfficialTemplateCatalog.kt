package os.kei.core.notification.focus

/**
 * Official Xiaomi Super Island template catalog distilled from:
 * - `pId=2131` development guide
 * - `pId=2142` expanded-state design guide
 * - `pId=2143` summary-state design guide
 * - `小米超级岛模板库20260129.pdf`
 *
 * This catalog keeps the official numbering, structure notes, OS support, and the recommended
 * entry point inside the local module. It is intended as an in-repo index so feature work does not
 * need to keep bouncing back to the web or PDF for basic template selection.
 */

enum class MiFocusOfficialOsSupport {
    OS2_OS3,
    OS3_ONLY,
    OS3_WITH_OS2_FALLBACK
}

enum class MiFocusOfficialSummaryTemplate(
    val code: Int,
    val title: String,
    val structure: String,
    val recommendedEntry: String
) {
    TEMPLATE_1(1, "手电筒等", "图文组件1 + 空", "MiFocusOfficialTemplatePresets.summaryTemplate1IconOnly"),
    TEMPLATE_2(2, "专注模式/航班等", "图文组件1 + 文本组件", "MiFocusOfficialTemplatePresets.summaryTemplate2Text"),
    TEMPLATE_3(3, "照片等", "图文组件1 + 图文组件2", "MiFocusOfficialTemplatePresets.summaryTemplate3IconText"),
    TEMPLATE_4(4, "打车/外卖等", "图文组件1 + 图文组件3", "MiFocusOfficialTemplatePresets.summaryTemplate4TerminalIconText"),
    TEMPLATE_5(5, "录音/上传等", "图文组件1 + 进度文本组件", "MiFocusOfficialTemplatePresets.summaryTemplate5ProgressText"),
    TEMPLATE_6(6, "电话/倒计时等", "图文组件1 + 等宽数字文本组件", "MiFocusOfficialTemplatePresets.summaryTemplate6Countdown"),
    TEMPLATE_7(7, "导航/打车等", "图文组件1 + 定宽数字文本组件", "MiFocusOfficialTemplatePresets.summaryTemplate7FixedDigit"),
    TEMPLATE_8(8, "音乐/设备电量等", "图文组件1 + 大图组件", "MiFocusOfficialTemplatePresets.summaryTemplate8LargePicture"),
    TEMPLATE_9(9, "导航红绿灯等", "图文组件5 + 图文组件6", "MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText")
}

enum class MiFocusOfficialSmallIslandTemplate(
    val code: Int,
    val title: String,
    val structure: String,
    val recommendedEntry: String
) {
    TEMPLATE_1(1, "手电筒/录音等", "图标组件", "MiFocusOfficialTemplatePresets.smallTemplate1Icon"),
    TEMPLATE_2(2, "上传/下载等", "图标组合组件", "MiFocusOfficialTemplatePresets.smallTemplate2ProgressIcon"),
    TEMPLATE_3(3, "倒计时/红绿灯等", "图标文本组件", "MiFocusOfficialTemplatePresets.smallTemplate3IconText")
}

enum class MiFocusOfficialExpandedTemplate(
    val code: String,
    val title: String,
    val structure: String,
    val osSupport: MiFocusOfficialOsSupport,
    val recommendedEntry: String
) {
    TEMPLATE_1("1", "天气/导航等", "文本组件1 + 识别图形组件3", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture"),
    TEMPLATE_2("2", "支付等", "文本组件2 + 识别图形组件1", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate2BaseAppIcon"),
    TEMPLATE_3("3", "IM等", "IM图文组件 + 识别图形组件2", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate3ChatMiddlePicture"),
    TEMPLATE_4("4", "打车/外卖等", "文本组件2 + 识别图形组件1 + 进度组件1", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate4BaseAppIconProgressNodes"),
    TEMPLATE_5("5", "进程状态等", "文本组件1 + 识别图形组件1 + 进度组件2", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate5BaseAppIconProgress"),
    TEMPLATE_6("6", "下载/上传等", "文本组件2 + 识别图形组件1 + 进度组件2", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate6BaseAppIconProgress"),
    TEMPLATE_7("7", "社交/群组进度等", "IM图文组件 + 识别图形组件1 + 进度组件2", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate7ChatAppIconProgress"),
    TEMPLATE_8("8", "聊天提醒等", "IM图文组件 + 识别图形组件1 + 按钮组件3", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3"),
    TEMPLATE_9("9", "待处理提醒等", "文本组件2 + 识别图形组件1 + 按钮组件2", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2"),
    TEMPLATE_10("10", "文本 CTA 等", "文本组件2 + 识别图形组件1 + 按钮组件3", MiFocusOfficialOsSupport.OS2_OS3, "MiFocusOfficialTemplatePresets.expandedTemplate10BaseButton3"),
    TEMPLATE_11("11", "高亮倒计时等", "强调图文组件 + 识别图形组件1 + 按钮组件2", MiFocusOfficialOsSupport.OS3_WITH_OS2_FALLBACK, "MiFocusOfficialTemplatePresets.expandedTemplate11HighlightButton2"),
    TEMPLATE_12("12", "IM 操作流", "IM图文组件 + 按钮组件1", MiFocusOfficialOsSupport.OS3_WITH_OS2_FALLBACK, "MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions"),
    TEMPLATE_13("13", "强调操作流", "强调图文组件 + 按钮组件1", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate13HighlightActions"),
    TEMPLATE_14_1("14-1", "新图文信息", "新图文组件", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate14_1NewImageText"),
    TEMPLATE_14_2("14-2", "新图文倒计时信息", "新图文组件 + 倒计时带图组件", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate14_2NewImageTextCountdownPicture"),
    TEMPLATE_15("15", "新图文操作流", "新图文组件 + 按钮组件1", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate15NewImageTextActions"),
    TEMPLATE_16("16", "新图文高亮操作", "新图文组件 + 识别图形组件1 + 按钮组件5", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate16NewImageTextHighlight"),
    TEMPLATE_17("17", "新图文双文字按钮", "新图文组件 + 识别图形组件1 + 按钮组件4", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate17NewImageTextTextButtons"),
    TEMPLATE_18("18", "封面主 CTA", "封面组件 + 识别图形组件1 + 按钮组件5", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate18CoverHighlight"),
    TEMPLATE_19("19", "套餐/流量余额等", "文本组件2 + 识别图形组件1 + 进度组件3", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate19PictureMultiProgress"),
    TEMPLATE_20("20", "游戏/应用下载等", "IM图文组件 + 进度组件2", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate20ChatProgress"),
    TEMPLATE_21("21", "任务/家居进程等", "新图文组件 + 识别图形组件1 + 进度组件3", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate21NewImageTextMultiProgress"),
    TEMPLATE_22("22", "网页新增编号", "新图文组件 + 识别图形组件1 + 进度组件3", MiFocusOfficialOsSupport.OS3_ONLY, "MiFocusOfficialTemplatePresets.expandedTemplate22NewImageTextMultiProgress")
}

enum class MiFocusOfficialTemplateFamily {
    SUMMARY,
    SMALL_ISLAND,
    EXPANDED
}

data class MiFocusOfficialTemplateRoute(
    val family: MiFocusOfficialTemplateFamily,
    val code: String,
    val title: String,
    val structure: String,
    val recommendedEntry: String,
    val primaryHelpers: List<String>,
    val osSupport: MiFocusOfficialOsSupport? = null
)

object MiFocusOfficialTemplateCatalog {
    val summaryTemplates: List<MiFocusOfficialSummaryTemplate> =
        MiFocusOfficialSummaryTemplate.entries.toList()

    val smallIslandTemplates: List<MiFocusOfficialSmallIslandTemplate> =
        MiFocusOfficialSmallIslandTemplate.entries.toList()

    val expandedTemplates: List<MiFocusOfficialExpandedTemplate> =
        MiFocusOfficialExpandedTemplate.entries.toList()

    val summaryRoutes: List<MiFocusOfficialTemplateRoute> = listOf(
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "1",
            title = "手电筒等",
            structure = "图文组件1 + 空",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate1IconOnly",
            primaryHelpers = listOf("MiFocusIslandSpec.iconOnlySummary", "MiFocusIslandBigTemplate.officialImageTextLeft", "MiFocusIslandSmallTemplate.officialPicture")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "2",
            title = "专注模式/航班等",
            structure = "图文组件1 + 文本组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate2Text",
            primaryHelpers = listOf("MiFocusIslandSpec.textOnlySummary", "MiFocusIslandBigTemplate.officialImageTextLeft")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "3",
            title = "照片等",
            structure = "图文组件1 + 图文组件2",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate3IconText",
            primaryHelpers = listOf("MiFocusIslandSpec.iconTextSummary", "MiFocusIslandBigTemplate.officialIconTextRight")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "4",
            title = "打车/外卖等",
            structure = "图文组件1 + 图文组件3",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate4TerminalIconText",
            primaryHelpers = listOf("MiFocusIslandSpec.terminalIconSummary", "MiFocusIslandBigTemplate.officialTerminalTextRight")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "5",
            title = "录音/上传等",
            structure = "图文组件1 + 进度文本组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate5ProgressText",
            primaryHelpers = listOf("MiFocusIslandSpec.progressSummary", "MiFocusIslandSmallTemplate.officialProgressPicture")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "6",
            title = "电话/倒计时等",
            structure = "图文组件1 + 等宽数字文本组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate6Countdown",
            primaryHelpers = listOf("MiFocusIslandSpec.countdownSummary")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "7",
            title = "导航/打车等",
            structure = "图文组件1 + 定宽数字文本组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate7FixedDigit",
            primaryHelpers = listOf("MiFocusIslandSpec.fixedDigitSummary")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "8",
            title = "音乐/设备电量等",
            structure = "图文组件1 + 大图组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate8LargePicture",
            primaryHelpers = listOf("MiFocusIslandSpec.largePictureSummary", "MiFocusIslandBigTemplate.officialPicture")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "9",
            title = "导航红绿灯等",
            structure = "图文组件5 + 图文组件6",
            recommendedEntry = "MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText",
            primaryHelpers = listOf("MiFocusIslandSpec.dualImageTextSummary", "MiFocusIslandBigTemplate.officialDualImageTextLeft", "MiFocusIslandBigTemplate.officialDualImageTextRight", "MiFocusIslandSmallTemplate.officialImageTextRight")
        )
    )

    val smallIslandRoutes: List<MiFocusOfficialTemplateRoute> = listOf(
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SMALL_ISLAND,
            code = "1",
            title = "手电筒/录音等",
            structure = "图标组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.smallTemplate1Icon",
            primaryHelpers = listOf("MiFocusIslandSmallTemplate.officialPicture")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SMALL_ISLAND,
            code = "2",
            title = "上传/下载等",
            structure = "图标组合组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.smallTemplate2ProgressIcon",
            primaryHelpers = listOf("MiFocusIslandSmallTemplate.officialProgressPicture")
        ),
        MiFocusOfficialTemplateRoute(
            family = MiFocusOfficialTemplateFamily.SMALL_ISLAND,
            code = "3",
            title = "倒计时/红绿灯等",
            structure = "图标文本组件",
            recommendedEntry = "MiFocusOfficialTemplatePresets.smallTemplate3IconText",
            primaryHelpers = listOf("MiFocusIslandSmallTemplate.officialImageTextRight")
        )
    )

    val expandedRoutes: List<MiFocusOfficialTemplateRoute> = listOf(
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "1", "天气/导航等", "文本组件1 + 识别图形组件3", "MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture", listOf("MiFocusExpandedComponent.officialBasePrimary", "MiFocusExpandedComponent.officialPictureLarge"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "2", "支付等", "文本组件2 + 识别图形组件1", "MiFocusOfficialTemplatePresets.expandedTemplate2BaseAppIcon", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "3", "IM等", "IM图文组件 + 识别图形组件2", "MiFocusOfficialTemplatePresets.expandedTemplate3ChatMiddlePicture", listOf("MiFocusExpandedComponent.officialChat", "MiFocusExpandedComponent.officialPictureMiddle"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "4", "打车/外卖等", "文本组件2 + 识别图形组件1 + 进度组件1", "MiFocusOfficialTemplatePresets.expandedTemplate4BaseAppIconProgressNodes", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialProgressNodes"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "5", "进程状态等", "文本组件1 + 识别图形组件1 + 进度组件2", "MiFocusOfficialTemplatePresets.expandedTemplate5BaseAppIconProgress", listOf("MiFocusExpandedComponent.officialBasePrimary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialProgressBar"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "6", "下载/上传等", "文本组件2 + 识别图形组件1 + 进度组件2", "MiFocusOfficialTemplatePresets.expandedTemplate6BaseAppIconProgress", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialProgressBar"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "7", "社交/群组进度等", "IM图文组件 + 识别图形组件1 + 进度组件2", "MiFocusOfficialTemplatePresets.expandedTemplate7ChatAppIconProgress", listOf("MiFocusExpandedComponent.officialChat", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialProgressBar"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "8", "聊天提醒等", "IM图文组件 + 识别图形组件1 + 按钮组件3", "MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3", listOf("MiFocusExpandedComponent.officialChat", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHintPrimary"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "9", "待处理提醒等", "文本组件2 + 识别图形组件1 + 按钮组件2", "MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHintSecondary"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "10", "文本 CTA 等", "文本组件2 + 识别图形组件1 + 按钮组件3", "MiFocusOfficialTemplatePresets.expandedTemplate10BaseButton3", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHintPrimary"), MiFocusOfficialOsSupport.OS2_OS3),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "11", "高亮倒计时等", "强调图文组件 + 识别图形组件1 + 按钮组件2", "MiFocusOfficialTemplatePresets.expandedTemplate11HighlightButton2", listOf("MiFocusExpandedComponent.officialHighlight", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHintSecondary"), MiFocusOfficialOsSupport.OS3_WITH_OS2_FALLBACK),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "12", "IM 操作流", "IM图文组件 + 按钮组件1", "MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions", listOf("MiFocusExpandedComponent.officialChat", "MiFocusExpandedComponent.officialActions"), MiFocusOfficialOsSupport.OS3_WITH_OS2_FALLBACK),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "13", "强调操作流", "强调图文组件 + 按钮组件1", "MiFocusOfficialTemplatePresets.expandedTemplate13HighlightActions", listOf("MiFocusExpandedComponent.officialHighlight", "MiFocusExpandedComponent.officialActions"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "14-1", "新图文信息", "新图文组件", "MiFocusOfficialTemplatePresets.expandedTemplate14_1NewImageText", listOf("MiFocusExpandedComponent.officialNewImageText"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "14-2", "新图文倒计时信息", "新图文组件 + 倒计时带图组件", "MiFocusOfficialTemplatePresets.expandedTemplate14_2NewImageTextCountdownPicture", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialPictureCountdown"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "15", "新图文操作流", "新图文组件 + 按钮组件1", "MiFocusOfficialTemplatePresets.expandedTemplate15NewImageTextActions", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialActions"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "16", "新图文高亮操作", "新图文组件 + 识别图形组件1 + 按钮组件5", "MiFocusOfficialTemplatePresets.expandedTemplate16NewImageTextHighlight", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHighlightCapsule"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "17", "新图文双文字按钮", "新图文组件 + 识别图形组件1 + 按钮组件4", "MiFocusOfficialTemplatePresets.expandedTemplate17NewImageTextTextButtons", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialTextButtons"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "18", "封面主 CTA", "封面组件 + 识别图形组件1 + 按钮组件5", "MiFocusOfficialTemplatePresets.expandedTemplate18CoverHighlight", listOf("MiFocusExpandedComponent.officialCover", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialHighlightCapsule"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "19", "套餐/流量余额等", "文本组件2 + 识别图形组件1 + 进度组件3", "MiFocusOfficialTemplatePresets.expandedTemplate19PictureMultiProgress", listOf("MiFocusExpandedComponent.officialBaseSecondary", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialMultiProgress"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "20", "游戏/应用下载等", "IM图文组件 + 进度组件2", "MiFocusOfficialTemplatePresets.expandedTemplate20ChatProgress", listOf("MiFocusExpandedComponent.officialChat", "MiFocusExpandedComponent.officialProgressBar"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "21", "任务/家居进程等", "新图文组件 + 识别图形组件1 + 进度组件3", "MiFocusOfficialTemplatePresets.expandedTemplate21NewImageTextMultiProgress", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialMultiProgress"), MiFocusOfficialOsSupport.OS3_ONLY),
        MiFocusOfficialTemplateRoute(MiFocusOfficialTemplateFamily.EXPANDED, "22", "网页新增编号", "新图文组件 + 识别图形组件1 + 进度组件3", "MiFocusOfficialTemplatePresets.expandedTemplate22NewImageTextMultiProgress", listOf("MiFocusExpandedComponent.officialNewImageText", "MiFocusExpandedComponent.officialPictureAppIcon", "MiFocusExpandedComponent.officialMultiProgress"), MiFocusOfficialOsSupport.OS3_ONLY)
    )

    private val summaryTemplateIndex: Map<Int, MiFocusOfficialSummaryTemplate> =
        summaryTemplates.associateBy { it.code }

    private val smallIslandTemplateIndex: Map<Int, MiFocusOfficialSmallIslandTemplate> =
        smallIslandTemplates.associateBy { it.code }

    private val expandedTemplateIndex: Map<String, MiFocusOfficialExpandedTemplate> =
        expandedTemplates.associateBy { it.code }

    private val routeIndex: Map<Pair<MiFocusOfficialTemplateFamily, String>, MiFocusOfficialTemplateRoute> =
        (summaryRoutes + smallIslandRoutes + expandedRoutes).associateBy { it.family to it.code }

    fun findSummaryTemplate(code: Int): MiFocusOfficialSummaryTemplate? = summaryTemplateIndex[code]

    fun findSmallIslandTemplate(code: Int): MiFocusOfficialSmallIslandTemplate? =
        smallIslandTemplateIndex[code]

    fun findExpandedTemplate(code: String): MiFocusOfficialExpandedTemplate? =
        expandedTemplateIndex[code]

    fun findRoute(
        family: MiFocusOfficialTemplateFamily,
        code: String
    ): MiFocusOfficialTemplateRoute? = routeIndex[family to code]
}

object MiFocusOfficialTemplatePresets {
    private fun defaultNewImageTextIcon(
        pic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        picDark: MiFocusPictureRef? = null
    ) = MiFocusAnimIcon(
        src = pic,
        srcDark = picDark
    )

    private fun defaultNewImageTextPayload(
        text: MiFocusExpandedText,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon(),
        type: Int? = null
    ) = MiFocusExpandedComponent.OfficialNewImageTextPayload(
        text = text,
        type = type,
        icon = icon
    )

    private fun defaultOfficialMultiProgressPayload(
        progressPercent: Int,
        progressTitle: String,
        color: String? = null,
        points: Int? = null,
        progressContent: String? = null,
        progressSubContent: String? = null
    ) = MiFocusExpandedComponent.OfficialMultiProgressPayload(
        progressPercent = progressPercent,
        color = color,
        points = points,
        text = MiFocusExpandedText(
            title = progressTitle,
            content = progressContent,
            subContent = progressSubContent
        )
    )

    private fun defaultOfficialHintPayload(
        type: Int,
        text: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        titleLineCount: Int? = null,
        colorContentBg: String? = null,
        picContent: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = MiFocusExpandedComponent.OfficialHintPayload(
        text = text,
        type = type,
        action = action,
        titleLineCount = titleLineCount,
        colorContentBg = colorContentBg,
        picContent = picContent,
        timer = timer
    )

    private fun defaultOfficialHighlightCapsulePayload(
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
    ) = MiFocusExpandedComponent.OfficialHighlightCapsulePayload(
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

    fun expandedTemplate1BaseLargePicture(
        text: MiFocusExpandedText,
        picture: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        picFunction: MiFocusPictureRef? = null,
        showDivider: Boolean? = null,
        showContentDivider: Boolean? = null,
        setMarginTop: Boolean? = null,
        setMarginBottom: Boolean? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBasePrimary(
                text = text,
                picFunction = picFunction,
                showDivider = showDivider,
                showContentDivider = showContentDivider,
                setMarginTop = setMarginTop,
                setMarginBottom = setMarginBottom
            ),
            MiFocusExpandedComponent.officialPictureLarge(
                pic = picture
            )
        )
    )

    fun expandedTemplate2BaseAppIcon(
        text: MiFocusExpandedText,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picFunction: MiFocusPictureRef? = null,
        showDivider: Boolean? = null,
        showContentDivider: Boolean? = null,
        setMarginTop: Boolean? = null,
        setMarginBottom: Boolean? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(
                text = text,
                picFunction = picFunction,
                showDivider = showDivider,
                showContentDivider = showContentDivider,
                setMarginTop = setMarginTop,
                setMarginBottom = setMarginBottom
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(
                pic = picture
            )
        )
    )

    fun expandedTemplate3ChatMiddlePicture(
        text: MiFocusExpandedText,
        picture: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null,
        timer: MiFocusTimer? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialChat(
                text = text,
                picProfile = picProfile,
                picProfileDark = picProfileDark,
                appIconPkg = appIconPkg,
                timer = timer
            ),
            MiFocusExpandedComponent.officialPictureMiddle(
                pic = picture
            )
        )
    )

    fun expandedTemplate4BaseAppIconProgressNodes(
        text: MiFocusExpandedText,
        progressPercent: Int,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picForward: MiFocusPictureRef,
        picMiddle: MiFocusPictureRef,
        picMiddleUnselected: MiFocusPictureRef,
        picEnd: MiFocusPictureRef,
        picEndUnselected: MiFocusPictureRef,
        colorReach: String? = null,
        colorEnd: String? = null,
        picFunction: MiFocusPictureRef? = null,
        showDivider: Boolean? = null,
        showContentDivider: Boolean? = null,
        setMarginTop: Boolean? = null,
        setMarginBottom: Boolean? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(
                text = text,
                picFunction = picFunction,
                showDivider = showDivider,
                showContentDivider = showContentDivider,
                setMarginTop = setMarginTop,
                setMarginBottom = setMarginBottom
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(
                pic = picture
            ),
            MiFocusExpandedComponent.officialProgressNodes(
                progressPercent = progressPercent,
                picForward = picForward,
                picMiddle = picMiddle,
                picMiddleUnselected = picMiddleUnselected,
                picEnd = picEnd,
                picEndUnselected = picEndUnselected,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )
    )

    fun expandedTemplate5BaseAppIconProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorReach: String? = null,
        colorEnd: String? = null,
        picFunction: MiFocusPictureRef? = null,
        showDivider: Boolean? = null,
        showContentDivider: Boolean? = null,
        setMarginTop: Boolean? = null,
        setMarginBottom: Boolean? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBasePrimary(
                text = text,
                picFunction = picFunction,
                showDivider = showDivider,
                showContentDivider = showContentDivider,
                setMarginTop = setMarginTop,
                setMarginBottom = setMarginBottom
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(
                pic = picture
            ),
            MiFocusExpandedComponent.officialProgressBar(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )
    )

    fun expandedTemplate6BaseAppIconProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorReach: String? = null,
        colorEnd: String? = null,
        picFunction: MiFocusPictureRef? = null,
        showDivider: Boolean? = null,
        showContentDivider: Boolean? = null,
        setMarginTop: Boolean? = null,
        setMarginBottom: Boolean? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(
                text = text,
                picFunction = picFunction,
                showDivider = showDivider,
                showContentDivider = showContentDivider,
                setMarginTop = setMarginTop,
                setMarginBottom = setMarginBottom
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(
                pic = picture
            ),
            MiFocusExpandedComponent.officialProgressBar(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )
    )

    fun expandedTemplate7ChatAppIconProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorReach: String? = null,
        colorEnd: String? = null,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null,
        timer: MiFocusTimer? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialChat(
                text = text,
                picProfile = picProfile,
                picProfileDark = picProfileDark,
                appIconPkg = appIconPkg,
                timer = timer
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(
                pic = picture
            ),
            MiFocusExpandedComponent.officialProgressBar(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )
    )

    fun expandedTemplate8ChatButton3(
        chatText: MiFocusExpandedText,
        hintPayload: MiFocusExpandedComponent.OfficialHintPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialChat(
                text = chatText,
                picProfile = picProfile,
                picProfileDark = picProfileDark,
                appIconPkg = appIconPkg
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHintPrimary(hintPayload)
        )
    )

    fun expandedTemplate8ChatButton3(
        chatText: MiFocusExpandedText,
        buttonText: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null,
        colorContentBg: String? = null,
        picContent: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate8ChatButton3(
        chatText = chatText,
        hintPayload = defaultOfficialHintPayload(
            type = 1,
            text = buttonText,
            action = action,
            colorContentBg = colorContentBg,
            picContent = picContent,
            timer = timer
        ),
        picture = picture,
        picProfile = picProfile,
        picProfileDark = picProfileDark,
        appIconPkg = appIconPkg
    )

    fun expandedTemplate9BaseButton2(
        baseText: MiFocusExpandedText,
        hintPayload: MiFocusExpandedComponent.OfficialHintPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(text = baseText),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHintSecondary(hintPayload)
        )
    )

    fun expandedTemplate9BaseButton2(
        baseText: MiFocusExpandedText,
        buttonText: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorContentBg: String? = null,
        picContent: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate9BaseButton2(
        baseText = baseText,
        hintPayload = defaultOfficialHintPayload(
            type = 2,
            text = buttonText,
            action = action,
            colorContentBg = colorContentBg,
            picContent = picContent,
            timer = timer
        ),
        picture = picture
    )

    fun expandedTemplate10BaseButton3(
        baseText: MiFocusExpandedText,
        hintPayload: MiFocusExpandedComponent.OfficialHintPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(text = baseText),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHintPrimary(hintPayload)
        )
    )

    fun expandedTemplate10BaseButton3(
        baseText: MiFocusExpandedText,
        buttonText: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorContentBg: String? = null,
        picContent: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate10BaseButton3(
        baseText = baseText,
        hintPayload = defaultOfficialHintPayload(
            type = 1,
            text = buttonText,
            action = action,
            colorContentBg = colorContentBg,
            picContent = picContent,
            timer = timer
        ),
        picture = picture
    )

    fun expandedTemplate11HighlightButton2(
        highlightText: MiFocusExpandedText,
        hintPayload: MiFocusExpandedComponent.OfficialHintPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picFunction: MiFocusPictureRef? = null,
        picFunctionDark: MiFocusPictureRef? = null,
        highlightTimer: MiFocusTimer? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialHighlight(
                text = highlightText,
                picFunction = picFunction,
                picFunctionDark = picFunctionDark,
                timer = highlightTimer
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHintSecondary(hintPayload)
        )
    )

    fun expandedTemplate11HighlightButton2(
        highlightText: MiFocusExpandedText,
        buttonText: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        picFunction: MiFocusPictureRef? = null,
        picFunctionDark: MiFocusPictureRef? = null,
        highlightTimer: MiFocusTimer? = null,
        colorContentBg: String? = null,
        picContent: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate11HighlightButton2(
        highlightText = highlightText,
        hintPayload = defaultOfficialHintPayload(
            type = 2,
            text = buttonText,
            action = action,
            colorContentBg = colorContentBg,
            picContent = picContent,
            timer = timer
        ),
        picture = picture,
        picFunction = picFunction,
        picFunctionDark = picFunctionDark,
        highlightTimer = highlightTimer
    )

    fun expandedTemplate12ChatActions(
        chatText: MiFocusExpandedText,
        actionsPayload: MiFocusExpandedComponent.OfficialActionsPayload,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null,
        timer: MiFocusTimer? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialChat(
                text = chatText,
                picProfile = picProfile,
                picProfileDark = picProfileDark,
                appIconPkg = appIconPkg,
                timer = timer
            ),
            MiFocusExpandedComponent.officialActions(actionsPayload)
        )
    )

    fun expandedTemplate12ChatActions(
        chatText: MiFocusExpandedText,
        actions: List<MiFocusNotificationAction>,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate12ChatActions(
        chatText = chatText,
        actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(actions = actions),
        picProfile = picProfile,
        picProfileDark = picProfileDark,
        appIconPkg = appIconPkg,
        timer = timer
    )

    fun expandedTemplate13HighlightActions(
        highlightText: MiFocusExpandedText,
        actionsPayload: MiFocusExpandedComponent.OfficialActionsPayload,
        picFunction: MiFocusPictureRef? = null,
        picFunctionDark: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialHighlight(
                text = highlightText,
                picFunction = picFunction,
                picFunctionDark = picFunctionDark,
                timer = timer
            ),
            MiFocusExpandedComponent.officialActions(actionsPayload)
        )
    )

    fun expandedTemplate13HighlightActions(
        highlightText: MiFocusExpandedText,
        actions: List<MiFocusNotificationAction>,
        picFunction: MiFocusPictureRef? = null,
        picFunctionDark: MiFocusPictureRef? = null,
        timer: MiFocusTimer? = null
    ) = expandedTemplate13HighlightActions(
        highlightText = highlightText,
        actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(actions = actions),
        picFunction = picFunction,
        picFunctionDark = picFunctionDark,
        timer = timer
    )

    fun expandedTemplate14_1NewImageText(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload)
        )
    )

    fun expandedTemplate14_1NewImageText(
        text: MiFocusExpandedText,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon()
    ) = expandedTemplate14_1NewImageText(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        )
    )

    fun expandedTemplate14_2NewImageTextCountdownPicture(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        countdownPayload: MiFocusExpandedComponent.OfficialPictureType5Payload,
        countdownPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        countdownPicDark: MiFocusPictureRef? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialPictureCountdown(
                payload = countdownPayload,
                pic = countdownPic,
                picDark = countdownPicDark
            )
        )
    )

    fun expandedTemplate14_2NewImageTextCountdownPicture(
        text: MiFocusExpandedText,
        countdownTitle: String,
        countdownColor: String,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon(),
        countdownPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        countdownPicDark: MiFocusPictureRef? = null
    ) = expandedTemplate14_2NewImageTextCountdownPicture(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        countdownPayload = MiFocusExpandedComponent.OfficialPictureType5Payload(
            title = countdownTitle,
            colorTitle = countdownColor
        ),
        countdownPic = countdownPic,
        countdownPicDark = countdownPicDark
    )

    fun summaryTemplate1IconOnly(
        pic: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusIslandSpec.iconOnlySummary(pic = pic)

    fun summaryTemplate2Text(
        title: String,
        content: String? = null,
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        showHighlightColor: Boolean? = null
    ) = MiFocusIslandSpec.textOnlySummary(
        title = title,
        content = content,
        pic = pic,
        showHighlightColor = showHighlightColor
    )

    fun summaryTemplate3IconText(
        rightTitle: String,
        rightFrontTitle: String? = null,
        rightContent: String? = null,
        rightPic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        highlightColor: String? = null,
        narrowFont: Boolean? = null
    ) = MiFocusIslandSpec.iconTextSummary(
        rightTitle = rightTitle,
        rightFrontTitle = rightFrontTitle,
        rightContent = rightContent,
        rightPic = rightPic,
        leftPic = leftPic,
        highlightColor = highlightColor,
        narrowFont = narrowFont
    )

    fun summaryTemplate4TerminalIconText(
        title: String,
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        highlightColor: String? = null,
        narrowFont: Boolean? = null
    ) = MiFocusIslandSpec.terminalIconSummary(
        title = title,
        pic = pic,
        highlightColor = highlightColor,
        narrowFont = narrowFont
    )

    fun summaryTemplate5ProgressText(
        progressPercent: Int,
        content: String,
        progressText: String = "$progressPercent%",
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        colorReach: String,
        colorUnReach: String,
        highlightColor: String? = null
    ) = MiFocusIslandSpec.progressSummary(
        progressPercent = progressPercent,
        content = content,
        progressText = progressText,
        pic = pic,
        colorReach = colorReach,
        colorUnReach = colorUnReach,
        highlightColor = highlightColor
    )

    fun summaryTemplate6Countdown(
        content: String,
        deadlineAtMs: Long,
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        nowMs: Long = System.currentTimeMillis(),
        highlightColor: String? = null
    ) = MiFocusIslandSpec.countdownSummary(
        content = content,
        deadlineAtMs = deadlineAtMs,
        pic = pic,
        nowMs = nowMs,
        highlightColor = highlightColor
    )

    fun summaryTemplate7FixedDigit(
        digit: String,
        content: String? = null,
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        highlightColor: String? = null
    ) = MiFocusIslandSpec.fixedDigitSummary(
        digit = digit,
        content = content,
        pic = pic,
        highlightColor = highlightColor
    )

    fun summaryTemplate8LargePicture(
        leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        rightPic: MiFocusPictureRef = MiFocusPictureRef.Expanded
    ) = MiFocusIslandSpec.largePictureSummary(
        leftPic = leftPic,
        rightPic = rightPic
    )

    fun summaryTemplate9DualImageText(
        rightTitle: String? = null,
        rightContent: String? = null,
        rightPic: MiFocusPictureRef? = null,
        leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        highlightColor: String? = null
    ) = MiFocusIslandSpec.dualImageTextSummary(
        rightTitle = rightTitle,
        rightContent = rightContent,
        rightPic = rightPic,
        leftPic = leftPic,
        highlightColor = highlightColor
    )

    fun smallTemplate1Icon(
        pic: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusIslandSmallTemplate.officialPicture(
        pic = pic
    )

    fun smallTemplate2ProgressIcon(
        progressPercent: Int,
        colorReach: String,
        colorUnReach: String,
        pic: MiFocusPictureRef = MiFocusPictureRef.Display,
        smallPic: MiFocusPictureRef? = null
    ) = MiFocusIslandSmallTemplate.officialProgressPicture(
        progressPercent = progressPercent,
        colorReach = colorReach,
        colorUnReach = colorUnReach,
        pic = pic,
        smallPic = smallPic
    )

    fun smallTemplate3IconText(
        rightTitle: String? = null,
        rightContent: String? = null,
        rightPic: MiFocusPictureRef? = null,
        leftPic: MiFocusPictureRef = MiFocusPictureRef.Display,
        highlightColor: String? = null
    ) = MiFocusIslandSmallTemplate.officialImageTextRight(
        text = MiFocusIslandText(
            title = rightTitle,
            content = rightContent,
            showHighlightColor = highlightColor != null
        ),
        pic = rightPic ?: leftPic
    )

    fun expandedTemplate15NewImageTextTextButton(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        textButtonsPayload: MiFocusExpandedComponent.OfficialTextButtonsPayload
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialTextButtons(textButtonsPayload)
        )
    )

    fun expandedTemplate15NewImageTextTextButton(
        text: MiFocusExpandedText,
        action: MiFocusNotificationAction,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon()
    ) = expandedTemplate15NewImageTextTextButton(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        textButtonsPayload = MiFocusExpandedComponent.OfficialTextButtonsPayload(
            actions = listOf(action)
        )
    )

    fun expandedTemplate15NewImageTextActions(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        actionsPayload: MiFocusExpandedComponent.OfficialActionsPayload
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialActions(actionsPayload)
        )
    )

    fun expandedTemplate15NewImageTextActions(
        text: MiFocusExpandedText,
        actions: List<MiFocusNotificationAction>,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon()
    ) = expandedTemplate15NewImageTextActions(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(actions = actions)
    )

    fun expandedTemplate16NewImageTextHighlight(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        highlightPayload: MiFocusExpandedComponent.OfficialHighlightCapsulePayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHighlightCapsule(highlightPayload)
        )
    )

    fun expandedTemplate16NewImageTextHighlight(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        primaryText: String,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
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
    ) = expandedTemplate16NewImageTextHighlight(
        payload = payload,
        highlightPayload = defaultOfficialHighlightCapsulePayload(
            text = payload.text,
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
        ),
        picture = picture
    )

    fun expandedTemplate16NewImageTextHighlight(
        text: MiFocusExpandedText,
        primaryText: String,
        action: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon(),
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
    ) = expandedTemplate16NewImageTextHighlight(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        primaryText = primaryText,
        action = action,
        picture = picture,
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

    fun expandedTemplate17NewImageTextTextButtons(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        textButtonsPayload: MiFocusExpandedComponent.OfficialTextButtonsPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialTextButtons(textButtonsPayload)
        )
    )

    fun expandedTemplate17NewImageTextTextButtons(
        text: MiFocusExpandedText,
        secondaryAction: MiFocusNotificationAction,
        primaryAction: MiFocusNotificationAction,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon()
    ) = expandedTemplate17NewImageTextTextButtons(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        textButtonsPayload = MiFocusExpandedComponent.OfficialTextButtonsPayload(
            actions = listOf(secondaryAction, primaryAction)
        ),
        picture = picture
    )

    fun expandedTemplate18CoverHighlight(
        text: MiFocusExpandedText,
        highlightPayload: MiFocusExpandedComponent.OfficialHighlightCapsulePayload,
        coverPic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialCover(
                text = text,
                pic = coverPic
            ),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialHighlightCapsule(highlightPayload)
        )
    )

    fun expandedTemplate18CoverHighlight(
        text: MiFocusExpandedText,
        primaryText: String,
        action: MiFocusNotificationAction,
        coverPic: MiFocusPictureRef = MiFocusPictureRef.Expanded,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
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
    ) = expandedTemplate18CoverHighlight(
        text = text,
        highlightPayload = defaultOfficialHighlightCapsulePayload(
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
        ),
        coverPic = coverPic,
        picture = picture
    )

    fun expandedTemplate19PictureMultiProgress(
        text: MiFocusExpandedText,
        progressPayload: MiFocusExpandedComponent.OfficialMultiProgressPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialBaseSecondary(text = text),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialMultiProgress(progressPayload)
        )
    )

    fun expandedTemplate19PictureMultiProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        progressTitle: String,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        color: String? = null,
        points: Int? = null,
        progressContent: String? = null,
        progressSubContent: String? = null
    ) = expandedTemplate19PictureMultiProgress(
        text = text,
        progressPayload = defaultOfficialMultiProgressPayload(
            progressPercent = progressPercent,
            progressTitle = progressTitle,
            color = color,
            points = points,
            progressContent = progressContent,
            progressSubContent = progressSubContent
        ),
        picture = picture
    )

    fun expandedTemplate20ChatProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        colorReach: String? = null,
        colorEnd: String? = null,
        picProfile: MiFocusPictureRef? = null,
        picProfileDark: MiFocusPictureRef? = null,
        appIconPkg: String? = null
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialChat(
                text = text,
                picProfile = picProfile,
                picProfileDark = picProfileDark,
                appIconPkg = appIconPkg
            ),
            MiFocusExpandedComponent.officialProgressBar(
                progressPercent = progressPercent,
                colorReach = colorReach,
                colorEnd = colorEnd
            )
        )
    )

    fun expandedTemplate21NewImageTextMultiProgress(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        progressPayload: MiFocusExpandedComponent.OfficialMultiProgressPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
    ) = MiFocusExpandedSpec(
        components = listOf(
            MiFocusExpandedComponent.officialNewImageText(payload),
            MiFocusExpandedComponent.officialPictureAppIcon(pic = picture),
            MiFocusExpandedComponent.officialMultiProgress(progressPayload)
        )
    )

    fun expandedTemplate21NewImageTextMultiProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        progressTitle: String,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon(),
        color: String? = null,
        points: Int? = null,
        progressContent: String? = null,
        progressSubContent: String? = null
    ) = expandedTemplate21NewImageTextMultiProgress(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        progressPayload = defaultOfficialMultiProgressPayload(
            progressPercent = progressPercent,
            progressTitle = progressTitle,
            color = color,
            points = points,
            progressContent = progressContent,
            progressSubContent = progressSubContent
        ),
        picture = picture,
    )

    fun expandedTemplate22NewImageTextMultiProgress(
        payload: MiFocusExpandedComponent.OfficialNewImageTextPayload,
        progressPayload: MiFocusExpandedComponent.OfficialMultiProgressPayload,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
    ) = expandedTemplate21NewImageTextMultiProgress(
        payload = payload,
        progressPayload = progressPayload,
        picture = picture,
    )

    fun expandedTemplate22NewImageTextMultiProgress(
        text: MiFocusExpandedText,
        progressPercent: Int,
        progressTitle: String,
        picture: MiFocusPictureRef = MiFocusPictureRef.Display,
        icon: MiFocusAnimIcon = defaultNewImageTextIcon(),
        color: String? = null,
        points: Int? = null,
        progressContent: String? = null,
        progressSubContent: String? = null
    ) = expandedTemplate22NewImageTextMultiProgress(
        payload = defaultNewImageTextPayload(
            text = text,
            icon = icon
        ),
        progressPayload = defaultOfficialMultiProgressPayload(
            progressPercent = progressPercent,
            progressTitle = progressTitle,
            color = color,
            points = points,
            progressContent = progressContent,
            progressSubContent = progressSubContent
        ),
        picture = picture,
    )
}
