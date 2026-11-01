package sb.linux.client.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.domain.update.UpdateProjectLinks
import sb.linux.client.ui.AppUpdatePromptDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    val (doCheck, updateState) = rememberUpdateChecker(session)

    // 应用图标（包管理器 → Bitmap）
    val appIcon = remember {
        runCatching {
            val d = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = android.graphics.Bitmap.createBitmap(
                d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val c = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            bmp.asImageBitmap()
        }.getOrNull()
    }
    val appName = remember {
        runCatching {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        }.getOrNull() ?: "LinuxSB"
    }
    val spacing = LocalAppSpacing.current
    val versionName = currentVersionName(context)
    val versionCode = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 0
    }

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = "关于应用",
        onBack = { nav.popBackStack() },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(spacing.largeSection))
            // 应用图标
            appIcon?.let {
                androidx.compose.foundation.Image(
                    bitmap = it,
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(Modifier.height(spacing.section))
            // 应用名称
            Text(appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(spacing.contentGap))
            // 版本号（圆角矩形）
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "v$versionName ($versionCode)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.inline)
                )
            }
            Spacer(Modifier.height(spacing.item))
            // 应用简介（不超过十个字）
            Text(
                "轻量社区客户端",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(spacing.section))
            // List group
            GroupCard {
                SettingMenuRow("开发人员", "Mei-Nagano", Icons.Filled.Person) { openExternal(UpdateProjectLinks.DEVELOPER_URL) }
                SettingMenuRow("项目地址", "GitHub · ${UpdateProjectLinks.REPO}", Icons.Filled.Code) { openExternal(UpdateProjectLinks.PROJECT_URL) }
                SettingMenuRow("开源协议", "MIT License", Icons.Filled.Gavel) { openExternal(UpdateProjectLinks.LICENSE_URL) }
                SettingMenuRow(
                    "检查更新",
                    if (updateState.checking) "正在检查…" else "检查 GitHub 最新版本",
                    Icons.Filled.SystemUpdate,
                ) { doCheck(false) }
                SettingMenuRow("意见反馈", "GitHub Issues", Icons.Filled.Feedback) { openExternal("${UpdateProjectLinks.PROJECT_URL}/issues/new") }
            }
            updateState.msg?.let {
                Spacer(Modifier.height(spacing.contentGap))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(spacing.section))
        }
    }

    // 发现新版本对话框
    updateState.update?.let { rel ->
        AppUpdatePromptDialog(
            releaseName = rel.name,
            currentVersion = versionName,
            notes = rel.notes,
            onDownload = { openExternal(rel.url); updateState.dismiss() },
            onDismiss = updateState.dismiss,
        )
    }
}
