package os.kei.ui.page.main.github.history

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubHistoryCardBackdropContractTest {
    @Test
    fun cardsWithLiquidChildrenExportIndependentCardMaterial() {
        val contracts =
            listOf(
                CardContract(GITHUB_REFRESH_HISTORY_CARDS_SOURCE, "GitHubHistoryOverviewCard"),
                CardContract(GITHUB_REFRESH_HISTORY_CARDS_SOURCE, "GitHubRefreshHistoryRecordCard"),
                CardContract(GITHUB_TRACK_CHANGE_HISTORY_CARDS_SOURCE, "GitHubTrackChangeHistoryRecordCard"),
                CardContract(GITHUB_APP_INSTALL_HISTORY_CARDS_SOURCE, "GitHubAppInstallHistoryRecordCard"),
                CardContract(GITHUB_ACTIONS_HISTORY_PAGE_SOURCE, "GitHubActionsHistoryRecordCard"),
            )

        contracts.forEach { contract ->
            val cardBlock = sourceFile(contract.sourcePath).composableFunctionBlock(contract.functionName)
            assertTrue(
                "exportBackdropToContent = true," in cardBlock,
                "${contract.functionName} must export an independent material to its Liquid children",
            )
        }
    }

    @Test
    fun stateCardWithoutLiquidChildrenRemainsSingleLayered() {
        val source = sourceFile(GITHUB_ACTIONS_HISTORY_PAGE_SOURCE)
        val stateCardBlock = source.composableFunctionBlock("GitHubActionsHistoryStateCard")

        assertFalse(
            "exportBackdropToContent = true," in stateCardBlock,
            "The state card has no nested Liquid consumer and should avoid an unused layer",
        )
    }

    @Test
    fun appAndTrackHistoryDetailsReuseTheCompactInfoListWithoutChangingRowContracts() {
        val contracts =
            listOf(
                InfoListContract(
                    sourcePath = GITHUB_APP_INSTALL_HISTORY_CARDS_SOURCE,
                    functionName = "GitHubAppInstallHistoryRecordCard",
                    rowCount = 18,
                    singleLineCount = 8,
                    twoLineCount = 10,
                    threeLineCount = 0,
                    stackedCount = 2,
                ),
                InfoListContract(
                    sourcePath = GITHUB_TRACK_CHANGE_HISTORY_CARDS_SOURCE,
                    functionName = "GitHubTrackChangeHistoryRecordCard",
                    rowCount = 8,
                    singleLineCount = 4,
                    twoLineCount = 3,
                    threeLineCount = 1,
                    stackedCount = 3,
                ),
            )

        contracts.forEach { contract ->
            val source = sourceFile(contract.sourcePath)
            val cardBlock = source.composableFunctionBlock(contract.functionName)
            val infoListHeader =
                cardBlock
                    .substringAfter("AppInfoListBody(")
                    .substringBefore(") {")

            assertTrue("import os.kei.ui.page.main.widget.core.AppInfoListBody" in source)
            assertEquals(1, cardBlock.occurrencesOf("AppInfoListBody("))
            assertFalse(Regex("(?m)^\\s*Column\\(").containsMatchIn(cardBlock))
            assertTrue("modifier = Modifier.fillMaxWidth()" in infoListHeader)
            assertTrue("verticalSpacing = CardLayoutRhythm.compactSectionGap" in infoListHeader)
            assertFalse("Arrangement.spacedBy" in cardBlock)
            assertEquals(contract.rowCount, cardBlock.occurrencesOf("AppInfoRow("))
            assertEquals(contract.rowCount, cardBlock.occurrencesOf("valueOverflow = TextOverflow.Ellipsis"))
            assertEquals(contract.singleLineCount, cardBlock.occurrencesOf("valueMaxLines = 1"))
            assertEquals(contract.twoLineCount, cardBlock.occurrencesOf("valueMaxLines = 2"))
            assertEquals(contract.threeLineCount, cardBlock.occurrencesOf("valueMaxLines = 3"))
            assertEquals(contract.stackedCount, cardBlock.occurrencesOf("stacked = true"))
            assertFalse("enableLongPressCopy" in cardBlock)
            assertTrue("exportBackdropToContent = true" in cardBlock)
            assertTrue("collapsible = true" in cardBlock)
            assertTrue("expanded = expanded" in cardBlock)
            assertTrue("onExpandedChange = onExpandedChange" in cardBlock)
        }

        val infoListBody = sourceFile(APP_CARD_BODY_LAYOUTS_SOURCE).composableFunctionBlock("AppInfoListBody")
        assertTrue("contentPadding = PaddingValues(0.dp)" in infoListBody)
    }
}

private data class CardContract(
    val sourcePath: String,
    val functionName: String,
)

private data class InfoListContract(
    val sourcePath: String,
    val functionName: String,
    val rowCount: Int,
    val singleLineCount: Int,
    val twoLineCount: Int,
    val threeLineCount: Int,
    val stackedCount: Int,
)

private fun String.occurrencesOf(value: String): Int = windowed(value.length).count { it == value }

private fun String.composableFunctionBlock(functionName: String): String {
    val marker = "fun $functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }
    val end = indexOf("\n@Composable", startIndex = start + marker.length).takeIf { it >= 0 } ?: length
    return substring(start, end)
}

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val GITHUB_REFRESH_HISTORY_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/history/GitHubRefreshHistoryCards.kt"
private const val GITHUB_TRACK_CHANGE_HISTORY_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/history/GitHubTrackChangeHistoryCards.kt"
private const val GITHUB_APP_INSTALL_HISTORY_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/history/GitHubAppInstallHistoryCards.kt"
private const val GITHUB_ACTIONS_HISTORY_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/history/GitHubActionsNotificationHistoryPage.kt"
private const val APP_CARD_BODY_LAYOUTS_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/core/AppCardBodyLayouts.kt"
