package sb.linux.client.ui.common.content

import org.jsoup.Jsoup

internal fun htmlTextLength(html: String): Int = Jsoup.parse(html).text().length
