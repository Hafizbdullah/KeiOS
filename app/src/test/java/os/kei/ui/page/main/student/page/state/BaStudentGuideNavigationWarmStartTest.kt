package os.kei.ui.page.main.student.page.state

import org.junit.After
import org.junit.Test
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.BaStudentGuideInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaStudentGuideNavigationWarmStartTest {
    @After
    fun tearDown() {
        BaStudentGuideNavigationWarmStartStore.clearForTest()
    }

    @Test
    fun `reserved id yields nothing until fulfilled`() {
        val id = BaStudentGuideNavigationWarmStartStore.reserve()

        // Navigation starts immediately, so the destination may consume before the snapshot lands.
        assertNull(BaStudentGuideNavigationWarmStartStore.consume(id))

        val snapshot =
            BaStudentGuideNavigationWarmStart(
                sourceUrl = "https://www.gamekee.com/ba/tj/10001.html",
                info = guideInfo(),
            )
        BaStudentGuideNavigationWarmStartStore.fulfil(id, snapshot)

        assertEquals(snapshot, BaStudentGuideNavigationWarmStartStore.consume(id))
    }

    @Test
    fun `a slow abandoned navigation cannot overwrite a newer one`() {
        val stale = BaStudentGuideNavigationWarmStartStore.reserve()
        val fresh = BaStudentGuideNavigationWarmStartStore.reserve()
        val freshSnapshot = BaStudentGuideNavigationWarmStart(sourceUrl = "fresh", info = guideInfo())

        BaStudentGuideNavigationWarmStartStore.fulfil(fresh, freshSnapshot)
        BaStudentGuideNavigationWarmStartStore.fulfil(
            stale,
            BaStudentGuideNavigationWarmStart(sourceUrl = "stale", info = guideInfo()),
        )

        assertNull(BaStudentGuideNavigationWarmStartStore.consume(stale))
        assertEquals(freshSnapshot, BaStudentGuideNavigationWarmStartStore.consume(fresh))
    }

    @Test
    fun `warm start handoff is consumed once by its route id`() {
        val snapshot =
            BaStudentGuideNavigationWarmStart(
                sourceUrl = "https://www.gamekee.com/ba/tj/10001.html",
                info = guideInfo(),
            )

        val id = BaStudentGuideNavigationWarmStartStore.publish(snapshot)

        assertNull(BaStudentGuideNavigationWarmStartStore.consume(id + 1L))
        assertEquals(snapshot, BaStudentGuideNavigationWarmStartStore.consume(id))
        assertNull(BaStudentGuideNavigationWarmStartStore.consume(id))
    }

    @Test
    fun `cached warm start keeps content visible during background validation`() {
        assertFalse(
            shouldShowBaStudentGuideBlockingLoading(
                currentInfo = guideInfo(),
                manualRefresh = false,
            ),
        )
        assertTrue(
            shouldShowBaStudentGuideBlockingLoading(
                currentInfo = null,
                manualRefresh = false,
            ),
        )
        assertTrue(
            shouldShowBaStudentGuideBlockingLoading(
                currentInfo = guideInfo(),
                manualRefresh = true,
            ),
        )
    }

    private fun guideInfo(): BaStudentGuideInfo =
        BaStudentGuideInfo(
            sourceUrl = "https://www.gamekee.com/ba/tj/10001.html",
            title = "测试学生",
            subtitle = "GameKee",
            description = "desc",
            imageUrl = "https://example.com/student.png",
            summary = "summary",
            stats = listOf("学校" to "夏莱"),
            profileRows = listOf(BaGuideRow("学校", "夏莱")),
            syncedAtMs = 1_000L,
        )
}
