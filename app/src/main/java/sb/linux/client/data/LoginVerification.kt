package sb.linux.client.data

/** 登录页当前要求的人机验证方式。 */
sealed interface LoginVerification {
    val csrf: String

    /** 旧版数学题 + SHA-256 工作量证明。 */
    data class Native(
        override val csrf: String,
        val question: String,
        val questionHtml: String = "",
        val token: String,
        val powPrefix: String,
        val powZeros: Int,
    ) : LoginVerification

    /** CAP Web Component：组件完成后产生一次性的表单令牌。 */
    data class Cap(
        override val csrf: String,
        val pageUrl: String,
        val endpoint: String,
        val scriptUrl: String,
        val wasmUrl: String = "",
        val fieldName: String = "cap-token",
        val widgetScript: String = "",
        val wasmDataUrl: String = "",
    ) : LoginVerification
}
