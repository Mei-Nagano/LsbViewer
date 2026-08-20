package sb.linux.client.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Html
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import androidx.compose.ui.graphics.toArgb

/**
 * 帖子导出：HTML / Markdown / 长图（文本渲染），导出后调起系统分享。
 */
object TopicExport {

    private fun safeName(t: String): String =
        t.replace(Regex("""[\\/:*?"<>|\s]+"""), "_").take(40).ifBlank { "topic" }

    /** 导出目录：filesDir 持久存储（供「已导出的帖子」列表读取，不会被系统清理缓存清除，3.17） */
    private fun exportDir(context: Context): File =
        File(context.filesDir, "exports").apply { mkdirs() }

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

            /** 从应用 ColorScheme 生成导出主题 */
            fun fromColorScheme(cs: androidx.compose.material3.ColorScheme): ExportTheme {
                val bg = cs.background.toArgb()
                val pc = cs.primaryContainer.toArgb()
                return ExportTheme(
                    background = bg,
                    onBackground = cs.onBackground.toArgb(),
                    mainCard = blendOver(pc, 0.13f, bg),
                    replyCard = cs.surfaceContainer.toArgb(),
                    onSurface = cs.onSurface.toArgb(),
                    onSurfaceVariant = cs.onSurfaceVariant.toArgb(),
                    primaryContainer = pc,
                    onPrimaryContainer = cs.onPrimaryContainer.toArgb(),
                    secondaryContainer = cs.secondaryContainer.toArgb(),
                    onSecondaryContainer = cs.onSecondaryContainer.toArgb(),
                    outlineVariant = cs.outlineVariant.toArgb(),
                )
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

    // ---------------- HTML ----------------

    fun buildHtml(title: String, posts: List<PostEntry>, url: String): String {
        val sb = StringBuilder()
        sb.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
        sb.append("<title>").append(esc(title)).append("</title>")
        sb.append("<style>body{font-family:system-ui,-apple-system,sans-serif;max-width:760px;margin:0 auto;padding:16px;line-height:1.7;color:#222}")
        sb.append("article{border:1px solid #e3e3e3;border-radius:10px;padding:14px;margin:12px 0}")
        sb.append(".meta{color:#888;font-size:13px;margin-bottom:6px}img{max-width:100%}pre{background:#f5f5f5;padding:10px;border-radius:8px;overflow:auto}blockquote{border-left:3px solid #99a;margin:0;padding-left:10px;color:#555}</style>")
        sb.append("</head><body><h1>").append(esc(title)).append("</h1>")
        sb.append("<p class=\"meta\">来源：<a href=\"").append(esc(url)).append("\">").append(esc(url)).append("</a></p>")
        posts.forEach { p ->
            sb.append("<article>")
            sb.append("<div class=\"meta\"><b>").append(esc(p.authorName)).append("</b>")
            if (p.floor > 0) sb.append(" · #").append(p.floor)
            sb.append(" · ").append(esc(p.timeText)).append("</div>")
            sb.append(p.contentHtml)
            sb.append("</article>")
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    // ---------------- Markdown ----------------

    fun buildMarkdown(title: String, posts: List<PostEntry>, url: String): String {
        val sb = StringBuilder()
        sb.append("# ").append(title).append("\n\n")
        sb.append("> 来源：").append(url).append("\n\n")
        sb.append("---\n\n")
        posts.forEach { p ->
            sb.append("**").append(p.authorName).append("**")
            if (p.floor > 0) sb.append(" `#").append(p.floor).append("`")
            sb.append(" · ").append(p.timeText).append("\n\n")
            sb.append(htmlToMarkdown(p.contentHtml)).append("\n\n---\n\n")
        }
        return sb.toString()
    }

    /** 服务端渲染的帖子 HTML → Markdown（覆盖常用元素） */
    fun htmlToMarkdown(html: String): String {
        val root = runCatching { Jsoup.parseBodyFragment(html).body() }.getOrNull() ?: return ""
        val sb = StringBuilder()

        fun inline(el: Element): String {
            val out = StringBuilder()
            el.childNodes().forEach { n ->
                when (n) {
                    is org.jsoup.nodes.TextNode -> out.append(n.text())
                    is Element -> when (n.tagName().lowercase()) {
                        "br" -> out.append("\n")
                        "strong", "b" -> out.append("**").append(inline(n)).append("**")
                        "em", "i" -> out.append("*").append(inline(n)).append("*")
                        "del", "s" -> out.append("~~").append(inline(n)).append("~~")
                        "code" -> out.append("`").append(n.text()).append("`")
                        "a" -> {
                            val href = n.attr("abs:href").ifBlank { n.attr("href") }
                            out.append("[").append(n.text()).append("](").append(href).append(")")
                        }
                        "img" -> {
                            val src = n.attr("abs:src").ifBlank { n.attr("src") }
                            out.append("![图片](").append(src).append(")")
                        }
                        else -> out.append(inline(n))
                    }
                    else -> {}
                }
            }
            return out.toString()
        }

        fun block(el: Element) {
            when (el.tagName().lowercase()) {
                "p", "div", "section" -> sb.append(inline(el)).append("\n\n")
                "pre" -> sb.append("```\n").append(el.wholeText().trim()).append("\n```\n\n")
                "blockquote" -> sb.append(inline(el).trim().lines().joinToString("\n") { "> $it" }).append("\n\n")
                "h1" -> sb.append("# ").append(inline(el)).append("\n\n")
                "h2" -> sb.append("## ").append(inline(el)).append("\n\n")
                "h3" -> sb.append("### ").append(inline(el)).append("\n\n")
                "h4", "h5", "h6" -> sb.append("#### ").append(inline(el)).append("\n\n")
                "ul", "ol" -> el.select("> li").forEachIndexed { i, li ->
                    sb.append(if (el.tagName().lowercase() == "ol") "${i + 1}. " else "- ")
                        .append(inline(li).trim()).append("\n")
                }.also { sb.append("\n") }
                "hr" -> sb.append("---\n\n")
                "table" -> {
                    val rows = el.select("tr")
                    rows.forEachIndexed { ri, tr ->
                        val cells = tr.select("th, td").map { it.text().trim() }
                        sb.append("| ").append(cells.joinToString(" | ")).append(" |\n")
                        if (ri == 0) sb.append("|").append(cells.joinToString("") { "--- |" }).append("\n")
                    }
                    sb.append("\n")
                }
                "img" -> sb.append("![图片](").append(el.attr("abs:src").ifBlank { el.attr("src") }).append(")\n\n")
                else -> {
                    val t = inline(el).trim()
                    if (t.isNotEmpty()) sb.append(t).append("\n\n")
                }
            }
        }

        root.children().forEach { block(it) }
        if (sb.isEmpty()) {
            val t = inline(root).trim()
            if (t.isNotEmpty()) sb.append(t).append("\n")
        }
        return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }

    // ---------------- 长图 ----------------

    /** 长图高度上限（px）：防止超长帖子导出时 OOM，超出部分截断并提示 */
    private const val MAX_IMAGE_HEIGHT = 12000

    /** 长图内容图片加载数量上限：保护内存与导出耗时 */
    private const val MAX_CONTENT_IMAGES = 80

    private fun roundedBg(color: String, radiusPx: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusPx
        }

    /**
     * 长图导出：按应用内帖子卡片的样式离屏渲染——
     * 头像（圆形）、楼主/楼层徽章、正文图片真实加载、跟随应用当前主题色（所见即所得）。
     * 需在 IO 线程调用（内部同步加载图片）。
     */
    suspend fun renderLongImage(
        context: Context,
        title: String,
        posts: List<PostEntry>,
        url: String,
        theme: ExportTheme = ExportTheme.Light,
    ): File {
        val widthPx = 1080
        val pad = (widthPx * 0.04f).toInt()          // 页面左右边距
        val cardPad = (widthPx * 0.032f).toInt()     // 卡片内边距
        val dp = context.resources.displayMetrics.density
        val loader = coil.Coil.imageLoader(context)
        val contentMaxW = widthPx - 2 * pad - 2 * cardPad
        var imageBudget = MAX_CONTENT_IMAGES

        /** 同步加载网络位图并按宽度等比缩小 */
        suspend fun fetchBitmap(u: String, maxW: Int): Bitmap? {
            if (u.isBlank()) return null
            return runCatching {
                val req = coil.request.ImageRequest.Builder(context)
                    .data(if (u.startsWith("http")) u else Endpoints.abs(u))
                    .size(maxW)
                    .allowHardware(false)
                    .build()
                (loader.execute(req).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }.getOrNull()?.let { bmp ->
                if (bmp.width <= maxW || maxW <= 0) bmp
                else {
                    val scale = maxW.toFloat() / bmp.width
                    Bitmap.createScaledBitmap(bmp, maxW, (bmp.height * scale).toInt().coerceAtLeast(1), true)
                }
            }
        }

        /** 圆形头像（与应用一致；加载失败显示"饼"占位圆） */
        suspend fun avatarDrawable(u: String, sizePx: Int): android.graphics.drawable.BitmapDrawable {
            val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val bmp = if (imageBudget > 0) fetchBitmap(u, sizePx * 2).also { if (it != null) imageBudget-- } else null
            if (bmp != null) {
                val shader = android.graphics.BitmapShader(
                    bmp, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP
                )
                val side = minOf(bmp.width, bmp.height)
                val scale = sizePx.toFloat() / side
                val m = android.graphics.Matrix()
                m.setScale(scale, scale)
                m.postTranslate(-(bmp.width - side) / 2f * scale, -(bmp.height - side) / 2f * scale)
                shader.setLocalMatrix(m)
                paint.shader = shader
                c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
            } else {
                paint.color = theme.primaryContainer
                c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
                paint.shader = null
                paint.color = theme.onPrimaryContainer
                paint.textSize = sizePx * 0.5f
                paint.textAlign = android.graphics.Paint.Align.CENTER
                val fm = paint.fontMetrics
                c.drawText("饼", sizePx / 2f, sizePx / 2f - (fm.ascent + fm.descent) / 2f, paint)
            }
            return android.graphics.drawable.BitmapDrawable(context.resources, out)
        }

        fun tv(sizeSp: Float, bold: Boolean = false, color: Int = Color.DKGRAY): TextView =
            TextView(context).apply {
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
                setTextColor(color)
                paint.isFakeBoldText = bold
                setTextIsSelectable(false)
            }

        fun tv(text: String, sizeSp: Float, bold: Boolean = false, color: Int = Color.DKGRAY): TextView =
            tv(sizeSp, bold, color).apply { setText(text) }

        fun lp(w: Int = ViewGroup.LayoutParams.WRAP_CONTENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
            LinearLayout.LayoutParams(w, h)

        /** 小徽章（楼主 / #N / 用户组），与应用内 Badge 样式一致 */
        fun badge(text: String, bg: String, fg: String, sizeSp: Float = 10f): TextView =
            tv(text, sizeSp, color = Color.parseColor(fg)).apply {
                background = roundedBg(bg, 4 * dp)
                val ph = (sizeSp * 2.2f * context.resources.displayMetrics.scaledDensity / 3f).toInt().coerceAtLeast(8)
                setPadding(ph, ph / 3, ph, ph / 3)
            }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.background)
            setPadding(pad, pad, pad, pad)
        }

        // ---------- 头部：标题 + 来源 ----------
        root.addView(tv(title, 20f, bold = true, color = theme.onBackground))
        root.addView(tv("来源：$url", 10f, color = theme.onSurfaceVariant))
        root.addView(
            tv("烧饼社区客户端 · 共 ${posts.size} 楼", 10f, color = theme.onSurfaceVariant),
            lp().apply { topMargin = (2 * dp).toInt() }
        )

        var renderedPosts = 0
        var truncated = false

        for (p in posts) {
            // 预加载正文图片（ImageGetter 内无法挂起，先取好）
            val imgs = runCatching {
                Jsoup.parseBodyFragment(p.contentHtml).select("img").map { it.attr("abs:src").ifBlank { it.attr("src") } }
            }.getOrDefault(emptyList())
            val imgMap = mutableMapOf<String, android.graphics.drawable.BitmapDrawable>()
            for (src in imgs) {
                if (imageBudget <= 0) break
                val bmp = fetchBitmap(src, contentMaxW)
                if (bmp != null) {
                    imageBudget--
                    imgMap[src] = android.graphics.drawable.BitmapDrawable(context.resources, bmp)
                }
            }
            fun resolved(s: String) = if (s.startsWith("http")) s else Endpoints.abs(s)

            // ---------- 卡片：楼主正文用主题色淡底，评论用表面色容器（与应用一致） ----------
            val isMain = p.floor == 0
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(
                    String.format("#%06X", 0xFFFFFF and (if (isMain) theme.mainCard else theme.replyCard)),
                    12 * dp
                )
                setPadding(cardPad, cardPad, cardPad, cardPad)
            }

            // 头部行：头像 + 名字/徽章 + 时间（+ 右侧楼层号，与应用一致）
            val headRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val avatarSize = if (isMain) (40 * dp).toInt() else (30 * dp).toInt()
            val avatarIv = android.widget.ImageView(context).apply { setImageDrawable(avatarDrawable(p.avatarUrl, avatarSize)) }
            headRow.addView(avatarIv, lp(avatarSize, avatarSize).apply { rightMargin = (8 * dp).toInt() })

            val infoCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val nameRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            nameRow.addView(
                tv(p.authorName, if (isMain) 15f else 13f, bold = true, color = theme.onSurface),
                lp().apply { rightMargin = (6 * dp).toInt() }
            )
            if (isMain) {
                nameRow.addView(
                    badge("楼主", String.format("#%06X", 0xFFFFFF and theme.primaryContainer), String.format("#%06X", 0xFFFFFF and theme.onPrimaryContainer)),
                    lp().apply { rightMargin = (6 * dp).toInt() }
                )
            }
            if (p.userGroup.isNotBlank()) {
                nameRow.addView(
                    badge(p.userGroup, String.format("#%06X", 0xFFFFFF and theme.secondaryContainer), String.format("#%06X", 0xFFFFFF and theme.onSecondaryContainer))
                )
            }
            infoCol.addView(nameRow)
            infoCol.addView(
                tv(buildString {
                    if (p.authorUid > 0) append("UID ${p.authorUid}")
                    if (p.timeText.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(p.timeText)
                    }
                    if (p.ipLocation.isNotBlank()) append(" · ${p.ipLocation}")
                }, 10f, color = theme.onSurfaceVariant)
            )
            headRow.addView(infoCol, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f })
            if (!isMain && p.floor > 0) {
                // 楼层号：右上角纯文本（与应用内一致）
                headRow.addView(
                    tv("#${p.floor}", 12f, bold = true, color = theme.onSurfaceVariant).apply {
                        val ph = (6 * dp).toInt()
                        setPadding(ph, ph / 2, 0, ph / 2)
                    }
                )
            }
            card.addView(headRow)

            // 正文：富文本 + 真实图片（与应用 HtmlContent 展示一致）
            val body = tv(14f, color = theme.onSurface).apply {
                text = Html.fromHtml(
                    p.contentHtml,
                    Html.FROM_HTML_MODE_LEGACY,
                    Html.ImageGetter { src -> imgMap[resolved(src)] },
                    null
                )
                setLineSpacing((3 * dp).toFloat(), 1f)
            }
            card.addView(body, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (8 * dp).toInt() })

            // 最后编辑信息：分割线 + 小字（与应用内一致）
            if (p.editInfo.isNotBlank()) {
                val divider = android.view.View(context).apply {
                    setBackgroundColor(theme.outlineVariant)
                }
                card.addView(
                    divider,
                    lp(ViewGroup.LayoutParams.MATCH_PARENT, (1 * dp).toInt()).apply { topMargin = (8 * dp).toInt() }
                )
                card.addView(
                    tv(p.editInfo, 10f, color = theme.onSurfaceVariant),
                    lp().apply { topMargin = (6 * dp).toInt() }
                )
            }

            root.addView(card, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * dp).toInt()
            })

            // 高度保护：接近上限时停止渲染
            val specW = android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY)
            root.measure(specW, android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
            if (root.measuredHeight > MAX_IMAGE_HEIGHT) {
                truncated = true
                root.removeView(card)
                break
            }
            renderedPosts++
        }

        if (truncated || renderedPosts < posts.size) {
            root.addView(
                tv("内容过长，仅导出前 $renderedPosts 楼", 11f, color = theme.onSurfaceVariant),
                lp().apply { topMargin = (10 * dp).toInt() }
            )
        }

        // 测量 + 布局
        val specW = android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY)
        root.measure(specW, android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
        val h = root.measuredHeight.coerceAtLeast(200)
        root.layout(0, 0, widthPx, h)

        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))

        val file = File(exportDir(context), "${safeName(title)}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bmp.recycle()
        return file
    }

    // ---------------- 分享 ----------------

    fun shareFile(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享帖子").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportHtml(context: Context, title: String, posts: List<PostEntry>, url: String): File {
        val f = File(exportDir(context), "${safeName(title)}.html")
        f.writeText(buildHtml(title, posts, url))
        return f
    }

    fun exportMarkdown(context: Context, title: String, posts: List<PostEntry>, url: String): File {
        val f = File(exportDir(context), "${safeName(title)}.md")
        f.writeText(buildMarkdown(title, posts, url))
        return f
    }
}
