package sb.linux.client.ui.feature.settings

import androidx.navigation.NavHostController
import sb.linux.client.ui.patterns.settings.SETTINGS_SEARCH_TARGET

/** 标题与真实设置控件一致；别名只参与检索，不改变定位目标。 */
internal object SettingsSearchIndex {
    data class Entry(val title: String, val category: String, val route: String, val aliases: String = "", val itemIndex: Int = 0)
    val entries = buildList {
        fun group(category: String, route: String, vararg options: String, itemIndex: Int = 0) {
            options.forEach { value -> add(Entry(value.substringBefore('|'), category, route, value.substringAfter('|', ""), itemIndex)) }
        }
        group("阅读与交互", "browseList", "首页无限滚动|首页滚动模式 分页", "板块无限滚动|版块 分页", "评论无限滚动|回复 分页",
            "每页帖子数（5 - 50）|帖子数量", "评论每页数量（5 - 100）|评论数量", "启动时不刷新首页|启动保留缓存",
            "折叠刷新后的新帖子|刷新新帖折叠", "头像与元数据同行|帖子卡片布局", "隐藏帖子标签|置顶 精华 抽奖 发卡")
        group("阅读与交互", "browseContent", "显示打赏弹幕|弹幕", "Markdown 表格横向滑动|自动适应表格宽度",
            "显示 UID|头像 数字", "智能识别编码内容|Base64 Hex URL Unicode ROT13", "恢复显示帖子签名|隐藏 评论 签名")
        group("阅读与交互", "browseInteraction", "点击头像显示信息卡|个人资料 卡片 直接进入用户主页", "对话串联|树形评论", "取消收藏确认")
        group("阅读与交互", "browseSidebar", "侧边栏双列显示|版块 快捷功能", "侧边栏展示在线用户|当前在线 人数")
        group("阅读与交互", "browseSettings", "重置阅读与交互设置")
        group("网络与链接", "networkLinks", "外部链接打开方式|浏览器 应用内 外部",
            "隐藏外链跳转提示|jump 自动跳转", "链接预览卡片|网站 图标 简介")
        group("网络与链接", "networkProxy", "代理类型|HTTP SOCKS5 关闭 翻墙", "代理主机|host 地址", "代理端口|port",
            "重新检测本地代理|自动检测 启动代理", "保存代理设置")
        group("网络与链接", "networkDns", "启用 DoH|DNS over HTTPS 加密 DNS", "对应用内打开的网站生效|外部网站 DNS",
            "VPN 开启时自动停用|停用 DoH", "无 VPN 时自动优化|自动启用 最快服务器", "DoH 服务器|t53 telekom neeb",
            "添加自定义服务器|DoH 名称 地址 备注", "测速全部服务器|延迟 毫秒", "取消全部测速")
        group("自定义 DoH 服务器", "networkDns", "名称|DoH 服务器名称", "HTTPS 地址|DoH 自定义地址", "备注（可选）|DoH 备注")
        group("网络与链接", "networkSettings", "重置网络设置")
        group("外观与布局", "themeColors", "深浅模式|跟随系统 浅色 深色 夜间 日间", itemIndex = 1)
        group("外观与布局", "themeColors", "配色角色与卡片元素|配色角色 元素颜色", "主要色|主色", "次要色", "强调色", "卡片背景|帖子卡片背景",
            "帖子标签|标签颜色", "时间|时间颜色", "用户名|用户名颜色", "评论数|评论数颜色", "所属板块|版块颜色", itemIndex = 3)
        group("外观与布局", "themeColors", "主题色|自定义 预选 颜色", itemIndex = 5)
        group("外观与布局", "themeColors", "调色风格|配色方案 调色板", itemIndex = 7)
        group("外观与布局", "themeColors", "Material 动态取色|系统壁纸", "OLED 纯黑|深色背景", itemIndex = 9)
        group("外观与布局", "themeColors", "对比度等级|低 标准 高", itemIndex = 11)
        group("外观与布局", "themeBackground", "自定义背景色|页面底色", "恢复背景色|跟随主题", "选择背景图片|壁纸 导入", "移除图片|删除壁纸",
            "启用背景图片", "背景图片不透明度|透明度", "卡片、顶栏表面不透明度|透明度", "禁止页面染色|中性色", itemIndex = 2)
        group("外观与布局", "themeFonts", "字体|默认 自定义 ttf otf 导入", "导入自定义字体", "移除自定义字体",
            "字体粗细|极细 细体 较细 标准 较粗 粗体", itemIndex = 2)
        group("外观与布局", "themeFonts", "界面缩放|显示比例 字体大小", itemIndex = 3)
        group("外观与布局", "themeSettings", "隐藏状态栏|系统 时间 电量 沉浸 安全空间")
        group("外观与布局", "themeLayout", "导航栏|入口 顺序 上移 下移", itemIndex = 1)
        group("外观与布局", "themeLayout", "社区", "版块", "节点", "私信", "我的", itemIndex = 2)
        group("外观与布局", "themeSettings", "宽屏列表与详情|单页 自动并排 内容布局 平板 双栏", "重置外观设置")
        group("AI 助手", "aiBehavior", "开启 AI 总结|开关", "打开帖子自动总结|自动运行", "解析评论区|总结包含评论")
        group("AI 助手", "aiConfiguration", "方案名称|已保存方案 配置方案", "保存当前配置为方案", "API 地址|接口 URL",
            "API 密钥|Key", "模型|模型列表", "获取模型列表|刷新模型", "温度 (0 - 2)|temperature", "自定义总结提示词|prompt", "保存 AI 设置")
        group("AI 助手", "aiHistory", "只显示已收藏的总结|收藏 历史筛选")
        group("发帖与图片上传", "uploadSettings", "默认上传方式|源站附件 内置图床 自定义图床", "选择图片前询问上传方式",
            "图床服务|Pixhost Catbox", "上传接口地址|URL", "访问令牌（可选）|token", "文件字段名", "保存自定义图床")
        group("存储与备份", "backupLocal", "应用设置", "浏览历史与阅读位置", "帖子与评论收藏", "AI 配置、总结历史与总结收藏",
            "发卡兑换记录", "导入设置|导入 JSON", "导出设置|导出 JSON")
        group("存储与备份", "backupCloud", "应用设置", "浏览历史与阅读位置", "帖子与评论收藏", "AI 配置、总结历史与总结收藏",
            "发卡兑换记录", "服务器地址|WebDAV URL 坚果云 Nextcloud", "账号|WebDAV 用户名", "密码 / 应用密码|WebDAV",
            "备份目录|WebDAV 路径", "备份到 WebDAV", "从 WebDAV 恢复")
        group("存储与备份", "storageAnalysis", "空间占用|缓存分析 大小", "头像|头像缓存", "帖子与其他图片|图片缓存", "清理所选图片缓存",
            "首页与侧边栏|首页缓存", "阅读量缓存", "AI 总结数据|总结缓存")
        val dataOptions = arrayOf("首页与侧边栏缓存", "帖子与阅读量缓存", "图片缓存", "阅读位置", "AI 总结历史", "浏览历史",
            "本地收藏", "评论收藏", "发卡兑换记录", "AI 配置与密钥", "链接预览缓存", "自定义背景图片")
        group("立即清理", "storageClear", *dataOptions, "清理所选项")
        group("退出时自动清理", "storageAutomatic", *dataOptions)
        group("应用与账号", "appUpdates", "检查更新时机|每次启动 间隔 从不", "检查间隔（1 - 720 小时）", "立即检查更新")
        group("应用与账号", "generalSettings", "重置所有应用设置")
        group("个人签名", "personalSignature", "签名内容|个人签名", "隐藏全站帖子签名", "保存签名设置")
        group("阅读与交互", "blockWords", "用户屏蔽名单|黑名单 屏蔽用户", "常用关键词|屏蔽词 过滤", "自定义屏蔽词|关键词 过滤",
            "新屏蔽词|添加关键词", "添加屏蔽词", "屏蔽版块|屏蔽板块", "恢复列表默认", "保存屏蔽规则")
        group("阅读与交互", "browseSettings", "列表与分页", "正文与评论", "帖子交互", "侧边栏", "屏蔽规则")
        group("网络与链接", "networkSettings", "应用代理", "加密 DNS", "链接设置")
        group("外观与布局", "themeSettings", "主题设置", "壁纸", "导航设置")
        group("AI 助手", "aiSettings", "总结行为", "接口与配置方案", "总结历史")
        group("存储与备份", "storageSettings", "立即清理", "自动清理", "本地备份", "WebDAV 云备份")
        group("应用与账号", "generalSettings", "个人资料与账号", "版本更新", "关于应用")
        group("设置分类", "appSettings", "阅读与交互", "外观与布局", "发帖与图片上传", "网络与链接", "AI 助手", "存储与备份")
    }

    fun search(query: String): List<Entry> {
        val words = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        return entries.filter { entry -> words.all { "${entry.title} ${entry.category} ${entry.aliases}".contains(it, true) } }
    }
}

internal fun openSettingsSearchResult(nav: NavHostController, entry: SettingsSearchIndex.Entry) {
    nav.navigate(entry.route)
    nav.currentBackStackEntry!!.savedStateHandle[SETTINGS_SEARCH_TARGET] = entry.title
    nav.currentBackStackEntry!!.savedStateHandle["settings_search_item"] = entry.itemIndex
}
