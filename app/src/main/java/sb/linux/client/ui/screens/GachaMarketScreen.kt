package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun GachaMarketScreen(session: Session, nav: NavHostController) {
    SourceGachaScreen(session, nav, "market")
}
