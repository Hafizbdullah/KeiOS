package os.kei.core.platform

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredictiveBackOemCompatTest {
    @Test
    fun `hyperos commits local back and finishes activities through the framework`() {
        val policy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = true,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "Xiaomi",
                manufacturer = "Xiaomi",
                display = "OS3.0.306.4.WBLCNXM",
                model = "Xiaomi 17 Pro",
                properties = mapOf(
                    "ro.mi.os.version.name" to "OS3.0",
                    "ro.mi.os.version.incremental" to "OS3.0.306.4.WBLCNXM"
                )
            )
        )

        assertEquals(PredictiveBackOemCompat.RomFamily.HyperOs, policy.romFamily)
        assertTrue(policy.frameworkAnimationsEnabled)
        assertEquals(
            PredictiveBackOemCompat.LocalBackPipeline.CommitOnly,
            policy.localBackPipeline
        )
        assertEquals(
            PredictiveBackOemCompat.ActivityBackPipeline.FrameworkFinish,
            policy.activityBackPipeline
        )
    }

    @Test
    fun `aosp drives local back through compose predictive back`() {
        val policy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = true,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "google",
                manufacturer = "Google",
                display = "CP21.260330.005",
                model = "Pixel 10 Pro",
                properties = emptyMap()
            )
        )

        assertEquals(PredictiveBackOemCompat.RomFamily.Aosp, policy.romFamily)
        assertTrue(policy.frameworkAnimationsEnabled)
        assertEquals(
            PredictiveBackOemCompat.LocalBackPipeline.ComposePredictive,
            policy.localBackPipeline
        )
        assertEquals(
            PredictiveBackOemCompat.ActivityBackPipeline.FrameworkFinish,
            policy.activityBackPipeline
        )
    }

    @Test
    fun `coloros commits local back and finishes activities through the framework`() {
        val policy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = true,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "OnePlus",
                manufacturer = "OPPO",
                display = "ColorOS",
                model = "PKR110",
                properties = mapOf("ro.build.version.oplusrom" to "V17")
            )
        )

        assertEquals(PredictiveBackOemCompat.RomFamily.ColorOs, policy.romFamily)
        assertTrue(policy.frameworkAnimationsEnabled)
        assertEquals(
            PredictiveBackOemCompat.LocalBackPipeline.CommitOnly,
            policy.localBackPipeline
        )
        assertEquals(
            PredictiveBackOemCompat.ActivityBackPipeline.FrameworkFinish,
            policy.activityBackPipeline
        )
    }

    @Test
    fun `miui and xiaomi families commit local back`() {
        val miuiPolicy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = true,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "Xiaomi",
                manufacturer = "Xiaomi",
                display = "MIUI",
                model = "Xiaomi",
                properties = mapOf("ro.miui.ui.version.name" to "V15")
            )
        )
        val xiaomiPolicy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = true,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "Redmi",
                manufacturer = "Xiaomi",
                display = "Android",
                model = "Redmi",
                properties = emptyMap()
            )
        )

        listOf(miuiPolicy, xiaomiPolicy).forEach { policy ->
            assertFalse(policy.localPredictiveBackEnabled)
            assertEquals(
                PredictiveBackOemCompat.LocalBackPipeline.CommitOnly,
                policy.localBackPipeline
            )
            assertEquals(
                PredictiveBackOemCompat.ActivityBackPipeline.FrameworkFinish,
                policy.activityBackPipeline
            )
        }
        assertEquals(PredictiveBackOemCompat.RomFamily.Miui, miuiPolicy.romFamily)
        assertEquals(PredictiveBackOemCompat.RomFamily.Xiaomi, xiaomiPolicy.romFamily)
    }

    @Test
    fun `disabled user setting disables framework predictive animations`() {
        val policy = PredictiveBackOemCompat.resolvePolicy(
            transitionAnimationsEnabled = true,
            predictiveBackAnimationsEnabled = false,
            signals = PredictiveBackOemCompat.DeviceSignals(
                brand = "Xiaomi",
                manufacturer = "Xiaomi",
                display = "OS3.0",
                model = "Xiaomi",
                properties = mapOf("ro.mi.os.version.name" to "OS3.0")
            )
        )

        assertEquals(PredictiveBackOemCompat.RomFamily.HyperOs, policy.romFamily)
        assertFalse(policy.frameworkAnimationsEnabled)
        assertEquals(
            PredictiveBackOemCompat.LocalBackPipeline.CommitOnly,
            policy.localBackPipeline
        )
        assertEquals(
            PredictiveBackOemCompat.ActivityBackPipeline.CommitCallback,
            policy.activityBackPipeline
        )
    }
}
