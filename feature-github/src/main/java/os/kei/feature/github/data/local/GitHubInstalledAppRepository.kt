package os.kei.feature.github.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.util.Locale
import os.kei.core.system.HyperOsSettingsIntents
import os.kei.core.system.getInstalledPackageInfosSafely
import os.kei.feature.github.model.GitHubLocalVersionInfo
import os.kei.feature.github.model.InstalledAppItem

object GitHubInstalledAppRepository {
    private const val INSTALLED_APPS_CACHE_TTL_MS = 5L * 60L * 1000L

    @Volatile
    private var installedAppsCache: CachedInstalledApps? = null

    fun buildAppListPermissionIntent(context: Context): Intent? {
        return HyperOsSettingsIntents.buildAppListPermissionIntent(context)
    }

    fun queryInstalledLaunchableApps(
        context: Context,
        forceRefresh: Boolean = false,
        ttlMs: Long = INSTALLED_APPS_CACHE_TTL_MS,
        includeSystemApps: Boolean = true,
        pinnedSystemPackageNames: Set<String> = emptySet(),
        requiredPackageNames: Set<String> = emptySet(),
    ): List<InstalledAppItem> {
        val now = System.currentTimeMillis()
        val normalizedPinnedSystemPackages = pinnedSystemPackageNames.normalizedPackageNameSet()
        val normalizedRequiredPackages = requiredPackageNames.normalizedPackageNameSet()
        if (!forceRefresh) {
            installedAppsCache?.takeIf { cache ->
                (now - cache.updatedAtMs).coerceAtLeast(0L) < ttlMs.coerceAtLeast(0L) &&
                    cache.canServeScope(
                        includeSystemApps = includeSystemApps,
                        pinnedSystemPackageNames = normalizedPinnedSystemPackages,
                    ) &&
                    cache.containsPackages(normalizedRequiredPackages)
            }?.let { cache ->
                return filterByScanScope(
                    apps = cache.apps,
                    includeSystemApps = includeSystemApps,
                    pinnedSystemPackageNames = normalizedPinnedSystemPackages,
                )
            }
        }

        val packageManager = context.packageManager
        val overlayFlagMask = installedAppResourceOverlayFlagMask()
        val installSourceLabelCache = mutableMapOf<String, String>()
        val labelSortLocale = Locale.getDefault()
        val scannedApps = packageManager.getInstalledPackageInfosSafely()
            .asSequence()
            .mapNotNull { packageInfo ->
                packageManager.toInstalledAppItem(
                    packageInfo = packageInfo,
                    overlayFlagMask = overlayFlagMask,
                    installSourceLabelCache = installSourceLabelCache,
                )
            }
            .toMutableList()
        val scannedPackages =
            scannedApps.mapTo(HashSet()) { app -> app.packageName.lowercase(Locale.ROOT) }
        normalizedRequiredPackages.forEach { packageName ->
            if (packageName in scannedPackages) return@forEach
            packageManager.installedAppItemOrNull(
                packageName = packageName,
                overlayFlagMask = overlayFlagMask,
                installSourceLabelCache = installSourceLabelCache,
            )?.let { app ->
                scannedApps += app
                scannedPackages += packageName
            }
        }
        val apps = scannedApps
            .asSequence()
            .filter { app ->
                includeSystemApps ||
                    !app.isSystemApp ||
                    app.packageName.lowercase(Locale.ROOT) in normalizedPinnedSystemPackages
            }
            .map { item ->
                InstalledAppSortEntry(
                    item = item,
                    labelSortKey = item.label.lowercase(labelSortLocale),
                    packageSortKey = item.packageName.lowercase(Locale.ROOT),
                )
            }
            .distinctBy { entry -> entry.item.packageName.lowercase(Locale.ROOT) }
            .sortedWith(
                compareBy<InstalledAppSortEntry> { entry -> entry.labelSortKey }
                    .thenBy { entry -> entry.packageSortKey },
            )
            .map { entry -> entry.item }
            .toList()
        installedAppsCache = CachedInstalledApps(
            updatedAtMs = now,
            apps = apps,
            includesSystemApps = includeSystemApps,
            pinnedSystemPackageNames = normalizedPinnedSystemPackages,
            reconciledPackageNames = normalizedRequiredPackages,
        )
        return apps
    }

    internal fun filterByScanScope(
        apps: List<InstalledAppItem>,
        includeSystemApps: Boolean,
        pinnedSystemPackageNames: Set<String>,
    ): List<InstalledAppItem> {
        if (includeSystemApps) return apps
        val normalizedPinnedSystemPackages = pinnedSystemPackageNames.normalizedPackageNameSet()
        return apps.filter { app ->
            !app.isSystemApp ||
                app.packageName.lowercase(Locale.ROOT) in normalizedPinnedSystemPackages
        }
    }

    fun invalidateCache() {
        installedAppsCache = null
    }

    fun localVersionName(context: Context, packageName: String): String {
        return localVersionInfoOrNull(context, packageName)?.versionName.orEmpty()
            .ifBlank { "unknown" }
    }

    fun localVersionCode(context: Context, packageName: String): Long {
        return localVersionInfoOrNull(context, packageName)?.versionCode ?: -1L
    }

    fun localVersionInfoOrNull(
        context: Context,
        packageName: String,
    ): GitHubLocalVersionInfo? {
        val packageManager = context.packageManager
        val packageInfo = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }.getOrElse { error ->
            if (error is PackageManager.NameNotFoundException) return null
            throw error
        }
        val appInfo = packageInfo.applicationInfo
            ?: packageManager.getApplicationInfoCompat(packageName)
        val overlayFlagMask = installedAppResourceOverlayFlagMask()
        if (appInfo != null && shouldIgnoreInstalledApp(appInfo, overlayFlagMask)) return null
        return GitHubLocalVersionInfo(
            versionName = packageInfo.versionName?.trim().orEmpty().ifBlank { "unknown" },
            versionCode = packageInfo.longVersionCode,
            isSystemApp = appInfo?.isSystemAppForPicker() == true,
        )
    }

    private data class CachedInstalledApps(
        val updatedAtMs: Long,
        val apps: List<InstalledAppItem>,
        val includesSystemApps: Boolean,
        val pinnedSystemPackageNames: Set<String>,
        val reconciledPackageNames: Set<String>,
    ) {
        fun canServeScope(
            includeSystemApps: Boolean,
            pinnedSystemPackageNames: Set<String>,
        ): Boolean {
            if (includesSystemApps) return true
            if (includeSystemApps) return false
            return pinnedSystemPackageNames.all { packageName ->
                packageName in this.pinnedSystemPackageNames
            }
        }

        fun containsPackages(packageNames: Set<String>): Boolean {
            if (packageNames.isEmpty()) return true
            val cachedPackages = apps.mapTo(HashSet()) { app ->
                app.packageName.lowercase(Locale.ROOT)
            }
            return packageNames.all { packageName ->
                packageName in cachedPackages || packageName in reconciledPackageNames
            }
        }
    }

    private data class InstalledAppSortEntry(
        val item: InstalledAppItem,
        val labelSortKey: String,
        val packageSortKey: String,
    )

    private fun PackageManager.installedAppItemOrNull(
        packageName: String,
        overlayFlagMask: Int,
        installSourceLabelCache: MutableMap<String, String>,
    ): InstalledAppItem? {
        val packageInfo = runCatching {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }.getOrNull() ?: return null
        return toInstalledAppItem(
            packageInfo = packageInfo,
            overlayFlagMask = overlayFlagMask,
            installSourceLabelCache = installSourceLabelCache,
        )
    }

    private fun PackageManager.toInstalledAppItem(
        packageInfo: PackageInfo,
        overlayFlagMask: Int,
        installSourceLabelCache: MutableMap<String, String>,
    ): InstalledAppItem? {
        val packageName = packageInfo.packageName.trim()
        if (packageName.isBlank()) return null
        val appInfo =
            packageInfo.applicationInfo ?: getApplicationInfoCompat(packageName) ?: return null
        if (shouldIgnoreInstalledApp(appInfo, overlayFlagMask)) return null
        val label = runCatching {
            getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName).trim().ifBlank { packageName }
        val installSource = resolveInstallSource(
            packageName = packageName,
            labelCache = installSourceLabelCache,
        )
        return InstalledAppItem(
            label = label,
            packageName = packageName,
            firstInstallTimeMs = packageInfo.firstInstallTime,
            lastUpdateTimeMs = packageInfo.lastUpdateTime,
            isSystemApp = appInfo.isSystemAppForPicker(),
            installSourcePackageName = installSource.packageName,
            installSourceLabel = installSource.label,
        )
    }
}

private fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo? {
    return runCatching {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    }.getOrNull()
}

private fun Set<String>.normalizedPackageNameSet(): Set<String> {
    return mapNotNullTo(LinkedHashSet()) { packageName ->
        packageName.trim().lowercase(Locale.ROOT).takeIf { it.isNotBlank() }
    }
}

private data class InstalledAppInstallSource(
    val packageName: String,
    val label: String,
)

private fun PackageManager.resolveInstallSource(
    packageName: String,
    labelCache: MutableMap<String, String>,
): InstalledAppInstallSource {
    val sourcePackageName = runCatching {
        val sourceInfo = getInstallSourceInfo(packageName)
        sourceInfo.installingPackageName
            ?: sourceInfo.initiatingPackageName
            ?: sourceInfo.originatingPackageName
    }.getOrNull()?.trim().orEmpty()
    if (sourcePackageName.isBlank()) return InstalledAppInstallSource("", "")
    val sourceLabel = labelCache.getOrPut(sourcePackageName) {
        runCatching {
            val sourceInfo = getApplicationInfoCompat(sourcePackageName)
            if (sourceInfo == null) sourcePackageName else getApplicationLabel(sourceInfo).toString()
        }.getOrDefault(sourcePackageName).trim().ifBlank { sourcePackageName }
    }
    return InstalledAppInstallSource(
        packageName = sourcePackageName,
        label = sourceLabel,
    )
}

private fun installedAppResourceOverlayFlagMask(): Int {
    return runCatching {
        ApplicationInfo::class.java.getField("FLAG_IS_RESOURCE_OVERLAY").getInt(null)
    }.getOrDefault(0)
}

private fun shouldIgnoreInstalledApp(
    appInfo: ApplicationInfo,
    overlayFlagMask: Int,
): Boolean {
    if (!appInfo.enabled) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_INSTALLED) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_HAS_CODE) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0) return true
    if (overlayFlagMask != 0 && (appInfo.flags and overlayFlagMask) != 0) return true
    return false
}

private fun ApplicationInfo.isSystemAppForPicker(): Boolean {
    return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
