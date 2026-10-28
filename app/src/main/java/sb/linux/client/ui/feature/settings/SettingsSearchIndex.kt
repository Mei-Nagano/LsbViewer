package sb.linux.client.ui.feature.settings

/** 具体选项级索引，不依赖一级分类简介；别名用于习惯叫法和英文缩写。 */
internal object SettingsSearchIndex {
    data class Entry(val title: String, val category: String, val route: String, val aliases: String = "")
    val entries = buildList {
        fun group(category: String, route: String, vararg titles: String) {
            titles.forEach { value -> add(Entry(value.substringBefore('|'), category, route, value.substringAfter('|', ""))) }
        }
        group("应用与账号", "generalSettings", "检查更新|版本 频率", "重置所有应用设置")
        group("应用与账号", "settings", "个人资料与账号|昵称 签名 密码")
        group("网络与链接", "networkSettings", "链接打开方式|浏览器 内置 外部", "链接预览|网站 图标 简介")
        group("发帖与图片上传", "uploadSettings", "内置图床|Pixhost Catbox 图片上传", "自定义图床接口|token 文件字段")
        group("网络与链接", "networkSettings", "应用代理|HTTP SOCKS5 翻墙 代理模式 主机 端口",
            "DNS over HTTPS|DoH 加密 域名解析", "VPN 自动停用 DoH|代理",
            "DoH 默认服务器|digitale-gesellschaft telekom t53 aa.net.uk", "DoH 自定义服务器|名称 备注 地址", "DoH 测速|延迟 毫秒 DNS 解析")
        group("阅读与交互", "browseSettings", "滚动模式|分页 无限", "每页帖子数量", "每页评论数量",
            "启动保留首页缓存", "刷新后新帖折叠", "Markdown 表格|横向滑动 适应屏幕",
            "显示 UID|头像 数字 隐藏", "智能编码识别|Base64 Hex URL Unicode ROT13 解密",
            "对话串联", "打赏弹幕", "侧边栏双列", "在线用户", "取消收藏确认")
        group("外观与布局", "themeSettings", "深浅模式|夜间 日间 系统")
        group("外观与布局", "themeColors", "OLED 纯黑", "动态取色", "主题色|主色 辅色 第三色", "调色风格|对比度", "帖子卡片颜色")
        group("外观与布局", "themeBackground", "自定义背景图片|壁纸 透明度 背景色", "禁止页面染色|中性 背景 卡片")
        group("外观与布局", "themeFonts", "字体大小|缩放", "字体粗细|字重", "自定义字体|ttf otf")
        group("外观与布局", "themeLayout", "底栏样式|液态玻璃 胶囊", "底栏按钮自定义", "帖子回复底栏", "平板双栏")
        group("AI 助手", "aiSettings", "开启 AI 总结", "自动总结", "总结包含评论")
        group("AI 助手", "aiConfiguration", "AI 接口配置|API URL Key 密钥 温度", "模型列表", "总结提示词|prompt", "AI 配置方案|多配置 切换")
        group("我的内容", "aiHistory", "总结历史|保存 记录", "收藏总结")
        group("存储与备份", "transferSettings", "选择备份内容|设置 历史 收藏 AI 阅读位置", "导入导出 JSON")
        group("存储与备份", "webDavSettings", "WebDAV 备份|同步 恢复")
        group("存储与备份", "storageSettings", "缓存分析|空间 大小")
        group("存储与备份", "dataManagement", "首页缓存清理", "帖子缓存清理", "图片缓存清理", "阅读位置清理", "AI 总结历史清理", "AI 配置与密钥清理", "链接预览缓存清理")
        group("存储与备份", "autoClearSettings", "自动清理|退出 启动")
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
