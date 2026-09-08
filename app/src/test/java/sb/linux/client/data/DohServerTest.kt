package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** DoH 故障切换候选顺序测试。 */
class DohServerTest {

    @Test
    fun runtimeRecoveryEndpointTakesPriorityAndCandidatesAreDeduplicated() {
        val servers = listOf(
            DohServer("a", "A", "https://a.example/dns-query"),
            DohServer("b", "B", "https://b.example/dns-query"),
        )

        assertEquals(
            listOf(
                "https://b.example/dns-query",
                "https://a.example/dns-query",
            ),
            dohCandidateUrls(
                activeUrl = "https://a.example/dns-query",
                runtimeUrl = "https://b.example/dns-query",
                servers = servers,
            ),
        )
    }

    @Test
    fun candidateCountIsBounded() {
        val servers = (1..5).map { index ->
            DohServer("$index", "$index", "https://dns$index.example/dns-query")
        }

        assertEquals(3, dohCandidateUrls("https://active.example/dns-query", null, servers).size)
    }
}
