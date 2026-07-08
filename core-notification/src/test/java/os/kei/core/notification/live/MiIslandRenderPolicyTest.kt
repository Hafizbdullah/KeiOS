package os.kei.core.notification.live

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import os.kei.core.notification.live.builder.NotificationRenderStyle

class MiIslandRenderPolicyTest {
    @Test
    fun `disabled user setting resolves to live update`() {
        val decision = resolve(
            preferSuperIsland = false,
            bypassRestriction = true,
            capability = capability(),
        )

        assertEquals(NotificationRenderStyle.LIVE_UPDATE, decision.style)
        assertFalse(decision.useXiaomiMagic)
        assertEquals(MiIslandRenderReason.DisabledByUser, decision.reason)
    }

    @Test
    fun `non hyper os resolves to live update`() {
        val decision = resolve(capability = capability(isHyperOS = false))

        assertEquals(NotificationRenderStyle.LIVE_UPDATE, decision.style)
        assertEquals(MiIslandRenderReason.HyperOsUnavailable, decision.reason)
    }

    @Test
    fun `unsupported focus protocol resolves to live update`() {
        val decision = resolve(capability = capability(focusProtocolVersion = 2))

        assertEquals(NotificationRenderStyle.LIVE_UPDATE, decision.style)
        assertEquals(MiIslandRenderReason.ProtocolUnavailable, decision.reason)
    }

    @Test
    fun `missing permission resolves to live update when magic bypass is off`() {
        val decision = resolve(
            bypassRestriction = false,
            capability = capability(
                supportsIslandFeature = true,
                hasFocusPermission = false,
            ),
        )

        assertEquals(NotificationRenderStyle.LIVE_UPDATE, decision.style)
        assertFalse(decision.useXiaomiMagic)
        assertEquals(MiIslandRenderReason.FocusPermissionRequired, decision.reason)
    }

    @Test
    fun `focus permission resolves to mi island without magic`() {
        val decision = resolve(
            bypassRestriction = false,
            capability = capability(
                supportsIslandFeature = true,
                hasFocusPermission = true,
            ),
        )

        assertEquals(NotificationRenderStyle.MI_ISLAND, decision.style)
        assertFalse(decision.useXiaomiMagic)
        assertEquals(MiIslandRenderReason.Selected, decision.reason)
    }

    @Test
    fun `magic bypass keeps mi island when permission is missing`() {
        val decision = resolve(
            bypassRestriction = true,
            capability = capability(
                supportsIslandFeature = true,
                hasFocusPermission = false,
            ),
        )

        assertEquals(NotificationRenderStyle.MI_ISLAND, decision.style)
        assertTrue(decision.useXiaomiMagic)
        assertEquals(MiIslandRenderReason.Selected, decision.reason)
    }

    @Test
    fun `magic bypass tolerates missing island property on compatible hyper os`() {
        val decision = resolve(
            bypassRestriction = true,
            capability = capability(
                supportsIslandFeature = false,
                hasFocusPermission = false,
            ),
        )

        assertEquals(NotificationRenderStyle.MI_ISLAND, decision.style)
        assertTrue(decision.useXiaomiMagic)
        assertEquals(MiIslandRenderReason.Selected, decision.reason)
    }

    private fun resolve(
        preferSuperIsland: Boolean = true,
        bypassRestriction: Boolean = false,
        capability: MiIslandCapability = capability(),
    ): MiIslandRenderDecision =
        MiIslandRenderPolicy.resolve(
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
            capability = capability,
        )

    private fun capability(
        isHyperOS: Boolean = true,
        focusProtocolVersion: Int = 3,
        supportsIslandFeature: Boolean = true,
        hasFocusPermission: Boolean = true,
    ): MiIslandCapability =
        MiIslandCapability(
            isHyperOS = isHyperOS,
            focusProtocolVersion = focusProtocolVersion,
            supportsIslandFeature = supportsIslandFeature,
            hasFocusPermission = hasFocusPermission,
        )
}
