package os.kei.ui.page.main.widget.glass

/**
 * The two iOS menu layouts that put a row above the list, plus the plain list, per Apple's Menus
 * guidance.
 *
 * - **Small** — a row of *four* items, each a symbol with **no label**. For closely related actions that
 *   read as a set on their own, like Bold / Italic / Underline / Strikethrough. Only worth using where
 *   the symbol alone identifies the action.
 * - **Medium** — a row of *three* items, each a symbol above a short label. The layout Notes uses for
 *   Scan / Lock / Pin, and the one every menu in this app already matched.
 * - **Large** — everything in the list, no top row. Apple's default.
 *
 * The counts are Apple's and are enforced, which is why [resolveLiquidMenuLayoutPlan] will decline a
 * layout it cannot fill rather than render a half-empty grid.
 */
enum class LiquidMenuLayout {
    Small,
    Medium,
    Large,
}

/** How many items Apple's top row holds, or null for the layout that has no top row. */
internal fun LiquidMenuLayout.topRowCapacity(): Int? =
    when (this) {
        LiquidMenuLayout.Small -> 4
        LiquidMenuLayout.Medium -> 3
        LiquidMenuLayout.Large -> null
    }

internal class LiquidMenuLayoutPlan(
    /**
     * The Apple layout in force, or null for this app's own flexible labelled row — the behaviour for
     * quick-action counts Apple does not define a layout for.
     */
    val layout: LiquidMenuLayout?,
    /** The quick actions occupying the top row, in order. Empty when there is no top row. */
    val topRow: List<LiquidGlassActionMenuQuickAction>,
    /**
     * Quick actions that did not fit a bounded layout the caller asked for. Rendered as ordinary rows at
     * the head of the list rather than dropped — an action the caller passed has to stay reachable.
     */
    val listed: List<LiquidGlassActionMenuQuickAction>,
) {
    /** Small is symbol-only by definition; everything else labels its top row. */
    val topRowShowsLabels: Boolean get() = layout != LiquidMenuLayout.Small
}

/**
 * Decides which layout a menu actually gets.
 *
 * Deliberately conservative about relocating anything. A caller who passes quick actions has made a
 * design decision, so **auto mode never moves one into the list**: it recognises Apple's two counts and
 * otherwise keeps the flexible labelled row this app already had. Relocation happens only when a caller
 * explicitly asks for a bounded layout and hands over more actions than it seats — and even then the
 * overflow is listed, never dropped.
 *
 * A bounded layout that cannot be filled is declined rather than half-drawn: a row of two in a
 * four-slot grid reads as broken, and Apple's advice for one or two items is to use buttons or toggles
 * instead of a row at all.
 */
internal fun resolveLiquidMenuLayoutPlan(
    requested: LiquidMenuLayout?,
    quickActions: List<LiquidGlassActionMenuQuickAction>,
): LiquidMenuLayoutPlan {
    if (quickActions.isEmpty()) {
        return LiquidMenuLayoutPlan(LiquidMenuLayout.Large, emptyList(), emptyList())
    }
    if (requested == LiquidMenuLayout.Large) {
        return LiquidMenuLayoutPlan(LiquidMenuLayout.Large, emptyList(), quickActions)
    }
    val bounded =
        when (requested) {
            null ->
                when (quickActions.size) {
                    4 -> LiquidMenuLayout.Small
                    3 -> LiquidMenuLayout.Medium
                    else -> null
                }

            else -> requested.takeIf { quickActions.size >= (it.topRowCapacity() ?: 0) }
        }
    val capacity = bounded?.topRowCapacity()
        ?: return LiquidMenuLayoutPlan(layout = null, topRow = quickActions, listed = emptyList())
    return LiquidMenuLayoutPlan(
        layout = bounded,
        topRow = quickActions.take(capacity),
        listed = quickActions.drop(capacity),
    )
}

/**
 * A quick action rendered as an ordinary list row, for overflow from a bounded layout.
 *
 * Apple asks for a uniform treatment inside a group — icons for all of a group's items or none — so the
 * symbol comes along as the row's leading icon. A custom [LiquidGlassActionMenuQuickAction.contentDescription]
 * does not survive, because a row announces its own text; the label is preserved, which is what a screen
 * reader needs.
 */
internal fun LiquidGlassActionMenuQuickAction.asMenuRow(): LiquidGlassActionMenuActionRow =
    LiquidGlassActionMenuActionRow(
        id = id,
        text = label,
        onClick = onClick,
        leadingIcon = icon,
        enabled = enabled,
        variant = variant,
    )
