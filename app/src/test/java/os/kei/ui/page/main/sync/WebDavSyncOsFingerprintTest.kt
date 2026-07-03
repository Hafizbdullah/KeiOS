package os.kei.ui.page.main.sync

import org.junit.Test
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtra
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtraType
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WebDavSyncOsFingerprintTest {
    @Test
    fun `activity card sync fingerprint ignores export timestamp`() {
        val defaults = OsGoogleSystemServiceConfig(intentFlags = "FLAG_ACTIVITY_NEW_TASK")
        val card =
            OsActivityShortcutCard(
                id = "activity-settings",
                visible = true,
                config =
                    defaults.copy(
                        title = "Settings",
                        subtitle = "System",
                        appName = "Settings",
                        packageName = "com.android.settings",
                        className = "com.android.settings.Settings",
                        intentAction = "android.intent.action.MAIN",
                    ),
            )
        val firstRaw =
            OsActivityShortcutCardStore.buildCardsExportJson(
                cards = listOf(card),
                defaults = defaults,
                exportedAtMillis = 1_000L,
            )
        val secondRaw =
            OsActivityShortcutCardStore.buildCardsExportJson(
                cards = listOf(card),
                defaults = defaults,
                exportedAtMillis = 2_000L,
            )

        val firstFingerprint =
            buildRemoteOsActivityCardsSyncFingerprintJson(
                raw = firstRaw,
                defaults = defaults,
                builtInSampleDefaults = defaults,
                builtInActivityShortcutCards = emptyList(),
            )
        val secondFingerprint =
            buildRemoteOsActivityCardsSyncFingerprintJson(
                raw = secondRaw,
                defaults = defaults,
                builtInSampleDefaults = defaults,
                builtInActivityShortcutCards = emptyList(),
            )
        val changedFingerprint =
            buildRemoteOsActivityCardsSyncFingerprintJson(
                raw =
                    OsActivityShortcutCardStore.buildCardsExportJson(
                        cards = listOf(card.copy(visible = false)),
                        defaults = defaults,
                        exportedAtMillis = 3_000L,
                    ),
                defaults = defaults,
                builtInSampleDefaults = defaults,
                builtInActivityShortcutCards = emptyList(),
            )

        assertEquals(firstFingerprint, secondFingerprint)
        assertNotEquals(firstFingerprint, changedFingerprint)
    }

    @Test
    fun `activity card sync fingerprint uses functional identity`() {
        val defaults = OsGoogleSystemServiceConfig(intentFlags = "FLAG_ACTIVITY_NEW_TASK")
        val first =
            OsActivityShortcutCard(
                id = "activity-local-id",
                visible = true,
                isBuiltInSample = false,
                config =
                    defaults.copy(
                        title = "Settings",
                        subtitle = "System",
                        appName = "Settings",
                        packageName = "com.android.settings",
                        className = "com.android.settings.Settings",
                        intentAction = "android.intent.action.MAIN",
                        intentExtras =
                            listOf(
                                ShortcutIntentExtra(
                                    key = "z",
                                    type = ShortcutIntentExtraType.String,
                                    value = "last",
                                ),
                                ShortcutIntentExtra(
                                    key = "a",
                                    type = ShortcutIntentExtraType.Boolean,
                                    value = "true",
                                ),
                            ),
                    ),
            )
        val second =
            first.copy(
                id = "activity-remote-id",
                isBuiltInSample = false,
                config =
                    first.config.copy(
                        intentExtras = first.config.intentExtras.reversed(),
                    ),
            )
        val changed = second.copy(visible = false)

        val firstFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(first), defaults)
        val secondFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(second), defaults)
        val changedFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(changed), defaults)

        assertEquals(firstFingerprint, secondFingerprint)
        assertNotEquals(firstFingerprint, changedFingerprint)
    }

    @Test
    fun `activity card sync fingerprint ignores built in template body`() {
        val defaults = OsGoogleSystemServiceConfig(intentFlags = "FLAG_ACTIVITY_NEW_TASK")
        val first =
            OsActivityShortcutCard(
                id = "builtin-settings-extra-dim",
                visible = true,
                isBuiltInSample = true,
                config =
                    defaults.copy(
                        title = "Extra dim",
                        subtitle = "Display",
                        appName = "Settings",
                        packageName = "com.android.settings",
                        className = "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity",
                        intentAction = "android.settings.REDUCE_BRIGHT_COLORS_SETTINGS",
                    ),
            )
        val second =
            first.copy(
                config =
                    first.config.copy(
                        title = "Extra dim updated",
                        subtitle = "Display updated",
                        intentAction = "android.settings.REDUCE_BRIGHT_COLORS_SETTINGS_UPDATED",
                    ),
            )
        val hidden = second.copy(visible = false)

        val firstFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(first), defaults)
        val secondFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(second), defaults)
        val hiddenFingerprint = buildOsActivityCardsSyncFingerprintJson(listOf(hidden), defaults)

        assertEquals(firstFingerprint, secondFingerprint)
        assertNotEquals(firstFingerprint, hiddenFingerprint)
    }

    @Test
    fun `shell card sync fingerprint ignores volatile run state`() {
        val card =
            OsShellCommandCard(
                id = "shell-settings",
                visible = true,
                title = "Settings",
                subtitle = "Global",
                command = "settings list global",
                runOutput = "old output",
                lastRunAtMillis = 10L,
                createdAtMillis = 100L,
                updatedAtMillis = 200L,
            )
        val firstRaw =
            OsShellCommandCardStore.buildCardsExportJson(
                cards = listOf(card),
                exportedAtMillis = 1_000L,
            )
        val secondRaw =
            OsShellCommandCardStore.buildCardsExportJson(
                cards =
                    listOf(
                        card.copy(
                            runOutput = "new output",
                            lastRunAtMillis = 20L,
                            createdAtMillis = 300L,
                            updatedAtMillis = 400L,
                        ),
                    ),
                exportedAtMillis = 2_000L,
            )

        val firstFingerprint = buildRemoteOsShellCardsSyncFingerprintJson(firstRaw)
        val secondFingerprint = buildRemoteOsShellCardsSyncFingerprintJson(secondRaw)
        val changedFingerprint =
            buildRemoteOsShellCardsSyncFingerprintJson(
                OsShellCommandCardStore.buildCardsExportJson(
                    cards = listOf(card.copy(command = "settings list secure")),
                    exportedAtMillis = 3_000L,
                ),
            )

        assertEquals(firstFingerprint, secondFingerprint)
        assertNotEquals(firstFingerprint, changedFingerprint)
    }
}
