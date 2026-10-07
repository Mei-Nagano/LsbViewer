package sb.linux.client.data.filter

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sb.linux.client.common.error.LsbException
import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.domain.community.KeywordFilterRepository
import sb.linux.client.service.KeywordFilterService

/** Coordinates account identity, legacy-rule migration, the account cache, and source sync. */
class AccountKeywordFilterRepositoryImpl(
    private val service: KeywordFilterService,
    private val loadPolicy: suspend () -> KeywordFilterPolicy,
    private val currentSnapshot: () -> KeywordFilterSnapshot,
    private val currentUserId: () -> Long?,
    private val legacySettings: () -> KeywordFilterSettings,
    private val clearLegacySettings: () -> Unit,
    private val updateSnapshot: (KeywordFilterSnapshot) -> Unit,
) : KeywordFilterRepository {
    private val mutex = Mutex()

    override fun snapshot(): KeywordFilterSnapshot = currentSnapshot()

    override suspend fun refresh(force: Boolean): KeywordFilterSnapshot = mutex.withLock {
        val userId = requireUserId()
        var policy = currentSnapshot().policy
        service.cached(userId, policy)?.let(updateSnapshot)
        if (policy.availablePresets.isEmpty() && policy.forums.isEmpty()) policy = loadPolicy()
        val legacy = legacySettings()
        service.refresh(userId, policy, force, legacy).also { snapshot ->
            updateSnapshot(snapshot)
            if (hasRules(legacy) && !snapshot.pending) clearLegacySettings()
        }
    }

    override suspend fun save(settings: KeywordFilterSettings): KeywordFilterSnapshot = mutex.withLock {
        val snapshot = service.save(requireUserId(), settings, currentSnapshot().policy)
        updateSnapshot(snapshot)
        if (hasRules(legacySettings())) clearLegacySettings()
        snapshot
    }

    override fun clearCache() {
        currentUserId()?.let(service::clear)
        updateSnapshot(KeywordFilterSnapshot(policy = currentSnapshot().policy))
    }

    private fun requireUserId(): Long = currentUserId()
        ?: throw LsbException("请先登录后使用源站屏蔽词")

    private fun hasRules(value: KeywordFilterSettings): Boolean =
        value.presets.isNotEmpty() || value.custom.isNotEmpty() || value.users.isNotEmpty() ||
            value.forumExcludedIds.isNotEmpty() || value.forumExtraIds.isNotEmpty()
}
