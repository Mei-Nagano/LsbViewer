package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.theme.LocalAppSpacing

/** 卡片内的空态/提示：不占满全屏，可放进滚动内容中间。 */
@Composable
internal fun GachaEmptyCard(text: String) {
    val spacing = LocalAppSpacing.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = spacing.section, horizontal = spacing.pageHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Inbox, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 未登录引导：复用统一的登录占位版式，不再单独维护一套图标与按钮。 */
@Composable
internal fun GachaLoginPrompt(onLogin: () -> Unit) {
    LoginRequiredBox(
        title = "登录后可进入称号中心",
        subtitle = "登录后查看已获得的称号与抽取记录",
        onLogin = onLogin,
    )
}
