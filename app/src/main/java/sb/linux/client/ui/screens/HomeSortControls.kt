package sb.linux.client.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
@Composable
internal fun ColumnScope.HomeSortControls(
    session: Session,
    combo: Int,
    tabIndex: Int,
    sortDrawerOpen: Boolean,
    onSortDrawerOpenChange: (Boolean) -> Unit,
    onSelectCombo: (Int, Int) -> Unit,
) {
                    // 组合过滤（全部/仅抽奖/仅发卡）移到上方，并作为（新评论/新帖子/精华）的抽屉容器
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 6.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                    ) {
                        // 折叠时底部留白与顶部（行内 top=7dp）一致，上下留白对称；展开时排序区自带底部间距
                        Column(Modifier.padding(bottom = if (sortDrawerOpen) 0.dp else 7.dp)) {
                            // 组合过滤行（质感样式）
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 4.dp, top = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("全部" to 0, "抽奖" to 1, "发卡" to 2, "红包" to 4, "申精" to 3).forEachIndexed { _, (label, c) ->
                                    val selected = combo == c
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable {
                                                onSelectCombo(c, if (listOf(0, 1, 2, 4, 3).indexOf(c) >
                                                    listOf(0, 1, 2, 4, 3).indexOf(session.homeBrowse.homeCombo)) 1 else -1)
                                                // 回到顶部由下方 LaunchedEffect(combo) 统一处理
                                            }
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                // 折叠 / 展开 排序抽屉按钮（状态持久化）
                                FilledTonalIconButton(
                                    onClick = {
                                        onSortDrawerOpenChange(!sortDrawerOpen)
                                        session.preferences.saveHomeSortDrawerOpen(sortDrawerOpen)
                                    },
                                    enabled = combo != 3,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        if (sortDrawerOpen) Icons.Filled.KeyboardArrowUp
                                        else Icons.Filled.KeyboardArrowDown,
                                        if (sortDrawerOpen) "收起排序" else "展开排序",
                                        Modifier.size(20.dp)
                                    )
                                }
                            }
                            // 排序 tab（新评论/新帖子/精华）做成下拉抽屉，横向可滑动
                            AnimatedVisibility(
                                visible = sortDrawerOpen && combo != 3,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SORTS.forEachIndexed { i, (_, label) ->
                                        val selected = tabIndex == i
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shadowElevation = if (selected) 2.dp else 0.dp,
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    session.homeBrowse.homeTabIndex = i; session.homeBrowse.homeCombo = 0
                                                    // 回到顶部由下方 LaunchedEffect(combo, tabIndex) 统一处理
                                                }
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
}
