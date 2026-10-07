package sb.linux.client.ui.screens
import sb.linux.client.common.network.Endpoints

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.ui.*

/** 应用内 WebView 页面：常规设置选择「应用内打开」时，站内未映射页面与外部链接在此展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rawUrl = entry.arguments?.getString("url").orEmpty()
    val url = sb.linux.client.common.network.Endpoints.abs(rawUrl)
    var pageTitle by remember { mutableStateOf(url) }
    // WebView 内页面可能发生跳转，记录当前实际地址供「在浏览器打开」使用
    var currentUrl by remember { mutableStateOf(url) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        pageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 在浏览器打开：调起系统浏览器查看当前页面
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(currentUrl)
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Filled.OpenInNew, "在浏览器打开")
                    }
                }
            )
        }
    ) { pad ->
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // 注入已保存的登录会话，私信等需要登录的页面在应用内即可直接看到（避免跳登录拦截）
                    session.accountAuthenticationRepository.exportWebCookies()
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun doUpdateVisitedHistory(
                            view: android.webkit.WebView?,
                            u: String?,
                            isReload: Boolean,
                        ) {
                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: u ?: url
                            currentUrl = u ?: url
                        }
                    }
                    WebViewDoh.load(this, url)
                }
            },
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        )
    }
}

// ---------------- 已导出帖子的应用内查看 ----------------

/** 本地导出 HTML 查看页：读取 filesDir/exports 下的导出文件并在 WebView 中离线展示 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
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

// ---------------- 每日签到 ----------------

