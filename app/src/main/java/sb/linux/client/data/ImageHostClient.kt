package sb.linux.client.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Duration

object ImageHostClient {
    const val BUILT_IN_ENDPOINT = "https://catbox.moe/user/api.php"
    private const val BUILT_IN_FIELD = "fileToUpload"
    private const val PIXHOST_ENDPOINT = "https://api.pixhost.to/images"
    private val http = AppNetwork.clientBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .writeTimeout(Duration.ofSeconds(90))
        .readTimeout(Duration.ofSeconds(90))
        .callTimeout(Duration.ofSeconds(120))
        .build()

    suspend fun upload(context: Context, settings: AppSettings, uri: Uri): String = withContext(Dispatchers.IO) {
        val builtIn = settings.useBuiltInImageHost
        val pixhost = builtIn && settings.builtInImageHostProvider == "pixhost"
        val endpoint = if (pixhost) PIXHOST_ENDPOINT else if (builtIn) BUILT_IN_ENDPOINT else settings.imageHostUrl.trim()
        if (endpoint.isBlank()) throw LsbException("请先在常规设置中配置图床上传地址")
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/*"
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (out.size() + count > 20 * 1024 * 1024) throw LsbException("图片不能超过 20 MB")
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        }
            ?: throw LsbException("无法读取所选图片")
        if (bytes.size > 20 * 1024 * 1024) throw LsbException("图片不能超过 20 MB")
        if (pixhost && bytes.size > 10 * 1024 * 1024) throw LsbException("Pixhost 单张图片不能超过 10 MB，可切换至 Catbox 或自定义图床")
        val filename = runCatching {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }?.replace(Regex("[\\r\\n\\\"]"), "_")
            ?: "upload.${android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"}"
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        if (pixhost) multipart.addFormDataPart("content_type", "0").addFormDataPart("max_th_size", "420")
        else if (builtIn) multipart.addFormDataPart("reqtype", "fileupload")
        val body = multipart.addFormDataPart(
                if (pixhost) "img" else if (builtIn) BUILT_IN_FIELD else settings.imageHostField.ifBlank { "file" }, filename,
                bytes.toRequestBody(mime.toMediaTypeOrNull()),
            ).build()
        val request = Request.Builder().url(endpoint).header("Accept", "application/json, text/plain, */*")
            .header("User-Agent", "LinuxSB/Android ImageUploader").post(body).apply {
            settings.imageHostToken.trim().takeIf { !builtIn && it.isNotBlank() }?.let {
                header("Authorization", if (it.startsWith("Bearer ", true)) it else "Bearer $it")
            }
        }.build()
        http.newCall(request).execute().use { response ->
            val text = response.peekBody(1_000_000).string().trim()
            if (!response.isSuccessful) throw LsbException("图床上传失败 ${response.code}：${text.take(180)}")
            val rawUrl = if (pixhost) {
                val showUrl = JSONObject(text).optString("show_url")
                check(URI(showUrl).host in setOf("pixhost.cc", "pixhost.to", "pixho.st")) { "图床返回了无效的图片页面" }
                http.newCall(Request.Builder().url(showUrl).build()).execute().use { page ->
                    check(page.isSuccessful) { "图片已上传，但读取原图链接失败" }
                    val doc = org.jsoup.Jsoup.parse(page.peekBody(2_000_000).string(), showUrl)
                    doc.selectFirst("img#image")?.absUrl("src")?.takeIf { it.isNotBlank() }
                        ?: doc.selectFirst("meta[property=og:image]")?.absUrl("content")?.takeIf { it.isNotBlank() }
                }
            } else extractUrl(text)
            if (rawUrl.isNullOrBlank()) throw LsbException("上传完成，但未获取到原图地址")
            runCatching { URI(endpoint).resolve(rawUrl).toString() }.getOrDefault(rawUrl)
        }
    }

    private fun extractUrl(text: String): String? {
        if (text.startsWith("http://") || text.startsWith("https://")) return text.lineSequence().first().trim()
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        fun find(value: Any?): String? = when (value) {
            is JSONObject -> {
                listOf("url", "link", "src", "image", "display_url").firstNotNullOfOrNull { key ->
                    value.optString(key).takeIf { it.startsWith("http") || it.startsWith("/") }
                } ?: listOf("data", "result", "image", "file").firstNotNullOfOrNull { key -> find(value.opt(key)) }
            }
            is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { find(value.opt(it)) }
            is String -> value.takeIf { it.startsWith("http") || it.startsWith("/") }
            else -> null
        }
        return find(root)
    }
}
