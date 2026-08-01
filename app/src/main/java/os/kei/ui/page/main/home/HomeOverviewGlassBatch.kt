@file:Suppress("FunctionName")

package os.kei.ui.page.main.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.RuntimeShaderCache
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import org.intellij.lang.annotations.Language
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.core.AppOverviewLiquidPillBatchHost
import os.kei.ui.page.main.widget.core.LocalOverviewPillBatchTransformProvider
import os.kei.ui.page.main.widget.core.OverviewPillBatchTransform
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import os.kei.ui.page.main.widget.glass.radialRefraction
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

private const val MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT = 8
private val HOME_OVERVIEW_CARD_CORNER_RADIUS = 20.dp

internal val LocalHomeOverviewCardBatchState = compositionLocalOf<HomeOverviewCardBatchState?> { null }

private data class HomeOverviewCardRegistration(
    val coordinates: LayoutCoordinates,
    val baseBounds: Rect,
    val interactiveHighlight: InteractiveHighlight?,
)

internal data class HomeOverviewCardVisualTransform(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
)

internal data class HomeOverviewCardBatchRect(
    val bounds: Rect,
    val radius: Float,
    val highlightScale: Float = 1f,
)

internal data class HomeOverviewCardBatchUniforms(
    val cardCount: Int,
    val bounds: FloatArray,
    val radii: FloatArray,
    val highlightScales: FloatArray,
    val refractionHeight: Float,
    val refractionAmount: Float,
)

internal data class ResolvedHomeOverviewCard(
    val material: HomeOverviewCardBatchRect,
    val pressProgress: Float,
    val touchCenter: Offset,
)

internal class HomeOverviewCardBatchState(
    private val highlightPressBoostRatio: Float = HOME_OVERVIEW_HIGHLIGHT_PRESS_BOOST,
) {
    private var rootCoordinates by mutableStateOf<LayoutCoordinates?>(null)
    private val registrations = mutableStateMapOf<Any, HomeOverviewCardRegistration>()
    private var outlineVersionState by mutableIntStateOf(0)

    val outlineVersion: Int
        get() = outlineVersionState

    fun updateRootCoordinates(coordinates: LayoutCoordinates) {
        val rootChanged = rootCoordinates !== coordinates
        rootCoordinates = coordinates
        val updates =
            registrations.mapNotNull { (key, registration) ->
                val bounds = resolveBaseBounds(registration.coordinates) ?: return@mapNotNull null
                if (registration.baseBounds != bounds) {
                    key to registration.copy(baseBounds = bounds)
                } else {
                    null
                }
            }
        updates.forEach { (key, registration) ->
            registrations[key] = registration
        }
        if (rootChanged || updates.isNotEmpty()) outlineVersionState++
    }

    fun register(
        key: Any,
        coordinates: LayoutCoordinates,
        interactiveHighlight: InteractiveHighlight?,
    ) {
        val baseBounds = resolveBaseBounds(coordinates) ?: Rect.Zero
        val registration = registrations[key]
        if (registration == null) {
            registrations[key] =
                HomeOverviewCardRegistration(
                    coordinates = coordinates,
                    baseBounds = baseBounds,
                    interactiveHighlight = interactiveHighlight,
                )
            outlineVersionState++
        } else if (
            registration.coordinates !== coordinates ||
            registration.baseBounds != baseBounds ||
            registration.interactiveHighlight !== interactiveHighlight
        ) {
            registrations[key] =
                registration.copy(
                    coordinates = coordinates,
                    baseBounds = baseBounds,
                    interactiveHighlight = interactiveHighlight,
                )
            outlineVersionState++
        }
    }

    fun unregister(key: Any) {
        if (registrations.remove(key) != null) {
            outlineVersionState++
        }
    }

    private fun resolveBaseBounds(coordinates: LayoutCoordinates): Rect? {
        val root = rootCoordinates?.takeIf(LayoutCoordinates::isAttached) ?: return null
        if (!coordinates.isAttached) return null
        val width = coordinates.size.width.toFloat()
        val height = coordinates.size.height.toFloat()
        if (width <= 0f || height <= 0f) return null
        val topLeft = root.localPositionOf(coordinates, Offset.Zero)
        return Rect(topLeft, Size(width, height))
    }

    fun maxPressProgress(): Float =
        registrations.values
            .maxOfOrNull { registration ->
                registration.interactiveHighlight?.pressProgress ?: 0f
            }?.coerceIn(0f, 1f) ?: 0f

    fun resolvePillTransform(
        key: Any,
        density: Density,
    ): OverviewPillBatchTransform? {
        val registration = registrations[key] ?: return null
        val highlight = registration.interactiveHighlight ?: return null
        val baseBounds = registration.baseBounds
        if (baseBounds.width <= 0f || baseBounds.height <= 0f) return null
        val visualTransform =
            resolveHomeOverviewCardVisualTransform(
                width = baseBounds.width,
                height = baseBounds.height,
                deformationProgress = highlight.deformationProgress,
                dragOffset = highlight.offset,
                pressExpansionPx = with(density) { 4.dp.toPx() },
            )
        return OverviewPillBatchTransform(
            pivot = baseBounds.center,
            translation = Offset(visualTransform.translationX, visualTransform.translationY),
            scaleX = visualTransform.scaleX,
            scaleY = visualTransform.scaleY,
        )
    }

    fun resolvedCards(density: Density): List<ResolvedHomeOverviewCard> {
        rootCoordinates?.takeIf(LayoutCoordinates::isAttached) ?: return emptyList()
        val cornerRadiusPx = with(density) { HOME_OVERVIEW_CARD_CORNER_RADIUS.toPx() }
        return registrations.values
            .asSequence()
            .take(MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT)
            .mapNotNull { registration ->
                if (!registration.coordinates.isAttached) return@mapNotNull null
                val baseBounds = registration.baseBounds
                val width = baseBounds.width
                val height = baseBounds.height
                if (width <= 0f || height <= 0f) return@mapNotNull null
                val highlight = registration.interactiveHighlight
                val transform =
                    resolveHomeOverviewCardVisualTransform(
                        width = width,
                        height = height,
                        deformationProgress = highlight?.deformationProgress ?: 0f,
                        dragOffset = highlight?.offset ?: Offset.Zero,
                        pressExpansionPx = with(density) { 4.dp.toPx() },
                    )
                val transformedBounds = baseBounds.transformedBy(transform)
                val pressProgress = highlight?.pressProgress?.coerceIn(0f, 1f) ?: 0f
                val baseTouch = highlight?.touchPosition ?: Offset(width / 2f, height / 2f)
                val touchCenter =
                    transformPointInBounds(
                        point = baseBounds.topLeft + baseTouch,
                        bounds = baseBounds,
                        transform = transform,
                    )
                ResolvedHomeOverviewCard(
                    material =
                        HomeOverviewCardBatchRect(
                            bounds = transformedBounds,
                            radius = cornerRadiusPx * min(transform.scaleX, transform.scaleY),
                            highlightScale = 1f + pressProgress * highlightPressBoostRatio,
                        ),
                    pressProgress = pressProgress,
                    touchCenter = touchCenter,
                )
            }.toList()
    }
}

@Composable
internal fun HomeOverviewGlassBatchHost(
    backdrop: Backdrop?,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val activeBackdrop = activeGlassBackdrop(backdrop.takeIf { blurEnabled })
    if (activeBackdrop == null) {
        Box(modifier = modifier) { content() }
        return
    }

    val isDark = isAppInDarkTheme()
    val blurRadius = resolvedGlassBlurDp(8.dp, GlassVariant.Content)
    val lensRadius = resolvedGlassLensDp(24.dp, GlassVariant.Content)
    val containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = HOME_INFO_CARD_SURFACE_ALPHA)
    val baseHighlightAlpha = if (isDark) 0.42f else 0.62f
    val density = LocalDensity.current
    val state =
        remember(baseHighlightAlpha) {
            HomeOverviewCardBatchState(
                highlightPressBoostRatio = 0.10f / baseHighlightAlpha,
            )
        }
    val outlineVersion = state.outlineVersion

    Box(
        modifier =
            modifier
                .onGloballyPositioned(state::updateRootCoordinates)
                .drawBackdrop(
                    backdrop = activeBackdrop,
                    shape = {
                        HomeOverviewCardBatchShape(
                            cards = state.resolvedCards(density).map(ResolvedHomeOverviewCard::material),
                            version = outlineVersion,
                        )
                    },
                    effects = {
                        vibrancy()
                        blur(blurRadius.toPx())
                        val cards = state.resolvedCards(this)
                        homeOverviewCardBatchLens(
                            cards = cards.map(ResolvedHomeOverviewCard::material),
                            refractionHeight = lensRadius.toPx(),
                            refractionAmount = lensRadius.toPx(),
                        )
                        cards.maxByOrNull(ResolvedHomeOverviewCard::pressProgress)
                            ?.takeIf { card -> card.pressProgress > 0f }
                            ?.let { pressedCard ->
                                radialRefraction(
                                    centerX = pressedCard.touchCenter.x,
                                    centerY = pressedCard.touchCenter.y,
                                    radius = lensRadius.toPx() * 2f,
                                    strength = 6f * pressedCard.pressProgress,
                                )
                            }
                    },
                    highlight = {
                        Highlight.Default.copy(
                            alpha = baseHighlightAlpha,
                            style =
                                HomeOverviewCardBatchHighlightStyle(
                                    state = state,
                                ),
                        )
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(alpha = 0.10f),
                        )
                    },
                    innerShadow = {
                        val progress = state.maxPressProgress()
                        if (progress > 0f) {
                            InnerShadow(
                                radius = 6.dp * progress,
                                alpha = 0.55f * progress,
                            )
                        } else {
                            null
                        }
                    },
                    onDrawSurface = {
                        state.resolvedCards(this).forEach { card ->
                            val material = card.material
                            drawRoundRect(
                                color = containerColor,
                                topLeft = material.bounds.topLeft,
                                size = material.bounds.size,
                                cornerRadius = CornerRadius(material.radius),
                            )
                        }
                    },
                ),
    ) {
        CompositionLocalProvider(LocalHomeOverviewCardBatchState provides state) {
            AppOverviewLiquidPillBatchHost(
                backdrop = activeBackdrop,
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
internal fun HomeOverviewBatchedCard(
    state: HomeOverviewCardBatchState,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val key = remember { Any() }
    val interactionSource = remember { MutableInteractionSource() }
    val animationScope = rememberCoroutineScope()
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val density = LocalDensity.current
    val interactiveHighlight =
        if (onClick != null) {
            remember(animationScope, animationsEnabled) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    animationsEnabled = animationsEnabled,
                )
            }
        } else {
            null
        }
    val pillTransformProvider =
        if (interactiveHighlight != null) {
            remember(state, key, density, interactiveHighlight) {
                { state.resolvePillTransform(key = key, density = density) }
            }
        } else {
            null
        }
    DisposableEffect(state, key) {
        onDispose { state.unregister(key) }
    }

    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }
    val interactiveModifier =
        if (interactiveHighlight != null) {
            Modifier
                .homeOverviewCardVisualTransform(interactiveHighlight)
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
        } else {
            Modifier
        }

    Box(
        modifier =
            Modifier
                .padding(horizontal = HOME_CARD_HORIZONTAL_PADDING_DP.dp)
                .padding(bottom = HOME_INFO_CARD_GAP),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        state.register(
                            key = key,
                            coordinates = coordinates,
                            interactiveHighlight = interactiveHighlight,
                        )
                    }.then(clickModifier)
                    .then(interactiveModifier)
                    .padding(
                        horizontal = HOME_INFO_CARD_HORIZONTAL_CONTENT_PADDING,
                        vertical = HOME_INFO_CARD_VERTICAL_CONTENT_PADDING,
                    ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        ) {
            CompositionLocalProvider(
                LocalOverviewPillBatchTransformProvider provides pillTransformProvider,
            ) {
                content()
            }
        }
    }
}

internal fun resolveHomeOverviewCardVisualTransform(
    width: Float,
    height: Float,
    deformationProgress: Float,
    dragOffset: Offset,
    pressExpansionPx: Float,
): HomeOverviewCardVisualTransform {
    if (width <= 0f || height <= 0f) {
        return HomeOverviewCardVisualTransform(0f, 0f, 1f, 1f)
    }
    val progress = deformationProgress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
    val safeOffset =
        Offset(
            dragOffset.x.takeIf(Float::isFinite) ?: 0f,
            dragOffset.y.takeIf(Float::isFinite) ?: 0f,
        )
    val expansion = pressExpansionPx.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 0f
    val scale = 1f + expansion / height * progress
    val maxOffset = min(width, height)
    val translationX = maxOffset * tanh(HOME_OVERVIEW_DRAG_DERIVATIVE * safeOffset.x / maxOffset)
    val translationY = maxOffset * tanh(HOME_OVERVIEW_DRAG_DERIVATIVE * safeOffset.y / maxOffset)
    val maxDragScale = expansion / height
    val offsetAngle = atan2(safeOffset.y, safeOffset.x)
    val scaleX =
        scale +
            maxDragScale * abs(cos(offsetAngle) * safeOffset.x / maxOf(width, height)) *
            (width / height).fastCoerceAtMost(1f)
    val scaleY =
        scale +
            maxDragScale * abs(sin(offsetAngle) * safeOffset.y / maxOf(width, height)) *
            (height / width).fastCoerceAtMost(1f)
    return HomeOverviewCardVisualTransform(
        translationX = translationX,
        translationY = translationY,
        scaleX = scaleX,
        scaleY = scaleY,
    )
}

private fun Modifier.homeOverviewCardVisualTransform(
    interactiveHighlight: InteractiveHighlight,
): Modifier =
    drawWithContent outer@{
        val transform =
            resolveHomeOverviewCardVisualTransform(
                width = size.width,
                height = size.height,
                deformationProgress = interactiveHighlight.deformationProgress,
                dragOffset = interactiveHighlight.offset,
                pressExpansionPx = 4.dp.toPx(),
            )
        val contentCenter = Offset(size.width / 2f, size.height / 2f)
        withTransform(
            transformBlock = {
                translate(transform.translationX, transform.translationY)
                scale(
                    scaleX = transform.scaleX,
                    scaleY = transform.scaleY,
                    pivot = contentCenter,
                )
            },
        ) {
            this@outer.drawContent()
        }
    }

private fun Rect.transformedBy(transform: HomeOverviewCardVisualTransform): Rect {
    val transformedCenter = this.center + Offset(transform.translationX, transform.translationY)
    val halfWidth = width * transform.scaleX / 2f
    val halfHeight = height * transform.scaleY / 2f
    return Rect(
        left = transformedCenter.x - halfWidth,
        top = transformedCenter.y - halfHeight,
        right = transformedCenter.x + halfWidth,
        bottom = transformedCenter.y + halfHeight,
    )
}

private fun transformPointInBounds(
    point: Offset,
    bounds: Rect,
    transform: HomeOverviewCardVisualTransform,
): Offset {
    val relative = point - bounds.center
    return bounds.center +
        Offset(transform.translationX, transform.translationY) +
        Offset(relative.x * transform.scaleX, relative.y * transform.scaleY)
}

private data class HomeOverviewCardBatchShape(
    val cards: List<HomeOverviewCardBatchRect>,
    val version: Int,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        cards.forEach { material ->
            path.addRoundRect(
                RoundRect(
                    rect = material.bounds,
                    cornerRadius = CornerRadius(material.radius),
                ),
            )
        }
        return Outline.Generic(path)
    }
}

private fun BackdropEffectScope.homeOverviewCardBatchLens(
    cards: List<HomeOverviewCardBatchRect>,
    refractionHeight: Float,
    refractionAmount: Float,
) {
    val uniforms =
        resolveHomeOverviewCardBatchUniforms(
            cards = cards,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
        ) ?: return
    if (padding > 0f) {
        padding = (padding - uniforms.refractionHeight).coerceAtLeast(0f)
    }
    runtimeShaderEffect(
        key = "HomeOverviewCardBatchLens-${uniforms.cardCount}",
        shaderString = homeOverviewCardBatchLensShader(uniforms.cardCount),
        uniformShaderName = "content",
    ) {
        setFloatUniform("cardBounds", uniforms.bounds)
        setFloatUniform("cardRadii", uniforms.radii)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("refractionHeight", uniforms.refractionHeight)
        setFloatUniform("refractionAmount", -uniforms.refractionAmount)
    }
}

internal fun resolveHomeOverviewCardBatchUniforms(
    cards: List<HomeOverviewCardBatchRect>,
    refractionHeight: Float,
    refractionAmount: Float,
): HomeOverviewCardBatchUniforms? {
    if (!refractionHeight.isFinite() || !refractionAmount.isFinite()) return null
    val validCards =
        cards
            .take(MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT)
            .filter { card ->
                card.bounds.width > 0f &&
                    card.bounds.height > 0f &&
                    card.radius.isFinite() &&
                    card.radius > 0f
            }
    if (validCards.isEmpty()) return null
    val uniformBounds = FloatArray(validCards.size * 4)
    val uniformRadii = FloatArray(validCards.size)
    val uniformHighlightScales = FloatArray(validCards.size) { 1f }
    validCards.forEachIndexed { index, card ->
        val offset = index * 4
        uniformBounds[offset] = card.bounds.left
        uniformBounds[offset + 1] = card.bounds.top
        uniformBounds[offset + 2] = card.bounds.right
        uniformBounds[offset + 3] = card.bounds.bottom
        uniformRadii[index] = card.radius.coerceAtMost(min(card.bounds.width, card.bounds.height) / 2f)
        uniformHighlightScales[index] =
            card.highlightScale.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 1f
    }
    val resolvedHeight =
        refractionHeight.coerceIn(
            minimumValue = 0f,
            maximumValue = validCards.minOf { card -> card.radius },
        )
    val resolvedAmount =
        refractionAmount.coerceIn(
            minimumValue = 0f,
            maximumValue = validCards.minOf { card -> min(card.bounds.width, card.bounds.height) },
        )
    return HomeOverviewCardBatchUniforms(
        cardCount = validCards.size,
        bounds = uniformBounds,
        radii = uniformRadii,
        highlightScales = uniformHighlightScales,
        refractionHeight = resolvedHeight,
        refractionAmount = resolvedAmount,
    ).takeIf { uniforms -> uniforms.refractionHeight > 0f && uniforms.refractionAmount > 0f }
}

private data class HomeOverviewCardBatchHighlightStyle(
    val state: HomeOverviewCardBatchState,
) : HighlightStyle {
    override val color: Color = Color.White.copy(alpha = 0.5f)
    override val blendMode: BlendMode = BlendMode.Plus

    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache,
    ): RuntimeShader? {
        if (!isRuntimeShaderSupported()) return null
        val uniforms =
            resolveHomeOverviewCardBatchUniforms(
                cards = state.resolvedCards(this).map(ResolvedHomeOverviewCard::material),
                refractionHeight = 1f,
                refractionAmount = 1f,
            ) ?: return null
        return runtimeShaderCache.obtainRuntimeShader(
            "HomeOverviewCardBatchHighlight-${uniforms.cardCount}",
            homeOverviewCardBatchHighlightShader(uniforms.cardCount),
        ).apply {
            setFloatUniform("cardBounds", uniforms.bounds)
            setFloatUniform("cardRadii", uniforms.radii)
            setFloatUniform("highlightScales", uniforms.highlightScales)
            setColorUniform("color", color.copy(alpha = 1f))
            setFloatUniform("angle", Math.toRadians(45.0).toFloat())
            setFloatUniform("falloff", 1f)
        }
    }
}

private const val HOME_OVERVIEW_DRAG_DERIVATIVE = 0.05f
private const val HOME_OVERVIEW_HIGHLIGHT_PRESS_BOOST = 0.10f / 0.62f

private val HomeOverviewCardBatchLensShaderCache =
    arrayOfNulls<String>(MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT + 1)
private val HomeOverviewCardBatchHighlightShaderCache =
    arrayOfNulls<String>(MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT + 1)

@Language("AGSL")
private fun homeOverviewCardBatchGeometryShader(
    cardCount: Int,
    includeHighlightScale: Boolean,
) =
    """
float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    }
    float gradX = step(cornerCoord.y, cornerCoord.x);
    return sign(coord) * float2(gradX, 1.0 - gradX);
}

float4 findCardBounds(
    float2 coord,
    float expansion,
    out float selectedRadius${if (includeHighlightScale) ",\n    out float selectedHighlightScale" else ""}
) {
    float4 selectedBounds = float4(0.0);
    selectedRadius = 0.0;
    ${if (includeHighlightScale) "selectedHighlightScale = 1.0;" else ""}
    for (int index = 0; index < $cardCount; index++) {
        float4 candidate = cardBounds[index];
        bool contains = coord.x >= candidate.x - expansion &&
            coord.x <= candidate.z + expansion &&
            coord.y >= candidate.y - expansion &&
            coord.y <= candidate.w + expansion;
        if (contains) {
            selectedBounds = candidate;
            selectedRadius = cardRadii[index];
            ${if (includeHighlightScale) "selectedHighlightScale = highlightScales[index];" else ""}
        }
    }
    return selectedBounds;
}
    """.trimIndent()

@Language("AGSL")
private fun homeOverviewCardBatchLensShader(cardCount: Int): String {
    require(cardCount in 1..MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT)
    HomeOverviewCardBatchLensShaderCache[cardCount]?.let { return it }
    val shader =
        """
uniform shader content;
uniform float4 cardBounds[$cardCount];
uniform float cardRadii[$cardCount];
uniform float2 offset;
uniform float refractionHeight;
uniform float refractionAmount;

${homeOverviewCardBatchGeometryShader(cardCount, includeHighlightScale = false)}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 componentCoord = coord + offset;
    float radius;
    float4 bounds = findCardBounds(componentCoord, 0.0, radius);
    if (bounds.z <= bounds.x || bounds.w <= bounds.y) {
        return content.eval(coord);
    }
    float2 halfSize = (bounds.zw - bounds.xy) * 0.5;
    float2 centeredCoord = componentCoord - (bounds.xy + halfSize);
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    float normalizedDistance = clamp(1.0 - -sd / refractionHeight, 0.0, 1.0);
    float displacement = circleMap(normalizedDistance) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 gradient = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius));
    return content.eval(coord + displacement * gradient);
}
        """.trimIndent()
    HomeOverviewCardBatchLensShaderCache[cardCount] = shader
    return shader
}

@Language("AGSL")
private fun homeOverviewCardBatchHighlightShader(cardCount: Int): String {
    require(cardCount in 1..MAX_BATCHED_HOME_OVERVIEW_CARD_COUNT)
    HomeOverviewCardBatchHighlightShaderCache[cardCount]?.let { return it }
    val shader =
        """
uniform float4 cardBounds[$cardCount];
uniform float cardRadii[$cardCount];
uniform float highlightScales[$cardCount];
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

${homeOverviewCardBatchGeometryShader(cardCount, includeHighlightScale = true)}

half4 main(float2 coord) {
    float radius;
    float highlightScale;
    float4 bounds = findCardBounds(coord, 2.0, radius, highlightScale);
    if (bounds.z <= bounds.x || bounds.w <= bounds.y) {
        return half4(0.0);
    }
    float2 halfSize = (bounds.zw - bounds.xy) * 0.5;
    float2 centeredCoord = coord - (bounds.xy + halfSize);
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 gradient = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float intensity = pow(abs(dot(gradient, normal)), falloff);
    return color * intensity * highlightScale;
}
        """.trimIndent()
    HomeOverviewCardBatchHighlightShaderCache[cardCount] = shader
    return shader
}
