package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sb.linux.client.common.filter.KeywordFilterSnapshot

/** Observable account-scoped status used by sign-in and filtering surfaces. */
internal class SessionAccountUiState {
    var checkinText by mutableStateOf("")
    var checkinCheckedToday by mutableStateOf(false)
    var keywordFilter by mutableStateOf(KeywordFilterSnapshot())
    var blockedWords by mutableStateOf<Set<String>>(emptySet())
    var blockedUsers by mutableStateOf<Set<String>>(emptySet())
}
