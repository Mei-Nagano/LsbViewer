package sb.linux.client.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.topic.CardColorOverrides
import sb.linux.client.ui.patterns.topic.TopicCardView

@Composable
internal fun PaletteStyleSwatch(
    seed: Color,
    style: PaletteStyle,
    label: String,
    contrast: Double,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = previewScheme(seed, style, contrast, dark)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .border(borderWidth, borderColor, MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
    ) {
        // 三色调色块预览（上 primary / 中 secondary / 下 tertiary）
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.primary))
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.secondary))
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.tertiary))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
}

/** 预览卡中的可自定义元素角色 */
internal enum class PreviewRole(val label: String) {
    PRIMARY("主要色"), SECONDARY("次要色"), TERTIARY("强调色")
}

internal fun previewRoleColor(scheme: ColorScheme, role: PreviewRole): Color = when (role) {
    PreviewRole.PRIMARY -> scheme.primary
    PreviewRole.SECONDARY -> scheme.secondary
    PreviewRole.TERTIARY -> scheme.tertiary
}

/** 帖子卡片可自定义元素（key 对应 CardColorOverrides；标题色不可改，仅保留源站彩色标题） */
internal data class CardElement(val key: String, val label: String)

internal val CardElements = listOf(
    CardElement(CardColorOverrides.BACKGROUND, "卡片背景"),
    CardElement(CardColorOverrides.TAG, "帖子标签"),
    CardElement(CardColorOverrides.TIME, "时间"),
    CardElement(CardColorOverrides.USER, "用户名"),
    CardElement(CardColorOverrides.COMMENTS, "评论数"),
    CardElement(CardColorOverrides.FORUM, "所属板块"),
)

/** 帖子卡片元素当前颜色：自定义覆盖优先，否则跟随主题 */
internal fun cardElementColor(session: Session, scheme: ColorScheme, key: String): Color =
    session.cardColors[key] ?: when (key) {
        CardColorOverrides.BACKGROUND -> scheme.surfaceContainerLow
        CardColorOverrides.TAG -> scheme.secondaryContainer
        CardColorOverrides.TIME -> scheme.onSurfaceVariant
        CardColorOverrides.USER -> scheme.primary
        CardColorOverrides.COMMENTS -> scheme.onSurfaceVariant
        CardColorOverrides.FORUM -> scheme.secondaryContainer
        else -> scheme.onSurface
    }

/**
 * 实时预览（吸顶）：直接复用首页 TopicCardView 渲染样例卡片，与主页帖子完全一致的展示样式，
 * 点按对应元素（含卡片空白处 = 背景）打开自定义取色板改色；标题不可改色（3.4 / 3.6 / 3.7）。
 */
