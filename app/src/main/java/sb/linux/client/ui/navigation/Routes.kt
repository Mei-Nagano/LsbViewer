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
import sb.linux.client.data.Session
import sb.linux.client.ui.screens.*

/** 页面转场统一时长。 */
internal const val NAV_FADE_MS = 150

/** 平板双栏下提供主栏 NavHostController；手机模式为 null。 */
val LocalMasterNav = staticCompositionLocalOf<NavHostController?> { null }

/** 主栏路由：底部导航顶层页面和应用设置菜单。 */
fun NavGraphBuilder.masterRoutes(session: Session, nav: NavHostController, homeDrawerState: DrawerState) {
    composable("home") { HomeScreen(session, nav, homeDrawerState) }
    composable("forums") { ForumListScreen(session, nav) }
    composable(
        "me",
        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
    ) { MeScreen(session, nav) }
    composable(
        "appSettings",
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { AppSettingsScreen(session, nav) }
}

/** 设置类子页的统一短淡入淡出转场。 */
fun NavGraphBuilder.settingsComposable(route: String, content: @Composable () -> Unit) {
    composable(
        route,
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { content() }
}

/** 详情路由：帖子、用户、搜索、设置子页等。 */
fun NavGraphBuilder.detailRoutes(session: Session, nav: NavHostController) {
    composable("detailEmpty") { DetailEmptyPane() }
    composable(
        "search?q={q}&scope={scope}&sort={sort}",
        arguments = listOf(
            navArgument("q") { type = NavType.StringType; defaultValue = "" },
            navArgument("scope") { type = NavType.StringType; defaultValue = "all" },
            navArgument("sort") { type = NavType.StringType; defaultValue = "relevance" },
        ),
    ) { SearchScreen(session, nav) }
    // Legacy deep links from pre-Meilisearch builds remain readable; SearchScreen maps field -> scope.
    composable(
        "search?q={q}&field={field}",
        arguments = listOf(
            navArgument("q") { type = NavType.StringType; defaultValue = "" },
            navArgument("field") { type = NavType.StringType; defaultValue = "title" },
        ),
    ) { SearchScreen(session, nav) }
    composable("login") { LoginScreen(session, nav) }
    composable(
        "forum/{id}?p={p}",
        arguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("p") { type = NavType.IntType; defaultValue = 1 },
        ),
    ) { ForumScreen(session, nav) }
    composable(
        "topic/{tid}?p={p}&floor={floor}",
        arguments = listOf(
            navArgument("tid") { type = NavType.LongType },
            navArgument("p") { type = NavType.IntType; defaultValue = 1 },
            navArgument("floor") { type = NavType.IntType; defaultValue = 0 },
        ),
    ) { TopicScreen(session, nav) }
    composable("newTopic") { NewTopicScreen(session, nav) }
    composable(
        "editTopic/{tid}",
        arguments = listOf(navArgument("tid") { type = NavType.LongType }),
    ) { NewTopicScreen(session, nav, editId = it.arguments?.getLong("tid") ?: 0L) }
    composable(
        "user/{uid}?tab={tab}",
        arguments = listOf(
            navArgument("uid") { type = NavType.LongType },
            navArgument("tab") { type = NavType.StringType; defaultValue = "topics" },
        ),
    ) { UserScreen(session, nav) }
    settingsComposable("settings") { SettingsScreen(session, nav) }
    settingsComposable("generalSettings") { GeneralSettingsScreen(session, nav) }
    settingsComposable("networkSettings") { NetworkSettingsScreen(session, nav) }
    settingsComposable("dohSettings") { DohSettingsScreen(session, nav) }
    settingsComposable("browseSettings") { BrowseSettingsScreen(session, nav) }
    settingsComposable("dataManagement") { DataManagementScreen(session, nav) }
    settingsComposable("usageStats") { UsageStatsScreen(session, nav) }
    settingsComposable("aiSettings") { AiSettingsScreen(session, nav) }
    settingsComposable("transferSettings") { TransferSettingsScreen(session, nav) }
    settingsComposable("themeSettings") { ThemeSettingsScreen(session, nav) }
    settingsComposable("exportedTopics") { ExportedTopicsScreen(session, nav) }
    composable(
        "exportedHtml?path={path}",
        arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" }),
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { ExportedHtmlScreen(session, nav) }
    settingsComposable("about") { AboutScreen(session, nav) }
    composable(
        "web?url={url}",
        arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" }),
    ) { WebScreen(session, nav) }
    settingsComposable("blockWords") { BlockWordsScreen(session, nav) }
    composable("inviteCenter") { InviteCenterScreen(session, nav) }
    composable("gachaProfile") { GachaProfileScreen(session, nav) }
    composable("gachaCenter") { GachaCenterScreen(session, nav) }
    composable("gachaMarket") { GachaMarketScreen(session, nav) }
    composable(
        "gachaOperation/{kind}?title={title}",
        arguments = listOf(
            navArgument("kind") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry -> GachaOperationScreen(session, nav, entry.arguments?.getString("kind").orEmpty()) }
    composable(
        "leaderboard?type={type}",
        arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "points" }),
    ) { LeaderboardScreen(session, nav) }
    composable("notifications") { NotificationsScreen(session, nav) }
    composable("directMessages") { DirectMessagesScreen(session, nav) }
    composable(
        "topicCollections?tab={tab}",
        arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "everyone" }),
    ) { TopicCollectionsScreen(session, nav, initialMine = it.arguments?.getString("tab") == "mine") }
    composable("directMessagesRoot") { DirectMessagesScreen(session, nav, showBack = false) }
    composable("topicCollectionsRoot") { TopicCollectionsScreen(session, nav, initialMine = false, showBack = false) }
    composable("collectionActions?path={path}") { CollectionDetailScreen(session, nav) }
    composable("footprint") { FootprintScreen(session, nav) }
    composable("favorites") { FavoritesScreen(session, nav) }
    composable(
        "chat/{userId}?name={name}&avatar={avatar}",
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("name") { type = NavType.StringType; defaultValue = "" },
            navArgument("avatar") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { ChatScreen(session, nav) }
    composable(
        "report/{type}/{id}",
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("id") { type = NavType.LongType },
        ),
    ) { ReportScreen(session, nav) }
}

/** 平板双栏右栏空态。 */
@Composable
private fun DetailEmptyPane() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("从左侧选择内容查看", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                "帖子、用户主页与设置子页将在此展示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 液态玻璃底栏。 */
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
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(6.dp.toPx())
                    lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true)
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
