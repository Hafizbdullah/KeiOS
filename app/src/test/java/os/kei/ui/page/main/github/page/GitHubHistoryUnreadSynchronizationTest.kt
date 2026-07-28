package os.kei.ui.page.main.github.page

import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class GitHubHistoryUnreadSynchronizationTest {
    @Test
    fun historyWatermarkChangesRefreshTheLongLivedPageBadgeState() {
        val storeSource = sourceFile(HISTORY_UNREAD_STORE_SOURCE)
        val serviceSource = sourceFile(HISTORY_UNREAD_SERVICE_SOURCE)
        val repositorySource = sourceFile(GITHUB_PAGE_REPOSITORY_SOURCE)
        val viewModelSource = sourceFile(GITHUB_PAGE_VIEW_MODEL_SOURCE)

        assertTrue(
            "GitHubHistoryUnreadStoreSignals.notifyChanged()" in storeSource,
            "Changing an unread watermark must publish a process-local signal",
        )
        assertTrue(
            "fun signalVersions(): StateFlow<Long>" in serviceSource,
            "The unread service must expose watermark changes",
        )
        assertTrue(
            "fun historyUnreadSignalVersions(): StateFlow<Long>" in repositorySource,
            "The GitHub page repository must expose unread watermark changes",
        )
        assertTrue(
            "repository.historyUnreadSignalVersions().collect" in viewModelSource,
            "The long-lived GitHub page ViewModel must observe unread watermark changes",
        )
        assertTrue(
            "refreshHistoryUnreadCount()" in viewModelSource,
            "An unread watermark signal must refresh the badge count",
        )
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

private const val HISTORY_UNREAD_STORE_SOURCE =
    "feature-github/src/main/java/os/kei/feature/github/data/local/GitHubHistoryUnreadStore.kt"
private const val HISTORY_UNREAD_SERVICE_SOURCE =
    "feature-github/src/main/java/os/kei/feature/github/domain/GitHubHistoryUnreadService.kt"
private const val GITHUB_PAGE_REPOSITORY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageRepository.kt"
private const val GITHUB_PAGE_VIEW_MODEL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageViewModel.kt"
