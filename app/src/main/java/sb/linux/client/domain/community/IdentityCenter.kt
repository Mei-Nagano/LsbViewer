package sb.linux.client.domain.community

data class IdentityRequirement(val label: String, val met: Boolean)

data class IdentityCenterData(
    val benefits: List<Pair<String, String>> = emptyList(),
    val creatorRequirements: List<IdentityRequirement> = emptyList(),
    val accountRequirements: List<IdentityRequirement> = emptyList(),
    val email: String = "",
    val emailVerified: Boolean = false,
    val canApply: Boolean = false,
    val applicationRecords: List<String> = emptyList(),
    val notice: String = "",
)

interface IdentityRepository {
    suspend fun loadIdentity(): IdentityCenterData
}
