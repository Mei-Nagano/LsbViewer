package sb.linux.client.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.sticker.StickerCollectConfig
import sb.linux.client.domain.sticker.StickerCollectResult
import sb.linux.client.domain.sticker.StickerItem
import sb.linux.client.domain.sticker.StickerPackRepository
import sb.linux.client.domain.sticker.StickerPage
import sb.linux.client.domain.sticker.StickerPickerConfig
import sb.linux.client.domain.sticker.StickerScope
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class StickerPackService(
    context: Context,
    private val client: LsbClient,
) : StickerPackRepository {
    private val prefs = context.getSharedPreferences("lsb_sticker_pack", Context.MODE_PRIVATE)
    private val recentMutex = Mutex()

    override fun savedScope(userId: String): StickerScope? {
        val raw = prefs.getString(tabKey(userId), null)
        return StickerScope.fromQuery(raw)
    }

    override fun saveScope(userId: String, scope: StickerScope) {
        prefs.edit().putString(tabKey(userId), scope.queryValue).apply()
    }

    override suspend fun loadPage(
        config: StickerPickerConfig,
        scope: StickerScope,
        query: String,
        page: Int,
    ): StickerPage {
        check(config.enabled) { "当前页面不支持表情包" }
        if (scope == StickerScope.RECENT) {
            // 与源站 pendingRecent 语义一致：等最近使用记录上报完成后再拉取列表
            recentMutex.withLock { }
        }
        val uri = Uri.parse(Endpoints.abs(config.pickerUrl)).buildUpon().apply {
            appendQueryParameter("forum_id", config.forumId)
            if (config.isDirectMessages) appendQueryParameter("context", config.context)
            appendQueryParameter("scope", scope.queryValue)
            appendQueryParameter("q", query.trim())
            appendQueryParameter("p", page.coerceAtLeast(1).toString())
        }.build().toString()
        val json = client.getAjaxJsonForRepository(uri)
        val array = json.optJSONArray("items")
        val items = if (array == null) emptyList() else (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val url = obj.optString("url").trim()
            val markdown = obj.optString("markdown")
            if (url.isBlank() || markdown.isBlank()) return@mapNotNull null
            StickerItem(
                id = obj.optString("id").trim(),
                title = obj.optString("title").trim(),
                url = Endpoints.abs(url),
                width = obj.optInt("width", 64).coerceAtLeast(16),
                markdown = markdown,
            )
        }
        return StickerPage(items = items, more = json.optBoolean("more", false))
    }

    override suspend fun recordUsage(config: StickerPickerConfig, stickerId: String) {
        if (config.recordUrl.isBlank() || stickerId.isBlank()) return
        recentMutex.withLock {
            runCatching {
                val csrf = config.csrf.ifBlank { client.csrf() }
                if (csrf.isBlank()) return@runCatching
                val form = buildMap {
                    put("_csrf", csrf)
                    put("forum_id", config.forumId)
                    put("sticker_id", stickerId)
                    if (config.isDirectMessages) put("context", config.context)
                }
                client.postForm(config.recordUrl, form)
            }
        }
    }

    override suspend fun collectImage(
        config: StickerCollectConfig,
        imageUrl: String,
        title: String,
    ): StickerCollectResult = withContext(Dispatchers.IO) {
        if (!config.enabled) return@withContext StickerCollectResult(false, "当前页面不支持收藏表情")
        val targetUrl = Endpoints.abs(imageUrl)
        val raw = try {
            client.fetchBytes(targetUrl)
        } catch (_: Exception) {
            return@withContext StickerCollectResult(false, "无法读取这张图片")
        }
        val prepared = prepareStickerUpload(raw)
            ?: return@withContext StickerCollectResult(false, "无法转换这张图片")
        if (prepared.error != null) return@withContext StickerCollectResult(false, prepared.error)

        val response = try {
            client.postMultipartBytes(
                path = config.collectUrl,
                fields = mapOf("_csrf" to config.csrf, "title" to title.trim()),
                fileField = "image",
                fileName = "sticker",
                bytes = prepared.bytes,
                mime = prepared.mime,
                ajax = true,
            )
        } catch (_: Exception) {
            return@withContext StickerCollectResult(false, "无法添加这张图片，请重试")
        }
        val json = runCatching { JSONObject(response.html) }.getOrNull()
        val ok = response.code in 200..299 && (json?.optBoolean("ok") == true || json?.optInt("ok") == 1)
        val message = json?.optString("message")?.trim().orEmpty()
            .ifBlank { if (ok) "已添加到我的表情" else "未能添加，请重试" }
        StickerCollectResult(ok, message)
    }

    private data class PreparedSticker(
        val bytes: ByteArray = ByteArray(0),
        val mime: String = "",
        val error: String? = null,
    )

    private fun prepareStickerUpload(raw: ByteArray): PreparedSticker? {
        if (raw.isEmpty()) return null
        val mime = detectMime(raw)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        val maxEdge = max(width, height)

        // 与源站 image_lightbox.collectImageFile 一致：
        // png/jpeg/gif/webp 且 <= 2MB 且最大边 <= 2048px 直接上传原图（保留动图）。
        if (mime in PASSTHROUGH_MIMES && raw.size <= MAX_PASSTHROUGH_BYTES && width > 0 && height > 0 && maxEdge <= MAX_PASSTHROUGH_EDGE) {
            return PreparedSticker(raw, mime)
        }
        if (mime == "image/gif") {
            return PreparedSticker(error = "请先缩小 GIF 后再收藏，以保留动画")
        }

        val decodeOpts = BitmapFactory.Options().apply {
            if (maxEdge > RESIZE_TARGET_EDGE * 2) {
                inSampleSize = Integer.highestOneBit(maxEdge / RESIZE_TARGET_EDGE).coerceAtLeast(1)
            }
        }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, decodeOpts) ?: return null
        var scaled: Bitmap? = null
        try {
            val edge = max(decoded.width, decoded.height).coerceAtLeast(1)
            val scale = min(1f, RESIZE_TARGET_EDGE.toFloat() / edge.toFloat())
            val outW = max(1, (decoded.width * scale).roundToInt())
            val outH = max(1, (decoded.height * scale).roundToInt())
            val target = if (outW != decoded.width || outH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, outW, outH, true).also { scaled = it }
            } else decoded
            val output = ByteArrayOutputStream()
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            if (!target.compress(format, 90, output)) return null
            return PreparedSticker(output.toByteArray(), "image/webp")
        } finally {
            if (scaled != null && scaled !== decoded) scaled?.recycle()
            decoded.recycle()
        }
    }

    private fun detectMime(bytes: ByteArray): String = when {
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 6 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte() -> "image/gif"
        bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte() -> "image/webp"
        else -> ""
    }

    private fun tabKey(userId: String) = "sticker_pack_tab:$userId"

    companion object {
        private const val MAX_PASSTHROUGH_BYTES = 2 * 1024 * 1024 // 2097152
        private const val MAX_PASSTHROUGH_EDGE = 2048
        private const val RESIZE_TARGET_EDGE = 512
        private val PASSTHROUGH_MIMES = setOf("image/png", "image/jpeg", "image/gif", "image/webp")
    }
}
