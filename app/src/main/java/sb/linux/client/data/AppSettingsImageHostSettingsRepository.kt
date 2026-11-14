package sb.linux.client.data

import sb.linux.client.domain.media.ImageHostSettings
import sb.linux.client.domain.media.ImageHostSettingsRepository
import sb.linux.client.domain.media.ImageUploadMethod

internal class AppSettingsImageHostSettingsRepository(
    private val settings: AppSettings,
) : ImageHostSettingsRepository {
    override fun current() = ImageHostSettings(
        settings.useBuiltInImageHost,
        settings.builtInImageHostProvider,
        settings.imageHostUrl,
        settings.imageHostToken,
        settings.imageHostField,
        ImageUploadMethod.fromValue(settings.imageUploadMethod),
        settings.askImageUploadMethod,
    )

    override fun setBuiltInEnabled(enabled: Boolean) {
        settings.useBuiltInImageHost = enabled
    }

    override fun saveUploadPreference(method: ImageUploadMethod, ask: Boolean) {
        settings.imageUploadMethod = method.value
        settings.askImageUploadMethod = ask
        if (method != ImageUploadMethod.ATTACHMENT_SPACE) settings.useBuiltInImageHost = method == ImageUploadMethod.BUILT_IN
    }

    override fun setProvider(provider: String) {
        settings.builtInImageHostProvider = provider
    }

    override fun saveCustom(url: String, token: String, fileField: String) {
        settings.imageHostUrl = url
        settings.imageHostToken = token
        settings.imageHostField = fileField
    }
}
