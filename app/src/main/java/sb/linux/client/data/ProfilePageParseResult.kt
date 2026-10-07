package sb.linux.client.data

import sb.linux.client.domain.profile.ProfileSettingsPage

internal data class ProfilePageParseResult(
    val page: ProfileSettingsPage,
    val sourceForms: Map<String, ProfileSourceForm>,
)

internal data class ProfileSourceForm(
    val action: String,
    val hasFile: Boolean,
    val fileFieldName: String,
)

internal data class ParsedProfileForm(
    val form: sb.linux.client.domain.profile.ProfileSettingsForm,
    val source: ProfileSourceForm,
)
