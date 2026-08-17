@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.shape.drawAppSquircleBackground
import os.kei.ui.page.main.widget.shape.drawAppSquircleBorder
import os.kei.ui.page.main.widget.shape.drawAppSquircleForeground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class LiquidGlassDropdownItemType {
    Action,
    SingleChoice,
    MultipleChoice,
}

@Composable
fun LiquidGlassDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    contentTint: Color? = null,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    highlighted: Boolean = selected,
    showCheck: Boolean = selected,
    highlightContent: Boolean = selected && showCheck,
    reserveCheckSlot: Boolean = false,
    textMaxLines: Int = 1,
    itemType: LiquidGlassDropdownItemType = LiquidGlassDropdownItemType.Action,
) {
    if (LocalLiquidGlassDropdownSizingPass.current) {
        LiquidGlassDropdownMeasureItem(
            text = text,
            selected = selected,
            modifier = modifier,
            index = index,
            optionSize = optionSize,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            subtitle = subtitle,
            trailingContent = trailingContent,
            accentColor = accentColor,
            variant = variant,
            highlighted = highlighted,
            showCheck = showCheck,
            highlightContent = highlightContent,
            reserveCheckSlot = reserveCheckSlot,
            textMaxLines = textMaxLines,
            enabled = enabled,
        )
        return
    }

    val isDark = isAppInDarkTheme()
    val material = LocalLiquidGlassDropdownMaterial.current
    val colors =
        liquidGlassDropdownItemColors(
            isDark = isDark,
            accentColor = accentColor,
            variant = variant,
            contentTint = contentTint,
            highlighted = highlighted,
            highlightContent = highlightContent,
            enabled = enabled,
        )
    // Smooth color transitions when selection changes — prevents the abrupt "blink" that a plain
    // ternary produces. Spring keeps it feeling natural and tied to the user's selection gesture.
    val textColorState =
        animateColorAsState(
            targetValue = colors.text,
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
            label = "liquid_glass_dropdown_item_text_color",
        )
    val iconColorState =
        animateColorAsState(
            targetValue = colors.icon,
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
            label = "liquid_glass_dropdown_item_icon_color",
        )
    val textColorProvider = remember(textColorState) { ColorProducer { textColorState.value } }
    val iconColorProvider = remember(iconColorState) { ColorProducer { iconColorState.value } }
    val currentOnClick by rememberUpdatedState(onClick)
    val itemRole =
        when (itemType) {
            LiquidGlassDropdownItemType.Action -> Role.Button
            LiquidGlassDropdownItemType.SingleChoice -> Role.RadioButton
            LiquidGlassDropdownItemType.MultipleChoice -> Role.Checkbox
        }
    val selectionSemantics =
        when (itemType) {
            LiquidGlassDropdownItemType.Action -> {
                Modifier
            }

            LiquidGlassDropdownItemType.SingleChoice -> {
                Modifier.semantics { this.selected = selected }
            }

            LiquidGlassDropdownItemType.MultipleChoice -> {
                Modifier.semantics { toggleableState = ToggleableState(selected) }
            }
        }
    val rowShape = RoundedRectangle(LiquidGlassDropdownItemRadius)
    val outerTopPadding =
        if (index == 0) {
            LiquidGlassDropdownItemPressSafePadding
        } else {
            2.dp
        }
    val outerBottomPadding =
        if (index == optionSize - 1) {
            LiquidGlassDropdownItemPressSafePadding
        } else {
            2.dp
        }

    // A popup owns one adaptive glass layer. Rows use tonal fills so selection and press
    // feedback stay legible without sampling the popup backdrop a second time.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Spring-based press feedback feels alive — settles quickly without overshoot,
    // matches the elastic feel of iOS/Liquid Glass interactions.
    val scaleState =
        animateFloatAsState(
            targetValue = if (pressed && enabled) AppInteractiveTokens.pressedScale else 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 700f),
            label = "liquid_glass_dropdown_item_scale",
        )
    // One press progress drives everything the press moves: the lift, the tonal surface, the border and
    // the darkening overlay.
    //
    // There used to be two springs on the same boolean — a "pill alpha" (0.92/600) for the surface,
    // border and shadow, and a separate "pressed alpha" (0.88/900) for a full-size overlay `Box` laid on
    // top. One gesture, two timelines a few milliseconds apart, and an extra layout node per row in a
    // list that renders one row per menu entry. Selection is not involved either way: it is carried by
    // the accent content and the check, exactly as the platform menu language does it, which is why the
    // old `showSelectionPill` was only ever `pressed`.
    val pressProgressState =
        animateFloatAsState(
            targetValue = if (pressed && enabled) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 600f),
            label = "liquid_glass_dropdown_item_press_progress",
        )
    val pillSurface =
        liquidGlassDropdownPressedSurfaceColor(
            isDark = isDark,
            material = material,
        )
    val pressBorderColor =
        liquidGlassDropdownSelectedBorderColor(
            isDark = isDark,
            material = material,
            accentColor = colors.accent,
        )
    // The overlay used to get its alpha from `appControlPressedOverlayAlpha(pressed, isDark)`, which is a
    // boolean gate in front of these two tokens. With one progress driving the fade, the peak is read
    // directly and scaled — passing a hardcoded `true` to a gate would only obscure that.
    val pressOverlayColor = MiuixTheme.colorScheme.onBackground
    val pressOverlayPeakAlpha =
        if (isDark) {
            AppInteractiveTokens.pressedOverlayAlphaDark
        } else {
            AppInteractiveTokens.pressedOverlayAlphaLight
        }
    val pressShadow = liquidGlassDropdownPressShadow(material = material, isDark = isDark)
    Box(
        modifier =
            modifier
                .padding(top = outerTopPadding, bottom = outerBottomPadding)
                .graphicsLayer {
                    val scale = scaleState.value
                    val progress = pressProgressState.value
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = pressShadow.elevation.toPx() * progress
                    shape = rowShape
                    clip = false
                    ambientShadowColor = Color.Black.copy(alpha = pressShadow.ambientAlpha * progress)
                    spotShadowColor = Color.Black.copy(alpha = pressShadow.spotAlpha * progress)
                }.drawAppSquircleBackground(LiquidGlassDropdownItemRadius) {
                    pillSurface.copy(alpha = pillSurface.alpha * pressProgressState.value)
                }.drawAppSquircleBorder(
                    width = 1.dp,
                    cornerRadius = LiquidGlassDropdownItemRadius,
                ) {
                    pressBorderColor.copy(alpha = pressBorderColor.alpha * pressProgressState.value)
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = itemRole,
                    onClick = { currentOnClick() },
                ).then(selectionSemantics)
                .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight),
    ) {
        LiquidGlassDropdownRowContent(
            text = text,
            textColor = textColorProvider,
            iconColor = iconColorProvider,
            checkColor = colors.check,
            showCheck = showCheck,
            reserveCheckSlot = reserveCheckSlot,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            subtitle = subtitle,
            trailingContent = trailingContent,
            // The darkening overlay is now a draw pass on the content rather than a sibling `Box` with
            // `matchParentSize`, which is one fewer node and one fewer measure per row.
            modifier =
                Modifier
                    .liquidGlassDropdownRowContent()
                    .drawAppSquircleForeground(LiquidGlassDropdownItemRadius) {
                        pressOverlayColor.copy(alpha = pressOverlayPeakAlpha * pressProgressState.value)
                    },
            textMaxLines = textMaxLines,
            enabled = enabled,
            isDark = isDark,
        )
    }
}

/**
 * The row's content colors, derived once for both the real row and the sizing pass.
 *
 * The two used to compute this separately — the same accent lookup, the same
 * highlighted/onBackground/onBackgroundVariant ladder, the same disabled alphas — and had already
 * drifted apart in the content-tint branch, which only the real row honoured.
 */
@Composable
private fun liquidGlassDropdownItemColors(
    isDark: Boolean,
    accentColor: Color,
    variant: GlassVariant,
    contentTint: Color?,
    highlighted: Boolean,
    highlightContent: Boolean,
    enabled: Boolean,
): LiquidGlassDropdownItemColors {
    val itemAccent =
        liquidGlassDropdownItemAccent(
            isDark = isDark,
            accentColor = accentColor,
            variant = variant,
        )
    val contentHighlighted = highlighted && highlightContent
    val resolvedContentTint =
        contentTint ?: itemAccent.takeIf { variant == GlassVariant.SheetDangerAction }
    val text =
        (
            resolvedContentTint ?: if (contentHighlighted) {
                itemAccent
            } else {
                MiuixTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.98f else 0.96f)
            }
        ).let { color -> if (enabled) color else color.copy(alpha = 0.42f) }
    val icon =
        (
            resolvedContentTint ?: if (contentHighlighted) {
                itemAccent
            } else {
                MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.94f else 0.86f)
            }
        ).let { color -> if (enabled) color else color.copy(alpha = 0.38f) }
    return LiquidGlassDropdownItemColors(
        accent = itemAccent,
        text = text,
        icon = icon,
        check = if (enabled) itemAccent else itemAccent.copy(alpha = 0.42f),
    )
}

private class LiquidGlassDropdownItemColors(
    val accent: Color,
    val text: Color,
    val icon: Color,
    val check: Color,
)

@Composable
private fun LiquidGlassDropdownMeasureItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    highlighted: Boolean = selected,
    showCheck: Boolean = selected,
    highlightContent: Boolean = selected && showCheck,
    reserveCheckSlot: Boolean = false,
    textMaxLines: Int = 1,
    enabled: Boolean = true,
) {
    val isDark = isAppInDarkTheme()
    val colors =
        liquidGlassDropdownItemColors(
            isDark = isDark,
            accentColor = accentColor,
            variant = variant,
            // The sizing pass measures text, and a tint never changes a glyph's advance — so unlike the
            // real row it has nothing to pass here. Sharing the derivation is still what keeps the two
            // in step on everything that *does* affect measurement.
            contentTint = null,
            highlighted = highlighted,
            highlightContent = highlightContent,
            enabled = enabled,
        )
    val outerTopPadding = if (index == 0) 0.dp else 2.dp
    val outerBottomPadding = if (index == optionSize - 1) 0.dp else 2.dp

    Box(
        modifier =
            modifier
                .padding(top = outerTopPadding, bottom = outerBottomPadding)
                .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight),
    ) {
        LiquidGlassDropdownRowContent(
            text = text,
            textColor = ColorProducer { colors.text },
            iconColor = ColorProducer { colors.icon },
            checkColor = colors.check,
            showCheck = showCheck,
            reserveCheckSlot = reserveCheckSlot,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            subtitle = subtitle,
            trailingContent = trailingContent,
            modifier = Modifier.liquidGlassDropdownRowContent(),
            textMaxLines = textMaxLines,
            enabled = enabled,
            isDark = isDark,
        )
    }
}

@Composable
private fun Modifier.liquidGlassDropdownRowContent(): Modifier {
    val minHeight =
        when (LocalLiquidGlassDropdownMaterial.current) {
            LiquidGlassDropdownMaterial.ActionMenu -> 42.dp
            LiquidGlassDropdownMaterial.Default -> LiquidGlassDropdownRowMinHeight
        }
    return fillMaxWidth()
        .defaultMinSize(minHeight = minHeight)
}

@Composable
private fun LiquidGlassDropdownRowContent(
    text: String,
    textColor: ColorProducer,
    iconColor: ColorProducer,
    checkColor: Color,
    showCheck: Boolean,
    reserveCheckSlot: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    subtitle: String?,
    trailingContent: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    textMaxLines: Int = 1,
    enabled: Boolean = true,
    isDark: Boolean,
) {
    val material = LocalLiquidGlassDropdownMaterial.current
    val textTypography =
        when {
            material == LiquidGlassDropdownMaterial.ActionMenu -> AppTypographyTokens.Supporting
            textMaxLines == 1 && subtitle == null -> AppTypographyTokens.Body
            else -> AppTypographyTokens.Supporting
        }
    val subtitleTypography =
        when (material) {
            LiquidGlassDropdownMaterial.ActionMenu -> AppTypographyTokens.Eyebrow
            LiquidGlassDropdownMaterial.Default -> AppTypographyTokens.Caption
        }
    val subtitleColor =
        MiuixTheme.colorScheme.onBackgroundVariant
            .copy(
                alpha =
                    if (enabled) {
                        if (material == LiquidGlassDropdownMaterial.ActionMenu) 0.78f else 0.76f
                    } else {
                        0.34f
                    },
            )
    val contentShadow =
        remember(isDark, enabled) {
            liquidGlassDropdownContentShadow(
                isDark = isDark,
                enabled = enabled,
            )
        }
    val actionMenuLeadingCheck =
        material == LiquidGlassDropdownMaterial.ActionMenu &&
            reserveCheckSlot
    // Was a three-branch `when` whose last two branches both returned 23.dp, so `actionMenuLeadingCheck`
    // discriminated nothing.
    val rowHorizontalPadding = if (material == LiquidGlassDropdownMaterial.ActionMenu) 23.dp else 12.dp
    val rowVerticalPadding = if (material == LiquidGlassDropdownMaterial.ActionMenu) 7.dp else 8.dp
    val rowSpacing = if (material == LiquidGlassDropdownMaterial.ActionMenu) 13.dp else 10.dp
    Row(
        modifier =
            modifier.padding(
                horizontal = rowHorizontalPadding,
                vertical = rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        if (actionMenuLeadingCheck) {
            LiquidGlassDropdownCheck(
                showCheck = showCheck,
                checkColor = checkColor,
                reserveSpace = true,
            )
        } else if (leadingIcon != null) {
            LiquidGlassDropdownIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(LiquidGlassDropdownIconSize),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            BasicText(
                text = text,
                color = textColor,
                style =
                    TextStyle(
                        fontSize = textTypography.fontSize,
                        lineHeight = textTypography.lineHeight,
                        fontWeight = FontWeight.Medium,
                        shadow = contentShadow,
                    ),
                maxLines = textMaxLines,
                overflow = if (textMaxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
            if (!subtitle.isNullOrBlank()) {
                BasicText(
                    text = subtitle,
                    style =
                        TextStyle(
                            color = subtitleColor,
                            fontSize = subtitleTypography.fontSize,
                            lineHeight = subtitleTypography.lineHeight,
                            fontWeight = FontWeight.Normal,
                            shadow = contentShadow,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingIcon != null) {
            LiquidGlassDropdownIcon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(LiquidGlassDropdownIconSize),
            )
        }
        trailingContent?.invoke(this)
        if (!actionMenuLeadingCheck) {
            LiquidGlassDropdownCheck(
                showCheck = showCheck,
                checkColor = checkColor,
                reserveSpace = false,
            )
        }
    }
}

/**
 * The selection check, in both of the shapes a dropdown row needs.
 *
 * There were two implementations of this — a `Box` plus two `animateFloatAsState` springs for the action
 * menu's leading slot, and an `AnimatedVisibility` with fade+scale for everyone else's trailing slot —
 * for one icon at one size with the same enter springs. The only real difference is whether the slot
 * holds its space, which is what [reserveSpace] now says out loud.
 *
 * Folding them also fixed a drift: the leading copy had no exit spec, so hiding it ran the *enter*
 * springs backwards (0.85/700, 0.7/600) while the trailing copy left on a distinctly snappier 0.95/900.
 * One check, one pair of specs, both directions.
 */
@Composable
private fun LiquidGlassDropdownCheck(
    showCheck: Boolean,
    checkColor: Color,
    reserveSpace: Boolean,
) {
    if (reserveSpace) {
        // Reserving the slot means the icon may not change the layout, so it fades and scales in place.
        val alphaState =
            animateFloatAsState(
                targetValue = if (showCheck) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
                label = "liquid_glass_dropdown_check_alpha",
            )
        val scaleState =
            animateFloatAsState(
                targetValue = if (showCheck) 1f else 0.6f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
                label = "liquid_glass_dropdown_check_scale",
            )
        Box(
            modifier = Modifier.size(LiquidGlassDropdownCheckSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                tint = checkColor,
                modifier =
                    Modifier
                        .size(LiquidGlassDropdownCheckSize)
                        .graphicsLayer {
                            alpha = alphaState.value
                            scaleX = scaleState.value
                            scaleY = scaleState.value
                        },
            )
        }
        return
    }

    // Not reserving means the row re-lays out around the icon, so the slot itself comes and goes.
    AnimatedVisibility(
        visible = showCheck,
        enter =
            fadeIn(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
            ) +
                scaleIn(
                    animationSpec =
                        spring(
                            dampingRatio = 0.7f,
                            stiffness = 600f,
                        ),
                    initialScale = 0.6f,
                ),
        exit =
            fadeOut(
                animationSpec =
                    spring(
                        dampingRatio = 0.95f,
                        stiffness = 900f,
                    ),
            ) +
                scaleOut(
                    animationSpec =
                        spring(
                            dampingRatio = 0.95f,
                            stiffness = 900f,
                        ),
                    targetScale = 0.6f,
                ),
    ) {
        Icon(
            imageVector = MiuixIcons.Basic.Check,
            contentDescription = null,
            tint = checkColor,
            modifier = Modifier.size(LiquidGlassDropdownCheckSize),
        )
    }
}

internal fun liquidGlassDropdownContentShadow(
    isDark: Boolean,
    enabled: Boolean,
): Shadow =
    Shadow(
        color =
            if (isDark) {
                Color.Black.copy(alpha = if (enabled) 0.44f else 0.24f)
            } else {
                Color.White.copy(alpha = if (enabled) 0.40f else 0.22f)
            },
        offset = Offset(0f, 1f),
        blurRadius = 2f,
    )

@Composable
private fun LiquidGlassDropdownIcon(
    imageVector: ImageVector,
    tint: ColorProducer,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = rememberVectorPainter(imageVector),
        tint = tint,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

/**
 * A passive dropdown row for context and supporting information.
 *
 * The row deliberately owns no click, selection, enabled, or role semantics. Its icon is
 * decorative and its text descendants are merged so accessibility services read the title and
 * subtitle as ordinary information.
 */
@Composable
fun LiquidGlassDropdownInfoItem(
    text: String,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    textMaxLines: Int = 1,
) {
    val isDark = isAppInDarkTheme()
    val infoAccent =
        liquidGlassDropdownItemAccent(
            isDark = isDark,
            accentColor = accentColor,
            variant = GlassVariant.SheetAction,
        )
    val textColor =
        MiuixTheme.colorScheme.onBackground.copy(
            alpha = if (isDark) 0.98f else 0.94f,
        )
    val iconColor = infoAccent.copy(alpha = if (isDark) 0.94f else 0.86f)
    val outerTopPadding =
        if (index == 0) LiquidGlassDropdownItemPressSafePadding else 2.dp
    val outerBottomPadding =
        if (index == optionSize - 1) LiquidGlassDropdownItemPressSafePadding else 2.dp
    val accessibilityModifier =
        if (LocalLiquidGlassDropdownSizingPass.current) {
            Modifier.clearAndSetSemantics {}
        } else {
            Modifier.semantics(mergeDescendants = true) {}
        }

    Box(
        modifier =
            modifier
                .padding(top = outerTopPadding, bottom = outerBottomPadding)
                .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight)
                .then(accessibilityModifier),
    ) {
        LiquidGlassDropdownRowContent(
            text = text,
            textColor = ColorProducer { textColor },
            iconColor = ColorProducer { iconColor },
            checkColor = Color.Transparent,
            showCheck = false,
            reserveCheckSlot = false,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            subtitle = subtitle,
            trailingContent = null,
            modifier = Modifier.liquidGlassDropdownRowContent(),
            textMaxLines = textMaxLines,
            isDark = isDark,
        )
    }
}

@Composable
fun LiquidGlassDropdownSingleChoiceItem(
    text: String,
    optionSize: Int,
    isSelected: Boolean,
    index: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    textMaxLines: Int = 1,
) {
    LiquidGlassDropdownItem(
        text = text,
        selected = isSelected,
        onClick = { onSelectedIndexChange(index) },
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        subtitle = subtitle,
        trailingContent = trailingContent,
        accentColor = accentColor,
        variant = variant,
        enabled = enabled,
        highlighted = isSelected,
        showCheck = isSelected,
        highlightContent = isSelected,
        reserveCheckSlot = true,
        textMaxLines = textMaxLines,
        itemType = LiquidGlassDropdownItemType.SingleChoice,
    )
}

@Composable
fun LiquidGlassDropdownMultipleChoiceItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    textMaxLines: Int = 1,
) {
    LiquidGlassDropdownItem(
        text = text,
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        subtitle = subtitle,
        trailingContent = trailingContent,
        accentColor = accentColor,
        variant = variant,
        enabled = enabled,
        highlighted = checked,
        showCheck = checked,
        highlightContent = checked,
        reserveCheckSlot = true,
        textMaxLines = textMaxLines,
        itemType = LiquidGlassDropdownItemType.MultipleChoice,
    )
}

@Composable
fun LiquidGlassDropdownSingleChoiceList(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    textMaxLines: Int = 1,
) {
    options.forEachIndexed { index, option ->
        LiquidGlassDropdownSingleChoiceItem(
            text = option,
            optionSize = options.size,
            isSelected = selectedIndex == index,
            index = index,
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            subtitle = subtitle,
            trailingContent = trailingContent,
            accentColor = accentColor,
            variant = variant,
            enabled = enabled,
            textMaxLines = textMaxLines,
        )
    }
}

@Composable
fun LiquidGlassDropdownActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    contentTint: Color? = null,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    LiquidGlassDropdownItem(
        text = text,
        selected = false,
        onClick = onClick,
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        subtitle = subtitle,
        trailingContent = trailingContent,
        accentColor = accentColor,
        contentTint = contentTint,
        variant = variant,
        enabled = enabled,
        highlighted = highlighted,
        showCheck = false,
        highlightContent = false,
    )
}
