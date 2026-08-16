package os.kei.ui.page.main.ba.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import os.kei.feature.ba.identity.normalizeBaFriendCodeInput
import os.kei.feature.ba.identity.sanitizeBaFriendCode
import os.kei.feature.ba.identity.sanitizeBaNickname

@Serializable
@JvmInline
internal value class BaAccountId(val value: String)

@Serializable
internal enum class BaAccountNotificationMode {
    @SerialName("follow_global")
    FollowGlobal,

    @SerialName("custom")
    Custom,
}

@Serializable
internal data class BaAccountProfile(
    val id: BaAccountId,
    val serverIndex: Int,
    val displayName: String,
    val nickname: String,
    val friendCode: String,
    val notificationMode: BaAccountNotificationMode = BaAccountNotificationMode.FollowGlobal,
    val remindersEnabled: Boolean = true,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
)

@Serializable
internal data class BaAccountRuntime(
    val apLimit: Int = DEFAULT_AP_LIMIT,
    val apCurrent: Double = DEFAULT_AP_CURRENT,
    val apRegenBaseMs: Long = 0L,
    val apSyncMs: Long = 0L,
    val cafeLevel: Int = DEFAULT_CAFE_LEVEL,
    val cafeStoredAp: Double = DEFAULT_CAFE_STORED_AP,
    val cafeLastHourMs: Long = 0L,
    val coffeeHeadpatMs: Long = 0L,
    val coffeeInvite1UsedMs: Long = 0L,
    val coffeeInvite2UsedMs: Long = 0L,
    /**
     * Craft Chamber slots, per account and never shared. Additive with a default, and `KeiJson.lenient`
     * sets `ignoreUnknownKeys`, so records written by older builds decode straight to an idle set.
     */
    val craft: BaCraftState = BaCraftState(),
)

@Serializable
internal data class BaGlobalReminderSettings(
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val keepApRemindersReadUntilBelowThreshold: Boolean = true,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
    val craftNotifyEnabled: Boolean = false,
)

@Serializable
internal data class BaAccountReminderOverride(
    val accountId: BaAccountId,
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val keepApRemindersReadUntilBelowThreshold: Boolean = true,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
    val craftNotifyEnabled: Boolean = false,
)

internal data class BaAccountProfileInput(
    val serverIndex: Int,
    val displayName: String,
    val nickname: String,
    val friendCode: String,
    val notificationMode: BaAccountNotificationMode = BaAccountNotificationMode.FollowGlobal,
    val remindersEnabled: Boolean = true,
    val customReminderSettings: BaGlobalReminderSettings = BaGlobalReminderSettings(),
)

@Serializable
internal data class BaAccountReminderRuntime(
    val apLastNotifiedLevel: Int = -1,
    val cafeApLastNotifiedLevel: Int = -1,
    val arenaRefreshLastNotifiedSlotMs: Long = 0L,
    val cafeVisitLastNotifiedSlotMs: Long = 0L,
    val craftNotified: BaCraftNotifiedMarkers = BaCraftNotifiedMarkers(),
)

@Serializable
internal data class BaAccountRecord(
    val profile: BaAccountProfile,
    val runtime: BaAccountRuntime = BaAccountRuntime(),
    val reminderRuntime: BaAccountReminderRuntime = BaAccountReminderRuntime(),
    val reminderOverride: BaAccountReminderOverride? = null,
    val profileUpdatedAtMs: Long = 0L,
    val runtimeUpdatedAtMs: Long = 0L,
    val reminderRuntimeUpdatedAtMs: Long = 0L,
    val reminderOverrideUpdatedAtMs: Long = 0L,
)

internal data class BaAccountStoreSnapshot(
    val accounts: List<BaAccountRecord>,
    val activeAccountId: BaAccountId?,
    val allAccountsFollowGlobalNotificationSettings: Boolean,
    val globalReminderSettings: BaGlobalReminderSettings,
    val activeAccountUpdatedAtMs: Long = 0L,
    val allAccountsFollowGlobalNotificationSettingsUpdatedAtMs: Long = 0L,
    val globalReminderSettingsUpdatedAtMs: Long = 0L,
)

internal fun BaAccountStoreSnapshot.enabledServerIndices(): List<Int> =
    accounts
        .asSequence()
        .filter { it.profile.enabled }
        .map { it.profile.serverIndex.coerceIn(0, 2) }
        .distinct()
        .toList()

internal data class BaAccountReminderSnapshot(
    val accountId: BaAccountId,
    val displayName: String,
    val snapshot: BaPageSnapshot,
)

internal fun sanitizeBaAccountNickname(name: String, serverIndex: Int? = null): String =
    sanitizeBaNickname(name, serverIndex)

internal fun normalizeBaAccountFriendCodeInput(code: String, serverIndex: Int? = null): String =
    normalizeBaFriendCodeInput(code, serverIndex)

internal fun sanitizeBaAccountFriendCode(code: String, serverIndex: Int? = null): String =
    sanitizeBaFriendCode(code, serverIndex)

internal fun sanitizeBaAccountDisplayName(
    displayName: String,
    nickname: String,
): String =
    displayName
        .trim()
        .take(24)
        .ifEmpty {
            nickname
                .trim()
                .take(24)
                .ifEmpty { BA_DEFAULT_NICKNAME }
        }

internal fun BaAccountRecord.normalized(defaultSortOrder: Int): BaAccountRecord? {
    val accountId = BaAccountId(profile.id.value.trim())
    if (accountId.value.isBlank()) return null
    val serverIndex = profile.serverIndex.coerceIn(0, 2)
    val nickname = sanitizeBaAccountNickname(profile.nickname, serverIndex)
    val friendCode = sanitizeBaAccountFriendCode(profile.friendCode, serverIndex)
    val normalizedProfile =
        profile.copy(
            id = accountId,
            serverIndex = serverIndex,
            displayName = sanitizeBaAccountDisplayName(profile.displayName, nickname),
            nickname = nickname,
            friendCode = friendCode,
            sortOrder = profile.sortOrder.takeIf { it >= 0 } ?: defaultSortOrder,
        )
    return copy(
        profile = normalizedProfile,
        runtime = runtime.normalized(),
        reminderRuntime = reminderRuntime.normalized(),
        reminderOverride = reminderOverride?.normalized(accountId),
        profileUpdatedAtMs = profileUpdatedAtMs.coerceAtLeast(0L),
        runtimeUpdatedAtMs = runtimeUpdatedAtMs.coerceAtLeast(0L),
        reminderRuntimeUpdatedAtMs = reminderRuntimeUpdatedAtMs.coerceAtLeast(0L),
        reminderOverrideUpdatedAtMs = reminderOverrideUpdatedAtMs.coerceAtLeast(0L),
    )
}

internal fun BaAccountRuntime.normalized(): BaAccountRuntime =
    cafeLevel.coerceIn(1, 10).let { safeCafeLevel ->
        copy(
            apLimit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX),
            apCurrent = normalizeAp(apCurrent),
            apRegenBaseMs = apRegenBaseMs.coerceAtLeast(0L),
            apSyncMs = apSyncMs.coerceAtLeast(0L),
            cafeLevel = safeCafeLevel,
            cafeStoredAp = normalizeAp(cafeStoredAp.coerceIn(0.0, cafeStorageCap(safeCafeLevel))),
            cafeLastHourMs = cafeLastHourMs.coerceAtLeast(0L),
            coffeeHeadpatMs = coffeeHeadpatMs.coerceAtLeast(0L),
            coffeeInvite1UsedMs = coffeeInvite1UsedMs.coerceAtLeast(0L),
            coffeeInvite2UsedMs = coffeeInvite2UsedMs.coerceAtLeast(0L),
            craft = craft.normalized(),
        )
    }

internal fun BaGlobalReminderSettings.normalized(): BaGlobalReminderSettings =
    copy(
        apNotifyThreshold = apNotifyThreshold.coerceIn(0, BA_AP_MAX),
        cafeApNotifyThreshold = cafeApNotifyThreshold.coerceIn(0, BA_AP_MAX),
    )

internal fun BaAccountReminderOverride.normalized(accountId: BaAccountId): BaAccountReminderOverride =
    copy(
        accountId = accountId,
        apNotifyThreshold = apNotifyThreshold.coerceIn(0, BA_AP_MAX),
        cafeApNotifyThreshold = cafeApNotifyThreshold.coerceIn(0, BA_AP_MAX),
    )

internal fun BaAccountReminderRuntime.normalized(): BaAccountReminderRuntime =
    copy(
        apLastNotifiedLevel = apLastNotifiedLevel.coerceIn(-1, BA_AP_MAX),
        cafeApLastNotifiedLevel = cafeApLastNotifiedLevel.coerceIn(-1, BA_AP_MAX),
        arenaRefreshLastNotifiedSlotMs = arenaRefreshLastNotifiedSlotMs.coerceAtLeast(0L),
        cafeVisitLastNotifiedSlotMs = cafeVisitLastNotifiedSlotMs.coerceAtLeast(0L),
        craftNotified = craftNotified.normalized(),
    )

internal fun BaAccountRecord.effectiveReminderSettings(
    globalSettings: BaGlobalReminderSettings,
    allAccountsFollowGlobalNotificationSettings: Boolean,
): BaGlobalReminderSettings {
    if (!profile.enabled || !profile.remindersEnabled) return BaGlobalReminderSettings()
    val normalizedGlobal = globalSettings.normalized()
    if (allAccountsFollowGlobalNotificationSettings) return normalizedGlobal
    val override = reminderOverride?.normalized(profile.id)
    return when (profile.notificationMode) {
        BaAccountNotificationMode.FollowGlobal -> normalizedGlobal
        BaAccountNotificationMode.Custom ->
            override?.let {
                BaGlobalReminderSettings(
                    apNotifyEnabled = it.apNotifyEnabled,
                    apNotifyThreshold = it.apNotifyThreshold,
                    cafeApNotifyEnabled = it.cafeApNotifyEnabled,
                    cafeApNotifyThreshold = it.cafeApNotifyThreshold,
                    keepApRemindersReadUntilBelowThreshold = it.keepApRemindersReadUntilBelowThreshold,
                    arenaRefreshNotifyEnabled = it.arenaRefreshNotifyEnabled,
                    cafeVisitNotifyEnabled = it.cafeVisitNotifyEnabled,
                    craftNotifyEnabled = it.craftNotifyEnabled,
                )
            } ?: normalizedGlobal
    }
}

internal fun BaGlobalReminderSettings.toAccountReminderOverride(accountId: BaAccountId): BaAccountReminderOverride {
    val normalized = normalized()
    return BaAccountReminderOverride(
        accountId = accountId,
        apNotifyEnabled = normalized.apNotifyEnabled,
        apNotifyThreshold = normalized.apNotifyThreshold,
        cafeApNotifyEnabled = normalized.cafeApNotifyEnabled,
        cafeApNotifyThreshold = normalized.cafeApNotifyThreshold,
        keepApRemindersReadUntilBelowThreshold = normalized.keepApRemindersReadUntilBelowThreshold,
        arenaRefreshNotifyEnabled = normalized.arenaRefreshNotifyEnabled,
        cafeVisitNotifyEnabled = normalized.cafeVisitNotifyEnabled,
        craftNotifyEnabled = normalized.craftNotifyEnabled,
    )
}
