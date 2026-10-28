@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.IdentityRequirement
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.IdentityCenterViewModel
import sb.linux.client.ui.feature.community.IdentityCenterViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppSpacing
import sb.linux.client.ui.theme.LocalAppSpacing

/** 认证中心：条件与福利分块展示，申请入口保持 48dp；数据全部来自源站。 */
@Composable
fun IdentityCenterScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.identityRepository) {
        IdentityCenterViewModelFactory(session.identityRepository)
    }
    val viewModel: IdentityCenterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val spacing = LocalAppSpacing.current
    LaunchedEffect(viewModel) { viewModel.load() }

    AppPageScaffold(
        title = "认证中心",
        onBack = { nav.popBackStack() },
        actions = { IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") } },
    ) { pad ->
        when {
            state.loading -> LoadingBox(Modifier.padding(pad))
            state.error != null -> ErrorBox(state.error.orEmpty(), Modifier.padding(pad), onRetry = viewModel::load)
            else -> {
                val data = state.data ?: return@AppPageScaffold
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.pageHorizontal,
                        end = spacing.pageHorizontal,
                        top = spacing.pageVertical,
                        bottom = spacing.largeSection,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
                ) {
                    item { IdentityHeader(data.email, spacing) }
                    if (data.benefits.isNotEmpty()) {
                        item { SectionTitle("创作者福利") }
                        item { BenefitsCard(data.benefits, spacing) }
                    }
                    item { IdentityRequirementCard("创作者认证条件", data.creatorRequirements, spacing) }
                    item { IdentityRequirementCard("账号核验条件", data.accountRequirements, spacing) }
                    item { ApplicationStatusCard(data, spacing) }
                    item {
                        Button(
                            onClick = { nav.navigate("gachaOperation/identity") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.large,
                        ) { Text(if (data.emailVerified) "继续申请" else "验证邮箱 / 继续申请") }
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
private fun IdentityHeader(email: String, spacing: AppSpacing) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier.padding(spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.inline),
        ) {
            Text(
                "创作者认证",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "完成源站条件后可申请图片与附件上传、举报免押金等创作者权益。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            if (email.isNotBlank()) {
                Text(
                    "当前邮箱：$email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BenefitsCard(benefits: List<Pair<String, String>>, spacing: AppSpacing) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            benefits.forEachIndexed { index, (title, description) ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap),
                ) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (description.isNotBlank()) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index != benefits.lastIndex) {
                    HorizontalDivider(
                        Modifier.padding(start = spacing.pageHorizontal),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentityRequirementCard(
    title: String,
    requirements: List<IdentityRequirement>,
    spacing: AppSpacing,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = spacing.item)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.inline),
            )
            if (requirements.isEmpty()) {
                Text(
                    "暂无条件信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
                )
            }
            requirements.forEach { requirement ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(requirement.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (requirement.met) "符合" else "不符合",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (requirement.met) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationStatusCard(
    data: sb.linux.client.domain.community.IdentityCenterData,
    spacing: AppSpacing,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.padding(spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.item),
        ) {
            Text("申请状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    data.canApply -> "当前已满足页面条件，可以继续申请"
                    data.emailVerified -> "邮箱已验证，仍有申请条件尚未满足"
                    else -> "请先完成邮箱验证及未满足的申请条件"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (data.canApply) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (data.applicationRecords.isEmpty()) {
                Text(
                    "暂无申请记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                data.applicationRecords.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
