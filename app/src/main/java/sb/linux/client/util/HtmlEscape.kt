package sb.linux.client.util

/** HTML 文本与属性值的最小安全转义工具。 */
fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
