package os.kei.feature.github.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitRepositoryIdentityParserTest {
    @Test
    fun `parses https and scp clone urls`() {
        val gitee = buildGitRepositoryTrackIdentity("https://gitee.com/demo/app.git")
        val gitlab = buildGitRepositoryTrackIdentity("git@gitlab.com:group/subgroup/app.git")
        val gitea = buildGitRepositoryTrackIdentity("https://gitea.com/gitea/tea")

        assertEquals(GitRepositoryPlatform.Gitee, gitee?.platform)
        assertEquals("gitee.com", gitee?.host)
        assertEquals("demo", gitee?.namespace)
        assertEquals("app", gitee?.repo)
        assertEquals("gitee.com/demo", gitee?.owner)
        assertEquals("gitee.com/demo/app", gitee?.displayName)
        assertEquals(GitRepositoryPlatform.GitLab, gitlab?.platform)
        assertEquals("group/subgroup", gitlab?.namespace)
        assertEquals("gitlab.com/group/subgroup", gitlab?.owner)
        assertEquals("app", gitlab?.repo)
        assertEquals(GitRepositoryPlatform.Gitea, gitea?.platform)
        assertEquals("gitea.com", gitea?.host)
        assertEquals("gitea", gitea?.namespace)
        assertEquals("tea", gitea?.repo)
    }

    @Test
    fun `trims repository page suffixes`() {
        val identity =
            buildGitRepositoryTrackIdentity("https://gitlab.com/group/subgroup/app/-/tree/main")

        assertEquals("gitlab.com", identity?.host)
        assertEquals("group/subgroup", identity?.namespace)
        assertEquals("app", identity?.repo)
        assertEquals("gitlab.com/group/subgroup/app", identity?.displayName)
    }

    @Test
    fun `normalizes supported schemes and generic hosts`() {
        val identity =
            buildGitRepositoryTrackIdentity("git+ssh://git@code.example.com/team/app.git/")

        assertEquals("ssh://git@code.example.com/team/app.git", identity?.url)
        assertEquals("code.example.com", identity?.host)
        assertEquals("team", identity?.namespace)
        assertEquals("app", identity?.repo)
        assertEquals(GitRepositoryPlatform.Generic, identity?.platform)
    }

    @Test
    fun `rejects unsupported and incomplete repository urls`() {
        assertNull(buildGitRepositoryTrackIdentity("ftp://example.com/demo/app.git"))
        assertNull(buildGitRepositoryTrackIdentity("https://example.com/app.git"))
        assertNull(buildGitRepositoryTrackIdentity("example.com/app"))
        assertNull(buildGitRepositoryTrackIdentity(""))
    }
}
