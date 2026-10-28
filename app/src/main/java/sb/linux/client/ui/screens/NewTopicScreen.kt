package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L) {
    SourceEditorScreen(session, nav, editId = editId)
}
