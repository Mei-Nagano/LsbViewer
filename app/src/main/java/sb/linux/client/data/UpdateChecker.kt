package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查更新（3.18 / 3.20）：查询 GitHub Releases 最新版本并与本地版本比较。
 */
object UpdateChecker {

    const val REPO = "Mei-Nagano/LsbViewer"
    const val DEVELOPER_URL = "https://github.com/Mei-Nagano"
    const val PROJECT_URL = "https://github.com/$REPO"
    const val RELEASES_URL = "$PROJECT_URL/releases"
    const val LICENSE_URL = "$PROJECT_URL/blob/main/LICENSE"

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

    /** 拉取最新 Release；无 Release（仓库未公开/未发布）或网络失败返回 null */
    suspend fun latestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
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
    }

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
