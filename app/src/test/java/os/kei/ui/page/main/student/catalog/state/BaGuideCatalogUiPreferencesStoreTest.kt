package os.kei.ui.page.main.student.catalog.state

import org.junit.Test
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import kotlin.test.assertEquals

class BaGuideCatalogUiPreferencesStoreTest {
    @Test
    fun `load returns defaults for missing or invalid values`() {
        val store =
            FakeCatalogUiPreferencesStore(
                "selected_student_catalog_tab" to "Unknown",
                "student_sort_mode" to "BadSort",
                "npc_satellite_sort_mode" to "",
            )

        val preferences = BaGuideCatalogUiPreferencesStore.load(store)

        assertEquals(BaGuideCatalogTab.Student, preferences.selectedStudentCatalogTab)
        assertEquals(BaGuideCatalogSortMode.Default, preferences.filterSortSnapshot.studentSortMode)
        assertEquals(BaGuideCatalogSortMode.Default, preferences.filterSortSnapshot.npcSatelliteSortMode)
    }

    @Test
    fun `selected student catalog tab is persisted`() {
        val store = FakeCatalogUiPreferencesStore()

        BaGuideCatalogUiPreferencesStore.saveSelectedStudentCatalogTab(
            tab = BaGuideCatalogTab.NpcSatellite,
            keyValueStore = store,
        )

        val preferences = BaGuideCatalogUiPreferencesStore.load(store)

        assertEquals(BaGuideCatalogTab.NpcSatellite, preferences.selectedStudentCatalogTab)
    }

    @Test
    fun `filter and sort snapshot is persisted per catalog tab`() {
        val store = FakeCatalogUiPreferencesStore()
        val snapshot =
            BaGuideCatalogFilterSortSnapshot(
                sortMode = BaGuideCatalogSortMode.GlobalScoreDesc,
                searchQuery = "ignored",
                selectedFiltersRaw = "legacy",
                studentSortMode = BaGuideCatalogSortMode.ReleaseDateDesc,
                npcSatelliteSortMode = BaGuideCatalogSortMode.CnScoreAsc,
                studentSelectedFiltersRaw = "68:176",
                npcSatelliteSelectedFiltersRaw = "12034:12035",
            )

        BaGuideCatalogUiPreferencesStore.saveFilterSortSnapshot(
            snapshot = snapshot,
            keyValueStore = store,
        )

        val restored = BaGuideCatalogUiPreferencesStore.load(store).filterSortSnapshot

        assertEquals(BaGuideCatalogSortMode.ReleaseDateDesc, restored.studentSortMode)
        assertEquals(BaGuideCatalogSortMode.CnScoreAsc, restored.npcSatelliteSortMode)
        assertEquals("68:176", restored.studentSelectedFiltersRaw)
        assertEquals("12034:12035", restored.npcSatelliteSelectedFiltersRaw)
        assertEquals("", restored.searchQuery)
        assertEquals("", restored.selectedFiltersRaw)
    }
}

private class FakeCatalogUiPreferencesStore(
    vararg entries: Pair<String, String>,
) : BaGuideCatalogUiPreferencesKeyValueStore {
    private val values = entries.toMap().toMutableMap()

    override fun decodeString(
        key: String,
        defaultValue: String,
    ): String = values[key] ?: defaultValue

    override fun encode(
        key: String,
        value: String,
    ) {
        values[key] = value
    }
}
