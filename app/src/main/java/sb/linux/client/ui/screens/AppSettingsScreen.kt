package sb.linux.client.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import sb.linux.client.LocalMasterNav
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import sb.linux.client.data.syncSourceUsage
import sb.linux.client.data.Session
import sb.linux.client.data.AiClient
import sb.linux.client.data.ThemeModePref
import sb.linux.client.data.UpdateChecker
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.LoginRequiredBox
import sb.linux.client.ui.MarkdownText
import sb.linux.client.ui.PaletteStyles
import sb.linux.client.ui.colorSeed
import sb.linux.client.ui.colorToHsv
import sb.linux.client.ui.colorToKey
import sb.linux.client.ui.hsvColor
import sb.linux.client.ui.isCustomColorKey
import sb.linux.client.ui.onColorFor
import sb.linux.client.ui.paletteStyleLabel
import sb.linux.client.ui.CardColorOverrides
import sb.linux.client.ui.TopicCardView
import java.io.File



/** 设置分组标题：小号主色标签，上方留白把相邻分组断开 */
@Composable
private fun GroupLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp)
    )
}

/** 设置分组卡片容器：标题行可折叠，展开/收起带动画，标题可带图标 */
@Composable
internal fun GroupCard(
    title: String? = null,
    initiallyExpanded: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    // 箭头随展开状态旋转，比换图标的跳变更连贯
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "groupArrow")
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (title != null) ListItem(
                headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) },
                leadingContent = icon?.let { vector -> { SettingIcon(vector) } },
                trailingContent = {
                    Icon(
                        Icons.Filled.ExpandMore,
                        if (expanded) "收起" else "展开",
                        Modifier.rotate(arrowRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { expanded = !expanded },
            )
            if (title == null) content()
            else AnimatedVisibility(expanded) { Column(content = content) }
        }
    }
}

/** 设置项图标：主色图标 + 淡主色圆底，让长列表可以靠图标定位 */
@Composable
internal fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

/** 带开关的设置行 */
@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    // 禁用时标题与副标题一起变淡，只淡标题会让人以为副标题还可操作
    val alpha = if (enabled) 1f else .38f
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
    )
}

/** 人类可读的字节大小 */
private fun fmtBytes(b: Long): String = when {
    b >= 1024 * 1024 -> "%.1f MB".format(b / 1024f / 1024f)
    b >= 1024 -> "%.1f KB".format(b / 1024f)
    else -> "$b B"
}

/** 本地缓存分析：统计各项缓存占用并用饼图展示（3.12） */
@Composable
private fun CacheAnalysisCard(session: Session, refreshKey: Int = 0) {
    val context = LocalContext.current
    var sizes by remember { mutableStateOf<List<Pair<String, Long>>?>(null) }

    LaunchedEffect(refreshKey) {
        sizes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val app = context.applicationContext
            fun prefsSize(name: String): Long =
                File(app.filesDir.parentFile, "shared_prefs/$name.xml").takeIf { it.exists() }?.length() ?: 0L
            fun dirSize(dir: File?): Long {
                if (dir == null || !dir.exists()) return 0
                return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            val imgDir = runCatching { coil.Coil.imageLoader(app).diskCache?.directory?.toFile() }.getOrNull()
            val imgSize = dirSize(imgDir)
            val cacheTotal = dirSize(app.cacheDir)
            listOf(
                "图片缓存" to imgSize,
                "首页缓存" to prefsSize("lsb_home_cache"),
                "侧边栏缓存" to prefsSize("lsb_sidebar"),
                "阅读量缓存" to prefsSize("lsb_views"),
                "临时文件" to (cacheTotal - imgSize).coerceAtLeast(0),
            ).filter { it.second > 0 }
        }
    }

    Column {
        Text(
            "本地缓存分析",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        val data = sizes
        if (data == null) {
            Text(
                "统计中…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (data.isEmpty()) {
            Text(
                "暂无本地缓存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val total = data.sumOf { it.second }.coerceAtLeast(1)
            val palette = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.primaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 饼图
                Canvas(Modifier.size(120.dp)) {
                    var start = -90f
                    data.forEachIndexed { i, (_, size) ->
                        val sweep = size.toFloat() / total * 360f
                        drawArc(
                            color = palette[i % palette.size],
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = true,
                        )
                        start += sweep
                    }
                }
                Spacer(Modifier.width(16.dp))
                // 图例
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.forEachIndexed { i, (name, size) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(palette[i % palette.size])
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${fmtBytes(size)} · ${size * 100 / total}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "合计 ${fmtBytes(total)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


/**
 * 色相滑条：背景常驻红→黄→绿→青→蓝→紫→红的彩虹渐变，
 * 滑到哪里就显示对应颜色，不再是盲操作。
 */
@Composable
private fun HueGradientSlider(hue: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onValueChange((down.position.x / size.width * 360f).coerceIn(0f, 360f))
                    while (true) {
                        val change = awaitDragOrCancellation(down.id) ?: break
                        change.consume()
                        onValueChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                    }
                }
            }
    ) {
        // 常驻彩虹渐变背景：与色相值一一对应
        drawRect(
            Brush.horizontalGradient(
                listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsvColor(it, 1f, 1f) }
            )
        )
        // 当前位置圆形手柄
        val x = size.width * (hue.coerceIn(0f, 360f) / 360f)
        val cy = size.height / 2f
        drawCircle(Color.White, radius = size.height * 0.30f, center = Offset(x, cy))
        drawCircle(
            Color.Black, radius = size.height * 0.30f,
            center = Offset(x, cy), style = Stroke(size.height * 0.06f)
        )
    }
}

/** 统一的 HSV 取色对话框：SV 方块 + 彩虹色相滑条 + 预设色 + 实时 HEX，遵循 MD3 对话框规范 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorPickerDialog(
    title: String = "自定义主题色",
    initial: Color,
    presets: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
    onSavePreset: ((Color) -> Unit)? = null,
    onRemovePreset: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null,
) {
    val initHsv = colorToHsv(initial)
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var sat by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }
    val current = remember(hue, sat, value) { hsvColor(hue, sat, value) }

    fun applyColor(c: Color) {
        val hsv = colorToHsv(c)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))

                val hueColor = hsvColor(hue, 1f, 1f)
                // SV 方块：横轴饱和度、纵轴亮度，可拖拽/点选（合并为单个手势循环，避免点按与拖动互相吞事件）
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                fun apply(pos: Offset) {
                                    sat = (pos.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                                }
                                apply(down.position)
                                while (true) {
                                    val change = awaitDragOrCancellation(down.id) ?: break
                                    change.consume()
                                    apply(change.position)
                                }
                            }
                        }
                ) {
                    drawRect(Brush.linearGradient(listOf(Color.White, hueColor)))
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.Black),
                            start = Offset(0f, 0f), end = Offset(0f, size.height)
                        )
                    )
                    val x = size.width * sat
                    val y = size.height * (1f - value)
                    drawCircle(Color.White, radius = size.minDimension * 0.05f, center = Offset(x, y))
                    drawCircle(
                        Color.Black, radius = size.minDimension * 0.035f,
                        center = Offset(x, y), style = Stroke(size.minDimension * 0.012f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 色相滑条：常驻彩虹渐变背景，划到哪里显示对应颜色
                HueGradientSlider(hue, { hue = it })
                Spacer(Modifier.height(4.dp))
                // 实时预览 + 十六进制值
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(current)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "当前颜色",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            current.hex(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 预设颜色：点击应用，长按移除（保存的预设统一在此展示）
                if (presets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "预设颜色（点击应用，长按移除）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets.size) { i ->
                            val key = presets[i]
                            val c = colorSeed(key)
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c)
                                    .combinedClickable(
                                        onClick = { applyColor(c) },
                                        onLongClick = { onRemovePreset?.invoke(key) }
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key.removePrefix("custom#").take(4),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = sb.linux.client.ui.onColorFor(c)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 操作按钮：取消 / 保存到预选 / 应用
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.weight(1f))
                    if (onSavePreset != null) {
                        OutlinedButton(onClick = { onSavePreset(current) }) { Text("存为预选") }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = { onPick(current) }) { Text("应用") }
                }
                if (onReset != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = onReset) { Text("重置为跟随主题") }
                    }
                }
            }
        }
    }
}

/** 把 Color 格式化为 #RRGGBB（Compose sRGB 编码在 value 高 32 位，必须经 toArgb 取色） */
private fun Color.hex(): String {
    val rgb = (toArgb().toLong() and 0xFFFFFFL)
    return "#" + rgb.toString(16).padStart(6, '0').uppercase()
}

/** 重置按钮（3.9）：右上角图标 + 简洁确认对话框 */
@Composable
private fun ResetAction(title: String, onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.RestartAlt, "重置")
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(title) },
            text = { Text("恢复默认设置？") },
            confirmButton = {
                TextButton(onClick = { show = false; onConfirm() }) { Text("重置") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("取消") } }
        )
    }
}

/**
 * 应用设置（区别于源站个人设置）：分类菜单入口，点进子页面再进行具体设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(session: Session, nav: NavHostController) {
    var resetTick by remember { mutableStateOf(0) }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    var showCardRedemptions by remember { mutableStateOf(false) }
    var redemptionRevision by remember { mutableIntStateOf(0) }
    val cardRedemptions = remember(showCardRedemptions, redemptionRevision) { session.settings.cardRedemptions() }
    val entries = listOf(
        Triple("常规设置", "打开链接方式 · DoH · 匿名免费图床 · 检查更新", "generalSettings"),
        Triple("浏览设置", "滚动模式 · 新帖子折叠 · Base64 · 表格 · UID · 评论 · 弹幕", "browseSettings"),
        Triple("数据管理", "缓存分析 · 分类清理 · 历史 · 收藏 · AI 数据 · 自动清理", "dataManagement"),
        Triple("使用统计", "浏览 · 收藏 · 发帖回帖 · 积分支出 · AI 总结", "usageStats"),
        Triple("外观设置", "深浅模式 · 主题色 · 背景 · 字体大小粗细 · 卡片颜色 · 底栏", "themeSettings"),
        Triple("AI 总结", "API · 模型列表 · 提示词 · 自动总结 · 总结历史", "aiSettings"),
        Triple("备份设置", "JSON · WebDAV · 导入 · 导出", "transferSettings"),
        Triple("屏蔽词管理", "源站同步 · 屏蔽用户 · 关键词过滤", "blockWords"),
        Triple("发卡兑换记录", "自动保存已兑换帖子、卡名与兑换码", "cardRedemptions"),
        Triple("已导出的帖子", "HTML · Markdown · 长图 · 多页导出", "exportedTopics"),
    )
    val visibleEntries = if (settingsQuery.isBlank()) entries else sb.linux.client.data.SettingsSearchIndex.search(settingsQuery)
        .map { Triple(it.title, it.category, it.route) }
    // 按目标路由取图标：搜索结果命中的是具体选项，也能借父分类的图标定位
    val entryIcons = mapOf(
        "generalSettings" to Icons.Filled.Tune,
        "dohSettings" to Icons.Filled.Dns,
        "browseSettings" to Icons.AutoMirrored.Filled.MenuBook,
        "dataManagement" to Icons.Filled.Storage,
        "usageStats" to Icons.Filled.Insights,
        "themeSettings" to Icons.Filled.Palette,
        "aiSettings" to Icons.Filled.AutoAwesome,
        "transferSettings" to Icons.Filled.Backup,
        "blockWords" to Icons.Filled.Block,
        "cardRedemptions" to Icons.Filled.Redeem,
        "exportedTopics" to Icons.Filled.Description,
    )
    // 平板双栏：本页位于主栏，返回箭头应弹出主栏（回到「我的」）；手机走单栈
    val masterNav = LocalMasterNav.current
    if (showCardRedemptions) {
        AlertDialog(
            onDismissRequest = { showCardRedemptions = false },
            title = { Text("发卡兑换记录（${cardRedemptions.size}）") },
            text = {
                if (cardRedemptions.isEmpty()) {
                    Text("暂无记录。打开已兑换过的发卡帖后，应用会从“我的购买记录”自动建档。")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cardRedemptions, key = { "${it.topicId}-${it.code}" }) { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showCardRedemptions = false
                                    nav.navigate("topic/${record.topicId}")
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(record.topicTitle.ifBlank { "帖子 #${record.topicId}" }, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    Text(record.cardTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SelectionContainer { Text(record.code, style = MaterialTheme.typography.bodyMedium) }
                                    Text(
                                        listOf(record.price, record.sourceTime).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (cardRedemptions.isNotEmpty()) TextButton(onClick = {
                    session.settings.clearCardRedemptions()
                    redemptionRevision++
                }) { Text("清空记录") }
            },
            dismissButton = { TextButton(onClick = { showCardRedemptions = false }) { Text("关闭") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (masterNav != null) masterNav.popBackStack() else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 一级菜单：重置所有应用设置（3.9）
                    ResetAction(
                        title = "重置所有应用设置",
                    ) {
                        session.resetAllAppSettings()
                        resetTick++
                        session.showToast("已重置所有应用设置")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = settingsQuery,
                onValueChange = { settingsQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                singleLine = true,
                placeholder = { Text("搜索设置项，如「主题色」「缓存」") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (settingsQuery.isNotBlank()) {{
                    IconButton(onClick = { settingsQuery = "" }) { Icon(Icons.Filled.Close, "清空") }
                }} else null,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            GroupCard {
                if (visibleEntries.isEmpty()) {
                    Text(
                        "没有找到相关设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                    )
                } else visibleEntries.forEachIndexed { index, (title, subtitle, route) ->
                    SettingMenuRow(title, subtitle, entryIcons[route]) {
                        if (route == "cardRedemptions") showCardRedemptions = true else nav.navigate(route)
                    }
                    if (index < visibleEntries.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = 66.dp, end = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

/** 独立数据管理：按项删除，并选择应用完全退出后需要自动清理的项目。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(session: Session, nav: NavHostController) {
    val options = listOf(
        "home" to ("首页与侧边栏缓存" to "帖子列表和侧边栏快照"),
        "topic" to ("帖子与阅读量缓存" to "正文、评论、弹幕和阅读量"),
        "images" to ("图片缓存" to "头像及正文图片的磁盘缓存"),
        "reading_positions" to ("阅读位置" to "各帖子上次看到的位置"),
        "ai_history" to ("AI 总结历史" to "已保存的总结与运行内缓存"),
        "history" to ("浏览历史" to "本地帖子浏览记录"),
        "favorites" to ("本地收藏" to "帖子收藏快照"),
        "comment_favorites" to ("评论收藏" to "收藏的评论快照"),
        "card_redemptions" to ("发卡兑换记录" to "已自动保存的帖子与兑换码"),
        "usage_stats" to ("使用统计" to "操作事件与已保存的 AI 分析"),
        "ai_config" to ("AI 配置与密钥" to "API 地址、密钥、模型、提示词及配置方案"),
        "drafts" to ("发帖草稿" to "未发布的标题、正文和特殊帖配置"),
        "link_previews" to ("链接预览缓存" to "网站标题、简介和图标地址"),
        "background_image" to ("自定义背景图片" to "删除应用内保存的背景图片，恢复主题底色"),
    )
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var autoClear by remember { mutableStateOf(session.settings.autoClearItems) }
    var confirmClear by remember { mutableStateOf(false) }
    var cacheRevision by remember { mutableIntStateOf(0) }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清理所选数据") },
        text = { Text("确定删除已选择的 ${selected.size} 类本地数据吗？此操作无法撤销。") },
        confirmButton = { TextButton(onClick = {
            confirmClear = false
            session.clearDataItems(selected)
            selected = emptySet()
            cacheRevision++
        }) { Text("清理") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("数据管理") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupLabel("空间占用")
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    CacheAnalysisCard(session, cacheRevision)
                }
            }
            GroupLabel("立即清理")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, (key, labels) ->
                    val checked = key in selected
                    Surface(
                        onClick = {
                            selected = if (checked) selected - key else selected + key
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            if (checked) 1.5.dp else 0.5.dp,
                            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f)) {
                                Text(labels.first, style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                                Text(labels.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (checked) Icon(Icons.Filled.CheckCircle, "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Button(
                onClick = { confirmClear = true }, enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("清理所选项") }

            GroupCard("退出时自动清理", initiallyExpanded = false, icon = Icons.Filled.Delete) {
                Text(
                    "正常退出时清理所选项目；系统强制结束进程时，会在下次启动补做。所选历史、草稿或 AI 配置会永久删除，请谨慎开启；账号登录不受影响。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
                options.forEach { (key, labels) ->
                    SwitchRow(
                        labels.first, labels.second,
                        checked = key in autoClear,
                        onCheckedChange = { enabled ->
                            autoClear = if (enabled) autoClear + key else autoClear - key
                            session.settings.autoClearItems = autoClear
                        },
                    )
                }
            }
        }
    }
}

/** 本地使用统计：只统计应用内真实完成的行为；AI 分析同时参考浏览历史与收藏快照。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UsageStatsScreen(session: Session, nav: NavHostController) {
    var revision by remember { mutableIntStateOf(0) }
    val allEvents = remember(revision) { session.settings.usageEvents() }
    var days by rememberSaveable { mutableIntStateOf(30) }
    val cutoff = remember(days) {
        if (days == 0) 0L else System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
    }
    val events = remember(allEvents, cutoff) { allEvents.filter { it.at >= cutoff } }
    val sourceTotals = remember(revision, session.loginState.userId) {
        runCatching { org.json.JSONObject(session.settings.sourceUsageJson) }.getOrDefault(org.json.JSONObject())
            .takeIf { it.optLong("userId") == session.loginState.userId }
    }
    val counts = remember(events) { events.groupingBy { it.type }.eachCount() }
    val pointsSpent = remember(events) { events.filter { it.type == "coin" || it.type == "donate" }.sumOf { it.value } }
    val history = remember(revision) { session.settings.historyList() }
    val favorites = remember(revision) { session.settings.favoriteList() }
    val sourcePoints = remember(revision, cutoff, sourceTotals) {
        if (sourceTotals == null) emptyList() else session.settings.sourcePoints().filter { it.timestamp >= cutoff }
    }
    val sourceIncome = sourcePoints.filter { it.positive }.sumOf { it.change.toLongOrNull() ?: 0 }
    val sourceExpense = sourcePoints.filterNot { it.positive }.sumOf { it.change.toLongOrNull() ?: 0 }
    var syncingPoints by remember { mutableStateOf(false) }
    var aiSummary by remember { mutableStateOf(session.settings.usageAiSummary) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runAiSummary() {
        if (aiBusy) return
        aiBusy = true
        aiError = null
        scope.launch {
            try {
                val topViewed = events.filter { it.type == "view" && it.title.isNotBlank() }
                    .groupingBy { it.title }.eachCount().entries.sortedByDescending { it.value }.take(8)
                val content = buildString {
                    appendLine("统计范围：${if (days == 0) "全部" else "最近 ${days} 天"}")
                    appendLine("浏览 ${counts["view"] ?: 0} 次，收藏 ${counts["favorite"] ?: 0} 次，发帖 ${counts["topic"] ?: 0} 次，回帖 ${counts["reply"] ?: 0} 次，源站积分支出 $sourceExpense。")
                    appendLine("源站当前总数：发帖 ${sourceTotals?.optInt("topics")}，回帖 ${sourceTotals?.optInt("replies")}，收藏 ${sourceTotals?.optInt("favorites")}。")
                    appendLine("本地浏览历史现有 ${history.size} 帖，本地帖子收藏 ${favorites.size} 帖，评论收藏 ${session.settings.commentFavoriteList().size} 条。")
                    appendLine("已同步源站积分流水 ${sourcePoints.size} 条，收入 $sourceIncome，支出 $sourceExpense。源站流水和本地操作统计可能重叠，不得相加；仅分析已同步范围。")
                    appendLine(sourcePoints.take(40).joinToString("\n") { "${it.timeText} ${it.reason} ${if (it.positive) "+" else "-"}${it.change}" })
                    if (topViewed.isNotEmpty()) appendLine("常看主题：${topViewed.joinToString { "${it.key}（${it.value}次）" }}")
                    if (favorites.isNotEmpty()) appendLine("近期收藏：${favorites.take(10).joinToString { it.title }}")
                    append("请分析使用偏好、近期关注主题和参与习惯，并给出简短总结。")
                }
                aiSummary = AiClient.summarize(session.settings, "我的 Linux.do/SB 使用统计", content) { }
                session.settings.usageAiSummary = aiSummary
            } catch (e: Exception) {
                aiError = e.message ?: "AI 总结失败"
            } finally {
                aiBusy = false
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清空使用统计") },
        text = { Text("将删除操作统计和已保存的 AI 分析，不影响浏览历史与收藏。") },
        confirmButton = { TextButton(onClick = {
            session.settings.clearUsageStats(); aiSummary = ""; revision++; confirmClear = false
        }) { Text("清空") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("使用统计") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, "清空统计") } },
        )
    }) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(7 to "7 天", 30 to "30 天", 0 to "全部").forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = days == value, onClick = { days = value },
                            shape = SegmentedButtonDefaults.itemShape(index, 3), modifier = Modifier.weight(1f),
                        ) { Text(label) }
                    }
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "浏览" to (counts["view"] ?: 0), "收藏（源站）" to (sourceTotals?.optInt("favorites") ?: "未同步"),
                        "发帖（源站）" to (sourceTotals?.optInt("topics") ?: "未同步"), "回帖（源站）" to (sourceTotals?.optInt("replies") ?: "未同步"),
                        "积分支出" to sourceExpense, "历史帖子" to history.size,
                    ).forEach { (label, value) ->
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.width(104.dp)) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                GroupCard {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("源站积分流水", style = MaterialTheme.typography.titleMedium)
                        Text("当前范围 ${sourcePoints.size} 条 · 收入 $sourceIncome · 支出 $sourceExpense")
                        Text("发帖、回帖、收藏为个人主页当前总数；积分按所选日期汇总完整流水。浏览次数来自本机。同步全部分页，成功后统一更新。", style = MaterialTheme.typography.bodySmall)
                        Button(enabled = !syncingPoints && session.loginState.loggedIn, onClick = {
                            syncingPoints = true
                            scope.launch {
                                try {
                                    session.syncSourceUsage()
                                    revision++
                                    session.showToast("源站帖子、回帖、收藏及积分已同步")
                                } catch (e: Exception) { session.showToast(e.message ?: "同步失败") }
                                finally { syncingPoints = false }
                            }
                        }) { Text(if (syncingPoints) "同步中…" else "同步个人主页") }
                    }
                }
            }
            item {
                GroupCard {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI 使用分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        when {
                            aiBusy -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text("正在分析…")
                            }
                            aiError != null -> Text(aiError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            aiSummary.isNotBlank() -> MarkdownText(aiSummary)
                            else -> Text("根据统计、浏览历史和收藏内容生成兴趣与参与习惯总结。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = ::runAiSummary, enabled = !aiBusy && AiClient.isConfigured(session.settings), modifier = Modifier.fillMaxWidth()) {
                            Text(if (aiSummary.isBlank()) "生成 AI 总结" else "重新生成")
                        }
                        if (!AiClient.isConfigured(session.settings)) Text("请先在 AI 总结设置中配置 API。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (events.isNotEmpty()) {
                item { GroupLabel("最近活动") }
                items(events.take(30), key = { "${it.at}-${it.type}-${it.topicId}" }) { event ->
                    val label = when (event.type) {
                        "view" -> "浏览帖子"; "favorite" -> "收藏帖子"; "topic" -> "发布帖子"
                        "reply" -> "发表回复"; "coin" -> "投币 ${event.value} 积分"; else -> "打赏 ${event.value} 积分"
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { if (event.title.isNotBlank()) Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = { Text(java.text.SimpleDateFormat("MM-dd HH:mm", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(java.util.Date(event.at)), style = MaterialTheme.typography.labelSmall) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                    )
                }
            } else item { EmptyBox("该时间范围内暂无统计；新版开始记录后会显示在这里") }
        }
    }
}

/** 设置菜单行：图标 + 标题 + 副标题 + 右箭头 */
@Composable
internal fun SettingMenuRow(title: String, subtitle: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SettingIcon(icon)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 浏览设置：列表与分页、正文与评论、文本选择操作。缓存分析归入数据管理。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSettingsScreen(session: Session, nav: NavHostController) {
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }
    // 重置后强制刷新本地输入框状态
    var resetTick by remember { mutableStateOf(0) }
    // 滚动模式生效分类（3.13）：all / home / forum / topic
    var applyScope by remember { mutableStateOf("all") }
    val homeInf = session.effectiveInfiniteScroll("home")
    val forumInf = session.effectiveInfiniteScroll("forum")
    val topicInf = session.effectiveInfiniteScroll("topic")
    val scopeInf = when (applyScope) {
        "home" -> homeInf; "forum" -> forumInf; "topic" -> topicInf
        else -> session.infiniteScroll
    }
    // 任一分类为翻页时展示翻页相关设置（3.14）
    val anyPaged = !(homeInf && forumInf && topicInf)

    // 每页帖子数 / 评论数输入框本地文本（3.10 / 3.11）
    var topicsText by remember(resetTick, session.topicsPerPage) {
        mutableStateOf("${session.topicsPerPage}")
    }
    var commentsText by remember(resetTick, session.settings.commentsPerPage) {
        mutableStateOf("${session.settings.commentsPerPage}")
    }

    fun applyScrollMode(infinite: Boolean) {
        when (applyScope) {
            "home" -> session.saveScrollModeOverride("home", infinite)
            "forum" -> session.saveScrollModeOverride("forum", infinite)
            "topic" -> session.saveScrollModeOverride("topic", infinite)
            else -> {
                session.saveInfiniteScroll(infinite)
                session.clearScrollModeOverrides()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 本菜单重置（3.9）
                    ResetAction(
                        title = "重置浏览设置",
                    ) {
                        session.resetBrowseSettings()
                        resetTick++
                        session.showToast("已重置浏览设置")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {


            GroupCard("列表与分页", icon = Icons.AutoMirrored.Filled.ViewList) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(
                        "滚动模式",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "选择生效分类后切换模式；当前：首页${if (homeInf) "无限滚动" else "翻页"} · 板块内${if (forumInf) "无限滚动" else "翻页"} · 评论${if (topicInf) "无限滚动" else "翻页"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    // 生效分类选择（MD3 FilterChip，3.13）。
                    // topic 范围实际作用于帖子内评论区的分页，故文案为「仅评论」
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "all" to "全部",
                            "home" to "仅首页",
                            "forum" to "仅板块",
                            "topic" to "仅评论",
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = applyScope == key,
                                onClick = { applyScope = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // MD3 SegmentedButton 切换无限滚动 / 翻页（3.13）
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = scopeInf,
                            onClick = { applyScrollMode(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("无限滚动") }
                        SegmentedButton(
                            selected = !scopeInf,
                            onClick = { applyScrollMode(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("翻页") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "分页按评论树根节点分组，子回复随父评论展示；无限滚动按需加载后续评论",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 翻页相关设置：仅当存在翻页分类时展示（3.14）
                if (anyPaged) {
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = topicsText,
                            onValueChange = { t ->
                                topicsText = t.filter { it.isDigit() }.take(3)
                                topicsText.toIntOrNull()?.let { session.saveTopicsPerPage(it) }
                            },
                            label = { Text("每页帖子数（5 - 50）") },
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = commentsText,
                            onValueChange = { t ->
                                commentsText = t.filter { it.isDigit() }.take(3)
                                commentsText.toIntOrNull()?.let { session.settings.commentsPerPage = it }
                            },
                            label = { Text("评论每页数量（5 - 100）") },
                            suffix = { Text("条") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "翻页模式下每页展示的帖子与评论条数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                SwitchRow(
                    "启动时不刷新首页",
                    "打开应用直接显示上次缓存内容，手动刷新才更新",
                    checked = session.homeCacheEnabled,
                    onCheckedChange = { session.settings.homeKeepCache = it; session.homeCacheEnabled = it }
                )
                SwitchRow(
                    "折叠刷新后的新帖子",
                    "刷新回到顶部，把新增帖子收纳为折叠条，点击后展开",
                    checked = session.collapseNewTopics,
                    onCheckedChange = { session.saveCollapseNewTopics(it) }
                )
                SwitchRow(
                    "侧边栏双列显示",
                    "快捷功能与版块列表双列排布（可在首页侧边栏切换）",
                    checked = session.sidebarTwoColumns,
                    onCheckedChange = { session.saveSidebarTwoColumns(it) }
                )
                SwitchRow(
                    "侧边栏展示在线用户",
                    "在侧边栏展示源站在线用户及其人数",
                    checked = session.showOnlineUsers,
                    onCheckedChange = { session.saveShowOnlineUsers(it) }
                )
            }
            GroupCard("正文与评论", icon = Icons.AutoMirrored.Filled.Chat) {
                SwitchRow(
                    "显示打赏弹幕",
                    "在帖子正文与评论区之间滚动展示打赏信息，可在帖子菜单中单独开关",
                    checked = session.danmakuOn,
                    onCheckedChange = { session.saveDanmaku(it) }
                )
                SwitchRow(
                    "Markdown 表格横向滑动",
                    if (session.tableDisplayMode == 1) "保持单元格可读宽度，可左右滑动" else "关闭时自动压缩列宽以适应手机屏幕",
                    checked = session.tableDisplayMode == 1,
                    onCheckedChange = { session.saveTableDisplayMode(if (it) 1 else 0) }
                )
                SwitchRow(
                    "显示 UID",
                    "在帖子作者头像下方仅显示 UID 数字",
                    checked = session.showUid,
                    onCheckedChange = { session.saveShowUid(it) }
                )
                SwitchRow(
                    "智能识别编码内容",
                    "识别 Base64、URL Base64、Hex、URL、Unicode 和显式 ROT13 内容",
                    checked = session.smartDecodeEnabled,
                    onCheckedChange = { session.saveSmartDecodeEnabled(it) }
                )
            }
            GroupCard("帖子交互", initiallyExpanded = false, icon = Icons.Filled.TouchApp) {
                // 正文长按已交回系统原生选择菜单（复制/翻译/搜索由系统提供），这里不再有自定义菜单项配置
                SwitchRow(
                    "对话串联",
                    "源站已支持树形结构评论，此功能不再需要；开启后评论区显示「查看对话」入口",
                    checked = session.threadDialogEnabled,
                    onCheckedChange = { session.saveThreadDialog(it) }
                )
                SwitchRow(
                    "取消收藏确认",
                    "关闭后已收藏帖子点击收藏图标直接取消收藏，不再提示",
                    checked = session.unfavoriteConfirm,
                    onCheckedChange = { session.saveUnfavoriteConfirm(it) }
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** AI 总结设置子页面（OpenAI 兼容 API） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(session: Session, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }
    var aiEnabled by remember { mutableStateOf(session.settings.aiEnabled) }
    var aiAutoRun by remember { mutableStateOf(session.settings.aiAutoRun) }
    var aiUrl by remember { mutableStateOf(session.settings.aiUrl) }
    var aiKey by remember { mutableStateOf(session.settings.aiKey) }
    var aiModel by remember { mutableStateOf(session.settings.aiModel) }
    var aiPrompt by remember { mutableStateOf(session.settings.aiPrompt) }
    var aiTemp by remember { mutableStateOf(session.settings.aiTemp.toString()) }
    var aiIncludeComments by remember { mutableStateOf(session.settings.aiIncludeComments) }
    var modelOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var modelBusy by remember { mutableStateOf(false) }
    var historyTick by remember { mutableIntStateOf(0) }
    var selectedSummary by remember { mutableStateOf<sb.linux.client.data.AiSummaryRecord?>(null) }
    var favoriteOnly by remember { mutableStateOf(false) }
    var historyLimit by remember { mutableIntStateOf(30) }
    val summaryHistory = remember(historyTick, favoriteOnly) { session.aiSummaryHistory().filter { !favoriteOnly || it.favorite } }
    var presetTick by remember { mutableIntStateOf(0) }
    var presetName by remember { mutableStateOf("") }
    var presetMenu by remember { mutableStateOf(false) }
    val aiPresets = remember(presetTick) { session.settings.aiConfigPresets() }

    selectedSummary?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedSummary = null },
            title = { Text(record.title.ifBlank { "帖子 #${record.topicId}" }) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(record.summary, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedSummary = null
                    nav.navigate("topic/${record.topicId}?p=1&floor=0")
                }) { Text("打开原帖") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        session.removeAiSummary(record)
                        historyTick++
                        selectedSummary = null
                    }) { Text("删除") }
                    TextButton(onClick = {
                        session.favoriteAiSummary(record, !record.favorite)
                        selectedSummary = record.copy(favorite = !record.favorite)
                        historyTick++
                    }) { Text(if (record.favorite) "取消收藏" else "收藏总结") }
                    TextButton(onClick = { selectedSummary = null }) { Text("关闭") }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 总结") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {


            GroupCard {
                Text(
                    "AI 配置方案",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 4.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = presetName, onValueChange = { presetName = it },
                        label = { Text("方案名称 / 选择方案") }, singleLine = true,
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { presetMenu = true }) { Icon(Icons.Filled.ArrowDropDown, "选择已保存方案") }
                                DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                                    if (aiPresets.isEmpty()) DropdownMenuItem(text = { Text("暂无已保存方案") }, onClick = {}, enabled = false)
                                    aiPresets.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.name) },
                                            trailingIcon = { IconButton(onClick = {
                                                session.settings.removeAiPreset(preset.name); presetTick++
                                            }) { Icon(Icons.Filled.Delete, "删除方案") } },
                                            onClick = {
                                                session.settings.applyAiPreset(preset)
                                                aiUrl = preset.url; aiKey = preset.key; aiModel = preset.model
                                                aiTemp = preset.temperature.toString(); aiPrompt = preset.prompt
                                                aiIncludeComments = preset.includeComments; presetName = preset.name
                                                presetMenu = false
                                                session.showToast("已切换到「${preset.name}」")
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            session.settings.aiUrl = aiUrl.trim(); session.settings.aiKey = aiKey.trim()
                            session.settings.aiModel = aiModel.trim(); session.settings.aiPrompt = aiPrompt.trim()
                            session.settings.aiTemp = (aiTemp.toFloatOrNull() ?: 0.3f).coerceIn(0f, 2f)
                            session.settings.aiIncludeComments = aiIncludeComments
                            session.settings.saveCurrentAiPreset(presetName)
                            presetTick++; msg = "✓ AI 配置方案已保存"
                        },
                        enabled = presetName.isNotBlank(),
                    ) { Text("保存") }
                }

            }

            GroupCard {
                SwitchRow(
                    "开启 AI 总结",
                    "帖子顶部显示「AI 总结本帖」按钮，点按后才请求；关闭则帖子内不显示 AI 入口",
                    checked = aiEnabled,
                    onCheckedChange = {
                        aiEnabled = it
                        session.settings.aiEnabled = it
                    }
                )
                SwitchRow(
                    "打开帖子自动总结",
                    "无需点按钮，进帖即请求总结。水贴也会照总结并消耗额度，建议保持关闭",
                    checked = aiAutoRun,
                    enabled = aiEnabled,
                    onCheckedChange = {
                        aiAutoRun = it
                        session.settings.aiAutoRun = it
                    }
                )
                SwitchRow(
                    "解析评论区",
                    "开启后连同所有回复一起总结；关闭则仅总结楼主正文",
                    checked = aiIncludeComments,
                    enabled = aiEnabled,
                    onCheckedChange = {
                        aiIncludeComments = it
                        session.settings.aiIncludeComments = it
                    }
                )
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = aiUrl,
                        onValueChange = { aiUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API 地址") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = aiKey,
                        onValueChange = { aiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API 密钥") },
                        placeholder = { Text("sk-…") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = aiModel,
                            onValueChange = { aiModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("模型") },
                            placeholder = { Text("gpt-4o-mini") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (modelOptions.isNotEmpty()) IconButton(onClick = { modelMenuOpen = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, "选择模型")
                                }
                            },
                        )
                        DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                            modelOptions.forEach { model ->
                                DropdownMenuItem(text = { Text(model) }, onClick = { aiModel = model; modelMenuOpen = false })
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            session.settings.aiUrl = aiUrl.trim()
                            session.settings.aiKey = aiKey.trim()
                            modelBusy = true
                            scope.launch {
                                try {
                                    modelOptions = AiClient.fetchModels(session.settings)
                                    if (modelOptions.isEmpty()) msg = "✗ 接口没有返回可用模型"
                                    else { modelMenuOpen = true; msg = "✓ 已获取 ${modelOptions.size} 个模型" }
                                } catch (e: Exception) { msg = "✗ ${e.message}" }
                                finally { modelBusy = false }
                            }
                        },
                        enabled = !modelBusy && aiUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (modelBusy) "获取中…" else "获取模型列表") }
                    OutlinedTextField(
                        value = aiTemp,
                        onValueChange = { if (it.length <= 4 && (it.isEmpty() || it.matches(Regex("""\d*\.?\d*""")))) aiTemp = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("温度 (0 - 2)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                        label = { Text("自定义总结提示词") },
                        placeholder = { Text(AiClient.DEFAULT_SUMMARY_PROMPT) },
                        supportingText = { Text("留空使用内置提示词") },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Button(
                    onClick = {
                        session.settings.aiUrl = aiUrl.trim()
                        session.settings.aiKey = aiKey.trim()
                        session.settings.aiModel = aiModel.trim()
                        session.settings.aiPrompt = aiPrompt.trim()
                        session.settings.aiTemp = (aiTemp.toFloatOrNull() ?: 0.3f).coerceIn(0f, 2f)
                        session.settings.aiIncludeComments = aiIncludeComments
                        msg = "✓ AI 设置已保存"
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("保存 AI 设置") }
            }
            SwitchRow("仅看收藏的总结", "历史保留每次总结结果，不再覆盖同帖旧记录", checked = favoriteOnly, onCheckedChange = { favoriteOnly = it })
            if (summaryHistory.isNotEmpty()) {
                GroupLabel("总结历史")
                GroupCard {
                    summaryHistory.take(historyLimit).forEachIndexed { index, record ->
                        ListItem(
                            headlineContent = { Text(record.title.ifBlank { "帖子 #${record.topicId}" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(record.summary.replace("\n", " "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                IconButton(onClick = {
                                    session.removeAiSummary(record)
                                    historyTick++
                                }) { Icon(Icons.Filled.Delete, "删除") }
                            },
                            modifier = Modifier.clickable { selectedSummary = record },
                        )
                        if (index < summaryHistory.take(historyLimit).lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                    }
                }
                if (summaryHistory.size > historyLimit) TextButton(onClick = { historyLimit += 30 }) { Text("显示更多总结") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** 备份设置子页面：本地 JSON 导入 / 导出 + WebDAV 云备份 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSettingsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }

    // WebDAV 配置（持久化在 AppSettings）
    var davUrl by remember { mutableStateOf(session.settings.webdavUrl) }
    var davUser by remember { mutableStateOf(session.settings.webdavUser) }
    var davPass by remember { mutableStateOf(session.settings.webdavPass) }
    var davDir by remember { mutableStateOf(session.settings.webdavDir.ifBlank { "/linuxsb/" }) }
    var davBusy by remember { mutableStateOf(false) }
    var davPassVisible by remember { mutableStateOf(false) }
    var backupItems by remember { mutableStateOf(session.settings.backupItems) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(session.settings.exportJson(backupItems).toByteArray())
                }
                msg = "✓ 已导出为 JSON"
            }.onFailure { msg = "导出失败：${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                if (text.isBlank()) msg = "文件为空"
                else {
                    val err = session.settings.importJson(text)
                    if (err == null) {
                        session.reloadPrefs()   // 导入后立即生效，无需重启
                        msg = "✓ 导入成功，已立即生效"
                    } else msg = "✗ $err"
                }
            }.onFailure { msg = "导入失败：${it.message}" }
        }
    }

    fun davFileUrl(): String? {
        if (davUrl.isBlank()) { msg = "✗ 请先填写 WebDAV 服务器地址"; return null }
        val base = davUrl.trim().trimEnd('/')
        val dir = davDir.trim().trim('/')
        return if (dir.isEmpty()) "$base/lsb_settings.json" else "$base/$dir/lsb_settings.json"
    }

    fun davBackup() {
        val url = davFileUrl() ?: return
        davBusy = true
        scope.launch {
            try {
                WebDav.put(url, davUser, davPass, session.settings.exportJson(backupItems))
                msg = "✓ 已备份到 WebDAV"
            } catch (e: Exception) {
                msg = "✗ WebDAV 备份失败：${e.message}"
            } finally { davBusy = false }
        }
    }

    fun davRestore() {
        val url = davFileUrl() ?: return
        davBusy = true
        scope.launch {
            try {
                val body = WebDav.get(url, davUser, davPass)
                val err = session.settings.importJson(body)
                if (err == null) {
                    session.reloadPrefs()   // 恢复后立即生效，无需重启
                    msg = "✓ 已从 WebDAV 恢复，已立即生效"
                } else msg = "✗ $err"
            } catch (e: Exception) {
                msg = "✗ WebDAV 恢复失败：${e.message}"
            } finally { davBusy = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {


            GroupCard {
                Text(
                    "选择要写入 JSON 和 WebDAV 的内容。导入时只恢复文件中存在的项目，未备份的本地数据不受影响。自定义字体文件本体不随备份迁移。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                val backupOptions = listOf(
                    "settings" to "应用设置",
                    "history" to "浏览历史与阅读位置",
                    "favorites" to "帖子与评论收藏",
                    "ai" to "AI 配置、总结历史与总结收藏",
                    "drafts" to "发帖草稿",
                    "usage" to "使用统计与 AI 分析",
                    "cards" to "发卡兑换记录",
                )
                backupOptions.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val next = if (key in backupItems) backupItems - key else backupItems + key
                            if (next.isNotEmpty()) { backupItems = next; session.settings.backupItems = next }
                        }.padding(horizontal = 10.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = key in backupItems,
                            onCheckedChange = { checked ->
                                val next = if (checked) backupItems + key else backupItems - key
                                if (next.isNotEmpty()) { backupItems = next; session.settings.backupItems = next }
                            },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            // 导出文件名带当前日期时间（3.3）
                            val now = java.text.SimpleDateFormat(
                                "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
                            ).format(java.util.Date())
                            exportJsonLauncher.launch("lsb_settings_$now.json")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("导出设置") }
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("导入设置") }
                }
            }

            // WebDAV 云备份
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(
                        "WebDAV 云备份",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "把上方已勾选的同一批内容备份到坚果云 / Nextcloud 等 WebDAV 网盘。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = davUrl,
                        onValueChange = {
                            davUrl = it
                            session.settings.webdavUrl = it
                        },
                        label = { Text("服务器地址") },
                        placeholder = { Text("https://dav.example.com/dav") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = davUser,
                        onValueChange = {
                            davUser = it
                            session.settings.webdavUser = it
                        },
                        label = { Text("账号") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = davPass,
                        onValueChange = {
                            davPass = it
                            session.settings.webdavPass = it
                        },
                        label = { Text("密码 / 应用密码") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (davPassVisible) androidx.compose.ui.text.input.VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { davPassVisible = !davPassVisible }) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    if (davPassVisible) "隐藏密码" else "显示密码",
                                    Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = davDir,
                        onValueChange = {
                            davDir = it
                            session.settings.webdavDir = it
                        },
                        label = { Text("备份目录") },
                        placeholder = { Text("/linuxsb/") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { davBackup() },
                            enabled = !davBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (davBusy) {
                                CircularProgressIndicator(
                                    Modifier.size(14.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("备份到 WebDAV")
                        }
                        OutlinedButton(
                            onClick = { davRestore() },
                            enabled = !davBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("从 WebDAV 恢复") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** WebDAV 最小客户端：PUT 上传 / GET 下载（Basic Auth） */
private object WebDav {
    private val client = sb.linux.client.data.AppNetwork.clientBuilder().build()

    private fun auth(user: String, pass: String): String =
        okhttp3.Credentials.basic(user, pass)

    suspend fun put(url: String, user: String, pass: String, body: String): Unit =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("Authorization", auth(user, pass))
                .put(
                    okhttp3.RequestBody.create(
                        "application/json; charset=utf-8".toMediaTypeOrNull(), body
                    )
                )
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            }
        }

    suspend fun get(url: String, user: String, pass: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("Authorization", auth(user, pass))
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("响应为空")
            }
        }
}

/** 屏蔽词管理（源站首页关键词过滤，与网页端设置一致） */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockWordsScreen(session: Session, nav: NavHostController) {
    var input by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var presets by remember { mutableStateOf<List<String>>(emptyList()) }
    var custom by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; msg = null
            try {
                val f = session.client.getKeywordFilter()
                presets = f.presets; custom = f.custom; users = f.users
                session.refreshKeywordFilter()
            } catch (e: Exception) {
                msg = e.message ?: "加载失败"
            } finally { loading = false }
        }
    }

    fun save(p: List<String>, c: List<String>, u: List<String>) {
        busy = true; msg = null
        scope.launch {
            try {
                val f = session.client.saveKeywordFilter(
                    sb.linux.client.data.LsbClient.KeywordFilter(p, c, u)
                )
                presets = f.presets; custom = f.custom; users = f.users
                session.refreshKeywordFilter()
                session.showToast("已同步到源站")
            } catch (e: Exception) {
                msg = e.message ?: "保存失败"
            } finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { if (session.loginState.loggedIn) load() else loading = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("屏蔽词 · 源站同步") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        if (!session.loginState.loggedIn) {
            Box(Modifier.padding(pad)) {
                LoginRequiredBox(
                    "屏蔽词与源站账号绑定",
                    "登录后可管理自定义屏蔽词、屏蔽用户，设置与网页端保持一致",
                ) { nav.navigate("login") }
            }
            return@Scaffold
        }
        if (loading) {
            Box(Modifier.padding(pad)) { LoadingBox() }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "与源站「首页关键词过滤」一致：标题含屏蔽词的帖子及被屏蔽用户的帖子将不再显示，网页端与本应用互通。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 预设屏蔽词（源站下发）
            if (presets.isNotEmpty()) {
                Text("预设屏蔽规则", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        InputChip(
                            selected = false,
                            onClick = { save(presets - p, custom, users) },
                            enabled = !busy,
                            label = { Text(p) },
                            trailingIcon = { Icon(Icons.Filled.Close, "移除", Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // 自定义屏蔽词
            Text("自定义屏蔽词", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("新屏蔽词") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        val w = input.trim()
                        if (w.isNotBlank() && w !in custom) save(presets, custom + w, users)
                        input = ""
                    },
                    enabled = !busy
                ) { Icon(Icons.Filled.Add, "添加") }
            }
            if (custom.isEmpty()) {
                Text("暂无自定义屏蔽词", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    custom.forEach { w ->
                        InputChip(
                            selected = false,
                            onClick = { save(presets, custom - w, users) },
                            enabled = !busy,
                            label = { Text(w) },
                            trailingIcon = { Icon(Icons.Filled.Close, "删除", Modifier.size(14.dp)) }
                        )
                    }
                }
                TextButton(onClick = { save(presets, emptyList(), users) }, enabled = !busy) { Text("清空屏蔽词") }
            }

            // 屏蔽用户
            Text("屏蔽用户", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("用户名") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        val u = user.trim()
                        if (u.isNotBlank() && u !in users) save(presets, custom, users + u)
                        user = ""
                    },
                    enabled = !busy
                ) { Icon(Icons.Filled.Add, "添加") }
            }
            if (users.isEmpty()) {
                Text("暂无屏蔽用户", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    users.forEach { u ->
                        InputChip(
                            selected = false,
                            onClick = { save(presets, custom, users - u) },
                            enabled = !busy,
                            label = { Text(u) },
                            trailingIcon = { Icon(Icons.Filled.Close, "删除", Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }
    }
}

/** 当前主题模式下是否处于暗色 */
@Composable
private fun isPreviewDark(mode: ThemeModePref): Boolean = when (mode) {
    ThemeModePref.SYSTEM -> isSystemInDarkTheme()
    ThemeModePref.LIGHT -> false
    ThemeModePref.DARK -> true
}

/** 由样式生成一套迷你预览方案（只用于预览，不落盘） */
@Composable
private fun previewScheme(seed: Color, style: PaletteStyle, contrast: Double, dark: Boolean): ColorScheme =
    rememberDynamicColorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = false,
        style = style,
        contrastLevel = contrast,
    )

/** 单个调色风格缩略色板项：用该风格实际生成的 primary/secondary/tertiary 色块预览 */
@Composable
private fun PaletteStyleSwatch(
    seed: Color,
    style: PaletteStyle,
    label: String,
    contrast: Double,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = previewScheme(seed, style, contrast, dark)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
    ) {
        // 三色调色块预览（上 primary / 中 secondary / 下 tertiary）
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.primary))
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.secondary))
            Box(Modifier.fillMaxWidth().height(22.dp).background(scheme.tertiary))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
}

/** 预览卡中的可自定义元素角色 */
private enum class PreviewRole(val label: String) {
    PRIMARY("主要色"), SECONDARY("次要色"), TERTIARY("强调色")
}

private fun previewRoleColor(scheme: ColorScheme, role: PreviewRole): Color = when (role) {
    PreviewRole.PRIMARY -> scheme.primary
    PreviewRole.SECONDARY -> scheme.secondary
    PreviewRole.TERTIARY -> scheme.tertiary
}

/** 帖子卡片可自定义元素（key 对应 CardColorOverrides；标题色不可改，仅保留源站彩色标题） */
private data class CardElement(val key: String, val label: String)

private val CardElements = listOf(
    CardElement(CardColorOverrides.BACKGROUND, "卡片背景"),
    CardElement(CardColorOverrides.TAG, "帖子标签"),
    CardElement(CardColorOverrides.TIME, "时间"),
    CardElement(CardColorOverrides.USER, "用户名"),
    CardElement(CardColorOverrides.COMMENTS, "评论数"),
    CardElement(CardColorOverrides.FORUM, "所属板块"),
)

/** 帖子卡片元素当前颜色：自定义覆盖优先，否则跟随主题 */
private fun cardElementColor(session: Session, scheme: ColorScheme, key: String): Color =
    session.cardColors[key] ?: when (key) {
        CardColorOverrides.BACKGROUND -> scheme.surfaceContainerLow
        CardColorOverrides.TAG -> scheme.secondaryContainer
        CardColorOverrides.TIME -> scheme.onSurfaceVariant
        CardColorOverrides.USER -> scheme.primary
        CardColorOverrides.COMMENTS -> scheme.onSurfaceVariant
        CardColorOverrides.FORUM -> scheme.secondaryContainer
        else -> scheme.onSurface
    }

/**
 * 实时预览（吸顶）：直接复用首页 TopicCardView 渲染样例卡片，与主页帖子完全一致的展示样式，
 * 点按对应元素（含卡片空白处 = 背景）打开自定义取色板改色；标题不可改色（3.4 / 3.6 / 3.7）。
 */
@Composable
private fun PostCardPreview(
    scheme: ColorScheme,
    onPick: (String) -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            // 顶部标题行（唯一的改色提示，避免重复 3.7）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "实时预览 · 点按元素可改色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            TopicCardView(
                card = sb.linux.client.data.TopicCard(
                    topicId = 0,
                    title = "预览帖子标题：与首页帖子完全一致的样式",
                    authorId = 1,
                    authorName = "用户名",
                    forumId = 1,
                    forumName = "闲聊灌水",
                    replies = 34,
                    lastReplier = "",
                    timeText = "3 小时前",
                    hot = true,
                    featured = true,
                ),
                onClick = {},
                onForumClick = {},
                onElementClick = onPick,
            )
        }
    }
}

/** 元素选择器（实时预览下半部分）：默认折叠，点击标题行展开（3.5） */
@Composable
private fun ElementSelectorList(
    session: Session,
    scheme: ColorScheme,
    onClickRole: (PreviewRole) -> Unit,
    onClickElement: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 折叠/展开标题行（默认折叠）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "配色角色与卡片元素",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = scheme.onSurfaceVariant
                )
            }
            if (!expanded) {
                Text(
                    "展开可按列表方式修改配色角色与卡片元素颜色",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            } else {
                Text(
                    "配色角色",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
                PreviewRole.entries.forEach { role ->
                    val color = previewRoleColor(scheme, role)
                    val overridden = when (role) {
                        PreviewRole.PRIMARY -> session.themePrimary != null
                        PreviewRole.SECONDARY -> session.themeSecondary != null
                        PreviewRole.TERTIARY -> session.themeTertiary != null
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClickRole(role) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color))
                        Spacer(Modifier.width(10.dp))
                        Text(role.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overridden) "已自定义" else "跟随主题",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
                    }
                }
                Text(
                    "帖子卡片元素",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp)
                )
                CardElements.forEach { el ->
                    val color = cardElementColor(session, scheme, el.key)
                    val overridden = session.cardColors.containsKey(el.key)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClickElement(el.key) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color))
                        Spacer(Modifier.width(10.dp))
                        Text(el.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overridden) "已自定义" else "跟随主题",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** 顶部日/夜/系统快捷切换：紧凑横向按钮，图标与文字并排，避免文字竖排导致容器过高 */
@Composable
private fun DayNightToggle(mode: ThemeModePref, onSelect: (ThemeModePref) -> Unit) {
    val items = listOf(
        Triple(ThemeModePref.SYSTEM, Icons.Filled.BrightnessAuto, "跟随系统"),
        Triple(ThemeModePref.LIGHT, Icons.Filled.LightMode, "浅色"),
        Triple(ThemeModePref.DARK, Icons.Filled.DarkMode, "深色"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (m, icon, label) ->
            val selected = mode == m
            val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(m) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, label, modifier = Modifier.size(18.dp), tint = fg)
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/** 主题色预设方形色板（ColorBlendr 风格，适应性排布） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    custom: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                .background(color)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            if (custom) {
                Icon(
                    Icons.Filled.Add,
                    "自定义取色",
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Black.copy(alpha = 0.55f)
                )
            } else if (onLongClick != null) {
                Text(
                    "长按移除",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.35f),
                    maxLines = 1,
                    fontSize = 8.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 读取 SAF 文档的显示名（用于自定义字体文件名展示） */
private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        } ?: uri.lastPathSegment
    }.getOrNull() ?: "自定义字体"

/** 字体选项块（24）：以「字A」预览该字体实际观感 */
@Composable
private fun FontSwatch(
    label: String,
    fontFamily: FontFamily?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    custom: Boolean = false,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            if (custom) {
                Icon(
                    Icons.Filled.Add,
                    "导入自定义字体",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "字A",
                    fontFamily = fontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 外观设置（独立分类页）：快捷切换 / 实时预览(帖子卡片元素级改色) / 主题色 / 调色风格 / 取色 / 对比度。
 * 基于 ColorBlendr 同款 HCT 主题引擎：主题色作为「种子色」，配合调色风格与对比度
 * 生成完整 MD3 配色，预览卡点按元素可独立覆盖对应颜色，只作用于本应用。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThemeSettingsScreen(session: Session, nav: NavHostController) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var pickingRole by remember { mutableStateOf<PreviewRole?>(null) }
    var pickingElement by remember { mutableStateOf<String?>(null) }
    val dark = isPreviewDark(session.themeMode)
    // 当前生效的整体配色方案（直接取自 LsbTheme 应用的主题，已含元素级覆盖）
    val currentScheme = MaterialTheme.colorScheme

    // ---------- 自定义字体（24）：导入器与预览字体 ----------
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customFontFamily = remember(session.customFontName) {
        session.customFontFile.takeIf { it.exists() }?.let { f ->
            runCatching { FontFamily(Font(f)) }.getOrNull()
        }
    }
    var backgroundImportBusy by remember { mutableStateOf(false) }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            backgroundImportBusy = true
            try { session.importBackgroundImage(uri); session.showToast("背景图片已保存到应用内") }
            catch (e: Exception) { session.showToast(e.message ?: "导入背景失败") }
            finally { backgroundImportBusy = false }
        }
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                if (session.saveCustomFont(uri, queryDisplayName(context, uri))) {
                    session.showToast("已应用自定义字体")
                } else {
                    session.showToast("导入失败：不是有效的字体文件")
                }
            }
        }
    }

    // 自定义主题色（种子色）取色对话框
    if (showColorPicker) {
        ColorPickerDialog(
            title = "自定义主题色",
            initial = colorSeed(
                if (isCustomColorKey(session.themeColorKey)) session.themeColorKey else "default"
            ),
            presets = session.themeCustomColors,
            onDismiss = { showColorPicker = false },
            onSavePreset = { c -> session.addThemeCustomColor(colorToKey(c)) },
            onRemovePreset = { k ->
                session.removeThemeCustomColor(k)
                session.showToast("已移除该预设颜色")
            },
            onPick = { c ->
                session.saveTheme(session.themeMode, false, colorToKey(c))
                session.addThemeCustomColor(colorToKey(c))
                showColorPicker = false
            }
        )
    }
    if (showBackgroundPicker) {
        ColorPickerDialog(
            title = "自定义页面背景",
            initial = session.themeBackground ?: currentScheme.background,
            presets = session.themeCustomColors,
            onDismiss = { showBackgroundPicker = false },
            onReset = {
                session.saveThemeBackground(null)
                showBackgroundPicker = false
            },
            onPick = { color ->
                session.saveThemeBackground(color)
                showBackgroundPicker = false
            },
        )
    }
    // 配色角色覆盖取色对话框
    pickingRole?.let { role ->
        ColorPickerDialog(
            title = "自定义${role.label}",
            initial = previewRoleColor(currentScheme, role),
            presets = session.themeCustomColors,
            onDismiss = { pickingRole = null },
            onSavePreset = { c -> session.addThemeCustomColor(colorToKey(c)) },
            onRemovePreset = { k ->
                session.removeThemeCustomColor(k)
                session.showToast("已移除该预设颜色")
            },
            onReset = {
                when (role) {
                    PreviewRole.PRIMARY -> session.saveThemePrimary(null)
                    PreviewRole.SECONDARY -> session.saveThemeSecondary(null)
                    PreviewRole.TERTIARY -> session.saveThemeTertiary(null)
                }
                pickingRole = null
            },
            onPick = { c ->
                when (role) {
                    PreviewRole.PRIMARY -> session.saveThemePrimary(c)
                    PreviewRole.SECONDARY -> session.saveThemeSecondary(c)
                    PreviewRole.TERTIARY -> session.saveThemeTertiary(c)
                }
                pickingRole = null
            }
        )
    }
    // 帖子卡片元素覆盖取色对话框（预览卡点按元素触发）
    pickingElement?.let { key ->
        val el = CardElements.firstOrNull { it.key == key }
        if (el != null) {
            ColorPickerDialog(
                title = "自定义${el.label}",
                initial = cardElementColor(session, currentScheme, key),
                presets = session.themeCustomColors,
                onDismiss = { pickingElement = null },
                onSavePreset = { c -> session.addThemeCustomColor(colorToKey(c)) },
                onRemovePreset = { k ->
                    session.removeThemeCustomColor(k)
                    session.showToast("已移除该预设颜色")
                },
                onReset = {
                    session.saveCardColor(key, null)
                    pickingElement = null
                },
                onPick = { c ->
                    session.saveCardColor(key, c)
                    pickingElement = null
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 重置外观设置（含卡片元素色与自定义色预选）
                    ResetAction(title = "重置外观设置") {
                        session.resetThemeSettings()
                        session.showToast("已重置外观设置")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ---------- 快捷切换（日/夜，紧凑横向按钮） ----------
            item { GroupLabel("快捷切换") }
            item {
                GroupCard {
                    DayNightToggle(session.themeMode) { mode -> session.saveTheme(mode, session.dynamicColor) }
                }
            }

            // ---------- 自定义全局背景（20、38） ----------
            item { GroupLabel("页面背景") }
            item {
                GroupCard {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自定义背景色", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (session.themeBackground == null) "当前跟随主题" else "已应用到所有页面底色",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (session.themeBackground != null) {
                            TextButton(onClick = { session.saveThemeBackground(null) }) { Text("恢复") }
                        }
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(session.themeBackground ?: currentScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showBackgroundPicker = true },
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(enabled = !backgroundImportBusy, onClick = { backgroundPicker.launch("image/*") }) {
                            Text(if (backgroundImportBusy) "正在处理图片…" else "选择背景图片")
                        }
                        if (session.backgroundImage.isNotEmpty()) TextButton(onClick = session::removeBackgroundImage) { Text("移除图片") }
                    }
                    if (session.backgroundImage.isNotEmpty()) {
                        SwitchRow("启用背景图片", "本地保存，随设置备份；不上传图床", checked = session.backgroundImageEnabled,
                            onCheckedChange = session::saveBackgroundImageEnabled)
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            Text("背景图片不透明度 ${(session.backgroundImageOpacity * 100).toInt()}%")
                            Slider(value = session.backgroundImageOpacity, onValueChange = session::saveBackgroundImageOpacity, valueRange = 0.05f..0.65f)
                            Text("卡片、顶栏表面不透明度 ${(session.surfaceOpacity * 100).toInt()}%")
                            Slider(value = session.surfaceOpacity, onValueChange = session::saveSurfaceOpacity, valueRange = 0.2f..1f)
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    SwitchRow(
                        "禁止页面染色",
                        "页面底色、普通卡片和弹窗保持中性色；按钮、选中状态、标签和高亮完整保留强调色。手动自定义背景不受影响",
                        checked = session.keepBackgroundColor,
                        onCheckedChange = session::saveKeepBackgroundColor,
                    )
                }
            }

            // ---------- 实时预览（上部吸顶，始终可见；下半部分默认折叠 3.5） ----------
            stickyHeader {
                PostCardPreview(currentScheme) { key -> pickingElement = key }
            }
            item {
                ElementSelectorList(
                    session,
                    currentScheme,
                    onClickRole = { role -> pickingRole = role },
                    onClickElement = { key -> pickingElement = key }
                )
            }

            // ---------- 主题色（单行横向滑动） ----------
            item { GroupLabel("主题色") }
            item {
                GroupCard {
                    Text(
                        "选择起点色由主题引擎生成配色；自定义色可「存为预选」，长按色块可移除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sb.linux.client.ui.ThemeColorPresets) { preset ->
                            val selected = session.themeColorKey == preset.key
                            ColorSwatch(
                                color = preset.lightPrimary,
                                label = preset.label,
                                selected = selected,
                                onClick = {
                                    session.saveTheme(
                                        session.themeMode,
                                        preset.key == "default" && session.dynamicColor,
                                        preset.key
                                    )
                                }
                            )
                        }
                        // 用户保存过的自定义主题色预选
                        items(session.themeCustomColors) { key ->
                            val selected = session.themeColorKey == key
                            ColorSwatch(
                                color = colorSeed(key),
                                label = "#" + key.removePrefix("custom#").take(4),
                                selected = selected,
                                onClick = { session.saveTheme(session.themeMode, false, key) },
                                onLongClick = {
                                    session.removeThemeCustomColor(key)
                                    session.showToast("已移除该自定义主题色")
                                }
                            )
                        }
                        item {
                            val isCustom = isCustomColorKey(session.themeColorKey)
                            ColorSwatch(
                                color = if (isCustom) colorSeed(session.themeColorKey) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                label = "自定义",
                                selected = isCustom,
                                custom = true,
                                onClick = { showColorPicker = true }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---------- 调色风格（单行横向滑动） ----------
            item { GroupLabel("调色风格") }
            item {
                GroupCard {
                    Text(
                        "同一主题色在不同调色风格下的观感差异，点按切换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(PaletteStyles) { style ->
                            PaletteStyleSwatch(
                                seed = colorSeed(session.themeColorKey),
                                style = style,
                                label = paletteStyleLabel(style),
                                contrast = session.themeContrast.toDouble(),
                                dark = dark,
                                selected = session.themeStyle == style.ordinal,
                                onClick = { session.saveThemeStyle(style.ordinal) },
                                modifier = Modifier.width(84.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---------- 取色（3.15：改为与其他设置一致的开关行样式） ----------
            item { GroupLabel("取色") }
            item {
                GroupCard {
                    SwitchRow(
                        "Material 动态取色",
                        "跟随 Android 12+ 系统壁纸配色",
                        checked = session.dynamicColor,
                        onCheckedChange = { on ->
                            // 开启动态取色 → 重置为默认主题色；关闭则保持当前色
                            session.saveTheme(session.themeMode, on, if (on) "default" else session.themeColorKey)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    SwitchRow(
                        "OLED 纯黑",
                        "深色模式下使用纯黑背景",
                        checked = session.oledDark,
                        onCheckedChange = { session.saveOledDark(it) }
                    )
                }
            }

            // ---------- 对比度（滑条 + 三档标签，两端与中间可点选，靠近档位时轻吸附） ----------
            item { GroupLabel("对比度") }
            item {
                GroupCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "对比度等级",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "拖动滑条或点选档位",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val snapPoints = listOf(-1f, 0f, 1f)
                    var dragging by remember { mutableStateOf(false) }
                    var dragValue by remember { mutableStateOf(session.themeContrast) }
                    Slider(
                        value = if (dragging) dragValue else session.themeContrast,
                        onValueChange = { dragging = true; dragValue = it },
                        onValueChangeFinished = {
                            dragging = false
                            // 轻吸附：距离档位 0.25 以内时吸附到档位
                            val snapped = snapPoints
                                .filter { kotlin.math.abs(it - dragValue) <= 0.25f }
                                .minByOrNull { kotlin.math.abs(it - dragValue) }
                                ?: dragValue
                            session.saveThemeContrast(snapped)
                        },
                        valueRange = -1f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    )
                    // 三档标签：点击直达（左中右三个特殊值的表达），当前档位高亮
                    val current = if (dragging) dragValue else session.themeContrast
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("低" to -1f, "标准" to 0f, "高" to 1f).forEach { (label, value) ->
                            val active = kotlin.math.abs(current - value) < 0.01f
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dragging = false; session.saveThemeContrast(value) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "仅对「主题色」派生方案生效，动态取色（跟随系统壁纸）不受此影响",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }

            // ---------- 布局：平板模式（从常规设置迁入） ----------
            item { GroupLabel("布局") }
            item {
                GroupCard {
                    SwitchRow(
                        title = "平板模式",
                        subtitle = "宽屏设备使用左右双栏：导航栏居左，主页/设置占左栏，帖子与详情占右栏。需屏幕宽度 ≥ 600dp 生效",
                        checked = session.tabletMode,
                        onCheckedChange = { session.saveTabletMode(it) },
                    )
                }
            }

            // ---------- 底栏样式：经典通栏 / 液态玻璃悬浮胶囊 ----------
            item { GroupLabel("底栏样式") }
            item {
                GroupCard {
                    Text(
                        "经典为普通通栏底栏；液态玻璃为居中悬浮胶囊，实时折射身后滚动内容（Android 12 以下退化为半透明条）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        SegmentedButton(
                            selected = session.bottomBarStyle == 0,
                            onClick = { session.saveBottomBarStyle(0) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("经典") }
                        SegmentedButton(
                            selected = session.bottomBarStyle == 1,
                            onClick = { session.saveBottomBarStyle(1) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("液态玻璃") }
                    }
                    HorizontalDivider(Modifier.padding(top = 10.dp))
                    Text(
                        "底栏项目与顺序",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
                    )
                    Text(
                        "可选首页、版块、发帖和我的；至少保留两项。箭头用于调整显示顺序。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    )
                    val bottomOptions = listOf("home" to "首页", "topicCollections" to "淘帖", "directMessages" to "私信", "newTopic" to "发帖", "me" to "我的")
                    val selectedBottomItems = session.bottomBarItems
                    (selectedBottomItems.mapNotNull { id -> bottomOptions.firstOrNull { it.first == id } } +
                        bottomOptions.filter { it.first !in selectedBottomItems }).forEach { (id, label) ->
                        val index = selectedBottomItems.indexOf(id)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = index >= 0,
                                enabled = index < 0 || selectedBottomItems.size > 2,
                                onCheckedChange = { checked ->
                                    if (checked) session.saveBottomBarItems(selectedBottomItems + id)
                                    else if (selectedBottomItems.size > 2) session.saveBottomBarItems(selectedBottomItems - id)
                                },
                            )
                            Text(label, Modifier.weight(1f))
                            TextButton(enabled = index > 0, onClick = {
                                val changed = selectedBottomItems.toMutableList()
                                java.util.Collections.swap(changed, index, index - 1)
                                session.saveBottomBarItems(changed)
                            }) { Text("↑") }
                            TextButton(enabled = index >= 0 && index < selectedBottomItems.lastIndex, onClick = {
                                val changed = selectedBottomItems.toMutableList()
                                java.util.Collections.swap(changed, index, index + 1)
                                session.saveBottomBarItems(changed)
                            }) { Text("↓") }
                        }
                    }
                    // 评论底栏（帖子内底部快捷回复栏）：与主导航底栏独立的样式选择
                    Text(
                        "评论底栏",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 2.dp)
                    )
                    Text(
                        "帖子内底部的回复/点赞/收藏快捷栏；液态玻璃为悬浮胶囊，实时折射身后滚动的帖子内容（Android 12 以下退化为半透明条）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        SegmentedButton(
                            selected = session.replyBarStyle == 0,
                            onClick = { session.saveReplyBarStyle(0) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("经典") }
                        SegmentedButton(
                            selected = session.replyBarStyle == 1,
                            onClick = { session.saveReplyBarStyle(1) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("液态玻璃") }
                    }
                }
            }

            // ---------- 字体（24）：全局应用字体，支持导入自定义字体文件 ----------
            item { GroupLabel("字体") }
            item {
                GroupCard {
                    Text(
                        "选择应用内全局字体；「自定义」从文件导入 ttf/otf 字体，导入后长按可移除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "default") {
                            FontSwatch(
                                label = "默认",
                                fontFamily = null,
                                selected = session.fontKey == "default",
                                onClick = { session.saveFont("default") }
                            )
                        }
                        item(key = "custom") {
                            FontSwatch(
                                label = session.customFontName
                                    .removeSuffix(".ttf").removeSuffix(".otf")
                                    .ifBlank { "自定义" },
                                fontFamily = customFontFamily,
                                selected = session.fontKey == "custom",
                                custom = customFontFamily == null,
                                onClick = {
                                    if (customFontFamily == null) {
                                        fontPicker.launch(
                                            arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream")
                                        )
                                    } else {
                                        session.saveFont("custom")
                                    }
                                },
                                onLongClick = if (customFontFamily != null) {
                                    {
                                        session.clearCustomFont()
                                        session.showToast("已移除自定义字体")
                                    }
                                } else null
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("字体大小", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text("${(session.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = session.fontScale,
                        onValueChange = session::saveFontScale,
                        valueRange = 0.85f..1.30f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    )
                    Text("字体粗细", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                        listOf("标准", "较粗", "粗体").forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = session.fontWeightLevel == index,
                                onClick = { session.saveFontWeightLevel(index) },
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                            ) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ==================== 常规设置（3.20） ====================

/** 当前应用版本名（包管理器读取，避免依赖 BuildConfig 配置） */
@Composable
private fun currentVersionName(context: android.content.Context): String =
    remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

/** 共用的检查更新逻辑：返回新版本信息或提示文本 */
@Composable
private fun rememberUpdateChecker(session: Session): Pair<(Boolean) -> Unit, UpdateUiState> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }

    val check: (Boolean) -> Unit = { silent ->
        scope.launch {
            checking = true; msg = null; update = null
            val rel = UpdateChecker.latestRelease()
            checking = false
            if (rel == null) {
                if (!silent) msg = "检查失败：无法获取版本信息，请稍后重试"
            } else {
                session.settings.lastUpdateCheck = System.currentTimeMillis()
                val cur = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: ""
                if (UpdateChecker.isNewer(rel.tag, cur)) update = rel
                else if (!silent) msg = "已是最新版本（v$cur）"
            }
        }
    }
    return check to UpdateUiState(checking, update, msg) { msg = null; update = null }
}

data class UpdateUiState(
    val checking: Boolean,
    val update: UpdateChecker.UpdateInfo?,
    val msg: String?,
    /** 关闭「发现新版本」弹窗（同时清掉提示文本） */
    val dismiss: () -> Unit,
)

/** 常规设置子页面：打开链接方式 + 检查更新时机（3.20） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(session: Session, nav: NavHostController) {
    var resetTick by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val (doCheck, updateState) = rememberUpdateChecker(session)
    // 选项用本地状态驱动 UI（SharedPreferences 属性每次重组都读盘且写入不触发重组）；
    // resetTick 作 key，重置后刷新
    var linkMode by remember(resetTick) { mutableStateOf(session.settings.linkOpenMode) }
    var useBuiltInImageHost by remember(resetTick) { mutableStateOf(session.settings.useBuiltInImageHost) }
    var imageHostUrl by remember(resetTick) { mutableStateOf(session.settings.imageHostUrl) }
    var imageHostToken by remember(resetTick) { mutableStateOf(session.settings.imageHostToken) }
    var imageHostField by remember(resetTick) { mutableStateOf(session.settings.imageHostField) }
    val scope = rememberCoroutineScope()
    var updateMode by remember(resetTick) { mutableStateOf(session.settings.updateCheckMode) }
    // 重置后强制刷新输入框
    var intervalText by remember(resetTick, session.settings.updateCheckIntervalHours) {
        mutableStateOf("${session.settings.updateCheckIntervalHours}")
    }

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("常规设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    ResetAction(
                        title = "重置常规设置",
                    ) {
                        session.settings.resetGeneral()
                        session.saveLinkPreviewEnabled(session.settings.linkPreviewEnabled)
                        resetTick++
                        session.showToast("已重置常规设置")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------- 打开链接方式 ----------
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text("打开外部链接方式", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "帖子内的外部链接与网页的打开方式；站内跳转（楼层、主页）始终在应用内进行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = linkMode == 0,
                            onClick = { linkMode = 0; session.settings.linkOpenMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("应用内打开", maxLines = 1) }
                        SegmentedButton(
                            selected = linkMode == 1,
                            onClick = { linkMode = 1; session.settings.linkOpenMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("外部浏览器打开", maxLines = 1) }
                    }
                }
                SwitchRow(
                    "链接预览卡片",
                    "首次点击显示网站图标、标题和介绍，再点击卡片或打开按钮访问网站",
                    checked = session.linkPreviewEnabled,
                    onCheckedChange = { session.saveLinkPreviewEnabled(it) },
                )
            }

            GroupCard {
                SettingMenuRow("DNS over HTTPS", "默认与自定义服务器 · 名称和备注 · 查询测速", Icons.Filled.Dns) { nav.navigate("dohSettings") }
            }
            GroupCard {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("内置图床", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        if (useBuiltInImageHost)
                            "内置 Pixhost 和 Catbox，免登录、无需 API Key。上传内容将公开托管于所选第三方服务，请勿上传隐私或敏感内容。"
                        else
                            "使用 multipart/form-data 上传到自定义接口，兼容返回 url、data.url、link 或 src 的常见 API。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = useBuiltInImageHost,
                            onClick = {
                                useBuiltInImageHost = true
                                session.settings.useBuiltInImageHost = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f),
                        ) { Text("内置匿名图床", maxLines = 1) }
                        SegmentedButton(
                            selected = !useBuiltInImageHost,
                            onClick = {
                                useBuiltInImageHost = false
                                session.settings.useBuiltInImageHost = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f),
                        ) { Text("自定义接口", maxLines = 1) }
                    }
                    if (useBuiltInImageHost) {
                        var provider by remember { mutableStateOf(session.settings.builtInImageHostProvider) }
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("pixhost" to "Pixhost", "catbox" to "Catbox").forEachIndexed { index, (value, label) ->
                                SegmentedButton(selected = provider == value, onClick = {
                                    provider = value; session.settings.builtInImageHostProvider = value
                                }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
                            }
                        }
                        Text(
                            "Pixhost 单张上限 10 MB，Catbox 上限 20 MB；均支持免登录上传，成功后插入原图链接。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    OutlinedTextField(
                        value = imageHostUrl, onValueChange = { imageHostUrl = it },
                        label = { Text("上传接口地址") }, placeholder = { Text("https://example.com/api/upload") },
                        singleLine = true, enabled = !useBuiltInImageHost, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = imageHostToken, onValueChange = { imageHostToken = it },
                        label = { Text("访问令牌（可选）") }, singleLine = true,
                        enabled = !useBuiltInImageHost,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = imageHostField, onValueChange = { imageHostField = it },
                        label = { Text("文件字段名") }, placeholder = { Text("file") },
                        singleLine = true, enabled = !useBuiltInImageHost, modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        session.settings.imageHostUrl = imageHostUrl.trim()
                        session.settings.imageHostToken = imageHostToken.trim()
                        session.settings.imageHostField = imageHostField.trim().ifBlank { "file" }
                        session.showToast("图床配置已保存")
                    }, enabled = !useBuiltInImageHost, modifier = Modifier.fillMaxWidth()) { Text("保存自定义图床") }
                }
            }

            // ---------- 检查更新 ----------
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text("检查更新时机", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = updateMode == 0,
                            onClick = { updateMode = 0; session.settings.updateCheckMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("每次启动", maxLines = 1) }
                        SegmentedButton(
                            selected = updateMode == 1,
                            onClick = { updateMode = 1; session.settings.updateCheckMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("按间隔", maxLines = 1) }
                        SegmentedButton(
                            selected = updateMode == 2,
                            onClick = { updateMode = 2; session.settings.updateCheckMode = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("从不", maxLines = 1) }
                    }
                    // 按间隔时的间隔小时数输入
                    if (updateMode == 1) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { t ->
                                intervalText = t.filter { it.isDigit() }.take(4)
                                intervalText.toIntOrNull()?.let { session.settings.updateCheckIntervalHours = it }
                            },
                            label = { Text("检查间隔（1 - 720 小时）") },
                            suffix = { Text("小时") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { doCheck(false) },
                        enabled = !updateState.checking,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (updateState.checking) "检查中…" else "立即检查更新") }
                    updateState.msg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // 发现新版本对话框
    updateState.update?.let { rel ->
        AlertDialog(
            onDismissRequest = updateState.dismiss,
            title = { Text("发现新版本") },
            text = {
                Column {
                    Text("${rel.name}（当前 v${currentVersionName(context)}）")
                    if (rel.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            rel.notes.take(300),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openExternal(rel.url); updateState.dismiss() }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = updateState.dismiss) { Text("取消") }
            }
        )
    }
}

// ==================== 已导出的帖子（3.17） ====================

/** 本地已导出为 HTML 的帖子列表（本地文件读取，不重新解析源站） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedTopicsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<java.io.File>?>(null) }
    var deleteTarget by remember { mutableStateOf<java.io.File?>(null) }

    fun refresh() {
        files = java.io.File(context.filesDir, "exports").takeIf { it.isDirectory }
            ?.listFiles { f -> f.isFile && f.extension.equals("html", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    LaunchedEffect(Unit) { refresh() }

    fun openFile(f: java.io.File) {
        // 应用内查看（3.21）：直接路由到本地 HTML 查看页，不再调起系统浏览器
        nav.navigate("exportedHtml?path=${android.net.Uri.encode(f.absolutePath)}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已导出的帖子") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, "刷新") }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                files == null -> LoadingBox()
                files!!.isEmpty() -> EmptyBox("暂无已导出的帖子\n在帖子右上角菜单选择「导出帖子」导出 HTML")
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files!!, key = { it.absolutePath }) { f ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { openFile(f) }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        f.nameWithoutExtension,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${fmtBytes(f.length())} · " +
                                            java.text.SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm",
                                                androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                                            ).format(java.util.Date(f.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { deleteTarget = f }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认
    deleteTarget?.let { f ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除导出文件") },
            text = { Text("确定删除「${f.nameWithoutExtension}」吗？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { f.delete() }
                    deleteTarget = null
                    refresh()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

// ==================== 关于应用（3.18） ====================

/** 关于应用：图标 / 名称 / 版本号 / 简介 / List group（开发人员、项目地址、开源协议、检查更新） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    val (doCheck, updateState) = rememberUpdateChecker(session)

    // 应用图标（包管理器 → Bitmap）
    val appIcon = remember {
        runCatching {
            val d = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = android.graphics.Bitmap.createBitmap(
                d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val c = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            bmp.asImageBitmap()
        }.getOrNull()
    }
    val appName = remember {
        runCatching {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        }.getOrNull() ?: "LinuxSB"
    }
    val versionName = currentVersionName(context)
    val versionCode = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 0
    }

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于应用") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            // 应用图标
            appIcon?.let {
                androidx.compose.foundation.Image(
                    bitmap = it,
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(Modifier.height(16.dp))
            // 应用名称
            Text(appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            // 版本号（圆角矩形）
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "v$versionName ($versionCode)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            // 应用简介（不超过十个字）
            Text(
                "轻量社区客户端",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            // List group
            GroupCard {
                val rowDivider = @Composable {
                    HorizontalDivider(Modifier.padding(start = 66.dp, end = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                SettingMenuRow("开发人员", "Mei-Nagano", Icons.Filled.Person) { openExternal(UpdateChecker.DEVELOPER_URL) }
                rowDivider()
                SettingMenuRow("项目地址", "GitHub · ${UpdateChecker.REPO}", Icons.Filled.Code) { openExternal(UpdateChecker.PROJECT_URL) }
                rowDivider()
                SettingMenuRow("开源协议", "MIT License", Icons.Filled.Gavel) { openExternal(UpdateChecker.LICENSE_URL) }
                rowDivider()
                SettingMenuRow(
                    "检查更新",
                    if (updateState.checking) "正在检查…" else "检查 GitHub 最新版本",
                    Icons.Filled.SystemUpdate,
                ) { doCheck(false) }
                rowDivider()
                SettingMenuRow("意见反馈", "GitHub Issues", Icons.Filled.Feedback) { openExternal("${UpdateChecker.PROJECT_URL}/issues/new") }
            }
            updateState.msg?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 发现新版本对话框
    updateState.update?.let { rel ->
        AlertDialog(
            onDismissRequest = updateState.dismiss,
            title = { Text("发现新版本") },
            text = {
                Column {
                    Text("${rel.name}（当前 v$versionName）")
                    if (rel.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            rel.notes.take(300),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openExternal(rel.url); updateState.dismiss() }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = updateState.dismiss) { Text("取消") }
            }
        )
    }
}
