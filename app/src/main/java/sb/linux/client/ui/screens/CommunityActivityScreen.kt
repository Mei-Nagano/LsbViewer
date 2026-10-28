package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun CommunityActivityScreen(session: Session, nav: NavHostController) {
    SourceAccountPage(session, nav, "社区活跃度", "/community_activity", "登录后查看原站社区活跃度")
}
