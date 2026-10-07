package sb.linux.client.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import java.io.File

internal fun fmtBytes(b: Long): String = when {
    b >= 1024 * 1024 -> "%.1f MB".format(b / 1024f / 1024f)
    b >= 1024 -> "%.1f KB".format(b / 1024f)
    else -> "$b B"
}

/** 本地缓存分析：统计各项缓存占用并用饼图展示（3.12） */
@Composable
internal fun CacheAnalysisCard(session: Session, refreshKey: Int = 0) {
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
