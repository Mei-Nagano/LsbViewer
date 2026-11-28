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
import sb.linux.client.ui.feature.forum.NodeScreen
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
    composable("nodesRoot") { NodeScreen(session, nav, "/nodes?tab=popular", showBack = false) }
    composable("topicCollectionsRoot") { NodeScreen(session, nav, "/nodes?tab=popular", showBack = false) }
    composable(
        "messages?section={section}",
        arguments = listOf(navArgument("section") { type = NavType.StringType; defaultValue = "dm" }),
    ) { entry ->
        if (entry.arguments?.getString("section") == "notifications") NotificationsScreen(session, nav)
        else DirectMessagesScreen(session, nav, showBack = false)
    }
    composable("directMessagesRoot") { DirectMessagesScreen(session, nav, showBack = false) }
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
    composable("nodes?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/nodes?tab=popular" },
    )) { entry -> sb.linux.client.ui.feature.forum.NodeScreen(session, nav,
        entry.arguments?.getString("url") ?: "/nodes?tab=popular") }
    composable(
        "forum/{id}?p={p}",
        arguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("p") { type = NavType.IntType; defaultValue = 1 },
        ),
    ) { ForumScreen(session, nav) }
    composable(
        "topic/{tid}?p={p}&floor={floor}&replyid={replyid}&donate={donate}",
        arguments = listOf(
            navArgument("tid") { type = NavType.LongType },
            navArgument("p") { type = NavType.IntType; defaultValue = 0 },
            navArgument("floor") { type = NavType.IntType; defaultValue = 0 },
            navArgument("replyid") { type = NavType.LongType; defaultValue = 0L },
            navArgument("donate") { type = NavType.BoolType; defaultValue = false },
        ),
    ) { entry -> TopicScreen(session, nav, entry) }
    composable("pollPage?url={url}", arguments = listOf(navArgument("url") { type = NavType.StringType })) { entry ->
        SourcePollScreen(session, nav, entry.arguments?.getString("url").orEmpty())
    }
    composable("nodeTopicActions?tid={tid}", arguments = listOf(
        navArgument("tid") { type = NavType.LongType },
    )) { entry -> WebScreen(session, nav, sourceUrl = "/topic/${entry.arguments?.getLong("tid")}",
        title = "管理帖子节点", initialContentSelector = "[data-node-open], .node-topic-tags, .post-content") }
    composable("newTopic") { NewTopicScreen(session, nav) }
    composable("editorPage?url={url}&tid={tid}", arguments = listOf(
        navArgument("url") { type = NavType.StringType }, navArgument("tid") { type = NavType.LongType; defaultValue = 0L },
    )) { entry -> NewTopicScreen(session, nav, editId = entry.arguments?.getLong("tid") ?: 0L,
        sourceUrl = entry.arguments?.getString("url")) }
    composable(
        "editTopic/{tid}",
        arguments = listOf(navArgument("tid") { type = NavType.LongType }),
    ) { NewTopicScreen(session, nav, editId = it.arguments?.getLong("tid") ?: 0L) }
    composable("stickerManage?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/sticker_pack_mine" },
    )) { entry -> SourceStickerManageScreen(session, nav, entry.arguments?.getString("url") ?: "/sticker_pack_mine") }
    composable("inviteRelay?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/invite_relay" },
    )) { entry -> SourceInviteRelayScreen(session, nav, entry.arguments?.getString("url") ?: "/invite_relay") }
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
    settingsComposable("licenses") { LicensesScreen(nav) }
    composable("followList?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/index.php?sort=following&follow_view=following" },
    )) { entry ->
        val url = entry.arguments?.getString("url").orEmpty()
        FollowingListScreen(session, nav, url)
    }
    settingsComposable("about") { AboutScreen(session, nav) }
    composable(
        "web?url={url}&inApp={inApp}",
        arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" },
            navArgument("inApp") { type = NavType.BoolType; defaultValue = false }),
    ) { WebScreen(session, nav) }
    settingsComposable("blockWords") { BlockWordsScreen(session, nav) }
    composable("inviteCenter") { InviteCenterScreen(session, nav) }
    composable("userBlock?url={url}", arguments = listOf(
        navArgument("url") { type = NavType.StringType; defaultValue = "/user_block" },
    )) { entry -> SourceUserBlockScreen(session, nav, entry.arguments?.getString("url") ?: "/user_block") }
    composable("gachaProfile") { SourceGachaScreen(session, nav) }
    composable("gachaPage?url={url}", arguments = listOf(navArgument("url") { type = NavType.StringType })) { entry ->
        WebScreen(session, nav, sourceUrl = entry.arguments?.getString("url") ?: "/gacha_profile",
            initialContentSelector = ".gacha-profile-page, .gacha-center-page, main")
    }
    composable("identityCenter") { IdentityCenterScreen(session, nav) }
    composable("accountPage?title={title}&url={url}", arguments = listOf(
        navArgument("title") { type = NavType.StringType }, navArgument("url") { type = NavType.StringType },
    )) { entry -> NativeAccountScreen(session, nav, entry.arguments?.getString("title").orEmpty(),
        entry.arguments?.getString("url").orEmpty(), onChanged = { session.refreshSession() }) }
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
    composable("lotteryPrizes") { LotteryPrizesScreen(session, nav) }
    composable("directMessages") { DirectMessagesScreen(session, nav) }
    composable(
        "topicCollections?tab={tab}",
        arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "everyone" }),
    ) { NodeScreen(session, nav, if (it.arguments?.getString("tab") == "mine") "/nodes?tab=mine" else "/nodes?tab=popular") }
    composable("directMessagesRoot") { DirectMessagesScreen(session, nav, showBack = false) }
    composable(
        "messages?section={section}",
        arguments = listOf(navArgument("section") { type = NavType.StringType; defaultValue = "dm" }),
    ) { entry ->
        if (entry.arguments?.getString("section") == "notifications") NotificationsScreen(session, nav)
        else DirectMessagesScreen(session, nav, showBack = false)
    }
    composable("topicCollectionsRoot") { NodeScreen(session, nav, "/nodes?tab=popular", showBack = false) }
    composable("collectionActions?path={path}") { NodeScreen(session, nav, "/nodes?tab=popular") }
    composable("topicCollectionManage?path={path}") { NodeScreen(session, nav, "/nodes?tab=mine") }
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
