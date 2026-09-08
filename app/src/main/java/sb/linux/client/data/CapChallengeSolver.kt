package sb.linux.client.data

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** CAP format 1/2 的本地挑战求解器，与源站组件 Worker 使用同一算法。 */
internal object CapChallengeSolver {
    fun solvePow(salt: String, target: String): Long {
        val bits = target.length * 4
        val fullBytes = bits / 8
        val partialBits = bits % 8
        val targetBytes = target.padEnd(if (target.length % 2 == 0) target.length else target.length + 1, '0')
            .chunked(2).map { it.toInt(16).toByte() }
        val digest = MessageDigest.getInstance("SHA-256")
        var nonce = 0L
        while (true) {
            val hash = digest.digest("$salt$nonce".toByteArray(StandardCharsets.UTF_8))
            var matched = true
            for (index in 0 until fullBytes) {
                if (hash[index] != targetBytes[index]) {
                    matched = false
                    break
                }
            }
            if (matched && partialBits > 0) {
                val mask = (0xFF shl (8 - partialBits)) and 0xFF
                matched = (hash[fullBytes].toInt() and mask) == (targetBytes[fullBytes].toInt() and mask)
            }
            if (matched) return nonce
            nonce++
        }
    }

    fun solveRsw(modulus: String, value: String, iterations: Int): String {
        var result = BigInteger(value, 16)
        val modulusValue = BigInteger(modulus, 16)
        repeat(iterations) { result = result.multiply(result).mod(modulusValue) }
        return result.toString(16)
    }
}
