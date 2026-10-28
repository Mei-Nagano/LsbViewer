package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun AttachmentsScreen(session: Session, nav: NavHostController) {
    SourceAccountPage(session, nav, "附件管理", "/user/${session.account.loginState.userId}?tab=attachments", "登录后查看和管理自己的附件")
}
