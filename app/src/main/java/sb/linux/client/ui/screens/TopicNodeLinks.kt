package sb.linux.client.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.NodeLink
import sb.linux.client.ui.LocalLinkHandler

@Composable
internal fun TopicNodeLinks(nodes: List<NodeLink>, loggedIn: Boolean, onManage: () -> Unit) {
    val open = LocalLinkHandler.current
    if (nodes.isEmpty() && !loggedIn) return
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        nodes.forEach { node -> AssistChip(onClick = { open?.invoke(node.url) }, label = { Text(node.title) }) }
        if (loggedIn) TextButton(onClick = onManage) { Text("管理节点") }
    }
}
