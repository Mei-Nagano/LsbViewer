package sb.linux.client.ui.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceMenu

/** 根页面名称与首页排序标题共享同一字阶和字重。 */
@Composable
fun AppPageTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** 当前来源为主，箭头为辅；字阶、最小触区、间距和菜单继承现有主题。 */
@Composable
internal fun <T> AppPageTitleSelector(
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    fallbackLabel: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selectedValue }?.second ?: fallbackLabel
    Box(modifier) {
        Row(
            Modifier.clip(MaterialTheme.shapes.large).clickable { expanded = true }
                .heightIn(min = LocalAppSizes.current.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.inline),
        ) {
            AppPageTitle(label, Modifier.weight(1f, fill = false))
            Icon(Icons.Filled.ArrowDropDown, description, Modifier.size(LocalAppSizes.current.icon))
        }
        SettingsChoiceMenu(expanded, { expanded = false }, options, selectedValue, { value ->
            expanded = false
            onSelect(value)
        })
    }
}

/** 全应用返回统一使用无容器的主题色图标按钮（§2.8）。 */
@Composable
fun AppBackButton(onClick: () -> Unit, description: String = "返回") {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, description)
    }
}

@Composable
fun AppRefreshButton(onClick: () -> Unit, enabled: Boolean = true, description: String = "刷新", loading: Boolean = false) {
    IconButton(onClick = { if (!loading) onClick() }, enabled = enabled && !loading,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp).semantics { contentDescription = "${description}中" }, strokeWidth = 2.dp)
        else Icon(Icons.Filled.Refresh, description)
    }
}
