package sb.linux.client.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.feature.forum.ForumListScreen
import sb.linux.client.ui.feature.forum.ForumScreen
import sb.linux.client.ui.feature.search.SearchScreen
import sb.linux.client.ui.screens.*

/** 页面转场统一时长。 */
internal const val NAV_FADE_MS = 150

/** 平板双栏下提供主栏 NavHostController；手机模式为 null。 */
val LocalMasterNav = staticCompositionLocalOf<NavHostController?> { null }

/** 主栏路由：根目的地与设置菜单；双栏时详情进入右侧 NavHost。 */
fun NavGraphBuilder.masterRoutes(session: Session, nav: NavHostController, homeDrawerState: DrawerState) {
    composable("home") { HomeScreen(session, nav, homeDrawerState) }
    composable("forums") { ForumListScreen(session, nav) }
    composable("topicCollectionsRoot") { TopicCollectionsScreen(session, nav, initialMine = false, showBack = false) }
    composable(
        "messages?section={section}",
        arguments = listOf(navArgument("section") { type = NavType.StringType; defaultValue = "dm" }),
    ) { entry ->
        MessagesScreen(session, nav, entry.arguments?.getString("section") ?: "dm", showBack = false)
    }
    composable("directMessagesRoot") { MessagesScreen(session, nav, initialSection = "dm", showBack = false) }
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
    ) { entry -> SearchScreen(session, nav, entry) }
    // Legacy deep links from pre-Meilisearch builds remain readable; SearchScreen maps field -> scope.
    composable(
        "search?q={q}&field={field}",
        arguments = listOf(
            navArgument("q") { type = NavType.StringType; defaultValue = "" },
            navArgument("field") { type = NavType.StringType; defaultValue = "title" },
        ),
    ) { entry -> SearchScreen(session, nav, entry) }
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
    composable("newTopic") { SourceEditorScreen(session, nav) }
    composable(
        "editTopic/{tid}",
        arguments = listOf(navArgument("tid") { type = NavType.LongType }),
    ) { SourceEditorScreen(session, nav, editId = it.arguments?.getLong("tid") ?: 0L) }
    composable(
        "user/{uid}?tab={tab}",
        arguments = listOf(
            navArgument("uid") { type = NavType.LongType },
            navArgument("tab") { type = NavType.StringType; defaultValue = "topics" },
        ),
    ) { UserScreen(session, nav) }
    // 源站 /user?username=xxx 形式的 @提及：先解析出 uid，再替换跳转到 user/{uid}
    composable(
        "userByName?name={name}&tab={tab}",
        arguments = listOf(
            navArgument("name") { type = NavType.StringType; defaultValue = "" },
            navArgument("tab") { type = NavType.StringType; defaultValue = "topics" },
        ),
    ) { UserByNameScreen(session, nav) }
    extraSettingsRoutes(session, nav)
    settingsComposable("settings") { SettingsScreen(session, nav) }
    settingsComposable("generalSettings") { GeneralSettingsScreen(session, nav) }
    settingsComposable("networkSettings") { NetworkSettingsScreen(session, nav) }
    settingsComposable("browseSettings") { BrowseSettingsScreen(session, nav) }
    settingsComposable("aiSettings") { AiSettingsScreen(session, nav) }
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
    composable("userBlock?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/user_block" },
    )) { entry -> SourceUserBlockScreen(session, nav, entry.arguments?.getString("url") ?: "/user_block") }
    composable("gachaProfile") { SourceGachaScreen(session, nav) }
    composable("gachaCenter") { SourceGachaScreen(session, nav, "center") }
    composable("gachaMarket") { SourceGachaScreen(session, nav, "market") }
    composable(
        "gachaOperation/{kind}?title={title}",
        arguments = listOf(
            navArgument("kind") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry -> SourceGachaScreen(session, nav, entry.arguments?.getString("kind").orEmpty()) }
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
    composable("directMessagesRoot") { MessagesScreen(session, nav, initialSection = "dm", showBack = false) }
    composable(
        "messages?section={section}",
        arguments = listOf(navArgument("section") { type = NavType.StringType; defaultValue = "dm" }),
    ) { entry ->
        MessagesScreen(session, nav, entry.arguments?.getString("section") ?: "dm", showBack = false)
    }
    composable("topicCollectionsRoot") { TopicCollectionsScreen(session, nav, initialMine = false, showBack = false) }
    composable("collectionActions?path={path}") { CollectionDetailScreen(session, nav) }
    composable("topicCollectionManage?path={path}") { TopicCollectionManageScreen(session, nav) }
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

/** 平板双栏右栏空态：复用统一的空态版式。 */
@Composable
private fun DetailEmptyPane() {
    EmptyBox("从左侧选择内容查看", "帖子、用户主页与设置子页将在此展示")
}

/** 液态玻璃底栏。 */
