package sb.linux.client.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.platform.webview.WebJsDialog
import sb.linux.client.platform.webview.WebViewWindowHost

/** 一个在线页面持有的完整子窗口；关闭父窗口同时释放它的后代。 */
internal class SourceWebWindows {
    private class Window(val parent: WebView, val view: WebView) {
        var title by mutableStateOf("网页")
    }
    private val windows = mutableStateListOf<Window>()
    private val dialogs = mutableStateListOf<WebJsDialog>()
    private var released = false
    private var activeView by mutableStateOf<WebView?>(null)

    fun show(parent: WebView, popup: WebView) {
        if (released) dispose(popup) else { windows.add(Window(parent, popup)); activeView = popup }
    }
    val activeWebView: WebView? get() = activeView
    fun updateHost(host: WebViewWindowHost, primary: WebView? = null) {
        host.show(listOfNotNull(primary) + windows.map { it.view },
            activeView ?: primary ?: windows.lastOrNull()?.view)
    }
    fun back(): Boolean {
        val active = windows.firstOrNull { it.view === activeView } ?: return false
        if (active.view.canGoBack()) active.view.goBack() else close(active.view)
        return true
    }

    @Composable
    fun WindowMenu() {
        if (windows.isEmpty()) return
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) { Text("窗口 ${windows.size}") }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(text = { Text("原网页") }, onClick = { activeView = null; expanded = false })
                windows.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = {
                    activeView = item.view; expanded = false
                }) }
            }
        }
    }
    fun showJsDialog(dialog: WebJsDialog) {
        if (released) dialog.finish(false) else dialogs.add(dialog)
    }
    private fun finish(dialog: WebJsDialog, confirmed: Boolean, text: String = dialog.defaultValue) {
        dialogs.remove(dialog)
        dialog.finish(confirmed, text)
    }
    fun title(view: WebView, title: String) {
        windows.firstOrNull { it.view === view }?.title = title.ifBlank { "网页" }
    }
    fun close(view: WebView) {
        val closing = mutableSetOf(view)
        windows.forEach { if (it.parent in closing) closing.add(it.view) }
        val removed = windows.filter { it.view in closing }
        dialogs.filter { it.owner in closing }.forEach { finish(it, false) }
        windows.removeAll(removed.toSet())
        if (activeView in closing) activeView = windows.lastOrNull()?.view
        removed.asReversed().forEach { dispose(it.view) }
    }
    fun clear() {
        released = true
        dialogs.toList().forEach { finish(it, false) }
        val removed = windows.toList()
        windows.clear()
        removed.asReversed().forEach { dispose(it.view) }
    }
    private fun dispose(view: WebView) {
        // Compose 先移除可见视图，再释放 Chromium 窗口。
        view.stopLoading()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            view.destroy()
        }
    }

    @Composable
    fun Content() {
        if (windows.isEmpty() && dialogs.isEmpty()) return
        // 外壳始终是同一个 Android 窗口；新子窗口不能盖住已经打开的 JS 对话。
        Dialog(onDismissRequest = {
            dialogs.firstOrNull()?.let { finish(it, false) }
                ?: windows.lastOrNull()?.let { close(it.view) }
        }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
            Box(Modifier.fillMaxSize()) {
                windows.lastOrNull()?.let { active ->
                    AppPageScaffold(active.title, onBack = {
                        if (active.view.canGoBack()) active.view.goBack() else close(active.view)
                    }, actions = {
                        IconButton(onClick = { close(active.view) }) { Icon(Icons.Filled.Close, "关闭窗口") }
                    }) { padding ->
                        Box(Modifier.padding(padding).fillMaxSize()) {
                            AndroidView(factory = { WebViewWindowHost(it) },
                                update = { updateHost(it) }, modifier = Modifier.fillMaxSize(),
                                onRelease = { it.removeAllViews() })
                        }
                    }
                }
                dialogs.firstOrNull()?.let { dialog -> key(dialog) {
                    SourceJsDialog(dialog) { confirmed, text -> finish(dialog, confirmed, text) }
                } }
            }
        }
    }

    @Composable
    fun InlineControls() {
        androidx.activity.compose.BackHandler(enabled = activeView != null) { back() }
        Box(Modifier.fillMaxSize()) {
            if (activeView != null && windows.isNotEmpty()) {
                var menu by remember { mutableStateOf(false) }
                val active = windows.firstOrNull { it.view === activeView }
                Surface(Modifier.fillMaxWidth().height(56.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { menu = true }) { Text(active?.title ?: "原网页 · ${windows.size} 个窗口") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("原网页") }, onClick = { activeView = null; menu = false })
                                windows.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = {
                                    activeView = item.view; menu = false
                                }) }
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        if (active != null) IconButton(onClick = { close(active.view) }) { Icon(Icons.Filled.Close, "关闭窗口") }
                    }
                }
            }
            dialogs.firstOrNull()?.let { dialog -> key(dialog) {
                SourceJsDialog(dialog) { confirmed, text -> finish(dialog, confirmed, text) }
            } }
        }
    }
}

@Composable
internal fun rememberSourceWebWindows(): SourceWebWindows {
    val windows = remember { SourceWebWindows() }
    DisposableEffect(windows) { onDispose { windows.clear() } }
    return windows
}
