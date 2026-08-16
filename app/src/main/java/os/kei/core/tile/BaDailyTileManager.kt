package os.kei.core.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BA_DAILY_TILE_SLOTS
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.accountIdAt
import os.kei.ui.page.main.ba.support.firstFreeSlot
import os.kei.ui.page.main.ba.support.retainingExistingAccounts
import os.kei.ui.page.main.ba.support.slotOf
import os.kei.ui.page.main.ba.support.withSlot

/** What came back from asking the system to add a tile. */
internal enum class BaDailyTileAddResult {
    Added,
    AlreadyAdded,
    Declined,

    /** The system refused outright — wrong user, no status bar, or the request quota is spent. */
    Unavailable,
}

/**
 * Claims, releases and keeps the daily-done tiles in step with the account list.
 *
 * Every tile component is declared `android:enabled="false"`, so nothing shows up in the quick-settings
 * editor until it is claimed here. Enabling is therefore part of claiming, and it has to happen *before*
 * the add request: an add for a disabled component comes back
 * `TILE_ADD_REQUEST_ERROR_BAD_COMPONENT`.
 *
 * The add request is rate limited by the platform **per component**, and its javadoc is explicit that the
 * system "can choose to auto-deny a request if the user has denied that specific request (user,
 * ComponentName) enough times before". So it is only ever fired from an explicit tap, never on a sheet
 * opening or an account change, and a slot's component keeps its identity for as long as possible rather
 * than being recycled between accounts.
 */
internal object BaDailyTileManager {
    private const val TAG = "BaDailyTile"

    private val accountTileClasses =
        listOf(
            BaDailyDoneAccountTileService1::class.java,
            BaDailyDoneAccountTileService2::class.java,
            BaDailyDoneAccountTileService3::class.java,
        )

    private fun allComponent(context: Context): ComponentName =
        ComponentName(context, BaDailyDoneAllTileService::class.java)

    private fun accountComponent(context: Context, slot: Int): ComponentName =
        ComponentName(context, accountTileClasses[slot.coerceIn(0, BA_DAILY_TILE_SLOTS - 1)])

    private fun setComponentEnabled(
        context: Context,
        component: ComponentName,
        enabled: Boolean,
    ) {
        val target =
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                component,
                target,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { throwable ->
            AppLogger.e(TAG, "failed to set $component enabled=$enabled", throwable)
        }
    }

    internal fun isComponentEnabled(
        context: Context,
        component: ComponentName,
    ): Boolean =
        context.packageManager.getComponentEnabledSetting(component) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun isAllAccountsTileEnabled(context: Context): Boolean =
        isComponentEnabled(context, allComponent(context))

    fun isAccountTileEnabled(context: Context, slot: Int): Boolean =
        isComponentEnabled(context, accountComponent(context, slot))

    /**
     * Enables the all-accounts tile and asks the system to add it.
     *
     * Must be called while the app is in the foreground — an add request from the background returns
     * `TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND`.
     */
    fun requestAllAccountsTile(
        context: Context,
        onResult: (BaDailyTileAddResult) -> Unit,
    ) {
        val component = allComponent(context)
        setComponentEnabled(context, component, enabled = true)
        request(
            context = context,
            component = component,
            label = context.getString(R.string.ba_daily_done_tile_label_all),
            onResult = onResult,
        )
    }

    /**
     * Binds [accountId] to a free pool slot, enables that slot's component and asks to add it.
     *
     * Reuses the slot the account already holds when there is one, so a teacher who removes and re-adds
     * the tile does not burn a second component's request quota.
     */
    fun requestAccountTile(
        context: Context,
        accountId: BaAccountId,
        accountDisplayName: String,
        onResult: (BaDailyTileAddResult) -> Unit,
    ): Boolean {
        val state = BASettingsStore.loadDailyTileState()
        val slot = state.slotOf(accountId) ?: state.firstFreeSlot() ?: return false
        BASettingsStore.saveDailyTileState(state.withSlot(slot, accountId))
        val component = accountComponent(context, slot)
        setComponentEnabled(context, component, enabled = true)
        request(
            context = context,
            component = component,
            label =
                context.getString(
                    R.string.ba_daily_done_tile_label_account_format,
                    accountDisplayName,
                ),
            onResult = onResult,
        )
        return true
    }

    /** Releases an account's slot and hides its component again. */
    fun releaseAccountTile(
        context: Context,
        accountId: BaAccountId,
    ) {
        val state = BASettingsStore.loadDailyTileState()
        val slot = state.slotOf(accountId) ?: return
        BASettingsStore.saveDailyTileState(state.withSlot(slot, null))
        setComponentEnabled(context, accountComponent(context, slot), enabled = false)
    }

    fun releaseAllAccountsTile(context: Context) {
        setComponentEnabled(context, allComponent(context), enabled = false)
    }

    /**
     * Drops bindings for accounts that no longer exist and hides the freed components.
     *
     * Safe to call after any account mutation; it writes only when something actually changed, because the
     * only available change signal fires on every BA write including each AP tick.
     */
    fun syncWithAccounts(
        context: Context,
        existingAccountIds: Collection<BaAccountId>,
    ) {
        val state = BASettingsStore.loadDailyTileState()
        val synced = state.retainingExistingAccounts(existingAccountIds)
        if (synced == state) return
        BASettingsStore.saveDailyTileState(synced)
        (0 until BA_DAILY_TILE_SLOTS).forEach { slot ->
            if (state.accountIdAt(slot) != null && synced.accountIdAt(slot) == null) {
                setComponentEnabled(context, accountComponent(context, slot), enabled = false)
                AppLogger.i(TAG) { "released tile slot=$slot after its account was deleted" }
            }
        }
    }

    private fun request(
        context: Context,
        component: ComponentName,
        label: String,
        onResult: (BaDailyTileAddResult) -> Unit,
    ) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
            onResult(BaDailyTileAddResult.Unavailable)
            return
        }
        runCatching {
            statusBarManager.requestAddTileService(
                component,
                label,
                Icon.createWithResource(context, R.drawable.ic_ba_ap_island_shift),
                context.mainExecutor,
            ) { code -> onResult(code.toAddResult()) }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "requestAddTileService threw for $component", throwable)
            onResult(BaDailyTileAddResult.Unavailable)
        }
    }

    private fun Int.toAddResult(): BaDailyTileAddResult =
        when (this) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> BaDailyTileAddResult.Added
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> BaDailyTileAddResult.AlreadyAdded
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> BaDailyTileAddResult.Declined
            else -> {
                // The whole 1000-series is a refusal the teacher cannot act on: mismatched package, a
                // request already in flight, a disabled component, the wrong user, the app in the
                // background, or no status bar service.
                AppLogger.w(TAG) { "tile add request refused with code $this" }
                BaDailyTileAddResult.Unavailable
            }
        }
}
