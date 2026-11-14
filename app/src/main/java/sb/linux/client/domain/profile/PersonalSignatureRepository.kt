package sb.linux.client.domain.profile

data class PersonalSignaturePage(
    val signature: String,
    val hidden: Boolean,
    val maxLength: Int?,
    val description: String,
    val signatureHint: String,
    val hiddenHint: String,
)

interface PersonalSignatureRepository {
    suspend fun load(): PersonalSignaturePage
    suspend fun save(signature: String, hidden: Boolean): PersonalSignaturePage
}
