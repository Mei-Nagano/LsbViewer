package sb.linux.client.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Endpoints {
    const val BASE = "https://linux.sb"
    fun abs(u: String): String = if (u.startsWith("http")) u else BASE + u
}

object TimeFmt {
    fun rel(epochSec: Long): String {
        val diff = System.currentTimeMillis() / 1000 - epochSec
        return when {
            diff < 60 -> "刚刚"
            diff < 3600 -> "${diff / 60}分钟前"
            diff < 86400 -> "${diff / 3600}小时前"
            diff < 86400 * 2 -> "昨天"
            diff < 86400 * 30 -> "${diff / 86400}天前"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(epochSec * 1000))
        }
    }
}
