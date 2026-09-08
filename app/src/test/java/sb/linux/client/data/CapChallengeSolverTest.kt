package sb.linux.client.data

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapChallengeSolverTest {
    @Test
    fun solvesSha256ProofOfWork() {
        val salt = "cap-test-salt"
        val nonce = CapChallengeSolver.solvePow(salt, "0")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("$salt$nonce".toByteArray())
        assertTrue((hash[0].toInt() and 0xF0) == 0)
    }

    @Test
    fun solvesRswChallenge() {
        assertEquals("38", CapChallengeSolver.solveRsw("64", "2", 3))
    }
}
