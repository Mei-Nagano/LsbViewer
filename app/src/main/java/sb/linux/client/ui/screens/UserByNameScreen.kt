package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.Session
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox

/**
 * `/user?username=xxx` 形式的 @提及 中转屏：源站这种链接不带 uid，而应用内主页路由
 * （`user/{uid}`）要求数字 id。这里只做用户名 → uid 的解析，随后替换自身跳转到主页，
 * 不复制 [UserScreen] 的任何加载逻辑，也不在返回栈里留下中转记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserByNameScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val username = entry.arguments?.getString("name").orEmpty()
    val tab = entry.arguments?.getString("tab") ?: "topics"
    var error by remember(username) { mutableStateOf<String?>(null) }
    var retryKey by remember(username) { mutableIntStateOf(0) }

    LaunchedEffect(username, retryKey) {
        if (username.isBlank()) {
            error = "链接里没有用户名"
            return@LaunchedEffect
        }
        error = null
        val resolved = runCatching {
            val response = session.client.get("/user?username=${android.net.Uri.encode(username)}")
            // 源站若 302 到 /user/{id}，最终 URL 里就有 uid，无需解析正文
            USER_ID_IN_URL.find(response.url)?.groupValues?.get(1)?.toLongOrNull()
                ?: HtmlParser.parseUserProfile(response.html).userId
        }.getOrElse {
            error = it.message ?: "查找用户失败"
            return@LaunchedEffect
        }
        if (resolved <= 0) {
            error = "未找到用户「$username」"
            return@LaunchedEffect
        }
        // 中转屏本身不该出现在返回栈里：从主页返回应回到原来的帖子
        nav.navigate("user/$resolved?tab=${android.net.Uri.encode(tab)}") {
            popUpTo(entry.destination.id) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(username.ifBlank { "用户主页" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val message = error
            if (message == null) LoadingBox() else ErrorBox(message) { retryKey++ }
        }
    }
}

private val USER_ID_IN_URL = Regex("""/user/(\d+)""")
