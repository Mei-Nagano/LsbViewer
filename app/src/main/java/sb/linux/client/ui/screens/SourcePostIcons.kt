package sb.linux.client.ui.screens

/** 操作图标独立于源站内容颜色，链接名称由文档生成器提供。 */
internal object SourcePostIcons {
    fun svg(kind: String): String {
        val shape = when (kind) {
            "like" -> "<path d='M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'/>"
            "coin" -> "<circle cx='12' cy='12' r='9'/><path d='M9 8h6m-6 4h6m-3-4v9'/>"
            "donate" -> "<path d='M4 11h16v10H4zM3 7h18v4H3zM12 7v14'/><path d='M12 7H8a3 3 0 1 1 3-3l1 3Zm0 0h4a3 3 0 1 0-3-3l-1 3Z'/>"
            "quote" -> "<path d='m9 5-7 7 7 7M2 12h12a7 7 0 0 1 7 7'/>"
            "report" -> "<path d='M5 21V3m0 1c5-3 9 3 14 0v10c-5 3-9-3-14 0'/>"
            "favorite", "favorite-topic" -> "<path d='M6 3h12v18l-6-4-6 4z'/>"
            "copy" -> "<rect x='8' y='8' width='12' height='13' rx='2'/><path d='M16 8V3H3v13h5'/>"
            "thread" -> "<path d='M3 3h14v10H7l-4 4V3Zm14 5h4v13l-4-4h-6v-4'/>"
            "expand" -> "<path d='m6 9 6 6 6-6'/>"
            "collapse" -> "<path d='m6 15 6-6 6 6'/>"
            "source-actions" -> "<path d='M4 6h16M4 12h16M4 18h16'/><circle cx='8' cy='6' r='2'/><circle cx='16' cy='12' r='2'/><circle cx='10' cy='18' r='2'/>"
            else -> ""
        }
        return "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'>$shape</svg>"
    }
}
