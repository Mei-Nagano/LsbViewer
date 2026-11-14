package sb.linux.client.domain.media

enum class ImageUploadMethod(val value: String, val label: String) {
    ATTACHMENT_SPACE("attachment_space", "附件空间"),
    BUILT_IN("built_in", "内置匿名图床"),
    CUSTOM("custom", "自定义接口");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: ATTACHMENT_SPACE
    }
}

data class ImageHostSettings(
    val useBuiltIn: Boolean,
    val provider: String,
    val url: String,
    val token: String,
    val fileField: String,
    val uploadMethod: ImageUploadMethod = ImageUploadMethod.ATTACHMENT_SPACE,
    val askUploadMethod: Boolean = true,
)

interface ImageHostSettingsRepository {
    fun current(): ImageHostSettings
    fun setBuiltInEnabled(enabled: Boolean)
    fun setProvider(provider: String)
    fun saveCustom(url: String, token: String, fileField: String)
    fun saveUploadPreference(method: ImageUploadMethod, ask: Boolean) { error("当前设置不支持上传方式选择") }
}
