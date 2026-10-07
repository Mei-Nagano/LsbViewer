package sb.linux.client.domain.account

data class CheckinResult(val succeeded: Boolean, val message: String)

interface CheckinRepository {
    suspend fun load(): CheckinInfo
    suspend fun submit(): CheckinResult
}
