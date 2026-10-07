package sb.linux.client.data

import java.io.Closeable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import sb.linux.client.domain.account.CapVerificationNetworkRepository
import sb.linux.client.domain.account.CapVerificationNetworkRequest
import sb.linux.client.domain.account.CapVerificationNetworkResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Keeps CAP requests on the forum client's DoH, proxy, Cookie and safe-replay policy. */
internal class ClientCapVerificationNetworkRepository(client: LsbClient) : CapVerificationNetworkRepository {
    private val http = client.http.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .build()

    override fun enqueue(
        request: CapVerificationNetworkRequest,
        onResult: (Result<CapVerificationNetworkResponse>) -> Unit,
    ): Closeable {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val body = request.body.toRequestBody(request.contentType?.toMediaTypeOrNull())
        builder.post(body)
        if (request.replayable) builder.tag(CronetReplayable::class.java, CronetReplayable)
        val call = http.newCall(builder.build())
        call.enqueue(callback(onResult))
        return Closeable { call.cancel() }
    }

    private fun callback(
        onResult: (Result<CapVerificationNetworkResponse>) -> Unit,
    ) = object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(Result.failure(e))

        override fun onResponse(call: Call, response: Response) {
            val result = response.use {
                CapVerificationNetworkResponse(
                    status = it.code,
                    body = runCatching { it.body?.string().orEmpty() }.getOrDefault(""),
                    contentType = it.header("Content-Type", "application/json"),
                )
            }
            onResult(Result.success(result))
        }
    }
}
