package sb.linux.client.ui.screens

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.common.content.clipboardHtmlToMarkdown
import sb.linux.client.domain.topic.SourceEditorMedia
import sb.linux.client.ui.session.Session
import java.io.File
import java.net.URI

/** 只在用户点击粘贴时读取；富文本转换在本地完成，图片按源站转存接口处理。 */
internal data class EditorClipboard(val markdown: String, val images: List<android.net.Uri>)

internal fun readEditorClipboard(context: Context): EditorClipboard? {
    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val item = clip.getItemAt(0)
    val images = (0 until clip.itemCount).mapNotNull { index ->
        clip.getItemAt(index).uri?.takeIf { runCatching { context.contentResolver.getType(it)?.startsWith("image/") == true }.getOrDefault(false) }
    }
    val markdown = item.htmlText?.takeIf(String::isNotBlank)?.let(::clipboardHtmlToMarkdown)
        ?: if (images.isEmpty()) item.coerceToText(context)?.toString().orEmpty() else ""
    return EditorClipboard(markdown, images)
}

internal suspend fun transferEditorClipboardImages(session: Session, media: SourceEditorMedia, context: Context,
    markdown: String, identity: Long, onTransferred: (String, String) -> Unit): Int {
    if (media.remoteImageUrl.isBlank() || media.uploadUrl.isBlank()) return 0
    val images = Regex("""!\[[^\]\n]*\]\((https?://[^)\s]+)\)""").findAll(markdown)
        .map { it.value to it.groupValues[1] }.distinct()
        .filter { runCatching { !sb.linux.client.common.network.Endpoints.isInternal(URI(it.second).host) }.getOrDefault(false) }.toList()
    var failed = 0
    for ((markup, url) in images) {
        if (identity != session.accountIdentityVersion) break
        try {
            val inserted = withContext(Dispatchers.IO) {
                val image = session.sourceEditorRepository.remoteImage(media.remoteImageUrl, media.csrf, url)
                val extension = when (image.mime) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/gif" -> "gif"; else -> "webp" }
                val file = File.createTempFile("source-paste-", ".$extension", context.cacheDir)
                try {
                    check(image.bytes.size <= media.maxUploadBytes) { "图片超过源站限制" }
                    file.writeBytes(image.bytes)
                    val prepared = prepareSourceAttachment(file, image.mime, media.maxUploadBytes, false)
                    try {
                        check(identity == session.accountIdentityVersion) { "账号已变化" }
                        session.sourceEditorRepository.upload(media.uploadUrl, media.csrf, prepared.file, prepared.mime, "paste.$extension", false)
                    } finally { if (prepared.file != file) prepared.file.delete() }
                } finally { file.delete() }
            }
            val newUrl = Regex("\\(([^)]+)\\)").find(inserted)?.groupValues?.get(1) ?: continue
            if (identity == session.accountIdentityVersion) onTransferred(markup, markup.replace(url, newUrl))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed++ }
    }
    return failed
}
