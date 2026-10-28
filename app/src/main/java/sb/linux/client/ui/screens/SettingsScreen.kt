package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.session.Session

@Composable
fun SettingsScreen(session: Session, nav: NavHostController) {
    SourceAccountPage(session, nav, "个人资料与账号", "/profile", "登录后管理原站个人资料与账号",
        onExit = { session.refreshSession() },
        onLoaded = { _, url ->
            if (Endpoints.isInternal(android.net.Uri.parse(url).host)) {
                session.accountAuthenticationRepository.importWebCookies(url)
            }
        })
}
