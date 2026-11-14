package sb.linux.client.domain.topic

import sb.linux.client.domain.account.AccountForm

data class SourceEditorExtension(val label: String, val toggle: Pair<String, String>,
    val form: AccountForm, val selected: Boolean, val unavailableReason: String = "")
data class SourceEditorPage(val url: String, val form: AccountForm,
    val extensions: List<SourceEditorExtension>, val mentionUrl: String, val uploadUrl: String,
    val maxUploadBytes: Long, val acceptedFiles: String, val previewUrl: String = "",
    val videoUrl: String = "", val editorKind: String = "topic", val topicId: String = "0", val remoteImageUrl: String = "") {
    val media: SourceEditorMedia get() = SourceEditorMedia(uploadUrl,
        form.hidden.firstOrNull { it.first == "_csrf" }?.second.orEmpty(), maxUploadBytes, acceptedFiles, remoteImageUrl)
}
data class SourceEditorMedia(val uploadUrl: String = "", val csrf: String = "",
    val maxUploadBytes: Long = 20L * 1024 * 1024, val acceptedFiles: String = "", val remoteImageUrl: String = "")
data class MentionUser(val username: String, val group: String, val avatarUrl: String)

interface SourceEditorRepository {
    suspend fun load(url: String): SourceEditorPage
    suspend fun submit(form: AccountForm, fields: List<Pair<String, String>>): Long
    suspend fun mentions(url: String, query: String): List<MentionUser>
    suspend fun upload(url: String, csrf: String, file: java.io.File, mime: String, originalName: String, original: Boolean = true): String
    suspend fun preview(url: String, csrf: String, body: String, kind: String, topicId: String): String
    suspend fun video(url: String, csrf: String, share: String, kind: String): String
    suspend fun remoteImage(url: String, csrf: String, imageUrl: String): SourceRemoteImage
    suspend fun poll(url: String): TopicPoll
}

data class SourceRemoteImage(val bytes: ByteArray, val mime: String)
