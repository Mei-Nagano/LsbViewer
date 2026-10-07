package sb.linux.client.ui.screens

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import sb.linux.client.domain.topic.TopicPageData

internal class TopicPagingState(savedState: SavedStateHandle) {
    val page = mutableStateOf<TopicPageData?>(null)
    val loading = mutableStateOf(true)
    val loadingMore = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val loadedMaxPage: MutableState<Int> = SavedMutableState(savedState, "loadedMaxPage", 1)
    val localReplyPage: MutableState<Int> = SavedMutableState(savedState, "localReplyPage", 1)
    val reverseStalledAtPage = mutableIntStateOf(0)
    val allSearchPagesLoaded = mutableStateOf(false)
    val usageViewRecorded: MutableState<Boolean> = SavedMutableState(savedState, "usageViewRecorded", false)
}

private class SavedMutableState<T : Any>(
    private val handle: SavedStateHandle,
    private val key: String,
    initialValue: T,
) : MutableState<T> {
    private val state = mutableStateOf(handle.get<T>(key) ?: initialValue)

    override var value: T
        get() = state.value
        set(value) {
            state.value = value
            handle[key] = value
        }

    override operator fun component1(): T = value
    override operator fun component2(): (T) -> Unit = { value = it }
}
