package os.kei.feature.keepalive.accessibility

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccessibilityGuardCoordinatorTest {
    private val alpha = AccessibilityServiceId("com.alpha", "com.alpha.Service")
    private val beta = AccessibilityServiceId("com.beta", "com.beta.Service")

    @Test
    fun `restore skips when no services are selected`() = runTest {
        val bridge = FakeSecureSettingsBridge(enabledIds = setOf(alpha))
        val coordinator = coordinator(bridge = bridge)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.SkippedNoTargets, result.status)
        assertTrue(bridge.writes.isEmpty())
    }

    @Test
    fun `restore skips with missing privilege when secure settings cannot be read`() = runTest {
        val store = InMemoryGuardStore(AccessibilityGuardSettings(guardedIds = setOf(alpha)))
        val bridge = FakeSecureSettingsBridge(readSuccess = false, readReason = "permission denied")
        val coordinator = coordinator(store = store, bridge = bridge)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.SkippedMissingPrivilege, result.status)
        assertEquals("permission denied", result.failureReason)
        assertEquals(setOf(alpha), result.skippedIds)
        assertTrue(bridge.writes.isEmpty())
    }

    @Test
    fun `restore skips when selected services are already enabled`() = runTest {
        val store = InMemoryGuardStore(AccessibilityGuardSettings(guardedIds = setOf(alpha)))
        val bridge = FakeSecureSettingsBridge(enabledIds = setOf(alpha, beta))
        val coordinator = coordinator(store = store, bridge = bridge)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.SkippedAlreadyEnabled, result.status)
        assertEquals(setOf(alpha, beta), result.beforeEnabledIds)
        assertTrue(bridge.writes.isEmpty())
    }

    @Test
    fun `restore preserves unrelated enabled services while adding missing selected services`() = runTest {
        val store = InMemoryGuardStore(AccessibilityGuardSettings(guardedIds = setOf(alpha)))
        val bridge = FakeSecureSettingsBridge(enabledIds = setOf(beta))
        val coordinator = coordinator(store = store, bridge = bridge, nowMs = 10_000L)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.Restored, result.status)
        assertEquals(setOf(alpha), result.restoredIds)
        assertEquals(setOf(alpha, beta), bridge.writes.single())
        assertEquals(
            10_000L + AccessibilityGuardCoordinator.SUCCESS_COOLDOWN_MS,
            store.loadSettings().cooldownUntilById[alpha],
        )
    }

    @Test
    fun `restore skips missing services that are still cooling down`() = runTest {
        val store =
            InMemoryGuardStore(
                AccessibilityGuardSettings(
                    guardedIds = setOf(alpha),
                    cooldownUntilById = mapOf(alpha to 20_000L),
                ),
            )
        val bridge = FakeSecureSettingsBridge(enabledIds = emptySet())
        val coordinator = coordinator(store = store, bridge = bridge, nowMs = 10_000L)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.SkippedCooldown, result.status)
        assertEquals(setOf(alpha), result.skippedIds)
        assertTrue(bridge.writes.isEmpty())
    }

    @Test
    fun `restore records long cooldown after repeated failure`() = runTest {
        val store =
            InMemoryGuardStore(
                AccessibilityGuardSettings(
                    guardedIds = setOf(alpha),
                    failureCountById = mapOf(alpha to 1),
                ),
            )
        val bridge =
            FakeSecureSettingsBridge(
                enabledIds = emptySet(),
                writeSuccess = false,
                writeReason = "write denied",
            )
        val coordinator = coordinator(store = store, bridge = bridge, nowMs = 10_000L)

        val result = coordinator.restoreMissing(AccessibilityGuardRestoreReason.Manual)

        assertEquals(AccessibilityGuardRestoreStatus.Failed, result.status)
        assertEquals("write denied", result.failureReason)
        assertEquals(2, store.loadSettings().failureCountById[alpha])
        assertEquals(
            10_000L + AccessibilityGuardCoordinator.REPEATED_FAILURE_COOLDOWN_MS,
            store.loadSettings().cooldownUntilById[alpha],
        )
    }

    @Test
    fun `set guarded clears stale cooldown and failure count`() {
        val store =
            InMemoryGuardStore(
                AccessibilityGuardSettings(
                    guardedIds = setOf(alpha),
                    cooldownUntilById = mapOf(alpha to 20_000L),
                    failureCountById = mapOf(alpha to 3),
                ),
            )
        val coordinator = coordinator(store = store)

        val settings = coordinator.setGuarded(alpha, guarded = false)

        assertTrue(alpha !in settings.guardedIds)
        assertTrue(alpha !in settings.cooldownUntilById)
        assertTrue(alpha !in settings.failureCountById)
    }

    private fun coordinator(
        store: AccessibilityGuardStateStore = InMemoryGuardStore(),
        bridge: AccessibilitySecureSettingsBridge = FakeSecureSettingsBridge(),
        nowMs: Long = 1_000L,
    ): AccessibilityGuardCoordinator =
        AccessibilityGuardCoordinator(
            serviceRepository = AccessibilityServiceRepository(),
            secureSettingsBridge = bridge,
            stateStore = store,
            wallClockMs = { nowMs },
            elapsedClockMs = { nowMs },
        )

    private class InMemoryGuardStore(
        private var settings: AccessibilityGuardSettings = AccessibilityGuardSettings(),
    ) : AccessibilityGuardStateStore {
        override fun loadSettings(): AccessibilityGuardSettings = settings

        override fun saveSettings(settings: AccessibilityGuardSettings) {
            this.settings = settings
        }
    }

    private class FakeSecureSettingsBridge(
        private val enabledIds: Set<AccessibilityServiceId> = emptySet(),
        private val readSuccess: Boolean = true,
        private val readReason: String = "",
        private val writeSuccess: Boolean = true,
        private val writeReason: String = "",
    ) : AccessibilitySecureSettingsBridge {
        val writes = mutableListOf<Set<AccessibilityServiceId>>()

        override suspend fun readEnabledServiceIds(): AccessibilitySecureSettingRead =
            AccessibilitySecureSettingRead(
                rawValue = enabledIds.joinToString(":") { id -> "${id.packageName}/${id.serviceName}" },
                ids = enabledIds,
                success = readSuccess,
                reason = readReason,
            )

        override suspend fun writeEnabledServiceIds(ids: Set<AccessibilityServiceId>): AccessibilitySecureSettingWrite {
            writes += ids
            return AccessibilitySecureSettingWrite(
                success = writeSuccess,
                changed = writeSuccess,
                reason = writeReason,
            )
        }

        override suspend fun setAccessibilityEnabled(enabled: Boolean): AccessibilitySecureSettingWrite =
            AccessibilitySecureSettingWrite(
                success = writeSuccess,
                changed = writeSuccess,
                reason = writeReason,
            )
    }
}
