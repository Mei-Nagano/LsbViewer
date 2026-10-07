package sb.linux.client.domain.media

data class ImageHostConfig(
    val useBuiltInHost: Boolean,
    val builtInProvider: String,
    val endpoint: String,
    val fieldName: String,
    val token: String,
)

interface ImageUploadRepository {
    suspend fun upload(uri: String, config: ImageHostConfig): String
}
