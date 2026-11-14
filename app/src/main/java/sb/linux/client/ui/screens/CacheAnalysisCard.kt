package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import sb.linux.client.ui.patterns.settings.SettingsCheckRow
import sb.linux.client.ui.patterns.settings.SettingsListRow
import sb.linux.client.ui.patterns.settings.SettingsActionRow
import sb.linux.client.ui.patterns.settings.SettingsCard
import sb.linux.client.ui.patterns.settings.settingsItemShape
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.LsbApp
import java.io.File
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor

internal fun fmtBytes(b: Long): String = when {
    b >= 1024 * 1024 -> "%.1f MB".format(b / 1024f / 1024f)
    b >= 1024 -> "%.1f KB".format(b / 1024f)
    else -> "$b B"
}

private data class CacheUsage(val key: String, val name: String, val bytes: Long)

/** 占用与清理按相同真实类别分组；不按图片用途猜测占比。 */
@Composable
@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal fun CacheAnalysisCard(session: Session, refreshKey: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = LocalAppSpacing.current
    var usage by remember { mutableStateOf<List<CacheUsage>?>(null) }
    var otherUsage by remember { mutableStateOf(emptyList<CacheUsage>()) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var clearing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(refreshKey, revision) {
        val report = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            fun prefsSize(name: String): Long = if (app.getSharedPreferences(name, 0).all.isEmpty()) 0 else
                File(app.filesDir.parentFile, "shared_prefs/$name.xml").takeIf { it.exists() }?.length() ?: 0
            val imageBytes = coil.Coil.imageLoader(app).diskCache?.size ?: 0L
            listOf(
                CacheUsage("avatars", "头像", (app as LsbApp).client.avatarCacheSize()),
                CacheUsage("body_images", "帖子与其他图片", imageBytes),
            ) to listOf(
                CacheUsage("home", "首页与侧边栏", prefsSize("lsb_home_cache") + prefsSize("lsb_sidebar")),
                CacheUsage("topic", "阅读量缓存", prefsSize("lsb_views")),
                CacheUsage("ai_history", "AI 总结数据", prefsSize("lsb_ai_summaries")),
            )
        }
        usage = report.first
        otherUsage = report.second
        selected = if (revision == 0) report.first.filter { it.bytes > 0 }.map { it.key }.toSet()
            else selected.intersect(report.first.filter { it.bytes > 0 }.map { it.key }.toSet())
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.section)) {
        val items = usage
        if (items == null) Text("统计中…", style = MaterialTheme.typography.bodyMedium)
        else {
            val total = items.sumOf { it.bytes }
            val selectedBytes = items.filter { it.key in selected }.sumOf { it.bytes }
            val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
            GroupCard {
                SettingsCard(Modifier.settingsSearchAnchor("空间占用")) {
                    Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("图片缓存", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(fmtBytes(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (total == 0L) Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                            else items.forEachIndexed { index, item ->
                                if (item.bytes > 0) Box(Modifier.weight((item.bytes.toFloat() / total).coerceAtLeast(0.025f))
                                    .fillMaxHeight().clip(CircleShape).background(palette[index]))
                            }
                        }
                    }
                }
                items.forEach { item ->
                    SettingsCheckRow(item.name, checked = item.key in selected,
                        subtitle = if (item.bytes == 0L) "无缓存" else "${fmtBytes(item.bytes)} · ${if (item.bytes * 100 / total == 0L) "<1" else (item.bytes * 100 / total).toString()}%",
                        enabled = item.bytes > 0 && !clearing,
                        onCheckedChange = { selected = if (it) selected + item.key else selected - item.key })
                }
                SettingsActionRow("清理所选图片缓存", { pending = selected }, enabled = selected.isNotEmpty() && !clearing,
                    subtitle = if (clearing) "清理中…" else "已选择 ${fmtBytes(selectedBytes)}")
            }
            GroupCard {
                otherUsage.forEach { item ->
                    SettingsListRow(item.name, if (item.bytes > 0) fmtBytes(item.bytes) else "无缓存",
                        enabled = item.bytes > 0 && !clearing, onClick = { pending = setOf(item.key) }, trailing = {
                            Text("清除", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        })
                }
            }
        }
    }
    pending?.let { keys ->
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = "清理所选数据", message = if ("ai_history" in keys) "删除所有已保存的 AI 总结及收藏？" else "清理所选缓存？账号登录不受影响。",
            onDismiss = { pending = null }, onConfirm = {
                pending = null
                clearing = true
                scope.launch {
                    try {
                        session.dataManagementRepository.clearDataItems(keys)
                        selected = selected - keys
                        revision++
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { session.showToast(error.message ?: "清理失败") }
                    finally { clearing = false }
                }
            }, confirmText = "清理",
        )
    }
}
