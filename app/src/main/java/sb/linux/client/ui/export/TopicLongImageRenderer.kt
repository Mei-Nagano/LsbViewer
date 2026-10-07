package sb.linux.client.ui.export
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.PostEntry
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Html
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

object TopicLongImageRenderer {
    internal const val MAX_IMAGE_HEIGHT = 6000
    /** 长图内容图片加载数量上限：保护内存与导出耗时 */
    private const val MAX_CONTENT_IMAGES = 80

    internal fun imagePageSlices(height: Int, pageHeight: Int): List<IntRange> {
        require(height > 0 && pageHeight > 0)
        return (0 until height step pageHeight).map { start -> start..minOf(start + pageHeight - 1, height - 1) }
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
        theme: TopicExport.ExportTheme = TopicExport.ExportTheme.Light,
        multiPage: Boolean = true,
    ): List<File> {
        val widthPx = 1080
        val pad = (widthPx * 0.04f).toInt()          // 页面左右边距
        val cardPad = (widthPx * 0.032f).toInt()     // 卡片内边距
        val dp = context.resources.displayMetrics.density
        val views = TopicExportViewFactory(context, dp)
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

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.background)
            setPadding(pad, pad, pad, pad)
        }

        // ---------- 头部：标题 + 来源 ----------
        root.addView(views.tv(title, 20f, bold = true, color = theme.onBackground))
        root.addView(views.tv("来源：$url", 10f, color = theme.onSurfaceVariant))
        root.addView(
            views.tv("烧饼社区客户端 · 共 ${posts.size} 楼", 10f, color = theme.onSurfaceVariant),
            views.lp().apply { topMargin = (2 * dp).toInt() }
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
                background = views.roundedBackground(
                    String.format("#%06X", 0xFFFFFF and (if (isMain) theme.mainCard else theme.replyCard)),
                    12 * dp
                )
                setPadding(cardPad, cardPad, cardPad, cardPad)
            }

            // 头部行：头像 + 名字/徽章 + 时间（+ 右侧楼层号，与应用一致）
            val headRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val avatarSize = if (isMain) (40 * dp).toInt() else (30 * dp).toInt()
            val avatarIv = android.widget.ImageView(context).apply { setImageDrawable(avatarDrawable(p.avatarUrl, avatarSize)) }
            headRow.addView(avatarIv, views.lp(avatarSize, avatarSize).apply { rightMargin = (8 * dp).toInt() })

            val infoCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val nameRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            nameRow.addView(
                views.tv(p.authorName, if (isMain) 15f else 13f, bold = true, color = theme.onSurface),
                views.lp().apply { rightMargin = (6 * dp).toInt() }
            )
            if (isMain) {
                nameRow.addView(
                    views.badge("楼主", String.format("#%06X", 0xFFFFFF and theme.primaryContainer), String.format("#%06X", 0xFFFFFF and theme.onPrimaryContainer)),
                    views.lp().apply { rightMargin = (6 * dp).toInt() }
                )
            }
            if (p.userGroup.isNotBlank()) {
                nameRow.addView(
                    views.badge(p.userGroup, String.format("#%06X", 0xFFFFFF and theme.secondaryContainer), String.format("#%06X", 0xFFFFFF and theme.onSecondaryContainer))
                )
            }
            infoCol.addView(nameRow)
            infoCol.addView(
                views.tv(buildString {
                    if (p.authorUid > 0) append("UID ${p.authorUid}")
                    if (p.timeText.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(p.timeText)
                    }
                    if (p.ipLocation.isNotBlank()) append(" · ${p.ipLocation}")
                }, 10f, color = theme.onSurfaceVariant)
            )
            headRow.addView(infoCol, views.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f })
            if (!isMain && p.floor > 0) {
                // 楼层号：右上角纯文本（与应用内一致）
                headRow.addView(
                    views.tv("#${p.floor}", 12f, bold = true, color = theme.onSurfaceVariant).apply {
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
            val body = views.tv(14f, color = theme.onSurface).apply {
                text = Html.fromHtml(
                    exportBody,
                    Html.FROM_HTML_MODE_LEGACY,
                    Html.ImageGetter { src -> imgMap[resolved(src)] },
                    null
                )
                setLineSpacing((3 * dp).toFloat(), 1f)
            }
            card.addView(body, views.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (8 * dp).toInt() })

            // 最后编辑信息：分割线 + 小字（与应用内一致）
            if (p.editInfo.isNotBlank()) {
                val divider = android.view.View(context).apply {
                    setBackgroundColor(theme.outlineVariant)
                }
                card.addView(
                    divider,
                    views.lp(ViewGroup.LayoutParams.MATCH_PARENT, (1 * dp).toInt()).apply { topMargin = (8 * dp).toInt() }
                )
                card.addView(
                    views.tv(p.editInfo, 10f, color = theme.onSurfaceVariant),
                    views.lp().apply { topMargin = (6 * dp).toInt() }
                )
            }

            root.addView(card, views.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * dp).toInt()
            })

        }

        // 只分配一页位图；同一楼层跨页也完整保留，不再丢弃超过高度上限的正文。
        val specW = android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY)
        root.measure(specW, android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
        val height = root.measuredHeight.coerceAtLeast(200)
        require(multiPage || height <= MAX_IMAGE_HEIGHT) { "内容超过单张长图上限，请开启自动分页后导出" }
        root.layout(0, 0, widthPx, height)
        return TopicLongImagePageWriter.write(root, widthPx, height, title, context)
    }

}

