package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppMenuDefaults

/** 设置项使用的根目的地候选；发帖是动作，不在这里列出。 */
private val BOTTOM_OPTIONS = listOf(
    "home" to "社区",
    "forums" to "版块",
    "topicCollections" to "淘帖",
    "messages" to "消息",
    "me" to "我的",
)

internal fun LazyListScope.themeLayoutSettings(session: Session) {
    // ---------- 布局：跟随窗口 / 始终单内容栏 ----------
    item { GroupLabel("布局") }
    item {
        GroupCard {
            Text(
                "跟随窗口时，宽窗可并排显示列表与详情；主导航仍按窗口宽度在底部导航与侧边 rail 之间切换。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 4.dp),
            ) {
                SegmentedButton(
                    selected = !session.singleContentColumn,
                    onClick = { session.preferences.saveTabletMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("跟随窗口") }
                SegmentedButton(
                    selected = session.singleContentColumn,
                    onClick = { session.preferences.saveTabletMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("始终单内容栏") }
            }
        }
    }

    // ---------- 主导航 ----------
    item { GroupLabel("主导航") }
    item {
        GroupCard {
            Text(
                "新版使用标准 MD3 底部导航或侧边 rail，不再渲染液态玻璃效果。发帖改为社区页的浮动按钮，" +
                    "不再占用导航目的地。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp),
            )
            Text(
                "根目的地与顺序（2—5 项）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = AppMenuDefaults.contentHorizontal, top = 14.dp, bottom = 4.dp),
            )
            Text(
                "可选社区、版块、淘帖、消息和我的；至少保留两项，最多五项。" +
                    "仅两项时使用顶部标签栏。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 4.dp),
            )
            val selectedBottomItems = session.bottomBarItems
            (selectedBottomItems.mapNotNull { id -> BOTTOM_OPTIONS.firstOrNull { it.first == id } } +
                BOTTOM_OPTIONS.filter { it.first !in selectedBottomItems }).forEach { (id, label) ->
                val index = selectedBottomItems.indexOf(id)
                val checked = index >= 0
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        // 少于两项不可再取消；达到五项后不再新增
                        enabled = if (checked) selectedBottomItems.size > 2 else selectedBottomItems.size < 5,
                        onCheckedChange = { wantChecked ->
                            when {
                                wantChecked && selectedBottomItems.size < 5 ->
                                    session.preferences.saveBottomBarItems(selectedBottomItems + id)
                                !wantChecked && selectedBottomItems.size > 2 ->
                                    session.preferences.saveBottomBarItems(selectedBottomItems - id)
                            }
                        },
                    )
                    Text(label, Modifier.weight(1f))
                    TextButton(enabled = index > 0, onClick = {
                        val changed = selectedBottomItems.toMutableList()
                        java.util.Collections.swap(changed, index, index - 1)
                        session.preferences.saveBottomBarItems(changed)
                    }) { Text("↑") }
                    TextButton(enabled = index in 0 until selectedBottomItems.lastIndex, onClick = {
                        val changed = selectedBottomItems.toMutableList()
                        java.util.Collections.swap(changed, index, index + 1)
                        session.preferences.saveBottomBarItems(changed)
                    }) { Text("↓") }
                }
            }
        }
    }

    // 旧的「评论底栏样式」开关已移除：阅读操作栏统一为标准 MD3 表面，不再渲染液态玻璃。
    item {
        GroupCard {
            Text(
                "阅读操作栏使用标准 MD3 表面：伪输入条、点赞与收藏固定在键盘与系统手势区之上。" +
                    "旧版液态玻璃样式不再提供。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp),
            )
        }
    }
}
