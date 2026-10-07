package sb.linux.client.ui.navigation
import sb.linux.client.common.network.Endpoints

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.update.UpdateInfo
import sb.linux.client.domain.update.isNewerVersion
import sb.linux.client.ui.feature.update.UpdateViewModel
import sb.linux.client.ui.feature.update.UpdateViewModelFactory
import sb.linux.client.common.ui.SessionUiInvalidation
import sb.linux.client.ui.clearBodyImageLayoutCache
import sb.linux.client.ui.LocalCardColorOverrides
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.LocalShowUid
import sb.linux.client.ui.LocalSmartDecode
import sb.linux.client.ui.LocalTableDisplayMode
import sb.linux.client.ui.VerificationDialog

/** 应用级 Compose 根节点，负责全局状态桥接与链接/更新副作用。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(session: Session) {
    SideEffect {
        session.uiInvalidation = SessionUiInvalidation(::clearBodyImageLayoutCache)
    }
    val nav = rememberNavController()
    val updateFactory = remember(session.updateRepository, session.generalSettingsRepository) {
        UpdateViewModelFactory(session.updateRepository, session.generalSettingsRepository)
    }
    val updateViewModel: UpdateViewModel = viewModel(factory = updateFactory)
    val masterNav = rememberNavController()
    val detailNav = rememberNavController()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeDrawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val twoPane = session.tabletMode && LocalConfiguration.current.screenWidthDp >= 600
    val linkNav = if (twoPane) detailNav else nav

    val verification = session.pendingVerification
    if (verification != null) {
        VerificationDialog(
            url = verification.url,
            initialHtml = verification.html,
            prepareCookies = { session.accountAuthenticationRepository.exportWebCookies() },
            onSucceeded = {
                session.accountAuthenticationRepository.importWebCookies(verification.url)
                session.completeVerification(verification, true)
            },
            onCancel = { session.completeVerification(verification, false) },
        )
    }

    LaunchedEffect(session.toast) {
        session.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            session.toast = null
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val rawRoute = (backStack?.destination?.route ?: "home").substringBefore("?")
    val route = BottomDestination.canonical(rawRoute)
    val bottomRoutes = setOf(
        BottomDestination.HOME,
        BottomDestination.FORUMS,
        BottomDestination.TOPIC_COLLECTIONS,
        BottomDestination.DIRECT_MESSAGES,
        BottomDestination.ME,
    )
    val bottomItems = session.bottomBarItems
    val bottomNavigation = remember(nav, scope, homeDrawerState) {
        BottomNavigationCoordinator(
            nav = nav,
            scope = scope,
            closeDrawer = {
                if (!homeDrawerState.isClosed) homeDrawerState.close()
            },
            onHomeReselect = session::requestHomeRefresh,
        )
    }


    var updateTip by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (!updateViewModel.beginAutomaticCheck(System.currentTimeMillis())) return@LaunchedEffect
        updateViewModel.loadLatest { result ->
            val rel = result.getOrNull() ?: return@loadLatest
            val cur = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: ""
            if (isNewerVersion(rel.tag, cur)) updateTip = rel
        }
    }
    updateTip?.let { rel ->
        val askNever = updateViewModel.isFirstRunCheckMode()
        AlertDialog(
            onDismissRequest = { updateTip = null },
            title = { Text("发现新版本") },
            text = { Text("${rel.name} 已发布，是否前往下载？" + if (!askNever) "\n\n（自动检查当前为「按间隔」。可前往 设置 → 常规设置 → 检查更新时机 调整）" else "") },
            confirmButton = {
                TextButton(onClick = {
                    updateTip = null
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rel.url))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("前往下载") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    if (askNever) {
                        TextButton(onClick = {
                            updateViewModel.disableAutomaticChecks()
                            updateTip = null
                            session.showToast("已设置为不再提示更新")
                        }) { Text("不再提示") }
                    }
                    TextButton(onClick = { updateTip = null }) { Text("取消") }
                }
            },
        )
    }

    CompositionLocalProvider(
        LocalCardColorOverrides provides session.cardColors,
        LocalTableDisplayMode provides session.tableDisplayMode,
        LocalShowUid provides session.showUid,
        LocalSmartDecode provides session.smartDecodeEnabled,
        LocalMasterNav provides (if (twoPane) masterNav else null),
    ) {
        AppLinkProvider(session, linkNav) {
            AppContent(
                session = session,
                nav = nav,
                masterNav = masterNav,
                detailNav = detailNav,
                snackbar = snackbar,
                scope = scope,
                homeDrawerState = homeDrawerState,
                twoPane = twoPane,
                route = route,
                bottomRoutes = bottomRoutes,
                bottomItems = bottomItems,
                onNavigateBottom = bottomNavigation::submit,
            )
        }
    }
}

