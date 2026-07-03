package os.kei.ui.page.main.github.history

import androidx.compose.runtime.Immutable
import os.kei.feature.github.model.GitHubAppInstallHistoryRecord

@Immutable
internal data class GitHubAppInstallHistoryUiRecord(
    val record: GitHubAppInstallHistoryRecord,
)
