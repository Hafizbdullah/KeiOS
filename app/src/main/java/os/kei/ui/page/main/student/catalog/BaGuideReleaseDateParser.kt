package os.kei.ui.page.main.student.catalog

import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.BaStudentGuideInfo
import java.time.LocalDate
import java.time.ZoneOffset

internal fun extractBaGuideReleaseDateSec(info: BaStudentGuideInfo): Long =
    extractBaGuideReleaseDateSec(
        profileRows = info.profileRows,
        stats = info.stats,
    )

internal fun extractBaGuideReleaseDateSec(
    profileRows: List<BaGuideRow>,
    stats: List<Pair<String, String>>,
): Long {
    val candidates =
        sequence {
            profileRows.forEach { row ->
                if (row.key.contains("实装日期", ignoreCase = true)) {
                    yield(row.value)
                }
            }
            stats.forEach { (key, value) ->
                if (key.contains("实装日期", ignoreCase = true)) {
                    yield(value)
                }
            }
        }
    return candidates
        .map { parseBaGuideReleaseDateSec(it) }
        .firstOrNull { it > 0L }
        ?: 0L
}

private fun parseBaGuideReleaseDateSec(raw: String): Long {
    if (raw.isBlank()) return 0L
    val compact =
        raw
            .substringBefore("<-")
            .substringBefore("←")
            .trim()
    if (compact.isBlank()) return 0L

    val classic = RELEASE_DATE_CLASSIC_REGEX.find(compact)
    if (classic != null) {
        return releaseDateToEpochSecond(
            year = classic.groupValues.getOrNull(1)?.toIntOrNull(),
            month = classic.groupValues.getOrNull(2)?.toIntOrNull(),
            day = classic.groupValues.getOrNull(3)?.toIntOrNull(),
        )
    }

    val packed = RELEASE_DATE_PACKED_REGEX.find(compact)
    if (packed != null) {
        return releaseDateToEpochSecond(
            year = packed.groupValues.getOrNull(1)?.toIntOrNull(),
            month = packed.groupValues.getOrNull(2)?.toIntOrNull(),
            day = packed.groupValues.getOrNull(3)?.toIntOrNull(),
        )
    }
    return 0L
}

private fun releaseDateToEpochSecond(
    year: Int?,
    month: Int?,
    day: Int?,
): Long {
    if (year == null || month == null || day == null) return 0L
    if (year < 2000 || month !in 1..12 || day !in 1..31) return 0L
    return runCatching {
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
    }.getOrDefault(0L)
}

private val RELEASE_DATE_CLASSIC_REGEX = Regex("""(20\d{2})[^\d]{1,4}(\d{1,2})[^\d]{1,4}(\d{1,2})""")
private val RELEASE_DATE_PACKED_REGEX = Regex("""(20\d{2})(\d{2})(\d{2})""")
