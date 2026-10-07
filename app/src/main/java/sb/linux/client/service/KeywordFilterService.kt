package sb.linux.client.service

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.api.KeywordFilterApi
import sb.linux.client.common.filter.KeywordFilterRemoteException
import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot

/** 账号级缓存、乐观保存和 5 分钟同步策略。 */
class KeywordFilterService(
    private val remote: KeywordFilterApi,
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun cached(userId: Long, policy: KeywordFilterPolicy): KeywordFilterSnapshot? =
        read(userId)?.let {
            it.copy(
                settings = KeywordFilterRules.sanitize(it.settings, policy),
                policy = policy,
            )
        }

    /**
     * 拉取并同步账号规则。
     *
     * legacy 用于接收旧版本全局屏蔽词。迁移规则先与源站取并集，再保存，避免首次同步覆盖远端设置。
     */
    suspend fun refresh(
        userId: Long,
        policy: KeywordFilterPolicy,
        force: Boolean = false,
        legacy: KeywordFilterSettings = KeywordFilterSettings(),
    ): KeywordFilterSnapshot {
        val legacyClean = KeywordFilterRules.sanitize(legacy, policy)
        val hasLegacy = hasRules(legacyClean)
        var local = cached(userId, policy)
        if (!force && local != null && !local.pending && !hasLegacy && now() - local.syncedAt < SYNC_TTL_MS) {
            return local
        }

        // exists=true 表示这是一次普通保存，保存失败时本地快照就是完整目标状态，可直接重试。
        if (local?.pending == true && local.exists) {
            val retried = runCatching { remote.save(local.settings, policy) }
            retried.onSuccess { saved ->
                write(userId, saved)
                local = saved
            }.onFailure { error ->
                if (isNetwork(error)) {
                    val pending = local!!.copy(syncedAt = now())
                    write(userId, pending)
                    return pending
                }
                throw error
            }
            if (!hasLegacy) return local!!
        }

        val fetched = try {
            remote.fetch(policy)
        } catch (error: Exception) {
            if (isNetwork(error) && (hasLegacy || local?.pending == true)) {
                val pendingSettings = KeywordFilterRules.merge(local?.settings ?: KeywordFilterSettings(), legacyClean, policy)
                val pending = KeywordFilterSnapshot(
                    settings = pendingSettings,
                    policy = policy,
                    exists = local?.exists ?: false,
                    pending = true,
                    syncedAt = now(),
                )
                write(userId, pending)
                return pending
            }
            throw error
        }

        // 旧版本地规则视为待迁移变更，与源站规则取并集后再上传。
        var merged = fetched.settings
        if (local?.pending == true && !local!!.exists) {
            merged = KeywordFilterRules.merge(merged, local!!.settings, policy)
        }
        if (hasLegacy) {
            merged = KeywordFilterRules.merge(merged, legacyClean, policy)
        }
        if (merged != fetched.settings) {
            return runCatching { remote.save(merged, policy) }
                .onSuccess { write(userId, it) }
                .getOrElse { error ->
                    if (isNetwork(error)) {
                        val pending = KeywordFilterSnapshot(
                            settings = merged,
                            policy = policy,
                            exists = fetched.exists,
                            pending = true,
                            syncedAt = now(),
                        )
                        write(userId, pending)
                        pending
                    } else {
                        throw error
                    }
                }
        }

        // 兼容源站返回 exists=false 但带有规则的异常响应，确保规则最终落库。
        if (!fetched.exists && hasRules(fetched.settings)) {
            return runCatching { remote.save(fetched.settings, policy) }
                .getOrElse { error ->
                    if (isNetwork(error)) {
                        val pending = fetched.copy(pending = true, syncedAt = now())
                        write(userId, pending)
                        pending
                    } else {
                        throw error
                    }
                }
                .also { write(userId, it) }
        }
        write(userId, fetched)
        return fetched
    }

    private fun isNetwork(error: Throwable): Boolean =
        error is KeywordFilterRemoteException.Network || error is java.io.IOException

    suspend fun save(userId: Long, settings: KeywordFilterSettings, policy: KeywordFilterPolicy): KeywordFilterSnapshot {
        val clean = KeywordFilterRules.sanitize(settings, policy)
        val pending = KeywordFilterSnapshot(clean, policy, exists = true, pending = true, syncedAt = now())
        write(userId, pending)
        return runCatching { remote.save(clean, policy) }
            .onSuccess { write(userId, it) }
            .getOrElse { error ->
                if (error is KeywordFilterRemoteException.Network || error is java.io.IOException) pending
                else throw error
            }
    }

    fun clear(userId: Long) {
        prefs.edit().remove(key(userId)).apply()
    }

    private fun read(userId: Long): KeywordFilterSnapshot? = runCatching {
        val root = JSONObject(prefs.getString(key(userId), "") ?: return null)
        val settings = root.optJSONObject("settings") ?: return null
        KeywordFilterSnapshot(
            settings = KeywordFilterSettings(
                presets = strings(settings, "presets"),
                custom = strings(settings, "custom"),
                users = strings(settings, "users"),
                forumExcludedIds = longs(settings, "forum_excluded_ids"),
                forumExtraIds = longs(settings, "forum_extra_ids"),
            ),
            exists = root.optBoolean("exists", false),
            pending = root.optBoolean("pending", false),
            syncedAt = root.optLong("syncedAt", 0L),
        )
    }.getOrNull()

    private fun write(userId: Long, snapshot: KeywordFilterSnapshot) {
        val settings = JSONObject()
            .put("presets", JSONArray(snapshot.settings.presets))
            .put("custom", JSONArray(snapshot.settings.custom))
            .put("users", JSONArray(snapshot.settings.users))
            .put("forum_excluded_ids", JSONArray(snapshot.settings.forumExcludedIds))
            .put("forum_extra_ids", JSONArray(snapshot.settings.forumExtraIds))
        prefs.edit().putString(
            key(userId),
            JSONObject()
                .put("settings", settings)
                .put("exists", snapshot.exists)
                .put("pending", snapshot.pending)
                .put("syncedAt", snapshot.syncedAt)
                .toString(),
        ).apply()
    }

    private fun hasRules(settings: KeywordFilterSettings): Boolean =
        settings.presets.isNotEmpty() || settings.custom.isNotEmpty() || settings.users.isNotEmpty() ||
            settings.forumExcludedIds.isNotEmpty() || settings.forumExtraIds.isNotEmpty()

    private fun strings(root: JSONObject, key: String): List<String> =
        (root.optJSONArray(key) ?: JSONArray()).let { array ->
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }

    private fun longs(root: JSONObject, key: String): List<Long> =
        (root.optJSONArray(key) ?: JSONArray()).let { array ->
            (0 until array.length()).mapNotNull { array.optLong(it, 0L).takeIf { id -> id > 0L } }
        }

    private fun key(userId: Long): String = "keyword_filter.v2.$userId"

    private companion object {
        const val SYNC_TTL_MS = 5 * 60 * 1000L
    }
}
