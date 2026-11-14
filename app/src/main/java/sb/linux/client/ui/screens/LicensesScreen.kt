package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import sb.linux.client.ui.theme.LocalAppSpacing

/** 仓库许可证随 APK 分发，离线也可以查看。 */
@Composable
internal fun LicensesScreen(nav: NavHostController) {
    val context = LocalContext.current
    val license = remember(context) {
        context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() } +
            "\n\nKaTeX 0.19.0\n\n" + context.assets.open("katex/LICENSE").bufferedReader().use { it.readText() } +
            "\n\nHighlight.js 11.10.0\n\n" + context.assets.open("source-topic/highlight-LICENSE").bufferedReader().use { it.readText() } +
            "\n\n" + context.assets.open("licenses/material-symbols.txt").bufferedReader().use { it.readText() }
    }
    SettingsPage("开源协议", nav) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(license, Modifier.fillMaxWidth().padding(LocalAppSpacing.current.pageHorizontal),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
