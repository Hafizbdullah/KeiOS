package os.kei.ui.page.main.github.section

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.R
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.ui.page.main.github.overviewLookupPillLabel
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(
    application = GitHubOverviewCopyTestApp::class,
    sdk = [35],
    qualifiers = "zh-rCN",
)
class GitHubOverviewCopyTest {
    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun apiLookupPillShowsModeOnly() {
        val config =
            GitHubLookupConfig(
                selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
                apiToken = "github_pat_example_token",
            )

        assertEquals("API", config.overviewLookupPillLabel(context))
    }

    @Test
    fun overviewTitleDescribesAllVersionTrackingSources() {
        assertEquals("版本追踪", context.getString(R.string.github_overview_title))
    }
}

class GitHubOverviewCopyTestApp : Application()
