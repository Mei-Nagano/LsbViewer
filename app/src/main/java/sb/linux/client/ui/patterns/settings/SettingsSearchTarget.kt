package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

internal const val SETTINGS_SEARCH_TARGET = "settings_search_target"
internal data class SettingsSearchTarget(val title: String, val consume: () -> Unit)
internal val LocalSettingsSearchTarget = staticCompositionLocalOf<SettingsSearchTarget?> { null }

@Composable
internal fun SettingsSearchHost(nav: NavHostController, content: @Composable () -> Unit) {
    val handle = nav.currentBackStackEntry?.savedStateHandle
    val flow = remember(handle) { handle?.getStateFlow<String?>(SETTINGS_SEARCH_TARGET, null) ?: MutableStateFlow(null) }
    val title by flow.collectAsState()
    CompositionLocalProvider(LocalSettingsSearchTarget provides title?.let {
        SettingsSearchTarget(it) { handle?.set<String?>(SETTINGS_SEARCH_TARGET, null) }
    }, content = content)
}

/** 与真实控件共用标题，只响应本详情页的一次搜索请求。 */
internal fun Modifier.settingsSearchAnchor(title: String): Modifier = composed {
    val target = LocalSettingsSearchTarget.current
    if (target?.title != title || title.isBlank()) return@composed this
    val requester = remember { BringIntoViewRequester() }
    var highlighted by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val scrollMargin = with(LocalDensity.current) { 12.dp.toPx() }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    LaunchedEffect(target.title) {
        withFrameNanos { }
        delay(180)
        requester.bringIntoView(Rect(0f, -scrollMargin, bounds.width.toFloat(), bounds.height + scrollMargin))
        highlighted = true
        delay(2400)
        target.consume()
    }
    this.bringIntoViewRequester(requester)
        .onSizeChanged { bounds = it }
        .drawWithContent {
            drawContent()
            if (highlighted) {
                // 绘在控件之后：TextField／Surface 的不透明底色不会盖住高光。
                // 向内留白避免分组圆角与视口边缘裁切描边。
                val inset = 4.dp.toPx()
                val innerSize = Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f))
                val outline = shape.createOutline(innerSize, layoutDirection, this)
                withTransform({ translate(inset, inset) }) {
                    drawOutline(outline, colors.primary.copy(alpha = 0.10f))
                    drawOutline(outline, colors.primary, style = Stroke(2.dp.toPx()))
                }
            }
        }
        .semantics { if (highlighted) stateDescription = "搜索定位" }
}

@Composable
internal fun searchingFor(vararg titles: String): Boolean = LocalSettingsSearchTarget.current?.title in titles
