package sb.linux.client.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.InviteCenterViewModel
import sb.linux.client.ui.feature.community.InviteCenterViewModelFactory
import sb.linux.client.ui.session.Session

/** 邀请中心：分享链接、奖励统计与已邀请用户；状态与内容共享同一外壳。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCenterScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.inviteRepository) {
        InviteCenterViewModelFactory(session.inviteRepository)
    }
    val viewModel: InviteCenterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    LaunchedEffect(viewModel) { viewModel.load() }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    AppPageScaffold(
        title = "邀请中心",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
        },
    ) { pad ->
        val data = state.data
        when {
            state.loading -> LoadingBox(Modifier.padding(pad))
            state.error != null -> ErrorBox(state.error!!, Modifier.padding(pad), onRetry = viewModel::load)
            !session.account.loginState.loggedIn -> LoginRequiredBox(
                "邀请中心", "登录后可查看邀请链接与已邀请用户", Modifier.padding(pad),
            ) { nav.navigate("login") }
            data == null -> LoadingBox(Modifier.padding(pad))
            else -> InviteContent(data, nav, copied) {
                clipboard?.setPrimaryClip(ClipData.newPlainText("邀请链接", data.link))
                copied = true
            }
        }
    }
}
