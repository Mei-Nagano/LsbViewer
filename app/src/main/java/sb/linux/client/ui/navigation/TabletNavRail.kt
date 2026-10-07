package sb.linux.client.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 平板左侧导航栏：侧边栏按钮与首页/我的顶层入口。 */
@Composable
internal fun TabletNavRail(selected: String, onSelect: (String) -> Unit, onOpenSidebar: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RectangleShape,
        modifier = Modifier.fillMaxHeight().width(80.dp),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onOpenSidebar, modifier = Modifier.padding(top = 10.dp).size(44.dp)) {
                Icon(Icons.Filled.Menu, "侧边栏", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            TabletRailItem(
                icon = Icons.Filled.Home,
                label = "首页",
                selected = selected == "home",
                onClick = { onSelect("home") },
                modifier = Modifier.weight(1f),
            )
            TabletRailItem(
                icon = Icons.Filled.Person,
                label = "我的",
                selected = selected == "me",
                onClick = { onSelect("me") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabletRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Box(Modifier.size(width = 52.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        Modifier.size(22.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
