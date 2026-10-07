package sb.linux.client.ui.screens

/** 中文按单字计，拉丁/数字连续串按一个词计；按约 300 字/分钟给出保守阅读时长。 */
internal fun estimateReading(text: String): Pair<Int, Int> {
    val cjk = text.count { c ->
        c.code in 0x3400..0x4DBF || c.code in 0x4E00..0x9FFF ||
            c.code in 0xF900..0xFAFF || c.code in 0x3040..0x30FF || c.code in 0xAC00..0xD7AF
    }
    val words = Regex("[A-Za-z0-9]+(?:['_-][A-Za-z0-9]+)*").findAll(text).count()
    val total = (cjk + words).coerceAtLeast(text.trim().takeIf { it.isNotEmpty() }?.let { 1 } ?: 0)
    return total to ((total + 299) / 300).coerceAtLeast(if (total > 0) 1 else 0)
}
