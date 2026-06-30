package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchRequest
import os.kei.feature.github.model.FdroidAppSearchRepoReport
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.ui.page.main.github.localizedGitHubPageErrorMessage

internal class GitHubFdroidTrackSearchActions(
    private val env: GitHubPageActionEnvironment
) {
    private var searchJob: Job? = null
    private var searchGeneration: Long = 0L
    private val state get() = env.state
    private val scope get() = env.scope
    private val repository get() = env.repository

    fun searchByName() {
        if (state.fdroidAppSearchRunning) return
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.FdroidRepository) return
        val query = state.fdroidAppSearchQueryInput.trim()
        if (query.isBlank()) {
            env.toast(R.string.github_toast_fdroid_search_requires_query)
            return
        }
        runSearch(
            query = query,
            packageName = "",
            emptyToastRes = R.string.github_toast_fdroid_search_no_match,
        )
    }

    fun scanReposFromPackage() {
        if (state.fdroidAppSearchRunning) return
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.FdroidRepository) return
        val packageName = state.packageNameInput.trim()
            .ifBlank { state.selectedApp?.packageName.orEmpty().trim() }
        if (packageName.isBlank()) {
            env.toast(R.string.github_toast_fdroid_scan_requires_package)
            return
        }
        val selectedAppLabel =
            state.selectedApp
                ?.takeIf { app -> app.packageName.equals(packageName, ignoreCase = true) }
                ?.label
                .orEmpty()
        runSearch(
            query = selectedAppLabel,
            packageName = packageName,
            emptyToastRes = R.string.github_toast_fdroid_scan_no_match,
        )
    }

    fun retryFailures() {
        if (state.fdroidAppSearchRunning) return
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.FdroidRepository) return
        val failedRepoUrls = state.fdroidAppSearchFailures
            .map { failure -> failure.repoUrl.trim().trimEnd('/') }
            .filter { url -> url.isNotBlank() }
            .distinct()
        if (failedRepoUrls.isEmpty()) return
        val query = state.fdroidAppSearchQueryInput.trim()
        val packageName = state.packageNameInput.trim()
            .ifBlank { state.selectedApp?.packageName.orEmpty().trim() }
        if (query.isBlank() && packageName.isBlank()) {
            env.toast(R.string.github_toast_fdroid_search_requires_query)
            return
        }
        runSearch(
            query = query,
            packageName = packageName,
            emptyToastRes = R.string.github_toast_fdroid_search_no_match,
            repoUrlsOverride = failedRepoUrls,
            forceRefresh = true,
            clearCandidates = false,
        )
    }

    fun selectCandidate(candidate: FdroidAppSearchCandidate) {
        cancel()
        state.repoUrlInput = candidate.repoUrl
        state.packageNameInput = candidate.packageName
        state.fdroidSelectedCandidate = candidate
        state.fdroidAppSearchQueryInput = candidate.appName.ifBlank { candidate.packageName }
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        state.fdroidRepoScopeIdInput = candidate.repoPresetId.ifBlank {
            FdroidRepositoryPresets.presetForRepoUrl(candidate.repoUrl)?.id
                ?: FdroidRepositoryPresets.CUSTOM_ID
        }
        state.selectedApp = state.appList.firstOrNull { app ->
            app.packageName.equals(candidate.packageName, ignoreCase = true)
        } ?: state.selectedApp?.takeIf { selected ->
            selected.packageName.equals(candidate.packageName, ignoreCase = true)
        }
        env.toast(
            R.string.github_toast_fdroid_candidate_selected,
            candidate.displayName,
            candidate.repoDisplayName
        )
    }

    fun cancel() {
        searchGeneration += 1
        searchJob?.cancel()
        searchJob = null
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        if (state.fdroidAppSearchRunning) {
            state.fdroidAppSearchRunning = false
        }
    }

    private fun runSearch(
        query: String,
        packageName: String,
        emptyToastRes: Int,
        repoUrlsOverride: List<String>? = null,
        forceRefresh: Boolean = false,
        clearCandidates: Boolean = true,
    ) {
        val repoUrls =
            repoUrlsOverride
                ?: FdroidRepositoryPresets.repoUrlsForScope(
                    scopeId = state.fdroidRepoScopeIdInput,
                    customRepoUrl = state.repoUrlInput,
                    commonRepoIds = state.lookupConfig.normalizedFdroidCommonRepoIds
                )
        if (repoUrls.isEmpty()) {
            env.toast(R.string.github_toast_fill_repo_and_select_app)
            return
        }
        state.fdroidAppSearchRunning = true
        if (clearCandidates) {
            state.fdroidAppSearchCandidates = emptyList()
        }
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        if (clearCandidates) {
            state.fdroidAppSearchRepoReports = emptyList()
        }
        if (clearCandidates) {
            state.fdroidSelectedCandidate = null
        }
        val generation = ++searchGeneration
        searchJob?.cancel()
        searchJob = scope.launch {
            try {
                val result = repository.searchFdroidApps(
                    FdroidAppSearchRequest(
                        query = query,
                        packageName = packageName,
                        repoUrls = repoUrls,
                        limit = FDROID_SEARCH_LIMIT,
                        forceRefresh = forceRefresh
                    )
                ).getOrElse { error ->
                    if (!isActive(generation)) return@launch
                    env.toast(
                        R.string.github_toast_fdroid_search_failed,
                        localizedGitHubPageErrorMessage(
                            context = env.context,
                            error = error,
                            fallbackMessage = env.string(R.string.github_error_fdroid_search_failed),
                        ),
                    )
                    return@launch
                }
                if (!isActive(generation)) return@launch
                val newCandidateCount = result.candidates.size
                state.fdroidAppSearchCandidates =
                    if (clearCandidates) {
                        result.candidates
                    } else {
                        mergeCandidates(state.fdroidAppSearchCandidates, result.candidates)
                    }
                state.fdroidAppSearchFailures = result.failures
                state.fdroidAppSearchRepoReports =
                    if (clearCandidates) {
                        result.repoReports
                    } else {
                        mergeRepoReports(state.fdroidAppSearchRepoReports, result.repoReports)
                    }
                if (state.fdroidAppSearchCandidates.isEmpty()) {
                    env.toast(emptyToastRes)
                } else {
                    env.toast(
                        R.string.github_toast_fdroid_search_candidates_found,
                        newCandidateCount.takeIf { count -> count > 0 }
                            ?: state.fdroidAppSearchCandidates.size
                    )
                }
            } finally {
                if (generation == searchGeneration) {
                    state.fdroidAppSearchRunning = false
                    searchJob = null
                }
            }
        }
    }

    private fun isActive(generation: Long): Boolean {
        return generation == searchGeneration &&
            state.showAddSheet &&
            state.trackSourceModeInput == GitHubTrackedSourceMode.FdroidRepository
    }

    private fun mergeCandidates(
        current: List<FdroidAppSearchCandidate>,
        incoming: List<FdroidAppSearchCandidate>
    ): List<FdroidAppSearchCandidate> {
        return (current + incoming)
            .distinctBy { candidate ->
                candidate.repoUrl.trim().trimEnd('/') +
                    "|" +
                    candidate.packageName.trim().lowercase()
            }
    }

    private fun mergeRepoReports(
        current: List<FdroidAppSearchRepoReport>,
        incoming: List<FdroidAppSearchRepoReport>
    ): List<FdroidAppSearchRepoReport> {
        val merged = LinkedHashMap<String, FdroidAppSearchRepoReport>()
        current.forEach { report ->
            merged[report.repoUrl.trim().trimEnd('/')] = report
        }
        incoming.forEach { report ->
            merged[report.repoUrl.trim().trimEnd('/')] = report
        }
        return merged.values.toList()
    }

    private companion object {
        const val FDROID_SEARCH_LIMIT = 16
    }
}
