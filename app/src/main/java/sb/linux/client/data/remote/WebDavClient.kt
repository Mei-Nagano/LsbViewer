package sb.linux.client.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sb.linux.client.data.AppNetwork
import sb.linux.client.domain.backup.WebDavBackupRepository

/** WebDAV 最小传输客户端：PUT 上传 / GET 下载（Basic Auth）。 */
internal class WebDavClient : WebDavBackupRepository {
    private val client = AppNetwork.clientBuilder().build()

    private fun auth(user: String, pass: String): String = Credentials.basic(user, pass)

    override suspend fun upload(url: String, user: String, password: String, content: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth(user, password))
            .put("application/json; charset=utf-8".toMediaTypeOrNull().let { content.toRequestBody(it) })
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        }
    }

    override suspend fun download(url: String, user: String, password: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth(user, password))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            response.body?.string() ?: throw IllegalStateException("响应为空")
        }
    }
}
