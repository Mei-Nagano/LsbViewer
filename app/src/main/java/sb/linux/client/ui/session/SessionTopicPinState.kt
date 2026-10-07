package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sb.linux.client.ui.feature.topic.TopicPinnedSnapshot

/** Transient topic pin shared between the topic page and navigation surfaces. */
internal class SessionTopicPinState {
    var topicId by mutableStateOf(0L)
    var title by mutableStateOf("")
    var snapshot: TopicPinnedSnapshot? = null
}
