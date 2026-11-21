package sb.linux.client.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session

internal class TopicScreenUiState(
    session: Session,
    favoriteCommentIds: Set<Long>,
) : androidx.lifecycle.ViewModel() {
    val listState = androidx.compose.foundation.lazy.LazyListState()
    var sourceBrowser: SourceContentWebView? = null
    var sourceLoadedShell by mutableStateOf<String?>(null)
    var sourceReady by mutableStateOf(false)
    var sourceLoadError by mutableStateOf(false)
    var sourceSlotHeight by mutableFloatStateOf(0f)
    var consumedBlockRefresh by mutableStateOf(session.homeBrowse.refreshRequest)
    var lastLoggedIn by mutableStateOf(session.account.loginState.loggedIn)
    var danmakuInitialized = false
    var donationEntryHandled = false

    override fun onCleared() {
        sourceBrowser?.let { web ->
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.stopLoading()
            web.webChromeClient = null
            web.webViewClient = android.webkit.WebViewClient()
            web.destroy()
        }
        sourceBrowser = null
    }

    var showReply by mutableStateOf(false)
    var quoteText by mutableStateOf("")
    val captchaOverride: MutableState<LoginVerification?> = mutableStateOf(null)
    var coinTarget by mutableStateOf<PostEntry?>(null)
    var copyPost by mutableStateOf<PostEntry?>(null)
    var userReplies by mutableStateOf<List<PostEntry>?>(null)
    var userRepliesTitle by mutableStateOf("")
    var showFloorDialog by mutableStateOf(false)
    var menuOpen by mutableStateOf(false)
    var topicSearchOpen by mutableStateOf(false)
    var topicSearchListOpen by mutableStateOf(false)
    var topicSearchIndex by mutableIntStateOf(0)
    var pendingSearchPost by mutableStateOf<PostEntry?>(null)
    val searchPositions = mutableMapOf<Long, Pair<String, Float>>()
    var tocOpen by mutableStateOf(false)
    var topicSearchQuery by mutableStateOf("")
    var showExportDialog by mutableStateOf(false)
    var exportBusy by mutableStateOf<String?>(null)
    var showDonate by mutableStateOf(false)
    var pollBusy by mutableStateOf(false)
    var pollViewBusy by mutableStateOf(false)
    var favoriteBusy by mutableStateOf(false)
    var essenceBusy by mutableStateOf(false)
    var essenceTopUpBusy by mutableStateOf(false)
    val sortOrder = 1
    var danmaku by mutableStateOf<List<DanmakuItem>>(emptyList())
    var danmakuLocalOn by mutableStateOf(session.danmakuOn)
    var threadPosts by mutableStateOf<List<PostEntry>?>(null)
    var threadLoading by mutableStateOf(false)
    var threadError by mutableStateOf<String?>(null)
    var highlightPostId by mutableStateOf<Long?>(null)
    var returnPos by mutableStateOf<Pair<Int, Int>?>(null)
    var pendingRestoreOffset by mutableStateOf<Int?>(null)
    var titleBottomInRoot by mutableFloatStateOf(Float.MAX_VALUE)
    var contentTopInRoot by mutableFloatStateOf(0f)
    var isFav by mutableStateOf(false)
    var showUnfavDialog by mutableStateOf(false)
    var favoriteCommentIds by mutableStateOf(favoriteCommentIds)
    var aiSummary by mutableStateOf<String?>(null)
    var aiBusy by mutableStateOf(false)
    var aiError by mutableStateOf<String?>(null)
    val headingPositions = mutableStateMapOf<Int, Float>()
    var bodyTop by mutableFloatStateOf(0f)
    var bodyHeight by mutableFloatStateOf(0f)
    var collapsedReplyIds by mutableStateOf(emptySet<Long>())
    var replyModeTick by mutableIntStateOf(0)
    val sourceGeometry = SourceTopicGeometry()
    var sourceHiddenSignatures by mutableStateOf(emptySet<Long>())
    var sourceSignatureRestoreVersion = 0
}
