package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session

internal fun LazyListScope.themeLayoutSettings(session: Session) {
// ---------- 布局：平板模式（从常规设置迁入） ----------
item { GroupLabel("布局") }
item {
    GroupCard {
        SwitchRow(
            title = "平板模式",
            subtitle = "宽屏设备使用左右双栏：导航栏居左，主页/设置占左栏，帖子与详情占右栏。需屏幕宽度 ≥ 600dp 生效",
            checked = session.tabletMode,
            onCheckedChange = { session.preferences.saveTabletMode(it) },
        )
    }
}

// ---------- 底栏样式：经典通栏 / 液态玻璃悬浮胶囊 ----------
item { GroupLabel("底栏样式") }
item {
    GroupCard {
        Text(
            "经典为普通通栏底栏；液态玻璃为居中悬浮胶囊，实时折射身后滚动内容（Android 12 以下退化为半透明条）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            SegmentedButton(
                selected = session.bottomBarStyle == 0,
                onClick = { session.preferences.saveBottomBarStyle(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("经典") }
            SegmentedButton(
                selected = session.bottomBarStyle == 1,
                onClick = { session.preferences.saveBottomBarStyle(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("液态玻璃") }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp))
        Text(
            "底栏项目与顺序",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
        )
        Text(
            "可选首页、版块、发帖和我的；至少保留两项。箭头用于调整显示顺序。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )
        val bottomOptions = listOf("home" to "首页", "topicCollections" to "淘帖", "directMessages" to "私信", "newTopic" to "发帖", "me" to "我的")
        val selectedBottomItems = session.bottomBarItems
        (selectedBottomItems.mapNotNull { id -> bottomOptions.firstOrNull { it.first == id } } +
            bottomOptions.filter { it.first !in selectedBottomItems }).forEach { (id, label) ->
            val index = selectedBottomItems.indexOf(id)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = index >= 0,
                    enabled = index < 0 || selectedBottomItems.size > 2,
                    onCheckedChange = { checked ->
                        if (checked) session.preferences.saveBottomBarItems(selectedBottomItems + id)
                        else if (selectedBottomItems.size > 2) session.preferences.saveBottomBarItems(selectedBottomItems - id)
                    },
                )
                Text(label, Modifier.weight(1f))
                TextButton(enabled = index > 0, onClick = {
                    val changed = selectedBottomItems.toMutableList()
                    java.util.Collections.swap(changed, index, index - 1)
                    session.preferences.saveBottomBarItems(changed)
                }) { Text("↑") }
                TextButton(enabled = index >= 0 && index < selectedBottomItems.lastIndex, onClick = {
                    val changed = selectedBottomItems.toMutableList()
                    java.util.Collections.swap(changed, index, index + 1)
                    session.preferences.saveBottomBarItems(changed)
                }) { Text("↓") }
            }
        }
        // 评论底栏（帖子内底部快捷回复栏）：与主导航底栏独立的样式选择
        Text(
            "评论底栏",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 2.dp)
        )
        Text(
            "帖子内底部的回复/点赞/收藏快捷栏；液态玻璃为悬浮胶囊，实时折射身后滚动的帖子内容（Android 12 以下退化为半透明条）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            SegmentedButton(
                selected = session.replyBarStyle == 0,
                onClick = { session.preferences.saveReplyBarStyle(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("经典") }
            SegmentedButton(
                selected = session.replyBarStyle == 1,
                onClick = { session.preferences.saveReplyBarStyle(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("液态玻璃") }
        }
    }
}

}
