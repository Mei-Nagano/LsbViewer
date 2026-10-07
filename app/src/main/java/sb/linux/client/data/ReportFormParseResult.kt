package sb.linux.client.data

import sb.linux.client.domain.community.ReportForm

internal data class ReportFormParseResult(
    val form: ReportForm,
    val targetType: String,
    val targetId: String,
)
