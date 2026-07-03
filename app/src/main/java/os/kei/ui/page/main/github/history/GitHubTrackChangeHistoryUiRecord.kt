package os.kei.ui.page.main.github.history

import androidx.compose.runtime.Immutable
import os.kei.feature.github.model.GitHubTrackChangeHistoryRecord

@Immutable
internal data class GitHubTrackChangeHistoryUiRecord(
    val record: GitHubTrackChangeHistoryRecord,
)
