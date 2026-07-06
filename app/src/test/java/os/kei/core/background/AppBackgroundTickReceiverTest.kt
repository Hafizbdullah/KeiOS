package os.kei.core.background

import android.app.Application
import android.app.job.JobScheduler
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = BackgroundTickReceiverTestApp::class, sdk = [35])
class AppBackgroundTickReceiverTest {
    @Test
    fun `github tick enqueues scheduler job instead of running in receiver`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val scheduler = context.getSystemService(JobScheduler::class.java)
        scheduler.cancel(GITHUB_BACKGROUND_REFRESH_JOB_ID)

        AppBackgroundTickReceiver().onReceive(
            context,
            Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = AppBackgroundTickReceiver.ACTION_GITHUB_TICK
            },
        )

        val pendingJob = scheduler.getPendingJob(GITHUB_BACKGROUND_REFRESH_JOB_ID)

        assertNotNull(pendingJob)
        assertEquals(
            GitHubBackgroundRefreshJobService::class.java.name,
            pendingJob.service.className,
        )
        assertNotNull(pendingJob.requiredNetwork)
    }

    @Test
    fun `github job enqueue is idempotent while job is pending`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val scheduler = context.getSystemService(JobScheduler::class.java)
        scheduler.cancel(GITHUB_BACKGROUND_REFRESH_JOB_ID)

        assertTrue(GitHubBackgroundRefreshJobService.enqueueNow(context))
        assertTrue(GitHubBackgroundRefreshJobService.enqueueNow(context))

        val githubJobs = scheduler.allPendingJobs
            .filter { job -> job.id == GITHUB_BACKGROUND_REFRESH_JOB_ID }

        assertEquals(1, githubJobs.size)
        assertEquals(
            GitHubBackgroundRefreshJobService::class.java.name,
            githubJobs.single().service.className,
        )
    }

    @Test
    fun `github job info stores enqueue time for refresh history diagnostics`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val jobInfo = GitHubBackgroundRefreshJobService.buildJobInfo(
            context = context,
            enqueuedAtMillis = 123_456L,
        )

        assertEquals(
            123_456L,
            jobInfo.extras.getLong("github_background_enqueued_at_ms"),
        )
    }
}

class BackgroundTickReceiverTestApp : Application()
