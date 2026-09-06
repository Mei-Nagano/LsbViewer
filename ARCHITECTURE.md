# LinuxSB 架构说明

> **状态：目标架构 + 当前迁移状态。** 本文描述分支 `codex/refactor-layered-architecture` 要达成的结构；屏蔽词垂直切片及多批低风险拆分已提交，其余重构按 §7 推进。
> 规划基点是 `main@1c47027`，屏蔽词基线提交为 `ecd9738`。规范依据：仓库根 `agents.md`。
> 执行计划见 `REFACTOR_PLAN.md`，扩展点见 `EXTENSION_POINTS.md`。

---

## 1. 重构前基线

测量时间 2026-09-05，基点 commit `1c47027`；统计包含屏蔽词提交 `ecd9738`。

| 指标 | 数值 |
| :--- | :--- |
| Kotlin 文件（main） | 74 |
| 代码行数（main） | 28,120 |
| 超 400 行红线的文件 | 15 个 |
| 最大文件 | `ui/screens/TopicScreen.kt` 4,293 行 |
| 最大函数 | `HtmlParser.parseTopicPage` 463 行 |
| 最大 composable | `TopicScreen` 1,886 行（177–2062） |
| `@Composable` 总数 | 154 |
| 导航目的地 | 44（master 4 + detail 40） |
| `interface` 总数 | 1（`ui/MarkdownText.kt:86`，private） |
| ViewModel 总数 | 1（`data/Session.kt`，1,013 行 god object） |
| repository / service | 屏蔽词专项各 1 个，其余业务域仍为 0 |
| 依赖注入 | 无。手工构造 + 15 个 `object` 单例 |
| 单元测试文件 | 1（屏蔽词纯函数）；`app/src/test/` 已纳入版本控制，`androidTest/` 仍为本机目录 |
| 本地存储 | 9 个 SharedPreferences 文件 / 96 个 key，无 DataStore |
| 硬编码中文字面量 | 1,835 处，项目无 `strings.xml` |
| 依赖版本管理 | 无 version catalog，坐标硬编码在 `app/build.gradle.kts` |
| 静态检查 | 无 detekt / ktlint / editorconfig |

工具链：AGP 9.2.1（内置 Kotlin 2.2.10）、Gradle 9.4.1、JDK 17、compileSdk 37、minSdk 26。

---

## 2. 现状的四个结构性问题

文件过大只是症状。真正要治的是下面四条，它们决定了拆分顺序。

### 2.1 表现层直连传输层，没有 repository

屏幕自己发请求、自己解析 HTML。全项目统一是这个形状：

```kotlin
// ui/screens/TopicScreen.kt:465 附近，同样的写法在 9 个屏幕文件里重复
fun load(p: Int) {
    scope.launch {
        try {
            val resp = session.client.get("/topic/$tid?p=$p")
            data = HtmlParser.parseTopicPage(resp.html, tid)
        } catch (e: Exception) { error = e.message }
    }
}
```

调用统计：

| 文件 | `session.client.*` | `HtmlParser.*` | `scope.launch` |
| :--- | ---: | ---: | ---: |
| `TopicScreen.kt` | 30 | 27 | 26 |
| `MiscScreens.kt` | 29 | 25 | 20 |
| `UserScreen.kt` | 1 | 6 | 2 |
| `SettingsScreen.kt` | 4 | 6 | 2 |
| `HomeScreen.kt` | 2 | 4 | 11 |
| `AppSettingsScreen.kt` | 0 | 0 | 9 |
| `ForumScreens.kt` | 3 | 3 | 4 |
| **合计（整个 `ui/`）** | **82** | **78** | – |

这违反 `agents.md` §4 的"禁止跨层直接调用"。后果不只是难看：数据层任何签名变化都会散射进 16k 行屏幕代码，所以**必须先建 repository 层，再动屏幕**，否则屏幕要改两遍。

### 2.2 反向依赖：数据层写进 UI 层

`data/Session.kt` 用全限定名绕过 import 写 UI 全局状态，共 4 处：

| 位置 | 越界写入 |
| :--- | :--- |
| `Session.kt:259`、`:472` | `sb.linux.client.ui.CardColorOverrides.map = cardColors` |
| `Session.kt:538` | `sb.linux.client.ui.CardColorOverrides.map = emptyMap()` |
| `Session.kt:1017` | `sb.linux.client.ui.clearBodyImageLayoutCache()`（定义在 `ui/BodyImage.kt:43`，`internal`） |

另有 Compose 依赖渗入数据层：

| 位置 | 内容 |
| :--- | :--- |
| `Session.kt:2-11` | 8 个 `androidx.compose.runtime.*` / `ui.graphics.*` import |
| `TopicExport.kt:16` | `import androidx.compose.ui.graphics.toArgb` |
| `TopicExport.kt:57` | `fun fromColorScheme(cs: androidx.compose.material3.ColorScheme)` |

`agents.md` §4 对循环依赖零容忍。修法：`CardColorOverrides` 由 UI 侧从状态读取，而非数据层推送。`TopicExport` 情况较轻——它已有自己的 `ExportTheme` 数据类（`:30`）与亮色兜底（`:77`），只需把 `fromColorScheme` 工厂移到调用方（UI 侧），数据层即彻底与 Compose 解耦。


### 2.3 解析逻辑散落在 UI 层，UI 逻辑散落在数据层

双向错位，各有四处：

**UI 里的数据逻辑**

| 位置 | 内容 | 行数 |
| :--- | :--- | ---: |
| `ui/screens/NewTopicScreen.kt:55-191` | 11 个发帖页表单解析函数 + 6 个模型类，**已验证不含 Compose 依赖** | 137 |
| `ui/screens/AppSettingsScreen.kt:1821` | `private object WebDav`——完整 WebDAV HTTP 客户端（PUT/GET/Basic Auth），`:1822` 自建 OkHttpClient | 39 |
| `ui/screens/HomeScreen.kt:1359-1389` | `parseSidebarStats`，**已验证不含 Compose 依赖** | 31 |

上面三处可无条件移入 `data/`。

**一个例外：`Components.kt` 的 HTML 引擎不能移入 `data/`**

`parseHtmlToBlocks:1592`(327 行) + `renderInline:1950`(109) + `annotateFloors:1919` 看似是"纯 Jsoup→模型"，实际**把 Compose 类型烘焙进了输出**：该区间有 9 处 `AnnotatedString`、3 处 `buildAnnotatedString`、4 处 `Color`；产物模型 `ContentBlock:849` 与 `TableCellData:841` 的字段类型是 `AnnotatedString?` 与 `TextAlign?`。

这是有意的性能设计，不是疏忽——`HtmlContent:889-895` 的注释说明缓存键包含 `linkColor`/`codeBg`，因为"解析产物中已烘焙颜色，换主题需重解析"，目的是避免长帖快滑时在主线程重跑 Jsoup。

**结论：这部分归 `ui/html/`，不进 `data/parser/`。** 强行拆成"无 Compose 中间模型 + 上色阶段"会破坏这个缓存设计（要么失去缓存收益，要么两级都缓存），代价大于收益。它本质是**表现层解析器**，放在 UI 层是正确的归属，不是违规。

**数据层里的表现逻辑**

| 位置 | 内容 |
| :--- | :--- |
| `data/Endpoints.kt:12-24` | `object TimeFmt`——相对时间中文文案（"刚刚"/"分钟前"） |
| `data/AppSettings.kt:964-992` | `relativeTimeText()`——私信列表时间文案 |
| `data/AppSettings.kt:993` | `dayKey()`——日期分组显示 |
| `data/SettingsSearchIndex.kt` | 设置项搜索索引，仅 `AppSettingsScreen.kt:587` 使用，含 77 条中文文案 |

### 2.4 无抽象，无法测试

全 main 源集 1 个 interface（还是 private 的）。数据层是 15 个 `object` 单例 + 13 个具体类，没有 `interface` / `abstract` / `sealed`。目前唯一可注入的是 `CronetFallbackInterceptor` 与 `DohTransport`（构造参数默认值，为 `androidTest` 的 `CronetDohDeviceTest` 留的）。

叠加 `AppSettings(context)` 在静态代码里随处 new（`AppNetwork` 内 8 次，其中 `AppNetwork.kt:217` 是**每次 DNS 查询**都 new 一个），当前没有任何东西可 mock。

---

## 3. 目标目录结构

### 3.1 淘帖垂直切片（已落地）

淘帖不再复用称号系统的 `GachaOperationPage`。源站 v9 插件的列表、详情、主题页收录和独立管理页由以下链路承载：

```text
ui/screens/TopicCollectionsScreen / CollectionDetailScreen / TopicCollectionPickerScreen
        ↓
service/TopicCollectionService
        ↓
repository/SourceTopicCollectionRepository
        ↓
data/parser/TopicCollectionParser + LsbClient
```

解析器保留源站表单的 `method`、`action`、hidden 字段和提交按钮值；服务层提交前刷新 CSRF，并在需要时重新读取源站页面。列表按源站行为只读取一页，详情按 `/topic_collection/{id}?p=N` 的真实分页读取。独立 `topic_collection_manage` 路由不再落入称号操作页。

`agents.md` §4 的分层是 Spring 术语（`controller/` `service/` `repository/` `config/`）。本项目是 Android 客户端，按等价语义映射，映射表见 §6。

```
app/src/main/java/sb/linux/client/
├── LsbApp.kt                       Application：Coil ImageLoader、容器初始化
├── MainActivity.kt                 单 Activity 宿主，仅生命周期与主题装配
│
├── di/                             【新增】手写依赖容器（替代 Spring Bean 定义）
│   ├── AppContainer.kt             单一持有者：client / repository / store
│   └── ViewModelFactories.kt       ViewModel 工厂
│
├── common/                         【新增】公共层：常量、错误、结果封装
│   ├── constants/  SourceRoutes.kt · FormFields.kt · Durations.kt · Regexes.kt
│   ├── error/      LsbException.kt · SourceError.kt（sealed）
│   └── result/     LsbResult.kt（sealed：Success / Failure / Loading）
│
├── model/                          【新增】全部数据模型，无逻辑
│   ├── topic/      Topic.kt · PostEntry.kt · TopicPageData.kt · Poll.kt · Essence.kt
│   ├── user/       UserProfile.kt · LoginState.kt · TitleBadge.kt
│   ├── forum/      Forum.kt · Pagination.kt
│   ├── gacha/      GachaCenter.kt · GachaMarket.kt · GachaOperation.kt
│   ├── message/    DirectMessage.kt · Notification.kt
│   ├── settings/   AiConfigPreset.kt · DohServer.kt · UsageEvent.kt
│   └── local/      CommentFavorite.kt · AppSettingsModels.kt
│
├── data/
│   ├── remote/                     HTTP 传输
│   │   ├── LsbHttpClient.kt        OkHttp 装配 + get/post 原语
│   │   ├── CookieStore.kt          CookieJar 实现
│   │   ├── CsrfProvider.kt         CSRF 取用与失效
│   │   ├── challenge/              反爬挑战：UamSolver · ProofOfWork · ChallengeDetector
│   │   ├── auth/                   LoginApi · LogoutApi · LoginCaptcha
│   │   └── media/                  AvatarCache · ImageHostApi
│   ├── network/                    网络策略（现 6 个文件重组）
│   │   ├── NetworkPolicy.kt        原 AppNetwork：唯一对外入口
│   │   ├── dns/                    DohTransport · DnsCache · BootstrapDns · DohBenchmark
│   │   ├── proxy/                  ProxyResolver · LocalProxyProbe
│   │   ├── cronet/                 CronetFallbackInterceptor · CronetTransport · CronetBridge
│   │   └── webview/                WebViewDoh · LocalDnsTunnel
│   ├── parser/                     HtmlParser 按域拆分
│   │   ├── ParserSupport.kt        共享私有工具（doc/idFrom/avatarOf/hiddenFields…）
│   │   ├── TopicListParser.kt · TopicPageParser.kt
│   │   ├── topicpage/              parseTopicPage 的 8 个分块解析器
│   │   ├── ForumParser.kt · UserParser.kt · NotificationParser.kt
│   │   ├── GachaParser.kt · MessageParser.kt · FormParser.kt
│   │   └── （注：HTML 正文渲染解析留在 ui/component/html/，见 §2.3）
│   ├── local/                      本地存储
│   │   ├── PreferenceKeys.kt       96 个 key 集中声明
│   │   ├── SettingsStore.kt        可观察偏好（消除 Session 镜像层）
│   │   └── store/                  HistoryStore · FavoriteStore · MessageStore ·
│   │                               UsageStore · DraftStore · ReadingPositionStore ·
│   │                               AiSummaryStore · TopicPageCache · HomeCache
│   └── repository/                 【新增】屏幕唯一数据入口
│       ├── TopicRepository.kt · ForumRepository.kt · UserRepository.kt
│       ├── GachaRepository.kt · MessageRepository.kt · AccountRepository.kt
│       └── SettingsRepository.kt
│
├── domain/                         【新增】跨 repository 的用例编排
│   ├── export/     ExportTopicUseCase · HtmlRenderer · MarkdownRenderer · LongImageRenderer
│   ├── backup/     BackupUseCase · WebDavClient（从 UI 迁入）
│   ├── ai/         SummarizeTopicUseCase
│   └── reply/      ReplyTreeBuilder（纯函数，可测）
│
├── ui/
│   ├── theme/          LsbTheme · ColorSchemes · Typography · PaletteStyles
│   ├── component/      按类型细分的共享组件
│   │   ├── avatar/ · card/ · state/ · pagination/ · dialog/
│   │   └── html/       HtmlBlockParser（含 Compose 类型，见 §2.3）· HtmlContent ·
│   │                   CodeBlockView · TableView · ImagePager
│   ├── screen/         按功能域分包，每包 ≤400 行/文件
│   │   ├── home/ · topic/ · forum/ · user/ · newtopic/ · settings/
│   │   ├── gacha/ · message/ · account/ · web/
│   │   └── 每包内：<Name>Screen.kt · <Name>ViewModel.kt · components/ · dialog/
│   └── navigation/     AppRoot.kt · AppContent.kt · Routes.kt · TabletNavRail.kt
│
└── util/                           无状态纯函数扩展
    ├── ColorExt.kt · TimeFormat.kt · FileExt.kt · TextExt.kt · HtmlEscape.kt
```

### 依赖方向

```
ui/screen  ──→  ui/component ──→ ui/theme
    │
    └──→ domain ──→ data/repository ──→ data/remote  ──→ data/network
                          │            data/parser
                          └──────────→ data/local
                                            │
         全层可依赖：model · common · util ←─┘
```

硬规则，由 §5 的 detekt 规则机械保证：

1. `data/**` 不得出现 `androidx.compose.*` 或 `sb.linux.client.ui.*`。
2. `ui/screen/**` 不得直接引用 `data/remote/**`、`data/parser/**`。
3. `model/**`、`common/**`、`util/**` 不得依赖 `data/**` 与 `ui/**`。
4. `util/**` 只含无状态纯函数，不持有 `Context`。

---

## 4. 各层职责

### `common/`

| 目录 | 职责 | 现状来源 |
| :--- | :--- | :--- |
| `constants/` | 源站路径、表单字段名、超时、正则。消除散落的路径字面量 | 现散落于各处；`Endpoints.BASE` 在 `data/Endpoints.kt:8` |
| `error/` | `LsbException`（已迁移至 `common/error/LsbException.kt`，被 `AiClient`/`ImageHostClient` 共用）+ `sealed class SourceError` 分类源站错误 | 现全项目 107 处 `try {`，多为裸捕获 |
| `result/` | `sealed class LsbResult<out T>`：`Success` / `Failure` / `Loading` | 现无，屏幕各自维护 `loading`/`error` 两个布尔 |

`agents.md` §3 要求"统一用 sealed class 承载业务错误，禁止裸 try-catch 吞异常"。`LsbResult` 是这条的落地形式。

### `model/`

现 `data/Model.kt` 已经很干净：49 个 data class，零逻辑（无 `fun`、无自定义 `get()`、无 `init`、无 `companion`）。工作是**把散落在别处的 21 个模型收回来**：

| 现位置 | 模型 |
| :--- | :--- |
| `HtmlParser.kt` | `DonateInfo:1165` `ReportInfo:1188` `ProfileField:1215` `ProfileFormData:1224` `AvatarPickerData:1239` `ProfilePageData:1248` `CheckinInfo:1387` |
| `LsbClient.kt` | `Resp:436` `AJAX transport:649` `LoginCaptcha:789` |
| `AppSettings.kt` | `AiConfigPreset:8` `CardRedemptionRecord:18` `UsageEvent:28` |
| `Session.kt` | `HomeTabState:21` `PendingVerification:33` `SearchResultCache:40` `TopicReadingPosition:49` `AiSummaryRecord:55` `ThemeModePref:1100` |
| `DohServer.kt` | `DohServer` `DohBenchmark` |
| `NewTopicScreen.kt` | `ForumOption:55` `PrizeRow:58` `SourceEditMeta:88` `LotteryInit:141` `CardInit:169` |
| `Components.kt` | `TableCellData:841` `ContentBlock:849` |

`HtmlParser.CheckinInfo` 被 `Session.kt:1038`、`:1041`、`:1060` 引用，是跨层耦合的具体证据。

### `data/remote/`

`LsbClient.kt`（775 行）拆为 6 块：

| 目标 | 现位置 | 行数 |
| :--- | :--- | ---: |
| `CookieStore.kt` | `:41-120` | 80 |
| `LsbHttpClient.kt` | `:121-146` 装配 + `:436-644` 请求原语 | ~230 |
| `challenge/` | `:156-433`（含 147 行 companion：UA 串、探测 JS、SHA-256 PoW、nonce 运算） | ~280 |
| `media/AvatarCache.kt` | `:149-155` + `:470-540`（内存 + 在途去重 + 磁盘 + magic byte 校验） | ~90 |
| `CsrfProvider.kt` | `:724-788`（含 Jsoup 失败时的正则兜底 `:763`） | ~65 |
| `auth/` | `:786-899` | ~115 |

顺带修掉：`LsbClient` 自己用裸 Jsoup 解析两处（`:755` CSRF、`:803` 登录页）；屏蔽词 JSON/HTML 解析已移交 `data/parser/KeywordFilterParser.kt`。

### `data/parser/`

`HtmlParser.kt`（1,973 行，单一 `object`，36 个公开方法 + 17 个私有工具）按域拆分。私有工具是所有拆分文件的共同依赖，抽为 `internal object ParserSupport`：

| 工具 | 位置 | 被引用次数 |
| :--- | :--- | ---: |
| `doc()` | `:180` | 30 |
| `idFrom()` | `:194` | 29 |
| `absUrl()` | `:197` | 16（全在文件内，实为 private） |
| `gachaTitleOf()` | `:240` | 12 |
| `avatarOf()` | `:204` | 10 |
| `textWithoutGachaTitle()` | `:235` | 9 |
| `hiddenFields()` | `:256` | 9 |
| `onlineOf()` / `onlineIdsOf()` | `:212` / `:223` | 6 / 6 |
| `labelOf()` | `:1256` | 6 |

**`parseTopicPage`（`:325-787`，463 行）是全项目最高风险的拆分点。** `:353-355` 有注释记录的顺序约束：面板必须在楼层之前解析，因为楼层解析会 `remove()` 这些 DOM 节点。内部分块与必须保持的顺序：

```
1. 抽奖面板    :356      ← 必须在 5 之前
2. 虚拟卡      :405      ← 必须在 5 之前
3. 申精评议    :438      ← 必须在 5 之前
4. 投票        :503      ← 必须在 5 之前
5. 楼层列表    :561      ← 会 remove 上面的节点
6. 收藏表单    :729
7. 回复验证码  :743
8. 组装        :758
```

拆分方式：`TopicPageParser` 保留编排与顺序，每个分块进 `topicpage/` 下独立文件，签名统一为 `(Document) -> T?`。顺序在编排处，不在分块里。

`markdownToHtml`（`:53-179`，127 行）与抓取无关，移入 `domain/export/MarkdownRenderer.kt`——它与 `TopicExport.kt:137` 的 `htmlToMarkdown` 是互逆操作，应当同处。

### `data/local/`

`AppSettings.kt`（1,186 行，59 个 `var` 计算属性 + 47 个函数 + 5 个 SharedPreferences 句柄）三分：

| 目标 | 现位置 | 行数 |
| :--- | :--- | ---: |
| `SettingsStore.kt` | `:52-368`（浏览/常规/网络/WebDAV/重置） | ~500 |
| `store/` | `:369-1059`（AI 预设、签到、凭据、历史、卡券、统计、收藏、评论收藏、私信） | ~500 |
| `BackupSerializer.kt` | `:1060-1343`（`exportJson` 106 行 + `exportJson(items)` 20 行 + `importJson` 158 行，手写逐 key 编组） | 285 |

**关键改造：`SettingsStore` 必须可观察。** 现在 `Session.kt:118-148` 有 20 个属性纯粹是 `AppSettings` 的镜像，各配一个 `saveX()`，加上 62 行的 `reloadPrefs()`，共约 250 行。存在的唯一原因写在 `Session.kt:120-121`：`AppSettings` 不是 Compose 可观察的，直接写不触发重组。把 `SettingsStore` 改为持有可观察状态，**这 250 行整块删除**，无需逐个搬迁。

同时修掉三个 pref 文件的重复打开：`lsb_topic_drafts` 被 `AppSettings.kt:42`、`Session.kt:1022`、`NewTopicScreen.kt:243` 各开一次；`lsb_reading_positions` 与 `lsb_ai_summaries` 同样各开两次。

### `data/repository/`

纯新增，`agents.md` §5 "新增功能通过新增文件实现"在此天然成立。每个 repository 是**唯一**同时接触 `remote` 与 `parser` 的地方，对上只暴露 `LsbResult<Model>`：

```kotlin
interface TopicRepository {
    suspend fun page(topicId: Long, page: Int): LsbResult<TopicPageData>
    suspend fun reply(topicId: Long, body: String, quote: String?): LsbResult<Unit>
    suspend fun toggleLike(postId: Long): LsbResult<LikeState>
    suspend fun vote(topicId: Long, optionIds: List<Int>): LsbResult<TopicPoll>
}
```

7 个 repository 覆盖现有全部数据访问点（统计见 §2.1）。

### `domain/`

只放**跨 repository 或含实质算法**的编排，避免变成无意义的转发层：

| 用例 | 现位置 | 说明 |
| :--- | :--- | :--- |
| `export/` | `data/TopicExport.kt`（514 行） | HTML/Markdown/长图渲染 + 文件 IO + 分享 Intent。拆为 3 个渲染器 + 1 个用例，`fromColorScheme:57` 工厂移到 UI 侧后数据层与 Compose 解耦 |
| `backup/` | `AppSettings.kt:1060-1343` + `AppSettingsScreen.kt:1821` | 备份编组在数据层、WebDAV 客户端在 UI 层、凭据在 `AppSettings.kt:293-321`——三处合一 |
| `ai/` | `data/AiClient.kt` + `TopicScreen.kt` 内的 `runAi` | |
| `reply/ReplyTreeBuilder.kt` | `TopicScreen.kt:2502-2534` | `buildReplyTree` / `flattenTree` 已是纯函数，直接移出即可单测 |

### `ui/`

`Session` god object 拆为 per-screen ViewModel + 少量真正全局的状态。全局保留项：登录态、主题、`SettingsStore`、Toast、人机验证、通知未读数。其余下沉到对应屏幕的 ViewModel。

---

## 5. 编码规范落地

`agents.md` §3 全部按 Kotlin 官方规范，通过工具机械保证而非人工检查。

新增到构建：

| 工具 | 用途 |
| :--- | :--- |
| `gradle/libs.versions.toml` | version catalog，消除硬编码坐标 |
| detekt | `MaxFileLength`（400 硬失败）、`LongMethod`、`LongParameterList`、`ForbiddenImport`（守 §3 的四条依赖规则） |
| ktlint | 官方 code style，含导入排序（`agents.md` §3 的四段分组） |
| `.editorconfig` | 缩进 4、行宽 120、LF |

命名与注释按 `agents.md` §3 执行，无偏离。注意 `.gitattributes` 是 `* text=auto` 且 `core.autocrlf=true`，工作区为 CRLF、仓库内为 LF，`.editorconfig` 声明 LF 与之一致。

### 消除重复（`agents.md` §2 零复制）

已定位的确切重复：

| 重复内容 | 位置 | 归入 |
| :--- | :--- | :--- |
| `esc()` HTML 转义，两处完全相同 | `HtmlParser.kt:44`、`TopicExport.kt:116` | `util/HtmlEscape.kt` |
| `openExternal()`，同文件内自我重复 | `AppSettingsScreen.kt:3256`、`:3643` | `util/IntentExt.kt` |
| 底栏图标/文案 `when` 映射，三处 | `MainActivity.kt:545-546`、`:948-949`，及 items 定义处 | `ui/navigation/BottomBarItems.kt` |
| 屏幕的 load/loading/error 三段式，40 处 | 各屏幕 | `LsbResult` + ViewModel 基类 |

**URL 绝对化三份实现不是重复**（已逐个读过，不要合并）：

```kotlin
Endpoints.abs(u)        // :9    最基础：非 http 开头就拼 BASE
HtmlParser.absUrl(u)    // :197  多一层：处理协议相对 URL "//host/path" → "https://host/path"，兜底调 Endpoints.abs
Components.abs(el)      // :1947 收 Element 而非 String：先取 Jsoup 的 abs:src，回退 src，再调 Endpoints.abs
```

后两者都已经在调用 `Endpoints.abs` 作为兜底，是**分层复用而非复制**。归位时按调用方就近放置（`absUrl` → `data/parser/ParserSupport`，`abs(el)` → `ui/component/html/`），保持三者的语义差异。

**不是重复的地方**（避免误删）：`MainActivity.kt:334-343` 的路由正则看似与 `:311-332` 重复，实则**锚定方式不同**——后者是 `^/topic/(\d+)$` 精确匹配，前者是 `/topic/(\d+)` 宽松匹配，作用是兜住 `/topic/123/xxx` 这类非精确路径，另含 `:342` 独有的 `uid` 查询参数处理。两块都要保留。

### 字符串外部化

1,835 处硬编码中文，项目连 `strings.xml` 都没有（`res/values/` 仅 `themes.xml`）。集中在 `AppSettingsScreen.kt`(483)、`TopicScreen.kt`(262)、`MiscScreens.kt`(241)、`HtmlParser.kt`(132)。

**本次重构不做外部化。** 理由：与拆分正交，1,835 处替换会把 diff 淹没到无法审查，且当前无 i18n 需求。仅在 `REFACTOR_PLAN.md` 记为后续独立任务。**例外**：`HtmlParser.kt` 的 132 处中文是解析器里的用户可见文案（错误消息、状态标签），随解析器拆分时就近归入对应 `constants/`。

---

## 6. `agents.md` 条款映射

`agents.md` 部分条款以 Spring 后端为背景，本项目是 Android/Compose 客户端。以下为等价替换，替换理由随条列出。**未列出的条款一律原样执行。**

| `agents.md` 原文 | 本项目落地 | 理由 |
| :--- | :--- | :--- |
| 表现层 `presentation/`、`controller/` | `ui/screen/` + per-screen ViewModel | 无 HTTP 入站，表现层即 Compose |
| 业务层 `service/`、`domain/`——含"事务管理" | `domain/` 用例，无事务 | 无数据库事务；一致性由源站保证 |
| 数据层 `repository/`、`dao/` | `data/repository/` + `data/remote` + `data/parser` + `data/local` | 数据源是 HTML 网页而非数据库，故 repository 之下多一层解析 |
| 配置层 `config/`——Spring/Nginx 配置、Bean 定义 | `di/AppContainer.kt` + `data/local/SettingsStore` | 无 Spring 容器，手写依赖持有者 |
| 全局异常处理 `@ControllerAdvice` / `ExceptionHandler` | `LsbResult` + ViewModel 统一错误状态 + `Thread.setDefaultUncaughtExceptionHandler` | 无 servlet 容器可挂全局 advice |
| 配置驱动：`application.yml` + `@ConfigurationProperties` | `SettingsStore`（SharedPreferences）+ `common/constants/` | Android 无 yml 配置装载 |
| 接口统一在 `api/` 包导出 | 各层 `interface` 与包内 `internal` 可见性约束 | Kotlin 有 `internal`，无需额外汇总包 |
| 交付 `API_CHANGELOG.md`（如有接口变更） | 已产出 | 屏蔽词专项新增内部接口并改变 APK 过滤行为，后续每个接口变化继续追加记录 |

以下条款**原样执行，无替换**：单文件 ≤400 行（§1）、零复制粘贴（§2）、Kotlin 官方命名与导入顺序与 KDoc（§3）、依赖方向单向与循环依赖零容忍（§4）、sealed class 承载错误（§3）、开闭原则与扩展点文档（§5）、功能保真与分模块多次提交（§6）。

关于 `agents.md` §6 的“回归测试通过”：`app/src/test/` 已纳入版本控制，纯逻辑、parser 与 repository 测试必须随代码提交；`app/src/androidTest/` 仍为设备专用本机目录，关键交互继续使用 §7 所述手工回归清单。

---

## 7. 实施进度

| 阶段 | 内容 | 状态 |
| :--- | :--- | :--- |
| 0 | 创建 `codex/refactor-layered-architecture`、测量基线、构建校验 | 完成（`main@1c47027`） |
| 1 | 屏蔽词垂直切片：api/common/parser/repository/service/util/UI/test | 完成（`ecd9738`）；单测与 Debug 构建通过 |
| 1.1 | 文档产出并纳入版本控制：本文 + `REFACTOR_PLAN.md` + `EXTENSION_POINTS.md` + `API_CHANGELOG.md` | 完成 |
| 2 | 工具链：version catalog、detekt、ktlint、`.editorconfig` | 未开始 |
| 3 | 零风险平移：`common/` `model/` `util/`，纯搬迁 | 进行中（异常、HTML 转义、本地收藏模型、称号/账号模型已完成） |
| 4 | 机械文件拆分：`MiscScreens.kt` `AppSettingsScreen.kt` `MainActivity.kt` | 进行中（Favorites、导航路由、Activity 宿主、WebDAV、导出、称号组件、Markdown 解析器、个人资料组件、用户主页组件已拆出） |
| 5 | 数据层拆分：parser、remote、local、network | 进行中（Cronet 响应适配器、Cookie 存储、新建帖子表单解析器已拆出） |
| 6 | repository 层建立 | 未开始 |
| 7 | 屏幕接入 repository + per-screen ViewModel | 未开始 |
| 8 | `TopicScreen` 拆分（最高风险） | 进行中（回复树纯函数已迁移，导出职责已拆分） |
| 9 | 收尾：`Session` 消解、依赖规则全量校验 | 未开始 |

各阶段的具体步骤、提交粒度、验证方式与回退点见 `REFACTOR_PLAN.md`。

## 8. 基线快照

历史基点 `35d79dc` 的可比对快照仍存于仓库外 `%TEMP%/lsb-refactor-baseline/`：

- `decls-before.txt` — 3,562 条声明清单，用于确认重构后无声明丢失
- `lines-before.txt` — 各文件行数
- `base-commit.txt` — 基点 commit hash

该快照只用于辅助识别历史声明；正式执行以 `main@1c47027` 加本分支首个屏蔽词提交为新的行为基线。阶段 2 会重新生成声明、行数与测试基线，避免拿旧快照误判当前代码。

## 9. 屏蔽词功能（已实施）

屏蔽功能已按源站 `home_keyword_filter` 契约接入，依赖方向为：

```text
BlockWordsScreen → Session → KeywordFilterService → KeywordFilterApi
                                      ↓
                     SourceKeywordFilterRepository → LsbClient
                                      ↓
                         KeywordFilterParser / TopicFilter
```

| 模块 | 职责 |
| :--- | :--- |
| `common/filter/KeywordFilterModels.kt` | 设置、页面策略、快照及源站限制常量 |
| `data/parser/KeywordFilterParser.kt` | 解析接口 JSON 与首页 `data-*` 策略 |
| `repository/SourceKeywordFilterRepository.kt` | GET/POST `/home_keyword_filter_settings`、错误分类 |
| `service/KeywordFilterService.kt` | 用户 ID 隔离、5 分钟 TTL、乐观保存、pending 重试 |
| `util/TopicFilter.kt` | 标题包含、用户名精确、首页版块匹配纯函数 |
| `Session.kt` | 登录生命周期与 Compose 状态桥接 |
| `HomeScreen.kt` / `ForumScreens.kt` | 帖子列表按页面边界调用统一过滤器 |

源站设置中的 `presets` 是已选择的预设词，可选预设和版块策略只从首页 HTML 的 `data-home-keyword-filter-*` 属性获得。规则只作用于帖子列表：版块规则只在首页生效，搜索和其他数据页不调用 `TopicFilter`。

账号缓存使用 `lsb_keyword_filter` SharedPreferences，键为 `keyword_filter.v2.<userId>`。旧版全局 `blocked_words`/`blocked_users` 首次登录同步时与源站规则取并集并上传，成功后清理；访客状态不直接应用旧账号规则。

`Session` 使用互斥锁串行化屏蔽设置的拉取、迁移和保存，避免网络恢复刷新与用户手动保存交叉覆盖。恢复事件仅在当前快照为 pending 时触发远端重试；验证弹层完成时先从 Compose 树移除，再恢复原请求，避免透明或滞留窗口拦截底部导航。

## 10. 底栏导航与源站搜索（2026-09-06）

底栏点击统一经过 `ui/navigation/BottomNavigationCoordinator`：点击意图串行消费，导航动画期间保留最后一次目标，标签栈使用 `saveState/restoreState`，并统一处理根路由别名。经典底栏和玻璃底栏均挂载在 `Scaffold.bottomBar`，玻璃效果不再通过覆盖 `NavHost` 的 `AnimatedVisibility` 改变点击层。

搜索按源站当前 Meilisearch HTML 协议分层：

```text
SearchScreen → SearchService → SearchRepository
                              ↓
                 SourceSearchRepository → LsbClient.get
                              ↓
                       SearchPageParser
```

请求使用 `GET /search?q=&scope=&sort=&p=`；解析结果分为主题和用户两种 `SearchResultItem`。源站新增范围或排序时只需扩展 `SearchModels.kt`、`SearchPageParser.kt` 与对应 UI 选项，不再修改帖子列表解析器。
