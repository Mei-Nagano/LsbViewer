package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LsbException
import sb.linux.client.data.Session

private data class ForumOption(val id: String, val name: String)

/** 从 /topic_edit 页面解析可选版块 */
private fun parseForums(html: String): List<ForumOption> {
    val d = Jsoup.parse(html, "https://linux.sb")
    return d.select("select[name=forum_id] option").map { ForumOption(it.attr("value"), it.text()) }
}

private fun parseEditValues(html: String): Triple<String, String, String> {
    val d = Jsoup.parse(html, "https://linux.sb")
    val form = d.selectFirst("form:has(input[name=title])")
    return Triple(
        form?.selectFirst("input[name=title]")?.attr("value") ?: "",
        form?.selectFirst("textarea[name=body]")?.text() ?: "",
        form?.selectFirst("input[name=id]")?.attr("value") ?: "0"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L) {
    var forums by remember { mutableStateOf<List<ForumOption>>(emptyList()) }
    var forumId by remember { mutableStateOf<String?>(null) }
    var forumExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var internalId by remember { mutableStateOf("0") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(editId) {
        try {
            if (editId > 0) {
                val resp = session.client.get("/topic_edit?id=$editId")
                forums = parseForums(resp.html)
                val (t, b, id) = parseEditValues(resp.html)
                title = t; body = b; internalId = id
                forumId = forums.firstOrNull()?.id
            } else {
                val resp = session.client.get("/topic_edit")
                forums = parseForums(resp.html)
                forumId = forums.firstOrNull()?.id
                internalId = "0"
            }
        } catch (e: Exception) {
            error = e.message
        } finally { loading = false }
    }

    fun insert(before: String, after: String = before, placeholder: String = "") {
        body = buildString {
            append(body)
            if (body.isNotEmpty() && !body.endsWith("\n")) append("\n")
            append(before)
            if (placeholder.isNotBlank()) append(placeholder)
            append(after)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId > 0) "编辑主题" else "发布新主题") },
                navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isBlank() || body.isBlank()) {
                                error = "标题和内容不能为空"; return@TextButton
                            }
                            val fid = forumId ?: run { error = "请选择版块"; return@TextButton }
                            busy = true; error = null
                            scope.launch {
                                try {
                                    if (!session.loginState.loggedIn) throw LsbException("请先登录")
                                    val csrf = session.client.csrf()
                                    val resp = session.client.postForm(
                                        "/topic_edit", mapOf(
                                            "_csrf" to csrf,
                                            "id" to internalId,
                                            "forum_id" to fid,
                                            "title" to title,
                                            "body" to body,
                                        )
                                    )
                                    if (resp.url.contains("form_error")) {
                                        error = HtmlParser.extractError(resp.html).ifBlank { "发布失败" }
                                    } else {
                                        session.showToast("发布成功")
                                        nav.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "发布失败"
                                } finally { busy = false }
                            }
                        },
                        enabled = !busy
                    ) { Text(if (busy) "发布中…" else "发布") }
                }
            )
        }
    ) { pad ->
        if (loading) {
            sb.linux.client.ui.LoadingBox()
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!session.loginState.loggedIn) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "未登录，发布前请先登录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = { nav.navigate("login") }) { Text("去登录") }
                    }
                }
            }
            error?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // 版块选择
            Box {
                OutlinedButton(
                    onClick = { forumExpanded = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("版块：${forums.firstOrNull { it.id == forumId }?.name ?: "选择"}")
                }
                DropdownMenu(expanded = forumExpanded, onDismissRequest = { forumExpanded = false }) {
                    forums.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.name) },
                            onClick = { forumId = f.id; forumExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("标题") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Markdown 工具栏
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { insert("**", "**", "粗体") }) { Icon(Icons.Filled.FormatBold, "粗体") }
                    IconButton(onClick = { insert("*", "*", "斜体") }) { Icon(Icons.Filled.FormatItalic, "斜体") }
                    IconButton(onClick = { insert("## ", "", "标题") }) { Icon(Icons.Filled.Title, "标题") }
                    IconButton(onClick = { insert("> ", "", "引用") }) { Icon(Icons.Filled.FormatQuote, "引用") }
                    IconButton(onClick = { insert("`", "`", "代码") }) { Icon(Icons.Filled.Code, "代码") }
                    IconButton(onClick = { insert("```\n", "\n```", "代码块") }) { Icon(Icons.Filled.Code, "代码块") }
                    IconButton(onClick = { insert("- ", "", "列表项") }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "列表") }
                    IconButton(onClick = { insert("[", "](https://)", "链接文字") }) { Icon(Icons.Filled.Link, "链接") }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("内容 (Markdown)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )
            Text(
                "支持 Markdown 语法，与源站编辑器一致。@用户名 可提及他人。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
