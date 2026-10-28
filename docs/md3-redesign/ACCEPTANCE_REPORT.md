# MD3 大改版实现验收记录

日期：2026-10-21；对象：当前工作区实现及 [总方案](../MD3_UI_REDESIGN.md)、[设计系统](DESIGN_SYSTEM.md)、[页面规格](SCREEN_SPECIFICATIONS.md)、[验收计划](IMPLEMENTATION_AND_ACCEPTANCE.md)。

**最近独立复验结论：仍未通过验收，当前 2 项 P1、2 项 P2。构建通过；F03 布局迁移、F05 背景保护仍未闭合，新增 F08 消息分区重复加载（P2）、F09 默认列表黑底（P1）。原首载死锁、消息根账号 key、钉住的非法路由及文件行数已有修正，两项导航 FAB 已运行复验。最新证据与限制见第 8 节。**

> 第 7 节保留修复阶段的记录。第 8 节独立复验表明：单个帖子路由迁移成功，不代表根导航、子栈与草稿迁移全部通过；钉住不再崩溃，也不代表所有前置栈下都回到空详情。最新状态以第 8 节为准。

以下第 1—5 节保留首轮历史记录。首轮只审查和记录，未修改生产源码、测试源码、偏好或设备设置；当时没有在线设备，行为问题仅有代码路径证据。后续模拟器复现以第 6 节为准。验收范围不包含修改实现代码。

## 1. 构建与结构证据

| 检查 | 实际结果 |
| --- | --- |
| `:app:compileDebugKotlin :app:assembleDebug` | 成功，36 个任务均 up-to-date，约 6 秒 |
| UI 改动 `git -c core.safecrlf=false diff --check` | 通过 |
| UI Kotlin 文件规模 | 318 个文件，最大 253 个物理行；HtmlContent.kt 超过 250 行上限 |
| 默认静态色表 | 对照设计文档，浅/深各 33 个显式角色无差异；shadow 未显式声明，本次不将其计为已核对的显式角色 |
| 字阶与限宽接入 | 已存在阅读 18/30、评论 16/26、代码 14/22 的公共角色；TopicScreenPostList 使用 720dp 最大宽度，SettingsPage 使用 640dp 表单最大宽度；仅代码证据 |
| 导航与偏好 | 已实现四个默认目的地、发帖 FAB、bar/rail、两项顶部 Tab、旧底栏别名迁移与原字符串备份；行为缺口见下文 |
| 旧布局偏好 | tablet_mode 缺失/true 跟随窗口，显式 false 限制单内容栏；keep_background_color 新默认 false，显式选择仍可读取 |
| 阅读位置格式 | 数字 topicId 仍保持四段 CSV 与两小时 TTL；新增锚点写入独立 v2 键；运行恢复效果未实测 |
| 设备 | 初次仅见 offline 的 emulator-5554，结束前列表为空；未安装本次 APK、未操作实际账号 |
| 测试 | 未运行业务单元测试，本次未重跑已有本地测试源码编译 |
| 审查期间源码稳定性 | 对 main/java 的文件集合与 SHA-256 做前后核对，未发现本轮审查中改动 |

当前本地 armv8a APK SHA-256：

```text
dff9e9c318d0f0995a4d9ac3b0e39bcd42b132e0f5c20df00e9168d0d12825e2
```

此哈希只标识本地产物，没有对应的本轮设备 base.apk 核对证据。

## 2. 需要修复的问题

### F01 · P1：消息中心首次加载被初始 loading 状态挡住

位置：`app/src/main/java/sb/linux/client/ui/screens/MessagesScreen.kt:133–138`。

`DirectMessagesUiState` 和 `NotificationsUiState` 均默认 `loading=true`，各自 ViewModel 构造时没有自动调用 load。新宿主却只在“列表为空且 loading=false”时加载。因此登录后首次打开私信，或第一次切换通知，条件都为 false，不会启动请求，页面保持初始加载状态。通知刷新按钮又在 loading=true 时禁用；私信可通过手动刷新绕过，但不是正常的首次进入流程。

建议：显式区分未加载/请求中/已加载，或由宿主对新账号与新分区保证一次初始请求；不能仅移除 busy 防护导致每次重组重新请求。

复验：登录用户从新建页面进入两个分区均各发起一次初始加载；空结果也能结束忙碌；来回切换不重复加载或标记另一区已读。对应 A14/A19、P08。

### F02 · P1：私信列表状态没有按账号隔离

位置：`app/src/main/java/sb/linux/client/ui/screens/MessagesScreen.kt:62–68`、`:133–138`。

私信 ViewModel 使用固定 key `messages-dm`，首次加载副作用只依赖分区及 loggedIn，不依赖用户 ID；列表非空时又跳过加载。账号 A 手动加载出联系人后，退出并登录 B，再恢复消息根页面，可复用保留在导航栈中的同一 ViewModel，显示 A 的联系人和预览。通知使用了带 uid 的 key，私信没有。MainActivity/AppRoot 没有在账号切换时整体重建导航，账号生命周期协调器也没有清除此 ViewModel。

这是基于保留栈与状态所有权的代码结论，双账号运行结果尚未实测；处理 F01 不能替代账号隔离。

建议：以账号身份隔离状态与在途任务，切换账号立即撤下旧联系人/查询/错误；新身份发起自己的首次加载。不要只在新请求成功后才覆盖旧显示。

复验：A 有列表 → 退出 → B 登录 → 返回消息，全程不呈现 A 的联系人/预览；覆盖保存/恢复根栈及 A 的迟到请求。对应 A14/A15/A17、P08。

### F03 · P1：单双栏切换与根切换没有保存完整详情状态

位置：`app/src/main/java/sb/linux/client/ui/navigation/AppRoot.kt:95–115`；`AppContent.kt:78–90`。

单栏、双栏使用三个独立 NavController。切换 effect 只按根目的地 ID 同步，没有搬移当前详情 route 参数、选中 ID、详情子栈或输入状态。单栏正在看 `topic/{tid}` 时，canonical 得不到根目的地，转宽后新详情从 detailEmpty 开始；反向切换也只尝试跳到 master 根。另一个路径是宽屏切换根：代码显式以 `saveState=false` 清掉详情栈，返回该根时不能恢复原会话/帖子详情。

建议：把逻辑根、选中内容和任务状态独立于显示宿主保存，切换布局投影同一状态；为每个根保留详情历史。需要迁移的是带实参的目的地和状态，不能把路由模板字符串当作实际目标。

复验：读帖子到某楼层/编辑回复 → 扩宽/缩窄 → 仍在同一内容及输入；宽屏切到另一根再回来保留详情与列表位置。对应 A06–A08/A12/A13、P01/P05/P09。

### F04 · P1：宽屏“钉住”导航到当前图不存在的 home

位置：`app/src/main/java/sb/linux/client/ui/screens/TopicScreenScaffold.kt:81–83`。

宽屏右侧 NavHost 只注册 detailRoutes，home 只注册在左侧 masterRoutes；帖子页收到的是 detailNav。“钉住”先保存快照，再调用同一个 nav.navigate("home")，目标无法在详情图中解析。TopicActionComponents 中仍有实际钉住按钮连接此回调，因此这不是未使用代码。

代码路径表明该操作会抛出导航目标不存在的异常；本轮没有设备异常日志。该行可能属于沿用代码，本报告不将它强行归为本轮新增，但新默认自适应使该现有宽屏路径成为验收范围。

建议：钉住后由拥有根导航的协调层切回社区，并让详情宿主按定义退出；不要在详情图添加重复 home 页面来回避所有权问题。

复验：单栏及宽屏打开帖子 → 钉住 → 返回社区 → 恢复该帖子，均无异常，快照和位置有效。对应 A05/A08/A12、P05。

### F05 · P2：背景图模式缺少正文与表单不透明保护

位置：`app/src/main/java/sb/linux/client/ui/theme/ThemeProvider.kt:122–130`；`ui/screens/TopicPostCard.kt:70–76`。

主题注释声称不统一修改角色 alpha，实际仍把 surface、surfaceVariant、全部 surfaceContainer 一起设置 surfaceOpacity，并把 background 置透明。主帖又已移除原卡片背景，只用 Column 承载正文，没有补阅读保护面。因此背景图直接进入文字背后，表单使用的表面也受同一透明设置影响。

按当前允许的配置进行静态合成反例计算：默认浅色背景 #FAF8FF、黑色背景图强度 0.65、主帖无保护面，正文 #1A1B21 与该背景的对比约为 **2.38:1**，低于设计的 4.5:1。这是颜色模型反例，不是设备截图测量；足以否定“全部配置都有正文保护”的实现假设。

建议：保持完整不透明 ColorScheme，将背景和可透明装饰面独立处理；正文/表单/关键状态明确使用不透明保护角色。保留用户原始背景设置，但遵守新版作用范围。

复验：浅深色、黑/白/高细节背景及透明度边界，核对最终合成颜色；背景仍可显示，正文与表单可读。对应 A01/A20、P05/P14。

### F06 · P2：HtmlContent 超过源码物理行上限

位置：`app/src/main/java/sb/linux/client/ui/content/HtmlContent.kt`，当前 **253 个物理行**。

仓库约束上限为 250，包含空行和注释。本次按文件逐行读取计数；不能用忽略空行的文本计数作为通过依据。

建议按职责拆分渲染分支/图片查看宿主等有意义的单元，不通过压缩格式或移除有用注释凑行数。

复验：受影响 authored source 均不超过 250 行，编译通过，职责与渲染行为保留。对应结构阶段检查门槛。

## 3. 设计覆盖与尚未验证项

已取得源码证据的内容包括：冻结配色、公共字阶与预览、主题形状、菜单/状态默认、平面帖子列表、阅读限宽、消息分段、导航模型及部分偏好迁移。以上证明相关实现存在，不证明所有视觉/状态已验收通过。

仍需按计划验证 P01—P18 的实际显示与交互、A01—A24 的适用条目及环境矩阵，尤其是大字号回流、折叠铰链、系统安全区、IME、TalkBack、源站 WebView、媒体及登录后流程。当前 AppWindowLayout 仅按配置宽高和固定预算计算，未见字号测量或折叠特征参与；不据此宣称完整自适应已完成。

自动签到没有手动入口不作为问题；本轮未登录，也未验证签到结果。旧两份测试的本地引用问题沿用历史记录，本轮未重新编译测试，不计入这六项新报告。

## 4. 修复与复验顺序

1. 先处理消息首次加载与账号隔离，避免修一处后保留另一处状态问题。
2. 收口根/详情导航所有权，修复布局迁移、根栈恢复和宽屏钉住。
3. 加入正文与表单保护面，并按职责拆分超长源码。
4. 编译打包后对受影响流程定向复验，再补设备矩阵；无需为未改动的部分反复重启全部检查。

## 5. 附件

- [机器检查记录](acceptance-2026-10-21/results.json)
- [构建日志](acceptance-2026-10-21/build.log)
- [静态色表与背景对比反例](acceptance-2026-10-21/color-check.json)

本报告及附件在 `docs/` 下，当前被 Git 忽略，仅保存在工作区。没有本轮真机截图，不能挪用上一轮截图作为新设计证据。

## 6. 六项问题复验与模拟器验证（2026-10-21）

### 当前结论

上轮 F01—F06 均未关闭。源码复验之外，本次实际执行了模拟器导航验证，F03 的窄转宽详情丢失和 F04 的钉住崩溃已复现。没有物理真机证据，也没有登录真实账号。

| 原问题 | 本次观察 | 状态 |
| --- | --- | --- |
| F01 首次消息加载 | MessagesScreen:136/138 仍要求 empty && !loading；两类状态仍默认 loading=true，构造无初载 | 未修复；静态证据，未登录复现 |
| F02 私信账号隔离 | messages-dm 仍是固定 key，effect 无 uid，账号协调器没有撤下这份状态 | 未修复；静态证据，未执行双账号场景 |
| F03 导航状态恢复 | 窄屏打开公开帖子后扩大窗口，右侧实际显示“从左侧选择内容查看”；根切换清空详情代码也仍存在 | 未修复；窄转宽已运行复现，其余状态恢复仍需验证 |
| F04 宽屏钉住 | 宽屏重新选帖、点击钉住，进程退出；日志为 home 不在 detailEmpty 所属导航图中，堆栈指向 TopicScreenScaffold.kt:83 | 未修复；已运行复现 |
| F05 阅读保护面 | ThemeProvider:122–130 仍统一透明化各表面，主帖仍是无背景 Column | 未修复；本次未改背景偏好复验颜色场景 |
| F06 文件行数 | HtmlContent.kt 仍为 253 个物理行 | 未修复；机器计数 |

### F07 · 新增 P2：两项兼容宽屏布局隐藏发帖 FAB

位置：`app/src/main/java/sb/linux/client/ui/navigation/AppContent.kt:139–140`，以及宽屏分支的 rail/header 条件。

当用户只保留两个根目的地、窗口宽 ≥600dp 且高 ≥480dp 时，AppWindowLayout.useRail=true，但 AppContent 因 twoItemLayout 不渲染 rail；浮动按钮又只在 !layout.useRail 时显示。同一发帖动作的两个承载位置因此都被跳过。双栏路径同样只把 FAB 放进被跳过的 rail header。

HomeTopBar 的“发帖”菜单仍存在，所以这是方案要求的显著发帖动作缺失，不能称为发帖功能完全不可达。本项是源码发现，未为了验证它改写模拟器的自定义导航偏好。

建议依据实际是否渲染了动作宿主选择 FAB 位置，而不是直接依据 useRail；同时覆盖两项与四项、单内容栏与双栏。

### 构建、产物与运行环境

- `:app:compileDebugKotlin :app:assembleDebug` 成功，36 个任务 up-to-date，约 17 秒；UI 差异格式检查通过。
- 当前 UI Kotlin 文件为 320 个，最大物理行数 253。main/java 文件集合及哈希在审查期间没有变化；本次未修改生产或测试源码。
- emulator-5554 为 API 37、x86_64 系统，实际 ABI 列表含 arm64-v8a，并启用 libndk_translation.so；本次保留数据安装 ARM64 包成功。不能再根据“x86 模拟器”名称推断该设备必然无法运行 ARM64 包。
- 原窗口 1080×2424、420dpi、系统字体 1.0；测试扩大到 2400×1800，即约 914×686dp。已有 tablet_mode=true，未改应用设置，截图使用模拟器原有主题/卡片偏好，不作为默认配色校准证据。
- 本地 APK 与已安装 base.apk 的 SHA-256 一致：

```text
c79eec3d17ff612deb1d4909db052bd79b7321147164e42ae78905f65d36f073
```

### 复现步骤与证据

1. 在窄屏社区打开公开热议公告；正文与“登录后回复”可见，确认是游客可读页面。
2. 扩大窗口；右侧由该帖变为空详情占位，捕获 XML 与截图。
3. 从左侧重新打开同一帖，确认右侧“钉住”动作可见。
4. 点击“钉住”；AndroidRuntime 记录主线程 IllegalArgumentException，进程 6037 退出。关键异常为：`Navigation destination that matches route home cannot be found`，当前图的 startDestination 是 detailEmpty。

测试后恢复原窗口尺寸，密度仍为 420dpi、系统字体仍为 1.0；重新启动应用并确认回到社区首页。未登录、发布、举报、清理数据或手动签到；阅读公开帖子会产生正常浏览记录。

私信初载/双账号、背景图保护、两项兼容 FAB、大字号/折叠/IME/TalkBack 与完整页面矩阵仍没有本轮运行证据。不能把模拟器成功启动或公开帖可读作为这些项目通过的证据。

### 本次附件

- [机器结果](recheck-2026-10-21/results.json)、[构建日志](recheck-2026-10-21/build.log)
- [窄屏帖子](recheck-2026-10-21/topic-compact.png)、[扩大窗口后空详情](recheck-2026-10-21/wide-empty-confirmed.png)
- [宽屏重新打开帖子及钉住按钮](recheck-2026-10-21/topic-wide.png)、[实际崩溃日志](recheck-2026-10-21/pin-runtime-errors.log)
- [原显示参数](recheck-2026-10-21/display-before.json)、[恢复确认](recheck-2026-10-21/display-after.json)、[恢复后的首页](recheck-2026-10-21/home-restored.png)

对应 XML 同存附件目录。所有截图属于模拟器，不是物理真机。

## 7. 本轮修复与复验（2026-10-21 晚）

结论：F01—F07 均已提交代码修复；F03、F04 已在模拟器运行复验通过；F01/F02 需登录与双账号、F05 需背景图合成、F07 需两项导航偏好，仍待对应场景复验。构建通过，HtmlContent.kt 回到 238 行、AppRoot.kt 194 行。

| 问题 | 处理 | 复验状态 |
| --- | --- | --- |
| F01 消息首次加载 | 状态增加 `loaded`；私信/通知初值 `loading=false`；宿主按 `!loaded` 发起一次请求 | 已修复（代码）；未登录复验 |
| F02 私信账号隔离 | ViewModel key 改为 `messages-dm-<uid>`，首次加载副作用依赖 uid | 已修复（代码）；未做双账号复验 |
| F03 布局切换丢详情 | 新增 `PaneMigration.kt`：按目的地 id/实参迁移，跨配置变更保留详情快照 | **运行复验通过**：窄屏打开公开帖后扩宽，右栏保留该帖且无空态（`47-topic-compact.xml`、`46-wide-migrated.*`） |
| F04 宽屏钉住崩溃 | 有 `LocalMasterNav` 时由主栏回社区并 `popBackStack`；窄屏仍走本图 home | **运行复验通过**：宽屏钉住无崩溃、详情回空态（`48-wide-pin.*`），日志无 `Navigation destination` 异常 |
| F05 背景图保护面 | `ColorScheme.surface` 保持不透明；主帖、回复、设置页在背景图模式加不透明保护面 | 部分修复；未做黑/白/高细节背景合成复验 |
| F06 文件行数 | 拆出 `HtmlDetailsBlock.kt`，`HtmlContent.kt` 253→238 | 已修复（机器计数） |
| F07 两项布局隐藏 FAB | 单栏分支在 `useRail` 且两项时仍出 FAB；双栏两项在主栏右下补 FAB | 已修复（代码）；未改导航偏好复验 |

构建与规模：`:app:compileDebugKotlin`、`:app:assembleDebug` 均通过；243 个变更 Kotlin 文件均 ≤250 行；`git diff --check` 通过。APK：

```text
armv7a  F9EDA9656674229FABFC4CEB87B12333C0656396F489E9AF3FE12B476DBA5302
armv8a  2F588C2E32DC234EBD850B112E636CDF6F32CA1A68A0383245E54D42C31D196E
universal 0947421345453BE5C837DF42CC619CFED24655C3BA89B37564848911AB997CDF
```

## 8. 独立复验（2026-10-21，本次验收）

结论：**未通过；当前明确待修为 F03（P1）、F05（P2）、新增 F08（P2）、新增 F09（P1）**。本轮只检查实现、构建与设备行为，未修改生产或测试源码。F01/F02 的原消息根代码缺陷已修正，但登录后的请求行为、双账号隔离尚无运行证据。

### 8.1 构建、产物与设备

| 检查 | 本轮证据 |
| --- | --- |
| 编译与打包 | `:app:compileDebugKotlin :app:assembleDebug` 成功，36 个任务 up-to-date，约 3 秒；未运行业务单元测试 |
| UI 物理行数 | `app/src/main/java/sb/linux/client/ui` 下 321 个 Kotlin 文件均 ≤250 行；HtmlContent.kt 为 238 行 |
| 差异空白检查 | UI 路径 `git -c core.safecrlf=false diff --check` 成功；该命令本身不检查未跟踪文件 |
| 源码稳定性 | main/java 的 558 个文件集合及 SHA-256 前后相同，本次没有新增、删除或修改源码 |
| 安装与身份 | 保留数据覆盖安装 armv8a debug APK；本地 APK 与设备 base.apk 的 SHA-256 相同，见下方 |
| 运行环境 | emulator-5554，sdk_gphone16k_x86_64，API 37，通过 ARM64 翻译运行；本轮为游客，无双账号或真实发帖操作 |
| 尺寸 | 窄屏 1080×2424，宽屏临时 2400×1800；密度 420dpi、系统字号 1.0 |
| 恢复 | 已恢复 1080×2424、420dpi、字号 1.0，导航顺序恢复 `home,topicCollections,messages,me`，tablet_mode 仍为 true；结束时社区首页及四项导航正常显示 |

```text
2f588c2e32dc234ebd850b112e636cdf6f32ca1a68a0383245e54d42c31d196e
```

机器结果：[results.json](finalcheck-2026-10-21/results.json)、[build.log](finalcheck-2026-10-21/build.log)、[设置恢复核对](finalcheck-2026-10-21/preferences-restore-result.json)。界面测试产生正常阅读足迹；未清除应用数据或设备日志。

### 8.2 原问题逐项复验

| 编号 | 本轮状态 | 依据与边界 |
| --- | --- | --- |
| F01 | 原始死锁代码修正 | 两个 UiState 默认 `loading=false, loaded=false`，宿主按 loaded 启动首载，空结果/错误均结束首次状态；请求中的再次进入缺口另记 F08。未登录实测 |
| F02 | 消息根代码修正 | 私信和通知 VM 均按 uid 分 key，加载 effect 包含 uid；B 不订阅 A 的 VM。未做双账号运行复验，不将此扩大为所有兼容入口均已通过 |
| F03 | **未闭合，P1** | 当前帖子窄转宽能恢复，但消息根会进入错误宿主；当前 route 的重导航也未迁移子栈、VM 和输入状态，详见 8.3 |
| F04 | 原崩溃路径修正并实测 | 宽屏打开公开帖后钉住，PID 8848 前后相同，社区仍显示，无上轮进程退出。此次前置栈含错误迁入详情栏的消息，pop 后右栏回到消息，并非空态；该栈问题纳入 F03，不宣称全部钉住恢复流程已通过 |
| F05 | **未闭合，P2** | surface 保持不透明已有改善，但保护条件遗漏 backgroundImage 存在且 surfaceOpacity=1 的配置，详见 8.4 |
| F06 | 关闭 | HtmlDetailsBlock 拆分后 HtmlContent 为 238 行，全部 321 个 UI Kotlin 文件物理行数均合规；构建通过 |
| F07 | 关闭原按钮缺失问题 | 通过 UI 暂时选择 `home,topicCollections` 两项；窄屏有发帖 FAB，扩宽后顶部两项 Tab 和主栏右下 FAB 均存在，随后恢复原顺序。未点击发送帖子 |

F04 证据：[宽屏帖子](finalcheck-2026-10-21/13-topic-wide-confirmed.png)、[钉住后界面](finalcheck-2026-10-21/14-wide-after-pin.png)。F07 证据：[两项窄屏](finalcheck-2026-10-21/32-two-nav-compact.png)、[两项宽屏](finalcheck-2026-10-21/33-two-nav-wide.png)。

### 8.3 F03：根页面宿主与栈状态仍不正确

**设备已复现的样本**：默认四项导航，窄屏点击消息，游客页显示“我的私信”；将窗口改为 2400×1800。宽屏左栏显示社区帖子列表、rail 选中社区，右侧详情栏却显示消息根。消息内容没有消失，而是迁到了错误宿主，因此不能写成“消息完全丢失”或“消息路由不存在而崩溃”。

对应证据：[窄屏消息](finalcheck-2026-10-21/04-messages-compact.png)、[扩宽后错误宿主](finalcheck-2026-10-21/06-messages-wide-confirmed.png)，同时保存同名 XML 供核对节点位置与选择状态。

原因位于 `ui/navigation/PaneMigration.kt:15–16、83–90`：根集合只列 `messages`，实际 destination.route 为 `messages?section={section}`，完整模板比较把消息根当作详情。`Routes.kt:173` 的详情图也注册了消息，故导航能成功，却没有同步左栏根目的地。`appSettings` 同样不属于该根集合，而其实际所有权在主栏；应统一按目的地身份和宿主规则分类，而不是继续补零散字符串。

**仍有源码证据的状态缺口**：

- `PaneMigration.kt:88–104` 在另一个 NavController 上 navigate 当前 route，只保存地址与参数；没有迁移完整子栈、VM、草稿或弹层状态。
- `AppContent.kt:84–90` 在主栏切换时依旧 `saveState=false` 清除详情，不能满足根目的地分别保留详情栈的约定。
- `TopicScreen.kt:62` 的页面 UI 使用 remember，`TopicScreenUiState.kt:19–20` 的回复显示/引用及 `TopicReplyDialog.kt:48` 的输入也未跨宿主保存。帖子地址恢复并不足以证明未提交输入保留；本轮游客未运行输入场景。

建议：统一根/详情目的地分类，明确每个根拥有的子栈和状态；从新宿主恢复状态时保持原目的地身份、参数、返回语义及草稿。不要吞掉迁移失败后就将 pending 清为完成。

复验应包含：消息的私信/通知两分区窄宽互转，帖子→用户等多级详情返回，根之间切换后恢复详情，以及已输入未提交回复的布局切换。单个帖子地址恢复的通过证据予以保留，不替代这些条件。

### 8.4 F05：surfaceOpacity=1 时仍跳过正文和表单保护

位置：`ui/theme/ThemeProvider.kt:122–130、147`；`ui/screens/TopicPostCard.kt:71、77`；`ui/screens/SettingsPage.kt:49、53`。

存在有效背景图时，ThemeProvider 仍将 background 设为透明。主帖和设置页却只在 `LocalSurfaceOpacity < 0.999f` 时画保护面；surfaceOpacity 为 1 时条件为 false，正文与设置空白/说明区域直接落在图片上。1 也是 SessionThemeState 的默认值，因此这不是不可达边界。将 surface 角色改为不透明，只有实际绘制该角色时才提供保护。

原颜色模型反例仍适用于这个遗漏配置：静态浅色背景 #FAF8FF、黑图强度 0.65、正文 #1A1B21，直接叠图后的对比约 2.38:1。这里仍是静态合成证据，本轮没有导入背景图或测量截图像素。

建议使用明确的背景模式/保护策略控制正文与表单保护，不用透明度数值推断是否存在图片；高亮回复的半透明分支也应叠在保护面之上。复验 surfaceOpacity=1 及小于 1、浅深主题、黑白和高细节图，核对最终合成背景。

### 8.5 F08（新增 P2）：首载中切换消息分区会再次请求

位置：`ui/screens/MessagesScreen.kt:133–138`；`ui/feature/community/NotificationsViewModel.kt:29–38`；`ui/feature/community/PrivateMessageViewModels.kt:32–37`。

触发：登录后首次打开通知，在请求完成前切到私信，再切回通知。此时 loaded 仍为 false，effect 再次调用 load；通知 VM 没有 busy 去重、任务替换或取消，两个请求可同时返回。较早请求若较晚完成，就能覆盖较新状态，也可能在另一请求仍运行时提前结束 loading。私信侧虽会取消旧 job，但同样会无必要地取消并重启首载。

这是控制流审查结论；本轮游客设备没有执行登录后的慢网样本。LaunchedEffect 的取消不会取消已进入 viewModelScope 的通知请求，因此不能依靠 effect 生命周期消除重入。

建议在 VM 统一区分普通首载与明确刷新，并约束在途任务；或至少在宿主首载条件中同时检查 !loaded 与 !loading。验证慢网下分区往返仍只保留一次首载，显式刷新可按约定替换任务，空结果和失败仍可正常结束及重试。

### 8.6 F09（新增 P1）：默认透明列表背景被改成不透明黑色

位置：`ui/patterns/topic/TopicCardView.kt:68、76`；`ui/patterns/topic/TopicCardColors.kt:23`；`ui/theme/ThemeProvider.kt:147`。

未设置卡片背景覆盖时，bgOverride 为 null，背景回退为 Color.Transparent；代码随后无条件调用 `.copy(alpha = LocalSurfaceOpacity.current)`。默认无背景图时 LocalSurfaceOpacity 为 1，而透明色的 RGB 为 0，因此最终得到不透明黑色，不再是透明背景。列表标题及元数据仍按浅色主题使用深色前景，造成黑底深色字，正常帖子标题难以辨认。

**设备及设置证据**：本轮前后偏好中均没有卡片颜色、主题颜色、背景图或表面透明度覆盖；默认浅色界面的社区列表持续显示黑底深色文字。见 [恢复后的首页](finalcheck-2026-10-21/48-restored-home.png)、[宽屏列表](finalcheck-2026-10-21/06-messages-wide-confirmed.png)。本轮没有为这个场景修改主题设置，黑底不是验收时导入的测试图片。

建议：没有背景覆盖时保留透明语义，或者明确采用设计要求的主题表面；不要对透明回退色覆盖 alpha。只有确实存在的装饰背景才按约定合成透明度。

复验：无背景图、无卡片颜色覆盖时，浅深主题列表底色与文字配对正确；再分别验证自定义卡片背景和背景图，避免修正默认值时破坏已有显式偏好。

### 8.7 未宣称通过的范围

本次没有登录凭据，消息数据、双账号、发帖/回复提交仍待运行验收。旧 `directMessages` 兼容页仍使用无 uid key 的 VM，effect 仅依赖 VM/loggedIn（DirectMessagesScreen.kt:51–55）；后续双账号样本应覆盖 loggedIn 保持 true、uid 变化这一分支，不能仅凭消息根修正宣称所有私信入口已隔离。

背景图、高对比、大字号、TalkBack、真实铰链与完整 WebView/媒体矩阵也未在本轮执行。以上边界不作为虚构的代码缺陷；当前不通过的依据是 8.3—8.6 的具体实现缺口。结束状态证据：[恢复后的首页](finalcheck-2026-10-21/48-restored-home.png)。本地 docs 仍受仓库 ignore 规则影响，本记录和证据保存在当前工作区。
