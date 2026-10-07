package sb.linux.client.domain.profile

data class ProfileSettingsField(
    val name: String,
    val type: String,
    val label: String,
    val value: String,
    val required: Boolean = false,
    val options: List<Pair<String, String>> = emptyList(),
)

data class ProfileSettingsForm(
    val id: String,
    val title: String,
    val hasFile: Boolean,
    val fields: List<ProfileSettingsField>,
    val submitText: String,
) {
    val hidden: Map<String, String> get() = fields.filter { it.type == "hidden" }.associate { it.name to it.value }
    val passwords: List<ProfileSettingsField> get() = fields.filter { it.type == "password" }
    val visibles: List<ProfileSettingsField> get() =
        fields.filter { it.type != "hidden" && it.type != "password" && it.type != "file" }
}

data class AvatarPickerData(
    val styleField: String,
    val seedField: String,
    val styles: List<Pair<String, String>>,
    val currentStyle: String,
    val currentSeed: String,
    val base: String,
)

data class ProfileSettingsPage(
    val avatarUrl: String = "",
    val info: List<Pair<String, String>> = emptyList(),
    val forms: List<ProfileSettingsForm> = emptyList(),
    val avatarPicker: AvatarPickerData? = null,
)

data class ProfileSettingsUpload(val path: String, val mimeType: String)

data class ProfileSettingsLoadResult(
    val page: ProfileSettingsPage? = null,
    val error: String? = null,
)

data class ProfileFormSubmissionResult(
    val message: String,
    val shouldRefresh: Boolean,
)

interface ProfileSettingsRepository {
    suspend fun load(): ProfileSettingsLoadResult

    suspend fun submit(
        form: ProfileSettingsForm,
        fields: Map<String, String>,
        upload: ProfileSettingsUpload?,
    ): ProfileFormSubmissionResult
}
