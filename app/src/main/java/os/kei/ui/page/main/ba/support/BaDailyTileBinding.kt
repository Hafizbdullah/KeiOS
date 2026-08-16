package os.kei.ui.page.main.ba.support

import kotlinx.serialization.Serializable

/**
 * How many per-account tile slots exist.
 *
 * A hard ceiling, not a preference. A quick-settings tile is a manifest-declared `<service>` and cannot
 * be created at runtime, so "one tile per account" can only ever mean "one tile per *declared* slot".
 * Three matches the game: an account's `serverIndex` is coerced to 0..2, so three servers is the
 * natural common maximum, and a teacher past that keeps using the all-accounts tile.
 */
internal const val BA_DAILY_TILE_SLOTS = 3

/** What a daily-done trigger acts on. */
@Serializable
internal enum class BaDailyTileMode {
    /** One tile, every enabled account. */
    AllAccounts,

    /** One tile per bound account, up to [BA_DAILY_TILE_SLOTS]. */
    PerAccount,
}

/**
 * Which account each per-account tile slot is claimed by.
 *
 * Only the id is stored, never the name. A tile reads its account's display name at listen time, so
 * renaming an account re-labels its tile with no bookkeeping — the only lifecycle event that needs
 * handling here is an account going away.
 */
@Serializable
internal data class BaDailyTileState(
    val mode: BaDailyTileMode = BaDailyTileMode.AllAccounts,
    /** Slot index -> account id. Absent or blank means the slot is unclaimed. */
    val slotAccountIds: List<String> = emptyList(),
)

internal fun BaDailyTileState.normalized(): BaDailyTileState =
    copy(
        slotAccountIds =
            List(BA_DAILY_TILE_SLOTS) { slot ->
                slotAccountIds.getOrElse(slot) { "" }.trim()
            },
    )

internal fun BaDailyTileState.accountIdAt(slot: Int): BaAccountId? =
    slotAccountIds
        .getOrNull(slot)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::BaAccountId)

internal fun BaDailyTileState.slotOf(accountId: BaAccountId): Int? =
    (0 until BA_DAILY_TILE_SLOTS).firstOrNull { accountIdAt(it) == accountId }

internal fun BaDailyTileState.boundSlots(): List<Int> =
    (0 until BA_DAILY_TILE_SLOTS).filter { accountIdAt(it) != null }

internal fun BaDailyTileState.withSlot(
    slot: Int,
    accountId: BaAccountId?,
): BaDailyTileState {
    if (slot !in 0 until BA_DAILY_TILE_SLOTS) return this
    val normalized = normalized()
    return normalized.copy(
        slotAccountIds =
            List(BA_DAILY_TILE_SLOTS) { index ->
                when {
                    index == slot -> accountId?.value?.trim().orEmpty()
                    // An account may hold at most one slot: claiming it elsewhere releases the old one,
                    // otherwise two tiles would fight over the same label and the same template.
                    accountId != null && normalized.accountIdAt(index) == accountId -> ""
                    else -> normalized.slotAccountIds.getOrElse(index) { "" }
                }
            },
    )
}

/** The first unclaimed slot, or `null` when the pool is full. */
internal fun BaDailyTileState.firstFreeSlot(): Int? =
    (0 until BA_DAILY_TILE_SLOTS).firstOrNull { accountIdAt(it) == null }

/**
 * Drops every binding whose account no longer exists.
 *
 * Called after any account-list change. A disabled account keeps its slot on purpose: disabling is a
 * temporary state a teacher toggles, and silently surrendering the slot would mean losing the tile —
 * which the system may then refuse to re-add, since the add request is rate limited per component and
 * can be auto-denied for good. Deletion is the only irreversible event, so it is the only one that frees
 * a slot.
 */
internal fun BaDailyTileState.retainingExistingAccounts(
    existingAccountIds: Collection<BaAccountId>,
): BaDailyTileState {
    val existing = existingAccountIds.toSet()
    val normalized = normalized()
    return normalized.copy(
        slotAccountIds =
            List(BA_DAILY_TILE_SLOTS) { slot ->
                val bound = normalized.accountIdAt(slot)
                if (bound != null && bound in existing) bound.value else ""
            },
    )
}

/** True when the pool no longer matches the accounts, i.e. a re-sync has to write and re-label. */
internal fun BaDailyTileState.needsSyncFor(existingAccountIds: Collection<BaAccountId>): Boolean =
    retainingExistingAccounts(existingAccountIds) != normalized()

/**
 * A stable fingerprint of what a tile or shortcut renders, so a sync can skip when nothing it shows
 * has changed.
 *
 * Needed because the only available change signal is bumped by *every* BA write, including each AP
 * regeneration tick. Re-labelling on all of those would be constant churn; comparing this instead
 * reduces it to actual identity changes. Enabled state is included because it decides which accounts an
 * all-accounts trigger covers.
 */
internal fun baDailyTileFingerprint(snapshot: BaAccountStoreSnapshot): String =
    snapshot.accounts
        .sortedBy { it.profile.sortOrder }
        .joinToString(separator = "|") { account ->
            listOf(
                account.profile.id.value,
                account.profile.displayName,
                account.profile.serverIndex.toString(),
                if (account.profile.enabled) "1" else "0",
            ).joinToString(separator = ",")
        }
