@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.account.CheckinViewModel
import sb.linux.client.ui.feature.account.CheckinViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 每日签到：连续/累计双统计 + 主操作；游客显示登录入口而不是空态。 */
@Composable
fun CheckinScreen(session: Session, nav: NavHostController) {
    val factory = remember(session) {
        CheckinViewModelFactory(session.checkinRepository, session::showToast, session::onCheckinDone)
    }
    val viewModel: CheckinViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val spacing = LocalAppSpacing.current
    LaunchedEffect(viewModel) { viewModel.load() }

    AppPageScaffold(
        title = "每日签到",
        onBack = { nav.popBackStack() },
    ) { pad ->
        when {
            state.loading -> LoadingBox(Modifier.padding(pad))
            state.info == null -> ErrorBox("签到信息加载失败", Modifier.padding(pad), onRetry = viewModel::load)
            else -> {
                val info = state.info!!
                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
                    verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                        StatTile(
                            value = if (info.streak > 0) "${info.streak}" else "-",
                            label = "连续签到",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = if (info.total > 0) "${info.total}" else "-",
                            label = "累计签到",
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (info.message.isNotBlank()) {
                        Text(
                            info.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when {
                        !session.account.loginState.loggedIn -> Button(
                            onClick = { nav.navigate("login") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.large,
                        ) { Text("登录后签到") }
                        info.canCheckin -> Button(
                            onClick = viewModel::submit,
                            enabled = !state.submitting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                if (state.submitting) "签到中…" else "立即签到",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(spacing.item))
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = container) {
        Column(
            Modifier.padding(vertical = spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = content)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.75f))
        }
    }
}
