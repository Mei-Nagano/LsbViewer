package sb.linux.client.ui.patterns.settings

import androidx.compose.ui.graphics.vector.ImageVector

/** 设置标题共用语义图标；菜单、开关、多选和输入框使用同一组本地资源。 */
internal fun settingsRowIcon(title: String): ImageVector = with(SettingsIconAssets) {
    when (title) {
        "阅读与交互", "正文与评论", "主题" -> Article
        "列表与分页", "首页无限滚动", "板块无限滚动", "评论无限滚动",
        "每页帖子数（5 - 50）", "评论每页数量（5 - 100）" -> List
        "启动时不刷新首页", "折叠刷新后的新帖子" -> Refresh
        "头像与元数据同行" -> Layout
        "隐藏帖子标签", "隐藏外链跳转提示" -> Hidden
        "恢复显示帖子签名", "自定义总结提示词", "个人签名", "签名", "签名内容", "隐藏全站帖子签名", "保存签名设置" -> Signature
        "Markdown 表格横向滑动" -> Table
        "显示 UID" -> Badge
        "智能识别编码内容" -> Terminal
        "帖子交互", "选择图片前询问上传方式" -> Touch
        "对话串联" -> Tree
        "显示打赏弹幕", "解析评论区", "回复" -> Forum
        "社区" -> Home
        "版块" -> Grid
        "私信" -> Mail
        "侧边栏", "侧边栏双列显示" -> Sidebar
        "侧边栏展示在线用户" -> Groups
        "社区活跃度", "活跃度" -> Activity
        "屏蔽规则", "用户屏蔽名单", "常用关键词", "自定义屏蔽词", "新屏蔽词", "屏蔽版块" -> Block
        "网络与链接", "源站域名" -> Globe
        "应用代理", "代理类型", "代理主机", "代理端口" -> Shield
        "加密 DNS", "启用 DoH", "DoH 服务器", "名称", "HTTPS 地址", "备注（可选）" -> Dns
        "对应用内打开的网站生效", "外部链接打开方式" -> Open
        "VPN 开启时自动停用" -> Shield
        "无 VPN 时自动优化", "重新检测本地代理", "测速全部服务器" -> Speed
        "链接设置", "链接预览卡片", "API 地址", "上传接口地址" -> Link
        "外观与布局", "主题设置", "主题色", "配色角色与卡片元素", "调色风格", "Material 动态取色",
        "主要色", "次要色", "强调色", "卡片背景", "帖子标签", "时间", "用户名", "评论数", "所属板块" -> Palette
        "壁纸", "选择背景图片", "启用背景图片", "自定义背景图片" -> Wallpaper
        "字体", "导入自定义字体" -> Font
        "移除自定义字体" -> Trash
        "取消全部测速" -> Block
        "字体粗细" -> Bold
        "界面缩放" -> Scale
        "深浅模式", "OLED 纯黑" -> DarkMode
        "对比度等级", "背景图片不透明度", "卡片、顶栏表面不透明度" -> Contrast
        "自定义背景色", "禁止页面染色" -> Palette
        "导航设置", "导航栏", "宽屏列表与详情" -> Layout
        "隐藏状态栏" -> Expand
        "AI 助手", "总结行为", "开启 AI 总结", "打开帖子自动总结", "AI 总结历史" -> Sparkles
        "接口与配置方案", "模型", "获取模型列表" -> Robot
        "API 密钥", "访问令牌（可选）", "密码 / 应用密码" -> Key
        "温度 (0 - 2)" -> Temperature
        "方案名称", "文件字段名", "保存当前配置为方案", "保存代理设置", "保存自定义图床" -> Tune
        "总结历史", "浏览历史", "浏览历史与阅读位置", "阅读位置" -> History
        "只显示已收藏的总结", "取消收藏确认", "我的收藏", "本地收藏", "评论收藏", "帖子与评论收藏", "收藏" -> Bookmark
        "发帖与图片上传", "默认上传方式", "图床服务" -> Upload
        "存储与备份", "空间占用", "首页与侧边栏缓存", "帖子与阅读量缓存", "链接预览缓存" -> Storage
        "图片缓存", "帖子与其他图片" -> Image
        "本地备份" -> Folders
        "WebDAV 云备份", "服务器地址", "备份目录", "备份到 WebDAV" -> CloudUpload
        "导出设置", "从 WebDAV 恢复", "已导出的帖子" -> Download
        "导入设置" -> Upload
        "自动清理", "退出时自动清理", "立即清理", "清理所选项", "清理所选图片缓存", "移除图片", "删除" -> Trash
        "应用与账号", "个人资料与账号", "账号", "我的" -> Accounts
        "资料" -> ProfileCard
        "头像" -> Profile
        "版本更新", "检查更新时机", "检查间隔（1 - 720 小时）", "立即检查更新" -> Update
        "关于应用", "关于", "开源协议" -> Info
        "我的内容" -> Folders
        "节点" -> Sitemap
        "附件管理", "附件" -> Attachment
        "表情", "我的表情", "管理我的表情" -> Sticker
        "邀请", "邀请中心" -> Invite
        "烧饼", "我的烧饼" -> Shaobing
        "称号", "我的称号" -> Crown
        "广告", "我的广告" -> Billboard
        "发卡兑换记录" -> Ticket
        "抽奖奖品记录" -> Gift
        "我的隐私", "隐私" -> Shield
        "原站广告" -> Campaign
        "通知" -> Notifications
        "应用设置", "应用代理设置" -> Settings
        else -> when {
            title.startsWith("重置") || title.startsWith("恢复") -> Refresh
            title.contains("清理") -> Trash
            title.contains("图片") || title.contains("图床") -> Image
            title.contains("缓存") -> Storage
            title.contains("AI") || title.contains("总结") -> Robot
            title.contains("收藏") -> Bookmark
            title.contains("历史") || title.contains("阅读位置") -> History
            title.contains("屏蔽") -> Block
            title.contains("用户") || title.contains("账号") -> Profile
            else -> Settings
        }
    }
}
