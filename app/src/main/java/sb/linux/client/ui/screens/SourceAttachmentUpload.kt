package sb.linux.client.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.data.SourceUploadedAttachment
import sb.linux.client.domain.topic.SourceEditorMedia
import sb.linux.client.ui.session.Session
import java.io.File

internal suspend fun uploadSourceAttachment(context: Context, session: Session, media: SourceEditorMedia,
    uri: Uri, original: Boolean, identity: Long, existing: Set<String>): SourceUploadedAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val metadata = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
        if (it.moveToFirst()) (if (nameIndex >= 0) it.getString(nameIndex) else null) to
            (if (sizeIndex >= 0 && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else -1L) else null
    }
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val defaultExtension = when (mime) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; else -> "bin" }
    val name = metadata?.first?.takeIf(String::isNotBlank) ?: "attachment.$defaultExtension"
    val key = "$name\u001f${metadata?.second}\u001f$uri"
    if (key in existing) return@withContext null
    val extension = name.substringAfterLast('.', "").lowercase()
    val allowed = media.acceptedFiles.split(',').map(String::trim).filter { it.startsWith('.') }.map { it.drop(1).lowercase() }
    check(allowed.isEmpty() || extension in allowed) { "源站不支持此附件类型" }
    val suffix = extension.filter(Char::isLetterOrDigit).take(12).ifBlank { "bin" }
    val file = File.createTempFile("source-attachment-", ".$suffix", context.cacheDir)
    try {
        resolver.openInputStream(uri)?.use { input -> file.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var size = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                size += read
                check(size <= if (original) media.maxUploadBytes else 100L * 1024 * 1024) { "附件超过大小限制" }
                output.write(buffer, 0, read)
            }
        } } ?: error("无法读取附件")
        check(identity == session.accountIdentityVersion) { "账号已变化，请重新选择附件" }
        val prepared = prepareSourceAttachment(file, mime, media.maxUploadBytes, original)
        try {
            check(prepared.file.length() <= media.maxUploadBytes) { "附件超过源站大小限制" }
            val markdown = session.sourceEditorRepository.upload(media.uploadUrl, media.csrf,
                prepared.file, prepared.mime, name, original)
            SourceUploadedAttachment(key, name, markdown)
        } finally { if (prepared.file != file) prepared.file.delete() }
    } finally { file.delete() }
}
