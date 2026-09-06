package sb.linux.client.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** 导出文件命名、落盘与系统分享。 */
object TopicExportFiles {
    internal fun safeExportName(title: String): String =
        title.replace(Regex("""[\\/:*?"<>|\s]+"""), "_").take(40).ifBlank { "topic" }

    internal fun exportDir(context: Context): File = File(context.filesDir, "exports").apply { mkdirs() }

    fun shareFile(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享帖子").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareFiles(context: Context, files: List<File>, mime: String) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFile(context, files.single(), mime)
            return
        }
        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享帖子（${files.size} 页）").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportHtml(context: Context, title: String, posts: List<PostEntry>, url: String): File =
        File(exportDir(context), "${safeExportName(title)}.html").also { it.writeText(TopicExportText.buildHtml(title, posts, url)) }

    fun exportMarkdown(context: Context, title: String, posts: List<PostEntry>, url: String): File =
        File(exportDir(context), "${safeExportName(title)}.md").also { it.writeText(TopicExportText.buildMarkdown(title, posts, url)) }
}
