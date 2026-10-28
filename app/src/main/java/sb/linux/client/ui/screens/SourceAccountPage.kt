package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.session.Session

/** Common login gate for pages whose data belongs to the signed-in source account. */
@Composable
internal fun AccountPageGate(
    loggedIn: Boolean, title: String, message: String, nav: NavHostController,
    content: @Composable () -> Unit,
) {
    if (loggedIn) { content(); return }
    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = title,
        onBack = { nav.popBackStack() },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoginRequiredBox(title, message) { nav.navigate("login") }
        }
    }
}

@Composable
internal fun SourceAccountPage(
    session: Session, nav: NavHostController, title: String, path: String, message: String,
    onExit: (() -> Unit)? = null,
    onLoaded: ((android.webkit.WebView, String) -> Unit)? = null,
) {
    AccountPageGate(session.account.loginState.loggedIn && session.account.loginState.userId > 0,
        title, message, nav) {
        if (onExit != null) DisposableEffect(session) { onDispose { onExit() } }
        WebScreen(session, nav, sourceUrl = path, title = title, onLoaded = onLoaded)
    }
}
