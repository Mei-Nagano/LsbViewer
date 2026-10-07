package sb.linux.client.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.account.CheckinViewModel
import sb.linux.client.ui.feature.account.CheckinViewModelFactory
import sb.linux.client.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(session: Session, nav: NavHostController) {
    val factory = remember(session) {
        CheckinViewModelFactory(session.checkinRepository, session::showToast, session::onCheckinDone)
    }
    val viewModel: CheckinViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日签到") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) { LoadingBox(); return@Column }
            val i = state.info
            if (i == null) {
                Text("加载失败，请重试", style = MaterialTheme.typography.bodyMedium)
            } else {
                // 连续 / 累计签到天数：双统计卡片
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (i.streak > 0) "${i.streak}" else "-",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("连续签到", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (i.total > 0) "${i.total}" else "-",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("累计签到", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (i.message.isNotBlank()) {
                    Text(i.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (session.account.loginState.loggedIn && i?.canCheckin == true) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (state.submitting) "签到中…" else "立即签到", style = MaterialTheme.typography.titleMedium) }
            } else if (!session.account.loginState.loggedIn) {
                Button(onClick = { nav.navigate("login") }) { Text("登录后签到") }
            }
        }
    }
}

// ---------------- 创作者认证中心 ----------------
