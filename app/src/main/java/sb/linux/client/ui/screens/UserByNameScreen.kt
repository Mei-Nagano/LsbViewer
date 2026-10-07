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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.feature.account.UserLookupViewModel
import sb.linux.client.ui.feature.account.UserLookupViewModelFactory

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
    val factory = remember(session.userLookupRepository, username) {
        UserLookupViewModelFactory(session.userLookupRepository, username)
    }
    val viewModel: UserLookupViewModel = viewModel(factory = factory, key = "user-by-name-$username")
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) { viewModel.resolve() }
    LaunchedEffect(state.userId) {
        val resolved = state.userId ?: return@LaunchedEffect
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
            val message = state.error
            if (state.loading) LoadingBox() else if (message != null) ErrorBox(message) { viewModel.resolve() }
        }
    }
}
