package sb.linux.client.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.media.ImageHostConfig
import sb.linux.client.domain.media.ImageUploadMethod
import sb.linux.client.domain.media.ImageUploadRepository
import sb.linux.client.domain.topic.SourceEditorRepository
import java.io.File

internal class SourceAttachmentImageClient(
    private val context: Context,
    private val source: SourceEditorRepository,
    private val external: ImageUploadRepository,
    private val userId: () -> Long,
    private val identityVersion: () -> Long,
) : ImageUploadRepository {
    override suspend fun upload(uri: String, config: ImageHostConfig): String {
        if (config.method != ImageUploadMethod.ATTACHMENT_SPACE) return external.upload(uri, config)
        val owner = userId()
        val identity = identityVersion()
        check(owner > 0) { "登录后才能上传到附件空间" }
        val page = source.load("/topic_edit")
        check(page.uploadUrl.isNotBlank()) { "源站没有提供附件空间上传入口" }
        return withContext(Dispatchers.IO) {
            val selected = Uri.parse(uri)
            val resolver = context.contentResolver
            val name = resolver.query(selected, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }.orEmpty().ifBlank { "image" }
            val namedExtension = name.substringAfterLast('.', "").lowercase()
            val mime = resolver.getType(selected)?.takeIf { it.startsWith("image/") }
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(namedExtension)
                ?: error("无法识别所选图片类型")
            check(mime.startsWith("image/")) { "请选择图片文件" }
            val extension = namedExtension.ifBlank {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty()
            }
            val allowed = page.acceptedFiles.split(',').map(String::trim)
                .filter { it.startsWith('.') }.map { it.drop(1).lowercase() }
            check(allowed.isEmpty() || extension in allowed) { "源站不支持此图片类型" }
            val suffix = extension.filter(Char::isLetterOrDigit).take(12).ifBlank { "img" }
            val file = File.createTempFile("source-image-", ".$suffix", context.cacheDir)
            try {
                resolver.openInputStream(selected)?.use { input -> file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var size = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        check(size <= page.maxUploadBytes) { "图片超过源站附件大小限制" }
                        output.write(buffer, 0, count)
                    }
                    check(size > 0) { "所选图片为空" }
                } } ?: error("无法读取所选图片")
                check(owner == userId() && identity == identityVersion()) { "账号已变化，请重新选择图片" }
                val csrf = page.form.hidden.firstOrNull { it.first == "_csrf" }?.second.orEmpty()
                check(csrf.isNotBlank()) { "源站未提供上传凭据，请重新登录" }
                val markdown = source.upload(page.uploadUrl, csrf, file, mime, name)
                check(owner == userId() && identity == identityVersion()) { "账号已变化，图片未插入当前编辑器" }
                val image = SourceHtmlSupport.sourceDocument(HtmlTextConversion.markdownToHtml(markdown))
                    .selectFirst("img[src]")?.attr("src").orEmpty()
                check(image.isNotBlank()) { "源站已上传附件，但未返回图片链接，请在附件管理中查看" }
                SourceHtmlSupport.absUrl(image)
            } finally { file.delete() }
        }
    }
}
