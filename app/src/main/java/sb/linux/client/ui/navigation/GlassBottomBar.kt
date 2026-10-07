package sb.linux.client.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.forum.ForumListScreen
import sb.linux.client.ui.feature.forum.ForumScreen
import sb.linux.client.ui.feature.search.SearchScreen
import sb.linux.client.ui.screens.*

@Composable
fun LiquidGlassBottomBar(
    backdrop: Backdrop,
    route: String,
    items: List<String>,
    enabled: Boolean = true,
    onNavigate: (String) -> Unit,
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceTint = if (light) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f)
    val width = ((LocalConfiguration.current.screenWidthDp - 32).coerceAtMost(items.size * 108)).dp
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        lens(10.dp.toPx(), 10.dp.toPx(), depthEffect = true, chromaticAberration = false)
                    },
                    onDrawSurface = { drawRect(surfaceTint) },
                )
                .height(58.dp)
                .width(width),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                GlassBottomTab(
                    icon = when (item) {
                        "home" -> Icons.Filled.Home
                        "topicCollections" -> Icons.Filled.ViewList
                        "directMessages" -> Icons.Filled.Email
                        "newTopic" -> Icons.Filled.Add
                        else -> Icons.Filled.Person
                    },
                    label = when (item) {
                        "home" -> "首页"
                        "topicCollections" -> "淘帖"
                        "directMessages" -> "私信"
                        "newTopic" -> "发帖"
                        else -> "我的"
                    },
                    selected = BottomDestination.canonical(route) == BottomDestination.canonical(item),
                    enabled = enabled,
                    onClick = { onNavigate(item) },
                )
            }
        }
    }
}

/** 玻璃底栏的单个标签。 */
@Composable
private fun RowScope.GlassBottomTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(5.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = tint)
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
