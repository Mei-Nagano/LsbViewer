package sb.linux.client.data

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Cronet 回退的重放边界测试。 */
class CronetFallbackTest {

    @Test
    fun replayableChallengeCanFallbackAfterSendStarted() {
        val request = postRequest().newBuilder()
            .tag(CronetReplayable::class.java, CronetReplayable)
            .build()

        assertTrue(canRetryWithCronet(request, IOException("reset"), mayHaveSent = true))
    }

    @Test
    fun ordinaryPostCannotFallbackAfterSendStarted() {
        assertFalse(canRetryWithCronet(postRequest(), IOException("reset"), mayHaveSent = true))
    }

    @Test
    fun replayableRequestStillRejectsCertificateFailure() {
        val request = postRequest().newBuilder()
            .tag(CronetReplayable::class.java, CronetReplayable)
            .build()
        val failure = IOException("tls", java.security.cert.CertificateException("invalid"))

        assertFalse(canRetryWithCronet(request, failure, mayHaveSent = true))
    }

    private fun postRequest(): Request = Request.Builder()
        .url("https://cap.linux.sb/example/challenge")
        .post("{}".toRequestBody())
        .build()
}
