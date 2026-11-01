package sb.linux.client.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.theme.AppStateDefaults

/** 首次加载占位：调用方传入内边距，不再在外面套一层 `Box(Modifier.padding(...))`。 */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * 行内加载指示器（加载更多、按钮内、局部刷新）。
 *
 * 默认 22dp / 2dp 描边，局部区域需要更大时传 [size]；整页首次加载用 [LoadingBox]。
 */
@Composable
fun AppInlineLoading(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = Color.Unspecified,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth,
    )
}

@Composable
fun ErrorBox(msg: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .padding(AppStateDefaults.pagePadding), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppStateDefaults.errorSpacing)
        ) {
            Box(
                Modifier
                    .size(AppStateDefaults.glyphContainerSize)
                    .clip(AppStateDefaults.glyphShape)
                    .background(
                        MaterialTheme.colorScheme.errorContainer
                            .copy(alpha = AppStateDefaults.glyphBackgroundAlpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudOff, null,
                    Modifier.size(AppStateDefaults.glyphSize),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                msg,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            FilledTonalButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
fun EmptyBox(text: String = "暂无内容", subtitle: String = "", modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppStateDefaults.infoSpacing)
        ) {
            Box(
                Modifier
                    .size(AppStateDefaults.glyphContainerSize)
                    .clip(AppStateDefaults.glyphShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Inbox, null,
                    Modifier.size(AppStateDefaults.glyphSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

/** 未登录占位：和 [EmptyBox]/[ErrorBox] 同一套版式，多一个去登录按钮 */
@Composable
fun LoginRequiredBox(
    title: String, subtitle: String, modifier: Modifier = Modifier, onLogin: () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(AppStateDefaults.pagePadding), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppStateDefaults.infoSpacing)
        ) {
            Box(
                Modifier
                    .size(AppStateDefaults.glyphContainerSize)
                    .clip(AppStateDefaults.glyphShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                            .copy(alpha = AppStateDefaults.glyphBackgroundAlpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login, null,
                    Modifier.size(AppStateDefaults.glyphSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onLogin, shape = AppStateDefaults.actionShape) { Text("去登录") }
        }
    }
}
