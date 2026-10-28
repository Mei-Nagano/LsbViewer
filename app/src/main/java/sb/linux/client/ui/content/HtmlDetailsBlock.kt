package sb.linux.client.ui.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** `<details><summary>` 折叠条：从 HtmlContent 拆出的独立渲染单元，保持展开/折叠语义。 */
@Composable
internal fun DetailsSummaryBlock(
    block: ContentBlock,
    open: Boolean,
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier,
    onToggle: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onToggle),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (open) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                contentDescription = if (open) "折叠" else "展开",
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = style.copy(textAlign = block.textAlign ?: TextAlign.Start),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
