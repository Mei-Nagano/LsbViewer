package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.LoginRequiredBox
import sb.linux.client.ui.feature.settings.KeywordFilterViewModel
import sb.linux.client.ui.feature.settings.KeywordFilterViewModelFactory
import sb.linux.client.ui.session.Session

/** Source forum home keyword, user, and forum blocking preferences. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockWordsScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.keywordFilterRepository) {
        KeywordFilterViewModelFactory(session.keywordFilterRepository)
    }
    val viewModel: KeywordFilterViewModel = viewModel(factory = factory, key = "keyword-filter")
    val state = viewModel.state
    val loggedIn = session.account.loginState.loggedIn

    LaunchedEffect(loggedIn) {
        if (loggedIn) viewModel.load()
    }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            session.showToast(it)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("屏蔽词 · 源站同步") },
                navigationIcon = {
                    IconButton(onClick = nav::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            !loggedIn -> Box(Modifier.padding(paddingValues)) {
                LoginRequiredBox(
                    "屏蔽词与源站账号绑定",
                    "登录后可管理自定义屏蔽词、屏蔽用户，设置与网页端保持一致",
                ) { nav.navigate("login") }
            }
            state.loading -> Box(Modifier.padding(paddingValues)) { LoadingBox() }
            else -> Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { ErrorBox(it, onRetry = viewModel::load) }
                Text(
                    "与源站「首页关键词过滤」一致：标题、用户名和首页版块规则只影响帖子列表，不影响搜索和其他页面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.snapshot.policy.availablePresets.isNotEmpty()) {
                    Text("常用关键词", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.snapshot.policy.availablePresets.forEach { preset ->
                            val selected = KeywordFilterRules.normalize(preset) in state.presets
                            InputChip(
                                selected = selected,
                                onClick = { viewModel.togglePreset(preset) },
                                enabled = !state.busy,
                                label = { Text(preset) },
                            )
                        }
                    }
                }

                EditableKeywordSection(
                    title = "自定义屏蔽词",
                    emptyMessage = "暂无自定义屏蔽词",
                    inputLabel = "新屏蔽词",
                    input = state.input,
                    onInputChange = viewModel::setInput,
                    values = state.custom,
                    onAdd = viewModel::addCustomWords,
                    onRemove = viewModel::removeCustomWord,
                    enabled = !state.busy,
                    supportingText = "最多 ${KeywordFilterRules.MAX_CUSTOM_WORDS} 个，每项 ${KeywordFilterRules.MAX_VALUE_LENGTH} 字符；保存时按源站规则清洗。",
                )

                ForumKeywordSection(
                    options = state.snapshot.policy.forums.map { it.id to it.name },
                    selected = state.selectedForums,
                    enabled = !state.busy,
                    blockingEnabled = state.snapshot.policy.forumBlockingEnabled,
                    warning = state.snapshot.policy.warning,
                    onSelectionChange = viewModel::selectForums,
                )

                EditableKeywordSection(
                    title = "屏蔽用户",
                    emptyMessage = "暂无屏蔽用户",
                    inputLabel = "用户名",
                    input = state.userInput,
                    onInputChange = viewModel::setUserInput,
                    values = state.users,
                    onAdd = viewModel::addUsers,
                    onRemove = viewModel::removeUser,
                    enabled = !state.busy,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::clearPersonalRules, enabled = !state.busy) {
                        Text("清空个人屏蔽")
                    }
                    Button(onClick = viewModel::save, enabled = !state.busy) {
                        Text(if (state.busy) "保存中…" else "保存")
                    }
                }
            }
        }
    }
}
