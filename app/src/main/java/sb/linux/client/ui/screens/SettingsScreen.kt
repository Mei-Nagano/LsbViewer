package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavHostController
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.session.Session

@Composable
fun SettingsScreen(session: Session, nav: NavHostController) {
    DisposableEffect(session) { onDispose { session.refreshSession() } }
    WebScreen(session, nav, title = "个人资料与账号", sourceUrl = "/profile",
        initialContentSelector = "main",
        onLoaded = { _, url ->
            if (Endpoints.isInternal(android.net.Uri.parse(url).host)) {
                session.accountAuthenticationRepository.importWebCookies(url)
            }
        })
}
