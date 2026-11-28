package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.sticker.StickerCollectConfig
import sb.linux.client.domain.sticker.StickerPickerConfig

internal object StickerPackHtmlParser {
    private val RASTER_EXT = Regex("""\.(?:avif|bmp|gif|jpe?g|png|webp)(?:$|[?#])""", RegexOption.IGNORE_CASE)
    private val SVG_URL = Regex("""(?:^data:image/svg\+xml|\.svg(?:$|[?#]))""", RegexOption.IGNORE_CASE)
    private val AVATAR_URL = Regex("""/(?:avatar|avatar_upload)(?:[/_-]|$)""", RegexOption.IGNORE_CASE)

    fun parsePicker(container: Element?, fallbackCsrf: String = ""): StickerPickerConfig? {
        if (container == null) return null
        val button = container.selectFirst("[data-sticker-pack-picker]") ?: return null
        val pickerUrl = button.attr("data-sticker-pack-picker").trim()
        if (pickerUrl.isBlank()) return null
        val allowedForums = button.attr("data-sticker-pack-forums")
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (button.hasAttr("hidden") && allowedForums.isEmpty()) return null
        val csrf = container.selectFirst("input[name=_csrf]")?.attr("value")?.trim().orEmpty()
            .ifBlank { fallbackCsrf }
        val recordUrl = button.attr("data-sticker-pack-record").trim()
        val manageUrl = button.attr("data-sticker-pack-manage").trim()
        return StickerPickerConfig(
            pickerUrl = Endpoints.abs(pickerUrl),
            recordUrl = if (recordUrl.isNotBlank()) Endpoints.abs(recordUrl) else "",
            manageUrl = if (manageUrl.isNotBlank()) Endpoints.abs(manageUrl) else "",
            forumId = button.attr("data-sticker-pack-forum").trim(),
            allowedForums = allowedForums,
            userId = button.attr("data-sticker-pack-user").trim(),
            context = button.attr("data-sticker-pack-context").trim(),
            hideMine = button.hasAttr("data-sticker-pack-hide-mine"),
            csrf = csrf,
        )
    }

    fun parseCollect(document: Element): StickerCollectConfig? {
        val el = document.selectFirst("[data-sticker-pack-collect]") ?: return null
        val collectUrl = el.attr("data-sticker-pack-collect").trim()
        val csrf = el.attr("data-sticker-pack-csrf").trim()
        if (collectUrl.isBlank() || csrf.isBlank()) return null
        return StickerCollectConfig(
            collectUrl = Endpoints.abs(collectUrl),
            csrf = csrf,
        )
    }

    fun isStickerImageUrl(url: String): Boolean =
        url.contains("/app/upload/sticker_pack/", ignoreCase = true)

    fun isCollectableImageUrl(url: String): Boolean {
        val normalized = url.trim()
        if (normalized.isBlank()) return false
        if (!normalized.startsWith("http://", ignoreCase = true) &&
            !normalized.startsWith("https://", ignoreCase = true)
        ) return false
        if (SVG_URL.containsMatchIn(normalized) || AVATAR_URL.containsMatchIn(normalized)) return false
        return RASTER_EXT.containsMatchIn(normalized)
    }
}
