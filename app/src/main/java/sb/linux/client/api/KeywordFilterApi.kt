package sb.linux.client.api

import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot

/** 屏蔽词远端能力，供 Session/service 使用，避免 UI 直接依赖网络客户端。 */
interface KeywordFilterApi {
    /** 拉取当前账号设置。 */
    suspend fun fetch(policy: KeywordFilterPolicy): KeywordFilterSnapshot

    /** 保存设置并返回源站最终接受的值。 */
    suspend fun save(settings: KeywordFilterSettings, policy: KeywordFilterPolicy): KeywordFilterSnapshot
}
