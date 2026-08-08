package os.kei.ui.page.main.widget.glass

import org.junit.Test
import kotlin.test.assertEquals

/**
 * The ordering rule for [LiquidOverlayHostState].
 *
 * Registration order is right for presentations — a sheet opened from a sheet should stack above it —
 * and wrong for notifications, because the toast host mounts once at the app root and therefore always
 * registers first. Without the split it would draw underneath every sheet and alert opened afterwards,
 * which is the opposite of what a toast is for.
 */
class LiquidOverlayPlacementTest {
    private data class Entry(
        val name: String,
        val layer: LiquidOverlayLayer,
    )

    private fun place(vararg entries: Entry): List<LiquidOverlayPlacement<Entry>> = liquidOverlayPlacements(entries.toList()) { it.layer }

    @Test
    fun aNotificationRegisteredFirstStillComposesLast() {
        val placements =
            place(
                Entry("toast", LiquidOverlayLayer.Notification),
                Entry("sheet", LiquidOverlayLayer.Presentation),
                Entry("alert", LiquidOverlayLayer.Presentation),
            )

        assertEquals(
            listOf("sheet", "alert", "toast"),
            placements.map { it.entry.name },
            "the toast registered first but must still draw on top",
        )
    }

    @Test
    fun presentationsKeepRegistrationOrderAndCountUpFromZero() {
        val placements =
            place(
                Entry("sheet", LiquidOverlayLayer.Presentation),
                Entry("alert", LiquidOverlayLayer.Presentation),
            )

        assertEquals(listOf("sheet", "alert"), placements.map { it.entry.name })
        // Depth 0 is the only one allowed to paint a full-screen blurred plate; the alert above it must
        // dim the sheet rather than replace it with a blurred copy of the page.
        assertEquals(listOf(0, 1), placements.map { it.depth })
    }

    @Test
    fun notificationDepthReportsThePresentationsBeneathIt() {
        val placements =
            place(
                Entry("toast", LiquidOverlayLayer.Notification),
                Entry("sheet", LiquidOverlayLayer.Presentation),
            )

        val toast = placements.single { it.entry.name == "toast" }
        assertEquals(1, toast.depth, "one presentation sits beneath the toast")
    }

    @Test
    fun aLoneNotificationSitsAtTheBottomOfTheStack() {
        val placements = place(Entry("toast", LiquidOverlayLayer.Notification))

        assertEquals(0, placements.single().depth)
    }

    @Test
    fun nothingRegisteredPlacesNothing() {
        assertEquals(emptyList(), liquidOverlayPlacements(emptyList<Entry>()) { it.layer })
    }
}
