package sb.linux.client.domain.account

import org.json.JSONException
import org.json.JSONObject
import sb.linux.client.common.error.LsbException

/** 仅提交网页原组件产生的当轮结果，刷新后的挑战不能与旧挑战混用。 */
internal fun LoginVerification.Pow.fields(result: String): Map<String, String> {
    val fields = try {
        val json = JSONObject(result)
        listOf("human_verification_pow_challenge", "human_verification_pow_proof")
            .associateWith { json.getString(it) }
    } catch (_: JSONException) {
        throw LsbException("请先完成人机验证")
    }
    if (fields.values.any(String::isBlank)) throw LsbException("请先完成人机验证")
    val expires = fields.getValue("human_verification_pow_challenge").split('.').getOrNull(2)?.toLongOrNull()
    if (expires != null && System.currentTimeMillis() / 1000 >= expires) {
        throw LsbException("人机验证已过期，请重新验证")
    }
    return fields
}
