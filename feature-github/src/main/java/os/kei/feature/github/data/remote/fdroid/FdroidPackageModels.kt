package os.kei.feature.github.data.remote.fdroid

data class FdroidPackageSnapshot(
    val repoUrl: String,
    val packageName: String,
    val suggestedVersionCode: Long?,
    val versions: List<FdroidVersionSnapshot>
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
