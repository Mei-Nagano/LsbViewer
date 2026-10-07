package sb.linux.client.domain.topic

data class DonationForm(
    val quickMessages: List<String>,
    val unavailableReason: String = "",
)
