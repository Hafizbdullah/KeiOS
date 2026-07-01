@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student.section.gallery

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.student.GuideMediaProgressState
import os.kei.ui.page.main.student.GuideRemoteImageAdaptive
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.icon.extended.Refresh
import kotlin.math.roundToInt

@Composable
internal fun GuideInlineVideoPreview(
    previewImageUrl: String,
    onOpenFullscreen: () -> Unit,
    previewProgressState: GuideMediaProgressState?,
    onPreviewLoadingChanged: ((Boolean) -> Unit)?
) {
    if (previewImageUrl.isNotBlank()) {
        Box(
            modifier = Modifier.clickable { onOpenFullscreen() }
        ) {
            GuideRemoteImageAdaptive(
                imageUrl = previewImageUrl,
                progressState = previewProgressState,
                onLoadingChanged = onPreviewLoadingChanged
            )
        }
    } else {
        LaunchedEffect(previewProgressState, onPreviewLoadingChanged) {
            previewProgressState?.set(1f)
            onPreviewLoadingChanged?.invoke(false)
        }
    }
}

@Composable
internal fun GuideInlineVideoPlayerBody(
    player: Player,
    videoRatio: Float,
    loopEnabled: Boolean,
    onToggleLoop: () -> Unit,
    onCollapse: () -> Unit,
    onVideoBoundsChanged: (Rect?) -> Unit = {},
    backdrop: Backdrop?
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(videoRatio)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val rect =
                    Rect(
                        bounds.left.roundToInt(),
                        bounds.top.roundToInt(),
                        bounds.right.roundToInt(),
                        bounds.bottom.roundToInt(),
                    )
                onVideoBoundsChanged(rect.takeUnless { it.isEmpty })
            }
            // NOTE: must stay a plain clip, NOT appSquircleClip. PlayerView renders through a
            // SurfaceView on its own system-composited layer, which draws nothing into Compose's
            // offscreen buffer. The squircle helper forces CompositingStrategy.Offscreen + a DstIn
            // mask, so that empty buffer masks the video to a white screen (audio still plays).
            // A regular clip avoids offscreen compositing, matching the working 1.8.0 behavior.
            .clip(RoundedCornerShape(14.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLiquidTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.Replace,
            textColor = if (loopEnabled) Color(0xFF34C759) else Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = onToggleLoop
        )
        AppLiquidTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.ExpandLess,
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = onCollapse
        )
    }
}

@Composable
internal fun GuideInlineVideoFailureBody(
    videoRatio: Float,
    loadError: String,
    mediaUrl: String,
    backdrop: Backdrop?,
    onRetry: () -> Unit,
    onCollapse: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoRatio.coerceIn(0.8f, 2.4f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF20B1020))
                    .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            GuideVideoFailureMessage(
                loadError = loadError,
                mediaUrl = mediaUrl,
                backdrop = backdrop,
                errorColor = Color(0xFFFCA5A5),
                supportingColor = Color(0xFFBFDBFE),
                buttonTextColor = Color(0xFFBFDBFE),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLiquidTextButton(
                backdrop = backdrop,
                text = "",
                leadingIcon = MiuixIcons.Regular.Refresh,
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.Compact,
                onClick = onRetry,
            )
            AppLiquidTextButton(
                backdrop = backdrop,
                text = "",
                leadingIcon = MiuixIcons.Regular.ExpandLess,
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.Compact,
                onClick = onCollapse,
            )
        }
    }
}
