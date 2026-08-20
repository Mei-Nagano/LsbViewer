package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查更新（3.18 / 3.20）：优先经 Cloudflare Worker 代理查询 GitHub 最新 Release
 * （规避官方 API 60 次/小时/IP 的速率限制与国内无法直连 api.github.com 的问题），
 * Worker 不可用时回退直连官方 API。与本地版本比较判断是否有新版。
 */
object UpdateChecker {

    const val REPO = "Mei-Nagano/LsbViewer"
    const val DEVELOPER_URL = "https://github.com/Mei-Nagano"
    const val PROJECT_URL = "https://github.com/$REPO"
    const val RELEASES_URL = "$PROJECT_URL/releases"
    const val LICENSE_URL = "$PROJECT_URL/blob/main/LICENSE"

    // 检查更新代理（worker/update-worker.js 部署在自定义域名，
    // workers.dev 域名在国内被污染不可用）；不可用时自动回退 GitHub API
    private const val WORKER_URL = "https://vercheck.060510.xyz"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val tag: String,
        val name: String,
        val url: String,
        val notes: String,
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 拉取最新 Release：Worker 优先，失败回退 GitHub API；均失败或无 Release 返回 null */
    suspend fun latestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        fetchFrom(WORKER_URL) ?: fetchFrom(GITHUB_API_URL)
    }

    /** 请求指定端点并解析（Worker 与官方 API 返回同构字段：tag_name/name/html_url/body） */
    private fun fetchFrom(url: String): UpdateInfo? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            val body = resp.body?.string() ?: return@runCatching null
            val o = JSONObject(body)
            val tag = o.optString("tag_name")
            if (tag.isBlank()) return@runCatching null
            UpdateInfo(
                tag = tag,
                name = o.optString("name").ifBlank { tag },
                url = o.optString("html_url").ifBlank { RELEASES_URL },
                notes = o.optString("body"),
            )
        }
    }.getOrNull()

    /** 比较版本号（忽略 v 前缀，按点分段数字逐段比较）：latest > current 时为 true */
    fun isNewer(latest: String, current: String): Boolean {
        fun norm(s: String) = s.trim()
            .removePrefix("v").removePrefix("V")
            .substringBefore("-").substringBefore("+")
            .split(".").mapNotNull { it.toIntOrNull() }
        val a = norm(latest)
        val b = norm(current)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i) ?: 0
            val y = b.getOrNull(i) ?: 0
            if (x != y) return x > y
        }
        return false
    }
}
