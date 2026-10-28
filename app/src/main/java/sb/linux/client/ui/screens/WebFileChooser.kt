package sb.linux.client.ui.screens

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/** 原站附件上传使用系统文档选择器，支持单选和批量选择。 */
@Composable
internal fun rememberWebFileChooser(): (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean {
    var pending by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pending?.onReceiveValue(uri?.let { arrayOf(it) }); pending = null
    }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pending?.onReceiveValue(uris.takeIf { it.isNotEmpty() }?.toTypedArray()); pending = null
    }
    DisposableEffect(Unit) { onDispose { pending?.onReceiveValue(null); pending = null } }
    return { callback, params ->
        pending?.onReceiveValue(null)
        pending = callback
        val types = params.acceptTypes.flatMap { it.split(',') }.map(String::trim)
            .filter { it.contains('/') }.toTypedArray().ifEmpty { arrayOf("*/*") }
        try {
            if (params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) multiple.launch(types)
            else single.launch(types)
        } catch (_: Exception) { pending?.onReceiveValue(null); pending = null }
        true
    }
}
