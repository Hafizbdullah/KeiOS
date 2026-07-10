package os.kei.ui.page.main.ba

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class BaApMutationRescheduleTest {
    @Test
    fun `ordinary AP edit callback persists account update before reschedule`() = runTest {
        val fixture = actionFixture()

        fixture.actions.onApCurrentInputChange("123")
        fixture.actions.onApCurrentDone()
        advanceUntilIdle()

        val update = assertNotNull(fixture.persistedUpdates.singleOrNull())
        assertEquals(ACCOUNT_ID, update.accountId)
        assertEquals(123.0, update.apCurrent)
        assertEquals(NOW_MS, update.apSyncMs)
        assertEquals(listOf("persist", "schedule"), fixture.events)
    }

    @Test
    fun `cafe AP edit callback persists account update before reschedule`() = runTest {
        val fixture = actionFixture()

        fixture.actions.onCafeStoredApInputChange("42.5")
        fixture.actions.onCafeStoredApDone()
        advanceUntilIdle()

        val update = assertNotNull(fixture.persistedUpdates.singleOrNull())
        assertEquals(ACCOUNT_ID, update.accountId)
        assertEquals(42.5, update.cafeStoredAp)
        assertEquals(-1, update.cafeApLastNotifiedLevel)
        assertEquals(listOf("persist", "schedule"), fixture.events)
    }

    @Test
    fun `cafe claim callback persists account update before reschedule`() = runTest {
        val fixture = actionFixture()

        fixture.actions.onClaimCafeStoredAp()
        advanceUntilIdle()

        val update = assertNotNull(fixture.persistedUpdates.singleOrNull())
        assertEquals(ACCOUNT_ID, update.accountId)
        assertEquals(150.0, update.apCurrent)
        assertEquals(0.0, update.cafeStoredAp)
        assertEquals(-1, update.cafeApLastNotifiedLevel)
        assertEquals(listOf("persist", "schedule"), fixture.events)
    }

    @Test
    fun `AP limit save persists limit and account runtime before reschedule`() = runTest {
        val events = mutableListOf<String>()
        val limits = mutableListOf<Int>()
        val updates = mutableListOf<BaRuntimePersistenceUpdate>()
        val coordinator =
            BaPageSheetApMutationPersistenceCoordinator(
                accountIdProvider = { ACCOUNT_ID },
                saveApLimit = { limit ->
                    events += "limit"
                    limits += limit
                },
                persistRuntimeUpdate = { update ->
                    events += "runtime"
                    updates += update
                },
                scheduleBaApThreshold = { events += "schedule" },
            )

        coordinator.persistApLimit(
            limit = 300,
            runtimeUpdate = BaRuntimePersistenceUpdate(apCurrent = 240.0),
        )

        assertEquals(listOf(300), limits)
        assertEquals(ACCOUNT_ID, updates.single().accountId)
        assertEquals(240.0, updates.single().apCurrent)
        assertEquals(listOf("limit", "runtime", "schedule"), events)
    }

    @Test
    fun `cafe calibration persists account update before reschedule`() = runTest {
        val events = mutableListOf<String>()
        val updates = mutableListOf<BaRuntimePersistenceUpdate>()
        val coordinator =
            BaPageSheetApMutationPersistenceCoordinator(
                accountIdProvider = { ACCOUNT_ID },
                saveApLimit = {},
                persistRuntimeUpdate = { update ->
                    events += "runtime"
                    updates += update
                },
                scheduleBaApThreshold = { events += "schedule" },
            )

        coordinator.persistCafeCalibration(
            BaRuntimePersistenceUpdate(
                cafeStoredAp = 0.0,
                cafeApLastNotifiedLevel = -1,
            ),
        )

        assertEquals(ACCOUNT_ID, updates.single().accountId)
        assertEquals(0.0, updates.single().cafeStoredAp)
        assertEquals(-1, updates.single().cafeApLastNotifiedLevel)
        assertEquals(listOf("runtime", "schedule"), events)
    }

    private fun TestScope.actionFixture(): ActionFixture {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val office =
            BaOfficeController(
                snapshot =
                    BaPageSnapshot(
                        apCurrent = 100.0,
                        apRegenBaseMs = NOW_MS,
                        apSyncMs = NOW_MS,
                        apLimit = 240,
                        cafeLevel = 10,
                        cafeStoredAp = 50.0,
                        cafeLastHourMs = NOW_MS,
                    ),
                clock = TestClock,
            )
        val events = mutableListOf<String>()
        val persistedUpdates = mutableListOf<BaRuntimePersistenceUpdate>()
        val coordinator =
            BaOfficeActionCoordinator(
                context = context,
                office = office,
                scope = this,
                serverIndexProvider = { 2 },
                accountIdProvider = { ACCOUNT_ID },
                onSettingsCafeLevelChange = {},
                onCafeLevelPopupAnchorBoundsChange = { _: IntRect? -> },
                onCafeLevelPopupChange = {},
                onOpenApLimitTools = {},
                onOpenCafeApTools = {},
                onOpenCafeCooldownEditSheet = {},
                onAccountSelected = {},
                onEditAccount = {},
                onRefreshCalendar = {},
                onRefreshPool = {},
                onOpenCalendarLink = {},
                onOpenPoolStudentGuide = {},
                persistRuntimeUpdate = { update ->
                    events += "persist"
                    persistedUpdates += update
                },
                scheduleBaApThreshold = { events += "schedule" },
            )
        return ActionFixture(
            actions = coordinator.buildContentActions(),
            events = events,
            persistedUpdates = persistedUpdates,
        )
    }

    private data class ActionFixture(
        val actions: BaPageContentActions,
        val events: MutableList<String>,
        val persistedUpdates: MutableList<BaRuntimePersistenceUpdate>,
    )

    private object TestClock : BaOfficeClock {
        override fun nowMs(): Long = NOW_MS
    }

    private companion object {
        val ACCOUNT_ID = BaAccountId("cn-main")
        const val NOW_MS = 20_000_000L
    }
}
