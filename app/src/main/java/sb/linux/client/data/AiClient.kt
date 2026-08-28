package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration

/**
 * OpenAI 兼容 API 客户端：自定义 API 地址 + 密钥 + 模型 + 温度，
 * 用于帖子 AI 总结。
 */
object AiClient {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(120))
            .build()
    }

    fun isConfigured(s: AppSettings): Boolean =
        s.aiUrl.isNotBlank() && s.aiModel.isNotBlank()

    /**
     * 对帖子内容做总结。
     * @param content 楼层纯文本（楼主 + 评论）
     */
    suspend fun summarize(
        s: AppSettings,
        title: String,
        content: String,
        onStatus: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured(s)) throw LsbException("请先在应用设置中配置 AI API")
        val base = s.aiUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put(
                "content",
                "你是一个论坛帖子总结助手。请用简体中文对帖子做简洁总结：先一句话概括主题，" +
                    "再分点列出关键信息（如有重要讨论、结论或争议请说明）。控制在 200 字以内。\n" +
                    "输出使用 Markdown：分点用 `- `，需要强调用 `**加粗**`，可用 `### ` 小标题。" +
                    "不要用表格，不要输出思考过程，不要把整段回答包在代码块里。"
            ))
            put(JSONObject().put("role", "user").put(
                "content",
                "帖子标题：$title\n\n帖子内容：\n${content.take(6000)}"
            ))
        }
        val body = JSONObject()
            .put("model", s.aiModel)
            .put("temperature", s.aiTemp.toDouble())
            .put("messages", messages)

        onStatus("请求 ${JSONObject().put("model", s.aiModel)} 中…")
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer ${s.aiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { r ->
            val text = r.body?.string() ?: ""
            if (!r.isSuccessful) {
                throw LsbException("AI 接口错误 ${r.code}：${text.take(160)}")
            }
            val json = JSONObject(text)
            json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
}
