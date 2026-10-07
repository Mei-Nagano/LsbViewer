package sb.linux.client.ui

import androidx.compose.runtime.staticCompositionLocalOf


/**
 * 帖子内容链接打开入口（常规设置「打开链接方式」，3.20）：
 * MainActivity 注入；站内链接路由到原生页面，其余按设置选择应用内 WebView 或外部浏览器。
 * 未注入时（预览/独立预览组件）回退系统浏览器。
 */
val LocalLinkHandler = staticCompositionLocalOf<((String) -> Unit)?> { null }
/** 0 = 表格适应屏幕，1 = 保持列宽横向滑动 */
val LocalTableDisplayMode = staticCompositionLocalOf { 1 }
val LocalShowUid = staticCompositionLocalOf { true }
val LocalSmartDecode = staticCompositionLocalOf { false }

