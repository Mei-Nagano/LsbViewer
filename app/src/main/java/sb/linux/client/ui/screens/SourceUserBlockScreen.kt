package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun SourceUserBlockScreen(session: Session, nav: NavHostController, sourceUrl: String = "/user_block") {
    NativeAccountScreen(session, nav, "用户屏蔽", sourceUrl,
        onChanged = {
            session.onUserSummaryBlockChanged()
        })
}
