package sb.linux.client.ui

import androidx.compose.material3.Text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign



internal data class TableCellData(
    val text: String,
    val isHeader: Boolean,
    val annotated: AnnotatedString? = null,
    val imageUrls: List<String> = emptyList(),
    val textAlign: TextAlign? = null,
)

internal data class ContentBlock(
    val annotated: AnnotatedString? = null,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(), // 连续多图：合并为一个翻页块
    val embeddedVideo: EmbeddedVideo? = null,
    val isQuote: Boolean = false,
    val quoteDepth: Int = 0,
    val textAlign: TextAlign? = null,
    val detailsId: Int? = null,
    val isDetailsSummary: Boolean = false,
    val detailsOpenByDefault: Boolean = false,
    val foldPath: List<Int> = emptyList(),
    val anchorIds: List<String> = emptyList(),
    val isCode: Boolean = false,
    val isDivider: Boolean = false,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,     // 标题级别 1~4，用于排版字号缩放
    val isList: Boolean = false,   // 列表项：悬挂缩进展示（对应源站 <ul>/<ol>）
    val indent: Int = 0,           // 嵌套列表层级
    val tableRows: List<List<TableCellData>> = emptyList(),  // 表格：按行列结构渲染
)

