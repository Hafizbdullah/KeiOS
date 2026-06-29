package os.kei.feature.github.data.local.fdroid

import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot

data class FdroidRepoCacheKey(
    val repoUrl: String
) {
    companion object {
        fun from(repoUrl: String): FdroidRepoCacheKey {
            return FdroidRepoCacheKey(repoUrl.trim().trimEnd('/'))
        }
    }
}

data class FdroidRepoCacheRecord(
    val repoUrl: String,
    val fetchedAtMillis: Long,
    val etag: String,
    val lastModified: String,
    val snapshot: FdroidRepositorySnapshot
) {
    fun isFresh(
        nowMillis: Long,
        maxAgeMillis: Long
    ): Boolean {
        if (maxAgeMillis <= 0L) return false
        if (fetchedAtMillis <= 0L) return false
        return nowMillis - fetchedAtMillis <= maxAgeMillis
    }
}
