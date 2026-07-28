package os.kei.core.privilege

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Privileged execution backend selected by the user.
 *
 * [Disabled] keeps privileged entry points inactive while ordinary app features continue to work.
 * [Shizuku] proxies work through the Shizuku server, which runs as `shell` (uid 2000) or `root`.
 * [Root] spawns `su` directly from the app process and needs a superuser manager such as Magisk,
 * KernelSU, or APatch.
 */
enum class PrivilegeMode(val storageId: String) {
    Disabled("disabled"),
    Shizuku("shizuku"),
    Root("root"),
    ;

    companion object {
        val Default: PrivilegeMode = Shizuku

        fun fromStorageId(raw: String?): PrivilegeMode {
            val normalized = raw.orEmpty().trim()
            return entries.firstOrNull { it.storageId == normalized } ?: Default
        }
    }
}

/**
 * Capabilities a backend can serve.
 *
 * [ShellCommand] is the only capability both backends provide. The binder capabilities hand the app
 * process a live system-service binder whose transactions run under a privileged uid, which needs a
 * long-lived privileged host process. Shizuku provides one; a `su` child process exits and takes its
 * binder with it, so [Root] falls back to shell equivalents for those surfaces.
 */
enum class PrivilegeCapability {
    ShellCommand,
    BinderPackageInstaller,
    BinderUidFirewall,
}

/**
 * Process-wide selected mode.
 *
 * Privileged entry points are created in several places that never see the settings screen —
 * background keep-alive checks, MCP tool serving, launcher shortcuts — so the selection lives here
 * rather than being threaded from the UI. [configure] runs once at startup from persisted prefs and
 * [set] applies a user switch.
 */
object PrivilegeModeRuntime {
    private val listeners = CopyOnWriteArrayList<(PrivilegeMode) -> Unit>()

    @Volatile
    var mode: PrivilegeMode = PrivilegeMode.Default
        private set

    fun configure(mode: PrivilegeMode) {
        this.mode = mode
    }

    fun set(mode: PrivilegeMode) {
        if (this.mode == mode) return
        this.mode = mode
        listeners.forEach { listener -> runCatching { listener(mode) } }
    }

    fun addListener(listener: (PrivilegeMode) -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: (PrivilegeMode) -> Unit) {
        listeners.remove(listener)
    }
}
