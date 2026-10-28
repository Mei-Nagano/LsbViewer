package sb.linux.client.ui.patterns.topic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 帖子卡片里可被用户单独覆盖颜色的语义槽位。
 *
 * 原先寄放在 `Avatar.kt` 里（头像与卡片配色无关），迁移时按归属拆出：
 * 这些键只被帖子卡片与其预览使用，放在 `patterns/topic` 下。
 */
object CardColorOverrides {
    const val TAG = "tag"               // 帖子标签（置顶/热/精华…）
    const val BACKGROUND = "background" // 帖子卡片背景容器
    const val TIME = "time"             // xx时间前
    const val USER = "user"             // 用户名
    const val VIEWS = "views"           // 浏览量
    const val COMMENTS = "comments"     // 评论数
    const val FORUM = "forum"           // 所属板块
}

/** 当前生效的卡片颜色覆盖表；由应用根部按设置注入，未注入时为空表。 */
val LocalCardColorOverrides = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }
