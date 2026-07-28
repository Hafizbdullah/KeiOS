package os.kei.ui.page.main.student.catalog

import java.io.File
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class BaGuideCatalogFavoritesSynchronizationTest {
    @Test
    fun `each favorite write advances store signal`() {
        val before = BaGuideCatalogFavoritesStoreSignals.version.value

        BaGuideCatalogFavoritesStoreSignals.notifyChanged()
        BaGuideCatalogFavoritesStoreSignals.notifyChanged()

        assertEquals(before + 2L, BaGuideCatalogFavoritesStoreSignals.version.value)
    }

    @Test
    fun `catalog favorite writes notify long lived view model`() {
        val storeSource =
            File(
                "src/main/java/os/kei/ui/page/main/student/catalog/BaGuideCatalogStore.kt",
            ).readText()
        val viewModelSource =
            File(
                "src/main/java/os/kei/ui/page/main/student/catalog/state/BaGuideCatalogViewModel.kt",
            ).readText()

        assertContains(storeSource, "BaGuideCatalogFavoritesStoreSignals.notifyChanged()")
        assertContains(viewModelSource, "BaGuideCatalogFavoritesStoreSignals.version.collect")
        assertContains(viewModelSource, "repository.loadCatalogFavorites()")
    }
}
