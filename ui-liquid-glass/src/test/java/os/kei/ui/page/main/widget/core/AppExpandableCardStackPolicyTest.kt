package os.kei.ui.page.main.widget.core

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppExpandableCardStackPolicyTest {
    @Test
    fun `collapsed resting cards participate in the edge stack`() {
        assertTrue(
            shouldApplyEdgeStackToExpandableCard(
                currentState = false,
                targetState = false,
            ),
        )
    }

    @Test
    fun `expanding and expanded cards stay out of the edge stack`() {
        assertFalse(
            shouldApplyEdgeStackToExpandableCard(
                currentState = false,
                targetState = true,
            ),
        )
        assertFalse(
            shouldApplyEdgeStackToExpandableCard(
                currentState = true,
                targetState = true,
            ),
        )
    }

    @Test
    fun `collapsing cards rejoin only after their body finishes leaving`() {
        assertFalse(
            shouldApplyEdgeStackToExpandableCard(
                currentState = true,
                targetState = false,
            ),
        )
    }
}
