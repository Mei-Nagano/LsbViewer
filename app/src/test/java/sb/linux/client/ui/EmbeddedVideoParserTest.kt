package sb.linux.client.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbeddedVideoParserTest {
    @Test
    fun `youtube player uses browser compatible user agent`() {
        val webViewUa = "Mozilla/5.0 (Linux; Android 15; Device; wv) " +
            "AppleWebKit/537.36 Version/4.0 Chrome/137.0 Mobile Safari/537.36"

        assertEquals(
            "Mozilla/5.0 (Linux; Android 15; Device) " +
                "AppleWebKit/537.36 Chrome/137.0 Mobile Safari/537.36",
            browserCompatibleUserAgent(webViewUa),
        )
    }

    @Test
    fun `parses current bilibili player output`() {
        val blocks = parseHtmlToBlocks(
            """
            <div class="nb-editor-post-content"><div class="nb-editor-bilibili">
              <iframe src="https://player.bilibili.com/player.html?isOutside=true&amp;page=1&amp;bvid=BV1e6YXzrE4f"></iframe>
            </div></div>
            """.trimIndent(),
            Color.Blue,
            Color.Black,
        )

        val video = blocks.single().embeddedVideo!!
        assertEquals(EmbeddedVideoPlatform.BILIBILI, video.platform)
        assertEquals("https://www.bilibili.com/video/BV1e6YXzrE4f", video.sourceUrl)
    }

    @Test
    fun `parses current douyin player output`() {
        val blocks = parseHtmlToBlocks(
            """
            <div class="nb-editor-douyin">
              <iframe src="https://open.douyin.com/player/video?vid=7682252376552023985&amp;autoplay=0"></iframe>
            </div>
            """.trimIndent(),
            Color.Blue,
            Color.Black,
        )

        val video = blocks.single().embeddedVideo!!
        assertEquals(EmbeddedVideoPlatform.DOUYIN, video.platform)
        assertEquals("https://www.douyin.com/video/7682252376552023985", video.sourceUrl)
    }

    @Test
    fun `parses youtube and privacy enhanced players`() {
        listOf(
            "https://www.youtube.com/embed/dQw4w9WgXcQ?start=12",
            "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
        ).forEach { source ->
            val blocks = parseHtmlToBlocks(
                "<iframe src=\"$source\"></iframe>",
                Color.Blue,
                Color.Black,
            )

            val video = blocks.single().embeddedVideo!!
            assertEquals(EmbeddedVideoPlatform.YOUTUBE, video.platform)
            assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", video.sourceUrl)
            val origin = "https%3A%2F%2Flinux.sb"
            val separator = if (source.contains('?')) '&' else '?'
            assertEquals(
                "$source${separator}origin=$origin&widget_referrer=$origin&playsinline=1",
                video.playerUrl,
            )
        }
    }

    @Test
    fun `rejects untrusted iframe`() {
        val blocks = parseHtmlToBlocks(
            "<iframe src=\"https://example.com/player\"></iframe>",
            Color.Blue,
            Color.Black,
        )

        assertNull(blocks.singleOrNull()?.embeddedVideo)
    }
}
