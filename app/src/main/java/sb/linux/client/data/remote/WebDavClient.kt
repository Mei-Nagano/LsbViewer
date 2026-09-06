package sb.linux.client.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sb.linux.client.data.AppNetwork

/** WebDAV 最小传输客户端：PUT 上传 / GET 下载（Basic Auth）。 */
object WebDavClient {
    private val client = AppNetwork.clientBuilder().build()

    private fun auth(user: String, pass: String): String = Credentials.basic(user, pass)

    suspend fun put(url: String, user: String, pass: String, body: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth(user, pass))
            .put("application/json; charset=utf-8".toMediaTypeOrNull().let { body.toRequestBody(it) })
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        }
    }

    suspend fun get(url: String, user: String, pass: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth(user, pass))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            response.body?.string() ?: throw IllegalStateException("响应为空")
        }
    }
}
