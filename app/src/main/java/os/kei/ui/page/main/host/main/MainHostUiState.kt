package os.kei.ui.page.main.host.main

import androidx.compose.runtime.Immutable
import os.kei.core.prefs.AppThemeMode
import os.kei.core.privilege.PrivilegeModeRuntime
import os.kei.core.privilege.PrivilegeStatus

/**
 * Single immutable snapshot of all `MainActivity`-owned UI state that flows into [MainScreen].
 *
 * The previous design exposed 11 separate `mutableStateOf`/`mutableIntStateOf` fields directly on
 * the Activity, which made every Compose recomposition of `MainScreen` re-read the activity's
 * current snapshot piecemeal and forced 19 individual parameters across the call site. This holder
 * collapses that surface into one stable `@Immutable` payload so:
 *
 * - Compose's structural-equality skipping can short-circuit recomposition when only unrelated
 *   parts of the activity state change.
 * - Intent-driven shortcut tokens are grouped with the bottom-page request they belong to, instead
 *   of being scattered across the activity body.
 * - Future activity-level fields can be added without growing the [MainScreen] parameter list.
 */
@Immutable
data class MainHostUiState(
    val privilegeStatus: PrivilegeStatus,
    val appThemeMode: AppThemeMode,
    val notificationPermissionGranted: Boolean,
    val transientExternalLaunchActive: Boolean,
    val requestedBottomPage: String?,
    val requestedBottomPageToken: Int,
    val requestedGitHubRefreshToken: Int,
    val requestedGitHubActionsTrackId: String?,
    val requestedGitHubActionsSheetToken: Int,
    val requestedBaBgmPlaybackToken: Int,
    val requestedBaAccountId: String?,
    val requestedBaAccountToken: Int,
    val requestedWebDavSyncToken: Int,
) {
    companion object {
        val Initial: MainHostUiState = MainHostUiState(
            privilegeStatus = PrivilegeStatus.initializing(PrivilegeModeRuntime.mode),
            appThemeMode = AppThemeMode.FOLLOW_SYSTEM,
            notificationPermissionGranted = true,
            transientExternalLaunchActive = false,
            requestedBottomPage = null,
            requestedBottomPageToken = 0,
            requestedGitHubRefreshToken = 0,
            requestedGitHubActionsTrackId = null,
            requestedGitHubActionsSheetToken = 0,
            requestedBaBgmPlaybackToken = 0,
            requestedBaAccountId = null,
            requestedBaAccountToken = 0,
            requestedWebDavSyncToken = 0,
        )
    }
}

/**
 * Stable bundle of activity-level callbacks consumed by [MainScreen]. Extracting them as a holder
 * lets Compose treat the lambda set as one unchanging reference for the lifetime of the Activity,
 * so adding or removing callbacks does not force every leaf composable to recompose.
 */
@Immutable
class MainHostCallbacks(
    val onCheckOrRequestPrivilege: () -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onAppThemeModeChanged: (AppThemeMode) -> Unit,
    val onRequestedBottomPageConsumed: () -> Unit
)
