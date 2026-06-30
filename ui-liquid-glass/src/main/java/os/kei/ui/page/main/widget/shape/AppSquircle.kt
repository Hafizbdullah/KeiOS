// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("FunctionName", "PropertyName")

package os.kei.ui.page.main.widget.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.squircle.addSquircleRect as addMiuixSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface

val LocalAppSquircleEnabled = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun isAppSquircleEnabled(): Boolean = LocalAppSquircleEnabled.current && isSquircleEnabled()

@Composable
fun Modifier.appSquircleBackground(
    color: Color,
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier = appSquircleBackground(color, cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension, control)

@Composable
fun Modifier.appSquircleBackground(
    color: Color,
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier {
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    if (!LocalAppSquircleEnabled.current) return background(color = color, shape = shape)
    return squircleBackground(
        color = color,
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        extension = extension,
    )
}

@Composable
fun Modifier.appSquircleClip(
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier = appSquircleClip(cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension, control)

@Composable
fun Modifier.appSquircleClip(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier {
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    if (!LocalAppSquircleEnabled.current) return clip(shape)
    return squircleClip(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        extension = extension,
    )
}

@Composable
fun Modifier.appSquircleSurface(
    color: Color,
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier = appSquircleSurface(color, cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension, control)

@Composable
fun Modifier.appSquircleSurface(
    color: Color,
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier {
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    if (!LocalAppSquircleEnabled.current) return clip(shape).background(color)
    return squircleSurface(
        color = color,
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        extension = extension,
    )
}

fun Modifier.appSquircleBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
): Modifier =
    drawAppSquircleBorder(
        width = width,
        cornerRadius = cornerRadius,
        extension = extension,
        control = control,
    ) { color }

fun Modifier.drawAppSquircleBorder(
    width: Dp,
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
    color: () -> Color,
): Modifier =
    drawAppSquircleBorder(
        width = { width },
        cornerRadius = cornerRadius,
        extension = extension,
        control = control,
        color = color,
    )

fun Modifier.drawAppSquircleBorder(
    width: () -> Dp,
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
    color: () -> Color,
): Modifier =
    drawWithCache {
        val cornerRadiusPx = cornerRadius.toPx()
        val path = Path()
        onDrawBehind {
            val widthPx = width().toPx()
            if (widthPx <= 0f) return@onDrawBehind
            val innerWidth = size.width - widthPx
            val innerHeight = size.height - widthPx
            if (innerWidth <= 0f || innerHeight <= 0f) return@onDrawBehind
            val halfStroke = widthPx / 2f
            path.rewind()
            path.addAppSquircleRect(
                width = innerWidth,
                height = innerHeight,
                cornerRadius = (cornerRadiusPx - halfStroke).coerceAtLeast(0f),
                extension = extension,
                control = control,
            )
            val drawColor = color()
            if (drawColor.alpha > 0f) {
                translate(halfStroke, halfStroke) {
                    drawPath(path = path, color = drawColor, style = Stroke(width = widthPx))
                }
            }
        }
    }

@Composable
fun Modifier.drawAppSquircleBackground(
    cornerRadius: Dp,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
    color: () -> Color,
): Modifier {
    val squircleEnabled = isAppSquircleEnabled()
    return drawWithCache {
        val cornerRadiusPx = cornerRadius.toPx()
        val path = Path()
        path.addAppSquircleRect(
            width = size.width,
            height = size.height,
            cornerRadius = cornerRadiusPx,
            extension = extension,
            control = control,
            squircleEnabled = squircleEnabled,
        )
        onDrawBehind {
            val drawColor = color()
            if (drawColor.alpha > 0f) {
                drawPath(path = path, color = drawColor)
            }
        }
    }
}

object AppSquircleDefaults {
    const val Extension = 1.1f
    const val Control = 0.643f
    const val ExtensionMin = 1f
    const val ExtensionMax = 2f
    const val ControlMin = 0.3f
    const val ControlMax = 0.9f
}

fun Path.addAppSquircleRect(
    width: Float,
    height: Float,
    cornerRadius: Float,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
) {
    addAppSquircleRect(
        width = width,
        height = height,
        cornerRadius = cornerRadius,
        extension = extension,
        control = control,
        squircleEnabled = true,
    )
}

fun Path.addAppSquircleRect(
    width: Float,
    height: Float,
    cornerRadius: Float,
    extension: Float = AppSquircleDefaults.Extension,
    control: Float = AppSquircleDefaults.Control,
    squircleEnabled: Boolean,
) {
    addMiuixSquircleRect(
        width = width,
        height = height,
        cornerRadius = cornerRadius,
        extension = extension,
        squircleEnabled = squircleEnabled,
    )
}
