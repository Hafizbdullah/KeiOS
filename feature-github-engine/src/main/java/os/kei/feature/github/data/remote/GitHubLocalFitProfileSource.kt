package os.kei.feature.github.data.remote

import os.kei.feature.github.model.GitHubRemoteApkVersionInfo
import os.kei.feature.github.model.GitHubRepositoryLocalFitProfile
import os.kei.feature.github.model.GitHubRepositoryProfileSource

object GitHubLocalFitProfileSource {
    fun build(
        localPackageName: String,
        localVersionName: String,
        localVersionCode: Long,
        preciseStableApkVersion: GitHubRemoteApkVersionInfo?,
        precisePreReleaseApkVersion: GitHubRemoteApkVersionInfo?,
        fetchedAtMillis: Long,
    ): GitHubRepositoryLocalFitProfile {
        val remoteApk = preciseStableApkVersion ?: precisePreReleaseApkVersion
        val localPackage = localPackageName.trim()
        val remotePackage = remoteApk?.packageName.orEmpty().trim()
        val packageMatched = localPackage.isNotBlank() &&
                remotePackage.isNotBlank() &&
                localPackage.equals(remotePackage, ignoreCase = true)
        val packageMismatchKnown =
            localPackage.isNotBlank() && remotePackage.isNotBlank() && !packageMatched
        return GitHubRepositoryLocalFitProfile(
            localPackageName = stringField(
                localPackage,
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            ),
            remotePackageName = stringField(
                remotePackage,
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            ),
            packageNameMatched = when {
                packageMatched -> booleanField(
                    true,
                    GitHubRepositoryProfileSource.LocalInstall,
                    fetchedAtMillis
                )

                packageMismatchKnown -> booleanField(
                    false,
                    GitHubRepositoryProfileSource.LocalInstall,
                    fetchedAtMillis
                )

                else -> null
            },
            localVersionName = stringField(
                localVersionName,
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            ),
            remoteVersionName = stringField(
                remoteApk?.versionName.orEmpty(),
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            ),
            localVersionCode = longField(
                localVersionCode,
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            ),
            remoteVersionCode = longField(
                remoteApk?.versionCodeLong ?: -1L,
                GitHubRepositoryProfileSource.LocalInstall,
                fetchedAtMillis
            )
        )
    }
}
