package os.kei.core.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.R
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.ui.page.main.ba.BaDailyDoneNotificationDispatcher
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaDailyDoneOutcome
import os.kei.ui.page.main.ba.support.accountIdAt

/**
 * One-tap "dailies done" from the quick settings panel.
 *
 * A tile is a manifest-declared service and cannot be created at runtime, so the per-account variants are
 * a fixed pool of subclasses that read their binding from [BASettingsStore]. Every one of them ships
 * `android:enabled="false"`: until the teacher claims it from the BA settings sheet the component does not
 * exist as far as the system is concerned, so it never appears in the quick-settings editor.
 *
 * The label is resolved on every [onStartListening] rather than cached, which is what makes renaming an
 * account re-label its tile with no bookkeeping at all.
 */
internal abstract class BaDailyDoneTileServiceBase : TileService() {
    /** `null` for the all-accounts tile; otherwise the pool slot this subclass owns. */
    protected abstract val slot: Int?

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        // Briefly unavailable so a double tap cannot queue a second pass; onStartListening restores the
        // real state when the panel next binds.
        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            updateTile()
        }
        scope.launch {
            val result = withContext(AppDispatchers.baFetch) { runCatching { apply() } }
            result
                .onSuccess(::report)
                .onFailure { throwable ->
                    // Log only: a toast from here would be dropped for the same background reason as the
                    // success path, and a failure notification for a template that changed nothing is noise.
                    AppLogger.e(TAG, "daily done failed slot=$slot", throwable)
                }
            refreshTile()
        }
    }

    private fun apply(): Map<BaAccountId, BaDailyDoneOutcome> =
        when (val targets = resolveTargets()) {
            Targets.Unbound -> emptyMap()
            Targets.AllEnabled -> BASettingsStore.applyDailyDone(accountIds = null)
            is Targets.Only -> BASettingsStore.applyDailyDone(accountIds = targets.accountIds)
        }

    /**
     * A null `accountIds` filter means "every enabled account" to the store, which is not the same as a
     * per-account tile that is bound to nothing — hence a named result instead of a nullable list.
     */
    private fun resolveTargets(): Targets {
        val currentSlot = slot ?: return Targets.AllEnabled
        val bound = BASettingsStore.loadDailyTileState().accountIdAt(currentSlot) ?: return Targets.Unbound
        return Targets.Only(listOf(bound))
    }

    /**
     * Reports through a notification, not a toast.
     *
     * Android 12 and up drop toasts from a background app, and a tile click is not foreground — verified
     * on the API 37 AVD, where the template applied correctly but no toast ever appeared. The tile's own
     * subtitle is not a fallback either: at the icon-only size the panel renders neither label nor
     * subtitle.
     *
     * Nothing is posted when nothing changed, so a stray tap stays silent.
     */
    private fun report(outcomes: Map<BaAccountId, BaDailyDoneOutcome>) {
        if (outcomes.isEmpty()) return
        val changedEntries = outcomes.filter { it.value.changedAnything }
        if (changedEntries.isEmpty()) return
        BaDailyDoneNotificationDispatcher.send(
            context = this,
            changedAccounts = changedEntries.size,
            craftSlotsStarted = outcomes.values.sumOf { it.craftSlotsStarted },
            // Derived from the outcome rather than from `slot`, because the all-accounts tile can also
            // end up having changed exactly one account — only one was enabled, or only one had anything
            // left to do — and that run has just as single a destination as a per-account tile's.
            targetAccountId = changedEntries.keys.singleOrNull(),
        )
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val currentSlot = slot
        if (currentSlot == null) {
            tile.label = getString(R.string.ba_daily_done_tile_label_all)
            tile.subtitle = getString(R.string.ba_daily_done_tile_subtitle)
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }
        val account =
            BASettingsStore
                .loadDailyTileState()
                .accountIdAt(currentSlot)
                ?.let { id -> BASettingsStore.loadAccountState().accounts.firstOrNull { it.profile.id == id } }
        if (account == null) {
            // The account was deleted while the tile stayed on the panel. Unavailable rather than a
            // silent no-op, so a tap cannot look like it worked.
            tile.label = getString(R.string.ba_daily_done_tile_label_unbound)
            tile.subtitle = null
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
            return
        }
        tile.label =
            getString(R.string.ba_daily_done_tile_label_account_format, account.profile.displayName)
        tile.subtitle = getString(R.string.ba_daily_done_tile_subtitle)
        tile.state = if (account.profile.enabled) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
        tile.updateTile()
    }

    private sealed interface Targets {
        data object AllEnabled : Targets

        data object Unbound : Targets

        data class Only(val accountIds: List<BaAccountId>) : Targets
    }

    private companion object {
        const val TAG = "BaDailyTile"
    }
}

/** Applies the template to every enabled account. */
internal class BaDailyDoneAllTileService : BaDailyDoneTileServiceBase() {
    override val slot: Int? = null
}

internal class BaDailyDoneAccountTileService1 : BaDailyDoneTileServiceBase() {
    override val slot: Int = 0
}

internal class BaDailyDoneAccountTileService2 : BaDailyDoneTileServiceBase() {
    override val slot: Int = 1
}

internal class BaDailyDoneAccountTileService3 : BaDailyDoneTileServiceBase() {
    override val slot: Int = 2
}
