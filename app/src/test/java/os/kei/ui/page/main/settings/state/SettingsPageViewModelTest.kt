package os.kei.ui.page.main.settings.state

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.settings.section.SettingsAccessibilityGuardUiState

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `accessibility guard refresh cancellation is not surfaced as failure toast`() =
        runTest {
            val viewModel =
                SettingsPageViewModel(
                    repository = CancellingAccessibilityGuardRepository(),
                    initialExpandedCards = emptyMap(),
                    initialWebDavSyncState = SettingsWebDavSyncUiState(),
                )
            val events = mutableListOf<SettingsPageEvent>()
            val collectJob =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { event -> events += event }
                }

            assertFailsWith<CancellationException> {
                viewModel.refreshAccessibilityGuardNow(ApplicationProvider.getApplicationContext())
            }
            advanceUntilIdle()

            assertTrue(events.isEmpty())
            collectJob.cancel()
        }
}

private class CancellingAccessibilityGuardRepository : SettingsPageRepository() {
    override suspend fun loadWebDavSyncState(): SettingsWebDavSyncUiState = SettingsWebDavSyncUiState()

    override suspend fun loadAccessibilityGuardState(context: Context): SettingsAccessibilityGuardUiState {
        throw CancellationException("refresh superseded")
    }
}
