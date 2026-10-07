package sb.linux.client.ui.export

import android.graphics.Color

/**
 * 帖子导出：HTML / Markdown / 长图（文本渲染），导出后调起系统分享。
 */
object TopicExport {

    /** 长图导出主题色：由调用方传入应用当前 ColorScheme，保证长图所见即所得（含深色模式） */
    data class ExportTheme(
        val background: Int,
        val onBackground: Int,
        val mainCard: Int,            // 楼主正文容器（primaryContainer 13% 叠加背景）
        val replyCard: Int,           // 回复容器（surfaceContainer）
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val primaryContainer: Int,
        val onPrimaryContainer: Int,
        val secondaryContainer: Int,
        val onSecondaryContainer: Int,
        val outlineVariant: Int,
    ) {
        companion object {
            /** fg 按透明度叠加在 bg 上（不透明结果） */
            fun blendOver(fg: Int, alpha: Float, bg: Int): Int {
                val a = alpha.coerceIn(0f, 1f)
                val fr = (fg shr 16) and 0xFF; val fgG = (fg shr 8) and 0xFF; val fb = fg and 0xFF
                val br = (bg shr 16) and 0xFF; val bgG = (bg shr 8) and 0xFF; val bb = bg and 0xFF
                val r = (fr * a + br * (1 - a)).toInt().coerceIn(0, 255)
                val g = (fgG * a + bgG * (1 - a)).toInt().coerceIn(0, 255)
                val b = (fb * a + bb * (1 - a)).toInt().coerceIn(0, 255)
                return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            /** 亮色兜底（无 ColorScheme 可用时） */
            val Light = ExportTheme(
                background = Color.WHITE,
                onBackground = Color.BLACK,
                mainCard = blendOver(Color.parseColor("#DDE1FF"), 0.13f, Color.WHITE),
                replyCard = Color.parseColor("#F4F5F7"),
                onSurface = Color.parseColor("#1B1B1F"),
                onSurfaceVariant = Color.DKGRAY,
                primaryContainer = Color.parseColor("#DDE1FF"),
                onPrimaryContainer = Color.parseColor("#101433"),
                secondaryContainer = Color.parseColor("#E4E6EB"),
                onSecondaryContainer = Color.parseColor("#44474E"),
                outlineVariant = Color.parseColor("#C4C6CF"),
            )
        }
    }

}
