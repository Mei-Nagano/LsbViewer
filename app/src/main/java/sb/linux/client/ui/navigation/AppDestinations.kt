package sb.linux.client.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 根目的地模型（MD3_UI_REDESIGN §4.1）。
 *
 * 显示名称只属于这一层：route 仍是兼容契约，不作为显示名或持久化主键。
 *「发帖」是动作，不进入本模型；旧 newTopic 值保留在偏好里但不渲染为目的地。
 */
internal data class AppDestination(
    val id: String,
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

internal object AppDestinations {
    val home = AppDestination("home", "社区", "home", Icons.Filled.Home, Icons.Outlined.Home)
    val collections = AppDestination(
        "topicCollections", "淘帖", "topicCollectionsRoot",
        Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark,
    )
    val messages = AppDestination(
        "messages", "消息", "messages",
        Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat,
    )
    val me = AppDestination("me", "我的", "me", Icons.Filled.Person, Icons.Outlined.Person)
    val forums = AppDestination("forums", "版块", "forums", Icons.Filled.ViewList, Icons.Outlined.ViewList)

    /** 新安装默认四项：社区 / 淘帖 / 消息 / 我的。 */
    val defaults: List<AppDestination> = listOf(home, collections, messages, me)

    /** 允许出现在自定义根导航里的全部目的地（最多 5 项）。 */
    val all: List<AppDestination> = listOf(home, collections, messages, me, forums)

    const val ACTION_NEW_TOPIC = "newTopic"

    fun byId(id: String): AppDestination? = all.firstOrNull { it.id == id }

    /**
     * 由已保存的原始项目派生可见目的地：
     * 旧别名先归一为稳定 ID，再去重并忽略未知项/发帖动作；
     * 有效项不足两项时回退默认四项，但**不写回**用户原始偏好。
     */
    fun visibleFrom(stored: List<String>): List<AppDestination> {
        val resolved = stored.mapNotNull(::byId).distinctBy { it.id }
        return if (resolved.size >= 2) resolved.take(5) else defaults
    }
}

/** route（含参数）→ 稳定目的地 ID；用于选中态与根切换判定。 */
internal object AppDestinationId {
    fun canonical(route: String): String = when (val raw = route.substringBefore("?")) {
        "topicCollectionsRoot" -> AppDestinations.collections.id
        "directMessagesRoot", "directMessages", "notifications" -> AppDestinations.messages.id
        else -> raw
    }
}
