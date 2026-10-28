# MD3 重设计 · 实施状态记录

日期：2026-10-21  
范围：B1 设计系统、B2 应用壳与导航、B3 阅读主链、B4 主要页面家族、B5 构建与静态收口。  
性质：**实现记录，不是验收报告**；设计合同仍以 [总方案](../MD3_UI_REDESIGN.md) 与三份子文档为准。

## 1. 已完成

### B5 适配收口（构建证据）

| 交付 | 位置 | 说明 |
| --- | --- | --- |
| 阅读位置锚点 | `domain/topic/TopicReadingPositionRepository.kt`、`data/ReadingPositionStore.kt`、`ui/screens/TopicScreenEffects.kt` | 旧四段 CSV 与两小时 TTL 不变；新增 `v2:<topicId>` 锚点（floor/postId/listIndex/offset），仅在旧记录仍有效时生效；进入帖子按楼层恢复，缺失时回退旧 listIndex；`clear()` 仍清空全部 |
| 头像触区 | `ui/patterns/user/UserAvatar.kt` | `minTouchTarget` 用 48dp 独立触区包住 40/32dp 视觉头像，辅助技术只有一个操作节点；列表、作者行、私信会话与联系人已接入 |
| 双栏宽度预算 | `ui/navigation/AppWindowLayout.kt`、`AppRoot.kt` | 两项兼容布局不再扣 rail 宽度，改为扣顶部 Tab 高度；列表宽度按余额推导（280—360dp） |
| 生产编译 | `:app:compileDebugKotlin` | 通过（exit 0） |
| 候选包 | `:app:assembleDebug` | 通过（exit 0）；armv7a / armv8a / universal 三个 APK |
| APK 摘要 | `app/build/outputs/apk/debug/` | armv7a `F9EDA965…A5302`、armv8a `2F588C2E…C31D196E`、universal `09474213…AB997CDF`（完整 SHA-256 见验收记录） |
| 单元测试源码 | `:app:compileDebugUnitTestKotlin` | 旧基线失败：两个测试引用工作区已删除的 gacha 解析器；未修改测试、未恢复暂存删除 |
| 验收登记 | [ACCEPTANCE_EVIDENCE.md](ACCEPTANCE_EVIDENCE.md) | A01—A24 逐项状态、环境矩阵、DoH/网络观察与残余 |
| 设备抽查 | `docs/md3-redesign/evidence/`（83 个 png/xml） | API37 / 411dp：四根导航、消息分区、我的游客态、FAB→原站编辑器与脏退出确认；帖子阅读与离开重进恢复；字号 2.0、320dp、840dp 双栏与 rail、1440dp 双栏、低高度回退底栏、深色、外观预览与布局设置；快速连点根切换；动画关闭渲染；窄转宽详情迁移与宽屏钉住复验 |
| 对比度抽样 | `evidence/` 截图 + `ACCEPTANCE_EVIDENCE.md` §5 | 分组标签 6.07、设置标题 12.13、说明 6.03、底栏标签 5.48（浅）/10.33（深）、游客说明 8.04、返回图标 12.13、FAB 图标 6.06/6.07；背景图/纯黑/高对比未覆盖 |
| 离线静态审计 | [OFFLINE_CHECKS.md](OFFLINE_CHECKS.md)、[ROUTE_COVERAGE.md](ROUTE_COVERAGE.md) | 路由 62/62（4 条主题子页动态注册，新增 `messages`）；`ui/theme`/`ui/components` 无会话、导航、仓库依赖；18/20dp 圆角 0 遗留；14 处主题目录外颜色均为数据语义/遮罩；图标按钮描述缺失 0 处；偏好迁移位置清单 |
| 源站外壳状态 | `ui/screens/WebScreens.kt`、`platform/webview/LinkWindowClient.kt` | 主文档加载进度条、主文档错误就近重试（不改写源站 DOM） |

### B4 其余页面家族

| 交付 | 位置 | 说明 |
| --- | --- | --- |
| 回复编辑器 | `ui/screens/TopicReplyDialog.kt` | 保留 ModalBottomSheet + 全屏展开、预览、验证码、图床上传与失败保留；新增可访问的关闭按钮（提交中禁用），标题改为 500 字重 |
| 我的 | `ui/screens/MeScreen.kt` | 资料摘要 + 我的内容 / 社区工具 / 应用三组；游客头部改为说明登录价值并给一个主按钮；去掉固定 80dp 底部留白 |
| 搜索 | `ui/feature/search/SearchScreen.kt` | 顶栏补返回；进入页聚焦搜索框；范围/排序 Chip；主操作用 Filled 按钮；结果改为平面列表项 + inset divider |
| 淘帖 | `ui/screens/TopicCollectionsScreen.kt`、`TopicCollectionsComponents.kt` | 根顶栏改名“淘帖”，分区改为“大家的 / 我的”分段控件；专辑条目由 ElevatedCard 改为色调表面 + titleMedium |
| 设置外壳 | `ui/screens/SettingsPage.kt` | 表单最大 640dp 居中，宽窗 24dp 页边距，窄窗 16dp；所有设置子页共用 |
| 外观概览 | `ui/screens/ThemeSettingsScreen.kt`、`ThemeCardPreview.kt` | 概览加入只读实时示例；入口说明更新为“主导航 / 单双栏 / 评论底栏” |
| 私信会话 | `ui/screens/ChatScreen.kt` | 接收 surfaceContainer、发送 primaryContainer + 成对 on 色；气泡最宽 80% 且 ≤480dp；正文 16/24；输入区去阴影改色调表面 |
| 登录 | `ui/screens/LoginScreen.kt` | 顶栏补取消/返回；表单最大 640dp 居中；页边距走命名 token |
| 原站外壳 | `ui/screens/WebScreens.kt` | 顶栏标题使用默认 titleLarge，不再单独降到 titleMedium |
| 形状收敛 | 28 个 UI 文件 | 31 处 18/20dp 圆角统一为 `MaterialTheme.shapes.large`（16dp），由主题形状表单一来源 |
| 设置子页统一外壳 | `NetworkSettingsScreen`、`BrowseSettingsScreen`、`GeneralSettingsScreen`、`AiSettingsScreen`、`TransferSettingsScreen`、`DataManagementScreen` | 改用 `SettingsPage`：圆角/间距 token 生效，表单最大 640dp 居中，宽窗 24dp / 窄窗 16dp 页边距；DoH（LazyColumn）与屏蔽规则（整页条件态）保留各自外壳 |
| 本地资料页（P12） | `FavoritesScreen`、`FavoriteComponents`、`FootprintScreen`、`ExportedTopicsScreen`、`CardRedemptionsScreen`、`components/input/LocalFilterField.kt` | 收藏/历史共用全圆角过滤框；历史新增 `TopicCard.visitedAt` 并按今天/昨天/日期分组，行内只留时刻；导出列表改平面行并显示“HTML · 大小/空文件/无法读取 · 时间”；兑换记录默认遮蔽兑换码、按需查看/复制、空码显示“未记录兑换码”；清空文案区分本地记录与源站 |
| 榜单/邀请/举报/签到（P18） | `LeaderboardScreen`、`InviteCenterScreen`、`ReportScreen`、`CheckinScreen` | 榜单补返回、前三名改用主题容器而非写死金银铜、其余行平面化且头像 48dp；邀请中心状态与内容同壳、链接卡/统计/用户列表用 token 与 56dp 行、复制 2 秒复位；举报理由整行单选单一语义节点、错误与押金提示用容器色；签到统计改主题容器、按钮 ≥48dp、游客显示登录入口 |
| 认证中心 / 帖子过滤（P18） | `IdentityCenterScreen`、`BlockWordsScreen` | 认证头部改实色 `primaryContainer` 并补对比色，福利合并为单卡分隔行，条件行与申请状态用间距 token，按钮 ≥48dp；屏蔽词页把登录/加载/错误移到滚动列之外，表单与底部双按钮用 token 且 ≥48dp |
| 剩余页面与形状收口 | `DohSettingsScreen`、`AboutScreen`、`UserScreen`、`CollectionDetail*`、`TopicCollectionManage*`、`NewTopicFormContent` 等 25 个文件 | DoH 列表与服务器卡改 token、空态改文本、结果用 labelLarge；关于页间距 token；用户页标签栏/空态/回顶 FAB/积分分隔线；全 UI 的 14/13/15dp 圆角统一到 12/16dp（形状角色 4/8/12/16/28）；原站页面确认共用 `WebScreen`/`SourceAccountPage` 外壳（进度、错误重试、外链） |

### B3 阅读主链

| 交付 | 位置 | 说明 |
| --- | --- | --- |
| 正文阅读字阶接入 | `ui/content/HtmlContent.kt` | 正文改用 `LocalReadingType.body`（18/30），评论用 `comment`（16/26），引用 16/26；段后间距 16dp；标题取 H1/H2/H3 并带 heading 语义 |
| 代码块 | `ui/content/CodeBlockView.kt` | 14/22 等宽、不自动换行；局部横滚 + 折叠限高，页面不整体横向滚动 |
| 阅读列限宽 | `ui/screens/TopicScreenPostList.kt` | 帖子与评论同列，最大 720dp 居中；宽窗剩余空间留白 |
| 标题层级 | `ui/screens/TopicHeaderItem.kt` | 完整标题 headlineSmall 24/32，阅读量/评论数与字数另起 bodySmall 元信息行 |
| 主帖与评论 | `ui/screens/TopicPostCard.kt` | 主帖去掉卡片底色；评论改为平面列表项（无大圆角卡片），缩进每层 16dp、最多两级视觉缩进 |
| 作者行 | `ui/screens/TopicPostHeader.kt` | 名称 titleSmall/500，UID 恢复 11sp；楼层号触区补足 48dp |
| 帖子条目 | `ui/patterns/topic/TopicCardView.kt` | 平面单列：48dp 头像槽 + 12dp 间距 + 弹性文字列；标题 titleMedium 两行，作者/称号/板块/时间/回复在 bodySmall 元信息行；inset divider |
| 标签优先级 | `ui/patterns/topic/TopicCardTags.kt` | 常显最多两枚，其余收进可展开的「+N」；未读改用中性容器，不与业务标签共用蓝底 |
| 阅读操作栏 | `ui/screens/TopicActionComponents.kt`、`TopicScreenScaffold.kt` | 删除液态玻璃回复栏与相关 backdrop；统一标准 MD3 表面，inset 由 Scaffold 消费，列表不再垫 64dp |

### B1 设计系统

| 交付 | 位置 | 说明 |
| --- | --- | --- |
| 默认静态色板 | `ui/theme/ThemeColors.kt` | 品牌种子 `#375BAA`；浅/深完整 ColorScheme 按 DESIGN_SYSTEM D02 冻结表实现；`background`/`onBackground`/`surfaceVariant` 只做兼容映射 |
| 字阶 | `ui/theme/AppTypography.kt` | 15 个角色的字号/行高/字重显式登记；用户字体、应用字号因子、字重增量只应用一次；中文 tracking=0 |
| 阅读字阶 | 同上（`LocalReadingType`） | 正文 18/30、引用 16/26、代码 14/22 等宽、H1–H3、评论 16/26；已注入根部，页面尚未接入（B3） |
| 形状 | `ui/theme/AppShapes.kt` | 4 / 8 / 12 / 16 / 28dp |
| 间距角色 | `ui/theme/AppSpacing.kt` | inline 4、item 8、contentGap 12、pageHorizontal 16/24、pageVertical 16、section 24、paneGap 24、largeSection 32 |
| 尺寸角色 | `ui/theme/AppSizes.kt` | 图标 24、触区 48、顶栏 64、底栏 80、rail 80、FAB 56、头像 40/32/64、阅读列 720、表单 640、对话框 560、回复区 64 |
| 主题解析顺序 | `ui/theme/ThemeProvider.kt` | 动态色仅 API31+ 且默认配色未被覆盖；默认配色走冻结表，自定义种子/角色覆盖走 MaterialKolor；中性表面（旧偏好）与 OLED 仍生效；`keep_background_color` 新默认 false |
| 组件默认值 | `AppMenuDefaults` / `AppNoticeDefaults` / `AppSummaryDefaults` / `AppPaginationDefaults` | 圆角收敛到 12/16dp；设置行最小高度 56/72dp；分页胶囊 48dp 触区 |
| 公共组件 | `AppPageScaffold`、`AppConfirmDialog`、`AppMenuRow` | 顶栏最小 64dp、对话框最大 560dp、菜单行高度与图标 24dp 走 token |
| 隔离预览 | `ui/theme/ThemePreviewSamples.kt` | 浅色/深色/大字号/禁用四个 @Preview，只依赖公共 token 与组件 |

### B2 应用壳与导航

| 交付 | 位置 | 说明 |
| --- | --- | --- |
| 根目的地模型 | `ui/navigation/AppDestinations.kt` | 社区 / 淘帖 / 消息 / 我的 + 可选版块；标签与选中/未选中图标；`newTopic` 不属于目的地 |
| 窗口策略 | `ui/navigation/AppWindowLayout.kt` | 600/840/1200/1600 断点；宽 ≥600 且高 ≥480 用 rail，否则底栏；`U = 宽 − rail − 两侧边距`，`U ≥ 704` 才双栏；列表宽度由余量推导（280–360） |
| 导航组件 | `ui/navigation/AppPrimaryNavigation.kt` | 标准 NavigationBar / NavigationRail / 两项顶部 PrimaryTabRow，共用同一目的地与选中态 |
| 应用外壳 | `ui/navigation/AppContent.kt` | bar / rail / 双栏三种形态；根路由才显示根导航；社区根发帖 FAB（rail 模式置于 rail 上方）；切换根目的地时右栏回到 `detailEmpty` |
| 根协调器 | `ui/navigation/BottomNavigationCoordinator.kt` | 双栏时分别持有手机与主栏协调器；发帖作为动作不改变选中根 |
| 消息根 | `ui/screens/MessagesScreen.kt` | 私信/通知分段；两个分区各自保留 ViewModel 状态、分页与未读；旧 `directMessagesRoot` 直达私信分区 |
| 分区复用 | `ui/screens/DirectMessagesScreen.kt`、`NotificationsScreen.kt` | 拆出 `DirectMessagesSection` / `NotificationsSection`，旧路由仍保留完整外壳与返回 |
| 旧偏好迁移 | `data/AppSettingsGeneralStore.kt` | 底栏列表版本 3：先备份原始值 `bottom_bar_items_pre_v3`，再归一别名（`directMessages*`→`messages`、`topicCollectionsRoot`→`topicCollections`）并去重；不补齐、不排序、不写回默认四项；未知 ID 保留但不渲染 |
| 布局偏好语义 | `AppSettingsGeneralStore` / `GeneralSettings` / Session | `tablet_mode` 缺失或 true = 跟随窗口；显式 false = 始终单内容栏；导航仍随窗口适配 |
| 旧视觉下线 | 删除 `GlassBottomBar.kt`、`TabletNavRail.kt` | 底部导航不再渲染玻璃/折射；相关固定 72dp 留白一并移除 |
| 设置入口 | `ui/screens/ThemeLayoutSettings.kt` | 「平板模式」改为「跟随窗口 / 始终单内容栏」；根目的地候选改为 2—5 项并去掉发帖；移除无效的底栏样式开关 |

## 2. 迁移映射（已实现）

| 旧数据/入口 | 新行为 |
| --- | --- |
| `bottom_bar_items` 含 `directMessages` / `directMessagesRoot` | 归一为 `messages`，选中消息根 |
| `bottom_bar_items` 含 `topicCollectionsRoot` | 归一为 `topicCollections` |
| `bottom_bar_items` 含 `newTopic` | 从目的地派生中忽略；路由仍可导航，改为社区 FAB |
| `forums` | 保留为可选根目的地（旧值不再被强制改写为淘帖） |
| 自定义项 <2 个 | 显示默认四项，**不写回**原值 |
| 自定义项恰为 2 个 | 顶部 PrimaryTabRow，取消底部导航 |
| `tablet_mode` 键缺失 | 跟随窗口（可双栏） |
| `tablet_mode=true` | 跟随窗口 |
| `tablet_mode=false` | 始终单内容栏；rail 仍随窗口 |
| `keep_background_color` 缺失 | false（带轻微色调的 MD3 表面） |
| `keep_background_color=true` | 保留旧中性面策略（`neutralPageColors`） |
| `theme_mode` / `dynamic_color` / `theme_color` / 覆盖色 / 字体 / 背景 | 原样读取，解析顺序见 ThemeProvider |
| `bottom_bar_style` / `reply_bar_style` | 仅作兼容数据；主导航与阅读操作栏都不再渲染玻璃 |

## 3. 验证证据

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 生产编译 | `.\gradlew.bat :app:compileDebugKotlin --offline --console=plain` | `BUILD SUCCESSFUL`（exit 0，2026-10-21） |
| 候选包 | `.\gradlew.bat :app:assembleDebug --offline --console=plain` | `BUILD SUCCESSFUL`（exit 0）；三个 ABI APK，SHA-256 见 [验收记录](ACCEPTANCE_EVIDENCE.md) |
| 差异格式 | `git diff --check` | exit 0，无新增格式错误 |
| 文件规模 | 变更的 236 个 Kotlin 文件逐行统计 | 全部 ≤250 行；`AppSettingsGeneralStore` 拆出 `BottomBarPreferenceStore`，`WebScreens` 拆出 `ExportedHtmlScreen` |
| 依赖边界 | 人工核对 | 新增 theme 文件不依赖 Session/导航/仓库；`AppContent` 只读 Session 的偏好快照 |
| 测试源码编译 | `:app:compileDebugUnitTestKotlin --offline` | 失败（旧基线）：测试引用已删除的 gacha 解析器，与本次 UI 改动无关 |

未执行：实机/模拟器回归、TalkBack、折叠与 IME 场景（见第 5 节与 [验收记录](ACCEPTANCE_EVIDENCE.md)）。

## 4. 实现中的决定

1. **不新增 adaptive 依赖**：窗口策略用 `LocalConfiguration` 的当前窗口 `screenWidthDp/screenHeightDp` 计算，未引入 `material3-adaptive` 或 `androidx.window`。理由：现有依赖已能表达窗口宽度分类，避免为外观名称强制升级/锁定新依赖；若后续需要折叠姿态或精确 pane 断点，可替换 `AppWindowLayout` 单点实现。
2. **glass 组件直接删除**：主导航已无生产调用，按实现计划允许清理；未保留新旧视觉切换器。
3. **发帖 FAB 与回到顶部按钮**：FAB 固定在右下，首页“回到顶部”移到左下，避免 48dp 触区重叠。
4. **底部留白**：根导航由外壳 Scaffold 的 `bottomBar` 消费 inset，首页/板块/淘帖列表移除了原先为悬浮玻璃条写死的 72dp 留白。

## 5. 残余与下一批

- **页面级重排已覆盖全部路由**：原站 WebView 页面按规格保留 DOM，只共用应用外壳；其余页面已改用公共组件与间距/形状 token。
- **微形状未逐一复核**：`6/9/10/22/24dp` 仍有少量（角标、媒体、头像预览等），未按 4/8/12/16/28 全量替换，需视觉确认后再决定。
- **A12 兼容性待实测**：旧键保留、新增 `v2:` 锚点并回退旧 listIndex；楼层锚点路径与旧版本/旧备份双向读取未测。
- 单/双栏已迁移当前详情；每根独立的详情历史仍未实现。- F01/F02/F05/F07 为代码修复，仍待登录/双账号/背景图/两项布局场景复验。
- **DoH 与网络阻断**：内置 7 个 DoH 端点在测试环境测速全部失败；`linux.sb:443` 从主机与模拟器均超时，依赖在线内容的检查无法完成；本机其他项目未找到可复用 DoH 配置。
- 设备证据仍不完整：A09/A11/A19/A21/A23 未测，截图未经视觉复核。
- 测试源码编译仍受旧基线 gacha 引用影响，未在本次修改范围内。

后续：补齐 DoH 地址后复测在线内容、旧版本/旧备份双向读取、TalkBack/对比度/性能测量与人工视觉复核。
