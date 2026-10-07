package sb.linux.client.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal enum class FullscreenVideoOrientation {
    PORTRAIT,
    LANDSCAPE,
}

internal val FullscreenVideoOrientation.requestedOrientation: Int
    get() = when (this) {
        FullscreenVideoOrientation.PORTRAIT ->
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        FullscreenVideoOrientation.LANDSCAPE ->
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

/** WebChromeClient 交付的全屏视图及其生命周期回调。 */
internal data class FullscreenVideoSession(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback,
    val orientation: FullscreenVideoOrientation,
    val activity: Activity?,
    val previousOrientation: Int?,
) {
    fun restoreOrientation() {
        previousOrientation?.let { activity?.requestedOrientation = it }
    }
}

/** 承载播放器全屏视图；返回键和网页自身的退出按钮统一交给调用方收口。 */
@Composable
internal fun FullscreenVideoDialog(
    session: FullscreenVideoSession,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val hostView = LocalView.current
        val window = (hostView.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.setBackgroundDrawableResource(android.R.color.black)
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                WindowCompat.getInsetsController(it, hostView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {
                window?.let {
                    WindowCompat.getInsetsController(it, hostView).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().background(Black)) {
            AndroidView(
                factory = {
                    (session.view.parent as? ViewGroup)?.removeView(session.view)
                    session.view.apply { setBackgroundColor(Color.BLACK) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
