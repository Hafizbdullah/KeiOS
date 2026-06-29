package os.kei.feature.github.model

import java.net.URI
import java.util.Locale

private val fdroidPackageNamePattern =
    Regex("""^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+$""")

data class FdroidRepositoryTrackIdentity(
    val repoBaseUrl: String,
    val normalizedRepoUrl: String,
    val host: String,
    val owner: String,
    val repo: String,
    val repoDisplayName: String,
    val packageName: String,
    val packagePageUrl: String = ""
)

data class FdroidTrackedAppConfig(
    val selectionMode: FdroidVersionSelectionMode = FdroidVersionSelectionMode.SuggestedVersionCode,
    val versionNameRegex: String = "",
    val apkNameRegex: String = "",
    val repoFingerprint: String = "",
    val indexFormat: FdroidIndexFormat = FdroidIndexFormat.Unknown,
    val trustPolicy: FdroidTrustPolicy = FdroidTrustPolicy.TrackOnlyWarn,
    val antiFeaturePolicy: FdroidAntiFeaturePolicy = FdroidAntiFeaturePolicy.ShowAndWarn,
    val blockedAntiFeatures: List<String> = emptyList(),
    val packagePageUrl: String = "",
    val repoPresetId: String = ""
)

enum class FdroidVersionSelectionMode(val storageId: String) {
    SuggestedVersionCode("suggested_version_code"),
    HighestCompatibleVersionCode("highest_compatible_version_code"),
    HighestVersionCode("highest_version_code"),
    VersionNameRegex("version_name_regex");

    companion object {
        fun fromStorageId(value: String?): FdroidVersionSelectionMode {
            val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.storageId == normalized }
                ?: when (normalized) {
                    "suggested", "recommended", "recommended_version" -> SuggestedVersionCode
                    "compatible", "highest_compatible" -> HighestCompatibleVersionCode
                    "highest", "highest_version" -> HighestVersionCode
                    "regex", "version_regex" -> VersionNameRegex
                    else -> SuggestedVersionCode
                }
        }
    }
}

enum class FdroidTrustPolicy(val storageId: String) {
    TrackOnlyWarn("track_only_warn"),
    RequireRepoFingerprint("require_repo_fingerprint"),
    RequireApkHash("require_apk_hash"),
    RequireOfficialSignerIndex("require_official_signer_index");

    companion object {
        fun fromStorageId(value: String?): FdroidTrustPolicy {
            val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.storageId == normalized }
                ?: when (normalized) {
                    "warn", "track", "track_warn" -> TrackOnlyWarn
                    "repo", "fingerprint" -> RequireRepoFingerprint
                    "hash", "apk_hash" -> RequireApkHash
                    "official", "signer_index" -> RequireOfficialSignerIndex
                    else -> TrackOnlyWarn
                }
        }
    }
}

enum class FdroidAntiFeaturePolicy(val storageId: String) {
    ShowAndWarn("show_and_warn"),
    HideTracking("hide_tracking"),
    HideSecurityRisk("hide_security_risk"),
    Custom("custom");

    companion object {
        fun fromStorageId(value: String?): FdroidAntiFeaturePolicy {
            val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.storageId == normalized }
                ?: when (normalized) {
                    "show", "warn" -> ShowAndWarn
                    "tracking" -> HideTracking
                    "security", "security_risk" -> HideSecurityRisk
                    else -> ShowAndWarn
                }
        }
    }
}

enum class FdroidIndexFormat(val storageId: String) {
    Unknown("unknown"),
    V2("v2"),
    V1("v1"),
    PackageApi("package_api");

    companion object {
        fun fromStorageId(value: String?): FdroidIndexFormat {
            val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.storageId == normalized }
                ?: when (normalized) {
                    "index-v2", "index_v2" -> V2
                    "index-v1", "index_v1", "jar" -> V1
                    "api", "package" -> PackageApi
                    else -> Unknown
                }
        }
    }
}

fun buildFdroidRepositoryTrackIdentity(
    rawUrl: String,
    rawPackageName: String = ""
): FdroidRepositoryTrackIdentity? {
    val normalizedUrl = rawUrl.trim()
    if (normalizedUrl.isBlank()) return null
    val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return null
    val scheme = uri.scheme.orEmpty().lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host
        .orEmpty()
        .lowercase(Locale.ROOT)
        .removePrefix("www.")
        .ifBlank { return null }
    val port = uri.port.takeIf { it >= 0 }
    val pathSegments = uri.path
        .orEmpty()
        .split('/')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val packageName = rawPackageName
        .trim()
        .ifBlank { fdroidPackageNameFromPath(host, pathSegments).orEmpty() }
    if (packageName.isNotBlank() && !fdroidPackageNamePattern.matches(packageName)) {
        return null
    }
    val repoSegments = fdroidRepoSegments(host, pathSegments)
    val normalizedRepoUrl = buildFdroidRepoUrl(
        scheme = scheme,
        host = host,
        port = port,
        repoSegments = repoSegments
    )
    val repo = repoSegments
        .joinToString("-")
        .sanitizeFdroidIdentityPart()
        .ifBlank { "repo" }
    return FdroidRepositoryTrackIdentity(
        repoBaseUrl = normalizedRepoUrl,
        normalizedRepoUrl = normalizedRepoUrl,
        host = host,
        owner = host,
        repo = repo,
        repoDisplayName = fdroidRepoDisplayName(host, repoSegments),
        packageName = packageName,
        packagePageUrl = fdroidPackagePageUrl(host, packageName)
    )
}

private fun fdroidPackageNameFromPath(
    host: String,
    pathSegments: List<String>
): String? {
    val normalizedSegments = pathSegments.map { it.lowercase(Locale.ROOT) }
    if (host == "f-droid.org") {
        val packageIndex = normalizedSegments.indexOf("packages")
        if (packageIndex >= 0) {
            return pathSegments.getOrNull(packageIndex + 1)
        }
    }
    val indexApkIndex = normalizedSegments.windowed(2).indexOfFirst { it == listOf("index", "apk") }
    if (indexApkIndex >= 0) {
        return pathSegments.getOrNull(indexApkIndex + 2)
    }
    val packageIndex = normalizedSegments.indexOf("packages")
    if (packageIndex >= 0) {
        return pathSegments.getOrNull(packageIndex + 1)
    }
    return null
}

private fun fdroidRepoSegments(
    host: String,
    pathSegments: List<String>
): List<String> {
    val normalizedSegments = pathSegments.map { it.lowercase(Locale.ROOT) }
    if (host == "f-droid.org") {
        return when {
            normalizedSegments.firstOrNull() == "archive" -> listOf("archive")
            else -> listOf("repo")
        }
    }
    if (host == "apt.izzysoft.de" &&
        normalizedSegments.take(2) == listOf("fdroid", "index")
    ) {
        return listOf("fdroid", "repo")
    }
    val trimmed = pathSegments.dropLastWhile { segment ->
        val lower = segment.lowercase(Locale.ROOT)
        lower == "entry.json" ||
            lower == "index-v2.json" ||
            lower == "index.xml" ||
            lower == "index.jar" ||
            lower == "index-v1.jar" ||
            lower == "signer-index.json"
    }
    return trimmed.ifEmpty { listOf("repo") }
}

private fun buildFdroidRepoUrl(
    scheme: String,
    host: String,
    port: Int?,
    repoSegments: List<String>
): String {
    val authority = if (port != null) "$host:$port" else host
    val path = repoSegments.joinToString("/", prefix = "/")
    return "$scheme://$authority$path".trimEnd('/')
}

private fun fdroidRepoDisplayName(
    host: String,
    repoSegments: List<String>
): String {
    val path = repoSegments.joinToString("/")
    return when {
        host == "f-droid.org" && path == "repo" -> "F-Droid"
        host == "f-droid.org" && path == "archive" -> "F-Droid Archive"
        host == "apt.izzysoft.de" && path == "fdroid/repo" -> "IzzyOnDroid"
        path.isBlank() -> host
        else -> "$host/$path"
    }
}

private fun fdroidPackagePageUrl(host: String, packageName: String): String {
    if (packageName.isBlank()) return ""
    return when (host) {
        "f-droid.org" -> "https://f-droid.org/packages/$packageName/"
        "apt.izzysoft.de" -> "https://apt.izzysoft.de/fdroid/index/apk/$packageName"
        else -> ""
    }
}

private fun String.sanitizeFdroidIdentityPart(): String {
    return lowercase(Locale.ROOT)
        .replace(Regex("""[^a-z0-9._-]+"""), "-")
        .trim('-', '.', '_')
}
