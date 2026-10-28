@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.ReportViewModel
import sb.linux.client.ui.feature.community.ReportViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 举报表单：理由整行单选（一个操作语义节点），提交保持 48dp 以上高度。 */
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
    val spacing = LocalAppSpacing.current
    LaunchedEffect(viewModel) { viewModel.load() }

    AppPageScaffold(
        title = "举报",
        onBack = { nav.popBackStack() },
    ) { pad ->
        val form = state.form
        if (form == null) {
            if (state.loading) LoadingBox(Modifier.padding(pad))
            else ErrorBox(state.message ?: "举报表单加载失败", Modifier.padding(pad), onRetry = viewModel::load)
            return@AppPageScaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
        ) {
            state.message?.let { message ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = spacing.contentGap, vertical = spacing.item),
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "举报押金 100 积分，举报成立原额退回",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = spacing.contentGap, vertical = spacing.item),
                )
            }
            form.reasons.forEach { (value, label) ->
                val selected = state.reason == value
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { viewModel.selectReason(value) },
                            )
                            .padding(horizontal = spacing.contentGap, vertical = spacing.item),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.item),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            label.take(30),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.details,
                onValueChange = viewModel::updateDetails,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充说明（选填）") },
                shape = MaterialTheme.shapes.medium,
                minLines = 3,
            )
            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    if (state.submitting) "提交中…" else "提交举报",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
