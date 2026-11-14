package sb.linux.client.domain.account

/** 源站用户组与称号分别建模；调色类别和内联变量保持源站提供的值。 */
data class UserGroupBadge(val name: String, val palette: String = "slate", val sourceStyle: String = "")
