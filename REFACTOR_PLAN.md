# LinuxSB 重构执行计划

> 执行分支 `codex/refactor-layered-architecture`，基点 `main@1c47027`。
> 屏蔽词垂直切片已固化为 `ecd9738`；后续按阶段推进，不复用旧 `refactor/layered-architecture` 分支的历史实现。
> 目标结构见 `ARCHITECTURE.md`，扩展点见 `EXTENSION_POINTS.md`。规范依据仓库根 `agents.md`。

---

## 0. 总则

### 三条不可违背的原则

**行为保真优先于结构纯洁。** `agents.md` §6 要求"拆分重构后必须保证原有全部业务逻辑行为不变"。任何一步若在保真与结构之间冲突，保真优先，把结构妥协记入本文 §待决事项，不要自行降低保真标准。

**先建层，再迁移。** 当前 UI 仍有 82 处 `session.client.*` 与 78 处 `HtmlParser.*` 直连（统计见 `ARCHITECTURE.md` §2.1）。若先拆屏幕再建 repository，屏幕要改两遍。顺序锁定为：基线固化 → 工具链 → 平移 → 机械拆分 → 数据层 → repository → 屏幕 → TopicScreen。

**每步可独立回退。** 每个提交自身编译通过、APK 可装、功能可用。禁止"下一个提交才能编译"的中间态。

### 里程碑总览

| 阶段 | 主要产物 | 前置条件 | 退出条件 | 风险 |
| :--- | :--- | :--- | :--- | :--- |
| 0–1 | 新分支、当前基线、屏蔽词垂直切片、四份文档 | 无 | 单测与 Debug 构建通过；变更可独立审查 | 低 |
| 2 | version catalog、`.editorconfig`、detekt、ktlint、当前基线快照 | 阶段 1 已固化 | 新代码受规则约束，18 个存量超限项有明确 baseline | 低 |
| 3 | `common/`、`model/`、`util/` 归位 | 工具链可用 | 只发生移动/导入变化，声明清单无意外差异 | 低 |
| 4 | 大型 UI 聚合文件按业务边界拆分 | 公共模型已归位 | 独立文件均 ≤400 行，导航行为不变 | 中 |
| 5 | parser、client、settings、network 分包 | 机械拆分稳定 | 数据层不依赖 UI，解析快照一致 | 高 |
| 6 | 按域 repository + `LsbResult` | 数据接缝稳定 | repository 测试覆盖成功、源站错误和解析错误 | 中 |
| 7 | 屏幕 ViewModel 化并移除直接网络/解析调用 | repository 可用 | `ui/` 中 `session.client.*`/`HtmlParser.*` 清零 | 高 |
| 8 | `TopicScreen`、`Components` 拆分 | 其他屏幕模式已验证 | 每文件 ≤400 行，楼层状态机与滚动恢复回归通过 | 极高 |
| 9 | `Session` 消解、依赖校验、Release 验证、文档回填 | 前述阶段完成 | detekt baseline 清空，Release 构建通过 | 中 |

文中行号是规划扫描时的定位提示，不作为迁移接口；执行每一步前必须按符号名重新定位并记录实际文件，避免后续提交导致行号漂移。

### 每步的完成定义

一步只有全部满足才算完成：

1. `./gradlew :app:assembleDebug` 通过。
2. 声明清单与基线一致——无声明意外丢失：
   ```sh
   # 生成当前清单并与 %TEMP%/lsb-refactor-baseline/decls-before.txt 比对
   # 预期差异只有本步有意的新增/重命名，逐条能说清
   ```
3. detekt 通过（阶段 2 之后）。
4. 本步涉及的功能在真机手测通过（清单见 §手工验收）。
5. 提交信息说明动机与影响面，按 `agents.md` §6"按模块拆分多次提交"。

### 提交规范

沿用仓库现有 Conventional Commits 前缀。重构提交统一：

```
refactor(<域>): <一句话动作>

<为什么拆：引用 agents.md 条款或具体问题>
<影响面：涉及文件数、是否有行为变化>
<验证：构建/手测结论>
```

纯文件移动**必须单独成一个提交**，不与内容修改混在一起——否则 `git log --follow` 追不到历史，review 也看不出哪些是真改动。

---

## 1. 阶段 0：分支与基线（已完成）

当前状态：

- 已从 `main@1c47027` 创建 `codex/refactor-layered-architecture`，没有覆盖或清理工作区。
- 屏蔽词专项已作为首个垂直切片提交（`ecd9738`）。
- 基线源集为 74 个 Kotlin 文件、28,120 行；15 个文件超过 400 行。当前执行扫描（包含已拆出的新文件）为 85 个 Kotlin 文件、29,715 行，仍有 10 个文件超过 400 行。
- 当前最大文件为 `TopicScreen.kt` 4,371 行；其次为 `AppSettingsScreen.kt` 3,560 行、`MiscScreens.kt` 2,505 行。行数使用 `Get-ChildItem app/src/main/java -Recurse -Filter *.kt` + `Get-Content` 统计，后续以同一口径复测。
- `:app:testDebugUnitTest :app:assembleDebug` 已通过。
- `%TEMP%/lsb-refactor-baseline/` 是旧基点 `35d79dc` 的历史快照；阶段 2 重新生成当前基线。

## 阶段 1：固化屏蔽词垂直切片（已完成）

该专项已经按 `api → repository → service → Session/UI` 的单向依赖实现并提交为 `ecd9738`，是后续业务域迁移的参考样板：

| 步 | 内容 | 验收 | 建议提交 |
| :--- | :--- | :--- | :--- |
| 1.1 | 复核源站契约、账号隔离、缓存与 pending 重试 | `KeywordFilterTest` 全绿，登录切换不串数据 | `feat(filter): align keyword filter with source` |
| 1.2 | 固化列表页统一过滤入口，禁止 UI 复制规则 | 首页与版块列表行为符合页面作用域 | 随 1.1 |
| 1.3 | 提交架构、计划、扩展点与 API 变更文档 | 文档中的分支、指标、状态与代码一致 | `docs: add layered refactor plan` |

阶段 1 不继续扩大范围：不借机拆 `Session`，不把其他页面塞进现有 repository，也不改源站 HTTP 路径。这样首个提交可独立审查和回退。

---

## 2. 阶段 2：工具链先行

先立规则，后面每步才有机械校验。**这一步不动任何业务代码。**

| 步 | 内容 | 提交 |
| :--- | :--- | :--- |
| 2.1 | `gradle/libs.versions.toml`，把 `app/build.gradle.kts:104-155` 的硬编码坐标迁入。注意 `build.gradle.kts:22-23` 的 versionCode/versionName 行尾被 `.github/workflows/release.yml:132-133` 的 sed 匹配，**不得改动这两行的格式** | `build: 引入 version catalog` |
| 2.2 | `.editorconfig`：缩进 4、行宽 120、LF（与 `.gitattributes` 的 `* text=auto` 一致） | `build: 添加 editorconfig` |
| 2.3 | detekt 接入，规则见下 | `build: 接入 detekt 与文件长度红线` |
| 2.4 | ktlint 接入，官方 code style（`gradle.properties` 已有 `kotlin.code.style=official`） | `build: 接入 ktlint` |

detekt 关键配置：

```yaml
complexity:
  LongMethod:          { threshold: 60 }
  LongParameterList:   { functionThreshold: 8 }   # PostCard 现 28 个参数
  TooManyFunctions:    { thresholdInClasses: 20 } # Session 现 86 个
style:
  MaxLineLength:       { maxLineLength: 120 }
  ForbiddenImport:                                # ARCHITECTURE.md §3 的四条依赖规则
    imports:
      - value: 'androidx.compose.**'
        reason: 'data 层不得依赖 Compose'
      - value: 'sb.linux.client.ui.**'
        reason: 'data 层不得反向依赖 UI'
```

**关键决定：`MaxFileLength` 的引入方式。** 400 行红线若一上线就 `failFast`，18 个现存文件立即全红，detekt 变成噪音，没人会看。做法是：

1. 阶段 2 先生成 `detekt-baseline.xml`，把 18 个现存超限文件全部纳入 baseline，新代码即时受约束。
2. 每个拆分阶段完成后，从 baseline 中**移除**该阶段处理掉的条目。
3. 阶段 9 收尾时 baseline 必须为空——这就是重构完成的机械判据，不靠人工数行数。

`ForbiddenImport` 只能挡 `import`，**挡不住全限定名**。`Session.kt:259` 的 `sb.linux.client.ui.CardColorOverrides.map = ...` 正是用全限定名绕过 import 的实例。补一条 CI grep 校验：

```sh
# data 层不得出现 ui 层全限定引用
grep -rn 'sb\.linux\.client\.ui\.' app/src/main/java/sb/linux/client/data/ && exit 1
```

---

## 3. 阶段 3：零风险平移

只移动代码、只改 `package` 与 `import`，**不改一行逻辑**。这一步收益是让后续每一步的 diff 都变小。

已完成：3.1 `LsbException` 已迁移至 `common/error/LsbException.kt`（提交 `826761f`）；3.3 的 HTML 转义已集中至 `util/HtmlEscape.kt`（提交 `53164db`）；3.4 的 `CommentFavorite` 已迁移至 `model/local/CommentFavorite.kt`（提交 `bc837b0`）。三步均通过单元测试与 Debug 构建。剩余模型迁移仍按下表执行。

| 步 | 内容 | 来源 | 风险 |
| :--- | :--- | :--- | :--- |
| 3.1 | 建 `common/error/LsbException.kt` | `LsbClient.kt:30` 迁出（被 `AiClient`、`ImageHostClient` 共用） | 极低 |
| 3.2 | 建 `common/result/LsbResult.kt` | 纯新增，此时无人使用 | 无 |
| 3.3 | 建 `util/`：`HtmlEscape.kt`、`TimeFormat.kt`、`IntentExt.kt` | 消除 `ARCHITECTURE.md` §5 表中的确切重复 | 低 |
| 3.4 | `model/` 建包，`data/Model.kt` 的 49 个 data class 按域拆入子包 | `Model.kt` 零逻辑，纯搬迁 | 低 |
| 3.5 | 散落的 21 个模型收回 `model/`（清单见 `ARCHITECTURE.md` §4） | 7 个文件 | 中——`HtmlParser.CheckinInfo` 被 `Session.kt:1038/:1041/:1060` 引用，改动点需全量替换 |
| 3.6 | `data/SettingsSearchIndex.kt` → `ui/screen/settings/`；`data/Endpoints.kt` 的 `TimeFmt` → `util/TimeFormat.kt` | 错位的表现逻辑 | 低 |

3.3 的去重要点：`esc()` 两份完全相同（`HtmlParser.kt:44`、`TopicExport.kt:116`），直接合并。

`abs`/`absUrl` 三份**不要合并**——已逐个读过，语义分层而非重复：`Endpoints.abs:9` 是基础（非 http 就拼 BASE）；`HtmlParser.absUrl:197` 多处理协议相对 URL（`//host` → `https://host`）后兜底调 `Endpoints.abs`；`Components.abs:1947` 收 `Element` 参数（先取 Jsoup `abs:src`、回退 `src`）后同样兜底调 `Endpoints.abs`。后两者已在复用第一个，归位时按调用方就近放置即可。

每小步一个提交。3.4 与 3.5 因涉及大量 import 变更，按域再分：topic / user / forum / gacha / message / settings 各一个提交。

---

## 4. 阶段 4：机械文件拆分

优先处理聚合型 UI 文件——其中多个屏幕之间**没有共享局部状态**，适合先做纯移动并建立后续拆分范式。

### 4.1 `MiscScreens.kt`（2,432 行 → 16 个屏幕分 6 个包；Favorites 已拆出）

按实际功能域拆，不是按行数切：

| 目标包 | 内容（行号为现位置） | 行数 |
| :--- | :--- | ---: |
| `ui/screen/web/` | `WebScreen:71`、`ExportedHtmlScreen:146` | 157 |
| `ui/screen/gacha/` | `GachaProfileScreen:670`、`GachaCenterScreen:767`、`GachaMarketScreen:896`、`GachaOperationScreen:1112`、`GachaDynamicForm:1291`、`PendingGachaSubmit:1104`、`optionText:1283` | 813 |
| `ui/screen/message/` | `DirectMessagesScreen:1820`、`ChatScreen:1959`、`ChatBubble:2100`、`NotificationsScreen:1644` | 456 |
| `ui/screen/account/` | `CheckinScreen:228`、`IdentityCenterScreen:361`、`IdentityRequirementCard:457`、`InviteCenterScreen:481` | 442 |
| `ui/screen/local/` | `FootprintScreen:2151`、`FavoritesScreen.kt`（已拆出，337 行） | 485 |
| `ui/screen/board/` | `LeaderboardScreen:1520`、`LeaderTabs:1511`、`TopicCollectionsScreen:1872`、`ReportScreen:2636` | 363 |

拆后仍超 400 行的：`GachaMarketScreen`(208) `GachaDynamicForm`(220) 同包合计 428——`GachaDynamicForm` 是 `internal`、也被 `CollectionDetailScreen.kt` 使用，独立成 `ui/screen/gacha/GachaDynamicForm.kt`。`FavoritesScreen.kt` 当前 337 行，已满足单文件红线。

本轮已先处理称号共享组件的超限风险：`GachaWidgets.kt` 中的新闻条与稀有度分组目录迁移到 `GachaCatalogViews.kt`，登录引导与空态迁移到 `GachaStateViews.kt`；主文件降至 366 行，两个新文件分别 99/70 行，调用方和交互保持不变。后续仍按业务域迁移 `MiscScreens.kt` 中的称号屏幕，不把新文件继续堆回聚合文件。

**发现的死代码**：`CheckinScreen` 与 `IdentityCenterScreen` 在 `MainActivity.kt` 的 44 个路由里**没有对应路由**，`openLinkDirect`（`MainActivity.kt:307-309`）把 `/daily_checkin` 和 `/identity_center` 映射成"暂不可用"toast。这两个屏幕当前不可达。**处理方式：本次重构照原样迁移，不删不接。** 理由：`agents.md` §6 行为保真——接上路由是新增功能，删除是移除代码，两者都改变现状。单独记入 §待决事项。

### 4.2 `AppSettingsScreen.kt`（3,454 行 → 10 个屏幕 + 共享组件；BlockWords、WebDAV 已拆出）

`BlockWordsScreen` 已在阶段 1 抽成独立文件；WebDAV 已在提交 `c6bf9c4` 迁移到 `data/remote/WebDavClient.kt`。本阶段以这两个边界作为设置页后续拆分范例。

个人信息页的 `ProfileHeaderCard` 与按源站字段类型分派的 `FieldInput` 已在提交 `b84de1c` 迁移到 `ProfileSettingsComponents.kt`；`SettingsScreen.kt` 当前 387 行，网络提交、头像选择器和动态表单状态仍由原屏幕编排。

模型聚合文件的称号/抽奖模型已在 `cac543a` 迁移到 `GachaModels.kt`，邀请中心模型迁移到 `AccountModels.kt`；`Model.kt` 当前 385 行，包名保持 `sb.linux.client.data` 不变，因此没有破坏已有调用方。

网络层的 `CronetResult` 已在 `01ed549` 从 `CronetFallback.kt` 移出，单独负责响应体解压、头部归一化与重定向请求映射；拦截器、引擎池和回调桥保持原边界，`CronetFallback.kt` 当前 376 行。

用户主页的资料头部与本人账号操作区已在 `1602b9d` 抽到 `UserProfileComponents.kt`、`UserAccountActions.kt`；`UserScreen.kt` 当前 359 行，列表加载、通知同步、标签页与单双栏退出导航仍由原屏幕编排。

新建/编辑帖子页的源站表单解析已在 `23d4a83` 迁移至 `data/parser/NewTopicParser.kt`（141 行），并新增 `NewTopicParserTest` 覆盖版块、编辑确认、抽奖数组字段和发卡默认值；`NewTopicScreen.kt` 当前 845 行，仅保留编辑状态、Compose 表单和提交编排。

`LsbClient` 内嵌的 Cookie 持久化对象已在 `a32dc77` 迁移到 `data/SessionCookieJar.kt`（80 行）；HTTP 客户端继续负责请求、验证和登录编排，Cookie 内存权威、持久化恢复、WebView 导入及退出清理语义保持不变。

| 目标 | 内容 | 行数 |
| :--- | :--- | ---: |
| `ui/component/settings/` | `GroupLabel:121` `GroupCard:133` `SettingIcon:170` `SwitchRow:184` `SettingMenuRow:1001` `ResetAction:545` `fmtBytes:206`（被 `SettingsScreen.kt`、`DohSettingsScreen.kt` 复用） | 137 |
| `ui/component/colorpicker/` | `ColorPickerDialog:368`(170) `HueGradientSlider:329` `Color.hex:538` | 216 |
| `ui/screen/settings/` 各屏幕 | `AppSettingsScreen` `DataManagementScreen` `UsageStatsScreen` `BrowseSettingsScreen` `AiSettingsScreen` `TransferSettingsScreen` `NetworkSettingsScreen` `GeneralSettingsScreen` `ExportedTopicsScreen` `AboutScreen` | 一屏一文件，均 <300 行；屏蔽词页已独立 |
| `ui/screen/settings/theme/` | `ThemeSettingsScreen:2459`(**620**) + 预览支持件 `:2034-2458`(425) | 见下 |
| `domain/backup/WebDavClient.kt` | `object WebDav:1821` —— **UI 文件里的 HTTP 客户端**，自建 OkHttpClient(`:1822`) | 39 |
| `ui/screen/settings/UpdateCheck.kt` | `currentVersionName:3079` `rememberUpdateChecker:3088` `UpdateUiState:3116` | 48 |

`ThemeSettingsScreen`(620 行) 是单个 `LazyColumn`，内部 10 个独立 section，每段 30–100 行，切分线是现成的：快捷切换`:2618` 页面背景`:2626` 实时预览`:2679` 主题色`:2693` 调色风格`:2752` 取色`:2784` 对比度`:2807` 布局`:2881` 底栏样式`:2894` 字体`:2993`。拆为 `theme/section/*.kt`，主文件仅保留 `LazyColumn` 编排。

预览支持件（`:2034-2458`）自成一组：`PostCardPreview` `ElementSelectorList`(118) `ColorSwatch` `FontSwatch` `DayNightToggle` `PaletteStyleSwatch` `CardElements` → `theme/preview/`。

### 4.3 `MainActivity.kt`（89 行；原 967 行已拆为 Activity 宿主、AppRoot、AppContent、Routes、TabletNavRail）

导航拆分已完成第一步：`MainActivity.kt` 只保留 Activity 生命周期与主题装配；`ui/navigation/` 承载全局 Compose 根、手机/平板布局、路由图和底栏。路由字符串、起始页、平板双栏和转场参数均保持原值。下一步只处理导航文件内部的测试接缝与命名，不再回写 Activity。

| 目标 | 内容 | 行数 |
| :--- | :--- | ---: |
| `MainActivity.kt` | Activity 生命周期与主题装配 | 89 |
| `ui/navigation/AppRoot.kt` | 全局 Compose 状态、链接预览、更新检查 | 307 |
| `ui/navigation/AppContent.kt` | 手机/平板三种 NavHost 装配 | 197 |
| `ui/navigation/Routes.kt` | master/detail 路由、转场、玻璃底栏 | 253 |
| `ui/navigation/TabletNavRail.kt` | 平板左侧导航栏 | 80 |

原 `LsbApp` 的 6 类职责已拆出第一批文件；剩余职责按下表继续细化：

| 内容 | 现位置 | 抽向 |
| :--- | :--- | :--- |
| 深链路由 `openLinkDirect` | `:276-354`（79 行：17 条 `when` 精确路径 + 6 个正则匹配 + 外链兜底） | `ui/navigation/DeepLinkRouter.kt` —— 纯逻辑零 Compose，**可单测** |
| 链接预览流程 | `openLink:356` + `LaunchedEffect:368` + 内联 `AlertDialog:376`(45 行) | `ui/component/dialog/LinkPreviewDialog.kt` |
| 更新检查 | `LaunchedEffect:425` + 内联 `AlertDialog:438`(39 行) | `ui/component/dialog/UpdateDialog.kt` |
| 人机验证接线 | `:214-225` | `ui/navigation/VerificationHost.kt` |
| Toast 泵 | `:227-232` | 同上 |
| 底栏路由 | `navigateBottom:246` + 路由归一 `:237-243` | `ui/navigation/BottomBarNavigation.kt` |

抽 `DeepLinkRouter` 时注意：`:334-343` 的正则与 `:311-332` 的**锚定方式不同**（宽松 vs `^...$` 精确），是兜底路径而非冗余，且 `:342` 有独有的 `uid` 查询参数处理。两块都保留。

`LsbApp` 根 composable 已重命名为 `AppRoot`，与 `LsbApp.kt` 的 `Application` 子类名称解耦；Activity 只调用 `ui/navigation/AppRoot.kt`。

`AppContent` 保留**三个** `NavHost` 装配分支（手机+玻璃底栏 / 手机+经典底栏 / 平板双栏）。三份都调用同一对 `masterRoutes`/`detailRoutes`，路由定义不重复；不要为了减少代码而合并布局分支，避免引入行为变化。

### 4.4 `TopicExport.kt`（已完成）

导出职责已按边界拆开：`TopicExportText.kt` 负责 HTML/Markdown 文本渲染，`TopicExportFiles.kt` 负责文件命名、落盘和系统分享，`TopicExport.kt` 保留带 Android View/Bitmap 的长图渲染与 `ExportTheme`。`TopicScreen` 的导出行为和文件格式保持不变；三个文件均低于 400 行。

### 4.5 `MarkdownText.kt`（已完成）

块级 Markdown 语法解析（标题、列表、引用、代码围栏、分割线、GFM 表格）已迁移至无 Compose 依赖的 `ui/MarkdownParser.kt`（182 行）；`MarkdownText.kt` 仅保留渲染、行内富文本和链接回调（237 行）。新增 `MarkdownParserTest` 覆盖换行归一化、中文标题、嵌套列表、转义表格分隔符等协议边界；调用签名与渲染行为保持不变。

---

## 5. 阶段 5：数据层拆分

### 5.1 `HtmlParser.kt`（1,872 行）

先抽 `ParserSupport`，再按域拆，最后处理 `parseTopicPage`。

| 步 | 内容 | 说明 |
| :--- | :--- | :--- |
| 5.1.1 | `internal object ParserSupport` —— 17 个私有工具（使用次数见 `ARCHITECTURE.md` §4） | 所有拆分文件的共同基座，必须先做 |
| 5.1.2 | `markdownToHtml:53`(127 行) → `domain/export/MarkdownRenderer.kt` | 与抓取无关；与 `TopicExport.kt:137` 的 `htmlToMarkdown` 是互逆操作，应同处 |
| 5.1.3 | 按域拆：`ForumParser`(41) `UserParser`(~130) `NotificationParser`(~60) `BoardParser`(~160) `FormParser`(~150) `GachaParser`(~280) `MessageParser`(~100) | 每个都远低于 400 行 |
| 5.1.4 | `parseTopicPage:325`(463 行) —— 见下，单独一个提交 | **全项目最高风险点** |

`parseTopicPage` 的拆分约束：`:353-355` 有注释记录的 DOM 顺序依赖——面板必须在楼层之前解析，因为楼层解析会 `remove()` 这些节点。八个分块与顺序见 `ARCHITECTURE.md` §4。

拆法：编排留在 `TopicPageParser`，八个分块各自成 `(Document) -> T?` 签名的文件，**顺序写在编排处**。分块内部不得依赖"自己被第几个调用"。

验证方式（这一步不能只靠编译）：

```
1. 拆分前：对 5 个代表性帖子的 HTML 抓快照存本地（普通帖 / 抽奖帖 / 投票帖 / 申精评议帖 / 虚拟卡帖）
2. 拆分前后分别跑同一份 HTML，比对 TopicPageData 的 toString() 逐字节一致
3. HTML 样本放入 `app/src/test/resources/` 并随测试入库；不得包含账号、Cookie 或其他私密信息
```

申精评议帖必须在样本里——它是近期新增且解析顺序敏感的功能，必须作为回归样本保留。

### 5.2 `LsbClient.kt`（775 行 → 6 块）

拆分表见 `ARCHITECTURE.md` §4。顺序：`CookieStore` → `CsrfProvider` → `AvatarCache` → `challenge/` → `auth/` → 残余为 `LsbHttpClient`。

`challenge/` 是最容易出错的一块：含 147 行 companion（UA 串、内嵌探测 JS、SHA-256 PoW `solvePow:172`、UAM nonce 运算、图片 magic byte 校验），且 `solveUamWithWebView:404` 依赖 `WebViewDoh`。**PoW 与 nonce 运算是纯函数，抽出后可单测**——这是本次重构少有的能立刻拿到测试覆盖的地方，优先补测试。

顺带修正：`LsbClient` 自己用裸 Jsoup 解析两处（`:755` CSRF、`:803` 登录页）；屏蔽词 JSON/HTML 解析已移交 `data/parser/KeywordFilterParser.kt`。注意 `:763` 是 Jsoup 失败时的正则兜底，**保留兜底逻辑**，别当冗余删掉。

### 5.3 `AppSettings.kt`（1,186 行 → 3 块）+ 消除 Session 镜像层

这是本阶段收益最大的一步。

| 步 | 内容 | 行数变化 |
| :--- | :--- | :--- |
| 5.3.1 | `SettingsStore` 改为**可观察**状态持有者 | — |
| 5.3.2 | 删除 `Session.kt:118-148` 的 20 个镜像属性、对应 20 个 `saveX()`、`reloadPrefs():235`(62 行) | **−250 行** |
| 5.3.3 | `store/` 拆出 9 个本地存储（`:369-1059`） | ~500 |
| 5.3.4 | `BackupSerializer`（`:1060-1343`，手写逐 key JSON 编组 285 行） | 285 |

5.3.2 的依据：`Session.kt:120-121` 的注释明确写了镜像存在的唯一原因是 `AppSettings` 非 Compose 可观察。根因消除后镜像层整块删除，不需要逐个搬迁。

同时修掉三个 pref 文件的重复打开：`lsb_topic_drafts` 被 `AppSettings.kt:42` / `Session.kt:1022` / `NewTopicScreen.kt:243` 各开一次；`lsb_reading_positions`、`lsb_ai_summaries` 各开两次。统一由对应 store 独占。

**风险**：`AppSettings` 有 96 个 key、59 个计算属性，且 `themePrefs` 指向的 `lsb_prefs` 与 `Session` **共享**（`AppSettings.kt:49` 有注释说明）。改可观察时若漏掉某个 key 的通知，表现为"改了设置界面没反应"——编译不报错，测试也测不到。**逐个 key 对照 `PreferenceKeys.kt` 清单核验，不要凭印象。**

### 5.4 `AppNetwork.kt` + 网络簇（1,072 行 → 4 个子包）

`AppNetwork` 是唯一对外入口（6 个调用方：`LsbClient:121`、`AiClient:22`、`ImageHostClient:21`、`LinkPreview:22`、`UpdateChecker:35`、`AppSettingsScreen:1822`），拆分时**保持这个门面不变**，只把内部实现分包。

| 目标 | 内容 |
| :--- | :--- |
| `network/NetworkPolicy.kt` | 门面 + `invalidate()` 扇出（`:192`） |
| `network/dns/` | `DohTransport.kt` 全部（159 行，全 `internal`，仅 `AppNetwork` 使用） |
| `network/proxy/` | `detectLocalProxy:145` `probeSocks5:159` `probeHttpProxy:169` + 动态 ProxySelector |
| `network/cronet/` | `CronetFallback.kt` 全部（452 行，5 个内聚类型） |
| `network/webview/` | `WebViewDoh.kt` + `LocalDnsTunnel.kt` |

顺带修 `AppNetwork.kt:217`：**每次 DNS 查询都 new 一个 `AppSettings`**。改为注入单例。

`LsbApp.kt:37`/`:49` 与 `AppNetwork.kt:243` 对 Cronet 拦截器有"先移除、后按序重加"的讲究（为了自定义 header 先生效）。这段顺序有实际原因，**移动代码时原样保留并保留注释**。

---

## 6. 阶段 6：repository 层

纯新增，不改屏幕。此阶段结束时 repository 已就位但无人调用——这是有意的，让阶段 7 的屏幕改造能一个个来。

7 个 repository：`Topic` `Forum` `User` `Gacha` `Message` `Account` `Settings`。每个都是**唯一**同时接触 `remote` 与 `parser` 的地方，对上只暴露 `LsbResult<Model>`。

`interface` + `impl` 成对出现（`agents.md` §4"通过接口抽象解耦"、§5"内部模块间调用通过 interface 隔离"）。这也是让数据层第一次可 mock。

`di/AppContainer.kt` 同期建立，替代现在的 `(app as LsbApp).client` 强制转换（`Session.kt:69`）。

**验收**：repository 层要有单元测试。用 `mockwebserver` 喂固定 HTML，断言 `LsbResult` 输出；测试放入已跟踪的 `app/src/test/`，与生产代码在同一提交中交付。

---

## 7. 阶段 7：屏幕接入 repository

按屏幕逐个改，一屏一提交。顺序按风险从低到高：

```
ForumScreens(276) → UserScreen(512) → SearchScreen(160) → CollectionDetailScreen(242)
  → LoginScreen(239) → MeScreen(254) → 阶段4拆出的各 gacha/message/account 屏幕
  → NewTopicScreen(962) → HomeScreen(1478) → TopicScreen(4425)
```

`ForumScreens.kt` 是**目标形态的参照模板**——它已经合规（`ForumListScreen` 95 行、`ForumScreen` 151 行），且两者都是同一形状：局部 `load()` → `LaunchedEffect(Unit)` → `Scaffold` → `when { loading / error / else }`。改造后的每个屏幕都应长成这样，只是 `load()` 换成 ViewModel 调 repository。

每屏改造内容：

1. 新建 `<Name>ViewModel`，持有 `LsbResult<T>` 状态，调 repository。
2. 屏幕删掉 `session.client.*` 与 `HtmlParser.*` 调用，改读 ViewModel。
3. 屏幕的 `session` 参数收窄——只保留仍需全局状态的部分（登录态、主题、Toast）。

### 7.x `NewTopicScreen.kt`（926 行）

先把顶部 11 个解析函数 + 6 个模型（`:55-192`，138 行）移入 `data/parser/FormParser.kt` 与 `model/`——这部分零风险，独立提交。

剩余 `NewTopicScreen`(770 行) 的 ~35 个表单状态是**平的、彼此独立**，且已自然聚成两簇：抽奖（`prizes` `drawAt` `participantTarget` `minReplyChars` `replyCaptcha` `prizeTypeMenu`）与虚拟卡（`cardName` `cardCurrency` `cardPrice` `cardLimit` `cardAutoReply` `cardAutoReplyContent` `cardValues` `currencyMenu`）。唯一耦合是 `doSubmit:342` 读全部、`saveDraft:270` 序列化子集。

抽 `NewTopicFormState` 状态持有者后，抽奖面板（`:658-799`）与虚拟卡面板（`:800-888`）可独立成文件。**这是四个大屏幕里最容易的一个**，建议作为屏幕改造的第一个练手对象（在 `ForumScreens` 之后）。

### 7.y `HomeScreen.kt`（1,446 行）

`HomeScreen`(855 行) + `HomeSidebarDrawer`(315 行) + 6 个侧栏小件。

先移 `parseSidebarStats:1359` 入 `data/parser/`（零风险）。`HomeSidebarDrawer` 及其小件独立成 `ui/screen/home/sidebar/`。

主 composable 的三个陷阱：

- `session.homeTabs[i]`（`HomeTabState`，`Session.kt:21`）把**全局**每 tab 状态（topics/page/scrollIndex）与**局部** `remember` 状态（loading/error/pendingNewTopics）混在一起，单个 `load()` 同时写两边。拆分时必须先把这条边界划清。
- 6 个 `rememberSaveable`（`lastRetCombo` `lastRetInfinite` `searchField` 等）存在的意义就是"在这个 composable 的作用域里存活过从帖子页返回的重组"。**移出作用域会静默失效**——不报错，只是从帖子返回后状态丢了。
- `drawerState` 被一路提升到 `MainActivity.kt:209` 再传回来；`categoryMotion`（`Animatable`，`:155`）驱动 `:802` 的滑动手势又被 `selectCombo` 读。

---

## 8. 阶段 8：`TopicScreen.kt`（4,293 行）

放在最后，因为它同时是最大的文件、churn 最高的文件（全仓库 170 个提交里改过 50 次，近 80 个提交里改过 40 次）、状态耦合最深的文件。

### 8.1 先摘容易的：2,362 行几乎零风险

`:2063` 之后的内容**只被 `TopicScreen.kt` 自己使用**，跨文件依赖只有 3 个符号（`HtmlContent`、`CodeBlockView`、`addSearchHighlights`，均来自 `Components.kt`）。整体搬迁即可：

| 目标 | 内容（行号为现位置） | 行数 |
| :--- | :--- | ---: |
| `data/reply/ReplyTree.kt` | `ReplyNode:2502` `buildReplyTree:2509` `flattenTree:2530` —— **纯函数，已迁移并可单测** | 33 |
| `topic/SmartDecode.kt` | `SmartDecodedContent:2535` `detectSmartEncodedContent:2538` —— 同样纯函数 | 43 |
| `topic/components/PostCard.kt` | `PostCard:2578`(122，签名跨 `:2578-2607`) `PillBadge:2700` `PostCardContent:2720`(**308**) | 450 |
| `topic/components/` 小件 | `StatChip:2286` `PostAction:2310` `ReplyBar:2334` `TopicBarAction:2392` `GlassReplyBar:2408` `LoginPromptCard:2464` `EditInfoText:3040` `estimateReading:3028` | 222 |
| `topic/panel/` | `TopicPollView:2063`(223) `LotteryPanelView:3308`(116) `AiSummaryCard:3424` `VirtualCardView:3510` `DanmakuBar:3088` `DonateSheet:3140`(168) | 488 |
| `topic/dialog/` | `ThreadDialog:3577` `ThreadPostItem:3654` `FloorJumpDialog:3723` `ReplyDialog:3753`(**219**) `ReplyToolbar:3972` `ReplyCaptchaField:3985` `PostCopySheet:4019` `CopySheetItem:4085` `FreeCopyDialog:4120` `ExportDialog:4190`(116) `ExportSheetItem:4306` `CoinDialog:4349` | 849 |
| `topic/search/` | `TopicSearchHit:153` `SearchHighlightedText:156` `FLOOR_PILL_ENABLED:151` | 26 |

`PostCardContent`(308 行) 与 `ReplyDialog`(219 行) 拆完仍需内部再分。`PostCard` 的签名跨 `:2578-2607`，**28 个参数、其中 15 个是函数类型**——已远超 detekt `LongParameterList` 阈值 8。拆分时引入 `PostCardState` / `PostCardActions` 两个数据类收拢，否则参数只会更多。

`ReplyDialog` 的 6 个局部状态（`body` `answer` `busy` `expanded` `previewReply` `imageUploadBusy`）完全自包含，是干净的整体搬迁。

### 8.2 再拆主 composable：1,886 行的状态网

回复树纯函数已在提交 `30252b5` 迁移至 `data/reply/ReplyTree.kt`；导出文本/文件职责已在 `2fe61fe` 拆分。后续继续围绕状态网、滚动控制器和测量状态拆分；不改变楼层排序、折叠和分页语义。

`TopicScreen`(`:177-2062`) 里 45 个 `mutableStateOf`（`:193-290` 一个平铺块）+ 20 个派生值（`:294-447`）+ 15 个闭包函数（`:269-941`）+ 14 个 `LaunchedEffect` + 1 个 `DisposableEffect`，互相咬合。

已识别的四个结上：

**结 1：加载状态被 5 个函数和 6 个 effect 同时读写。** `data` `loading` `loadingMore` `error` `localReplyPage` `loadedMaxPage` `sortOrder` `collapsedReplyIds` 被 `load:465` `goLocalReplyPage:519` `loadAllRemaining:559` `loadAllForSearch:594` `jumpFloor:636` 写入。其中 `jumpFloor` 一个函数就改 7 个状态。

**结 2：楼层跳转是多趟状态机。** `pendingSearchPost` + `:671` 的 `LaunchedEffect(pendingSearchPost, flatTree, localReplyPage, collapsedReplyIds, sortedPosts)` 会**故意 `return@LaunchedEffect` 让自己因依赖变化再次触发**——它先改 `localReplyPage`/`collapsedReplyIds`，等重组后再继续。这个模式不能分割，必须把整个回复树模型收进一个持有者。

**结 3：一个 `LazyListState` 串了 7 个特性。** `listState:238` 被 `jumpFloor` `jumpToFloor` `scrollToComments` 回到顶部 无限滚动 `returnPos`/`pendingRestoreOffset` 恢复 以及 `:1019` 的 effect 共用。

**结 4：测量值向上流、派生值向下流。** `titleBottomInRoot` `contentTopInRoot` `bodyTop` `bodyHeight` `headingPositions` 由 `item("header")` 与 `PostCard` 内部的 `onGloballyPositioned` / `onBodyLayout` / `onHeadingPosition` 回调**从深处写上来**，又被顶部的 `derivedStateOf`（`showTitleInTopBar:252` `bodyVisible:375` `readingProgress:382`）读走。抽取必须同时向下传回调、向上传派生值。

拆分顺序（每步一提交，每步后真机验收）：

```
8.2.1  抽 TopicUiState 状态持有者      —— 45 个 mutableStateOf 收拢，先不动逻辑
8.2.2  抽 TopicViewModel                —— 5 个加载函数 + repository 接入（解结 1）
8.2.3  抽 ReplyTreeState                —— 回复树 + 楼层跳转状态机整体搬（解结 2）
8.2.4  抽 TopicScrollController          —— LazyListState 与 7 个特性的中介（解结 3）
8.2.5  抽 TopicMeasurements              —— 测量值与派生值（解结 4）
8.2.6  拆 Scaffold 各 slot 与 9 个尾部对话框调用
```

其余零散点：`item("header")` 里有 **~120 行内联的 CSRF 表单重试逻辑**（`:1236-1310`，抽奖面板提交），属于业务逻辑写在 UI 里，移入 repository。`:1644` 的楼层胶囊被 `FLOOR_PILL_ENABLED = false`(`:151`) 关掉，是死代码——同 §4.1 的死屏幕一样，**照原样迁移，不删**。

### 8.3 `Components.kt`（1,996 行）

与 `TopicScreen` 同期做，因为 `HtmlContent` 是两者的接缝。

**先纠正一个可能的误判**：`:1592-2058` 的 467 行（`parseHtmlToBlocks:1592`(327) `renderInline:1950`(109) `annotateFloors:1919` `abs:1947`）看起来该移入 `data/parser/`，但它**把 Compose 类型烘焙进了输出**——区间内 9 处 `AnnotatedString`、3 处 `buildAnnotatedString`、4 处 `Color`，产物模型 `ContentBlock:849`/`TableCellData:841` 的字段类型就是 `AnnotatedString?` 与 `TextAlign?`。

这是有意的性能设计。`HtmlContent:889-895` 的注释写明缓存键含 `linkColor`/`codeBg`，因为"解析产物中已烘焙颜色，换主题需重解析"，为的是避免长帖快滑时主线程重跑 Jsoup。

**处理方式：整组移入 `ui/component/html/`，不进 `data/`。** 它是表现层解析器，当前归属正确。强行拆成"无 Compose 中间模型 + 上色阶段"会破坏缓存设计，代价大于收益。`ContentBlock`/`TableCellData` 同理留在 `ui/component/html/`，不进 `model/`。

余下按类型分包：

| 目标 | 内容 | 行数 |
| :--- | :--- | ---: |
| `ui/component/html/` | 上述 467 行 + `HtmlContent:877`(206) `CodeBlockView:1098` `TableView:1187`(129) `ImagePager:1340` `ZoomImageViewer:1391` `ZoomableImage:1516` `headingStyle:1083` `addSearchHighlights:1316` `htmlBlockCache:1590` | ~1,120，需内部再分 |
| `ui/component/avatar/` | `Avatar:192`(126) `Badge:321` `TitleBadgeView:354` `titleRarityColor:339` `OnlineDotColor:318` + SVG 三件套 `:166-191` | ~200 |
| `ui/component/card/` | `TopicCardView:417`(190) —— 7 个屏幕复用，最高价值组件 | 190 |
| `ui/component/state/` | `LoadingBox:680` `ErrorBox:687` `EmptyBox:721` `LoginRequiredBox:747` | 104 |
| `ui/component/pagination/` | `PaginationBar:607` | 73 |
| `ui/component/text/` | `LinkText:784` `formatViews:828` | 57 |
| `util/MediaStoreExt.kt` | `saveImageToGallery:1480` —— MediaStore 文件 IO | 36 |
| `ui/theme/` | `CardColorOverrides:139` `onColorFor:154` + 4 个 CompositionLocal `:129-138` | 37 |

`ui/component/html/` 拆完仍有 1,120 行，须内部再分为 `parser/`（467）、`render/`（HtmlContent + CodeBlockView + TableView）、`image/`（ImagePager + ZoomImageViewer + ZoomableImage，~250）三组。

`object CardColorOverrides:139` 是可变全局色覆盖存储，也是 §2.2 反向依赖的靶子——改为由 UI 从主题状态读取，不再由 `Session.kt:259/:472/:538` 推送。

---

## 9. 阶段 9：收尾

| 步 | 内容 | 判据 |
| :--- | :--- | :--- |
| 9.1 | `Session` 消解 —— 只剩登录态、主题、Toast、验证、通知未读 | 目标 <400 行 |
| 9.2 | 反向依赖清零 | `grep -rn 'sb\.linux\.client\.ui\.' app/src/main/java/sb/linux/client/data/` 无输出 |
| 9.3 | detekt baseline 清空 | `detekt-baseline.xml` 为空即重构完成 |
| 9.4 | 声明清单终比对 | 与 `%TEMP%/lsb-refactor-baseline/decls-before.txt` 差异逐条可解释 |
| 9.5 | Release 构建校验 | `./gradlew assembleRelease` 通过（CI 走这条，`release.yml:79`） |
| 9.6 | 文档回填 | 本文 §进度、`ARCHITECTURE.md` §7、`EXTENSION_POINTS.md` 更新为实际形态 |

---

## 10. 手工验收清单

自动化测试覆盖纯逻辑、parser 与 repository；Compose 交互、真实登录态和特定网络环境仍需每阶段按此表手测。**任何一项失败即停止合并并修复，不带着已知问题往下走。**

| 域 | 验收点 |
| :--- | :--- |
| 启动 | 冷启动、恢复登录态、主题正确应用（深/浅/OLED） |
| 首页 | 5 个分类 tab 切换、滑动切换分类、下拉刷新、分页/无限滚动、侧栏、从帖子返回后**滚动位置与 tab 状态保留** |
| 帖子 | 打开、翻页、楼层跳转、帖内搜索、树形折叠、回复、引用、点赞、投币、收藏、导出、AI 总结、弹幕 |
| 帖子特型 | 抽奖帖、投票帖、**申精评议帖**（基点新功能）、虚拟卡帖 |
| 发帖 | 普通帖、抽奖帖、虚拟卡帖、草稿保存与恢复、编辑已有帖 |
| 称号 | 抽取、交易、熔炼/回收动态表单、赠送 |
| 消息 | 私信列表、会话、通知 |
| 设置 | 12 个设置子页各自可开、改动生效、主题实时预览、备份导出/导入、WebDAV |
| 网络 | DoH 开关、代理、测速、Cronet 回退（弱网或 SNI 阻断环境）、WebView 内页面 |
| 平板 | 双栏布局、导航栏、详情空态 |

第 4 行"申精评议帖"与第 9 行"Cronet 回退"最容易漏测：前者是最新功能未经检验，后者需要特定网络环境。

---

## 11. 风险登记

| 风险 | 触发条件 | 缓解 |
| :--- | :--- | :--- |
| `parseTopicPage` DOM 顺序被破坏 | 拆分后分块顺序改变 | HTML 快照前后逐字节比对（§5.1）。编译与 UI 都发现不了这个 |
| `rememberSaveable` 移出作用域失效 | `HomeScreen` 的 6 个变量被移进子 composable | 手测"从帖子返回后状态保留"；此项不报错只丢状态 |
| `SettingsStore` 漏通知某个 key | 96 个 key 逐个改可观察时遗漏 | 对照 `PreferenceKeys.kt` 清单逐项核验，不凭印象 |
| `lsb_prefs` 双写冲突 | `AppSettings.themePrefs` 与 `Session` 共享该文件（`AppSettings.kt:49`） | 改造时该文件只留一个所有者 |
| Cronet 拦截器顺序被打乱 | 移动 `LsbApp.kt:37/:49`、`AppNetwork.kt:243` 的移除-重加逻辑 | 原样保留代码与注释；弱网环境实测 |
| 与 `main` 分支冲突 | 重构期间 `main` 继续开发。churn 最高的 5 个文件正是要拆的 5 个 | 每阶段结束 rebase 一次；阶段间不要跨越太久 |
| 版本号写回被破坏 | 改 `app/build.gradle.kts:22-23` 格式 | 这两行被 `release.yml:132-133` 的 sed 匹配，不动行尾格式 |
| 死代码被"顺手"删掉 | `CheckinScreen`/`IdentityCenterScreen` 无路由、`FLOOR_PILL_ENABLED=false` | `agents.md` §6 保真优先，照原样迁移并登记 |

---

## 12. 待决事项

以下事项不阻塞前期阶段；默认决策已记录，若执行中出现新证据再单独调整：

**1. 测试边界。** `app/src/test/` 已跟踪，用于纯函数、parser、repository 和 HTML 快照回归；`app/src/androidTest/` 暂保留为设备专用本机目录。若后续需要稳定的 Compose/设备 CI，再以独立提交放开，避免把不稳定设备测试混入结构重构。

**2. `agents.md` 是否入库。** 当前作为本机执行约束保持忽略；其核心交付要求已落实到本计划、`ARCHITECTURE.md` 和 `EXTENSION_POINTS.md`。默认不提交该本机文件。

**3. 两个无路由屏幕如何处置。** `CheckinScreen`、`IdentityCenterScreen` 存在但不可达。接上路由（新增功能）或删除（移除代码）都改变现状。**默认：照原样迁移。**

**4. 1,835 处硬编码中文是否外部化。** 与拆分正交，且会把 diff 淹没。**默认：本次不做，记为后续独立任务。** 例外见 `ARCHITECTURE.md` §5。

**5. 旧重构分支处置。** `refactor/layered-architecture` 基点较旧，仅作为历史参考；不整体 merge/cherry-pick。若其中有仍有价值的独立提交，先逐个审查其差异，再在当前分支重新实现或选择性提取。
