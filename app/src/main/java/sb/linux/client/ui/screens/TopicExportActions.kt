package sb.linux.client.ui.screens

import android.content.Context
import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.export.TopicExportFiles
import sb.linux.client.ui.export.TopicLongImageRenderer
import java.io.File

/** Selects topic content and runs file rendering and sharing off the main thread. */
internal class TopicExportActions(
    private val session: Session,
    private val topicId: Long,
    private val scope: CoroutineScope,
    private val state: TopicPagingState,
    private val ui: TopicScreenUiState,
    private val context: Context,
) {
    fun export(
        kind: String,
        scopeMode: String,
        fromFloor: Int,
        toFloor: Int,
        multiPage: Boolean,
        selectedIds: Set<Long>,
        colorScheme: ColorScheme,
    ) {
        val page = state.page.value ?: return
        ui.exportBusy = kind
        val exportTheme = sb.linux.client.ui.export.TopicExportThemeFactory.from(colorScheme)
        val selectedPosts = when (scopeMode) {
            "main" -> page.posts.filter { it.floor == 0 }.take(1)
            "range" -> page.posts.filter { it.floor in fromFloor.coerceAtMost(toFloor)..fromFloor.coerceAtLeast(toFloor) }
            "selected" -> page.posts.filter { it.id in selectedIds }
            else -> page.posts
        }
        if (selectedPosts.isEmpty()) {
            ui.exportBusy = null
            session.showToast("所选范围没有可导出的楼层，请先加载完整帖子")
            return
        }
        scope.launch {
            try {
                val url = Endpoints.abs("/topic/$topicId")
                val files: List<File> = withContext(Dispatchers.IO) {
                    when (kind) {
                        "html" -> listOf(TopicExportFiles.exportHtml(context, page.title, selectedPosts, url))
                        "md" -> listOf(TopicExportFiles.exportMarkdown(context, page.title, selectedPosts, url))
                        else -> TopicLongImageRenderer.renderLongImages(
                            context, page.title, selectedPosts, url, exportTheme, multiPage,
                        )
                    }
                }
                TopicExportFiles.shareFiles(
                    context, files,
                    when (kind) { "html" -> "text/html"; "md" -> "text/markdown"; else -> "image/png" },
                )
            } catch (error: Exception) {
                session.showToast("导出失败：${error.message}")
            } finally {
                ui.exportBusy = null
            }
        }
    }
}
