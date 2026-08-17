package os.kei.ui.page.main.student.catalog

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The undo offer on BGM favourite removal.
 *
 * `BaGuideBgmUndoBlock` was written with its own string resources and an Undo action and then **never
 * called** — an affordance sitting in the tree, invisible. That is the bug this guards, and a source
 * contract is the right shape for it: the composable compiling is not evidence that anything renders it.
 *
 * The plan had this item filed as "a destructive menu item that should confirm through an action sheet",
 * per Apple's pull-down-button rule. Checked, and that premise was wrong on both halves: neither removal
 * path is a menu item — both are heart toggles on a track row — and an undo affordance already existed.
 * Apple treats undo as the *alternative* to confirming a reversible destructive action, not a complement,
 * so wiring the card is the correct resolution and an action sheet would have been the wrong one.
 */
class BaGuideBgmFavoriteUndoTest {
    @Test
    fun `the undo card has a caller`() {
        val callers =
            FAVOURITE_UI_SOURCES
                .map(::sourceFile)
                .count { source -> "BaGuideBgmUndoBlock(" in source }

        assertTrue(
            callers >= 1,
            "BaGuideBgmUndoBlock must be rendered by something; it spent its first life orphaned",
        )
    }

    @Test
    fun `only the list that loses the row offers the undo`() {
        val actions = sourceFile(PAGE_ACTIONS_SOURCE)

        // The favourites list deletes the row from the only screen that shows it, so it offers it back.
        assertContains(actions, "offerUndo = true")
        // The student BGM tab empties a heart and keeps the row, so tapping again is the undo. It toasts
        // and must NOT also stack an offer card on a screen where nothing disappeared.
        val toastPath = actions.substringAfter("onRemoveBgmFavoriteWithToast = ")
        assertTrue(
            "offerUndo" !in toastPath.substringBefore("},"),
            "The toast path keeps its row, so it must not also offer an undo",
        )
    }

    @Test
    fun `the removal captures the item before deleting it`() {
        val viewModel = sourceFile(VIEW_MODEL_SOURCE)
        val capture = viewModel.indexOf("favoriteBgms.value.firstOrNull { item ->")
        val delete = viewModel.indexOf("repository.removeBgmFavorite(normalizedAudioUrl)")

        assertTrue(capture >= 0, "The removal must look the item up to be able to restore it")
        assertTrue(delete >= 0, "The removal must still delete")
        assertTrue(
            capture < delete,
            "The item has to be captured BEFORE the delete; afterwards there is nothing to rebuild it from",
        )
    }

    @Test
    fun `restoring reuses the toggle rather than a second add path`() {
        val viewModel = sourceFile(VIEW_MODEL_SOURCE)
        val restore = viewModel.substringAfter("fun restorePendingBgmFavorite()")

        assertContains(
            restore.substringBefore("fun clearPendingBgmFavoriteUndo"),
            "repository.toggleBgmFavorite(item)",
        )
    }

    @Test
    fun `the offer expires on its own`() {
        val viewModel = sourceFile(VIEW_MODEL_SOURCE)

        // Without the timeout the card would sit on the page until the tab changed.
        assertContains(viewModel, "delay(BGM_FAVORITE_UNDO_WINDOW_MS)")
        assertContains(viewModel, "private const val BGM_FAVORITE_UNDO_WINDOW_MS")
    }
}

private val FAVOURITE_UI_SOURCES =
    listOf(
        "src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideFavoriteBgmMusicContent.kt",
        "src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideBgmFavoriteCards.kt",
    )

private const val PAGE_ACTIONS_SOURCE =
    "src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideCatalogPageActions.kt"

private const val VIEW_MODEL_SOURCE =
    "src/main/java/os/kei/ui/page/main/student/catalog/state/BaGuideCatalogViewModel.kt"

private fun sourceFile(relativePath: String): String {
    val file = File(relativePath)
    assertTrue(file.isFile, "Missing source: $relativePath (cwd ${System.getProperty("user.dir")})")
    return file.readText()
}
