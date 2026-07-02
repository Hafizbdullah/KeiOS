package os.kei.ui.pip

fun resolveAppPictureInPictureSeekPositionMs(
    currentPositionMs: Long,
    durationMs: Long,
    deltaMs: Long,
): Long {
    val targetPosition = currentPositionMs.coerceAtLeast(0L) + deltaMs
    if (durationMs <= 0L) {
        return targetPosition.coerceAtLeast(0L)
    }
    return targetPosition.coerceIn(0L, durationMs)
}
