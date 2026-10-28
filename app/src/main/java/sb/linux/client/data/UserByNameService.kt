package sb.linux.client.data

import android.net.Uri
import sb.linux.client.domain.account.UserLookupRepository
import sb.linux.client.common.error.LsbException
import sb.linux.client.common.network.Endpoints

internal class UserByNameService(private val client: LsbClient) : UserLookupRepository {
    override suspend fun resolve(username: String): Long {
        val response = client.get("/user?username=${Uri.encode(username)}")
        if (response.code !in 200..299) throw LsbException("查找用户失败：${response.code}")
        val uri = Uri.parse(response.url)
        val idInUrl = if (Endpoints.isInternal(uri.host)) {
            USER_ID_IN_URL.matchEntire(uri.path.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
        } else null
        if (idInUrl != null) return idInUrl
        // 未重定向的用户名页面只接受目标用户的资料链接，避免错误页侧栏中的当前账号。
        val document = SourceHtmlSupport.sourceDocument(response.html)
        val nameLink = document.select("a.user-name[href^=/user/]")
            .firstOrNull { it.text().trim() == username.trim() } ?: return 0L
        return USER_ID_IN_URL.matchEntire(Uri.parse(nameLink.attr("href")).path.orEmpty())
            ?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private companion object {
        val USER_ID_IN_URL = Regex("""/user/(\d+)""")
    }
}
