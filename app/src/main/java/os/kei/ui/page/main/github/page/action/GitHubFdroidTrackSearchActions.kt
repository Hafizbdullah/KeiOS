package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchRequest
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

    fun selectCandidate(candidate: FdroidAppSearchCandidate) {
        cancel()
        state.repoUrlInput = candidate.repoUrl
        state.packageNameInput = candidate.packageName
        state.fdroidSelectedCandidate = candidate
        state.fdroidAppSearchQueryInput = candidate.appName.ifBlank { candidate.packageName }
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
        if (state.fdroidAppSearchRunning) {
            state.fdroidAppSearchRunning = false
        }
    }

    private fun runSearch(
        query: String,
        packageName: String,
        emptyToastRes: Int
    ) {
        val repoUrls = FdroidRepositoryPresets.repoUrlsForScope(
            scopeId = state.fdroidRepoScopeIdInput,
            customRepoUrl = state.repoUrlInput
        )
        if (repoUrls.isEmpty()) {
            env.toast(R.string.github_toast_fill_repo_and_select_app)
            return
        }
        state.fdroidAppSearchRunning = true
        state.fdroidAppSearchCandidates = emptyList()
        state.fdroidSelectedCandidate = null
        val generation = ++searchGeneration
        searchJob?.cancel()
        searchJob = scope.launch {
            try {
                val result = repository.searchFdroidApps(
                    FdroidAppSearchRequest(
                        query = query,
                        packageName = packageName,
                        repoUrls = repoUrls,
                        limit = FDROID_SEARCH_LIMIT
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
                state.fdroidAppSearchCandidates = result.candidates
                if (result.candidates.isEmpty()) {
                    env.toast(emptyToastRes)
                } else {
                    env.toast(
                        R.string.github_toast_fdroid_search_candidates_found,
                        result.candidates.size
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

    private companion object {
        const val FDROID_SEARCH_LIMIT = 16
    }
}
