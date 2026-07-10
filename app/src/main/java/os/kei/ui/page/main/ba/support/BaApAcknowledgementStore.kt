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
    ): Long = loadTimestamp(readAnchorKey(accountId, kind))

    fun setSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ): Boolean = setTimestamp(readAnchorKey(accountId, kind), anchorAtMs)

    fun loadDismissedUntil(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long = loadTimestamp(dismissedUntilKey(accountId, kind))

    fun setDismissedUntil(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        dismissedUntilAtMs: Long,
    ): Boolean = setTimestamp(dismissedUntilKey(accountId, kind), dismissedUntilAtMs)

    fun updateState(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        suppressionAnchorAtMs: Long? = null,
        dismissedUntilAtMs: Long? = null,
    ): Boolean {
        var changed = false
        suppressionAnchorAtMs?.let { value ->
            changed = setSuppressionAnchor(accountId, kind, value) || changed
        }
        dismissedUntilAtMs?.let { value ->
            changed = setDismissedUntil(accountId, kind, value) || changed
        }
        return changed
    }

    fun clear(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Boolean = setSuppressionAnchor(accountId, kind, 0L)

    fun clearDismissedUntil(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Boolean = setDismissedUntil(accountId, kind, 0L)

    fun clearAccount(accountId: BaAccountId): Boolean =
        BaApReminderKind.entries
            .flatMap { kind ->
                listOf(
                    clear(accountId, kind),
                    clearDismissedUntil(accountId, kind),
                )
            }
            .any { it }

    private fun loadTimestamp(key: String): Long =
        store.decodeLong(key, 0L).coerceAtLeast(0L)

    private fun setTimestamp(
        key: String,
        value: Long,
    ): Boolean {
        val normalized = value.coerceAtLeast(0L)
        if (normalized == 0L) {
            if (!store.containsKey(key)) return false
            store.removeValueForKey(key)
        } else {
            if (store.containsKey(key) && store.decodeLong(key, 0L) == normalized) return false
            store.encode(key, normalized)
        }
        return true
    }

    private fun readAnchorKey(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): String = "ba_ap_read_anchor:${kind.keyPart}:${accountId.value}"

    private fun dismissedUntilKey(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): String = "ba_ap_dismissed_until:${kind.keyPart}:${accountId.value}"
}
