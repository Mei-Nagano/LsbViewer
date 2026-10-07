package sb.linux.client.domain.community

import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.domain.account.SessionSnapshot

data class HomeSessionObservation(
    val snapshot: SessionSnapshot?,
    val keywordPolicy: KeywordFilterPolicy,
)
