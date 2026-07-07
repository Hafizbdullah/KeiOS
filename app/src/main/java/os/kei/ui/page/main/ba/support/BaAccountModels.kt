package os.kei.ui.page.main.ba.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

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
)

@Serializable
internal data class BaGlobalReminderSettings(
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
)

@Serializable
internal data class BaAccountReminderOverride(
    val accountId: BaAccountId,
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = DEFAULT_AP_NOTIFY_THRESHOLD,
    val cafeApNotifyEnabled: Boolean = false,
    val cafeApNotifyThreshold: Int = DEFAULT_CAFE_AP_NOTIFY_THRESHOLD,
    val arenaRefreshNotifyEnabled: Boolean = false,
    val cafeVisitNotifyEnabled: Boolean = false,
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
    name
        .trim()
        .take(baAccountNicknameMaxLength(serverIndex))
        .ifEmpty { BA_DEFAULT_NICKNAME }

internal fun normalizeBaAccountFriendCodeInput(code: String, serverIndex: Int? = null): String {
    val trimmed = code.trim()
    val normalizedServerIndex = serverIndex?.coerceIn(0, 2)
    val maxLength = baAccountFriendCodeLength(normalizedServerIndex)
    return if (normalizedServerIndex == BA_SERVER_INDEX_CN) {
        trimmed
            .lowercase(Locale.ROOT)
            .filter { it in 'a'..'z' || it in '0'..'9' }
            .take(maxLength)
    } else {
        trimmed
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(maxLength)
    }
}

internal fun sanitizeBaAccountFriendCode(code: String, serverIndex: Int? = null): String {
    val requiredLength = baAccountFriendCodeLength(serverIndex)
    val normalized = normalizeBaAccountFriendCodeInput(code, serverIndex)
    return if (normalized.length == requiredLength) {
        normalized
    } else {
        normalizeBaAccountFriendCodeInput(BA_DEFAULT_FRIEND_CODE, serverIndex)
    }
}

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

private fun baAccountNicknameMaxLength(serverIndex: Int?): Int =
    if (serverIndex?.coerceIn(0, 2) == BA_SERVER_INDEX_GLOBAL) {
        BA_GLOBAL_NICKNAME_MAX_LENGTH
    } else {
        BA_DEFAULT_NICKNAME_MAX_LENGTH
    }

private fun baAccountFriendCodeLength(serverIndex: Int?): Int =
    if (serverIndex?.coerceIn(0, 2) == BA_SERVER_INDEX_CN) {
        BA_CN_FRIEND_CODE_LENGTH
    } else {
        BA_DEFAULT_FRIEND_CODE_LENGTH
    }

private const val BA_SERVER_INDEX_CN = 0
private const val BA_SERVER_INDEX_GLOBAL = 1
private const val BA_DEFAULT_NICKNAME_MAX_LENGTH = 10
private const val BA_GLOBAL_NICKNAME_MAX_LENGTH = 12
private const val BA_CN_FRIEND_CODE_LENGTH = 7
private const val BA_DEFAULT_FRIEND_CODE_LENGTH = 8

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
                    arenaRefreshNotifyEnabled = it.arenaRefreshNotifyEnabled,
                    cafeVisitNotifyEnabled = it.cafeVisitNotifyEnabled,
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
        arenaRefreshNotifyEnabled = normalized.arenaRefreshNotifyEnabled,
        cafeVisitNotifyEnabled = normalized.cafeVisitNotifyEnabled,
    )
}
