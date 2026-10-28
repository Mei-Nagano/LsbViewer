package sb.linux.client.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

import sb.linux.client.domain.topic.PostEntry
/** 最后编辑信息：编辑人名字高亮可点击，跳转其个人主页 */
@Composable
internal fun EditInfoText(post: PostEntry, onEditUser: (Long) -> Unit) {
    val info = post.editInfo
    val name = post.editUserName
    if (post.editUserId > 0 && name.isNotBlank() && info.contains(name)) {
        val idx = info.indexOf(name)
        val annotated = buildAnnotatedString {
            append(info.substring(0, idx))
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                pushStringAnnotation("USER", post.editUserId.toString())
                append(name)
                pop()
            }
            append(info.substring(idx + name.length))
        }
        val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            annotated,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.pointerInput(annotated) {
                detectTapGestures { pos ->
                    val res = layoutResult.value ?: return@detectTapGestures
                    val offset = res.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("USER", offset, offset).firstOrNull()?.let {
                        it.item.toLongOrNull()?.let { uid -> onEditUser(uid) }
                    }
                }
            }
        )
    } else {
        Text(
            info,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
