package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaAccountTransferTest {
    @Test
    fun `export json round trips account state and count`() {
        val activeAccountId = BaAccountId("cn-alt")
        val snapshot =
            BaAccountStoreSnapshot(
                accounts =
                    listOf(
                        testAccount(id = "cn-main", serverIndex = 0, sortOrder = 0),
                        testAccount(id = activeAccountId.value, serverIndex = 0, sortOrder = 1),
                    ),
                activeAccountId = activeAccountId,
                allAccountsFollowGlobalNotificationSettings = false,
                globalReminderSettings =
                    BaGlobalReminderSettings(
                        apNotifyEnabled = true,
                        apNotifyThreshold = 120,
                        cafeApNotifyEnabled = true,
                        cafeApNotifyThreshold = 80,
                    ),
            )

        val raw = buildBaAccountsExportJson(snapshot = snapshot, nowMs = 42L)
        val parsed = parseBaAccountsExportJson(raw)

        assertEquals(2, countBaAccountsExportJson(raw))
        assertEquals(42L, parsed.exportedAtMs)
        assertEquals(activeAccountId, parsed.activeAccountId)
        assertFalse(parsed.allAccountsFollowGlobalNotificationSettings)
        assertTrue(parsed.globalReminderSettings.apNotifyEnabled)
        assertEquals(120, parsed.globalReminderSettings.apNotifyThreshold)
        assertEquals(listOf("cn-main", "cn-alt"), parsed.accounts.map { it.profile.id.value })
    }

    @Test
    fun `merge keeps local account fields when remote copy has no newer field timestamps`() {
        val localActiveAccountId = BaAccountId("local-only")
        val local =
            BaAccountStoreSnapshot(
                accounts =
                    listOf(
                        testAccount(
                            id = "cn-main",
                            serverIndex = 0,
                            nickname = "Local",
                            sortOrder = 0,
                            runtime = BaAccountRuntime(apCurrent = 66.0),
                            profileUpdatedAtMs = 1_000L,
                            runtimeUpdatedAtMs = 1_000L,
                        ),
                        testAccount(id = localActiveAccountId.value, serverIndex = 1, sortOrder = 1),
                    ),
                activeAccountId = localActiveAccountId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
                activeAccountUpdatedAtMs = 1_000L,
                allAccountsFollowGlobalNotificationSettingsUpdatedAtMs = 1_000L,
                globalReminderSettingsUpdatedAtMs = 1_000L,
            )
        val remoteActiveAccountId = BaAccountId("jp-alt")
        val remote =
            BaAccountsTransferPayload(
                exportedAtMs = 100L,
                accounts =
                    listOf(
                        testAccount(id = remoteActiveAccountId.value, serverIndex = 2, sortOrder = 0),
                        testAccount(
                            id = "cn-main",
                            serverIndex = 0,
                            nickname = "Remote",
                            sortOrder = 1,
                        ).copy(runtime = BaAccountRuntime(apCurrent = 88.0)),
                    ),
                activeAccountId = remoteActiveAccountId,
                allAccountsFollowGlobalNotificationSettings = false,
                globalReminderSettings =
                    BaGlobalReminderSettings(
                        apNotifyEnabled = true,
                        apNotifyThreshold = 90,
                    ),
            )

        val merged = mergeBaAccountsForSync(local = local, remote = remote, nowMs = 200L)

        assertEquals(
            listOf("cn-main", "local-only", "jp-alt"),
            merged.accounts.map { it.profile.id.value },
        )
        assertEquals(localActiveAccountId, merged.activeAccountId)
        assertTrue(merged.allAccountsFollowGlobalNotificationSettings)
        assertFalse(merged.globalReminderSettings.apNotifyEnabled)
        val preserved = merged.accounts.first { it.profile.id.value == "cn-main" }
        assertEquals("Local", preserved.profile.nickname)
        assertEquals(66.0, preserved.runtime.apCurrent)
        assertEquals(remoteActiveAccountId, merged.accounts.last().profile.id)
    }

    @Test
    fun `merge applies remote account fields when remote field timestamps are newer`() {
        val accountId = BaAccountId("cn-main")
        val local =
            BaAccountStoreSnapshot(
                accounts =
                    listOf(
                        testAccount(
                            id = accountId.value,
                            serverIndex = 0,
                            nickname = "Local",
                            runtime = BaAccountRuntime(apCurrent = 66.0),
                            profileUpdatedAtMs = 1_000L,
                            runtimeUpdatedAtMs = 1_000L,
                        ),
                    ),
                activeAccountId = accountId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
                activeAccountUpdatedAtMs = 1_000L,
                allAccountsFollowGlobalNotificationSettingsUpdatedAtMs = 1_000L,
                globalReminderSettingsUpdatedAtMs = 1_000L,
            )
        val remote =
            BaAccountsTransferPayload(
                exportedAtMs = 100L,
                accounts =
                    listOf(
                        testAccount(
                            id = accountId.value,
                            serverIndex = 0,
                            nickname = "Remote",
                            runtime = BaAccountRuntime(apCurrent = 88.0),
                            profileUpdatedAtMs = 2_000L,
                            runtimeUpdatedAtMs = 2_000L,
                        ),
                    ),
                activeAccountId = accountId,
                allAccountsFollowGlobalNotificationSettings = false,
                globalReminderSettings = BaGlobalReminderSettings(apNotifyEnabled = true),
                activeAccountUpdatedAtMs = 2_000L,
                allAccountsFollowGlobalNotificationSettingsUpdatedAtMs = 2_000L,
                globalReminderSettingsUpdatedAtMs = 2_000L,
            )

        val merged = mergeBaAccountsForSync(local = local, remote = remote, nowMs = 3_000L)

        assertEquals(accountId, merged.activeAccountId)
        assertFalse(merged.allAccountsFollowGlobalNotificationSettings)
        assertTrue(merged.globalReminderSettings.apNotifyEnabled)
        val updated = merged.accounts.single()
        assertEquals("Remote", updated.profile.nickname)
        assertEquals(88.0, updated.runtime.apCurrent)
        assertEquals(2_000L, updated.profileUpdatedAtMs)
        assertEquals(2_000L, updated.runtimeUpdatedAtMs)
    }

    @Test
    fun `stable sync fingerprint ignores export time metadata`() {
        val snapshot =
            BaAccountStoreSnapshot(
                accounts = listOf(testAccount(id = "cn-main", serverIndex = 0)),
                activeAccountId = BaAccountId("cn-main"),
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )

        val first = buildBaAccountsSyncFingerprintJson(buildBaAccountsExportJson(snapshot, nowMs = 1_000L))
        val second = buildBaAccountsSyncFingerprintJson(buildBaAccountsExportJson(snapshot, nowMs = 2_000L))

        assertEquals(first, second)
    }

    @Test
    fun `merge falls back to local active account when remote active is missing`() {
        val localActiveAccountId = BaAccountId("cn-main")
        val local =
            BaAccountStoreSnapshot(
                accounts = listOf(testAccount(id = localActiveAccountId.value, serverIndex = 0)),
                activeAccountId = localActiveAccountId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )
        val remote =
            BaAccountsTransferPayload(
                accounts = listOf(testAccount(id = "jp-alt", serverIndex = 2)),
                activeAccountId = BaAccountId("missing"),
            )

        val merged = mergeBaAccountsForSync(local = local, remote = remote, nowMs = 10L)

        assertEquals(localActiveAccountId, merged.activeAccountId)
        assertEquals(listOf("cn-main", "jp-alt"), merged.accounts.map { it.profile.id.value })
    }

    @Test
    fun `merge falls back to valid local active account when newer remote active is invalid`() {
        val localActiveAccountId = BaAccountId("cn-main")
        val local =
            BaAccountStoreSnapshot(
                accounts = listOf(testAccount(id = localActiveAccountId.value, serverIndex = 0)),
                activeAccountId = localActiveAccountId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
                activeAccountUpdatedAtMs = 1_000L,
            )
        val remote =
            BaAccountsTransferPayload(
                accounts = listOf(testAccount(id = "jp-alt", serverIndex = 2)),
                activeAccountId = BaAccountId("missing"),
                activeAccountUpdatedAtMs = 2_000L,
            )

        val merged = mergeBaAccountsForSync(local = local, remote = remote, nowMs = 3_000L)

        assertEquals(localActiveAccountId, merged.activeAccountId)
        assertEquals(listOf("cn-main", "jp-alt"), merged.accounts.map { it.profile.id.value })
    }

    private fun testAccount(
        id: String,
        serverIndex: Int,
        nickname: String = "Kei",
        sortOrder: Int = 0,
        runtime: BaAccountRuntime = BaAccountRuntime(),
        profileUpdatedAtMs: Long = 0L,
        runtimeUpdatedAtMs: Long = 0L,
    ): BaAccountRecord =
        BaAccountRecord(
            profile =
                BaAccountProfile(
                    id = BaAccountId(id),
                    serverIndex = serverIndex,
                    displayName = nickname,
                    nickname = nickname,
                    friendCode = "ABCDEFGH",
                    sortOrder = sortOrder,
                ),
            runtime = runtime,
            profileUpdatedAtMs = profileUpdatedAtMs,
            runtimeUpdatedAtMs = runtimeUpdatedAtMs,
        )
}
