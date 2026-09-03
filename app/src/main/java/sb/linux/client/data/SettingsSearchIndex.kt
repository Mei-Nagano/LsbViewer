package sb.linux.client.data

/** 具体选项级索引，不依赖一级分类简介；别名用于习惯叫法和英文缩写。 */
internal object SettingsSearchIndex {
    data class Entry(val title: String, val category: String, val route: String, val aliases: String = "")
    val entries = buildList {
        fun group(category: String, route: String, vararg titles: String) {
            titles.forEach { value -> add(Entry(value.substringBefore('|'), category, route, value.substringAfter('|', ""))) }
        }
        group("常规设置", "generalSettings", "链接打开方式|浏览器 内置 外部", "链接悬浮预览|网站 图标 简介",
            "匿名免费图床|Catbox 图片上传 免登录", "自定义图床接口|token 文件字段", "检查更新|版本 频率")
        group("DNS over HTTPS", "dohSettings", "DNS over HTTPS|DoH 加密 域名解析", "VPN 自动停用 DoH|代理",
            "DoH 默认服务器|digitale-gesellschaft telekom t53 aa.net.uk", "DoH 自定义服务器|名称 备注 地址", "DoH 测速|延迟 毫秒 DNS 解析")
        group("浏览设置", "browseSettings", "滚动模式|分页 无限", "每页帖子数量", "每页评论数量", "评论排序|热度 正序 倒序",
            "启动保留首页缓存", "刷新后新帖折叠", "Markdown 表格|横向滑动 适应屏幕",
            "显示 UID|头像 数字 隐藏", "智能编码识别|Base64 Hex URL Unicode ROT13 解密",
            "对话串联", "打赏弹幕", "侧边栏双列", "在线用户", "取消收藏确认")
        group("外观设置", "themeSettings", "深浅模式|夜间 日间 系统", "OLED 纯黑", "动态取色", "主题色|颜色 主色 辅色 第三色",
            "强调色精确取值|原始 色值 不调和 保真", "禁止页面染色|屏幕 泛绿 泛紫 蒙色 中性 背景 卡片",
            "自定义背景图片|壁纸 透明度 背景色", "调色风格|对比度",
            "帖子卡片颜色", "字体大小|缩放", "字体粗细|字重", "自定义字体|ttf otf", "底栏样式|液态玻璃 胶囊",
            "底栏按钮自定义", "帖子回复底栏", "平板双栏")
        group("AI 总结", "aiSettings", "AI 接口配置|API URL Key 密钥 温度", "模型列表", "总结提示词|prompt",
            "AI 配置方案|多配置 切换", "自动总结", "总结包含评论", "总结历史|保存 记录", "收藏总结")
        group("备份设置", "transferSettings", "选择备份内容|设置 历史 收藏 AI 草稿 阅读位置", "导入导出 JSON", "WebDAV 备份|同步 恢复")
        group("数据管理", "dataManagement", "缓存分析|空间 大小", "首页缓存清理", "帖子缓存清理", "图片缓存清理",
            "阅读位置清理", "AI 总结历史清理", "AI 配置与密钥清理", "草稿清理", "链接预览缓存清理", "自动清理|退出 启动")
        group("使用统计", "usageStats", "使用统计 AI 分析|浏览 收藏 发帖 回帖 积分")
        group("屏蔽词管理", "blockWords", "屏蔽词与屏蔽用户|关键词 黑名单 过滤")
        group("发卡兑换记录", "cardRedemptions", "发卡兑换记录|卡密 已兑换")
        group("已导出的帖子", "exportedTopics", "已导出的帖子|导出 文件 长图 多页")
    }

    fun search(query: String): List<Entry> {
        val words = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        return entries.filter { entry -> words.all { "${entry.title} ${entry.category} ${entry.aliases}".contains(it, true) } }
    }
}
