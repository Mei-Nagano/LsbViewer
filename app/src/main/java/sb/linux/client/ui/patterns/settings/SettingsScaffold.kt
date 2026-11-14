package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.components.navigation.AppBackButton
import sb.linux.client.ui.components.navigation.AppPageTitle

// 主层级：页名与分类；次层级：设置标题；辅助层级：说明和当前值。
// 颜色使用现有 ColorScheme，间距与尺寸使用 AppSpacing / AppSizes，圆角使用 Shapes。
// 返回和顶栏操作为次要导航，不增加与设置内容竞争的主要提交按钮。
/** 设置专用外壳：只处理主题保护面、系统 inset 和宽窗限宽，滚动由内容决定。 */
@Composable
internal fun SettingsScaffold(content: @Composable BoxScope.() -> Unit) {
    val sizes = LocalAppSizes.current
    val spacing = LocalAppSpacing.current
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            val horizontal = if (maxWidth >= 600.dp) spacing.pageHorizontalWide else spacing.pageHorizontal
            Box(
                Modifier.fillMaxSize().padding(horizontal = horizontal),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.widthIn(max = sizes.formMaxWidth).fillMaxHeight(), content = content)
            }
        }
    }
}

/** 滚动大标题；紧凑模式不额外预留顶栏高度与分组间距。 */
@Composable
internal fun SettingsHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    val sizes = LocalAppSizes.current
    Column(modifier.fillMaxWidth().padding(top = spacing.item, bottom = if (compact) 0.dp else spacing.section)) {
        if (compact) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppPageTitle(title, Modifier.weight(1f))
                actions()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) AppBackButton(onBack)
                Spacer(Modifier.weight(1f))
                actions()
            }
            Text(
                title,
                Modifier.padding(top = spacing.pageVertical),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 固定操作栏在滚动内容之外，页面大标题仍随内容滚动。 */
@Composable
internal fun SettingsTopBar(title: String, onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}, showTitle: Boolean = true) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = LocalAppSizes.current.topBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.item)) {
            onBack?.let { AppBackButton(it) }
            Box(Modifier.weight(1f)) {
                if (showTitle) Text(title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = settingsCardShape(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().then(SettingsItemData),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalSettingsGrouped provides false) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

/** 连续条目仅首尾使用大圆角，中间使用较小圆角，形成一个完整的设置分组。 */
@Composable
internal fun settingsItemShape(index: Int, count: Int): Shape {
    val outer = MaterialTheme.shapes.extraLarge
    val inner = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    return outer.copy(
        topStart = if (index == 0) outer.topStart else inner.topStart,
        topEnd = if (index == 0) outer.topEnd else inner.topEnd,
        bottomStart = if (index == count - 1) outer.bottomStart else inner.bottomStart,
        bottomEnd = if (index == count - 1) outer.bottomEnd else inner.bottomEnd,
    )
}
