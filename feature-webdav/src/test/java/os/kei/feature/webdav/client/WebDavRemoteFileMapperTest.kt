package os.kei.feature.webdav.client

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.ktor.MultiStatusItem
import at.bitfire.dav4jvm.ktor.PropStat
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetETag
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import at.bitfire.dav4jvm.property.webdav.ResourceType
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import os.kei.feature.webdav.model.WebDavRemoteFile

class WebDavRemoteFileMapperTest {
    @Test
    fun `member file response keeps remote metadata`() {
        val item = responseItem(
            properties = listOf(
                GetETag("\"etag-42\""),
                GetLastModified(Instant.parse("2026-07-16T00:00:00Z")),
                GetContentLength(42L),
                ResourceType(),
            ),
        )

        assertEquals(
            WebDavRemoteFile(
                href = "https://dav.example.test/KeiOS/snapshot.json",
                displayName = "snapshot.json",
                lastModified = "2026-07-16T00:00:00Z",
                contentLength = 42L,
                etag = "\"etag-42\"",
            ),
            item.toWebDavRemoteFileOrNull(),
        )
    }

    @Test
    fun `collection self and auxiliary responses stay out of file listings`() {
        val collection = responseItem(
            properties = listOf(ResourceType(setOf(WebDAV.Collection))),
        )
        val self = responseItem(relation = Response.HrefRelation.SELF)
        val extraProperty = MultiStatusItem.ExtraProperty(GetETag("\"sync-token\""))

        assertNull(collection.toWebDavRemoteFileOrNull())
        assertNull(self.toWebDavRemoteFileOrNull())
        assertNull(extraProperty.toWebDavRemoteFileOrNull())
    }

    private fun responseItem(
        relation: Response.HrefRelation = Response.HrefRelation.MEMBER,
        properties: List<Property> = emptyList(),
    ): MultiStatusItem.Response =
        MultiStatusItem.Response(
            response = Response(
                requestedUrl = Url("https://dav.example.test/KeiOS/"),
                href = Url("https://dav.example.test/KeiOS/snapshot.json"),
                status = HttpStatusCode.OK,
                propstat = listOf(PropStat(properties, HttpStatusCode.OK)),
            ),
            relation = relation,
        )
}
