package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData

@Composable
internal fun HomeSidebarHeader(
    session: Session,
    onClose: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onLogin: () -> Unit,
) {
    val state = session.account.loginState
    if (state.loggedIn) {
        UserInfoCard(
            data = state.toUserInfoCardData(), compact = true,
            modifier = Modifier.padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 12.dp),
            onProfile = { onClose(); onOpenUser(state.userId) },
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 12.dp),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(Modifier.padding(LocalAppSpacing.current.pageHorizontal), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("访客", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("登录后可发帖、回帖与私信", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onLogin) { Text("登录") }
            }
        }
    }
            // 单列 / 双列显示切换（作用于侧板的快捷功能与版块列表），放进容器显式分组；标题独占一行，按钮在其下方横排
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.large,
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

