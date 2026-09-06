package sb.linux.client.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Html
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

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

            /** 从应用 ColorScheme 生成导出主题 */
            fun fromColorScheme(cs: androidx.compose.material3.ColorScheme): ExportTheme {
                val bg = (if (cs.background.alpha < 1f) cs.surface else cs.background).toArgb()
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

    // ---------------- 长图 ----------------

    /** 每页高度上限：逐页分配位图，超长正文不截断。 */
    private const val MAX_IMAGE_HEIGHT = 6000

    internal fun imagePageSlices(height: Int, pageHeight: Int): List<IntRange> {
        require(height > 0 && pageHeight > 0)
        return (0 until height step pageHeight).map { start -> start..minOf(start + pageHeight - 1, height - 1) }
    }

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
    suspend fun renderLongImages(
        context: Context,
        title: String,
        posts: List<PostEntry>,
        url: String,
        theme: ExportTheme = ExportTheme.Light,
        multiPage: Boolean = true,
    ): List<File> {
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
                    val resolvedSrc = if (src.startsWith("http")) src else Endpoints.abs(src)
                    imgMap[resolvedSrc] = android.graphics.drawable.BitmapDrawable(context.resources, bmp).apply {
                        setBounds(0, 0, bmp.width, bmp.height)
                    }
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

            // 图片加载失败或达到内存保护上限时保留 URL，不能无声遗漏原帖图片。
            val exportBody = Jsoup.parseBodyFragment(p.contentHtml).body().apply {
                select("img").forEach { image ->
                    val src = resolved(image.attr("src"))
                    if (src !in imgMap) image.replaceWith(org.jsoup.nodes.Element("p").text("[图片未嵌入，可打开原图] $src"))
                }
            }.html()
            val body = tv(14f, color = theme.onSurface).apply {
                text = Html.fromHtml(
                    exportBody,
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

        }

        // 只分配一页位图；同一楼层跨页也完整保留，不再丢弃超过高度上限的正文。
        val specW = android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY)
        root.measure(specW, android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
        val height = root.measuredHeight.coerceAtLeast(200)
        require(multiPage || height <= MAX_IMAGE_HEIGHT) { "内容超过单张长图上限，请开启自动分页后导出" }
        root.layout(0, 0, widthPx, height)
        val slices = imagePageSlices(height, MAX_IMAGE_HEIGHT)
        val stamp = System.currentTimeMillis()
        return slices.mapIndexed { index, slice ->
            val bmp = Bitmap.createBitmap(widthPx, slice.last - slice.first + 1, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bmp)
                canvas.translate(0f, -slice.first.toFloat())
                root.draw(canvas)
                val suffix = if (slices.size > 1) "_第${index + 1}页" else ""
                File(TopicExportFiles.exportDir(context), "${TopicExportFiles.safeExportName(title)}_${stamp}${suffix}.png").also { file ->
                    file.outputStream().use { check(bmp.compress(Bitmap.CompressFormat.PNG, 95, it)) { "图片写入失败" } }
                }
            } finally { bmp.recycle() }
        }
    }

}
