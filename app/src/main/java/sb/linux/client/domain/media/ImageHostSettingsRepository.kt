package sb.linux.client.domain.media

data class ImageHostSettings(
    val useBuiltIn: Boolean,
    val provider: String,
    val url: String,
    val token: String,
    val fileField: String,
)

interface ImageHostSettingsRepository {
    fun current(): ImageHostSettings
    fun setBuiltInEnabled(enabled: Boolean)
    fun setProvider(provider: String)
    fun saveCustom(url: String, token: String, fileField: String)
}
