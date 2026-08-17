package os.kei.feature.ba.mcp

import os.kei.mcp.server.McpToolEnvironment

interface McpBaToolDelegate {
    fun defaultGuideRefreshIntervalHours(): Int

    /**
     * @param accountId blank for the active account. Every BA account owns its own AP, cafe, cooldowns
     *   and craft slots, so a snapshot without this can only ever describe one of them.
     */
    fun buildBaSnapshotText(accountId: String): String

    /** Every account at once — the only tool that can answer "which of my accounts needs attention". */
    fun buildBaAccountsText(): String

    /**
     * The one-tap dailies template, the same one the quick-settings tile and the launcher shortcut run.
     *
     * @param accountId blank for every enabled account.
     * @param apply `false` reports what would change and writes nothing. Mutating game state on a bare
     *   tool call would be too easy to trigger by accident, and the template is not reversible.
     */
    fun buildBaDailyDoneText(accountId: String, apply: Boolean): String

    fun buildBaCalendarCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int,
    ): String

    fun buildBaPoolCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int,
    ): String

    fun buildGuideCatalogCacheText(
        tab: String,
        includeEntries: Boolean,
        limit: Int,
    ): String

    fun buildGuideCacheOverviewText(): String

    fun buildGuideCacheInspectText(
        url: String,
        includeSections: Boolean,
        refreshIntervalHours: Int,
    ): String

    fun buildGuideMediaListText(
        url: String,
        kind: String,
        limit: Int,
    ): String

    suspend fun buildGuideBgmFavoritesText(
        action: String,
        query: String,
        limit: Int,
        rawJson: String,
        apply: Boolean,
    ): String

    fun buildCacheClearText(scope: String, url: String): String
}

fun interface McpBaToolDelegateFactory {
    fun create(environment: McpToolEnvironment): McpBaToolDelegate
}
