package sb.linux.client.ui.feature.linkpreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.linkpreview.LinkPreviewInfo
import sb.linux.client.domain.linkpreview.LinkPreviewRepository

class LinkPreviewViewModel(private val repository: LinkPreviewRepository) : ViewModel() {
    suspend fun load(url: String): LinkPreviewInfo = repository.load(url)
}

class LinkPreviewViewModelFactory(
    private val repository: LinkPreviewRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LinkPreviewViewModel(repository) as T
}
