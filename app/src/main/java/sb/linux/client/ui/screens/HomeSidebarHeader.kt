package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.TitleBadgeView

@Composable
internal fun HomeSidebarHeader(
    session: Session,
    onClose: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onLogin: () -> Unit,
) {
            // 顶部用户卡片：头像、账号信息与当前佩戴称号。
            val state = session.account.loginState
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.loggedIn) {
                            // 点击头像打开个人主页（原右侧圆形图标按钮已移除）
                            Avatar(
                                state.avatarUrl, 46,
                                Modifier.clickable { onClose(); onOpenUser(state.userId) }
                            )
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                // 称号跟在名字后面同一行：名字可省略号截断，称号先占位不被挤走
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Text(
                                        state.username.ifBlank { "饼友" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    state.titleBadge?.let { TitleBadgeView(it, small = true) }
                                }
                                Text(
                                    "${state.userGroup.ifBlank { "饼友" }} · 积分 ${state.points.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            Column(Modifier.weight(1f)) {
                                Text("访客", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "登录后可发帖、回帖与私信",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(
                                onClick = onLogin,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) { Text("登录", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
            // 单列 / 双列显示切换（作用于侧板的快捷功能与版块列表），放进容器显式分组；标题独占一行，按钮在其下方横排
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "侧栏显示",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { session.preferences.saveSidebarTwoColumns(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ViewList, "单列", Modifier.size(16.dp),
                                    tint = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "单列", style = MaterialTheme.typography.labelMedium,
                                    color = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { session.preferences.saveSidebarTwoColumns(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.GridView, "双列", Modifier.size(16.dp),
                                    tint = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "双列", style = MaterialTheme.typography.labelMedium,
                                    color = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

}

