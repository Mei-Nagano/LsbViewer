package sb.linux.client.data

import sb.linux.client.domain.topic.NewTopicFormData
import sb.linux.client.domain.topic.NewTopicRepository
import sb.linux.client.domain.topic.NewTopicSubmission

internal class NewTopicPageService(
    private val formLoader: NewTopicFormLoader,
    private val submitter: NewTopicSubmitter,
) : NewTopicRepository {
    override suspend fun load(editId: Long): NewTopicFormData = formLoader.load(editId)
    override suspend fun submit(input: NewTopicSubmission): String? = submitter.submit(input)
}
