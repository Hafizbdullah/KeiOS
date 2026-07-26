@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import os.kei.R
import os.kei.core.ui.effect.background.blend.ColorBlendToken
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * Debug catalog proof for the miuix texture-blur channel.
 *
 * Each stage owns a self-contained miuix [layerBackdrop] pattern layer, so the frosted
 * overlays only ever sample miuix-captured content and never mix with the kyant glass
 * that renders the surrounding catalog cards.
 */
@Composable
internal fun DebugMiuixTextureBlurCard(accent: Color) {
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_miuix_texture_blur_title),
        subtitle = stringResource(R.string.debug_component_lab_miuix_texture_blur_body),
        sectionIcon = appLucideFlaskIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DebugMiuixFrostedBlurStage(accent = accent)
            DebugMiuixProgressiveBlurStage(accent = accent)
        }
    }
}

@Composable
private fun DebugMiuixFrostedBlurStage(accent: Color) {
    val isDark = isAppInDarkTheme()
    val miuixBackdrop = rememberLayerBackdrop()
    val frostedBlend =
        remember(isDark) {
            if (isDark) ColorBlendToken.Colored_Regular_Dark else ColorBlendToken.Colored_Regular_Light
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedRectangle(20.dp)),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .layerBackdrop(miuixBackdrop),
        ) {
            DebugMiuixBlurPatternBackground(accent = accent, mirrored = false)
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f)
                    .height(88.dp)
                    .textureBlur(
                        backdrop = miuixBackdrop,
                        shape = RoundedRectangle(18.dp),
                        blurRadius = 40f,
                        colors = BlurColors(blendColors = frostedBlend),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_miuix_texture_blur_frosted_label),
                color = Color.White,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DebugMiuixProgressiveBlurStage(accent: Color) {
    val miuixBackdrop = rememberLayerBackdrop()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedRectangle(20.dp)),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .layerBackdrop(miuixBackdrop),
        ) {
            DebugMiuixBlurPatternBackground(accent = accent, mirrored = true)
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .progressiveTextureBlur(
                        backdrop = miuixBackdrop,
                        shape = RectangleShape,
                        blurRadius = 36f,
                        gradient = ProgressiveBlur.Bottom,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color(0x66000000), RoundedRectangle(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_miuix_progressive_blur_label),
                color = Color.White,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
            )
        }
    }
}

/**
 * High-contrast deterministic pattern; frosted overlays should keep its silhouettes
 * readable, which separates real sampling blur from a plain translucent scrim.
 */
@Composable
private fun DebugMiuixBlurPatternBackground(
    accent: Color,
    mirrored: Boolean,
) {
    val hueA = if (mirrored) Color(0xFF38BDF8) else Color(0xFFF472B6)
    val hueB = if (mirrored) Color(0xFFA78BFA) else Color(0xFFFACC15)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.90f), hueA, hueB),
                    ),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .offset(x = 24.dp, y = 18.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .offset(x = (-30).dp, y = 52.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .offset(x = (-16).dp, y = (-14).dp)
                    .background(hueB.copy(alpha = 0.95f), CircleShape),
        )
    }
}
