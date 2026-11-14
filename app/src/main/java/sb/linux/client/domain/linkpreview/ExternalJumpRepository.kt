package sb.linux.client.domain.linkpreview

interface ExternalJumpRepository {
    /** 返回源站许可的目标；黑名单、无效签名和无法读取时保留提示页。 */
    suspend fun permittedTarget(url: String): String?
}
