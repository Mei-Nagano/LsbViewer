package sb.linux.client.ui.content

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight



internal fun addSearchHighlights(source: AnnotatedString, query: String, color: Color): AnnotatedString {
    val target = query.trim()
    if (target.isEmpty() || source.text.isEmpty()) return source
    val ranges = Regex(Regex.escape(target), RegexOption.IGNORE_CASE).findAll(source.text).toList()
    if (ranges.isEmpty()) return source
    return buildAnnotatedString {
        append(source)
        ranges.forEach { match ->
            addStyle(
                SpanStyle(background = color, fontWeight = FontWeight.SemiBold),
                match.range.first,
                match.range.last + 1,
            )
        }
    }
}

/**
 * 连续多图：占用一个图片位置，左右滑动翻页，右下角显示页码指示。
 * 点击任意图片进入全屏放大查看。
 * 容器高度：记住每张图已加载出的宽高比，切换时保持上一张的比例直到新图加载成功，
 * 避免容器先跳回默认比例再突变（边框突然变大又恢复）。
 */
