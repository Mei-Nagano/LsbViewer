package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

data class LinkPreviewInfo(
    val url: String,
    val title: String,
    val description: String,
    val iconUrl: String,
)

object LinkPreviewClient {
    fun clearCache() { cache.clear() }
    private val cache = ConcurrentHashMap<String, LinkPreviewInfo>()
    private val http = AppNetwork.clientBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(12))
        .followRedirects(true)
        .build()

    suspend fun load(url: String): LinkPreviewInfo = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        val fallback = runCatching {
            val u = URI(url)
            LinkPreviewInfo(url, u.host ?: url, "", "${u.scheme}://${u.authority}/favicon.ico")
        }.getOrElse { LinkPreviewInfo(url, url, "", "") }
        val result = runCatching {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) LsbViewer/1.0")
                .get().build()
            http.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type").orEmpty()
                if (!response.isSuccessful || !contentType.contains("html", ignoreCase = true)) return@use fallback
                val html = response.peekBody(1_000_000).string()
                val doc = Jsoup.parse(html, finalUrl)
                val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
                    ?.trim().orEmpty().ifBlank { doc.title().trim() }.ifBlank { fallback.title }
                val description = doc.selectFirst("meta[property=og:description], meta[name=description]")
                    ?.attr("content")?.trim().orEmpty()
                val icon = doc.selectFirst("link[rel~=icon]")?.attr("abs:href")
                    ?.trim().orEmpty().ifBlank { fallback.iconUrl }
                LinkPreviewInfo(finalUrl, title, description, icon)
            }
        }.getOrDefault(fallback)
        cache[url] = result
        result
    }
}
