package sb.linux.client.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** DoH 服务地址的校验规则，与服务器列表及设置表单共用。 */
object DohEndpointPolicy {
    fun isValid(url: String): Boolean = url.trim().toHttpUrlOrNull()?.let {
        it.scheme == "https" && it.username.isEmpty() && it.password.isEmpty() && it.fragment == null
    } == true
}
