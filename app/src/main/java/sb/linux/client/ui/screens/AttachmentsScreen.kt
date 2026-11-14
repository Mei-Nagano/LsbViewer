package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun AttachmentsScreen(session: Session, nav: NavHostController) {
    NativeAccountScreen(session, nav, "附件管理", "/user/${session.account.loginState.userId}?tab=attachments", onChanged = { session.refreshSession() })
}
