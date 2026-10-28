package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun SourceUserBlockScreen(session: Session, nav: NavHostController, sourceUrl: String = "/user_block") {
    SourceAccountPage(session, nav, "用户屏蔽", sourceUrl, "登录后管理原站屏蔽名单",
        onExit = {
            session.topicPageCacheRepository.clearAll()
            session.homeBrowseCache.clearHomeCache()
            session.homeBrowse.homeTabs.forEach { it.topics = emptyList(); it.loaded = false }
            session.homeBrowse.requestRefresh()
        })
}
