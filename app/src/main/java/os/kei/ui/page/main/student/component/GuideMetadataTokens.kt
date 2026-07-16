package os.kei.ui.page.main.student.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stabilizes the one-line compact pill at its intended 26 dp visual height. */
internal val GuidePassiveMetadataPillMinHeight = 26.dp

/** Keeps dynamic student-guide metadata from starving adjacent titles on compact screens. */
internal val GuidePassiveMetadataPillMaxWidth = 100.dp

/** Restores the original compact geometry for primary skill metadata such as type and cost. */
internal val GuideSkillPrimaryMetadataPillMinHeight = 32.dp

internal val GuideSkillPrimaryMetadataPillPadding =
    PaddingValues(horizontal = 10.dp, vertical = 6.dp)

/** Keeps stacked skill state tags tighter than the primary metadata pills. */
internal val GuideSkillStateMetadataPillMinHeight = 30.dp

internal val GuideSkillStateMetadataPillPadding =
    PaddingValues(horizontal = 8.dp, vertical = 5.dp)

/** Allows the pill to grow naturally with font scale while preserving a readable body rhythm. */
internal val GuideSkillMetadataPillTypography =
    TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
