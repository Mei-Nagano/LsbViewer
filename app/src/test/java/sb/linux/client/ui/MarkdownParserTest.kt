package sb.linux.client.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun `parses headings lists quotes code and rules`() {
        val blocks = parseMarkdown(
            """
            # 标题
            > 引用
            - [x] 已完成
              - 子项
            ```
            code
            ```
            ---
            """.trimIndent(),
        )

        assertEquals(MdBlock.Heading(1, "标题"), blocks[0])
        assertEquals(MdBlock.Quote("引用"), blocks[1])
        assertEquals(MdBlock.Item(0, "☑", "已完成"), blocks[2])
        assertEquals(MdBlock.Item(1, null, "子项"), blocks[3])
        assertEquals(MdBlock.Code("code"), blocks[4])
        assertEquals(MdBlock.Rule, blocks[5])
    }

    @Test
    fun `normalizes table width and escaped separators`() {
        val table = parseMarkdown("| 名称 | 值 |\r\n| --- | :--: |\r\n| A\\|B | 1 |").single() as MdBlock.Table

        assertEquals(listOf(listOf("名称", "值"), listOf("A|B", "1")), table.rows)
    }

    @Test
    fun `supports chinese heading without a space and setext heading`() {
        val blocks = parseMarkdown("##中文标题\n\n普通段落\n===")

        assertTrue(blocks.contains(MdBlock.Heading(2, "中文标题")))
        assertTrue(blocks.contains(MdBlock.Heading(1, "普通段落")))
    }
}
