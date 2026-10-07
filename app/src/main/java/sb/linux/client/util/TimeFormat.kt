package sb.linux.client.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 将源站 Unix 秒时间戳格式化为中文相对时间。 */
object TimeFormat {
    fun relative(epochSec: Long): String {
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
