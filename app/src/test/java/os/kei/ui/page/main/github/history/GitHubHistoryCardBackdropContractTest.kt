package os.kei.ui.page.main.github.history

import org.junit.Test
import java.io.File
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
}

private data class CardContract(
    val sourcePath: String,
    val functionName: String,
)

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
