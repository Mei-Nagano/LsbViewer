package sb.linux.client.ui.navigation

import android.net.Uri
import sb.linux.client.common.network.Endpoints

/** 仅识别原站的外链中转；原始签名 URL 仍用于打开跳转提醒。 */
internal object SourceExternalJump {
    fun target(url: String): String? = runCatching {
        val uri = Uri.parse(url)
        if (!Endpoints.isInternal(uri.host) || uri.path?.trimEnd('/') != "/jump") return null
        val target = uri.getQueryParameter("to")?.trim().orEmpty()
        val destination = Uri.parse(target)
        target.takeIf {
            destination.scheme?.lowercase() in setOf("https", "http") && !destination.host.isNullOrBlank()
        }
    }.getOrNull()
}
