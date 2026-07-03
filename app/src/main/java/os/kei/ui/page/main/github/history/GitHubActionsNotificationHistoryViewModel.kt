package os.kei.ui.page.main.github.history

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.feature.github.domain.GitHubRefreshHistoryOutcomeFilter
import os.kei.feature.github.domain.GitHubRefreshHistoryQuery
import os.kei.feature.github.domain.GitHubRefreshHistoryQueryDefaults
import os.kei.ui.page.main.github.page.GitHubAppIconLoader
import os.kei.ui.page.main.github.page.GitHubAppIconUiState

internal data class GitHubActionsNotificationHistoryUiState(
    val loading: Boolean = true,
    val historyMode: GitHubHistoryMode = GitHubHistoryMode.Refresh,
    val records: List<GitHubActionsNotificationHistoryUiRecord> = emptyList(),
    val refreshRecords: List<GitHubRefreshHistoryUiRecord> = emptyList(),
    val trackChangeRecords: List<GitHubTrackChangeHistoryUiRecord> = emptyList(),
    val appInstallRecords: List<GitHubAppInstallHistoryUiRecord> = emptyList(),
    val totalRecordCount: Int = 0,
    val totalRefreshRecordCount: Int = 0,
    val totalTrackChangeRecordCount: Int = 0,
    val totalAppInstallRecordCount: Int = 0,
    val errorMessage: String = "",
    val filterMode: GitHubActionsHistoryFilterMode = GitHubActionsHistoryFilterMode.All,
    val refreshFilterMode: GitHubRefreshHistoryFilterMode = GitHubRefreshHistoryFilterMode.All,
    val trackChangeFilterMode: GitHubTrackChangeHistoryFilterMode = GitHubTrackChangeHistoryFilterMode.All,
    val appInstallFilterMode: GitHubAppInstallHistoryFilterMode = GitHubAppInstallHistoryFilterMode.All,
    val sortMode: GitHubActionsHistorySortMode = GitHubActionsHistorySortMode.NotifiedAt,
    val refreshSortMode: GitHubRefreshHistorySortMode = GitHubRefreshHistorySortMode.FinishedAt,
    val trackChangeSortMode: GitHubTrackChangeHistorySortMode = GitHubTrackChangeHistorySortMode.ChangedAt,
    val appInstallSortMode: GitHubAppInstallHistorySortMode = GitHubAppInstallHistorySortMode.ChangedAt,
    val sortDirection: GitHubActionsHistorySortDirection = GitHubActionsHistorySortDirection.Descending,
    val lastCleanupRemovedCount: Int? = null,
    val exportInProgress: Boolean = false,
    val searchExpanded: Boolean = false,
    val searchQuery: String = "",
)

internal sealed interface GitHubActionsNotificationHistoryEvent {
    data class LaunchRefreshHistoryExport(
        val fileName: String,
    ) : GitHubActionsNotificationHistoryEvent

    data object RefreshHistoryExported : GitHubActionsNotificationHistoryEvent

    data class RefreshHistoryExportFailed(
        val reason: String,
    ) : GitHubActionsNotificationHistoryEvent
}

private data class PendingRefreshHistoryExport(
    val content: String,
)

private data class GitHubHistorySnapshot(
    val refreshRecords: List<GitHubRefreshHistoryUiRecord>,
    val actionRecords: List<GitHubActionsNotificationHistoryUiRecord>,
    val trackChangeRecords: List<GitHubTrackChangeHistoryUiRecord>,
    val appInstallRecords: List<GitHubAppInstallHistoryUiRecord>,
)

internal class GitHubActionsNotificationHistoryViewModel(
    private val repository: GitHubActionsNotificationHistoryRepository =
        GitHubActionsNotificationHistoryRepository(),
) : ViewModel() {
    private val appIconLoader = GitHubAppIconLoader(viewModelScope)
    private val historyOperationMutex = Mutex()
    private val _uiState = MutableStateFlow(GitHubActionsNotificationHistoryUiState())
    private val _events =
        MutableSharedFlow<GitHubActionsNotificationHistoryEvent>(
            replay = 0,
            extraBufferCapacity = 4,
        )
    val uiState: StateFlow<GitHubActionsNotificationHistoryUiState> = _uiState.asStateFlow()
    val events: SharedFlow<GitHubActionsNotificationHistoryEvent> = _events.asSharedFlow()
    val appIconState: StateFlow<GitHubAppIconUiState> = appIconLoader.state
    private var allRecords: List<GitHubActionsNotificationHistoryUiRecord> = emptyList()
    private var allRefreshRecords: List<GitHubRefreshHistoryUiRecord> = emptyList()
    private var allTrackChangeRecords: List<GitHubTrackChangeHistoryUiRecord> = emptyList()
    private var allAppInstallRecords: List<GitHubAppInstallHistoryUiRecord> = emptyList()
    private var pendingRefreshHistoryExport: PendingRefreshHistoryExport? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            historyOperationMutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        loading = true,
                        errorMessage = "",
                    )
                }
                val result =
                    runCatching {
                        GitHubHistorySnapshot(
                            refreshRecords = repository.loadRefreshHistory(),
                            actionRecords = repository.loadHistory(),
                            trackChangeRecords = repository.loadTrackChangeHistory(),
                            appInstallRecords = repository.loadAppInstallHistory(),
                        )
                    }
                result
                    .onSuccess { snapshot ->
                        allRefreshRecords = snapshot.refreshRecords
                        allRecords = snapshot.actionRecords
                        allTrackChangeRecords = snapshot.trackChangeRecords
                        allAppInstallRecords = snapshot.appInstallRecords
                        updateDisplayRecords {
                            copy(
                                loading = false,
                                totalRecordCount = snapshot.actionRecords.size,
                                totalRefreshRecordCount = snapshot.refreshRecords.size,
                                totalTrackChangeRecordCount = snapshot.trackChangeRecords.size,
                                totalAppInstallRecordCount = snapshot.appInstallRecords.size,
                                errorMessage = "",
                                lastCleanupRemovedCount = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                loading = false,
                                records = emptyList(),
                                refreshRecords = emptyList(),
                                trackChangeRecords = emptyList(),
                                appInstallRecords = emptyList(),
                                totalRecordCount = allRecords.size,
                                totalRefreshRecordCount = allRefreshRecords.size,
                                totalTrackChangeRecordCount = allTrackChangeRecords.size,
                                totalAppInstallRecordCount = allAppInstallRecords.size,
                                errorMessage = error.message.orEmpty(),
                            )
                        }
                    }
            }
        }
    }

    fun requestAppIcons(
        context: Context,
        packageNames: List<String>,
    ) {
        appIconLoader.requestIcons(
            context = context.applicationContext,
            packageNames = packageNames,
        )
    }

    fun setFilterMode(value: GitHubActionsHistoryFilterMode) {
        updateDisplayRecords { copy(filterMode = value) }
    }

    fun setHistoryMode(value: GitHubHistoryMode) {
        updateDisplayRecords { copy(historyMode = value) }
    }

    fun setSearchExpanded(value: Boolean) {
        _uiState.update { state -> state.copy(searchExpanded = value) }
    }

    fun setSearchQuery(value: String) {
        updateDisplayRecords { copy(searchQuery = value.take(128)) }
    }

    fun setRefreshFilterMode(value: GitHubRefreshHistoryFilterMode) {
        updateDisplayRecords { copy(refreshFilterMode = value) }
    }

    fun setTrackChangeFilterMode(value: GitHubTrackChangeHistoryFilterMode) {
        updateDisplayRecords { copy(trackChangeFilterMode = value) }
    }

    fun setAppInstallFilterMode(value: GitHubAppInstallHistoryFilterMode) {
        updateDisplayRecords { copy(appInstallFilterMode = value) }
    }

    fun setSortMode(value: GitHubActionsHistorySortMode) {
        updateDisplayRecords { copy(sortMode = value) }
    }

    fun setRefreshSortMode(value: GitHubRefreshHistorySortMode) {
        updateDisplayRecords { copy(refreshSortMode = value) }
    }

    fun setTrackChangeSortMode(value: GitHubTrackChangeHistorySortMode) {
        updateDisplayRecords { copy(trackChangeSortMode = value) }
    }

    fun setAppInstallSortMode(value: GitHubAppInstallHistorySortMode) {
        updateDisplayRecords { copy(appInstallSortMode = value) }
    }

    fun setSortDirection(value: GitHubActionsHistorySortDirection) {
        updateDisplayRecords { copy(sortDirection = value) }
    }

    fun pruneOlderThan(age: GitHubActionsHistoryCleanupAge) {
        viewModelScope.launch {
            historyOperationMutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        loading = true,
                        errorMessage = "",
                    )
                }
                val mode = _uiState.value.historyMode
                val result =
                    runCatching {
                        when (mode) {
                            GitHubHistoryMode.Refresh -> repository.pruneRefreshHistoryOlderThanDays(age.days)
                            GitHubHistoryMode.Actions -> repository.pruneOlderThanDays(age.days)
                            GitHubHistoryMode.Tracking -> repository.pruneTrackChangeHistoryOlderThanDays(age.days)
                            GitHubHistoryMode.Apps -> repository.pruneAppInstallHistoryOlderThanDays(age.days)
                        }
                    }
                result
                    .onSuccess { removedCount ->
                        val refreshRecords = repository.loadRefreshHistory()
                        val records = repository.loadHistory()
                        val trackChangeRecords = repository.loadTrackChangeHistory()
                        val appInstallRecords = repository.loadAppInstallHistory()
                        allRefreshRecords = refreshRecords
                        allRecords = records
                        allTrackChangeRecords = trackChangeRecords
                        allAppInstallRecords = appInstallRecords
                        updateDisplayRecords {
                            copy(
                                loading = false,
                                totalRecordCount = records.size,
                                totalRefreshRecordCount = refreshRecords.size,
                                totalTrackChangeRecordCount = trackChangeRecords.size,
                                totalAppInstallRecordCount = appInstallRecords.size,
                                errorMessage = "",
                                lastCleanupRemovedCount = removedCount,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                loading = false,
                                errorMessage = error.message.orEmpty(),
                            )
                        }
                    }
            }
        }
    }

    fun requestRefreshHistoryExport() {
        viewModelScope.launch {
            historyOperationMutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        exportInProgress = true,
                        errorMessage = "",
                    )
                }
                runCatching {
                    val query =
                        GitHubRefreshHistoryQuery(
                            outcome = _uiState.value.refreshFilterMode.toDomainOutcomeFilter(),
                            limit = GitHubRefreshHistoryQueryDefaults.MAX_LIMIT,
                        )
                    val content = repository.buildRefreshHistoryExportJson(query)
                    val fileName = buildRefreshHistoryExportFileName()
                    pendingRefreshHistoryExport =
                        PendingRefreshHistoryExport(
                            content = content,
                        )
                    _events.emit(
                        GitHubActionsNotificationHistoryEvent.LaunchRefreshHistoryExport(
                            fileName = fileName,
                        ),
                    )
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    pendingRefreshHistoryExport = null
                    _events.emit(
                        GitHubActionsNotificationHistoryEvent.RefreshHistoryExportFailed(
                            reason = error.message.orEmpty(),
                        ),
                    )
                }
                _uiState.update { state -> state.copy(exportInProgress = false) }
            }
        }
    }

    fun writePendingRefreshHistoryExport(
        contentResolver: ContentResolver,
        uri: Uri?,
    ) {
        val pending = pendingRefreshHistoryExport
        if (uri == null || pending == null) {
            pendingRefreshHistoryExport = null
            return
        }
        viewModelScope.launch {
            _uiState.update { state -> state.copy(exportInProgress = true) }
            runCatching {
                repository.writeText(
                    contentResolver = contentResolver,
                    uri = uri,
                    content = pending.content,
                )
            }.onSuccess {
                pendingRefreshHistoryExport = null
                _events.emit(GitHubActionsNotificationHistoryEvent.RefreshHistoryExported)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _events.emit(
                    GitHubActionsNotificationHistoryEvent.RefreshHistoryExportFailed(
                        reason = error.message.orEmpty(),
                    ),
                )
            }
            _uiState.update { state -> state.copy(exportInProgress = false) }
        }
    }

    private fun updateDisplayRecords(
        transform: GitHubActionsNotificationHistoryUiState.() -> GitHubActionsNotificationHistoryUiState,
    ) {
        _uiState.update { previous ->
            val next = previous.transform()
            next.copy(
                records =
                    buildGitHubActionsHistoryDisplayRecords(
                        records = allRecords,
                        filterMode = next.filterMode,
                        sortMode = next.sortMode,
                        sortDirection = next.sortDirection,
                        searchQuery = next.searchQuery,
                    ),
                refreshRecords =
                    buildGitHubRefreshHistoryDisplayRecords(
                        records = allRefreshRecords,
                        filterMode = next.refreshFilterMode,
                        sortMode = next.refreshSortMode,
                        sortDirection = next.sortDirection,
                        searchQuery = next.searchQuery,
                    ),
                trackChangeRecords =
                    buildGitHubTrackChangeHistoryDisplayRecords(
                        records = allTrackChangeRecords,
                        filterMode = next.trackChangeFilterMode,
                        sortMode = next.trackChangeSortMode,
                        sortDirection = next.sortDirection,
                        searchQuery = next.searchQuery,
                    ),
                appInstallRecords =
                    buildGitHubAppInstallHistoryDisplayRecords(
                        records = allAppInstallRecords,
                        filterMode = next.appInstallFilterMode,
                        sortMode = next.appInstallSortMode,
                        sortDirection = next.sortDirection,
                        searchQuery = next.searchQuery,
                    ),
                totalRecordCount = allRecords.size,
                totalRefreshRecordCount = allRefreshRecords.size,
                totalTrackChangeRecordCount = allTrackChangeRecords.size,
                totalAppInstallRecordCount = allAppInstallRecords.size,
            )
        }
    }

    private fun GitHubRefreshHistoryFilterMode.toDomainOutcomeFilter(): GitHubRefreshHistoryOutcomeFilter {
        return when (this) {
            GitHubRefreshHistoryFilterMode.All -> GitHubRefreshHistoryOutcomeFilter.All
            GitHubRefreshHistoryFilterMode.Completed -> GitHubRefreshHistoryOutcomeFilter.Completed
            GitHubRefreshHistoryFilterMode.Updatable -> GitHubRefreshHistoryOutcomeFilter.Updatable
            GitHubRefreshHistoryFilterMode.Failed -> GitHubRefreshHistoryOutcomeFilter.Failed
            GitHubRefreshHistoryFilterMode.Cancelled -> GitHubRefreshHistoryOutcomeFilter.Cancelled
        }
    }

    private fun buildRefreshHistoryExportFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "keios-github-refresh-history-${formatter.format(Date())}.json"
    }
}
