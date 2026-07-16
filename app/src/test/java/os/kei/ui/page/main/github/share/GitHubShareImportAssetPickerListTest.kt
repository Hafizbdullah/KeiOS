package os.kei.ui.page.main.github.share

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = GitHubShareImportAssetPickerListTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class GitHubShareImportAssetPickerListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disabledSelectionDisablesTheChoiceIndicator() {
        var selectCount = 0
        setAssetPickerContent(selectionEnabled = false) { selectCount++ }

        composeRule
            .onNode(radioButtonMatcher())
            .assertIsSelected()
            .assertIsNotEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(0, selectCount) }
    }

    @Test
    fun enabledSelectionInvokesTheChoiceIndicatorOnce() {
        var selectCount = 0
        setAssetPickerContent(selectionEnabled = true) { selectCount++ }

        composeRule
            .onNode(radioButtonMatcher())
            .assertIsSelected()
            .assertIsEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, selectCount) }
    }

    private fun setAssetPickerContent(
        selectionEnabled: Boolean,
        onSelect: () -> Unit,
    ) {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                GitHubShareImportAssetPickerList(
                    assets = listOf(asset),
                    supportedAbis = listOf("arm64-v8a"),
                    selectedIndex = 0,
                    selectionEnabled = selectionEnabled,
                    onSelect = { onSelect() },
                )
            }
        }
    }

    private fun radioButtonMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.RadioButton,
        )

    private companion object {
        val asset =
            GitHubReleaseAssetFile(
                name = "KeiOS-arm64-v8a.apk",
                downloadUrl = "https://example.invalid/KeiOS-arm64-v8a.apk",
                sizeBytes = 1024L,
                downloadCount = 1,
            )
    }
}

class GitHubShareImportAssetPickerListTestApp : Application()
