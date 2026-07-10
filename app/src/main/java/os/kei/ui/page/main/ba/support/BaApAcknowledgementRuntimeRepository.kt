package os.kei.ui.page.main.ba.support

import os.kei.ui.page.main.ba.BaReminderCoordinator

internal fun deleteBaAccountAndClearAcknowledgements(
    accountStore: BaAccountStore,
    acknowledgementStore: BaApAcknowledgementStore,
    accountId: BaAccountId,
): Boolean {
    if (!accountStore.deleteAccount(accountId)) return false
    acknowledgementStore.clearAccount(accountId)
    return true
}

internal class BaApAcknowledgementRuntimeRepository(
    keyValueStore: BaAccountKeyValueStore,
    private val onLocalStateChanged: () -> Unit,
) {
    private val acknowledgementStore = BaApAcknowledgementStore(keyValueStore)

    fun withLocalAcknowledgements(
        snapshot: BaPageSnapshot,
        accountId: BaAccountId,
    ): BaPageSnapshot =
        snapshot.withLocalApAcknowledgementAnchors(accountId, acknowledgementStore)

    fun loadSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
    ): Long = acknowledgementStore.loadSuppressionAnchor(accountId, kind)

    fun saveSuppressionAnchor(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ): Boolean =
        acknowledgementStore
            .setSuppressionAnchor(accountId, kind, anchorAtMs)
            .also { changed ->
                if (changed) onLocalStateChanged()
            }

    fun clearAccount(accountId: BaAccountId): Boolean =
        acknowledgementStore
            .clearAccount(accountId)
            .also { changed ->
                if (changed) onLocalStateChanged()
            }

    fun reconcile(
        accountStore: BaAccountStore,
        baseSnapshot: BaPageSnapshot,
        nowMs: Long,
    ): Boolean =
        reconcileBaApAcknowledgements(
            accountState = accountStore.loadState(),
            baseSnapshot = baseSnapshot,
            accountStore = accountStore,
            acknowledgementStore = acknowledgementStore,
            nowMs = nowMs,
        ).also { changed ->
            if (changed) onLocalStateChanged()
        }
}

internal fun reconcileBaApAcknowledgements(
    accountState: BaAccountStoreSnapshot,
    baseSnapshot: BaPageSnapshot,
    accountStore: BaAccountStore,
    acknowledgementStore: BaApAcknowledgementStore,
    nowMs: Long,
): Boolean {
    var changed = false
    accountState.accounts
        .map { account ->
            BaAccountReminderSnapshot(
                accountId = account.profile.id,
                displayName = account.profile.displayName,
                snapshot =
                    baseSnapshot.withBaAccount(
                        accountState = accountState,
                        account = account,
                    ).withLocalApAcknowledgementAnchors(account.profile.id, acknowledgementStore),
            )
        }.forEach { reminderSnapshot ->
            val accountId = reminderSnapshot.accountId
            val snapshot = reminderSnapshot.snapshot
            val apPlan =
                BaReminderCoordinator.evaluateApThreshold(
                    snapshot = snapshot,
                    nowMs = nowMs,
                )
            if (apPlan.resetLastNotifiedLevel) {
                changed =
                    accountStore.updateAccountReminderRuntime(accountId) { runtime ->
                        runtime.copy(apLastNotifiedLevel = -1)
                    } || changed
            }
            if (apPlan.resetSuppressionAnchor) {
                changed =
                    acknowledgementStore.clear(accountId, BaApReminderKind.Ap) || changed
            }
            val cafeApPlan =
                BaReminderCoordinator.evaluateCafeApThreshold(
                    snapshot = snapshot,
                    nowMs = nowMs,
                )
            if (cafeApPlan.resetLastNotifiedLevel) {
                changed =
                    accountStore.updateAccountReminderRuntime(accountId) { runtime ->
                        runtime.copy(cafeApLastNotifiedLevel = -1)
                    } || changed
            }
            if (cafeApPlan.resetSuppressionAnchor) {
                changed =
                    acknowledgementStore.clear(accountId, BaApReminderKind.CafeAp) || changed
            }
        }
    return changed
}
