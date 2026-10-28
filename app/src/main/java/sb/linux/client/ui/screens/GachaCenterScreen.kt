package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun GachaCenterScreen(session: Session, nav: NavHostController) {
    SourceGachaScreen(session, nav, "center")
}
