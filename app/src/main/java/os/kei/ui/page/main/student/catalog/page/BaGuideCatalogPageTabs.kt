package os.kei.ui.page.main.student.catalog.page

import os.kei.R
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab

internal enum class BaGuideCatalogPageTab(
    val labelRes: Int,
    val compactLabelRes: Int,
    val catalogTab: BaGuideCatalogTab?,
    val specialTab: BaGuideCatalogSpecialTab? = null,
) {
    Student(
        labelRes = R.string.ba_catalog_tab_students,
        compactLabelRes = R.string.ba_catalog_tab_students_short,
        catalogTab = null,
    ),
    MemoryLobby(
        labelRes = R.string.ba_catalog_tab_memory_lobby,
        compactLabelRes = R.string.ba_catalog_tab_memory_lobby_short,
        catalogTab = null,
        specialTab = BaGuideCatalogSpecialTab.MemoryLobby,
    ),
    StudentBgm(
        labelRes = R.string.ba_catalog_tab_student_bgm,
        compactLabelRes = R.string.ba_catalog_tab_student_bgm_short,
        catalogTab = null,
        specialTab = BaGuideCatalogSpecialTab.StudentBgm,
    ),
    Bgm(
        labelRes = R.string.ba_catalog_tab_bgm,
        compactLabelRes = R.string.ba_catalog_tab_bgm,
        catalogTab = null,
        specialTab = BaGuideCatalogSpecialTab.FavoriteBgm,
    ),
}

internal enum class BaGuideCatalogSpecialTab {
    MemoryLobby,
    StudentBgm,
    FavoriteBgm,
}

internal fun BaGuideCatalogPageTab.resolvedCatalogTab(
    selectedStudentCatalogTab: BaGuideCatalogTab,
): BaGuideCatalogTab? =
    when (this) {
        BaGuideCatalogPageTab.Student -> selectedStudentCatalogTab
        else -> catalogTab
    }

internal fun BaGuideCatalogPageTab.searchPlaceholderRes(
    selectedStudentCatalogTab: BaGuideCatalogTab,
): Int =
    when (this) {
        BaGuideCatalogPageTab.Student ->
            when (selectedStudentCatalogTab) {
                BaGuideCatalogTab.Student -> R.string.ba_catalog_search_placeholder_student
                BaGuideCatalogTab.NpcSatellite -> R.string.ba_catalog_search_placeholder_npc_satellite
            }

        BaGuideCatalogPageTab.MemoryLobby -> R.string.ba_catalog_search_placeholder_memory_lobby
        BaGuideCatalogPageTab.StudentBgm -> R.string.ba_catalog_search_placeholder_music
        BaGuideCatalogPageTab.Bgm -> R.string.ba_catalog_search_placeholder_playback
    }

internal val BaGuideCatalogTab.studentTypeLabelRes: Int
    get() =
        when (this) {
            BaGuideCatalogTab.Student -> R.string.ba_catalog_tab_student
            BaGuideCatalogTab.NpcSatellite -> R.string.ba_catalog_tab_npc_satellite
        }
