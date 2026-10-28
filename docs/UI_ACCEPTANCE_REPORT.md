# UI 公共层验收记录

初验日期：2026-10-20；最近复验：2026-10-21（Asia/Shanghai）  
对象：[UI 分层与公共组件开发规范](UI_DEVELOPMENT_GUIDE.md) 及当前工作区 A—K 批次实现  
结论：**两项既有 P2 修复已关闭。H—K 的应用构建、结构检查和代表页面真机抽查通过；发现一项本地测试引用迁移遗漏，测试源码编译未通过。** 当前两份相关测试已暂存删除、仍物理存在且被忽略，此问题影响本地测试编译，不能据此判定安装包运行失败。完整业务与设备矩阵尚未完成。最新证据与范围见第 7 节。

## 1. 初验证据（2026-10-20）

| 检查 | 本次结果 |
| --- | --- |
| `:app:compileDebugKotlin` | 成功 |
| `:app:assembleDebug` | 成功 |
| UI 改动 `git diff --check` | 通过 |
| `ui/components`、`ui/theme` 源码行数 | 14 个文件，最大 139 行，全部不超过 250 行 |
| 基础公共层依赖 | 未发现 Session、ViewModel、NavController、repository 或 data 依赖 |
| 公共入口归属 | 核对了新入口定义与旧根包全限定引用，没有发现并存的同名旧实现 |
| ARM 真机安装 | 连接的 REP-AN00 支持 arm64-v8a，本次构建安装成功；未清除应用数据 |
| APK 一致性 | 本地 armv8a APK 与设备 base.apk 的 SHA-256 相同，见下方 |
| 真机基础导航 | 启动 → 我的 → 设置 → 阅读与交互 → 返回设置成功 |
| 深色界面 | 已观察设置菜单、开关行、说明文字和返回栏，截图见附件 |
| 无障碍节点 | 实机 XML 证实开关与说明行被拆成两个可操作节点，见问题 1 |

本次验收 APK SHA-256：

```text
975fd2e94891f7eb574efd3fb3c54c3380391998e86be9b00c75acfc3dc94771
```

验收中未提交源站内容，未操作登录或删除数据，未修改设置值；结束后设备返回桌面。本次没有运行单元测试，也没有将历史测试失败当作本轮新增失败。

## 2. 初验未满足项（两项源码问题已在 2026-10-21 复验关闭）

### 1. 开关行没有形成完整的开关语义（P2）

位置：`app/src/main/java/sb/linux/client/ui/components/input/AppSwitchRow.kt:34–35`；父行点击实现在 `components/layout/AppMenuComponents.kt:56`。

`AppSwitchRow` 把可交互的 `Switch` 放入可点击的 `AppMenuRow`。实机节点中，说明行是 `clickable=true`、`checkable=false`；内部 Switch 是 `checkable=true`，但文本及 content-desc 为空，并被 UIAutomator 标记为 `NAF=true`。辅助技术无法在同一个操作节点取得设置名称和开关状态，不满足规范 §5.2.7。

建议：整行通过 `toggleable` 提供 `Role.Switch`、checked、enabled 和回调，内部 Switch 使用 `onCheckedChange=null`；共享视觉排版仍从同一入口维护。不能只给现有父行再叠加一个点击处理器。

复验要求：每个设置只有一个包含名称、状态及切换动作的可访问操作节点；点击整行或开关区域均只切换一次；禁用时不可操作。当前证据不支持“普通触摸必然双触发”的结论。

### 2. 设置页间距未接入公共来源（P2）

位置：`app/src/main/java/sb/linux/client/ui/screens/SettingsPage.kt:29–30`；公共字段位于 `ui/theme/AppSpacing.kt:22–26`。

公共设置页仍写死 `16.dp`、`12.dp` 和分组间距 `16.dp`。已声明的 `section`、`pageHorizontal`、`pageVertical` 没有实际消费方。未来调整公共配置时，该公共外壳不会随之变化，尚未实现本次要求的单一视觉来源。

建议：`SettingsPage` 读取 `LocalAppSpacing.current` 的相应字段，保持当前默认视觉值。其它具有明确不同语义的业务页面不应机械替换数值。

复验要求：三个字段具有真实消费方；调整统一配置能作用到设置页；默认值下间距保持不变，相关编译通过。

### 3. 文档中的实现状态相互矛盾

§4.1 曾将 `AppPageScaffold`、`AppSwitchRow` 继续列为“未实现”，与实际源码和后续批次记录冲突。已在本次验收中校正。批次 E 中“没有空 token”的表述也已根据问题 2 修正。本次只修正文档状态，未修复上述两项源码问题。

## 3. 验收边界与后续复验

初验只取得手机深色配置下的代表页面证据。2026-10-21 已补齐两项修复的定向复验，以及设置/阅读页的浅色、深色与代表禁用状态。仍未完成用户字体/字号组合、平板、背景图/透明度、全部空/错误/忙碌状态，以及分页、摘要、确认框等完整交互矩阵。基础导航成功不能代替这些项目。

两项初验问题已完成针对性复验，不再列为未修复问题。整体 UI 最终验收仍需按规范 §9.2 补齐剩余设备矩阵；已取得的有效证据可以保留，后续改动影响相应证据时复查受影响部分。

## 4. 实机附件

- [深色阅读设置截图](ui-acceptance/2026-10-20/browse-dark.png)
- [开关无障碍树](ui-acceptance/2026-10-20/browse-semantics.xml)
- [设置页节点](ui-acceptance/2026-10-20/settings.xml)
- [返回设置后的节点](ui-acceptance/2026-10-20/returned-settings.xml)

`docs/` 当前被 Git 忽略，本报告和附件仅保存在工作区，没有变更忽略规则。

## 5. 定向复验（2026-10-21）

### 范围与构建

复验上次两项 P2 的源码修复、相关公共组件和实际调用方。未改动生产源码。本次执行 `:app:compileDebugKotlin :app:assembleDebug` 成功，36 个任务为 up-to-date；UI 差异格式检查通过；14 个公共组件/主题文件及 `SettingsPage.kt` 共 15 个文件均不超过 250 行，最大 139 行；基础公共层未发现新增导航、Session 或数据层依赖。

连接的 REP-AN00 支持 arm64-v8a，分辨率 1224×2700，密度 520 dpi，系统字体缩放为 1.0。使用保留数据的安装方式安装本次 armv8a 构建。设备 base.apk 与本地 APK 的 SHA-256 相同：

```text
7cb79dc380870ff86c439c246e640d9ac0da17c45a1a185f9d56ad54055a9fb2
```

### 复验结果

| 项目 | 证据与结果 |
| --- | --- |
| 问题 1：统一开关语义 | 源码为整行 `toggleable` + `Role.Switch`；内部 Switch 无回调并清空自身语义；AppMenuRow 不再追加 clickable。实机前 7 个完整可见设置行分别只有一个可操作开关节点，标题/说明为其非交互文本后代，状态位于该父节点；没有嵌套的可操作开关子节点。通过 |
| 行区域点击 | “启动时不刷新首页”初值 false，点击行的文字区域后为 true。通过 |
| 开关区域点击 | 在同一设置的开关视觉区域点击后恢复 false，原值已还原。结合单一动作来源的源码检查，通过 |
| 禁用状态 | AI 总结未开启时，“打开帖子自动总结”为 enabled=false、checked=false。分别点击该行文字和开关区域后，状态保持不变。通过 |
| 问题 2：公共间距 | `SettingsPage` 已读取 `LocalAppSpacing.current` 的 pageHorizontal、pageVertical、section，三个字段均有真实消费方；默认值仍为 16/12/16。引用及源码检查通过；代表页面默认展示通过。没有临时修改源码来测试另一组间距数值 |
| 返回与外壳 | 我的 → 设置 → 阅读/AI/外观页面及返回设置正常。通过 |
| 浅色与深色 | 检查设置菜单与阅读开关行，未见本次修复导致的文本截断、控件遮挡或状态颜色混淆。通过；仅代表页面，不覆盖全应用 |

UIAutomator XML 中父节点的 `text` 可能为空，标题和说明由其非交互子节点提供。验收以同一操作语义子树是否包含名称与状态、是否存在独立的无名操作子节点为准，不能只按父节点 `text=""` 判定失败。

### 恢复与限制

测试中的开关已恢复原值。一次导航过渡中的点击误中了板块开关，随后依据初始快照恢复；最后逐项核对首页/板块/评论无限滚动、启动缓存、新帖折叠、侧边栏双列和在线用户七项均与初始值一致。主题在浅色、深色检查后恢复为原来的“跟随系统”（ordinal 0），应用留在设置页。未执行登录、发帖、AI 请求或重置操作。

本次没有运行单元测试。针对性证据足以关闭两项已报告问题；不据此声称完整 UI 重构或全部设备矩阵验收完成。

### 最新附件

- [机器核对结果](ui-acceptance/2026-10-21/results.json)
- [修复后的开关语义树](ui-acceptance/2026-10-21/browse-before.xml)
- [点击行后](ui-acceptance/2026-10-21/after-row-tap.xml)、[点击开关区域后](ui-acceptance/2026-10-21/after-switch-tap.xml)
- [禁用开关初始节点](ui-acceptance/2026-10-21/ai-disabled-before.xml)、[点击禁用行后](ui-acceptance/2026-10-21/ai-disabled-row-tap.xml)、[点击禁用开关区域后](ui-acceptance/2026-10-21/ai-disabled-switch-tap.xml)
- [浅色阅读页](ui-acceptance/2026-10-21/browse-light.png)、[深色阅读页](ui-acceptance/2026-10-21/browse-dark.png)、[禁用展示](ui-acceptance/2026-10-21/ai-disabled.png)
- [设置页浅色](ui-acceptance/2026-10-21/settings-light.png)、[设置页深色](ui-acceptance/2026-10-21/settings-dark.png)
- [浏览偏好恢复记录](ui-acceptance/2026-10-21/browse-final-restored.xml)、[主题恢复记录](ui-acceptance/2026-10-21/theme-restored.xml)、[最终设置页](ui-acceptance/2026-10-21/settings-final.xml)

## 6. 批次 G 外壳迁移验收（2026-10-21）

### 结论与范围

本批涉及八个页面源码文件与 `AppPageScaffold.kt`。静态审查、编译和打包通过，未发现本批新增的代码问题；四个可访问页面完成真机浅色、深色与返回检查。邀请、举报、用户名中转页仅完成静态审查，未取得本次真机证据；不能将八个源码文件全部记为实机通过。

**签到需求校正**：用户明确签到由自动流程完成，不需要手动入口。`CheckinScreen` 是当前未接线的旧页面，缺少手动路由不构成缺陷，也不要求为本次验收新增入口。本批不改变签到业务流程，当前游客会话没有验证登录后的自动签到结果。

### 构建与结构

- 执行 `:app:compileDebugKotlin :app:assembleDebug` 成功，36 个任务为 up-to-date。
- 八个页面源码文件与外壳共九个文件均不超过 250 行；最大为 `InviteCenterScreen.kt` 的 197 行。
- `git diff --check` 通过；基础组件未发现新增 Session、导航控制器、ViewModel 或数据层依赖。
- 八处迁移均使用公共外壳；内容 padding、各自滚动容器与原回调保留，举报页返回标签已改为 `return@AppPageScaffold`。
- 外壳标题的单行省略已集中实现；真实长用户名场景本次未完成设备验证。

本地 armv8a APK 已保留数据安装，设备 base.apk 与本地产物 SHA-256 一致：

```text
a6ce8a8fd55222391340f5a02f66467036257b27686c58a4984904bd9096ad5e
```

### 实机核对

设备为 REP-AN00，1224×2700、520 dpi；本次以游客状态检查。对照源码和截图检查顶栏主题角色、返回图标与内容边界；使用 `material-3` 技能中的 Compose 顶栏和 inset 指引作为审查参考，不将有限样本转换为全应用的合规分数。

| 页面或场景 | 本次结果 |
| --- | --- |
| 关于应用 | 浅色、深色标题及返回正常 |
| 阅读与交互 | 浅色、深色标题及返回正常；系统字体 1.3 倍下，标题、说明和开关未见遮挡或裁切 |
| 应用与账号 | 浅色、深色标题及返回正常；未执行更新、重置或账号修改 |
| 已导出的帖子 | 浅色、深色标题、返回及右侧刷新动作正常；当前空列表使用公共空态，刷新后仍正确展示空态 |
| 设置页 | 1.3 倍系统字体下分类说明可换行，返回后页面正常 |
| 邀请中心、举报、用户名中转 | 仅静态审查，本次游客路径未完成设备验证 |
| 每日签到旧页面 | 不计为手动入口验收项，按用户明确的自动签到需求处理 |

系统字体已从测试值 1.3 恢复至原值 1.0；主题从深色测试恢复为原“跟随系统”（ordinal 0），应用留在设置页。本批验收未修改生产源码、清除用户数据、登录、发帖、提交举报或主动签到，也未运行业务单元测试。

### 尚余工作

本节记录批次 G 当时的剩余工作：十个同形态二级页、特殊外壳及用户卡/正文目录整理。H—K 已继续完成其中的普通外壳与目录迁移；当前结果以第 7 节为准。

最终设备矩阵仍未覆盖平板、自定义字体、背景图/透明度和所有业务状态组合；本轮新增的大字体检查只代表系统字体 1.3 倍下的设置/阅读页，不能替代其余字体与页面组合。

### 本批附件

- [机器检查结果](ui-acceptance/2026-10-21-batch-g/results.json)
- [关于页浅色](ui-acceptance/2026-10-21-batch-g/about-light.png)、[深色](ui-acceptance/2026-10-21-batch-g/about-dark.png)
- [阅读页浅色](ui-acceptance/2026-10-21-batch-g/browse-light.png)、[深色](ui-acceptance/2026-10-21-batch-g/browse-dark.png)、[1.3 倍系统字体](ui-acceptance/2026-10-21-batch-g/browse-large-font.png)
- [应用与账号浅色](ui-acceptance/2026-10-21-batch-g/general-light.png)、[深色](ui-acceptance/2026-10-21-batch-g/general-dark.png)
- [导出页浅色](ui-acceptance/2026-10-21-batch-g/exports-light.png)、[深色](ui-acceptance/2026-10-21-batch-g/exports-dark.png)
- [大字体设置页](ui-acceptance/2026-10-21-batch-g/settings-large-font.png)、[字体恢复后](ui-acceptance/2026-10-21-batch-g/settings-font-restored.png)

## 7. 批次 H—K 综合验收（2026-10-21）

### 结论与新增问题

应用构建和下表所列真机抽查通过，未观察到相应页面的新显示异常。主干迁移已落地：27 个页面源码文件消费公共外壳，用户/帖子模式、正文渲染、验证弹窗及主题基础设施已按职责归位。本轮只审查和记录，未修改生产源码或测试源码。

**P2：本地两份 UI 测试未同步新的 content 包引用。**

- `app/src/test/java/sb/linux/client/ui/MarkdownParserTest.kt:10`：测试仍处于 `sb.linux.client.ui` 包且未导入 `content` 中的声明，`parseMarkdown`、`MdBlock` 无法解析。
- `app/src/test/java/sb/linux/client/ui/EmbeddedVideoParserTest.kt:17`、`:23`：同样缺少新引用，`browserCompatibleUserAgent`、`parseHtmlToBlocks`、`EmbeddedVideoPlatform` 无法解析。

执行 `:app:compileDebugUnitTestKotlin` 实际失败，日志证实以上新增错误；没有执行测试用例。已有抽卡相关测试引用错误也仍存在，两者需分别归因。此前批次 E 中“测试失败与本次 UI 迁移无关”的历史结论不能套用到当前全部错误。

两份文件在 Git 中已暂存删除，物理副本仍在工作区，且 `src/test/` 被 `.gitignore` 忽略；忽略规则不会阻止 Gradle 编译磁盘上的文件。因此这是一项本地验证缺口，不是当前已安装 APK 的构建或运行失败证据。若保留这些本地测试，应同步 package 或导入，保留原断言；本轮不擅自恢复暂存删除或删除物理副本。此项不阻止记录已通过的应用和实机检查。

### 构建与结构证据

| 检查 | 本次结果 |
| --- | --- |
| `:app:compileDebugKotlin :app:assembleDebug` | 成功，36 个任务 up-to-date |
| `:app:compileDebugUnitTestKotlin` | 失败，包含上述两份 UI 测试和已有抽卡测试引用错误；未运行测试 |
| UI 差异格式检查 | `git -c core.safecrlf=false diff --check -- app/src/main/java/sb/linux/client/ui` 通过 |
| 文件规模 | UI 下 309 个 Kotlin 文件，最大 250 个物理行，全部符合上限 |
| 公共外壳 | 27 个消费方源码文件；剩余独立 Scaffold 位于 5 个文件，含聊天输入栏、帖子搜索/回复栏、Web 通道和 tab 根页面，不能仅按剩余数量判定重复实现 |
| 基础公共层依赖 | components/theme 未发现 Session、NavController、ViewModel、repository 或 data 层依赖 |
| 搬迁行为核对 | 去除包声明、导入和空行后，21 个 content 文件中 20 个与 HEAD 原位置正文一致；ImagePager 的差异包含前批已引入的公共确认框。该比较支持保留行为，不能代替全部媒体运行验证 |
| 根目录归属 | 剩余 AppUpdatePromptDialog、BadgeComponents、UiCompositionLocals 三个文件；没有实际候选的 model 目录不需为空而建 |

安装到 REP-AN00 的 armv8a 包与本地产物 SHA-256 一致：

```text
67375e668bd7806c3b4e43c9192d41f38b5f10ade7f38128aff12d66b5334c87
```

### 真机抽查

设备为 REP-AN00，1224×2700、520 dpi、系统字体缩放 1.0，游客会话，当前浅色显示。使用保留应用数据的安装方式。本轮未调整设置值。

| 页面或场景 | 观察结果与边界 |
| --- | --- |
| 私信、淘帖 tab 根 | 条件返回图标正确隐藏；保留底部 tab 导航 |
| 用户榜单 | 没有顶栏返回图标；系统返回成功回到“我的” |
| AI、网络与链接、DoH、外观与布局 | 标题、返回和内容区域正常；DoH 添加动作可见，未执行添加、测速或修改配置 |
| 数据清理、本地备份 | 标题及返回正常；仅查看，没有执行清理或备份 |
| 浏览历史、我的收藏 | 历史列表和收藏游客登录门禁可见，返回正常；不据此证明已登录收藏流程 |
| 首页及用户主页帖子列表 | 卡片、头像、标签可见；未专门配置卡片颜色覆盖 |
| 原生帖子正文 | 正文段落、行内链接、头像、称号、签名、回复登录提示可见，滚动后无新遮挡；样本未包含表格、代码块或嵌入视频 |
| 用户弹层 → 主页 | 弹层资料加载完成，进入主页后用户卡和帖子列表正常；返回图标可见，系统返回正常 |

最近 200 条日志的 `AndroidRuntime:E` 过滤结果为空，仅代表这次采样没有记录该类错误。设备最终回到首页；字体缩放仍为 1.0、密度仍为 520 dpi。未登录、发帖、举报、主动签到或清除数据。阅读公开帖子会产生应用正常的浏览记录。

### 未覆盖范围

本轮没有重新覆盖全部浅深色组合、平板、自定义字体、背景图与透明度、所有空/错误/忙碌状态；也未验证完整表格/代码/图片查看及嵌入视频、源站验证弹窗、登录后的收藏/通知/集合管理与自动签到。前轮针对开关语义、浅深色及 1.3 倍字体的有效证据仍保留，但不将它们表述为本轮所有新页面均已验证。

签到按用户明确的自动流程处理，缺少手动入口不作为问题。此报告支持当前构建与列明场景的验收，不声称完整业务矩阵已通过。

### 附件

- [机器检查记录](ui-acceptance/2026-10-21-batches-h-k/results.json)、[测试源码编译完整日志](ui-acceptance/2026-10-21-batches-h-k/test-source-compile.log)、[搬迁正文比较](ui-acceptance/2026-10-21-batches-h-k/moved-body-comparison.json)
- [私信根页面](ui-acceptance/2026-10-21-batches-h-k/messages-root.png)、[淘帖根页面](ui-acceptance/2026-10-21-batches-h-k/collections-root.png)、[榜单](ui-acceptance/2026-10-21-batches-h-k/leaderboard.png)
- [网络设置](ui-acceptance/2026-10-21-batches-h-k/network.png)、[DoH](ui-acceptance/2026-10-21-batches-h-k/doh.png)、[外观设置](ui-acceptance/2026-10-21-batches-h-k/theme.png)
- [清理页](ui-acceptance/2026-10-21-batches-h-k/cleanup.png)、[备份页](ui-acceptance/2026-10-21-batches-h-k/backup.png)、[历史页](ui-acceptance/2026-10-21-batches-h-k/history.png)、[游客收藏门禁](ui-acceptance/2026-10-21-batches-h-k/favorites.png)
- [正文滚动后](ui-acceptance/2026-10-21-batches-h-k/topic-body-loaded.png)、[用户弹层](ui-acceptance/2026-10-21-batches-h-k/user-summary-loaded.png)、[用户主页](ui-acceptance/2026-10-21-batches-h-k/user-profile-loaded.png)

对应 XML 与返回后快照同存附件目录。`docs/` 仍被 Git 忽略，报告和附件仅保存在工作区。
