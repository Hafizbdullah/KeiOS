package os.kei.core.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import os.kei.MainActivity
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BaAccountRecord
import os.kei.ui.page.main.ba.support.BaDailyTileMode

internal object AppShortcuts {
    private const val TAG = "AppShortcuts"

    private const val SHORTCUT_ID_BA_AP_ISLAND = "keios.ba.ap_island"
    private const val SHORTCUT_ID_MCP_TOGGLE = "keios.mcp.toggle"
    private const val SHORTCUT_ID_GITHUB_REFRESH = "keios.github.refresh_tracked"
    private const val SHORTCUT_ID_BA_DAILY_DONE_ALL = "keios.ba.daily_done"
    private const val SHORTCUT_ID_BA_DAILY_DONE_ACCOUNT_PREFIX = "keios.ba.daily_done."

    /**
     * Rebuilds the whole dynamic set.
     *
     * `setDynamicShortcuts` replaces rather than appends, and it throws past
     * `getMaxShortcutCountPerActivity`, so the daily-done entries are appended last and truncated to
     * whatever room the three fixed ones leave. Truncation is logged: a silently dropped shortcut reads
     * to the teacher as the feature not working.
     */
    fun sync(context: Context) {
        val fixed =
            listOf(
                buildBaApIslandShortcut(context),
                buildMcpToggleShortcut(context),
                buildGitHubRefreshShortcut(context),
            )
        val daily = buildDailyDoneShortcuts(context, startRank = fixed.size)
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtLeast(fixed.size)
        val room = (max - fixed.size).coerceAtLeast(0)
        if (daily.size > room) {
            AppLogger.w(TAG) {
                "dropping ${daily.size - room} daily-done shortcut(s): max=$max fixed=${fixed.size}"
            }
        }
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, fixed + daily.take(room))
        }.onFailure { throwable ->
            AppLogger.e(TAG, "setDynamicShortcuts failed", throwable)
        }
    }

    /**
     * One entry for every enabled account in per-account mode, or a single all-accounts entry otherwise.
     *
     * Unlike a quick-settings tile these are created at runtime, so per-account really is per account
     * here — no fixed pool, and the only ceiling is the launcher's shortcut budget.
     */
    private fun buildDailyDoneShortcuts(
        context: Context,
        startRank: Int,
    ): List<ShortcutInfoCompat> {
        val tileState = BASettingsStore.loadDailyTileState()
        if (tileState.mode != BaDailyTileMode.PerAccount) {
            return listOf(buildDailyDoneAllShortcut(context, rank = startRank))
        }
        val accounts =
            BASettingsStore
                .loadAccountState()
                .accounts
                .filter { it.profile.enabled }
                .sortedBy { it.profile.sortOrder }
        if (accounts.isEmpty()) {
            return listOf(buildDailyDoneAllShortcut(context, rank = startRank))
        }
        return accounts.mapIndexed { index, account ->
            buildDailyDoneAccountShortcut(context, account, rank = startRank + index)
        }
    }

    private fun dailyDoneIntent(context: Context, accountId: String?): Intent =
        Intent(context, AppShortcutActionActivity::class.java).apply {
            action = AppShortcutActionReceiver.ACTION_HANDLE_SHORTCUT
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_BA)
            putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, MainActivity.SHORTCUT_ACTION_BA_DAILY_DONE)
            if (accountId != null) {
                putExtra(MainActivity.EXTRA_BA_ACCOUNT_ID, accountId)
            }
        }

    private fun buildDailyDoneAllShortcut(context: Context, rank: Int): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, SHORTCUT_ID_BA_DAILY_DONE_ALL)
            .setShortLabel(context.getString(R.string.shortcut_label_ba_daily_done_short))
            .setLongLabel(context.getString(R.string.shortcut_label_ba_daily_done_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_ba_ap_island_shift))
            .setIntent(dailyDoneIntent(context, accountId = null))
            .setRank(rank)
            .build()

    private fun buildDailyDoneAccountShortcut(
        context: Context,
        account: BaAccountRecord,
        rank: Int,
    ): ShortcutInfoCompat {
        val label =
            context.getString(
                R.string.shortcut_label_ba_daily_done_account_format,
                account.profile.displayName,
            )
        return ShortcutInfoCompat.Builder(
            context,
            SHORTCUT_ID_BA_DAILY_DONE_ACCOUNT_PREFIX + account.profile.id.value,
        )
            // Short labels are truncated hard by launchers, so the account name carries the long one.
            .setShortLabel(context.getString(R.string.shortcut_label_ba_daily_done_short))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_ba_ap_island_shift))
            .setIntent(dailyDoneIntent(context, accountId = account.profile.id.value))
            .setRank(rank)
            .build()
    }

    private fun buildBaApIslandShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_BA_AP_ISLAND)
            .setShortLabel(context.getString(R.string.shortcut_label_ap_island_short))
            .setLongLabel(context.getString(R.string.shortcut_label_ap_island_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_ba_ap_island_shift))
            .setIntent(
                Intent(context, AppShortcutActionActivity::class.java).apply {
                    action = AppShortcutActionReceiver.ACTION_HANDLE_SHORTCUT
                    putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_BA)
                    putExtra(MainActivity.EXTRA_SHORTCUT_ACTION, MainActivity.SHORTCUT_ACTION_BA_AP_ISLAND)
                }
            )
            .setRank(0)
            .build()
    }

    private fun buildMcpToggleShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_MCP_TOGGLE)
            .setShortLabel(context.getString(R.string.shortcut_label_mcp_toggle_short))
            .setLongLabel(context.getString(R.string.shortcut_label_mcp_toggle_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_mcp_lobehub))
            .setIntent(
                Intent(context, AppShortcutActionActivity::class.java).apply {
                    action = AppShortcutActionReceiver.ACTION_HANDLE_SHORTCUT
                    putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_MCP)
                    putExtra(MainActivity.EXTRA_MCP_SERVER_ACTION, MainActivity.MCP_SERVER_ACTION_TOGGLE)
                }
            )
            .setRank(1)
            .build()
    }

    private fun buildGitHubRefreshShortcut(context: Context): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_GITHUB_REFRESH)
            .setShortLabel(context.getString(R.string.shortcut_label_github_refresh_short))
            .setLongLabel(context.getString(R.string.shortcut_label_github_refresh_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_github_invertocat))
            .setIntent(
                Intent(context, AppShortcutActionActivity::class.java).apply {
                    action = AppShortcutActionReceiver.ACTION_HANDLE_SHORTCUT
                    putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_GITHUB)
                    putExtra(
                        MainActivity.EXTRA_SHORTCUT_ACTION,
                        MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED
                    )
                }
            )
            .setRank(2)
            .build()
    }
}
