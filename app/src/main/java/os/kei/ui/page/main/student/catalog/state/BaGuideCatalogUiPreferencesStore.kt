package os.kei.ui.page.main.student.catalog.state

import androidx.compose.runtime.Immutable
import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab

@Immutable
internal data class BaGuideCatalogUiPreferences(
    val selectedStudentCatalogTab: BaGuideCatalogTab = BaGuideCatalogTab.Student,
    val filterSortSnapshot: BaGuideCatalogFilterSortSnapshot = BaGuideCatalogFilterSortSnapshot(),
)

internal object BaGuideCatalogUiPreferencesStore {
    private const val KV_ID = "ba_guide_catalog_ui_preferences"
    private const val KEY_SELECTED_STUDENT_CATALOG_TAB = "selected_student_catalog_tab"
    private const val KEY_STUDENT_SORT_MODE = "student_sort_mode"
    private const val KEY_NPC_SATELLITE_SORT_MODE = "npc_satellite_sort_mode"
    private const val KEY_STUDENT_FILTERS_RAW = "student_filters_raw"
    private const val KEY_NPC_SATELLITE_FILTERS_RAW = "npc_satellite_filters_raw"

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    fun load(keyValueStore: BaGuideCatalogUiPreferencesKeyValueStore = mmkvStore()): BaGuideCatalogUiPreferences =
        BaGuideCatalogUiPreferences(
            selectedStudentCatalogTab =
                decodeCatalogTab(
                    keyValueStore.decodeString(KEY_SELECTED_STUDENT_CATALOG_TAB, "").orEmpty(),
                ),
            filterSortSnapshot =
                BaGuideCatalogFilterSortSnapshot(
                    studentSortMode =
                        decodeCatalogSortMode(
                            keyValueStore.decodeString(KEY_STUDENT_SORT_MODE, "").orEmpty(),
                        ),
                    npcSatelliteSortMode =
                        decodeCatalogSortMode(
                            keyValueStore.decodeString(KEY_NPC_SATELLITE_SORT_MODE, "").orEmpty(),
                        ),
                    studentSelectedFiltersRaw = keyValueStore.decodeString(KEY_STUDENT_FILTERS_RAW, "").orEmpty(),
                    npcSatelliteSelectedFiltersRaw = keyValueStore.decodeString(KEY_NPC_SATELLITE_FILTERS_RAW, "").orEmpty(),
                ),
        )

    fun saveSelectedStudentCatalogTab(
        tab: BaGuideCatalogTab,
        keyValueStore: BaGuideCatalogUiPreferencesKeyValueStore = mmkvStore(),
    ) {
        keyValueStore.encode(KEY_SELECTED_STUDENT_CATALOG_TAB, tab.name)
    }

    fun saveFilterSortSnapshot(
        snapshot: BaGuideCatalogFilterSortSnapshot,
        keyValueStore: BaGuideCatalogUiPreferencesKeyValueStore = mmkvStore(),
    ) {
        keyValueStore.encode(KEY_STUDENT_SORT_MODE, snapshot.studentSortMode.name)
        keyValueStore.encode(KEY_NPC_SATELLITE_SORT_MODE, snapshot.npcSatelliteSortMode.name)
        keyValueStore.encode(KEY_STUDENT_FILTERS_RAW, snapshot.studentSelectedFiltersRaw)
        keyValueStore.encode(KEY_NPC_SATELLITE_FILTERS_RAW, snapshot.npcSatelliteSelectedFiltersRaw)
    }

    private fun mmkvStore(): BaGuideCatalogUiPreferencesKeyValueStore =
        MmkvBaGuideCatalogUiPreferencesKeyValueStore(store)
}

internal interface BaGuideCatalogUiPreferencesKeyValueStore {
    fun decodeString(
        key: String,
        defaultValue: String,
    ): String?

    fun encode(
        key: String,
        value: String,
    )
}

private class MmkvBaGuideCatalogUiPreferencesKeyValueStore(
    private val kv: MMKV,
) : BaGuideCatalogUiPreferencesKeyValueStore {
    override fun decodeString(
        key: String,
        defaultValue: String,
    ): String? = kv.decodeString(key, defaultValue)

    override fun encode(
        key: String,
        value: String,
    ) {
        kv.encode(key, value)
    }
}

internal fun decodeCatalogTab(raw: String): BaGuideCatalogTab =
    BaGuideCatalogTab.entries.firstOrNull { tab -> tab.name == raw.trim() }
        ?: BaGuideCatalogTab.Student

internal fun decodeCatalogSortMode(raw: String): BaGuideCatalogSortMode =
    BaGuideCatalogSortMode.entries.firstOrNull { mode -> mode.name == raw.trim() }
        ?: BaGuideCatalogSortMode.Default
