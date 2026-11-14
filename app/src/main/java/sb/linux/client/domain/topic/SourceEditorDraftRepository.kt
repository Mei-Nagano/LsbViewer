package sb.linux.client.domain.topic

data class SourceEditorDraft(
    val id: String,
    val userId: Long,
    val topicId: Long,
    val updatedAt: Long,
    val extensionName: String,
    val fields: List<Pair<String, String>>,
    val automatic: Boolean,
) {
    val title: String get() = fields.firstOrNull { it.first == "title" }?.second.orEmpty()
    val body: String get() = fields.firstOrNull { it.first == "body" }?.second.orEmpty()
}

interface SourceEditorDraftRepository {
    fun list(userId: Long, topicId: Long): List<SourceEditorDraft>
    fun save(draft: SourceEditorDraft)
    fun remove(userId: Long, id: String)
}
