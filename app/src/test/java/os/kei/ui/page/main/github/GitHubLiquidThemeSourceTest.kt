package os.kei.ui.page.main.github

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class GitHubLiquidThemeSourceTest {
    @Test
    fun corePageAndTrackedItemSurfacesFollowTheKeiOSAppTheme() {
        val pageSource = sourceFile(GITHUB_PAGE_SOURCE)
        val infoCardsSource = sourceFile(GITHUB_TRACKED_ITEM_INFO_CARDS_SOURCE)
        val healthCardsSource = sourceFile(GITHUB_TRACKED_ITEM_HEALTH_CARDS_SOURCE)
        val assetCountBubbleSource = sourceFile(GITHUB_TRACKED_ITEM_ASSET_COUNT_BUBBLE_SOURCE)
        val appSelectionRowsSource = sourceFile(GITHUB_APP_SELECTION_ROWS_SOURCE)
        val pendingShareImportSource = sourceFile(GITHUB_PENDING_SHARE_IMPORT_SOURCE)

        assertEquals(1, pageSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(2, infoCardsSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(1, healthCardsSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(1, assetCountBubbleSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(3, appSelectionRowsSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(2, pendingShareImportSource.occurrencesOf("isAppInDarkTheme()"))

        listOf(
            pageSource,
            infoCardsSource,
            healthCardsSource,
            assetCountBubbleSource,
            appSelectionRowsSource,
            pendingShareImportSource,
        ).forEach { source ->
            assertFalse("isSystemInDarkTheme" in source)
        }
    }
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val GITHUB_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPage.kt"
private const val GITHUB_TRACKED_ITEM_INFO_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemInfoCards.kt"
private const val GITHUB_TRACKED_ITEM_HEALTH_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHealthCards.kt"
private const val GITHUB_TRACKED_ITEM_ASSET_COUNT_BUBBLE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemAssetCountBubble.kt"
private const val GITHUB_APP_SELECTION_ROWS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/GitHubAppSelectionRows.kt"
private const val GITHUB_PENDING_SHARE_IMPORT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubPendingShareImportSection.kt"
