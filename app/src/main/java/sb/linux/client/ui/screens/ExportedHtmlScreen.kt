package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.session.Session

/**
 * 本地导出 HTML 查看页：只读取 filesDir/exports 下的导出文件，JS 关闭、baseUrl 指向导出目录。
 * 与联网 WebScreen 完全分开，避免把离线内容放进可执行脚本的环境。
 */
@Composable
fun ExportedHtmlScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rawPath = entry.arguments?.getString("path").orEmpty()
    var title by remember { mutableStateOf("已导出的帖子") }
    var html by remember(rawPath) { mutableStateOf<String?>(null) }
    var error by remember(rawPath) { mutableStateOf<String?>(null) }
    // 重试计数：文件读取失败后可重新读取（如文件刚被恢复）
    var retryKey by remember(rawPath) { mutableStateOf(0) }

    LaunchedEffect(rawPath, retryKey) {
        if (rawPath.isBlank()) {
            error = "文件路径为空"
            return@LaunchedEffect
        }
        runCatching {
            val f = java.io.File(rawPath)
            if (!f.isFile) throw IllegalStateException("文件不存在或已被删除")
            html = f.readText()
        }.onFailure { error = it.message ?: "读取失败" }
    }

    AppPageScaffold(title = title, onBack = { nav.popBackStack() }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                error != null -> ErrorBox(error!!) { html = null; error = null; retryKey++ }
                html == null -> LoadingBox()
                else -> {
                    // delegated property 不能 smart cast：先取局部非空变量
                    val content = html ?: ""
                    // baseUrl 指向导出目录：导出 HTML 若引用同目录相对资源可正常加载
                    val baseUrl = java.io.File(rawPath).parentFile?.toURI()?.toString()
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", null)
                                webViewClient = object : android.webkit.WebViewClient() {
                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                        val t = view?.title?.takeIf { it.isNotBlank() } ?: return
                                        if (t.startsWith("file:")) title = java.io.File(rawPath).nameWithoutExtension
                                        else title = t
                                    }
                                }
                            }
                        },
                        update = { w ->
                            // 配置变更重建后重新注入内容（html 只读一次，重载保证显示）
                            val u = w.url
                            if (u == null || u.startsWith("about:blank")) {
                                w.loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
