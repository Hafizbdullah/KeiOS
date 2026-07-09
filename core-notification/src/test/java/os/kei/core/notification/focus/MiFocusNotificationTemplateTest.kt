package os.kei.core.notification.focus

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.notification.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(
    application = MiFocusNotificationTemplateTestApp::class,
    sdk = [35]
)
class MiFocusNotificationTemplateTest {
    @Test
    fun `summary island facade emits every supported big and small template`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val now = 1778000000000L
        val cases = listOf(
            "textInfo" to MiFocusIslandBigTemplate.Text(MiFocusIslandText(title = "完成")),
            "picInfo" to MiFocusIslandBigTemplate.Picture(),
            "imageTextInfoLeft" to MiFocusIslandBigTemplate.ImageTextLeft(),
            "imageTextInfoRight" to MiFocusIslandBigTemplate.ImageTextRight(
                text = MiFocusIslandText(title = "失败")
            ),
            "progressTextInfo" to MiFocusIslandBigTemplate.ProgressText(
                text = MiFocusIslandText(title = "72%"),
                progress = MiFocusIslandProgress(72, "#2563EB", "#334155")
            ),
            "fixedWidthDigitInfo" to MiFocusIslandBigTemplate.FixedWidthDigit(
                digit = "128",
                content = "AP"
            ),
            "sameWidthDigitInfo" to MiFocusIslandBigTemplate.SameWidthDigit(
                content = "活动",
                timer = MiFocusTimer.countdown(deadlineAtMs = now + 60000L, nowMs = now)
            )
        )

        cases.forEach { (token, bigTemplate) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    island = MiFocusIslandSpec(
                        bigTemplates = listOf(bigTemplate),
                        smallTemplate = MiFocusIslandSmallTemplate.CombinePic(
                            progress = MiFocusIslandProgress(72, "#2563EB", "#334155")
                        )
                    )
                )
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()

            assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
            assertTrue(
                focusParam.contains("combinePicInfo"),
                "Missing combinePicInfo in $focusParam"
            )
        }
    }

    @Test
    fun `expanded facade emits all supported expanded templates and actions`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val action = MiFocusNotificationAction(
            key = "focus_test_open",
            title = "Open",
            pendingIntent = openPendingIntent,
            isHighlighted = true
        )
        val bundle = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(
                expanded = MiFocusExpandedSpec(
                    components = listOf(
                        MiFocusExpandedComponent.Base(MiFocusExpandedText(title = "Base")),
                        MiFocusExpandedComponent.Chat(MiFocusExpandedText(title = "Chat")),
                        MiFocusExpandedComponent.Highlight(MiFocusExpandedText(title = "Highlight")),
                        MiFocusExpandedComponent.Hint(
                            text = MiFocusExpandedText(title = "Hint"),
                            action = action
                        ),
                        MiFocusExpandedComponent.Progress(MiFocusExpandedProgress(64, "#2563EB")),
                        MiFocusExpandedComponent.Picture(action = action),
                        MiFocusExpandedComponent.Background(color = "#101010"),
                        MiFocusExpandedComponent.Cover(
                            text = MiFocusExpandedText(title = "Cover"),
                            pic = MiFocusPictureRef.Expanded
                        ),
                        MiFocusExpandedComponent.HighlightV3(
                            text = MiFocusExpandedText(title = "V3"),
                            label = "HOT",
                            action = action
                        ),
                        MiFocusExpandedComponent.IconText(
                            text = MiFocusExpandedText(title = "Icon"),
                            icon = MiFocusAnimIcon()
                        ),
                        MiFocusExpandedComponent.MultiProgress(
                            progressPercent = 64,
                            color = "#2563EB",
                            points = 2
                        ),
                        MiFocusExpandedComponent.AnimText(
                            text = MiFocusExpandedText(title = "Anim"),
                            icon = MiFocusAnimIcon(),
                            timer = MiFocusTimer.countdown(1778000060000L, 1778000000000L)
                        ),
                        MiFocusExpandedComponent.TextButtons(listOf(action))
                    )
                )
            )
        )
        val focusParam = bundle.getString("miui.focus.param").orEmpty()
        val actionBundle = bundle.getBundle("miui.focus.actions")

        listOf(
            "baseInfo",
            "chatInfo",
            "highlightInfo",
            "hintInfo",
            "progressInfo",
            "picInfo",
            "bgInfo",
            "coverInfo",
            "highlightInfoV3",
            "iconTextInfo",
            "multiProgressInfo",
            "animTextInfo",
            "textButton"
        ).forEach { token ->
            assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
        }
        assertNotNull(actionBundle?.getActionCompat("focus_test_open"))
    }

    @Test
    fun `summary helper presets emit progress countdown and terminal templates`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val now = 1778000000000L
        val cases = listOf(
            MiFocusIslandSpec.progressSummary(
                progressPercent = 64,
                content = "2/3",
                colorReach = "#2563EB",
                colorUnReach = "#334155"
            ) to listOf("progressTextInfo", "combinePicInfo"),
            MiFocusIslandSpec.countdownSummary(
                content = "开奖",
                deadlineAtMs = now + 60000L,
                nowMs = now,
                highlightColor = "#22C55E"
            ) to listOf("sameWidthDigitInfo", "timerInfo"),
            MiFocusIslandSpec.terminalSummary(
                title = "完成",
                content = "已同步",
                highlightColor = "#22C55E"
            ) to listOf("imageTextInfoRight", "picInfo")
        )

        cases.forEach { (island, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(island = island)
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
            }
        }
    }

    @Test
    fun `summary helper presets emit icon text fixed digit large picture and dual image templates`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cases = listOf(
            MiFocusIslandSpec.iconOnlySummary() to listOf("imageTextInfoLeft", "picInfo"),
            MiFocusIslandSpec.textOnlySummary(
                title = "专注中",
                content = "剩余1小时"
            ) to listOf("imageTextInfoLeft", "textInfo"),
            MiFocusIslandSpec.iconTextSummary(
                rightTitle = "24%",
                rightFrontTitle = "充电中",
                rightContent = "剩5分钟"
            ) to listOf("imageTextInfoLeft", "imageTextInfoRight"),
            MiFocusIslandSpec.fixedDigitSummary(
                digit = "12",
                content = "站"
            ) to listOf("imageTextInfoLeft", "fixedWidthDigitInfo"),
            MiFocusIslandSpec.largePictureSummary() to listOf("imageTextInfoLeft", "picInfo"),
            MiFocusIslandSpec.dualImageTextSummary(
                rightTitle = "34",
                rightContent = "秒",
                rightPic = MiFocusPictureRef.Expanded
            ) to listOf("imageTextInfoLeft", "imageTextInfoRight")
        )

        cases.forEach { (island, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(island = island)
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
            }
        }
    }

    @Test
    fun `summary helper presets emit terminal icon template`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bundle = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(
                island = MiFocusIslandSpec.terminalIconSummary(
                    title = "完成",
                    highlightColor = "#22C55E"
                )
            )
        )
        val focusParam = bundle.getString("miui.focus.param").orEmpty()
        assertTrue(focusParam.contains("imageTextInfoLeft"))
        assertTrue(focusParam.contains("imageTextInfoRight"))
    }

    @Test
    fun `small island image text template emits official type 6 payload`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bundle = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(
                island = MiFocusIslandSpec(
                    bigTemplates = listOf(
                        MiFocusIslandBigTemplate.ImageTextLeft(
                            type = 5,
                            pic = MiFocusIslandPic.officialCompact()
                        )
                    ),
                    smallTemplate = MiFocusIslandSmallTemplate.ImageTextRight(
                        text = MiFocusIslandText(title = "34", showHighlightColor = true),
                        pic = MiFocusIslandPic.officialCompact()
                    )
                )
            )
        )

        val root = JSONObject(bundle.getString("miui.focus.param").orEmpty())
        val smallIsland = root.getJSONObject("island").getJSONObject("smallIslandArea")
        val imageText = smallIsland.getJSONObject("imageTextInfoRight")

        assertFalse(smallIsland.has("combinePicInfo"))
        assertFalse(smallIsland.has("picInfo"))
        assertEquals(6, imageText.getInt("type"))
        assertEquals(4, imageText.getJSONObject("picInfo").getInt("type"))
        assertEquals("34", imageText.getJSONObject("textInfo").getString("title"))
        assertTrue(imageText.getJSONObject("textInfo").getBoolean("showHighlightColor"))
    }

    @Test
    fun `official summary presets cover icon text and terminal mappings`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cases = listOf(
            MiFocusOfficialTemplatePresets.summaryTemplate3IconText(
                rightTitle = "24%",
                rightFrontTitle = "充电中",
                rightContent = "剩5分钟"
            ) to listOf("imageTextInfoLeft", "imageTextInfoRight"),
            MiFocusOfficialTemplatePresets.summaryTemplate4TerminalIconText(
                title = "完成",
                highlightColor = "#22C55E"
            ) to listOf("imageTextInfoLeft", "imageTextInfoRight"),
            MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText(
                rightTitle = "34",
                rightContent = "秒"
            ) to listOf("imageTextInfoLeft", "imageTextInfoRight")
        )

        cases.forEach { (island, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(island = island)
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
            }
        }

        val summary9Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    island = MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText(
                        rightTitle = "34",
                        rightContent = "秒"
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        val summary9Small = summary9Root.getJSONObject("island").getJSONObject("smallIslandArea")
            .getJSONObject("imageTextInfoRight")
        assertEquals(6, summary9Small.getInt("type"))
    }

    @Test
    fun `official island summary helpers centralize terminal and dual image text semantics`() {
        val iconText = MiFocusIslandBigTemplate.officialIconTextRight(
            text = MiFocusIslandText(title = "24%"),
            pic = MiFocusIslandPic.officialStatic(pic = MiFocusPictureRef.Expanded)
        )
        val terminal = MiFocusIslandBigTemplate.officialTerminalTextRight(
            text = MiFocusIslandText(title = "完成")
        )
        val dualLeft = MiFocusIslandBigTemplate.officialDualImageTextLeft(
            pic = MiFocusPictureRef.Display
        )
        val dualRight = MiFocusIslandBigTemplate.officialDualImageTextRight(
            text = MiFocusIslandText(title = "34"),
            pic = MiFocusPictureRef.Expanded
        )
        val smallRight = MiFocusIslandSmallTemplate.officialImageTextRight(
            text = MiFocusIslandText(title = "34"),
            pic = MiFocusPictureRef.Display
        )

        assertEquals(2, iconText.type)
        assertEquals(3, terminal.type)
        assertEquals(5, dualLeft.type)
        assertEquals(4, dualLeft.pic?.type)
        assertEquals(6, dualRight.type)
        assertEquals(4, dualRight.pic?.type)
        assertEquals(6, smallRight.type)
        assertEquals(4, smallRight.pic.type)

        val summary4Right = MiFocusOfficialTemplatePresets.summaryTemplate4TerminalIconText(
            title = "完成"
        ).bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.ImageTextRight
        val summary3Right = MiFocusOfficialTemplatePresets.summaryTemplate3IconText(
            rightTitle = "24%",
            rightFrontTitle = "充电中",
            rightContent = "剩5分钟"
        ).bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.ImageTextRight
        assertNotNull(summary3Right)
        assertEquals(2, summary3Right.type)
        assertNotNull(summary4Right)
        assertEquals(3, summary4Right.type)

        val summary9 = MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText(
            rightTitle = "34",
            rightContent = "秒"
        )
        val summary9Left = summary9.bigTemplates.getOrNull(0) as? MiFocusIslandBigTemplate.ImageTextLeft
        val summary9Right = summary9.bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.ImageTextRight
        val summary9Small = summary9.smallTemplate as? MiFocusIslandSmallTemplate.ImageTextRight
        assertNotNull(summary9Left)
        assertNotNull(summary9Right)
        assertNotNull(summary9Small)
        assertEquals(5, summary9Left.type)
        assertEquals(4, summary9Left.pic?.type)
        assertEquals(6, summary9Right.type)
        assertEquals(null, summary9Right.pic)
        assertEquals(6, summary9Small.type)
        assertEquals(4, summary9Small.pic.type)

        val summary9WithRightPic = MiFocusOfficialTemplatePresets.summaryTemplate9DualImageText(
            rightTitle = "34",
            rightContent = "秒",
            rightPic = MiFocusPictureRef.Expanded
        )
        val summary9WithRightPicRight =
            summary9WithRightPic.bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.ImageTextRight
        assertNotNull(summary9WithRightPicRight)
        assertEquals(4, summary9WithRightPicRight.pic?.type)
    }

    @Test
    fun `official island pic helpers centralize static and compact picture semantics`() {
        val staticPic = MiFocusIslandPic.officialStatic(
            pic = MiFocusPictureRef.Display,
            contentDescription = "app"
        )
        val compactPic = MiFocusIslandPic.officialCompact(
            pic = MiFocusPictureRef.Expanded,
            contentDescription = "countdown"
        )

        assertEquals(1, staticPic.type)
        assertEquals(MiFocusPictureRef.Display, staticPic.pic)
        assertEquals("app", staticPic.contentDescription)
        assertEquals(4, compactPic.type)
        assertEquals(MiFocusPictureRef.Expanded, compactPic.pic)
        assertEquals("countdown", compactPic.contentDescription)

        val dualRight = MiFocusIslandBigTemplate.officialDualImageTextRight(
            text = MiFocusIslandText(title = "34"),
            pic = MiFocusPictureRef.Expanded
        )
        val smallRight = MiFocusIslandSmallTemplate.officialImageTextRight(
            text = MiFocusIslandText(title = "12"),
            pic = MiFocusPictureRef.Display
        )

        assertEquals(4, dualRight.pic?.type)
        assertEquals(4, smallRight.pic.type)
    }

    @Test
    fun `official summary helpers centralize remaining static and progress template semantics`() {
        val left = MiFocusIslandBigTemplate.officialImageTextLeft(
            pic = MiFocusPictureRef.Display
        )
        val picture = MiFocusIslandBigTemplate.officialPicture(
            pic = MiFocusPictureRef.Expanded
        )
        val smallPicture = MiFocusIslandSmallTemplate.officialPicture(
            pic = MiFocusPictureRef.Display
        )
        val smallProgress = MiFocusIslandSmallTemplate.officialProgressPicture(
            progressPercent = 64,
            colorReach = "#2563EB",
            colorUnReach = "#334155",
            pic = MiFocusPictureRef.Display,
            smallPic = MiFocusPictureRef.Expanded
        )

        assertEquals(1, left.type)
        assertEquals(1, left.pic?.type)
        assertEquals(MiFocusPictureRef.Display, left.pic?.pic)
        assertEquals(1, picture.pic.type)
        assertEquals(MiFocusPictureRef.Expanded, picture.pic.pic)
        assertEquals(1, smallPicture.pic.type)
        assertEquals(1, smallProgress.pic.type)
        assertEquals(64, smallProgress.progress.progressPercent)
        assertEquals("#2563EB", smallProgress.progress.colorReach)
        assertEquals("#334155", smallProgress.progress.colorUnReach)
        assertEquals(1, smallProgress.smallPic?.type)
        assertEquals(MiFocusPictureRef.Expanded, smallProgress.smallPic?.pic)

        val summary1 = MiFocusOfficialTemplatePresets.summaryTemplate1IconOnly()
        val summary2 = MiFocusOfficialTemplatePresets.summaryTemplate2Text(
            title = "专注中",
            content = "剩余1小时"
        )
        val summary3 = MiFocusOfficialTemplatePresets.summaryTemplate3IconText(
            rightTitle = "24%",
            rightFrontTitle = "充电中",
            rightContent = "剩5分钟"
        )
        val summary4 = MiFocusOfficialTemplatePresets.summaryTemplate4TerminalIconText(
            title = "完成"
        )
        val summary8 = MiFocusOfficialTemplatePresets.summaryTemplate8LargePicture()
        val small2 = MiFocusOfficialTemplatePresets.smallTemplate2ProgressIcon(
            progressPercent = 72,
            colorReach = "#2563EB",
            colorUnReach = "#334155",
            smallPic = MiFocusPictureRef.Expanded
        )

        val summary1Left = summary1.bigTemplates.firstOrNull() as? MiFocusIslandBigTemplate.ImageTextLeft
        val summary2Text = summary2.bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.Text
        val summary3Left = summary3.bigTemplates.firstOrNull() as? MiFocusIslandBigTemplate.ImageTextLeft
        val summary4Left = summary4.bigTemplates.firstOrNull() as? MiFocusIslandBigTemplate.ImageTextLeft
        val summary8Right = summary8.bigTemplates.getOrNull(1) as? MiFocusIslandBigTemplate.Picture

        assertNotNull(summary1Left)
        assertEquals(1, summary1Left.type)
        assertEquals(1, summary1Left.pic?.type)
        assertNotNull(summary2Text)
        assertEquals("专注中", summary2Text.text.title)
        assertNotNull(summary3Left)
        assertEquals(1, summary3Left.type)
        assertEquals(1, summary3Left.pic?.type)
        assertNotNull(summary4Left)
        assertEquals(1, summary4Left.type)
        assertNotNull(summary8Right)
        assertEquals(1, summary8Right.pic.type)
        assertEquals(1, small2.pic.type)
        assertEquals(1, small2.smallPic?.type)
    }

    @Test
    fun `official template catalog exposes local route index for summary small island and expanded templates`() {
        assertEquals(9, MiFocusOfficialTemplateCatalog.summaryTemplates.size)
        assertEquals(3, MiFocusOfficialTemplateCatalog.smallIslandTemplates.size)
        assertEquals(23, MiFocusOfficialTemplateCatalog.expandedTemplates.size)

        val summary3 = MiFocusOfficialTemplateCatalog.findSummaryTemplate(3)
        val small3 = MiFocusOfficialTemplateCatalog.findSmallIslandTemplate(3)
        val expanded14_2 = MiFocusOfficialTemplateCatalog.findExpandedTemplate("14-2")
        val expandedRoute14_2 = MiFocusOfficialTemplateCatalog.findRoute(
            family = MiFocusOfficialTemplateFamily.EXPANDED,
            code = "14-2"
        )
        val summaryRoute4 = MiFocusOfficialTemplateCatalog.findRoute(
            family = MiFocusOfficialTemplateFamily.SUMMARY,
            code = "4"
        )

        assertEquals("MiFocusOfficialTemplatePresets.summaryTemplate3IconText", summary3?.recommendedEntry)
        assertEquals("MiFocusOfficialTemplatePresets.smallTemplate3IconText", small3?.recommendedEntry)
        assertEquals("MiFocusOfficialTemplatePresets.expandedTemplate14_2NewImageTextCountdownPicture", expanded14_2?.recommendedEntry)
        assertNotNull(expandedRoute14_2)
        assertEquals(MiFocusOfficialOsSupport.OS3_ONLY, expandedRoute14_2.osSupport)
        assertTrue(
            expandedRoute14_2.primaryHelpers.contains("MiFocusExpandedComponent.officialPictureCountdown")
        )
        assertNotNull(summaryRoute4)
        assertTrue(
            summaryRoute4.primaryHelpers.contains("MiFocusIslandSpec.terminalIconSummary")
        )
        assertEquals(null, MiFocusOfficialTemplateCatalog.findExpandedTemplate("999"))
    }

    @Test
    fun `expanded helper presets emit official button components`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val secondaryAction = MiFocusNotificationAction(
            key = "focus_test_ignore",
            title = "Ignore",
            pendingIntent = openPendingIntent
        ).asSecondaryTextButton()
        val primaryAction = MiFocusNotificationAction(
            key = "focus_test_done",
            title = "Done",
            pendingIntent = openPendingIntent
        ).asPrimaryTextButton(backgroundColor = "#22C55E")
        val capsuleAction = MiFocusNotificationAction(
            key = "focus_test_pay",
            title = "Pay",
            pendingIntent = openPendingIntent
        ).asHighlightCapsuleButton(backgroundColor = "#2563EB")

        val dualBundle = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(
                expanded = MiFocusExpandedSpec.dualTextButtons(
                    text = MiFocusExpandedText(title = "Reminder", content = "19:30 | Item"),
                    secondaryAction = secondaryAction,
                    primaryAction = primaryAction,
                    picture = MiFocusPictureRef.Expanded
                )
            )
        )
        val dualFocusParam = dualBundle.getString("miui.focus.param").orEmpty()
        assertTrue(dualFocusParam.contains("textButton"), "Missing textButton in $dualFocusParam")

        val capsuleBundle = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(
                expanded = MiFocusExpandedSpec.highlightCapsuleAction(
                    text = MiFocusExpandedText(title = "Offer"),
                    primaryText = "4899元",
                    secondaryText = "4999元",
                    label = "限时优惠",
                    action = capsuleAction
                )
            )
        )
        val capsuleFocusParam = capsuleBundle.getString("miui.focus.param").orEmpty()
        assertTrue(
            capsuleFocusParam.contains("highlightInfoV3"),
            "Missing highlightInfoV3 in $capsuleFocusParam"
        )

        val dualComponents = MiFocusExpandedSpec.dualTextButtons(
            text = MiFocusExpandedText(title = "Reminder", content = "19:30 | Item"),
            secondaryAction = secondaryAction,
            primaryAction = primaryAction,
            picture = MiFocusPictureRef.Expanded
        ).components
        assertEquals(2, (dualComponents.getOrNull(0) as? MiFocusExpandedComponent.Base)?.type)
        assertEquals(1, (dualComponents.getOrNull(1) as? MiFocusExpandedComponent.OfficialPicture)?.type)
        assertEquals(2, (dualComponents.getOrNull(2) as? MiFocusExpandedComponent.OfficialTextButtons)?.actions?.size)

        val hintComponents = MiFocusExpandedSpec.hintAction(
            text = MiFocusExpandedText(title = "待处理事项"),
            action = capsuleAction
        ).components
        assertEquals(2, (hintComponents.firstOrNull() as? MiFocusExpandedComponent.OfficialHint)?.type)

        val capsuleComponents = MiFocusExpandedSpec.highlightCapsuleAction(
            text = MiFocusExpandedText(title = "Offer"),
            primaryText = "4899元",
            action = capsuleAction
        ).components
        val baseComponents = MiFocusExpandedSpec.base(
            title = "Title",
            content = "Content"
        ).components
        assertEquals(
            "4899元",
            (capsuleComponents.firstOrNull() as? MiFocusExpandedComponent.OfficialHighlightCapsule)?.primaryText
        )
        assertEquals(2, (baseComponents.firstOrNull() as? MiFocusExpandedComponent.Base)?.type)
    }

    @Test
    fun `official button helpers centralize public button semantics`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val textAction = MiFocusNotificationAction(
            key = "focus_test_text_button",
            title = "打开",
            pendingIntent = openPendingIntent
        ).asPrimaryTextButton(backgroundColor = "#22C55E")
        val capsuleAction = MiFocusNotificationAction(
            key = "focus_test_capsule_button",
            title = "处理",
            pendingIntent = openPendingIntent
        ).asHighlightCapsuleButton(backgroundColor = "#2563EB")

        val hintSecondary = MiFocusExpandedComponent.officialHintSecondary(
            payload = MiFocusExpandedComponent.OfficialHintPayload(
                text = MiFocusExpandedText(title = "4排6座"),
                type = 2,
                action = capsuleAction
            )
        )
        val hintPrimary = MiFocusExpandedComponent.officialHintPrimary(
            payload = MiFocusExpandedComponent.OfficialHintPayload(
                text = MiFocusExpandedText(title = "回复"),
                type = 1,
                action = capsuleAction
            )
        )
        val actions = MiFocusExpandedComponent.officialActions(
            MiFocusExpandedComponent.OfficialActionsPayload(listOf(textAction))
        )
        val textButtons = MiFocusExpandedComponent.officialTextButtons(
            MiFocusExpandedComponent.OfficialTextButtonsPayload(listOf(textAction))
        )
        val capsule = MiFocusExpandedComponent.officialHighlightCapsule(
            payload = MiFocusExpandedComponent.OfficialHighlightCapsulePayload(
                text = MiFocusExpandedText(title = "限时优惠"),
                primaryText = "4899元",
                action = capsuleAction
            )
        )

        assertEquals(2, hintSecondary.type)
        assertEquals(1, hintPrimary.type)
        assertEquals(1, actions.actions.size)
        assertEquals(1, textButtons.actions.size)
        assertEquals("4899元", capsule.primaryText)

        val template8Hint = MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3(
            chatText = MiFocusExpandedText(title = "小米同学"),
            hintPayload = MiFocusExpandedComponent.OfficialHintPayload(
                text = MiFocusExpandedText(title = "回复"),
                type = 1,
                action = capsuleAction
            )
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialHint
        val template12Actions = MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions(
            chatText = MiFocusExpandedText(title = "新的群聊消息"),
            actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(
                actions = listOf(textAction)
            )
        ).components.getOrNull(1) as? MiFocusExpandedComponent.OfficialActions
        val template17Buttons = MiFocusOfficialTemplatePresets.expandedTemplate17NewImageTextTextButtons(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(title = "取药提醒")
            ),
            textButtonsPayload = MiFocusExpandedComponent.OfficialTextButtonsPayload(
                actions = listOf(
                    textAction.copy(key = "focus_test_secondary"),
                    textAction.copy(key = "focus_test_primary")
                )
            )
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialTextButtons
        val template16Capsule = MiFocusOfficialTemplatePresets.expandedTemplate16NewImageTextHighlight(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(title = "取药提醒")
            ),
            highlightPayload = MiFocusExpandedComponent.OfficialHighlightCapsulePayload(
                text = MiFocusExpandedText(title = "取药提醒"),
                primaryText = "立即处理",
                action = capsuleAction
            )
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialHighlightCapsule

        assertNotNull(template8Hint)
        assertEquals(1, template8Hint.type)
        assertNotNull(template12Actions)
        assertEquals(1, template12Actions.payload.actions.size)
        assertNotNull(template17Buttons)
        assertEquals(2, template17Buttons.payload.actions.size)
        assertNotNull(template16Capsule)
        assertEquals("立即处理", template16Capsule.primaryText)
    }

    @Test
    fun `expanded actions component emits official actions payload`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val progressAction = MiFocusNotificationAction(
            key = "focus_test_download",
            title = "下载中",
            pendingIntent = openPendingIntent
        ).asProgressButton(
            progressPercent = 40,
            colorReach = "#FF8514"
        )

        val root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusExpandedSpec(
                        components = listOf(
                            MiFocusExpandedComponent.Base(
                                text = MiFocusExpandedText(title = "资源下载")
                            ),
                            MiFocusExpandedComponent.button1Actions(
                                actions = listOf(progressAction)
                            )
                        )
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        val actions = root.getJSONObject("param_v2").getJSONArray("actions")
        val actionInfo = actions.getJSONObject(0)
        val progressInfo = actionInfo.getJSONObject("progressInfo")

        assertEquals(1, actions.length())
        assertEquals(1, actionInfo.getInt("type"))
        assertTrue(actionInfo.getString("action").startsWith("miui.focus.action_"))
        assertEquals(40, progressInfo.getInt("progress"))
        assertEquals("#FF8514", progressInfo.getString("colorProgress"))
    }

    @Test
    fun `official expanded presets cover template 1 to 7 families`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val nodePictures = listOf(
            MiFocusPictureAsset(
                ref = MiFocusPictureRef("node_forward"),
                source = MiFocusPictureSource.Resource(R.drawable.ic_kei_logo_island)
            ),
            MiFocusPictureAsset(
                ref = MiFocusPictureRef("node_middle"),
                source = MiFocusPictureSource.Resource(R.drawable.ic_kei_logo_island)
            ),
            MiFocusPictureAsset(
                ref = MiFocusPictureRef("node_middle_unselected"),
                source = MiFocusPictureSource.Resource(R.drawable.ic_kei_logo_island)
            ),
            MiFocusPictureAsset(
                ref = MiFocusPictureRef("node_end"),
                source = MiFocusPictureSource.Resource(R.drawable.ic_kei_logo_island)
            ),
            MiFocusPictureAsset(
                ref = MiFocusPictureRef("node_end_unselected"),
                source = MiFocusPictureSource.Resource(R.drawable.ic_kei_logo_island)
            )
        )

        val cases = listOf(
            MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture(
                text = MiFocusExpandedText(
                    title = "当前天气",
                    content = "多云 28°C"
                )
            ) to listOf("baseInfo", "picInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate2BaseAppIcon(
                text = MiFocusExpandedText(
                    title = "支付成功",
                    content = "订单已完成"
                )
            ) to listOf("baseInfo", "picInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate3ChatMiddlePicture(
                text = MiFocusExpandedText(
                    title = "小米同学",
                    content = "发来一条消息"
                )
            ) to listOf("chatInfo", "picInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate4BaseAppIconProgressNodes(
                text = MiFocusExpandedText(
                    title = "骑手正在赶来",
                    content = "预计 8 分钟送达"
                ),
                progressPercent = 52,
                picForward = MiFocusPictureRef("node_forward"),
                picMiddle = MiFocusPictureRef("node_middle"),
                picMiddleUnselected = MiFocusPictureRef("node_middle_unselected"),
                picEnd = MiFocusPictureRef("node_end"),
                picEndUnselected = MiFocusPictureRef("node_end_unselected"),
                colorReach = "#2563EB"
            ) to listOf("baseInfo", "picInfo", "progressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate5BaseAppIconProgress(
                text = MiFocusExpandedText(
                    title = "进程状态",
                    content = "正在处理"
                ),
                progressPercent = 64,
                colorReach = "#2563EB"
            ) to listOf("baseInfo", "picInfo", "progressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate6BaseAppIconProgress(
                text = MiFocusExpandedText(
                    title = "下载中",
                    content = "第 3 个文件"
                ),
                progressPercent = 71,
                colorReach = "#22C55E"
            ) to listOf("baseInfo", "picInfo", "progressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate7ChatAppIconProgress(
                text = MiFocusExpandedText(
                    title = "群组同步",
                    content = "资源拉取中"
                ),
                progressPercent = 45,
                colorReach = "#F97316"
            ) to listOf("chatInfo", "picInfo", "progressInfo")
        )

        cases.forEachIndexed { index, (expanded, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = expanded,
                    extraPictures = nodePictures
                )
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Case $index missing $token in $focusParam")
            }
        }

        val template1Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture(
                        text = MiFocusExpandedText(
                            title = "当前天气",
                            content = "多云 28°C"
                        )
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        assertEquals(1, template1Root.getJSONObject("param_v2").getJSONObject("baseInfo").getInt("type"))
        assertEquals(3, template1Root.getJSONObject("param_v2").getJSONObject("picInfo").getInt("type"))

        val template3Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate3ChatMiddlePicture(
                        text = MiFocusExpandedText(
                            title = "小米同学",
                            content = "发来一条消息"
                        )
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        assertEquals(2, template3Root.getJSONObject("param_v2").getJSONObject("picInfo").getInt("type"))

        val template4Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate4BaseAppIconProgressNodes(
                        text = MiFocusExpandedText(
                            title = "骑手正在赶来",
                            content = "预计 8 分钟送达"
                        ),
                        progressPercent = 52,
                        picForward = MiFocusPictureRef("node_forward"),
                        picMiddle = MiFocusPictureRef("node_middle"),
                        picMiddleUnselected = MiFocusPictureRef("node_middle_unselected"),
                        picEnd = MiFocusPictureRef("node_end"),
                        picEndUnselected = MiFocusPictureRef("node_end_unselected"),
                        colorReach = "#2563EB"
                    ),
                    extraPictures = nodePictures
                )
            ).getString("miui.focus.param").orEmpty()
        )
        val template4Progress = template4Root.getJSONObject("param_v2").getJSONObject("progressInfo")
        assertEquals(1, template4Root.getJSONObject("param_v2").getJSONObject("picInfo").getInt("type"))
        assertTrue(template4Progress.has("picForward"))
        assertTrue(template4Progress.has("picMiddle"))
        assertTrue(template4Progress.has("picMiddleUnselected"))
        assertTrue(template4Progress.has("picEnd"))
        assertTrue(template4Progress.has("picEndUnselected"))

        val template5Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate5BaseAppIconProgress(
                        text = MiFocusExpandedText(
                            title = "进程状态",
                            content = "正在处理"
                        ),
                        progressPercent = 64,
                        colorReach = "#2563EB"
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        assertEquals(1, template5Root.getJSONObject("param_v2").getJSONObject("baseInfo").getInt("type"))
    }

    @Test
    fun `official expanded presets cover template 8 to 13 families`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val button3Action = MiFocusNotificationAction(
            key = "focus_test_reply",
            title = "回复",
            pendingIntent = openPendingIntent
        ).asHighlightCapsuleButton(backgroundColor = "#2563EB")
        val textAction = MiFocusNotificationAction(
            key = "focus_test_open_chat",
            title = "打开",
            pendingIntent = openPendingIntent
        ).asPrimaryTextButton(backgroundColor = "#22C55E")

        val cases = listOf(
            MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3(
                chatText = MiFocusExpandedText(
                    title = "小米同学",
                    content = "今晚一起看电影吗"
                ),
                buttonText = MiFocusExpandedText(
                    title = "回复",
                    content = "新消息"
                ),
                action = button3Action
            ) to listOf("chatInfo", "picInfo", "hintInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2(
                baseText = MiFocusExpandedText(
                    title = "待处理事项",
                    content = "19:30 开场"
                ),
                buttonText = MiFocusExpandedText(
                    title = "4排6座",
                    content = "座位"
                ),
                action = button3Action
            ) to listOf("baseInfo", "picInfo", "hintInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate10BaseButton3(
                baseText = MiFocusExpandedText(
                    title = "立即查看",
                    content = "活动即将开始"
                ),
                buttonText = MiFocusExpandedText(
                    title = "查看",
                    content = "活动"
                ),
                action = button3Action
            ) to listOf("baseInfo", "picInfo", "hintInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate11HighlightButton2(
                highlightText = MiFocusExpandedText(
                    title = "12",
                    content = "分钟后开始"
                ),
                buttonText = MiFocusExpandedText(
                    title = "9:20",
                    content = "开场"
                ),
                action = button3Action
            ) to listOf("highlightInfo", "picInfo", "hintInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions(
                chatText = MiFocusExpandedText(
                    title = "新的群聊消息",
                    content = "点击继续查看"
                ),
                actions = listOf(textAction)
            ) to listOf("chatInfo", "\"actions\""),
            MiFocusOfficialTemplatePresets.expandedTemplate13HighlightActions(
                highlightText = MiFocusExpandedText(
                    title = "¥4899",
                    content = "限时优惠"
                ),
                actions = listOf(textAction)
            ) to listOf("highlightInfo", "\"actions\"")
        )

        cases.forEachIndexed { index, (expanded, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(expanded = expanded)
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Case $index missing $token in $focusParam")
            }
        }

        val template8Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3(
                        chatText = MiFocusExpandedText(
                            title = "小米同学",
                            content = "今晚一起看电影吗"
                        ),
                        buttonText = MiFocusExpandedText(
                            title = "回复",
                            content = "新消息"
                        ),
                        action = button3Action
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        assertEquals(1, template8Root.getJSONObject("param_v2").getJSONObject("hintInfo").getInt("type"))

        val template9Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2(
                        baseText = MiFocusExpandedText(
                            title = "待处理事项",
                            content = "19:30 开场"
                        ),
                        buttonText = MiFocusExpandedText(
                            title = "4排6座",
                            content = "座位"
                        ),
                        action = button3Action
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        )
        assertEquals(2, template9Root.getJSONObject("param_v2").getJSONObject("hintInfo").getInt("type"))

        val template12Actions = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(
                    expanded = MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions(
                        chatText = MiFocusExpandedText(
                            title = "新的群聊消息",
                            content = "点击继续查看"
                        ),
                        actions = listOf(textAction)
                    )
                )
            ).getString("miui.focus.param").orEmpty()
        ).getJSONObject("param_v2").getJSONArray("actions")
        assertEquals(2, template12Actions.getJSONObject(0).getInt("type"))
    }

    @Test
    fun `official expanded presets cover template 15 to 22 families`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val secondaryAction = MiFocusNotificationAction(
            key = "focus_test_later",
            title = "Later",
            pendingIntent = openPendingIntent
        ).asSecondaryTextButton()
        val primaryAction = MiFocusNotificationAction(
            key = "focus_test_open_now",
            title = "Open",
            pendingIntent = openPendingIntent
        ).asPrimaryTextButton(backgroundColor = "#2563EB")
        val capsuleAction = MiFocusNotificationAction(
            key = "focus_test_buy_now",
            title = "Buy",
            pendingIntent = openPendingIntent
        ).asHighlightCapsuleButton(backgroundColor = "#E25B6A")
        val newImageText = MiFocusExpandedText(
            title = "取药提醒",
            content = "阿莫西林 1 粒"
        )

        val cases = listOf(
            MiFocusOfficialTemplatePresets.expandedTemplate15NewImageTextActions(
                text = newImageText,
                actions = listOf(primaryAction)
            ) to listOf("iconTextInfo", "\"actions\""),
            MiFocusOfficialTemplatePresets.expandedTemplate16NewImageTextHighlight(
                text = newImageText,
                primaryText = "立即处理",
                action = capsuleAction
            ) to listOf("iconTextInfo", "picInfo", "highlightInfoV3"),
            MiFocusOfficialTemplatePresets.expandedTemplate17NewImageTextTextButtons(
                text = newImageText,
                secondaryAction = secondaryAction,
                primaryAction = primaryAction
            ) to listOf("iconTextInfo", "picInfo", "textButton"),
            MiFocusOfficialTemplatePresets.expandedTemplate18CoverHighlight(
                text = MiFocusExpandedText(
                    title = "电影票已开售",
                    content = "两人同行票",
                    subContent = "19:30 场次"
                ),
                primaryText = "去购票",
                action = capsuleAction
            ) to listOf("coverInfo", "picInfo", "highlightInfoV3"),
            MiFocusOfficialTemplatePresets.expandedTemplate19PictureMultiProgress(
                text = MiFocusExpandedText(
                    title = "套餐余量",
                    content = "剩余 42GB"
                ),
                progressPercent = 64,
                progressTitle = "本月进度",
                points = 3
            ) to listOf("baseInfo", "picInfo", "multiProgressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate20ChatProgress(
                text = MiFocusExpandedText(
                    title = "资源下载中",
                    content = "第 2 章"
                ),
                progressPercent = 71,
                colorReach = "#22C55E"
            ) to listOf("chatInfo", "progressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate21NewImageTextMultiProgress(
                text = newImageText,
                progressPercent = 45,
                progressTitle = "阶段进度",
                points = 2
            ) to listOf("iconTextInfo", "picInfo", "multiProgressInfo"),
            MiFocusOfficialTemplatePresets.expandedTemplate22NewImageTextMultiProgress(
                text = newImageText,
                progressPercent = 45,
                progressTitle = "阶段进度",
                points = 2
            ) to listOf("iconTextInfo", "picInfo", "multiProgressInfo")
        )

        cases.forEach { (expanded, tokens) ->
            val bundle = MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(expanded = expanded)
            )
            val focusParam = bundle.getString("miui.focus.param").orEmpty()
            tokens.forEach { token ->
                assertTrue(focusParam.contains(token), "Missing $token in $focusParam")
            }
        }
    }

    @Test
    fun `official expanded presets cover template 14 families`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val template14_1 = MiFocusOfficialTemplatePresets.expandedTemplate14_1NewImageText(
            text = MiFocusExpandedText(
                title = "快递已到站",
                content = "请及时查收"
            )
        )
        val template14_1Param = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(expanded = template14_1)
        ).getString("miui.focus.param").orEmpty()
        assertTrue(
            template14_1Param.contains("iconTextInfo"),
            "Missing iconTextInfo in $template14_1Param"
        )

        val template14_2 = MiFocusOfficialTemplatePresets.expandedTemplate14_2NewImageTextCountdownPicture(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(
                    title = "红灯倒计时",
                    content = "路口请稍候"
                )
            ),
            countdownPayload = MiFocusExpandedComponent.OfficialPictureType5Payload(
                title = "12",
                colorTitle = "#CC234567"
            )
        )
        val countdownPicture =
            template14_2.components.getOrNull(1) as? MiFocusExpandedComponent.OfficialPicture
        assertNotNull(countdownPicture)
        assertEquals(5, countdownPicture.type)
        val countdownPayload = requireNotNull(countdownPicture.type5Payload)
        assertEquals("12", countdownPayload.title)
        assertEquals("#CC234567", countdownPayload.colorTitle)
        assertEquals("12", countdownPicture.title)
        assertEquals("#CC234567", countdownPicture.colorTitle)
        val template14_2Root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(expanded = template14_2)
            ).getString("miui.focus.param").orEmpty()
        )
        val template14_2Payload = template14_2Root.getJSONObject("param_v2")
        assertTrue(template14_2Payload.has("iconTextInfo"))
        val picInfo = template14_2Payload.getJSONObject("picInfo")
        assertEquals(5, picInfo.getInt("type"))
        assertEquals("12", picInfo.getString("title"))
        assertEquals("#CC234567", picInfo.getString("colorTitle"))
    }

    @Test
    fun `official picture helpers centralize public picture semantics`() {
        val appIcon = MiFocusExpandedComponent.officialPictureAppIcon()
        val middle = MiFocusExpandedComponent.officialPictureMiddle()
        val large = MiFocusExpandedComponent.officialPictureLarge()
        val countdown = MiFocusExpandedComponent.officialPictureCountdown(
            payload = MiFocusExpandedComponent.OfficialPictureType5Payload(
                title = "12",
                colorTitle = "#CC234567"
            )
        )

        assertEquals(1, appIcon.type)
        assertEquals(2, middle.type)
        assertEquals(3, large.type)
        assertEquals(5, countdown.type)
        val countdownType5Payload = requireNotNull(countdown.type5Payload)
        assertEquals("12", countdownType5Payload.title)
        assertEquals("12", countdown.title)

        val template1Picture = MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture(
            text = MiFocusExpandedText(title = "当前天气")
        ).components.getOrNull(1) as? MiFocusExpandedComponent.OfficialPicture
        assertNotNull(template1Picture)
        assertEquals(3, template1Picture.type)

        val template8Picture = MiFocusOfficialTemplatePresets.expandedTemplate8ChatButton3(
            chatText = MiFocusExpandedText(title = "小米同学"),
            buttonText = MiFocusExpandedText(title = "回复"),
            action = MiFocusNotificationAction(
                key = "focus_test_reply_centralized",
                title = "回复",
                pendingIntent = buildOpenPendingIntent(
                    ApplicationProvider.getApplicationContext()
                )
            ).asHighlightCapsuleButton()
        ).components.getOrNull(1) as? MiFocusExpandedComponent.OfficialPicture
        assertNotNull(template8Picture)
        assertEquals(1, template8Picture.type)

        val template14Picture = MiFocusOfficialTemplatePresets
            .expandedTemplate14_2NewImageTextCountdownPicture(
                text = MiFocusExpandedText(title = "红灯倒计时"),
                countdownTitle = "12",
                countdownColor = "#CC234567"
            )
            .components.getOrNull(1) as? MiFocusExpandedComponent.OfficialPicture
        assertNotNull(template14Picture)
        assertEquals(5, template14Picture.type)
        val template14Payload = requireNotNull(template14Picture.type5Payload)
        assertEquals("#CC234567", template14Payload.colorTitle)
        assertEquals("12", template14Picture.title)

        assertFailsWith<IllegalArgumentException> {
            MiFocusExpandedComponent.OfficialPicture(
                type = 3,
                type5Payload = MiFocusExpandedComponent.OfficialPictureType5Payload(
                    title = "12",
                    colorTitle = "#CC234567"
                )
            )
        }
    }

    @Test
    fun `official progress helpers centralize public progress semantics`() {
        val nodes = MiFocusExpandedComponent.officialProgressNodes(
            progressPercent = 40,
            picForward = MiFocusPictureRef("node_forward"),
            picMiddle = MiFocusPictureRef("node_middle"),
            picMiddleUnselected = MiFocusPictureRef("node_middle_unselected"),
            picEnd = MiFocusPictureRef("node_end"),
            picEndUnselected = MiFocusPictureRef("node_end_unselected"),
            colorReach = "#FF8514",
            colorEnd = "#FF8514"
        )
        val bar = MiFocusExpandedComponent.officialProgressBar(
            progressPercent = 64,
            colorReach = "#2563EB",
            colorEnd = "#2563EB"
        )

        assertNotNull(nodes.picForward)
        assertNotNull(nodes.picMiddle)
        assertNotNull(nodes.picMiddleUnselected)
        assertNotNull(nodes.picEnd)
        assertNotNull(nodes.picEndUnselected)
        assertEquals(40, nodes.progress.progressPercent)

        assertEquals(64, bar.progress.progressPercent)
        assertEquals(null, bar.picForward)
        assertEquals(null, bar.picMiddle)
        assertEquals(null, bar.picMiddleUnselected)
        assertEquals(null, bar.picEnd)
        assertEquals(null, bar.picEndUnselected)

        val template4Progress = MiFocusOfficialTemplatePresets.expandedTemplate4BaseAppIconProgressNodes(
            text = MiFocusExpandedText(title = "骑手正在赶来"),
            progressPercent = 52,
            picForward = MiFocusPictureRef("node_forward"),
            picMiddle = MiFocusPictureRef("node_middle"),
            picMiddleUnselected = MiFocusPictureRef("node_middle_unselected"),
            picEnd = MiFocusPictureRef("node_end"),
            picEndUnselected = MiFocusPictureRef("node_end_unselected")
        ).components.getOrNull(2) as? MiFocusExpandedComponent.Progress
        assertNotNull(template4Progress)
        assertNotNull(template4Progress.picForward)
        assertNotNull(template4Progress.picEndUnselected)

        val template20Progress = MiFocusOfficialTemplatePresets.expandedTemplate20ChatProgress(
            text = MiFocusExpandedText(title = "资源下载中"),
            progressPercent = 71,
            colorReach = "#22C55E"
        ).components.getOrNull(1) as? MiFocusExpandedComponent.Progress
        assertNotNull(template20Progress)
        assertEquals(null, template20Progress.picForward)
        assertEquals(null, template20Progress.picEnd)
    }

    @Test
    fun `official expanded content helpers centralize public chat highlight new image cover and multi progress semantics`() {
        val chat = MiFocusExpandedComponent.officialChat(
            text = MiFocusExpandedText(title = "新的群聊消息"),
            appIconPkg = "os.kei.demo"
        )
        val highlight = MiFocusExpandedComponent.officialHighlight(
            text = MiFocusExpandedText(title = "12", content = "分钟后开始"),
            picFunction = MiFocusPictureRef.Expanded
        )
        val newImagePayload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
            text = MiFocusExpandedText(title = "取药提醒"),
            icon = MiFocusAnimIcon(src = MiFocusPictureRef.Expanded)
        )
        val newImageText = MiFocusExpandedComponent.officialNewImageText(
            payload = newImagePayload
        )
        val cover = MiFocusExpandedComponent.officialCover(
            text = MiFocusExpandedText(title = "电影票已开售"),
            pic = MiFocusPictureRef.Expanded
        )
        val multiProgress = MiFocusExpandedComponent.officialMultiProgress(
            progressPercent = 64,
            color = "#2563EB",
            points = 3,
            text = MiFocusExpandedText(title = "本月进度")
        )

        assertEquals("os.kei.demo", chat.appIconPkg)
        assertEquals(MiFocusPictureRef.Expanded, highlight.picFunction)
        assertEquals("取药提醒", newImageText.payload.text.title)
        assertEquals(MiFocusPictureRef.Expanded, newImageText.payload.icon?.src)
        assertEquals(MiFocusPictureRef.Expanded, cover.pic)
        assertEquals(64, multiProgress.progressPercent)
        assertEquals(3, multiProgress.points)
        assertEquals("本月进度", multiProgress.text?.title)

        val template3Chat = MiFocusOfficialTemplatePresets.expandedTemplate3ChatMiddlePicture(
            text = MiFocusExpandedText(title = "小米同学")
        ).components.firstOrNull() as? MiFocusExpandedComponent.Chat
        val template11Highlight = MiFocusOfficialTemplatePresets.expandedTemplate11HighlightButton2(
            highlightText = MiFocusExpandedText(title = "12"),
            buttonText = MiFocusExpandedText(title = "9:20"),
            action = MiFocusNotificationAction(
                key = "focus_test_template11",
                title = "打开",
                pendingIntent = buildOpenPendingIntent(
                    ApplicationProvider.getApplicationContext()
                )
            ).asHighlightCapsuleButton()
        ).components.firstOrNull() as? MiFocusExpandedComponent.Highlight
        val template14NewImage = MiFocusOfficialTemplatePresets.expandedTemplate14_1NewImageText(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(title = "快递已到站"),
                icon = MiFocusAnimIcon(src = MiFocusPictureRef.Expanded)
            )
        ).components.firstOrNull() as? MiFocusExpandedComponent.OfficialNewImageText
        val template18Cover = MiFocusOfficialTemplatePresets.expandedTemplate18CoverHighlight(
            text = MiFocusExpandedText(title = "电影票已开售"),
            primaryText = "去购票",
            action = MiFocusNotificationAction(
                key = "focus_test_template18",
                title = "购票",
                pendingIntent = buildOpenPendingIntent(
                    ApplicationProvider.getApplicationContext()
                )
            ).asHighlightCapsuleButton()
        ).components.firstOrNull() as? MiFocusExpandedComponent.Cover
        val template19MultiProgress = MiFocusOfficialTemplatePresets.expandedTemplate19PictureMultiProgress(
            text = MiFocusExpandedText(title = "套餐余量"),
            progressPercent = 64,
            progressTitle = "本月进度"
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialMultiProgress

        assertNotNull(template3Chat)
        assertNotNull(template11Highlight)
        assertNotNull(template14NewImage)
        assertNotNull(template18Cover)
        assertNotNull(template19MultiProgress)
        assertEquals("快递已到站", template14NewImage.payload.text.title)
        assertEquals(MiFocusPictureRef.Expanded, template14NewImage.payload.icon?.src)
        assertEquals(MiFocusPictureRef.Expanded, template18Cover.pic)
        assertEquals("本月进度", template19MultiProgress.payload.text?.title)
    }

    @Test
    fun `official multi progress payload centralizes expanded progress family semantics`() {
        val payload = MiFocusExpandedComponent.OfficialMultiProgressPayload(
            progressPercent = 64,
            color = "#2563EB",
            points = 3,
            text = MiFocusExpandedText(
                title = "本月进度",
                content = "已用 58GB"
            )
        )
        val helper = MiFocusExpandedComponent.officialMultiProgress(payload)

        assertEquals(64, helper.payload.progressPercent)
        assertEquals("#2563EB", helper.payload.color)
        assertEquals(3, helper.payload.points)
        assertEquals("本月进度", helper.payload.text?.title)
        assertEquals("已用 58GB", helper.payload.text?.content)

        val template19 = MiFocusOfficialTemplatePresets.expandedTemplate19PictureMultiProgress(
            text = MiFocusExpandedText(title = "套餐余量"),
            progressPayload = payload
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialMultiProgress

        val template21 = MiFocusOfficialTemplatePresets.expandedTemplate21NewImageTextMultiProgress(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(title = "下载任务"),
                icon = MiFocusAnimIcon(src = MiFocusPictureRef.Expanded)
            ),
            progressPayload = payload
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialMultiProgress

        assertNotNull(template19)
        assertNotNull(template21)
        assertEquals("本月进度", template19.payload.text?.title)
        assertEquals(3, template21.payload.points)
    }

    @Test
    fun `new image text preset family accepts official payload directly`() {
        val payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
            text = MiFocusExpandedText(
                title = "快递已签收",
                content = "丰巢 18:20"
            ),
            icon = MiFocusAnimIcon(src = MiFocusPictureRef.Expanded)
        )
        val openAction = MiFocusNotificationAction(
            key = "focus_test_template15_payload",
            title = "打开",
            pendingIntent = buildOpenPendingIntent(
                ApplicationProvider.getApplicationContext()
            )
        )

        val template15 = MiFocusOfficialTemplatePresets.expandedTemplate15NewImageTextActions(
            payload = payload,
            actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(
                actions = listOf(openAction)
            )
        ).components.firstOrNull() as? MiFocusExpandedComponent.OfficialNewImageText

        val template21 = MiFocusOfficialTemplatePresets.expandedTemplate21NewImageTextMultiProgress(
            payload = payload,
            progressPayload = MiFocusExpandedComponent.OfficialMultiProgressPayload(
                progressPercent = 64,
                text = MiFocusExpandedText(title = "本月进度")
            )
        ).components.firstOrNull() as? MiFocusExpandedComponent.OfficialNewImageText

        assertNotNull(template15)
        assertNotNull(template21)
        assertEquals("快递已签收", template15.payload.text.title)
        assertEquals("丰巢 18:20", template15.payload.text.content)
        assertEquals(MiFocusPictureRef.Expanded, template21.payload.icon?.src)
    }

    @Test
    fun `official action payload families centralize actions and text buttons`() {
        val openPendingIntent = buildOpenPendingIntent(
            ApplicationProvider.getApplicationContext()
        )
        val actionsPayload = MiFocusExpandedComponent.OfficialActionsPayload(
            actions = listOf(
                MiFocusNotificationAction(
                    key = "focus_test_action_payload",
                    title = "打开",
                    pendingIntent = openPendingIntent
                )
            )
        )
        val textButtonsPayload = MiFocusExpandedComponent.OfficialTextButtonsPayload(
            actions = listOf(
                MiFocusNotificationAction(
                    key = "focus_test_text_payload_left",
                    title = "稍后",
                    pendingIntent = openPendingIntent
                ).asSecondaryTextButton(),
                MiFocusNotificationAction(
                    key = "focus_test_text_payload_right",
                    title = "立即",
                    pendingIntent = openPendingIntent
                ).asPrimaryTextButton(backgroundColor = "#2563EB")
            )
        )

        val template12 = MiFocusOfficialTemplatePresets.expandedTemplate12ChatActions(
            chatText = MiFocusExpandedText(title = "新的群聊消息"),
            actionsPayload = actionsPayload
        ).components.getOrNull(1) as? MiFocusExpandedComponent.OfficialActions

        val template17 = MiFocusOfficialTemplatePresets.expandedTemplate17NewImageTextTextButtons(
            payload = MiFocusExpandedComponent.OfficialNewImageTextPayload(
                text = MiFocusExpandedText(title = "取药提醒")
            ),
            textButtonsPayload = textButtonsPayload
        ).components.getOrNull(2) as? MiFocusExpandedComponent.OfficialTextButtons

        assertNotNull(template12)
        assertNotNull(template17)
        assertEquals("打开", template12.payload.actions.first().title)
        assertEquals("立即", template17.payload.actions.last().title)
    }

    @Test
    fun `official hint and highlight capsule payloads centralize remaining button families`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val action = MiFocusNotificationAction(
            key = "focus_test_hint_capsule_payload",
            title = "处理",
            pendingIntent = buildOpenPendingIntent(context)
        ).asHighlightCapsuleButton()
        val secondaryHint = MiFocusExpandedComponent.OfficialHintPayload(
            text = MiFocusExpandedText(title = "4排6座"),
            type = 2,
            action = action
        )
        val primaryHint = MiFocusExpandedComponent.OfficialHintPayload(
            text = MiFocusExpandedText(title = "回复"),
            type = 1,
            action = action
        )
        val capsule = MiFocusExpandedComponent.OfficialHighlightCapsulePayload(
            text = MiFocusExpandedText(title = "限时优惠"),
            primaryText = "4899元",
            action = action,
            label = "限时"
        )

        val template9 = MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2(
            baseText = MiFocusExpandedText(title = "待处理事项"),
            hintPayload = secondaryHint
        )
        val template10 = MiFocusOfficialTemplatePresets.expandedTemplate10BaseButton3(
            baseText = MiFocusExpandedText(title = "消息提醒"),
            hintPayload = primaryHint
        )
        val template18 = MiFocusOfficialTemplatePresets.expandedTemplate18CoverHighlight(
            text = MiFocusExpandedText(title = "电影票已开售"),
            highlightPayload = capsule
        )

        assertEquals(2, (template9.components.getOrNull(2) as? MiFocusExpandedComponent.OfficialHint)?.type)
        assertEquals(1, (template10.components.getOrNull(2) as? MiFocusExpandedComponent.OfficialHint)?.type)
        assertEquals(
            "4899元",
            (template18.components.getOrNull(2) as? MiFocusExpandedComponent.OfficialHighlightCapsule)?.primaryText
        )

        val hintParam = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(expanded = template9)
        ).getString("miui.focus.param").orEmpty()
        val capsuleParam = MiFocusNotificationTemplate.build(
            context = context,
            spec = baseSpec(expanded = template18)
        ).getString("miui.focus.param").orEmpty()

        assertTrue(hintParam.contains("hintInfo"), "Missing hintInfo in $hintParam")
        assertTrue(capsuleParam.contains("highlightInfoV3"), "Missing highlightInfoV3 in $capsuleParam")
        assertFailsWith<IllegalArgumentException> {
            MiFocusExpandedComponent.officialHintPrimary(secondaryHint)
        }
    }

    @Test
    fun `official base helpers centralize public base semantics`() {
        val primary = MiFocusExpandedComponent.officialBasePrimary(
            text = MiFocusExpandedText(title = "处理中")
        )
        val secondary = MiFocusExpandedComponent.officialBaseSecondary(
            text = MiFocusExpandedText(title = "支付成功")
        )

        assertEquals(1, primary.type)
        assertEquals(2, secondary.type)

        val template1Base = MiFocusOfficialTemplatePresets.expandedTemplate1BaseLargePicture(
            text = MiFocusExpandedText(title = "当前天气")
        ).components.first() as MiFocusExpandedComponent.Base
        assertEquals(1, template1Base.type)

        val template9Base = MiFocusOfficialTemplatePresets.expandedTemplate9BaseButton2(
            baseText = MiFocusExpandedText(title = "待处理事项"),
            buttonText = MiFocusExpandedText(title = "座位"),
            action = MiFocusNotificationAction(
                key = "focus_test_open_base",
                title = "打开",
                pendingIntent = buildOpenPendingIntent(
                    ApplicationProvider.getApplicationContext()
                )
            ).asHighlightCapsuleButton()
        ).components.first() as MiFocusExpandedComponent.Base
        assertEquals(2, template9Base.type)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy countdown picture compatibility still patches official type 5 payload`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val legacyExpanded = MiFocusExpandedSpec(
            components = listOf(
                MiFocusExpandedComponent.button3IconText(
                    text = MiFocusExpandedText(title = "红灯倒计时")
                ),
                MiFocusExpandedComponent.CountdownPicture(
                    title = "15",
                    colorTitle = "#CC234567"
                )
            )
        )

        val root = JSONObject(
            MiFocusNotificationTemplate.build(
                context = context,
                spec = baseSpec(expanded = legacyExpanded)
            ).getString("miui.focus.param").orEmpty()
        )
        val picInfo = root.getJSONObject("param_v2").getJSONObject("picInfo")
        assertEquals(5, picInfo.getInt("type"))
        assertEquals("15", picInfo.getString("title"))
        assertEquals("#CC234567", picInfo.getString("colorTitle"))
    }

    @Test
    fun `text buttons reject more than two actions`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val openPendingIntent = buildOpenPendingIntent(context)
        val action = MiFocusNotificationAction(
            key = "focus_test_action",
            title = "Action",
            pendingIntent = openPendingIntent
        )

        assertFailsWith<IllegalArgumentException> {
            MiFocusExpandedComponent.TextButtons(
                listOf(action, action.copy(key = "a2"), action.copy(key = "a3"))
            )
        }
    }

    private fun baseSpec(
        island: MiFocusIslandSpec = MiFocusIslandSpec.summaryText(title = "运行"),
        expanded: MiFocusExpandedSpec = MiFocusExpandedSpec.base("Title", "Content"),
        extraPictures: List<MiFocusPictureAsset> = emptyList()
    ) = MiFocusNotificationSpec(
        title = "Title",
        content = "Content",
        displayIconResId = R.drawable.ic_kei_logo_island,
        extraPictures = extraPictures,
        island = island,
        expanded = expanded,
        outerGlow = true
    )

    private fun buildOpenPendingIntent(context: Application): PendingIntent {
        val intent = Intent("os.kei.core.notification.test.OPEN").setPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            9301,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getActionCompat(key: String): Notification.Action {
        return getParcelable<Notification.Action>(key)
            ?: error("Missing focus action: $key")
    }
}

class MiFocusNotificationTemplateTestApp : Application()
