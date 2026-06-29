package os.kei.feature.github.data.remote.fdroid

data class FdroidPackageSnapshot(
    val repoUrl: String,
    val packageName: String,
    val suggestedVersionCode: Long?,
    val versions: List<FdroidVersionSnapshot>,
    val appName: String = "",
    val summary: String = "",
    val description: String = "",
    val license: String = "",
    val sourceCodeUrl: String = "",
    val webSiteUrl: String = "",
    val issueTrackerUrl: String = "",
    val changelogUrl: String = "",
    val categories: List<String> = emptyList(),
    val antiFeatures: List<FdroidAntiFeatureSnapshot> = emptyList()
) {
    val selectedSuggestedVersion: FdroidVersionSnapshot?
        get() = suggestedVersionCode?.let { code ->
            versions.firstOrNull { version -> version.versionCode == code }
        }
}

data class FdroidVersionSnapshot(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkPath: String,
    val apkSha256: String,
    val apkSizeBytes: Long,
    val addedAtMillis: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val nativeAbis: List<String>,
    val signerSha256: List<String>,
    val releaseChannels: List<String>,
    val whatsNew: String,
    val antiFeatures: List<FdroidAntiFeatureSnapshot>
)

data class FdroidAntiFeatureSnapshot(
    val id: String,
    val label: String = "",
    val description: String = ""
)

data class FdroidRepositorySnapshot(
    val repoUrl: String,
    val format: os.kei.feature.github.model.FdroidIndexFormat,
    val repoName: String,
    val repoDescription: String,
    val timestampMillis: Long?,
    val mirrors: List<String>,
    val packages: Map<String, FdroidPackageSnapshot>
) {
    val packageCount: Int
        get() = packages.size

    fun packageSnapshot(packageName: String): FdroidPackageSnapshot? {
        val key = packageName.trim()
        if (key.isBlank()) return null
        return packages[key] ?: packages[key.lowercase()]
    }
}
