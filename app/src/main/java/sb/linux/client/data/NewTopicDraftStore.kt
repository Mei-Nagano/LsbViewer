package sb.linux.client.data

import sb.linux.client.domain.topic.NewTopicDraft
import sb.linux.client.domain.topic.NewTopicDraftRepository

/** 旧编辑器的兼容空实现；实际编辑、保存和恢复草稿由原站负责。 */
internal class NewTopicDraftStore : NewTopicDraftRepository {
    override fun save(draft: NewTopicDraft) = Unit
    override fun load() = NewTopicDraft("", "", "", null)
    override fun hasRestorableDraft() = false
    override fun hasSavedDraft() = false
    override fun clear() = Unit
}
