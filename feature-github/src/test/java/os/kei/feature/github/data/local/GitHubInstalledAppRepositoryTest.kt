package os.kei.feature.github.data.local

import android.os.BadParcelableException
import android.os.DeadObjectException
import android.os.TransactionTooLargeException
import org.junit.Test
import os.kei.core.system.isPackageManagerBulkQueryFailure
import os.kei.feature.github.model.InstalledAppItem
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubInstalledAppRepositoryTest {
    @Test
    fun `installed package query detects binder parcel failures`() {
        assertTrue(BadParcelableException("short package list").isPackageManagerBulkQueryFailure())
        assertTrue(DeadObjectException().isPackageManagerBulkQueryFailure())
        assertTrue(TransactionTooLargeException().isPackageManagerBulkQueryFailure())
    }

    @Test
    fun `installed package query detects nested binder parcel failures`() {
        val wrapped =
            IllegalStateException("package manager failed", BadParcelableException("partial list"))

        assertTrue(wrapped.isPackageManagerBulkQueryFailure())
    }

    @Test
    fun `installed package query keeps ordinary failures distinct`() {
        val ordinary = IllegalArgumentException("bad package flag")

        assertEquals(false, ordinary.isPackageManagerBulkQueryFailure())
    }

    @Test
    fun `installed app scan scope keeps user apps and pinned system exceptions`() {
        val userApp = InstalledAppItem(
            label = "User",
            packageName = "com.example.user",
            isSystemApp = false,
        )
        val systemApp = InstalledAppItem(
            label = "System",
            packageName = "com.example.system",
            isSystemApp = true,
        )
        val pinnedSystemApp = InstalledAppItem(
            label = "Pinned",
            packageName = "com.example.pinned",
            isSystemApp = true,
        )

        val filtered = GitHubInstalledAppRepository.filterByScanScope(
            apps = listOf(userApp, systemApp, pinnedSystemApp),
            includeSystemApps = false,
            pinnedSystemPackageNames = setOf("COM.EXAMPLE.PINNED"),
        )

        assertEquals(listOf(userApp, pinnedSystemApp), filtered)
    }
}
