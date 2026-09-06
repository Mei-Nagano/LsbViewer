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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.data.Session
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.LoginRequiredBox

/** 源站首页关键词、用户及版块屏蔽设置。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockWordsScreen(session: Session, nav: NavHostController) {
    var input by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var presetOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var presets by remember { mutableStateOf<List<String>>(emptyList()) }
    var custom by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<String>>(emptyList()) }
    var forumOptions by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var selectedForums by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun bind(snapshot: sb.linux.client.common.filter.KeywordFilterSnapshot) {
        presetOptions = snapshot.policy.availablePresets
        presets = snapshot.settings.presets
        custom = snapshot.settings.custom
        users = snapshot.settings.users
        forumOptions = snapshot.policy.forums.map { it.id to it.name }
        selectedForums = KeywordFilterRules.configuredForumIds(snapshot.settings, snapshot.policy)
    }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                bind(session.fetchKeywordFilter(force = true))
            } catch (cause: Exception) {
                bind(session.keywordFilter)
                error = cause.message ?: "加载失败"
            } finally {
                loading = false
            }
        }
    }

    fun save(
        selectedPresets: List<String> = presets,
        customWords: List<String> = custom,
        blockedUsers: List<String> = users,
        forums: Set<Long> = selectedForums,
    ) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val policy = session.keywordFilter.policy
                val defaults = policy.defaultForumIds
                val value = KeywordFilterSettings(
                    presets = selectedPresets,
                    custom = customWords,
                    users = blockedUsers,
                    forumExcludedIds = defaults.filter { it !in forums },
                    forumExtraIds = forums.filter { it !in defaults },
                )
                val snapshot = session.saveKeywordFilter(value)
                bind(snapshot)
                session.showToast(
                    if (snapshot.pending) "已保存到本地，网络恢复后自动同步" else "已同步到源站",
                )
            } catch (cause: Exception) {
                error = cause.message ?: "保存失败"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.loginState.loggedIn) load() else loading = false
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
            !session.loginState.loggedIn -> Box(Modifier.padding(paddingValues)) {
                LoginRequiredBox(
                    "屏蔽词与源站账号绑定",
                    "登录后可管理自定义屏蔽词、屏蔽用户，设置与网页端保持一致",
                ) { nav.navigate("login") }
            }
            loading -> Box(Modifier.padding(paddingValues)) { LoadingBox() }
            else -> Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let {
                    ErrorBox(it) { load() }
                }
                Text(
                    "与源站「首页关键词过滤」一致：标题、用户名和首页版块规则只影响帖子列表，不影响搜索和其他页面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (presetOptions.isNotEmpty()) {
                    Text("常用关键词", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetOptions.forEach { preset ->
                            val selected = KeywordFilterRules.normalize(preset) in presets
                            InputChip(
                                selected = selected,
                                onClick = {
                                    presets = if (selected) {
                                        presets - KeywordFilterRules.normalize(preset)
                                    } else {
                                        presets + KeywordFilterRules.normalize(preset)
                                    }
                                },
                                enabled = !busy,
                                label = { Text(preset) },
                            )
                        }
                    }
                }

                Text("自定义屏蔽词", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("新屏蔽词") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            val words = KeywordFilterRules.splitInput(input)
                            if (words.isNotEmpty()) {
                                custom = KeywordFilterRules.clean(custom + words, KeywordFilterRules.MAX_CUSTOM_WORDS)
                            }
                            input = ""
                        },
                        enabled = !busy,
                    ) { Icon(Icons.Filled.Add, "添加") }
                }
                if (custom.isEmpty()) {
                    Text("暂无自定义屏蔽词", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        custom.forEach { word ->
                            InputChip(
                                selected = false,
                                onClick = { custom = custom - word },
                                enabled = !busy,
                                label = { Text(word) },
                                trailingIcon = { Icon(Icons.Filled.Close, "删除", Modifier) },
                            )
                        }
                    }
                    Text(
                        "最多 ${KeywordFilterRules.MAX_CUSTOM_WORDS} 个，每项 ${KeywordFilterRules.MAX_VALUE_LENGTH} 字符；保存时按源站规则清洗。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (session.keywordFilter.policy.forumBlockingEnabled && forumOptions.isNotEmpty()) {
                    Text("屏蔽版块", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        forumOptions.forEach { (id, name) ->
                            InputChip(
                                selected = id in selectedForums,
                                onClick = {
                                    selectedForums = if (id in selectedForums) selectedForums - id else selectedForums + id
                                },
                                enabled = !busy,
                                label = { Text(name) },
                            )
                        }
                    }
                    Text("进入具体版块时版块屏蔽不生效。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (session.keywordFilter.policy.warning.isNotBlank()) {
                    Text(session.keywordFilter.policy.warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Text("屏蔽用户", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("用户名") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            val values = KeywordFilterRules.splitInput(user)
                            if (values.isNotEmpty()) users = KeywordFilterRules.clean(users + values, KeywordFilterRules.MAX_USERS)
                            user = ""
                        },
                        enabled = !busy,
                    ) { Icon(Icons.Filled.Add, "添加") }
                }
                if (users.isEmpty()) {
                    Text("暂无屏蔽用户", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        users.forEach { blockedUser ->
                            InputChip(
                                selected = false,
                                onClick = { users = users - blockedUser },
                                enabled = !busy,
                                label = { Text(blockedUser) },
                                trailingIcon = { Icon(Icons.Filled.Close, "删除", Modifier) },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val defaults = session.keywordFilter.policy.defaultForumIds
                            presets = emptyList(); custom = emptyList(); users = emptyList(); selectedForums = defaults
                            save(emptyList(), emptyList(), emptyList(), defaults)
                        },
                        enabled = !busy,
                    ) { Text("清空个人屏蔽") }
                    Button(onClick = { save() }, enabled = !busy) { Text(if (busy) "保存中…" else "保存") }
                }
            }
        }
    }
}
