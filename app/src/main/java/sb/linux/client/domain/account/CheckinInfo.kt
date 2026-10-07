package sb.linux.client.domain.account

data class CheckinInfo(
    val streak: Int = 0,
    val total: Int = 0,
    val canCheckin: Boolean,
    val checkedToday: Boolean,
    val message: String = "",
)
