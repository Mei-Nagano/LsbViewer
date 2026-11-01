package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.settings.KeywordFilterViewModel
import sb.linux.client.ui.feature.settings.KeywordFilterViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 源站帖子列表关键词/用户/板块屏蔽；显式保存，错误与登录门禁不混进表单。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockWordsScreen(session: Session, nav: NavHostController) {
    val spacing = LocalAppSpacing.current
    AppPageScaffold(
        title = "帖子列表过滤 · 源站同步",
        onBack = { nav.popBackStack() },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
        ) {
            BlockWordsContent(session, nav)
        }
    }
}

/** 屏蔽规则内容：并入「阅读与交互」二级页展示，也可由独立路由复用。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BlockWordsContent(session: Session, nav: NavHostController) {
    val factory = remember(session.keywordFilterRepository) {
        KeywordFilterViewModelFactory(session.keywordFilterRepository)
    }
    val viewModel: KeywordFilterViewModel = viewModel(factory = factory, key = "keyword-filter")
    val state = viewModel.state
    val loggedIn = session.account.loginState.loggedIn
    val spacing = LocalAppSpacing.current

    LaunchedEffect(loggedIn) {
        if (loggedIn) viewModel.load()
    }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            session.showToast(it)
            viewModel.consumeToast()
        }
    }

    when {
        !loggedIn -> LoginRequiredBox(
            "屏蔽词与源站账号绑定",
            "登录后可管理自定义屏蔽词、屏蔽用户，设置与网页端保持一致",
            Modifier.fillMaxWidth().height(240.dp),
        ) { nav.navigate("login") }
        state.loading -> LoadingBox(Modifier.fillMaxWidth().height(200.dp))
        state.error != null -> ErrorBox(
            state.error!!,
            Modifier.fillMaxWidth().height(200.dp),
            onRetry = viewModel::load,
        )
        else -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
        ) {
            SettingMenuRow("用户屏蔽名单", "按用户 ID 屏蔽主题、回帖及私信，与列表过滤独立") {
                nav.navigate("userBlock")
            }
            Text(
                "与源站「首页关键词过滤」一致：标题、用户名和首页版块规则只影响帖子列表，不影响搜索和其他页面。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.snapshot.policy.availablePresets.isNotEmpty()) {
                Text("常用关键词", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline)) {
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
                title = "列表过滤用户名",
                emptyMessage = "暂无屏蔽用户",
                inputLabel = "用户名",
                input = state.userInput,
                onInputChange = viewModel::setUserInput,
                values = state.users,
                onAdd = viewModel::addUsers,
                onRemove = viewModel::removeUser,
                enabled = !state.busy,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.item),
            ) {
                OutlinedButton(
                    onClick = viewModel::clearPersonalRules,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("清空个人屏蔽") }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(if (state.busy) "保存中…" else "保存") }
            }
        }
    }
}
