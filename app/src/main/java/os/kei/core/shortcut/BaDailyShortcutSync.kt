package os.kei.core.shortcut

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.tile.BaDailyTileManager
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BASettingsStoreSignals
import os.kei.ui.page.main.ba.support.baDailyTileFingerprint

/**
 * Keeps the daily-done tiles and launcher shortcuts in step with the account list.
 *
 * The only change signal available is [BASettingsStoreSignals.version], and it is bumped by *every* BA
 * write — including each AP regeneration tick and every reminder marker. Re-labelling on all of those
 * would be constant churn against system storage, so this collapses them by comparing
 * [baDailyTileFingerprint], which covers exactly what a tile or shortcut renders: id, display name,
 * server and enabled state.
 *
 * Hooked at the signal rather than in the ViewModel on purpose. The WebDAV sync merge can add, rename,
 * delete and reorder accounts in one shot without the BA ViewModel ever existing, so a ViewModel-level
 * hook would miss it.
 */
internal object BaDailyShortcutSync {
    private const val TAG = "BaDailySync"

    @Volatile
    private var lastFingerprint: String? = null

    fun start(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch(AppDispatchers.fileIo) {
            BASettingsStoreSignals.version.collectLatest {
                runCatching { syncIfChanged(appContext) }
                    .onFailure { throwable -> AppLogger.e(TAG, "daily shortcut sync failed", throwable) }
            }
        }
    }

    /** Forces a rebuild, for when the teacher changes the mode from the settings sheet. */
    suspend fun resync(context: Context) {
        val appContext = context.applicationContext
        withContext(AppDispatchers.fileIo) {
            lastFingerprint = null
            runCatching { syncIfChanged(appContext) }
                .onFailure { throwable -> AppLogger.e(TAG, "daily shortcut resync failed", throwable) }
        }
    }

    private fun syncIfChanged(context: Context) {
        val accountState = BASettingsStore.loadAccountState()
        val fingerprint = baDailyTileFingerprint(accountState)
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint
        BaDailyTileManager.syncWithAccounts(
            context = context,
            existingAccountIds = accountState.accounts.map { it.profile.id },
        )
        AppShortcuts.sync(context)
        AppLogger.i(TAG) { "re-synced daily tiles and shortcuts" }
    }
}
