package os.kei.feature.github.model

import java.util.Locale

data class FdroidRepositoryPreset(
    val id: String,
    val displayName: String,
    val repoUrl: String
)

object FdroidRepositoryPresets {
    const val COMMON_ID = "common"
    const val CUSTOM_ID = "custom"
    const val MAIN_ID = "fdroid_main"
    const val ARCHIVE_ID = "fdroid_archive"
    const val IZZY_ID = "izzyondroid"
    const val GUARDIAN_ID = "guardian_project"

    val Main = FdroidRepositoryPreset(
        id = MAIN_ID,
        displayName = "F-Droid",
        repoUrl = "https://f-droid.org/repo"
    )

    val Archive = FdroidRepositoryPreset(
        id = ARCHIVE_ID,
        displayName = "F-Droid Archive",
        repoUrl = "https://f-droid.org/archive"
    )

    val IzzyOnDroid = FdroidRepositoryPreset(
        id = IZZY_ID,
        displayName = "IzzyOnDroid",
        repoUrl = "https://apt.izzysoft.de/fdroid/repo"
    )

    val GuardianProject = FdroidRepositoryPreset(
        id = GUARDIAN_ID,
        displayName = "Guardian Project",
        repoUrl = "https://guardianproject.info/fdroid/repo"
    )

    val entries: List<FdroidRepositoryPreset> =
        listOf(Main, IzzyOnDroid, GuardianProject, Archive)

    val commonSearchRepos: List<FdroidRepositoryPreset> =
        listOf(Main, IzzyOnDroid, GuardianProject)

    fun presetForId(id: String): FdroidRepositoryPreset? {
        val normalized = id.trim().lowercase(Locale.ROOT)
        return entries.firstOrNull { preset -> preset.id == normalized }
    }

    fun presetForRepoUrl(repoUrl: String): FdroidRepositoryPreset? {
        val normalized = repoUrl.normalizedFdroidRepoUrlKey()
        return entries.firstOrNull { preset ->
            preset.repoUrl.normalizedFdroidRepoUrlKey() == normalized
        }
    }

    fun repoUrlsForScope(scopeId: String, customRepoUrl: String): List<String> {
        return when (scopeId.trim().lowercase(Locale.ROOT)) {
            COMMON_ID -> commonSearchRepos.map { preset -> preset.repoUrl }
            CUSTOM_ID -> listOf(customRepoUrl)
            else -> presetForId(scopeId)?.let { listOf(it.repoUrl) }.orEmpty()
        }.map { url -> url.trim().trimEnd('/') }
            .filter { url -> url.isNotBlank() }
            .distinctBy { url -> url.normalizedFdroidRepoUrlKey() }
    }
}

fun String.normalizedFdroidRepoUrlKey(): String =
    trim()
        .trimEnd('/')
        .lowercase(Locale.ROOT)
