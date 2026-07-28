package os.kei.core.privilege

/**
 * Structured privileged-runtime state.
 *
 * Presentation layers switch on [PrivilegeStatus.code] and localise it themselves. [PrivilegeStatus.text]
 * stays English and is reserved for diagnostics that need a flat string: exports, MCP responses, and
 * the OS page info rows.
 */
enum class PrivilegeStatusCode {
    Disabled,
    Initializing,
    Ready,
    ServiceUnavailable,
    ServiceDisconnected,
    PreV11Unsupported,
    PermissionNotGranted,
    PermissionDenied,
    PermissionBlocked,
    RequestingPermission,
    UnsupportedIdentity,
    InitFailed,
    RequestFailed,
    Notice,
}

data class PrivilegeStatus(
    val mode: PrivilegeMode,
    val code: PrivilegeStatusCode,
    val detail: String = "",
) {
    val isCommandReady: Boolean
        get() = code == PrivilegeStatusCode.Ready

    val text: String
        get() = when (mode) {
            PrivilegeMode.Disabled -> "Privilege mode: disabled"
            PrivilegeMode.Shizuku -> shizukuText()
            PrivilegeMode.Root -> rootText()
        }

    private fun shizukuText(): String = when (code) {
        PrivilegeStatusCode.Disabled -> "Privilege mode: disabled"
        PrivilegeStatusCode.Initializing -> "Shizuku status: initializing..."
        PrivilegeStatusCode.Ready -> "Shizuku permission: granted ($detail)"
        PrivilegeStatusCode.ServiceUnavailable -> "Shizuku service unavailable (start Shizuku app first)"
        PrivilegeStatusCode.ServiceDisconnected -> "Shizuku service disconnected"
        PrivilegeStatusCode.PreV11Unsupported -> "Shizuku pre-v11 is unsupported"
        PrivilegeStatusCode.PermissionNotGranted -> "Shizuku permission: not granted"
        PrivilegeStatusCode.PermissionDenied -> "Shizuku permission: denied"
        PrivilegeStatusCode.PermissionBlocked -> "Shizuku permission blocked; grant it in Shizuku manager"
        PrivilegeStatusCode.RequestingPermission -> "Requesting Shizuku permission..."
        PrivilegeStatusCode.UnsupportedIdentity -> "Shizuku command unavailable: unsupported service uid $detail"
        PrivilegeStatusCode.InitFailed -> "Shizuku init failed: $detail"
        PrivilegeStatusCode.RequestFailed -> "Shizuku request failed: $detail"
        PrivilegeStatusCode.Notice -> detail
    }

    private fun rootText(): String = when (code) {
        PrivilegeStatusCode.Disabled -> "Privilege mode: disabled"
        PrivilegeStatusCode.Initializing -> "Root status: initializing..."
        PrivilegeStatusCode.Ready -> "Root permission: granted ($detail)"
        PrivilegeStatusCode.ServiceUnavailable -> "Root unavailable (no su binary found)"
        PrivilegeStatusCode.ServiceDisconnected -> "Root shell disconnected"
        PrivilegeStatusCode.PreV11Unsupported -> "Root manager is unsupported"
        PrivilegeStatusCode.PermissionNotGranted -> "Root permission: not granted"
        PrivilegeStatusCode.PermissionDenied -> "Root permission: denied by superuser manager"
        PrivilegeStatusCode.PermissionBlocked -> "Root permission blocked; grant it in your superuser manager"
        PrivilegeStatusCode.RequestingPermission -> "Requesting root permission..."
        PrivilegeStatusCode.UnsupportedIdentity -> "Root command unavailable: su returned uid $detail"
        PrivilegeStatusCode.InitFailed -> "Root init failed: $detail"
        PrivilegeStatusCode.RequestFailed -> "Root request failed: $detail"
        PrivilegeStatusCode.Notice -> detail
    }

    companion object {
        fun initializing(mode: PrivilegeMode): PrivilegeStatus =
            PrivilegeStatus(mode = mode, code = PrivilegeStatusCode.Initializing)
    }
}
