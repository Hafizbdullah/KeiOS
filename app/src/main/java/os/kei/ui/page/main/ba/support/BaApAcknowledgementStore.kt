package os.kei.ui.page.main.ba.support

internal enum class BaApReminderKind(
    val keyPart: String,
) {
    Ap("ap"),
    CafeAp("cafe_ap"),
}

internal class BaApAcknowledgementStore(
    private val store: BaAccountKeyValueStore,
) {
    fun loadSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long = store.decodeLong(key(accountId, kind), 0L).coerceAtLeast(0L)

    fun setSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ): Boolean {
        val key = key(accountId, kind)
        val normalized = anchorAtMs.coerceAtLeast(0L)
        if (loadSuppressionAnchor(accountId, kind) == normalized) return false
        if (normalized == 0L) {
            store.removeValueForKey(key)
        } else {
            store.encode(key, normalized)
        }
        return true
    }

    fun clear(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Boolean = setSuppressionAnchor(accountId, kind, 0L)

    fun clearAccount(accountId: BaAccountId): Boolean =
        BaApReminderKind.entries
            .map { kind -> clear(accountId, kind) }
            .any { it }

    private fun key(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): String = "ba_ap_read_anchor:${kind.keyPart}:${accountId.value}"
}
