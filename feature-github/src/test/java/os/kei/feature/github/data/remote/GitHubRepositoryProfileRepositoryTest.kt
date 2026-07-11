package os.kei.feature.github.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import os.kei.feature.github.model.GitHubAtomFeed
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubProfileDepth
import os.kei.feature.github.model.GitHubReleaseSignalSource
import os.kei.feature.github.model.GitHubReleaseVersionSignals
import os.kei.feature.github.model.GitHubRepositoryProfileAvailabilityStatus
import os.kei.feature.github.model.GitHubRepositoryProfileCapability
import os.kei.feature.github.model.GitHubRepositoryProfileConfidence
import os.kei.feature.github.model.GitHubRepositoryProfilePurpose
import os.kei.feature.github.model.GitHubRepositoryProfileSource
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubRepositoryProfileRepositoryTest {
    @Test
    fun `version check fast requests only core repository sources`() {
        MockWebServer().use { server ->
            server.dispatcher = profileDispatcher()
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = "https://github.test"
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig(profileDepth = GitHubProfileDepth.Basic),
                    releaseSnapshot = releaseSnapshot(
                        strategyId = "atom_feed",
                        source = GitHubReleaseSignalSource.LatestRedirect
                    )
                )
                )
            }

            val paths = server.takeRequestPaths()
            assertContains(paths, "/repos/demo/app")
            assertFalse(paths.any { it.contains("/actions/") })
            assertFalse(paths.any { it.contains("/community/profile") })
            assertFalse(paths.any { it.contains("/traffic/") })
            assertFalse(paths.any { it.contains("/dependabot/") })
            assertFalse(paths.any { it.contains("/code-scanning/") })
            assertEquals(GitHubRepositoryProfilePurpose.VersionCheckFast, profile.purpose)
            assertTrue(
                profile.capabilities.containsAll(
                    setOf(
                        GitHubRepositoryProfileCapability.RepositoryCore,
                        GitHubRepositoryProfileCapability.ReleaseSignals,
                        GitHubRepositoryProfileCapability.LocalFit
                    )
                )
            )
            assertFalse(GitHubRepositoryProfileCapability.Actions in profile.capabilities)
            assertTrue(
                profile.sourceAvailability.any {
                    it.source == GitHubRepositoryProfileSource.HtmlLatestReleaseRedirect
                }
            )
        }
    }

    @Test
    fun `health card requests actions and community while skipping deep endpoints`() {
        MockWebServer().use { server ->
            server.dispatcher = profileDispatcher()
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig(profileDepth = GitHubProfileDepth.Deep),
                    purpose = GitHubRepositoryProfilePurpose.HealthCard
                )
                )
            }

            val paths = server.takeRequestPaths()
            assertContains(paths, "/repos/demo/app/actions/runs?per_page=12")
            assertContains(paths, "/repos/demo/app/actions/artifacts?per_page=30")
            assertContains(paths, "/repos/demo/app/community/profile")
            assertFalse(paths.any { it.contains("/traffic/") })
            assertFalse(paths.any { it.contains("/dependabot/") })
            assertTrue(GitHubRepositoryProfileCapability.Actions in profile.capabilities)
            assertTrue(GitHubRepositoryProfileCapability.Community in profile.capabilities)
            assertFalse(GitHubRepositoryProfileCapability.Security in profile.capabilities)
        }
    }

    @Test
    fun `api failure falls back to html repository page with low confidence fields`() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.path) {
                        "/repos/demo/app" -> MockResponse()
                            .setResponseCode(500)
                            .setBody("""{"message":"temporary"}""")

                        "/demo/app" -> htmlResponse(
                            """
                                <html>
                                  <body>
                                    <div>This repository has been archived by the owner.</div>
                                    <a href="/topics/android">Android</a>
                                    <div id="readme">README.md</div>
                                  </body>
                                </html>
                            """.trimIndent()
                        )

                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig()
                )
                )
            }

            val archived = profile.lifecycle.archived ?: error("archived field should exist")
            assertTrue(archived.value)
            assertEquals(
                GitHubRepositoryProfileConfidence.Low,
                archived.confidence
            )
            assertEquals(listOf("android"), profile.identity.topics?.value)
            assertTrue(
                profile.sourceAvailability.any {
                    it.source == GitHubRepositoryProfileSource.GitHubApiRepository &&
                            it.status == GitHubRepositoryProfileAvailabilityStatus.Failed &&
                            it.required
                }
            )
        }
    }

    @Test
    fun `detail full deep profile collects enhanced endpoints and keeps partial failures`() {
        MockWebServer().use { server ->
            server.dispatcher = profileDispatcher(fork = true, codeScanningCode = 403)
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig(profileDepth = GitHubProfileDepth.Deep),
                    purpose = GitHubRepositoryProfilePurpose.DetailFull
                )
                )
            }

            val paths = server.takeRequestPaths()
            assertContains(paths, "/demo/app")
            assertContains(paths, "/repos/demo/app/traffic/views")
            assertContains(paths, "/repos/demo/app/traffic/clones")
            assertContains(paths, "/repos/upstream/app/compare/main...demo:main")
            assertContains(paths, "/repos/demo/app/dependabot/alerts?state=open&per_page=100")
            assertContains(paths, "/repos/demo/app/code-scanning/alerts?state=open&per_page=100")
            assertEquals(11, profile.traffic.viewCount?.value)
            assertEquals(1, profile.security.openDependabotAlertsCount?.value)
            assertFalse(profile.security.codeScanningAvailable?.value == true)
            assertTrue(
                profile.sourceAvailability.any {
                    it.source == GitHubRepositoryProfileSource.CodeScanningAlertsApi &&
                            it.status == GitHubRepositoryProfileAvailabilityStatus.Failed
                }
            )
            assertTrue(GitHubRepositoryProfileCapability.Security in profile.capabilities)
        }
    }

    @Test
    fun `manual deep refresh follows basic or deep profile depth`() {
        MockWebServer().use { server ->
            server.dispatcher = profileDispatcher(fork = true)
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig(profileDepth = GitHubProfileDepth.Basic),
                    purpose = GitHubRepositoryProfilePurpose.ManualDeepRefresh
                )
                )
            }

            val paths = server.takeRequestPaths()
            assertContains(paths, "/demo/app")
            assertFalse(paths.any { it.contains("/traffic/") })
            assertFalse(paths.any { it.contains("/dependabot/") })
            assertEquals(GitHubRepositoryProfilePurpose.ManualDeepRefresh, profile.purpose)
            assertFalse(GitHubRepositoryProfileCapability.Security in profile.capabilities)
        }

        MockWebServer().use { server ->
            server.dispatcher = profileDispatcher(fork = true)
            val repository = GitHubRepositoryProfileRepository(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                htmlBaseUrl = server.url("/").toString().trimEnd('/')
            )

            val profile = runBlocking {
                repository.fetchProfile(
                GitHubRepositoryProfileRequest(
                    owner = "demo",
                    repo = "app",
                    lookupConfig = GitHubLookupConfig(profileDepth = GitHubProfileDepth.Deep),
                    purpose = GitHubRepositoryProfilePurpose.ManualDeepRefresh
                )
                )
            }

            val paths = server.takeRequestPaths()
            assertContains(paths, "/repos/demo/app/traffic/views")
            assertContains(paths, "/repos/demo/app/dependabot/alerts?state=open&per_page=100")
            assertTrue(GitHubRepositoryProfileCapability.Security in profile.capabilities)
        }
    }

    private fun releaseSnapshot(
        strategyId: String,
        source: GitHubReleaseSignalSource
    ): GitHubRepositoryReleaseSnapshot {
        return GitHubRepositoryReleaseSnapshot(
            strategyId = strategyId,
            feed = GitHubAtomFeed(title = "demo/app releases"),
            latestStable = releaseSignal("v1.2.0", source),
            hasStableRelease = true,
            latestPreRelease = releaseSignal("v1.3.0-beta1", source)
        )
    }

    private fun releaseSignal(
        tag: String,
        source: GitHubReleaseSignalSource
    ): GitHubReleaseVersionSignals {
        return GitHubReleaseVersionSignals(
            displayVersion = tag,
            rawTag = tag,
            rawName = tag,
            updatedAtMillis = 1_700_000_000_000L,
            source = source,
            authorName = "maintainer"
        )
    }

    private fun apiRepositoryJson(
        archived: Boolean,
        fork: Boolean,
        disabled: Boolean = false,
        includeLicense: Boolean = true,
        topics: List<String> = listOf("android", "compose")
    ): String {
        val topicsJson = topics.joinToString(",") { "\"$it\"" }
        val licenseJson = if (includeLicense) {
            """"license": {"name": "Apache License 2.0", "spdx_id": "Apache-2.0"},"""
        } else {
            """"license": null,"""
        }
        return """
            {
              "name": "app",
              "full_name": "demo/app",
              "html_url": "https://github.com/demo/app",
              "default_branch": "main",
              "visibility": "public",
              "private": false,
              "archived": $archived,
              "disabled": $disabled,
              "fork": $fork,
              "mirror_url": null,
              "created_at": "2020-01-01T00:00:00Z",
              "updated_at": "2024-01-01T00:00:00Z",
              "pushed_at": "2023-10-01T00:00:00Z",
              "stargazers_count": 1200,
              "forks_count": 45,
              "watchers_count": 1200,
              "subscribers_count": 80,
              "open_issues_count": 12,
              "size": 2048,
              "topics": [$topicsJson],
              $licenseJson
              "owner": {
                "login": "demo",
                "type": "Organization",
                "avatar_url": "https://avatars.githubusercontent.com/u/42?v=4"
              },
              "parent": {
                "full_name": "upstream/app",
                "html_url": "https://github.com/upstream/app",
                "archived": true,
                "disabled": false,
                "pushed_at": "2022-01-01T00:00:00Z",
                "default_branch": "main"
              }
            }
        """.trimIndent()
    }

    private fun profileDispatcher(
        fork: Boolean = false,
        codeScanningCode: Int = 200
    ): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/demo/app" -> jsonResponse(
                        apiRepositoryJson(
                            archived = false,
                            fork = fork
                        )
                    )

                    "/demo/app" -> htmlResponse(
                        """
                            <html>
                              <body>
                                <a href="/topics/android">Android</a>
                                <a href="/demo/app/tree/main">main</a>
                                <div id="readme">README.md</div>
                              </body>
                            </html>
                        """.trimIndent()
                    )

                    "/repos/demo/app/actions/runs?per_page=12" -> jsonResponse("""{"workflow_runs": []}""")
                    "/repos/demo/app/actions/artifacts?per_page=30" -> jsonResponse("""{"artifacts": []}""")
                    "/repos/demo/app/community/profile" -> jsonResponse(
                        """{"health_percentage": 80, "files": {"readme": {"name": "README.md"}}}"""
                    )

                    "/repos/demo/app/traffic/views" -> jsonResponse("""{"count": 11, "uniques": 5, "views": []}""")
                    "/repos/demo/app/traffic/clones" -> jsonResponse("""{"count": 3, "uniques": 2, "clones": []}""")
                    "/repos/upstream/app/compare/main...demo:main" -> jsonResponse(
                        """{"ahead_by": 1, "behind_by": 2, "status": "behind", "total_commits": 3}"""
                    )
                    "/repos/demo/app/dependabot/alerts?state=open&per_page=100" -> jsonResponse("""[{"number": 1}]""")
                    "/repos/demo/app/code-scanning/alerts?state=open&per_page=100" -> if (codeScanningCode == 200) {
                        jsonResponse("[]")
                    } else {
                        MockResponse().setResponseCode(codeScanningCode)
                            .setBody("""{"message":"forbidden"}""")
                    }

                    else -> MockResponse().setResponseCode(404).setBody("""{"message":"missing"}""")
                }
            }
        }
    }

    private fun jsonResponse(body: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }

    private fun htmlResponse(body: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html")
            .setBody(body)
    }

    private fun MockWebServer.takeRequestPaths(): List<String> {
        return buildList {
            repeat(requestCount) {
                add(takeRequest().path.orEmpty())
            }
        }
    }

    private companion object {
        const val FETCHED_AT = 1_700_000_100_000L
    }
}
