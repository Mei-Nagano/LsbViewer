package sb.linux.client.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.components.navigation.AppTopBar
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    session: Session,
    nav: NavHostController,
    current: HomeTabState,
    masterNav: NavHostController?,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchField: String,
    onSearchFieldChange: (String) -> Unit,
    searchFieldMenu: Boolean,
    onSearchFieldMenuChange: (Boolean) -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    homeInfinite: Boolean,
    listState: LazyListState,
    scope: CoroutineScope,
    searchFocus: FocusRequester,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitSearch: () -> Unit,
) {
                    // 顶栏：正常态 / 搜索态 二选一
                    if (searchActive) {
                        // 搜索态：左侧搜索范围选择 + 搜索输入框 + 右侧搜索按钮；
                        // 范围/关键词在顶栏一次选好，跳转搜索页直接出结果（不再进二级页面才能搜）
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                TextButton(
                                    onClick = { onSearchFieldMenuChange(true) },
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(
                                        fieldLabelOf(searchField),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        "选择搜索范围",
                                        Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = searchFieldMenu,
                                    onDismissRequest = { onSearchFieldMenuChange(false) }
                                ) {
                                    listOf(
                                        "all" to "全部内容",
                                        "title" to "标题",
                                        "body" to "主题正文",
                                        "reply" to "回帖",
                                        "user" to "用户",
                                    ).forEach { (v, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            trailingIcon = if (searchField == v) {
                                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                            } else null,
                                            onClick = { onSearchFieldChange(v); onSearchFieldMenuChange(false) }
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "搜索关键词",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { onSearchQueryChange(it) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Search
                                        ),
                                        keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp)
                                            .focusRequester(searchFocus),
                                    )
                                }
                            }
                            IconButton(onClick = { onSubmitSearch() }) {
                                Icon(Icons.Filled.Search, "搜索")
                            }
                        }
                    } else {
                        AppTopBar(
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            title = "LINUX SB",
                            navigationIcon = {
                                // 平板双栏：侧边栏按钮已移到左侧导航栏顶部，顶栏不再重复展示
                                if (masterNav == null) {
                                    IconButton(onClick = { onOpenDrawer() }) {
                                        Icon(Icons.Filled.Menu, "侧板")
                                    }
                                }
                            },
                            actions = {
                                // 通知：源站有未读时右上角显示小红点（未读数来自首页 .notify-badge）
                                Box {
                                    IconButton(
                                        onClick = { nav.navigate("notifications") }
                                    ) {
                                        Icon(Icons.Filled.Notifications, "通知")
                                    }
                                    if (session.account.notifUnreadCount > 0) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 8.dp, end = 3.dp)
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }
                                // 搜索：点击展开为顶栏搜索框
                                IconButton(onClick = { onSearchActiveChange(true) }) {
                                    Icon(Icons.Filled.Search, "搜索")
                                }
                                // 三点菜单：刷新 / 切换滚动模式（刷新也可下拉触发）
                                Box {
                                    IconButton(onClick = { onMenuOpenChange(true) }) {
                                        Icon(Icons.Filled.MoreVert, "菜单")
                                    }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                                        // 发帖：与源站网页端一致（支持帖子类型等表单附加字段）
                                        DropdownMenuItem(
                                            text = { Text("发帖") },
                                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                            onClick = { onMenuOpenChange(false); nav.navigate("newTopic") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("刷新") },
                                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                            onClick = { onMenuOpenChange(false); onRefresh() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("屏蔽词管理") },
                                            leadingIcon = { Icon(Icons.Filled.Tune, null) },
                                            onClick = { onMenuOpenChange(false); nav.navigate("blockWords") }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (homeInfinite) "切换为翻页模式" else "切换为无限滚动")
                                            },
                                            leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                                            onClick = {
                                                onMenuOpenChange(false)
                                                session.preferences.saveInfiniteScroll(!homeInfinite)
                                                if (homeInfinite) {
                                                    // 离开无限滚动 → 切到翻页模式：回到本地第一页
                                                    current.page = 1
                                                } else {
                                                    // 离开翻页模式 → 切到无限滚动：页码回到已加载的源站页
                                                    current.page = current.sourcePage
                                                    current.totalPages = current.sourceTotalPages
                                                }
                                                scope.launch { listState.scrollToItem(0) }
                                                session.showToast(
                                                    if (homeInfinite) "已切换为翻页模式" else "已切换为无限滚动"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
}
