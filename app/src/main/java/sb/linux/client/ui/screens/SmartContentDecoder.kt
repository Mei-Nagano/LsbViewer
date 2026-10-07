package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
internal data class SmartDecodedContent(val type: String, val source: String, val decoded: String)

/** 保守识别常用文本编码：只展示高可读结果，二进制、乱码和过短片段不提示。 */
internal fun detectSmartEncodedContent(html: String): List<SmartDecodedContent> {
    val plain = HtmlTextConversion.htmlToText(html)
    val found = mutableListOf<SmartDecodedContent>()
    fun add(type: String, source: String, result: String?) {
        val decoded = result?.trim().orEmpty()
        if (source.length > 8_000 || decoded.length < 4 || decoded == source || decoded.contains('\uFFFD')) return
        val readable = decoded.count { it == '\n' || it == '\r' || it == '\t' || !it.isISOControl() }
        if (readable.toFloat() / decoded.length >= 0.9f) found += SmartDecodedContent(type, source, decoded)
    }
    Regex("(?<![A-Za-z0-9_+/=-])([A-Za-z0-9_+/-]{16,}={0,2})(?![A-Za-z0-9_+/=-])")
        .findAll(plain).forEach { m ->
            val source = m.groupValues[1]
            val padded = source + "=".repeat((4 - source.length % 4) % 4)
            val flags = if (source.contains('-') || source.contains('_')) android.util.Base64.URL_SAFE else android.util.Base64.DEFAULT
            val decoded = runCatching { android.util.Base64.decode(padded, flags).toString(Charsets.UTF_8) }.getOrNull()
            add(if (flags == android.util.Base64.URL_SAFE) "Base64 URL" else "Base64", source, decoded)
        }
    Regex("(?<![0-9A-Fa-f])([0-9A-Fa-f]{16,})(?![0-9A-Fa-f])").findAll(plain).forEach { m ->
        val source = m.groupValues[1]
        if (source.length % 2 == 0) add("Hex", source, runCatching {
            source.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
        }.getOrNull())
    }
    Regex("(?:%[0-9A-Fa-f]{2}){4,}").findAll(plain).forEach { m -> add("URL", m.value, runCatching { java.net.URLDecoder.decode(m.value, "UTF-8") }.getOrNull()) }
    Regex("(?:\\\\u[0-9A-Fa-f]{4}){2,}").findAll(plain).forEach { m ->
        add("Unicode", m.value, Regex("\\\\u([0-9A-Fa-f]{4})").replace(m.value) { it.groupValues[1].toInt(16).toChar().toString() })
    }
    Regex("(?i)rot13\\s*[:：]\\s*([A-Za-z][A-Za-z ]{3,})").findAll(plain).forEach { m ->
        val source = m.groupValues[1].trim()
        add("ROT13", source, source.map { c -> when (c) { in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26; in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26; else -> c } }.joinToString(""))
    }
    return found.distinctBy { it.type to it.source }.take(6)
}


