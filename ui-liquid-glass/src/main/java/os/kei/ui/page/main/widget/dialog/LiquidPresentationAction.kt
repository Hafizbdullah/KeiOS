@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * What a button in an alert or action sheet means. Apple defines three roles; [Primary] is the
 * emphasised default, which the alert guidance calls for on the trailing side of a button row.
 */
enum class LiquidActionRole {
    /** No special meaning. */
    Default,

    /** The action the person is most likely to want. Rendered filled. */
    Primary,

    /** Destroys data or performs a destructive action. */
    Destructive,

    /** Dismisses without taking action. Always titled "Cancel". */
    Cancel,
}

@Immutable
class LiquidPresentationAction(
    val label: String,
    val onClick: () -> Unit,
    val role: LiquidActionRole = LiquidActionRole.Default,
    val enabled: Boolean = true,
    val testTag: String? = null,
)

internal fun LiquidActionRole.glassVariant(): GlassVariant =
    when (this) {
        LiquidActionRole.Primary -> GlassVariant.SheetPrimaryAction
        LiquidActionRole.Destructive -> GlassVariant.SheetDangerAction
        LiquidActionRole.Default, LiquidActionRole.Cancel -> GlassVariant.SheetAction
    }

@Composable
internal fun LiquidActionRole.contentColor(): Color =
    when (this) {
        LiquidActionRole.Primary -> MiuixTheme.colorScheme.primary
        LiquidActionRole.Destructive -> MiuixTheme.colorScheme.error
        LiquidActionRole.Default -> MiuixTheme.colorScheme.primary
        LiquidActionRole.Cancel -> MiuixTheme.colorScheme.onBackgroundVariant
    }

@Composable
internal fun LiquidPresentationActionButton(
    action: LiquidPresentationAction,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    AppLiquidTextButton(
        backdrop = backdrop,
        text = action.label,
        onClick = action.onClick,
        modifier = action.testTag?.let { modifier.testTag(it) } ?: modifier,
        textColor = action.role.contentColor(),
        enabled = action.enabled,
        variant = action.role.glassVariant(),
    )
}

/**
 * Orders an action sheet's buttons the way Apple specifies: destructive choices at the top where they
 * are most noticeable, then the ordinary ones, with Cancel last.
 *
 * The ordering is enforced rather than left to the caller because both rules are absolute in the
 * guidance, and a Cancel that is not at the bottom is the kind of thing that only gets noticed in
 * review.
 */
internal fun liquidActionSheetOrder(
    actions: List<LiquidPresentationAction>,
): List<LiquidPresentationAction> =
    actions.filter { it.role == LiquidActionRole.Destructive } +
        actions.filter { it.role != LiquidActionRole.Destructive && it.role != LiquidActionRole.Cancel } +
        actions.filter { it.role == LiquidActionRole.Cancel }

/**
 * Alerts lay two buttons out side by side and three in a column.
 *
 * Apple caps alerts at three buttons and wants the most likely choice on the trailing side of a row.
 * Beyond two a row cannot hold readable titles, so it stacks.
 */
internal fun liquidAlertUsesButtonRow(actionCount: Int): Boolean = actionCount <= 2

/**
 * Trailing side of an alert's button row is where the expected choice goes, so Cancel sorts first and
 * the emphasised action last.
 */
internal fun liquidAlertRowOrder(
    actions: List<LiquidPresentationAction>,
): List<LiquidPresentationAction> =
    actions.sortedBy { action ->
        when (action.role) {
            LiquidActionRole.Cancel -> 0
            LiquidActionRole.Destructive -> 1
            LiquidActionRole.Default -> 2
            LiquidActionRole.Primary -> 3
        }
    }
