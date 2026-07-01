package os.kei.ui.page.main.student.section.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.intent.SafeExternalIntents
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DecoderCapabilityErrorCodes =
    setOf(
        "ERROR_CODE_DECODER_INIT_FAILED",
        "ERROR_CODE_DECODING_FAILED",
        "ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES",
        "ERROR_CODE_DECODING_FORMAT_UNSUPPORTED",
    )

@Composable
internal fun GuideInlineVideoUnavailableHint() {
    Text(
        text = stringResource(R.string.guide_gallery_video_unavailable),
        color = MiuixTheme.colorScheme.onBackgroundVariant
    )
}

@Composable
internal fun GuideInlineVideoStatusHints(
    isBuffering: Boolean,
    loadError: String?,
    mediaUrl: String = "",
    backdrop: Backdrop? = null,
) {
    if (isBuffering) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidCircularProgressBar(
                progress = { 0.35f },
                size = 14.dp,
                strokeWidth = 2.dp,
                activeColor = Color(0xFF60A5FA),
                inactiveColor = Color(0x3360A5FA)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.guide_gallery_video_loading),
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        }
    }

    loadError?.takeIf { it.isNotBlank() }?.let { err ->
        GuideVideoFailureMessage(
            loadError = err,
            mediaUrl = mediaUrl,
            backdrop = backdrop,
        )
    }
}

@Composable
internal fun GuideVideoFailureMessage(
    loadError: String,
    mediaUrl: String = "",
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    errorColor: Color? = null,
    supportingColor: Color? = null,
    buttonTextColor: Color = Color(0xFF3B82F6),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val context = LocalContext.current
    val openFailedText = stringResource(R.string.common_open_link_failed)
    val decoderCapabilityError = isGuideVideoDecoderCapabilityError(loadError)
    val resolvedErrorColor = errorColor ?: MiuixTheme.colorScheme.error
    val resolvedSupportingColor = supportingColor ?: MiuixTheme.colorScheme.onBackgroundVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text =
                if (decoderCapabilityError) {
                    stringResource(R.string.guide_gallery_video_failed_decoder_capability)
                } else {
                    stringResource(R.string.guide_gallery_video_failed_with_reason, loadError)
                },
            color = resolvedErrorColor,
            maxLines = if (decoderCapabilityError) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.guide_gallery_video_error_code, loadError),
            color = resolvedSupportingColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (mediaUrl.isNotBlank()) {
            AppLiquidTextButton(
                backdrop = backdrop,
                text = stringResource(R.string.guide_gallery_video_open_external),
                leadingIcon = appLucideExternalLinkIcon(),
                textColor = buttonTextColor,
                variant = GlassVariant.Compact,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
                textSoftWrap = false,
                onClick = {
                    if (!SafeExternalIntents.startBrowsableUrl(context, mediaUrl, newTask = true)) {
                        context.showToast(openFailedText)
                    }
                },
            )
        }
    }
}

internal fun isGuideVideoDecoderCapabilityError(errorCode: String): Boolean =
    errorCode in DecoderCapabilityErrorCodes
