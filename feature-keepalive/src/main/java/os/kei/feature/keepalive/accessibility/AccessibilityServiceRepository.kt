package os.kei.feature.keepalive.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers

class AccessibilityServiceRepository(
    private val derivationDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
) {
    suspend fun listInstalledServices(
        context: Context,
        guardedIds: Set<AccessibilityServiceId> = emptySet(),
    ): List<AccessibilityServiceSnapshot> =
        withContext(derivationDispatcher) {
            val enabledIds = readEnabledServiceIds(context)
            val installed = listInstalledServiceSnapshots(context)
            deriveEnabledState(
                installed = installed,
                enabledIds = enabledIds,
                guardedIds = guardedIds,
            )
        }

    fun readEnabledServiceIds(context: Context): Set<AccessibilityServiceId> {
        val raw =
            runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                )
            }.getOrNull().orEmpty()
        return parseAccessibilityServiceIds(raw)
    }
}

fun deriveEnabledState(
    installed: List<AccessibilityServiceSnapshot>,
    enabledIds: Set<AccessibilityServiceId>,
    guardedIds: Set<AccessibilityServiceId>,
): List<AccessibilityServiceSnapshot> =
    installed
        .asSequence()
        .map { snapshot ->
            snapshot.copy(
                enabled = snapshot.id in enabledIds,
                guarded = snapshot.id in guardedIds,
                installed = true,
            )
        }
        .sortedWith(
            compareBy<AccessibilityServiceSnapshot> { it.id.packageName }
                .thenBy { it.id.serviceName },
        )
        .toList()

private fun listInstalledServiceSnapshots(context: Context): List<AccessibilityServiceSnapshot> {
    val appContext = context.applicationContext
    val packageManager = appContext.packageManager
    val accessibilityManager =
        appContext.getSystemService(AccessibilityManager::class.java) ?: return emptyList()
    return runCatching { accessibilityManager.installedAccessibilityServiceList }
        .getOrDefault(emptyList())
        .mapNotNull { info -> info.toSnapshot(packageManager) }
}

private fun AccessibilityServiceInfo.toSnapshot(
    packageManager: android.content.pm.PackageManager,
): AccessibilityServiceSnapshot? {
    val serviceInfo = resolveInfo?.serviceInfo ?: return null
    val packageName = serviceInfo.packageName?.trim().orEmpty()
    val serviceName = serviceInfo.name?.trim().orEmpty()
    if (packageName.isBlank() || serviceName.isBlank()) return null
    val id = AccessibilityServiceId(
        packageName = packageName,
        serviceName = serviceName,
    )
    val appInfo = serviceInfo.applicationInfo
    return AccessibilityServiceSnapshot(
        id = id,
        label =
            runCatching { resolveInfo.loadLabel(packageManager).toString().trim() }
                .getOrDefault("")
                .ifBlank { serviceInfo.name.substringAfterLast('.') },
        packageLabel =
            runCatching { appInfo?.loadLabel(packageManager)?.toString()?.trim().orEmpty() }
                .getOrDefault("")
                .ifBlank { packageName },
        enabled = false,
        guarded = false,
        installed = true,
        system = appInfo?.isSystemApp() == true,
    )
}

private fun ApplicationInfo.isSystemApp(): Boolean =
    (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
