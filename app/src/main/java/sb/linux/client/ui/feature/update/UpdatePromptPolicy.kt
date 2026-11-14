package sb.linux.client.ui.feature.update

import sb.linux.client.BuildConfig
import sb.linux.client.domain.update.UpdateInfo
import sb.linux.client.domain.update.UpdateProjectLinks
import sb.linux.client.domain.update.isNewerVersion

/** Debug 使用明确标记的开发预览；Release 始终严格比较真实版本。 */
internal object UpdatePromptPolicy {
    val debugPreview: Boolean get() = BuildConfig.DEBUG
    fun available(release: UpdateInfo?, current: String): UpdateInfo? {
        if (debugPreview) return UpdateInfo(
            tag = "v$current-dev-preview",
            name = "开发版更新弹窗预览",
            url = release?.url ?: UpdateProjectLinks.RELEASES_URL,
            notes = "当前为 Debug 开发版，暂按有更新展示弹窗。这是预览数据，不代表已发布新版本。\n\n" +
                (release?.notes?.takeIf(String::isNotBlank) ?: "更新内容预览：\n• 新增隐藏状态栏设置\n• 改善正文滑动和阅读进度\n• 完善网页弹窗展示\n\n点击下载将打开项目的 Release 页面。"),
        )
        return release?.takeIf { isNewerVersion(it.tag, current) }
    }
}
