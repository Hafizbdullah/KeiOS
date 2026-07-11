package os.kei.feature.github.fixture

/** Captured from iebb/mithka Releases API and Atom feed on 2026-07-11. */
internal object MithkaReleaseCorpus {
    const val owner = "iebb"
    const val repo = "mithka"
    const val stableTag = "v0.3.0"
    const val latestPreReleaseTag = "v0.3.0-master.26071104.7880c18"
    const val stableVersionCode = 26_071_014L
    const val olderPreReleaseVersionCode = 26_071_018L
    const val latestPreReleaseVersionCode = 26_071_104L

    val atomXml: String by lazy {
        loadResource("/github/mithka-releases-2026-07-11.atom")
    }

    val apiJson: String by lazy {
        loadResource("/github/mithka-releases-2026-07-11.json")
    }

    private fun loadResource(path: String): String {
        return checkNotNull(MithkaReleaseCorpus::class.java.getResource(path)) {
            "Missing release corpus resource: $path"
        }.readText()
    }
}
