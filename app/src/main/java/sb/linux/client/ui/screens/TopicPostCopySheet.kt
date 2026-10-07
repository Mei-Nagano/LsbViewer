package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sb.linux.client.ui.*
import sb.linux.client.domain.topic.PostEntry
/** 长按正文功能菜单（MD3 风格，2.14）：
 * - 复制全文：把该楼层正文纯文本复制到剪贴板
 * - 自由复制：弹出全屏大窗口展示内容，长按自由选择复制（2.15）
 * - TA 的回复：查看发出该条内容的用户在本帖的所有回复
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCopySheet(
    post: PostEntry,
    onDismiss: () -> Unit,
    onCopied: () -> Unit,
    onViewUserReplies: (() -> Unit)? = null,
) {
    val plain = remember(post.contentHtml) { HtmlTextConversion.htmlToText(post.contentHtml) }
    var freeMode by remember(post) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    fun copyAll() {
        if (plain.isNotBlank()) clipboard.setText(AnnotatedString(plain))
        onCopied()
        onDismiss()
    }
    if (freeMode) {
        // 全屏大窗口自由复制（2.15）
        FreeCopyDialog(
            title = "${post.authorName}${if (post.floor > 0) " #${post.floor}" else ""}",
            plain = plain,
            onClose = { freeMode = false },
        )
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("正文操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
            Text(
                "${post.authorName}${if (post.floor > 0) " #${post.floor}" else " · 楼主"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            CopySheetItem(
                icon = Icons.Filled.CopyAll,
                title = "复制全文",
                subtitle = "复制整段正文到剪贴板"
            ) { copyAll() }
            CopySheetItem(
                icon = Icons.Filled.ContentPaste,
                title = "自由复制",
                subtitle = "全屏展示正文，长按选择任意部分复制"
            ) { freeMode = true }
            if (onViewUserReplies != null) {
                CopySheetItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "TA 的回复",
                    subtitle = "查看该用户在本帖的所有回复"
                ) {
                    onDismiss()
                    onViewUserReplies()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
