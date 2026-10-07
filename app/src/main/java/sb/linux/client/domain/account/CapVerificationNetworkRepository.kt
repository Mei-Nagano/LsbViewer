package sb.linux.client.domain.account

import java.io.Closeable

data class CapVerificationNetworkRequest(
    val url: String,
    val headers: Map<String, String>,
    val contentType: String?,
    val body: String,
    val replayable: Boolean,
)

data class CapVerificationNetworkResponse(
    val status: Int,
    val body: String,
    val contentType: String?,
)

/** Callbacks are delivered asynchronously; closing the returned handle cancels the request. */
interface CapVerificationNetworkRepository {
    fun enqueue(
        request: CapVerificationNetworkRequest,
        onResult: (Result<CapVerificationNetworkResponse>) -> Unit,
    ): Closeable
}
