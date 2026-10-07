package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.update.UpdateInfo
import sb.linux.client.domain.update.isNewerVersion
import sb.linux.client.ui.feature.update.UpdateViewModel
import sb.linux.client.ui.feature.update.UpdateViewModelFactory

@Composable
internal fun currentVersionName(context: android.content.Context): String =
    remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

/** 共用的检查更新逻辑：返回新版本信息或提示文本 */
@Composable
internal fun rememberUpdateChecker(session: Session): Pair<(Boolean) -> Unit, UpdateUiState> {
    val context = LocalContext.current
    val updateFactory = remember(session.updateRepository, session.generalSettingsRepository) {
        UpdateViewModelFactory(session.updateRepository, session.generalSettingsRepository)
    }
    val updateViewModel: UpdateViewModel = viewModel(factory = updateFactory)
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }

    val check: (Boolean) -> Unit = { silent ->
        checking = true; msg = null; update = null
        updateViewModel.loadLatest { result ->
            checking = false
            val rel = result.getOrNull()
            if (rel == null) {
                if (!silent) msg = "检查失败：无法获取版本信息，请稍后重试"
            } else {
                updateViewModel.markChecked(System.currentTimeMillis())
                val cur = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: ""
                if (isNewerVersion(rel.tag, cur)) update = rel
                else if (!silent) msg = "已是最新版本（v$cur）"
            }
        }
    }
    return check to UpdateUiState(checking, update, msg) { msg = null; update = null }
}

internal data class UpdateUiState(
    val checking: Boolean,
    val update: UpdateInfo?,
    val msg: String?,
    /** 关闭「发现新版本」弹窗（同时清掉提示文本） */
    val dismiss: () -> Unit,
)

/** 应用联网策略：代理与 DoH 统一收纳，代理优先于 WebView DoH 通路。 */
