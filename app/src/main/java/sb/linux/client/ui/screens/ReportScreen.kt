@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import sb.linux.client.ui.feature.community.ReportViewModel
import sb.linux.client.ui.feature.community.ReportViewModelFactory

@Composable
fun ReportScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rtype = entry.arguments?.getString("type") ?: "reply"
    val rid = entry.arguments?.getLong("id") ?: return
    val factory = remember(session.reportRepository, rid, rtype, nav, session) {
        ReportViewModelFactory(session.reportRepository, rid, rtype) {
            session.showToast("举报已提交")
            nav.popBackStack()
        }
    }
    val viewModel: ReportViewModel = viewModel(factory = factory, key = "report-$rtype-$rid")
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("举报") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        val i = state.form ?: run {
            if (state.loading) LoadingBox()
            else ErrorBox(state.message ?: "举报表单加载失败", viewModel::load)
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.message?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            // 押金说明卡
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "举报押金 100 积分，举报成立原额退回",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            // 举报理由：可选中卡片
            i.reasons.forEach { (v, label) ->
                val selected = state.reason == v
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (selected) androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.selectReason(v) },
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selected, onClick = { viewModel.selectReason(v) })
                        Text(
                            label.take(30),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.details, onValueChange = viewModel::updateDetails,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充说明（选填）") },
                shape = RoundedCornerShape(16.dp),
                minLines = 3
            )
            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (state.submitting) "提交中…" else "提交举报",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
