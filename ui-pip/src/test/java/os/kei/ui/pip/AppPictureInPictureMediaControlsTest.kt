package os.kei.ui.pip

import org.junit.Test
import kotlin.test.assertEquals

class AppPictureInPictureMediaControlsTest {
    private val playback = action("playback")
    private val seekBack = action("seek_back")
    private val seekForward = action("seek_forward")
    private val secondary = action("secondary")

    @Test
    fun `media controls keep balanced four action layout when unconstrained`() {
        val controls =
            AppPictureInPictureMediaControlActions(
                playbackAction = playback,
                seekBackAction = seekBack,
                seekForwardAction = seekForward,
                secondaryAction = secondary,
            )

        assertEquals(
            listOf("seek_back", "playback", "seek_forward", "secondary"),
            controls.resolveVisibleActions(maxActions = null).map { it.action },
        )
    }

    @Test
    fun `media controls keep seek pair around playback when three actions are available`() {
        val controls =
            AppPictureInPictureMediaControlActions(
                playbackAction = playback,
                seekBackAction = seekBack,
                seekForwardAction = seekForward,
                secondaryAction = secondary,
            )

        assertEquals(
            listOf("seek_back", "playback", "seek_forward"),
            controls.resolveVisibleActions(maxActions = 3).map { it.action },
        )
    }

    @Test
    fun `media controls keep playback and secondary action when two actions are available`() {
        val controls =
            AppPictureInPictureMediaControlActions(
                playbackAction = playback,
                seekBackAction = seekBack,
                seekForwardAction = seekForward,
                secondaryAction = secondary,
            )

        assertEquals(
            listOf("playback", "secondary"),
            controls.resolveVisibleActions(maxActions = 2).map { it.action },
        )
    }

    @Test
    fun `media controls keep playback only when one action is available`() {
        val controls =
            AppPictureInPictureMediaControlActions(
                playbackAction = playback,
                seekBackAction = seekBack,
                seekForwardAction = seekForward,
                secondaryAction = secondary,
            )

        assertEquals(
            listOf("playback"),
            controls.resolveVisibleActions(maxActions = 1).map { it.action },
        )
    }

    @Test
    fun `media controls treat negative action limit as zero`() {
        val controls =
            AppPictureInPictureMediaControlActions(
                playbackAction = playback,
                seekBackAction = seekBack,
                seekForwardAction = seekForward,
                secondaryAction = secondary,
            )

        assertEquals(emptyList(), controls.resolveVisibleActions(maxActions = -1))
    }

    private fun action(action: String): AppPictureInPictureRemoteActionSpec {
        return AppPictureInPictureRemoteActionSpec(
            action = action,
            iconRes = 0,
            title = action,
            requestCode = action.hashCode(),
        )
    }
}
