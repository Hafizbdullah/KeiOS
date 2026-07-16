package os.kei.ui.page.main.about.section

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.about.model.AboutAppDetails
import os.kei.ui.page.main.about.ui.AboutCompactInfoRow
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.os.appLucideVersionIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.shape.appSquircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AboutAppCardSection(
    details: AboutAppDetails,
    cardColor: Color,
    accent: Color,
    subtitleColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    AppFeatureCard(
        title = stringResource(R.string.about_card_app_title),
        subtitle = stringResource(R.string.about_card_app_subtitle),
        modifier = Modifier.fillMaxWidth(),
        containerColor = cardColor,
        contentColor = MiuixTheme.colorScheme.onBackground,
        titleColor = accent,
        subtitleColor = subtitleColor,
        sectionStartAction = {
            AboutAppIcon(
                contentDescription = details.iconContentDescription,
                size = AppInteractiveTokens.cardHeaderLeadingSlotSize,
            )
        },
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        collapseOnSurfaceClick = true,
        contentPadding =
            PaddingValues(
                start = CardLayoutRhythm.cardHorizontalPadding,
                top = CardLayoutRhythm.sectionGap,
                end = CardLayoutRhythm.cardHorizontalPadding,
                bottom = CardLayoutRhythm.cardVerticalPadding,
            ),
        contentVerticalSpacing = 0.dp,
    ) {
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_name),
            value = details.appLabel,
            titleIcon = appLucideInfoIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_package_name),
            value = details.packageName,
            titleIcon = appLucideNotesIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_version),
            value = details.versionText,
            titleIcon = appLucideVersionIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_build_type),
            value = details.buildType,
            titleIcon = appLucideFilterIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_commit_time),
            value = details.commitTime,
            titleIcon = appLucideTimeIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_build_time),
            value = details.buildTime,
            titleIcon = appLucideTimeIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_last_update),
            value = details.updatedAt,
            titleIcon = appLucideTimeIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_debug),
            value = details.debugEnabledText,
            titleIcon = appLucideAlertIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_test_only),
            value = details.testOnlyEnabledText,
            titleIcon = appLucideAlertIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_api_level),
            value = details.apiLevel,
            titleIcon = appLucideFilterIcon(),
        )
        AboutCompactInfoRow(
            title = stringResource(R.string.about_label_security_patch),
            value = details.securityPatch,
            titleIcon = appLucideLockIcon(),
        )
    }
}

@Composable
private fun AboutAppIcon(
    contentDescription: String,
    size: Dp,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .appSquircleClip(999.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF0F5),
                            Color(0xFFFFD3E0),
                            Color(0xFFFF9FBE),
                        ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
        )
    }
}
