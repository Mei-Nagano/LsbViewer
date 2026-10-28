package sb.linux.client.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengePagePolicyTest {
    private val detectionScript = """
        <script>var a=document.createElement('script');
        a.src='/cdn-cgi/challenge-platform/scripts/jsd/main.js';</script>
    """.trimIndent()

    @Test
    fun backgroundDetectionDoesNotRequireVerification() {
        val html = "<title>烧饼社区</title><main>欢迎访问</main>$detectionScript"
        assertFalse(ChallengePagePolicy.isChallenge(html))
        assertFalse(ChallengePagePolicy.needsVerification(200, html))
    }

    @Test
    fun homeTopicsWithChallengeWordsAreNormalContent() {
        val html = """
            <title>烧饼社区</title>
            <ul class="post-list"><li class="post-item">
            <a class="post-title" href="/topic/42">SECURITY_VERIFICATION 与 cf-chl-</a>
            </li></ul>
            $detectionScript
        """.trimIndent()
        assertFalse(ChallengePagePolicy.isChallenge(html))
    }

    @Test
    fun genuineChallengesStillRequireVerification() {
        val cloudflare = """
            <title>Just a moment...</title><form id="challenge-form"></form>
            <script src="/cdn-cgi/challenge-platform/h/g/orchestrate/chl_page/v1"></script>
            $detectionScript
        """.trimIndent()
        assertTrue(ChallengePagePolicy.needsVerification(403, cloudflare))
        assertTrue(ChallengePagePolicy.needsVerification(503, cloudflare))
        assertTrue(ChallengePagePolicy.isChallenge(
            "<script src='/cdn-cgi/challenge-platform/h/g/orchestrate/chl_page/v1'></script>",
        ))
        assertTrue(ChallengePagePolicy.isUamChallenge(
            "<title>Checking your Browser</title><script>var nonce=123; /* fl_ua_p */</script>",
        ))
    }
}
