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

import sb.linux.client.common.error.LsbException
import sb.linux.client.domain.ai.AiAssistantConfig
import sb.linux.client.domain.ai.AiAssistantDefaults
import sb.linux.client.domain.ai.AiAssistantRepository

/**
 * OpenAI 兼容 API 客户端：自定义 API 地址 + 密钥 + 模型 + 温度，
 * 用于帖子 AI 总结。
 */
object AiClient : AiAssistantRepository {
    private val http: OkHttpClient by lazy {
        AppNetwork.clientBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(120))
            .build()
    }

    /**
     * 对帖子内容做总结。
     * @param content 楼层纯文本（楼主 + 评论）
     */
    override suspend fun summarize(
        config: AiAssistantConfig,
        title: String,
        content: String,
        onStatus: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        if (!config.isConfigured) throw LsbException("请先在应用设置中配置 AI API")
        val base = config.apiUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put(
                "content", config.prompt.trim().ifBlank { AiAssistantDefaults.SUMMARY_PROMPT }
            ))
            put(JSONObject().put("role", "user").put(
                "content",
                "帖子标题：$title\n\n帖子内容：\n${content.take(6000)}"
            ))
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("temperature", config.temperature)
            .put("messages", messages)

        onStatus("请求 ${JSONObject().put("model", config.model)} 中…")
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
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

    /** 从 OpenAI 兼容的 /models 端点读取可用模型。 */
    override suspend fun fetchModels(apiUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        if (apiUrl.isBlank()) throw LsbException("请先填写 API 地址")
        val base = apiUrl.trimEnd('/').removeSuffix("/chat/completions")
        val request = Request.Builder().url("$base/models")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .get().build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw LsbException("模型列表请求失败 ${response.code}：${text.take(160)}")
            val data = JSONObject(text).optJSONArray("data") ?: return@use emptyList()
            (0 until data.length()).mapNotNull { data.optJSONObject(it)?.optString("id")?.takeIf(String::isNotBlank) }
                .distinct().sorted()
        }
    }
}
