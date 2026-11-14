package sb.linux.client.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.session.Session

@Composable
internal fun SourcePollScreen(session: Session, nav: NavHostController, url: String) {
    var poll by remember(url) { mutableStateOf<TopicPoll?>(null) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var loading by remember(url) { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    val identity = session.accountIdentityVersion
    LaunchedEffect(url, identity, revision) {
        loading = true; poll = null
        try {
            val result = session.sourceEditorRepository.poll(url)
            if (identity == session.accountIdentityVersion) { poll = result; error = null }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "投票结果加载失败" }
        finally { loading = false }
    }
    SettingsPage("投票结果", nav, headerActions = {
        sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = { if (!loading) { loading = true; revision++ } }, loading = loading)
    }) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { revision++ })
            else -> TopicPollView(poll, null, null, session.account.loginState.loggedIn,
                busy = false, topUpBusy = false, onLogin = { nav.navigate("login") },
                onApply = {}, onTopUp = {}, onVote = { _, _ -> })
        }
    }
}
