package os.kei.ui.page.main.settings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.privilege.PrivilegeStatus
import os.kei.core.privilege.PrivilegeStatusCode

/**
 * Localises a [PrivilegeStatus] for the permission card.
 *
 * The status travels as a structured code so each mode maps onto its own wording without the UI
 * having to recognise backend prose.
 */
@Composable
internal fun localizedPrivilegeStatusText(
    status: PrivilegeStatus,
    granted: Boolean,
): String = when (status.mode) {
    PrivilegeMode.Disabled -> stringResource(R.string.settings_privileged_mode_summary_disabled)
    PrivilegeMode.Shizuku -> shizukuStatusText(status, granted)
    PrivilegeMode.Root -> rootStatusText(status, granted)
}

@Composable
private fun shizukuStatusText(
    status: PrivilegeStatus,
    granted: Boolean,
): String = when (status.code) {
    PrivilegeStatusCode.Disabled -> stringResource(R.string.settings_privileged_mode_summary_disabled)
    PrivilegeStatusCode.Initializing -> stringResource(R.string.settings_privilege_status_initializing)
    PrivilegeStatusCode.Ready ->
        stringResource(
            R.string.settings_shizuku_status_permission_granted_identity,
            status.detail.ifBlank { if (granted) "shell" else "unknown" },
        )

    PrivilegeStatusCode.ServiceUnavailable -> stringResource(R.string.settings_shizuku_status_service_unavailable)
    PrivilegeStatusCode.ServiceDisconnected -> stringResource(R.string.settings_shizuku_status_service_disconnected)
    PrivilegeStatusCode.PreV11Unsupported -> stringResource(R.string.settings_shizuku_status_pre_v11_unsupported)
    PrivilegeStatusCode.PermissionNotGranted -> stringResource(R.string.settings_shizuku_status_permission_not_granted)
    PrivilegeStatusCode.PermissionDenied -> stringResource(R.string.settings_shizuku_status_permission_denied)
    PrivilegeStatusCode.PermissionBlocked -> stringResource(R.string.settings_shizuku_status_permission_blocked)
    PrivilegeStatusCode.RequestingPermission -> stringResource(R.string.settings_shizuku_status_requesting_permission)
    PrivilegeStatusCode.UnsupportedIdentity ->
        stringResource(
            R.string.settings_shizuku_status_unsupported_service_uid,
            status.detail.ifBlank { "unknown" },
        )

    PrivilegeStatusCode.InitFailed ->
        stringResource(R.string.settings_shizuku_status_init_failed, status.detail.ifBlank { "unknown" })

    PrivilegeStatusCode.RequestFailed ->
        stringResource(R.string.settings_shizuku_status_request_failed, status.detail.ifBlank { "unknown" })

    PrivilegeStatusCode.Notice -> status.detail.trim()
}

@Composable
private fun rootStatusText(
    status: PrivilegeStatus,
    granted: Boolean,
): String = when (status.code) {
    PrivilegeStatusCode.Disabled -> stringResource(R.string.settings_privileged_mode_summary_disabled)
    PrivilegeStatusCode.Initializing -> stringResource(R.string.settings_privilege_status_initializing)
    PrivilegeStatusCode.Ready ->
        stringResource(
            R.string.settings_root_status_permission_granted_identity,
            status.detail.ifBlank { if (granted) "root" else "unknown" },
        )

    PrivilegeStatusCode.ServiceUnavailable -> stringResource(R.string.settings_root_status_su_unavailable)
    PrivilegeStatusCode.ServiceDisconnected -> stringResource(R.string.settings_root_status_shell_disconnected)
    PrivilegeStatusCode.PreV11Unsupported -> stringResource(R.string.settings_root_status_manager_unsupported)
    PrivilegeStatusCode.PermissionNotGranted -> stringResource(R.string.settings_root_status_permission_not_granted)
    PrivilegeStatusCode.PermissionDenied -> stringResource(R.string.settings_root_status_permission_denied)
    PrivilegeStatusCode.PermissionBlocked -> stringResource(R.string.settings_root_status_permission_blocked)
    PrivilegeStatusCode.RequestingPermission -> stringResource(R.string.settings_root_status_requesting_permission)
    PrivilegeStatusCode.UnsupportedIdentity ->
        stringResource(R.string.settings_root_status_unsupported_uid, status.detail.ifBlank { "unknown" })

    PrivilegeStatusCode.InitFailed ->
        stringResource(R.string.settings_root_status_init_failed, status.detail.ifBlank { "unknown" })

    PrivilegeStatusCode.RequestFailed ->
        stringResource(R.string.settings_root_status_request_failed, status.detail.ifBlank { "unknown" })

    PrivilegeStatusCode.Notice -> status.detail.trim()
}
