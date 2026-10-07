package sb.linux.client.data

import sb.linux.client.domain.topic.DonationForm

internal data class DonateFormParseResult(
    val form: DonationForm,
    val csrf: String,
    val topicId: String,
    val requestKey: String,
)
