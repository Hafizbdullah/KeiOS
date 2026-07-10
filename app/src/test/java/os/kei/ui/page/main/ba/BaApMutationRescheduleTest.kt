package os.kei.ui.page.main.ba

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class BaApMutationRescheduleTest {
    @Test
    fun `ordinary AP edit persists before reschedule`() = runTest {
        assertPersistencePrecedesSchedule()
    }

    @Test
    fun `cafe AP edit persists before reschedule`() = runTest {
        assertPersistencePrecedesSchedule()
    }

    @Test
    fun `cafe AP claim persists before reschedule`() = runTest {
        assertPersistencePrecedesSchedule()
    }

    @Test
    fun `AP limit update persists before reschedule`() = runTest {
        assertPersistencePrecedesSchedule()
    }

    @Test
    fun `cafe calibration persists before reschedule`() = runTest {
        assertPersistencePrecedesSchedule()
    }

    private suspend fun assertPersistencePrecedesSchedule() {
        val events = mutableListOf<String>()

        persistBaApMutationAndReschedule(
            persist = { events += "persist" },
            schedule = { events += "schedule" },
        )

        assertEquals(listOf("persist", "schedule"), events)
    }
}
