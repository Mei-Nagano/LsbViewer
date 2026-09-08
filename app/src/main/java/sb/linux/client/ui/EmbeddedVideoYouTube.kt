package sb.linux.client.ui

import sb.linux.client.data.Endpoints
import java.net.URI
import java.net.URLEncoder

internal val YOUTUBE_PLAYER_HOSTS = setOf(
    "youtube.com",
    "www.youtube.com",
    "youtube-nocookie.com",
    "www.youtube-nocookie.com",
)
internal val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{6,32}")
internal const val YOUTUBE_EMBED_BASE_URL = "${Endpoints.BASE}/"

/** 保留设备与 Chromium 版本，只移除系统 WebView 专属标记。 */
internal fun browserCompatibleUserAgent(userAgent: String): String = userAgent
    .replace("; wv)", ")")
    .replace(" Version/4.0", "")

/** 补齐 YouTube 要求的站点身份参数，并保留源站给出的播放器选项。 */
internal fun buildYouTubePlayerUrl(source: String, uri: URI): String {
    val query = uri.rawQuery.orEmpty()
    val encodedReferrer = URLEncoder.encode(Endpoints.BASE, "UTF-8")
    val params = buildList {
        if (youtubeQueryValue(query, "origin").isNullOrBlank()) add("origin=$encodedReferrer")
        if (youtubeQueryValue(query, "widget_referrer").isNullOrBlank()) {
            add("widget_referrer=$encodedReferrer")
        }
        if (youtubeQueryValue(query, "playsinline").isNullOrBlank()) add("playsinline=1")
    }
    if (params.isEmpty()) return source
    val fragmentIndex = source.indexOf('#')
    val base = if (fragmentIndex >= 0) source.substring(0, fragmentIndex) else source
    val fragment = if (fragmentIndex >= 0) source.substring(fragmentIndex) else ""
    val separator = if (base.contains('?')) '&' else '?'
    return base + separator + params.joinToString("&") + fragment
}

internal fun buildYouTubeEmbedDocument(playerUrl: String): String {
    val escapedUrl = playerUrl
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>" +
        "html,body{width:100%;height:100%;margin:0;background:#000;overflow:hidden}" +
        "iframe{display:block;width:100%;height:100%;border:0}" +
        "</style></head><body><iframe src=\"$escapedUrl\" " +
        "allow=\"autoplay; encrypted-media; picture-in-picture\" allowfullscreen " +
        "referrerpolicy=\"strict-origin-when-cross-origin\"></iframe></body></html>"
}

private fun youtubeQueryValue(query: String, name: String): String? = query.split('&')
    .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
    .firstOrNull { it[0] == name }
    ?.get(1)
    ?.takeIf(String::isNotBlank)
