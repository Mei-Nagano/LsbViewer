package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun GachaOperationScreen(session: Session, nav: NavHostController, kind: String) {
    SourceGachaScreen(session, nav, kind)
}
