package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import sb.linux.client.ui.components.navigation.AppPageTitleSelector
import sb.linux.client.ui.session.Session

/** 九个源站首页标签；申精是独立审核入口，放在更多菜单。 */
private val homeSortOptions = listOf(
    "comment" to "新评论", "post" to "新帖子", "featured" to "精华", "hot" to "热议",
    "lucky" to "抽奖", "card" to "发卡", "red_packet" to "红包",
    "following" to "关注", "footprint" to "足迹",
)

@Composable
internal fun HomeSortSelector(session: Session, onSelectCombo: (Int, Int) -> Unit, onLogin: () -> Unit) {
    val combo = session.homeBrowse.homeCombo
    val selected = when (combo) {
        1 -> "lucky"; 2 -> "card"; 3 -> "essence"; 4 -> "red_packet"
        else -> SORTS[session.homeBrowse.homeTabIndex.coerceIn(SORTS.indices)].first
    }
    AppPageTitleSelector(homeSortOptions, selected, description = "选择首页排序", fallbackLabel = "申精",
        onSelect = { value ->
            if (value in setOf("following", "footprint") && !session.account.loginState.loggedIn) onLogin()
            else when (value) {
                "lucky" -> onSelectCombo(1, 1)
                "card" -> onSelectCombo(2, 1)
                "red_packet" -> onSelectCombo(4, 1)
                else -> {
                    session.homeBrowse.homeTabIndex = SORTS.indexOfFirst { it.first == value }
                    session.homeBrowse.homeCombo = 0
                }
            }
        })
}
