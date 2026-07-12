package os.kei.ui.page.main.student.catalog.component.bgm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon

@Composable
internal fun BaGuideBgmInlineIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    size: Dp = 36.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = BaGuideBgmInlineIconMinimumTouchSize,
                    minHeight = BaGuideBgmInlineIconMinimumTouchSize,
                ).then(modifier)
                .size(size)
                .semantics { this.contentDescription = contentDescription }
                .then(
                    if (interactionSource != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier.clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

private val BaGuideBgmInlineIconMinimumTouchSize = 48.dp
