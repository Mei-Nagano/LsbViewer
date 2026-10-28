package sb.linux.client.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/** 单/双栏共用的根路由；其余路由视为需要随布局迁移的详情。 */
private val PaneRootRoutes = setOf(
    "home", "forums", "topicCollectionsRoot", "directMessagesRoot", "me", "messages", "detailEmpty",
)

private val RouteArgPattern = Regex("\\{([^}]+)\\}")

/**
 * 由当前目的地与实参还原可再次 navigate 的地址：路径占位符必须齐全，查询参数有则带上。
 * 无法还原时返回 null，迁移宁可不做也不跳到错误页面。
 */
private fun NavBackStackEntry.routeForMigration(): String? {
    val full = destination.route?.takeIf { it.isNotBlank() } ?: return null
    val pathPattern = full.substringBefore("?")
    val bundle = arguments
    var route = pathPattern
    RouteArgPattern.findAll(pathPattern).forEach { match ->
        val key = match.groupValues[1].substringBefore("=")
        val value = bundle?.get(key)?.toString() ?: return null
        route = route.replace(match.value, Uri.encode(value))
    }
    val queryPattern = full.substringAfter("?", "")
    if (queryPattern.isNotBlank()) {
        val query = queryPattern.split("&").mapNotNull { token ->
            val key = token.substringBefore("=").removePrefix("{").removeSuffix("}")
            bundle?.get(key)?.toString()?.let { "$key=${Uri.encode(it)}" }
        }
        if (query.isNotEmpty()) route += "?" + query.joinToString("&")
    }
    return route
}

/**
 * 布局切换时把当前详情（含实参）搬到新宿主，避免窄转宽后右栏回到空态。
 *
 * 配置变更会重建 Activity，且未显示的一侧 NavHost 不参与组合、`currentBackStackEntry`
 * 可能为空，因此同时保存另一布局下的最后详情快照；迁移时优先实时路由，再退回快照。
 */
@Composable
internal fun PaneLayoutMigration(
    useTwoPane: Boolean,
    nav: NavHostController,
    masterNav: NavHostController,
    detailNav: NavHostController,
    phoneRoute: String?,
    masterRoute: String?,
    detailRoute: String?,
) {
    var pending by remember { mutableStateOf(false) }
    var savedPhoneDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var savedPaneDetail by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(useTwoPane) { pending = true }

    LaunchedEffect(useTwoPane, phoneRoute, detailRoute) {
        if (useTwoPane) {
            val entry = detailNav.currentBackStackEntry
            val route = entry?.destination?.route
            savedPaneDetail = if (route != null && route !in PaneRootRoutes) entry?.routeForMigration() else null
        } else {
            val entry = nav.currentBackStackEntry
            val route = entry?.destination?.route
            savedPhoneDetail = if (route != null && route !in PaneRootRoutes) entry?.routeForMigration() else null
        }
    }

    LaunchedEffect(useTwoPane, phoneRoute, detailRoute) {
        if (!pending) return@LaunchedEffect
        fun targetOf(entry: NavBackStackEntry?): String? {
            val route = entry?.destination?.route ?: return null
            if (route in PaneRootRoutes) return null
            return entry.routeForMigration()
        }
        if (useTwoPane) {
            val target = targetOf(nav.currentBackStackEntry) ?: savedPhoneDetail
            if (target != null) {
                runCatching { detailNav.navigate(target) { launchSingleTop = true } }
            } else {
                AppDestinations.byId(AppDestinationId.canonical(phoneRoute.orEmpty()))?.let { destination ->
                    val masterId = AppDestinationId.canonical(
                        masterNav.currentBackStackEntry?.destination?.route.orEmpty(),
                    )
                    if (destination.id != masterId) {
                        masterNav.navigate(destination.route) { launchSingleTop = true }
                    }
                }
            }
        } else {
            val target = targetOf(detailNav.currentBackStackEntry) ?: savedPaneDetail
            if (target != null) {
                runCatching { nav.navigate(target) { launchSingleTop = true } }
            } else {
                AppDestinations.byId(AppDestinationId.canonical(masterRoute.orEmpty()))?.let { destination ->
                    val phoneId = AppDestinationId.canonical(
                        nav.currentBackStackEntry?.destination?.route.orEmpty(),
                    )
                    if (destination.id != phoneId) {
                        nav.navigate(destination.route) { launchSingleTop = true }
                    }
                }
            }
        }
        pending = false
    }
}
