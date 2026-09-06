# LinuxSB 扩展点说明

> **状态：目标设计 + 已落地样板。** 本文描述 `codex/refactor-layered-architecture` 的扩展方式；屏蔽词扩展点已实施，其余条目仍是目标形态。
> 当前代码处于渐进迁移阶段，各扩展点的“现状”一栏说明尚未迁移的入口。
> 依据 `agents.md` §5（开闭原则）与 §6（扩展点文档）。结构见 `ARCHITECTURE.md`。

---

## 总原则

`agents.md` §5 要求"新增功能应通过新增文件/模块实现，避免修改已有核心类"。本项目的现实约束决定了扩展点的形态：

**数据来源是 HTML 网页，不是 API。** 源站改版会同时影响解析、模型、UI 三层。因此扩展点设计的首要目标是：**源站改一处，客户端改一处**。当前做不到——`ARCHITECTURE.md` §2.1 显示同一份解析逻辑散在 9 个屏幕文件里。

**源站是唯一真相。** 不在客户端猜测字数限制、权限规则、冷却时间。这条已经是现有代码的习惯（如 `Model.kt` 中 `EssenceApplication` 的注释"未申请状态与可用操作均来自源站，不在客户端猜测字数、权限或冷却规则"），扩展时继续遵守。

---

## 1. 新增一个源站页面

最常见的扩展。目标是**只新增文件，不改已有文件**（除了注册路由）。

| 步 | 新增 | 说明 |
| :--- | :--- | :--- |
| 1 | `model/<域>/<Name>.kt` | data class，零逻辑 |
| 2 | `data/parser/<Name>Parser.kt` | 复用 `internal object ParserSupport` 的 17 个工具，不要自己重写 `doc()`/`idFrom()` |
| 3 | `data/repository/<域>Repository.kt` 加一个方法，或新建 repository | 返回 `LsbResult<Model>` |
| 4 | `ui/screen/<域>/<Name>Screen.kt` + `<Name>ViewModel.kt` | 形状照 `ui/screen/forum/`（见 §模板） |
| 5 | `ui/navigation/Routes.kt` 注册路由 | **唯一需要修改的已有文件** |

若该页面有深链，另在 `ui/navigation/DeepLinkRouter.kt` 加映射。

**现状**：今天做这件事要改 `HtmlParser.kt`（加 parse 方法）、`Model.kt`（加模型）、`MiscScreens.kt`（塞进屏幕）、`MainActivity.kt`（加路由）四个大文件，其中三个超过 400 行红线。

### 模板

`ui/screen/forum/`（现 `ui/screens/ForumScreens.kt`）是目标形态的参照——它是重构前唯一已合规的屏幕文件（`ForumListScreen` 95 行、`ForumScreen` 151 行），形状为：

```
局部 load() → LaunchedEffect(Unit) → Scaffold → when { loading / error / else -> LazyColumn }
```

重构后每个屏幕都长成这样，只是 `load()` 换成 ViewModel 调 repository、`when` 换成对 `LsbResult` 的分支。新增屏幕时复制这个形状，不要发明新形状。

---

## 2. 源站 HTML 结构变化

### 2.1 淘帖插件协议

淘帖协议的扩展入口是 `data/parser/TopicCollectionParser.kt`。新增操作时应：

1. 在 `TopicCollectionOperation` 增加语义操作类型；
2. 在 `operationFrom()` 增加源站按钮值/文案映射；
3. 保留表单原始 action、hidden 字段和 submit name/value，不在 UI 硬编码路径；
4. 在 `TopicCollectionParserTest` 添加脱敏 HTML 夹具；
5. 通过 `TopicCollectionService.execute()` 提交，并重新读取源站验证状态。

主题页选择器的 `item_add`、`item_remove`、`item_remove_all` 是显式操作，不得用“当前状态取反”的本地逻辑替代。源站新增管理区块时，应扩展独立管理页模型与屏幕，不能重新塞回称号动态表单。

源站改版时的定位路径。

| 变化类型 | 改动位置 | 不需要改 |
| :--- | :--- | :--- |
| 某个字段的 CSS 选择器变了 | 对应 `data/parser/<域>Parser.kt` 一处 | 模型、repository、UI |
| 新增一个字段 | 该域 parser + `model/` 对应 data class 加字段（**给默认值**） | repository、其他屏幕 |
| 新增一个面板/区块 | `data/parser/topicpage/` 加一个 `(Document) -> T?` 文件，在 `TopicPageParser` 编排处插入调用 | 其他分块 |
| 帖子**正文** HTML 标签变了 | `ui/component/html/HtmlBlockParser.kt`（不在 `data/`，原因见 `ARCHITECTURE.md` §2.3） | 其他 |
| 表单字段名变了 | `common/constants/FormFields.kt` | 其他 |
| 路径变了 | `common/constants/SourceRoutes.kt` | 其他 |

模型加字段必须带默认值——这是现有代码的既定习惯（`Model.kt` 中几乎所有字段都有默认值），保证老的解析路径不因新字段而编译失败。

### 帖子页面板的顺序约束

`data/parser/topicpage/` 有一条**必须遵守的顺序约束**，来自现 `HtmlParser.kt:353-355` 的注释：

```
抽奖 → 虚拟卡 → 申精评议 → 投票 → 【楼层列表】→ 收藏表单 → 回复验证码 → 组装
                                      ↑
                    楼层解析会 remove() 上面各面板的 DOM 节点
```

新增面板解析器**必须插在楼层列表之前**。顺序写在 `TopicPageParser` 的编排里，分块内部不得依赖调用次序。

违反的后果是静默的：编译通过、UI 不报错、新面板拿到空数据。**加面板后必须用真实 HTML 验证，不能只看编译。**

---

## 3. 新增一个设置项

| 步 | 位置 |
| :--- | :--- |
| 1 | `data/local/PreferenceKeys.kt` 声明 key |
| 2 | `data/local/SettingsStore.kt` 加可观察属性 |
| 3 | 对应 `ui/screen/settings/<子页>.kt` 加一行 `SwitchRow` / `SettingMenuRow` |
| 4 | 若需进设置搜索：`ui/screen/settings/SettingsSearchIndex.kt` 加条目 |
| 5 | 若需进备份：`domain/backup/BackupSerializer.kt` 加字段 |
| 6 | 若需进分类重置：`SettingsStore` 对应 `resetX()` |

**注意第 2 步的可观察性。** 重构的一个核心改动是让 `SettingsStore` 可观察——现在 `Session.kt:118-148` 有 20 个属性纯粹是 `AppSettings` 的镜像，存在原因写在 `Session.kt:120-121`：`AppSettings` 不是 Compose 可观察的，直接写不触发重组。重构后不再需要镜像。**新增设置项时若忘了走可观察通道，症状是"改了设置界面没反应"——编译不报错。**

第 5、6 步容易漏。备份漏了表现为用户换机后该设置丢失；重置漏了表现为"恢复默认"不生效。

**现状**：除屏蔽词设置外，通常仍要改 `AppSettings.kt`（1,342 行）+ `Session.kt` 加镜像属性和 `saveX()` + `AppSettingsScreen.kt`（3,597 行）+ `SettingsSearchIndex.kt`，四处，其中两个严重超限。屏蔽词页已作为独立屏幕与 service/repository 分层样板。

---

## 4. 新增一个导出格式

现有三种：HTML、Markdown、分页长图。

```kotlin
// domain/export/TopicRenderer.kt
interface TopicRenderer {
    val formatName: String
    val fileExtension: String
    suspend fun render(topic: TopicPageData, options: ExportOptions): ExportArtifact
}
```

新增格式 = 新增一个 `TopicRenderer` 实现 + 在 `ExportTopicUseCase` 的渲染器列表注册。`ui/screen/topic/dialog/ExportDialog.kt` 从列表动态生成选项，不需要改。

**现状**：三种格式都硬编码在 `data/TopicExport.kt`（514 行）里，且该文件违规依赖 Compose（`:16`、`:57` 引入 `material3.ColorScheme`）。重构时配色改为普通数据类传入。

---

## 5. 新增一个 AI 提供方

现有 OpenAI 兼容接口（`data/AiClient.kt`，92 行）。

```kotlin
// domain/ai/SummaryProvider.kt
interface SummaryProvider {
    val id: String
    suspend fun summarize(text: String, config: AiConfig): LsbResult<String>
    suspend fun listModels(config: AiConfig): LsbResult<List<String>>
}
```

新增提供方 = 新增实现 + 在 `di/AppContainer` 注册。`AiSettingsScreen` 的提供方下拉从注册表生成。

现有的多方案管理（`AppSettings.kt:369-458` 的 `AiConfigPreset` CRUD）不变，只是 `AiConfig` 多一个 `providerId` 字段。

---

## 6. 新增一个图床

现有 Catbox、Pixhost、自定义接口（`data/ImageHostClient.kt`，101 行）。

```kotlin
// data/remote/media/ImageHost.kt
interface ImageHost {
    val id: String
    val displayName: String
    suspend fun upload(bytes: ByteArray, fileName: String): LsbResult<String>
}
```

"自定义接口"本身是一个 `ImageHost` 实现（配置驱动），新增内置图床与它并列，不改已有代码。

---

## 7. 新增一个 DoH 服务器

已经是配置驱动的，是现有代码里扩展性最好的部分。

`data/network/dns/DohServers.kt`（现 `data/DohServer.kt:47`）的 `defaults` 列表加一项即可。该文件已有 `merge()` 处理停用内置项的迁移逻辑——停用某个内置服务器时把它移入 `retired`，用户配置会自动迁移，**不要直接从 `defaults` 删除**，否则用户已选的项会变成无效状态。

测速（`DohBenchmark`）与校验（`isValidEndpoint`）自动适用，无需改动。

---

## 8. 新增一个底栏项 / 导航目的地

`ui/navigation/BottomBarItems.kt` 集中定义 id → 图标 + 文案 + 路由。

**现状**：这个映射今天有三份重复（`MainActivity.kt:545-546` 经典底栏、`:948-949` 玻璃底栏、items 定义处），加一项要改三处，`agents.md` §2 零复制的直接违例。

注意底栏有两种样式（经典 `NavigationBar` / 玻璃 `LiquidGlassBottomBar`）与平板导航栏（`TabletNavRail`）三个渲染位置，重构后共享同一份数据源。

当前底栏点击由 `ui/navigation/BottomNavigationCoordinator` 串行处理。新增标签时应先在 `BottomDestination` 声明规范名称和根路由映射，再在设置项列表注册；不要在 `AppRoot`、`AppContent`、`Routes` 各自增加导航分支。新标签必须补充“动画中点击、抽屉打开、快速重复点击、进程恢复”测试。

### 8.1 源站搜索范围或排序

搜索协议以 `data/SearchModels.kt` 为领域模型、`repository/SourceSearchRepository.kt` 为唯一 HTTP 接缝、`data/parser/SearchPageParser.kt` 为唯一 HTML 解析入口。源站改版时按以下顺序处理：

1. 在 `SearchScope` / `SearchSort` 增加带源站值的枚举项。
2. 在 parser 增加新的结果 DOM 解析，并为旧页面保留容错选择器。
3. 在 `SearchScreen` 增加对应展示和跳转，不把 HTML 选择器写回 UI。
4. 更新 `SearchPageParserTest` 的 HTML 夹具，覆盖分页和无结果。

当前源站使用 GET 查询参数，搜索请求不需要 `_csrf`；若源站未来切换为 JSON/AJAX，应在 `SourceSearchRepository` 内新增适配器，保持 `SearchService` 与 UI 接口不变。

---

## 9. 新增一个网络传输回退

现有链路：OkHttp/TCP → 失败 → Cronet/QUIC(HTTP3)。

`data/network/cronet/` 的 `CronetFallbackInterceptor` 是 OkHttp 拦截器，新增回退层按同样方式插入 `NetworkPolicy.clientBuilder()` 的拦截器链。

**两个必须保留的既有约束：**

1. `canRetryWithCronet`（现 `CronetFallback.kt:57`）做**安全重放**门控——不是所有请求都能重试。新增回退层必须做等价门控，否则会重复提交表单。
2. `LsbApp.kt:37`/`:49` 与 `AppNetwork.kt:243` 有"先移除拦截器、后按序重加"的处理，为了让自定义 header 先生效。这段顺序有实际原因，**改动网络装配时原样保留**。

---

## 10. 依赖注入扩展

`di/AppContainer.kt` 是唯一的依赖持有者。新增可注入组件在此登记。

**现状**：无 DI。`Session.kt:69` 用 `(app as sb.linux.client.LsbApp).client` 强制转换取客户端；`AppSettings(context)` 在静态代码里随处 new——`AppNetwork` 内 8 次，其中 `AppNetwork.kt:217` 是**每次 DNS 查询**都 new 一个。

重构后全 main 源集第一次有可 mock 的接缝。目前唯一可注入的是 `CronetFallbackInterceptor` 与 `DohTransport`（构造参数默认值，为 `androidTest/CronetDohDeviceTest.kt` 留的）。

---

## 11. 不是扩展点的地方

以下位置的修改会影响全局，**改前先读 `REFACTOR_PLAN.md` §11 风险登记**：

| 位置 | 原因 |
| :--- | :--- |
| `data/parser/topicpage/` 的调用顺序 | DOM 节点被消费，顺序错了静默失败（§2） |
| `ui/component/html/HtmlBlockParser` 的缓存键 | 缓存键含 `linkColor`/`codeBg`，因为解析产物已烘焙颜色；漏掉颜色键会导致换主题后正文不重解析 |
| `app/build.gradle.kts` versionCode/versionName 两行的行尾格式 | 被 `.github/workflows/release.yml:132-133` 的 sed 匹配，改格式会破坏发版自动写回 |
| `NetworkPolicy.invalidate()` 的扇出 | 现 `AppNetwork.kt:192`，串联 Cronet 失效、连接池清空、DNS 缓存清空、WebView 刷新、恢复信号 |
| `lsb_prefs` 的所有者 | 重构前被 `AppSettings.themePrefs`（`:49`）与 `Session` 共享，重构后须单一所有者 |
| `app/src/androidTest/` 的版本控制边界 | 当前为设备专用本机目录；若引入设备 CI，按独立基础设施变更处理 |

## 12. 屏蔽规则扩展点

屏蔽功能的源站字段扩展应遵循以下顺序：

1. 在 `common/filter/KeywordFilterModels.kt` 增加带默认值的字段。
2. 在 `KeywordFilterParser` 增加 JSON 与 HTML `data-*` 解析。
3. 在 `SourceKeywordFilterRepository` 增加请求序列化字段。
4. 在 `KeywordFilterRules.sanitize` 定义数量、长度和允许值校验。
5. 在 `TopicFilter` 增加纯函数匹配分支，并补充单元测试。
6. 最后在设置页展示；不要在 `HomeScreen` 或 `ForumScreens` 复制匹配逻辑。

若源站新增“只对首页生效”的规则，应由调用方传入 `isHome` 控制；若新增“所有帖子列表均生效”的规则，应放入 `TopicFilter` 的通用匹配部分。网络失败必须保留本地 pending 设置，不得在 UI 中直接吞掉异常。
