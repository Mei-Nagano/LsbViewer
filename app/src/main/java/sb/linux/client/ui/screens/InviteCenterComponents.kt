package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.InviteCenter
import sb.linux.client.ui.theme.AppSpacing
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun InviteContent(
    data: InviteCenter,
    nav: NavHostController,
    copied: Boolean,
    onCopy: () -> Unit,
) {
    val spacing = LocalAppSpacing.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
        verticalArrangement = Arrangement.spacedBy(spacing.section),
    ) {
        ShareLinkCard(data, copied, onCopy, spacing)
        InviteStats(data, spacing)
        InviteUsers(data, nav, spacing)
    }
}

@Composable
private fun ShareLinkCard(
    data: InviteCenter,
    copied: Boolean,
    onCopy: () -> Unit,
    spacing: AppSpacing,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
            Text("我的分享链接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (data.desc.isNotBlank()) {
                Text(
                    data.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    Modifier.padding(start = spacing.contentGap, end = spacing.inline),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        data.link.ifBlank { "暂无邀请链接" },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(vertical = spacing.item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = onCopy, enabled = data.link.isNotBlank()) {
                        Text(if (copied) "已复制" else "复制")
                    }
                }
            }
            if (data.rule.isNotBlank()) {
                Text(
                    data.rule,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InviteStats(data: InviteCenter, spacing: AppSpacing) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
        StatCard("已邀请用户", data.invitedCount, spacing, Modifier.weight(1f))
        StatCard("首奖积分", rewardValue(data.firstReward), spacing, Modifier.weight(1f))
        StatCard("二奖积分", rewardValue(data.secondReward), spacing, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    spacing: AppSpacing,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.padding(vertical = spacing.contentGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.ifBlank { "-" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InviteUsers(data: InviteCenter, nav: NavHostController, spacing: AppSpacing) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing.item)) {
        Text("我邀请到的用户", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (data.users.isEmpty()) {
            Text(
                "暂时还没有用户通过你的分享链接注册",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    data.users.forEachIndexed { index, user ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .then(
                                    if (user.userId > 0) {
                                        Modifier.clickable { nav.navigate("user/${user.userId}") }
                                    } else Modifier
                                )
                                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                if (user.statusText.isNotBlank()) {
                                    Text(
                                        user.statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (user.userId > 0) {
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (index != data.users.lastIndex) {
                            HorizontalDivider(
                                Modifier.padding(start = spacing.pageHorizontal),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 源站文本形如「首奖积分（5）」：优先取括号内数值，取不到就原样显示。 */
private fun rewardValue(text: String): String {
    val match = Regex("[（(]([^）)]+)[）)]").find(text)
    return match?.groupValues?.get(1)?.trim().takeUnless { it.isNullOrBlank() } ?: text
}
