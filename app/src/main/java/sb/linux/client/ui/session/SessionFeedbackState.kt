package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sb.linux.client.common.ui.SessionUiInvalidation

/** Owner for app-wide feedback and verification presentation state. */
internal class SessionFeedbackState {
    var toast by mutableStateOf<String?>(null)
    var uiInvalidation: SessionUiInvalidation? = null
    var pendingVerification by mutableStateOf<PendingVerification?>(null)
}
