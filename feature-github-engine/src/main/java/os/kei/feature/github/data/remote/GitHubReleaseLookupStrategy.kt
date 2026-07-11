package os.kei.feature.github.data.remote

import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot

interface GitHubReleaseLookupStrategy {
    val id: String

    suspend fun loadSnapshot(owner: String, repo: String): Result<GitHubRepositoryReleaseSnapshot>

    fun clearCaches()
}
