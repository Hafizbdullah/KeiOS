package os.kei.core.notification.live

import os.kei.core.notification.live.builder.NotificationRenderStyle

private const val MI_ISLAND_PROTOCOL_VERSION = 3

data class MiIslandCapability(
    val isHyperOS: Boolean,
    val focusProtocolVersion: Int,
    val supportsIslandFeature: Boolean,
    val hasFocusPermission: Boolean,
) {
    val supportsMiIslandProtocol: Boolean
        get() = focusProtocolVersion >= MI_ISLAND_PROTOCOL_VERSION

    val canBuildMiIslandTemplate: Boolean
        get() = isHyperOS && supportsMiIslandProtocol

    val hasRuntimeIslandSignal: Boolean
        get() = supportsIslandFeature || hasFocusPermission
}

data class MiIslandRenderDecision(
    val style: NotificationRenderStyle,
    val useXiaomiMagic: Boolean,
    val reason: MiIslandRenderReason,
    val preferSuperIsland: Boolean,
    val bypassRestriction: Boolean,
    val capability: MiIslandCapability,
) {
    fun logSummary(): String =
        "style=$style useXiaomiMagic=$useXiaomiMagic reason=${reason.logKey} " +
            "preferSuperIsland=$preferSuperIsland bypass=$bypassRestriction " +
            "hyperOS=${capability.isHyperOS} protocol=${capability.focusProtocolVersion} " +
            "islandFeature=${capability.supportsIslandFeature} " +
            "focusPermission=${capability.hasFocusPermission}"
}

enum class MiIslandRenderReason(val logKey: String) {
    Selected("selected"),
    DisabledByUser("disabled_by_user"),
    HyperOsUnavailable("hyper_os_unavailable"),
    ProtocolUnavailable("protocol_unavailable"),
    IslandSignalUnavailable("island_signal_unavailable"),
    FocusPermissionRequired("focus_permission_required"),
}

object MiIslandRenderPolicy {
    fun resolve(
        preferSuperIsland: Boolean,
        bypassRestriction: Boolean,
        capability: MiIslandCapability,
    ): MiIslandRenderDecision {
        if (!preferSuperIsland) {
            return liveUpdate(
                reason = MiIslandRenderReason.DisabledByUser,
                preferSuperIsland = preferSuperIsland,
                bypassRestriction = bypassRestriction,
                capability = capability,
            )
        }
        if (!capability.isHyperOS) {
            return liveUpdate(
                reason = MiIslandRenderReason.HyperOsUnavailable,
                preferSuperIsland = preferSuperIsland,
                bypassRestriction = bypassRestriction,
                capability = capability,
            )
        }
        if (!capability.supportsMiIslandProtocol) {
            return liveUpdate(
                reason = MiIslandRenderReason.ProtocolUnavailable,
                preferSuperIsland = preferSuperIsland,
                bypassRestriction = bypassRestriction,
                capability = capability,
            )
        }
        if (!capability.hasRuntimeIslandSignal && !bypassRestriction) {
            return liveUpdate(
                reason = MiIslandRenderReason.IslandSignalUnavailable,
                preferSuperIsland = preferSuperIsland,
                bypassRestriction = bypassRestriction,
                capability = capability,
            )
        }
        if (!capability.hasFocusPermission && !bypassRestriction) {
            return liveUpdate(
                reason = MiIslandRenderReason.FocusPermissionRequired,
                preferSuperIsland = preferSuperIsland,
                bypassRestriction = bypassRestriction,
                capability = capability,
            )
        }
        return MiIslandRenderDecision(
            style = NotificationRenderStyle.MI_ISLAND,
            useXiaomiMagic = bypassRestriction,
            reason = MiIslandRenderReason.Selected,
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
            capability = capability,
        )
    }

    private fun liveUpdate(
        reason: MiIslandRenderReason,
        preferSuperIsland: Boolean,
        bypassRestriction: Boolean,
        capability: MiIslandCapability,
    ): MiIslandRenderDecision =
        MiIslandRenderDecision(
            style = NotificationRenderStyle.LIVE_UPDATE,
            useXiaomiMagic = false,
            reason = reason,
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
            capability = capability,
        )
}
