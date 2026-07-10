package os.kei.ui.page.main.ba.support

internal fun BaPageSnapshot.withActiveBaAccount(
    accountState: BaAccountStoreSnapshot,
): BaPageSnapshot {
    val activeAccount =
        accountState.accounts.firstOrNull { account ->
            account.profile.id == accountState.activeAccountId
        } ?: return this
    return withBaAccount(accountState = accountState, account = activeAccount)
}

internal fun BaPageSnapshot.withBaAccount(
    accountState: BaAccountStoreSnapshot,
    account: BaAccountRecord,
): BaPageSnapshot {
    val runtime = account.runtime.normalized()
    val reminderRuntime = account.reminderRuntime.normalized()
    val reminderSettings =
        account.effectiveReminderSettings(
            globalSettings = accountState.globalReminderSettings,
            allAccountsFollowGlobalNotificationSettings =
                accountState.allAccountsFollowGlobalNotificationSettings,
        )
    return copy(
        serverIndex = account.profile.serverIndex.coerceIn(0, 2),
        cafeLevel = runtime.cafeLevel,
        cafeStoredAp = runtime.cafeStoredAp,
        cafeLastHourMs = runtime.cafeLastHourMs,
        cafeApNotifyEnabled = reminderSettings.cafeApNotifyEnabled,
        cafeApNotifyThreshold = reminderSettings.cafeApNotifyThreshold,
        cafeApLastNotifiedLevel = reminderRuntime.cafeApLastNotifiedLevel,
        idNickname = account.profile.nickname,
        idFriendCode = account.profile.friendCode,
        apLimit = runtime.apLimit,
        apCurrent = runtime.apCurrent,
        apRegenBaseMs = runtime.apRegenBaseMs,
        apSyncMs = runtime.apSyncMs,
        apNotifyEnabled = reminderSettings.apNotifyEnabled,
        apNotifyThreshold = reminderSettings.apNotifyThreshold,
        keepApRemindersReadUntilBelowThreshold = reminderSettings.keepApRemindersReadUntilBelowThreshold,
        apLastNotifiedLevel = reminderRuntime.apLastNotifiedLevel,
        arenaRefreshNotifyEnabled = reminderSettings.arenaRefreshNotifyEnabled,
        arenaRefreshLastNotifiedSlotMs = reminderRuntime.arenaRefreshLastNotifiedSlotMs,
        cafeVisitNotifyEnabled = reminderSettings.cafeVisitNotifyEnabled,
        cafeVisitLastNotifiedSlotMs = reminderRuntime.cafeVisitLastNotifiedSlotMs,
        coffeeHeadpatMs = runtime.coffeeHeadpatMs,
        coffeeInvite1UsedMs = runtime.coffeeInvite1UsedMs,
        coffeeInvite2UsedMs = runtime.coffeeInvite2UsedMs,
    )
}

internal fun BaPageSnapshot.withLocalApAcknowledgementAnchors(
    accountId: BaAccountId,
    acknowledgementStore: BaApAcknowledgementStore,
): BaPageSnapshot =
    copy(
        apSuppressionAnchorAtMs =
            acknowledgementStore.loadSuppressionAnchor(
                accountId,
                BaApReminderKind.Ap,
            ),
        cafeApSuppressionAnchorAtMs =
            acknowledgementStore.loadSuppressionAnchor(
                accountId,
                BaApReminderKind.CafeAp,
            ),
        apDismissedUntilAtMs =
            acknowledgementStore.loadDismissedUntil(
                accountId,
                BaApReminderKind.Ap,
            ),
        cafeApDismissedUntilAtMs =
            acknowledgementStore.loadDismissedUntil(
                accountId,
                BaApReminderKind.CafeAp,
            ),
    )
