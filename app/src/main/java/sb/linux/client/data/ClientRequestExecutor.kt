package sb.linux.client.data
import sb.linux.client.common.network.Endpoints

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import sb.linux.client.common.error.LsbException
import sb.linux.client.data.LsbClient.Resp
import sb.linux.client.domain.topic.DanmakuItem

internal class ClientRequestExecutor(
    private val http: OkHttpClient,
    private val userAgent: String,
    private val verificationRequired: (Int, String) -> Boolean,
    private val challengeDetected: (String) -> Boolean,
    private val solveVerification: suspend (String, String) -> Boolean,
    private val recordCsrf: (String) -> Unit,
) {    suspend fun get(path: String): Resp = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", userAgent)
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (verificationRequired(resp.code, resp.html)) {
                    // 取消/超时即放弃：否则同一个验证对话框会被 repeat(3) 连弹三次
                    if (!solveVerification(resp.url, resp.html)) throw LsbException("已取消人机验证")
                } else {
                    if (resp.code !in 200..299) throw LsbException("原站请求失败（HTTP ${resp.code}），请稍后重试")
                    if (resp.html.isBlank()) throw LsbException("原站返回空页面，请重试")
                    recordCsrf(resp.html)
                    return@withContext resp
                }
            }
        }
        throw LsbException("访问验证恢复失败")
    }

    /** 请求首页触发一次 UAM，成功后头像/图片后续即可带上会话 Cookie */
    suspend fun ensureAccess() {
        get("/")
    }

    /** 同步读取头像内存缓存，不执行网络或磁盘 I/O。 */
    suspend fun postForm(path: String, form: Map<String, String>): Resp =
        postFormPairs(path, form.entries.map { it.key to it.value })

    /** 键值对表单提交（支持重复键：发帖的奖品数组字段 lottery_prize_name[] 等需要） */
    suspend fun postFormPairs(path: String, pairs: List<Pair<String, String>>): Resp = withContext(Dispatchers.IO) {
        val b = FormBody.Builder()
        pairs.forEach { (k, v) -> b.add(k, v) }
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", userAgent)
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (verificationRequired(resp.code, resp.html)) {
                    // 取消/超时即放弃：否则同一个验证对话框会被 repeat(3) 连弹三次
                    if (!solveVerification(resp.url, resp.html)) throw LsbException("已取消人机验证")
                } else {
                    recordCsrf(resp.html)
                    return@withContext resp
                }
            }
        }
        throw LsbException("访问失败")
    }

    /** AJAX 提交，返回 JSON 文本 */
    suspend fun postAjax(path: String, form: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val b = FormBody.Builder()
        form.forEach { (k, v) -> b.add(k, v) }
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", userAgent)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                val trimmed = text.trim()
                val json = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    runCatching { JSONObject(trimmed) }.getOrNull()
                } else {
                    null
                }
                if (r.request.url.encodedPath == "/login" || r.code == 401) {
                    throw LsbException("AUTH_REQUIRED")
                }
                // 403/503 的 JSON 是业务/CSRF 错误，不应弹出全屏人机验证并拦截应用导航。
                if (challengeDetected(text) || (json == null && (r.code == 403 || r.code == 503))) {
                    if (!solveVerification(r.request.url.toString(), text)) throw LsbException("已取消人机验证")
                } else {
                    return@withContext json ?: JSONObject(if (text.isBlank()) "{}" else text)
                }
            }
        }
        throw LsbException("接口访问失败")
    }

    /** multipart 表单提交（头像上传等文件场景） */
    suspend fun postMultipart(
        path: String,
        fields: Map<String, String>,
        fileField: String,
        file: File,
        mime: String,
    ): Resp = withContext(Dispatchers.IO) {
        val b = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
        fields.forEach { (k, v) -> b.addFormDataPart(k, v) }
        val media = mime.toMediaTypeOrNull() ?: "image/jpeg".toMediaType()
        b.addFormDataPart(fileField, file.name, okhttp3.RequestBody.create(media, file))
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", userAgent)
            .post(b.build())
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val resp = Resp(r.request.url.toString(), r.body?.string() ?: "", r.code)
                if (verificationRequired(resp.code, resp.html)) {
                    // 取消/超时即放弃：否则同一个验证对话框会被 repeat(3) 连弹三次
                    if (!solveVerification(resp.url, resp.html)) throw LsbException("已取消人机验证")
                } else {
                    return@withContext resp
                }
            }
        }
        throw LsbException("上传访问失败")
    }

    /** AJAX GET，返回 JSON 文本（失败/HTML 时抛 LsbException） */
    internal suspend fun getAjaxJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(Endpoints.abs(path))
            .header("User-Agent", userAgent)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .build()
        repeat(3) {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                val t = text.trim()
                if (r.request.url.encodedPath == "/login" || r.code == 401) {
                    throw LsbException("AUTH_REQUIRED")
                }
                if (challengeDetected(text)) {
                    if (!solveVerification(r.request.url.toString(), text)) throw LsbException("已取消人机验证")
                } else {
                    if (t.startsWith("{") || t.startsWith("[")) return@withContext JSONObject(t)
                    throw LsbException("接口未返回 JSON")
                }
            }
        }
        throw LsbException("接口访问失败")
    }

    // ---------------- 打赏弹幕 ----------------

    /** 拉取帖子打赏弹幕（源站 /donate_feed AJAX 接口） */
    suspend fun getDonateFeed(topicId: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(Endpoints.abs("/donate_feed?topic_id=$topicId&last_id=0&bootstrap=1"))
                .header("User-Agent", userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json")
                .build()
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: ""
                val json = JSONObject(if (text.isBlank()) "{}" else text)
                DonateBarrageHtmlParser.parseDonateBarrage(json.optString("area_html"))
            }
        }.getOrDefault(emptyList())
    }

}

