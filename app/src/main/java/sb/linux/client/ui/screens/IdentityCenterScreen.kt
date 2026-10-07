@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.community.IdentityCenterViewModel
import sb.linux.client.ui.feature.community.IdentityCenterViewModelFactory
import sb.linux.client.domain.community.IdentityRequirement

@Composable
fun IdentityCenterScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.identityRepository) {
        IdentityCenterViewModelFactory(session.identityRepository)
    }
    val viewModel: IdentityCenterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("认证中心") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") } },
        )
    }) { pad ->
        when {
            state.loading -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { LoadingBox() }
            state.error != null -> Box(Modifier.padding(pad).fillMaxSize()) { ErrorBox(state.error.orEmpty(), viewModel::load) }
            else -> {
                val value = state.data ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("创作者认证", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("完成源站条件后可申请图片与附件上传、举报免押金等创作者权益。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (value.email.isNotBlank()) Text("当前邮箱：${value.email}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (value.benefits.isNotEmpty()) {
                        item { Text("创作者福利", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                        items(value.benefits, key = { it.first }) { (title, description) ->
                            ListItem(
                                headlineContent = { Text(title) },
                                supportingContent = { Text(description) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    }
                    item { IdentityRequirementCard("创作者认证条件", value.creatorRequirements) }
                    item { IdentityRequirementCard("账号核验条件", value.accountRequirements) }
                    item {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("申请状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        value.canApply -> "当前已满足页面条件，可以继续申请"
                                        value.emailVerified -> "邮箱已验证，仍有申请条件尚未满足"
                                        else -> "请先完成邮箱验证及未满足的申请条件"
                                    },
                                    color = if (value.canApply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (value.applicationRecords.isEmpty()) Text("暂无申请记录", style = MaterialTheme.typography.bodySmall)
                                else value.applicationRecords.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { nav.navigate("gachaOperation/identity") },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (value.emailVerified) "继续申请" else "验证邮箱 / 继续申请") }
                    }
                    item {
                        Text(
                            "验证码发送、邮箱核验和最终申请均使用源站实时表单，结果与账号同步。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityRequirementCard(title: String, requirements: List<IdentityRequirement>) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            if (requirements.isEmpty()) Text("暂无条件信息", modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            requirements.forEach { requirement ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(requirement.label, Modifier.weight(1f))
                    Text(
                        if (requirement.met) "✓ 符合" else "× 不符合",
                        color = if (requirement.met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ---------------- 邀请中心 ----------------
