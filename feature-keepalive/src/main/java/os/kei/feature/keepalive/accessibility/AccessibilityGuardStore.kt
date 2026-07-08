package os.kei.feature.keepalive.accessibility

import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv

data class AccessibilityGuardSettings(
    val guardedIds: Set<AccessibilityServiceId> = emptySet(),
    val daemonEnabled: Boolean = false,
    val bootRestoreEnabled: Boolean = false,
    val screenOnCheckEnabled: Boolean = false,
    val cooldownUntilById: Map<AccessibilityServiceId, Long> = emptyMap(),
    val failureCountById: Map<AccessibilityServiceId, Int> = emptyMap(),
)

data class AccessibilityGuardSnapshot(
    val settings: AccessibilityGuardSettings,
    val services: List<AccessibilityServiceSnapshot>,
)

interface AccessibilityGuardStateStore {
    fun loadSettings(): AccessibilityGuardSettings

    fun saveSettings(settings: AccessibilityGuardSettings)
}

class AccessibilityGuardStore(
    private val store: MMKV = KeiMmkv.byId(KV_ID),
) : AccessibilityGuardStateStore {
    override fun loadSettings(): AccessibilityGuardSettings =
        AccessibilityGuardSettings(
            guardedIds = decodeIds(store.decodeString(KEY_GUARDED_IDS, "").orEmpty()),
            daemonEnabled = store.decodeBool(KEY_DAEMON_ENABLED, false),
            bootRestoreEnabled = store.decodeBool(KEY_BOOT_RESTORE_ENABLED, false),
            screenOnCheckEnabled = store.decodeBool(KEY_SCREEN_ON_CHECK_ENABLED, false),
            cooldownUntilById = decodeLongMap(store.decodeString(KEY_COOLDOWN_UNTIL_BY_ID, "").orEmpty()),
            failureCountById = decodeIntMap(store.decodeString(KEY_FAILURE_COUNT_BY_ID, "").orEmpty()),
        )

    override fun saveSettings(settings: AccessibilityGuardSettings) {
        store.encode(KEY_GUARDED_IDS, encodeIds(settings.guardedIds))
        store.encode(KEY_DAEMON_ENABLED, settings.daemonEnabled)
        store.encode(KEY_BOOT_RESTORE_ENABLED, settings.bootRestoreEnabled)
        store.encode(KEY_SCREEN_ON_CHECK_ENABLED, settings.screenOnCheckEnabled)
        store.encode(KEY_COOLDOWN_UNTIL_BY_ID, encodeLongMap(settings.cooldownUntilById))
        store.encode(KEY_FAILURE_COUNT_BY_ID, encodeIntMap(settings.failureCountById))
    }

    companion object {
        private const val KV_ID = "accessibility_guard"
        private const val KEY_GUARDED_IDS = "guarded_ids"
        private const val KEY_DAEMON_ENABLED = "daemon_enabled"
        private const val KEY_BOOT_RESTORE_ENABLED = "boot_restore_enabled"
        private const val KEY_SCREEN_ON_CHECK_ENABLED = "screen_on_check_enabled"
        private const val KEY_COOLDOWN_UNTIL_BY_ID = "cooldown_until_by_id"
        private const val KEY_FAILURE_COUNT_BY_ID = "failure_count_by_id"
    }
}

private fun encodeIds(ids: Set<AccessibilityServiceId>): String =
    ids
        .sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .joinToString(separator = "\n") { it.flatten() }

private fun decodeIds(raw: String): Set<AccessibilityServiceId> =
    raw
        .lineSequence()
        .mapNotNull { line -> line.toAccessibilityServiceIdOrNull() }
        .distinct()
        .sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .toCollection(LinkedHashSet())

private fun encodeLongMap(values: Map<AccessibilityServiceId, Long>): String =
    values
        .filterValues { value -> value > 0L }
        .toSortedMap(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .entries
        .joinToString(separator = "\n") { (id, value) -> "${id.flatten()}=$value" }

private fun decodeLongMap(raw: String): Map<AccessibilityServiceId, Long> =
    raw
        .lineSequence()
        .mapNotNull { line ->
            val id = line.substringBefore('=').toAccessibilityServiceIdOrNull() ?: return@mapNotNull null
            val value = line.substringAfter('=', "").toLongOrNull()?.takeIf { it > 0L } ?: return@mapNotNull null
            id to value
        }
        .toMap()

private fun encodeIntMap(values: Map<AccessibilityServiceId, Int>): String =
    values
        .filterValues { value -> value > 0 }
        .toSortedMap(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .entries
        .joinToString(separator = "\n") { (id, value) -> "${id.flatten()}=$value" }

private fun decodeIntMap(raw: String): Map<AccessibilityServiceId, Int> =
    raw
        .lineSequence()
        .mapNotNull { line ->
            val id = line.substringBefore('=').toAccessibilityServiceIdOrNull() ?: return@mapNotNull null
            val value = line.substringAfter('=', "").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            id to value
        }
        .toMap()
