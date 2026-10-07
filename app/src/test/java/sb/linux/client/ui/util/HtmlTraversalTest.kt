/** HTML 正文节点遍历判定回归测试。 */
package sb.linux.client.ui.util

import org.jsoup.Jsoup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTraversalTest {
    @Test
    fun `inline image keeps surrounding text on inline parsing path`() {
        val paragraph = Jsoup.parseBodyFragment(
            "<p>图片前文字<img src='https://example.com/a.png'>图片后文字</p>",
        ).selectFirst("p")!!

        assertFalse(paragraph.hasNestedBlockChild())
        assertTrue(paragraph.ownText().contains("图片前文字"))
        assertTrue(paragraph.ownText().contains("图片后文字"))
    }

    @Test
    fun `nested paragraph still uses recursive block parsing path`() {
        val container = Jsoup.parseBodyFragment(
            "<div><p>第一段</p><p>第二段</p></div>",
        ).selectFirst("div")!!

        assertTrue(container.hasNestedBlockChild())
    }
}
