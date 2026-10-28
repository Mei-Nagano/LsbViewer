package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun UserPrivacyScreen(session: Session, nav: NavHostController) {
    SourceAccountPage(session, nav, "我的隐私", "/user_privacy", "登录后管理原站个人主页隐私")
}
