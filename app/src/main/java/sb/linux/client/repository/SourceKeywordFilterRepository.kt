package sb.linux.client.repository

import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.api.KeywordFilterApi
import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.common.filter.KeywordFilterRemoteException
import sb.linux.client.data.LsbClient
import sb.linux.client.data.parser.KeywordFilterParser

/** 源站 /home_keyword_filter_settings 的传输适配器。 */
class SourceKeywordFilterRepository(private val client: LsbClient) : KeywordFilterApi {
    override suspend fun fetch(policy: KeywordFilterPolicy): KeywordFilterSnapshot {
        val json = try {
            client.getAjaxJsonForRepository(SETTINGS_PATH)
        } catch (error: Exception) {
            throw classify(error)
        }
        if (json.optInt("ok", 1) != 1 && !json.has("settings")) {
            throw KeywordFilterRemoteException.Rejected(json.optString("message").ifBlank { "读取失败" })
        }
        return try {
            val (exists, raw) = KeywordFilterParser.parseSettingsResponse(json)
            KeywordFilterSnapshot(
                settings = KeywordFilterRules.sanitize(raw, policy),
                policy = policy,
                exists = exists,
                pending = false,
                syncedAt = System.currentTimeMillis(),
            )
        } catch (error: Exception) {
            throw KeywordFilterRemoteException.InvalidResponse(error)
        }
    }

    override suspend fun save(settings: KeywordFilterSettings, policy: KeywordFilterPolicy): KeywordFilterSnapshot {
        val clean = KeywordFilterRules.sanitize(settings, policy)
        val payload = JSONObject()
            .put("presets", JSONArray(clean.presets))
            .put("custom", JSONArray(clean.custom))
            .put("users", JSONArray(clean.users))
            .put("forum_excluded_ids", JSONArray(clean.forumExcludedIds))
            .put("forum_extra_ids", JSONArray(clean.forumExtraIds))
        var json = try {
            postSettings(payload, forceCsrfRefresh = false)
        } catch (error: Exception) {
            throw classify(error)
        }
        if (json.optInt("ok", 0) != 1) {
            // 源站拒绝时用新页面令牌重试一次，覆盖会话恢复后缓存 CSRF 已失效的情况。
            client.invalidateCsrf()
            json = try {
                postSettings(payload, forceCsrfRefresh = true)
            } catch (error: Exception) {
                throw classify(error)
            }
        }
        if (json.optInt("ok", 0) != 1) {
            throw KeywordFilterRemoteException.Rejected(json.optString("message").ifBlank { "保存失败" })
        }
        val raw = if (json.has("settings")) {
            KeywordFilterParser.parseSettingsResponse(json).second
        } else {
            // 源站可能只返回 {ok:1}；网页端此时沿用刚提交的规范化设置。
            clean
        }
        return KeywordFilterSnapshot(
            settings = KeywordFilterRules.sanitize(raw, policy),
            policy = policy,
            exists = true,
            pending = false,
            syncedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun postSettings(payload: JSONObject, forceCsrfRefresh: Boolean): JSONObject =
        client.postAjax(
            SETTINGS_PATH,
            mapOf(
                "_csrf" to client.csrf(forceRefresh = forceCsrfRefresh),
                "settings" to payload.toString(),
            ),
        )

    private fun classify(error: Exception): KeywordFilterRemoteException = when {
        error is KeywordFilterRemoteException -> error
        error.message?.contains(AUTH_REQUIRED, ignoreCase = true) == true -> KeywordFilterRemoteException.Authentication(error)
        error is java.io.IOException -> KeywordFilterRemoteException.Network(error)
        else -> KeywordFilterRemoteException.InvalidResponse(error)
    }

    private companion object {
        const val SETTINGS_PATH = "/home_keyword_filter_settings"
        const val AUTH_REQUIRED = "AUTH_REQUIRED"
    }
}
