package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 保留原站广告 DOM、点击脚本、CSRF 表单及奖励弹窗，不自行发起奖励请求。 */
@Composable
fun SourcePromotionsScreen(session: Session, nav: NavHostController) {
    WebScreen(session, nav, sourceUrl = "/", title = "原站广告",
        initialContentSelector = ".sidebar-promotions-card")
}
