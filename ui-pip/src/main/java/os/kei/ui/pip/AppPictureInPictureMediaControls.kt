package os.kei.ui.pip

const val APP_PIP_SEEK_INTERVAL_10_SECONDS_MS: Long = 10_000L

data class AppPictureInPictureMediaControlActions(
    val playbackAction: AppPictureInPictureRemoteActionSpec,
    val seekBackAction: AppPictureInPictureRemoteActionSpec? = null,
    val seekForwardAction: AppPictureInPictureRemoteActionSpec? = null,
    val secondaryAction: AppPictureInPictureRemoteActionSpec? = null,
)

fun AppPictureInPictureMediaControlActions.resolveVisibleActions(
    maxActions: Int?,
): List<AppPictureInPictureRemoteActionSpec> {
    val normalizedMaxActions = maxActions?.coerceAtLeast(0)
    if (normalizedMaxActions == 0) return emptyList()
    val seekPair = listOfNotNull(seekBackAction, seekForwardAction)
        .takeIf { actions -> actions.size == 2 }
    return when {
        normalizedMaxActions == null -> buildBalancedMediaControlActions(includeSeekPair = true)
        normalizedMaxActions >= 4 -> buildBalancedMediaControlActions(includeSeekPair = true)
        normalizedMaxActions == 3 && seekPair != null ->
            listOf(seekPair.first(), playbackAction, seekPair.last())
        normalizedMaxActions >= 2 && secondaryAction != null ->
            listOf(playbackAction, secondaryAction)
        else -> listOf(playbackAction)
    }
}

private fun AppPictureInPictureMediaControlActions.buildBalancedMediaControlActions(
    includeSeekPair: Boolean,
): List<AppPictureInPictureRemoteActionSpec> {
    return buildList {
        if (includeSeekPair && seekBackAction != null) {
            add(seekBackAction)
        }
        add(playbackAction)
        if (includeSeekPair && seekForwardAction != null) {
            add(seekForwardAction)
        }
        if (secondaryAction != null) {
            add(secondaryAction)
        }
    }
}
