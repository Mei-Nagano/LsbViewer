package sb.linux.client.data

import android.content.SharedPreferences

internal class AppSettingsUploadOptionsStore(private val prefs: SharedPreferences) {
    var builtInImageHostProvider: String
        get() = prefs.getString("builtin_image_provider", "pixhost") ?: "pixhost"
        set(value) = prefs.edit().putString("builtin_image_provider", value).apply()

    var confirmGachaPull: Boolean
        get() = prefs.getBoolean("confirm_gacha_pull", true)
        set(value) = prefs.edit().putBoolean("confirm_gacha_pull", value).apply()
}
