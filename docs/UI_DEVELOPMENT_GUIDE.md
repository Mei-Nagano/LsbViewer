# UI 分层与公共组件开发规范

版本：1.0  
日期：2026-10-20  
适用范围：Android / Kotlin / Jetpack Compose 原生界面及原站页面的应用侧外壳  
状态：开发约定与迁移方案；本文不代表所列目标 API 已实现

新一轮视觉改版：见 [MD3 全面界面重设计开发方案](MD3_UI_REDESIGN.md)。该方案替代本文保持旧外观的视觉基线；本文的公共层职责、复用与依赖边界继续适用。两轮开发和验收状态需分别记录。

最近验收（2026-10-21）：上次两项 P2 已关闭；H—K 的应用构建、结构检查与代表页面真机抽查通过。两份本地 UI 测试未同步 content 包引用，测试源码编译仍失败；这两份文件已暂存删除但物理副本仍在，不能将其等同于安装包失败。完整业务与设备矩阵尚未完成。详见 [UI 公共层验收记录](UI_ACCEPTANCE_REPORT.md) 第 7 节；前轮浅深色、开关与 1.3 倍字体证据见第 5、6 节。

## 1. 目标与边界

**页面负责组合业务内容，公共 UI 层负责统一视觉与通用交互。同一种界面语义只能有一套公共实现。**

新增菜单、状态占位、确认框、用户信息、摘要卡片等界面时，先使用已有公共组件；缺少能力时扩展公共入口，不在每个页面重新拼一套样式。统一包括颜色、字体、间距、形状、加载和错误反馈、禁用行为及可访问性，而不只是组件名称一致。

“公共”表示工程内共享，不要求全部声明为 Kotlin `public`。默认采用满足调用范围的最小可见性。

本方案继续使用现有 `:app` 和 Material 3，不要求新增依赖、拆 Gradle 模块、引入组件工厂或基类体系。保留主题设置、原站样式信息、页面状态、导航行为和数据契约。源站 WebView 内部由源站定义样式；应用只统一标题栏、登录门禁、加载反馈与平台桥接，不改写源站业务表单。

签到由自动流程完成，不需要手动签到入口；当前未接线的旧 `CheckinScreen` 不作为手动入口的验收项，不因其缺少路由要求新增入口。

本文补充 [整体重构开发文档](REFACTOR_DEVELOPMENT_GUIDE.md) 的 UI 约束，不改动其业务分层、兼容性与验证安排。已有实现按批迁移；新增或实际修改的界面先遵守本规范，不要求一次搬完全部目录。

## 2. 当前可复用的入口

下列路径相对于 `app/src/main/java/sb/linux/client/`，来自 2026-10-20 工作区检查。它们是迁移起点，不应并行另建同用途实现。

| 用途 | 已有入口与位置 | 使用约定 |
| --- | --- | --- |
| 主题 | `ui/theme/ThemeProvider.kt`：`LsbTheme`；`ui/theme/Theme.kt`：主题模式和色板 | 在应用根部装配；页面读取主题角色 |
| 中性表面 | `ui/theme/NeutralPageColors.kt`：`neutralPageColors` | 普通表面不跟随强调色整体染色 |
| 命名间距与尺寸 | `ui/theme/AppSpacing.kt`：`AppSpacing`、`LocalAppSpacing`；`ui/theme/AppSizes.kt`：`AppSizes`、`LocalAppSizes` | 由 `LsbTheme` 根部注入；公共布局只读角色，不自建页面级常量 |
| 组件专属默认值 | `ui/theme/` 下 `AppMenuDefaults`、`AppNoticeDefaults`、`AppSummaryDefaults`、`AppStateDefaults`、`AppPaginationDefaults` | 圆角、内边距、禁用透明度由组件集中拥有 |
| 分组与菜单 | `ui/components/layout/AppMenuComponents.kt`：`AppGroupLabel`、`AppSection`、`AppMenuGroup`、`AppMenuRow` | 设置、我的、快捷入口共用 |
| 设置适配 | `ui/screens/SettingsPage.kt`；`SettingsComponents.kt` 的 `GroupCard`、`SwitchRow`、`SettingMenuRow` | 继续委托公共组件，逐步减少旧适配 |
| 页面状态与分页 | `ui/components/feedback/ScreenStateComponents.kt`：`LoadingBox`、`ErrorBox`、`EmptyBox`、`LoginRequiredBox`；`ui/components/navigation/PaginationBar.kt`：`PaginationBar` | 首次加载、失败、空态、登录引导与分页共用 |
| 行内反馈 | `ui/components/feedback/AppNotice.kt`：`AppNotice` | 已有内容下的错误、重试、说明与关闭 |
| 确认 | `ui/components/feedback/AppConfirmDialog.kt`：`AppConfirmDialog` | 删除、重置、清理等简单确认共用 |
| 设置开关 | `ui/components/input/AppSwitchRow.kt`：`AppSwitchRow`；`ui/screens/SettingsComponents.kt`：`SwitchRow`（仅转发） | 设置行与表单开关行共用一套语义 |
| 版本更新提示 | `ui/AppUpdatePromptDialog.kt`：`AppUpdatePromptDialog` | 关于页与常规设置页共用同一提示 |
| 用户展示 | `ui/patterns/user/Avatar.kt`、`UserAvatar.kt`、`UserInfoCard.kt` | 区分纯头像、交互头像和完整信息卡 |
| 用户显示模型 | `ui/patterns/user/UserInfoCard.kt`：`UserInfoCardData`；`UserInfoCardData.kt`：转换函数 | 登录态和用户主页统一映射后显示 |
| 内容摘要 | `ui/patterns/topic/TopicCardView.kt`、`ReplySummaryCard.kt`；`ui/screens/NotificationSummaryCard.kt` | 帖子、回帖、通知按语义组合，避免复制排版 |
| 徽章 | `ui/BadgeComponents.kt`：`Badge`、`TitleBadgeView`；`ui/patterns/topic/TopicCardTags.kt` | 称号、标签复用；保留源站提供的含义 |
| 富文本与媒体 | `ui/content/`：`HtmlContent.kt`、`MarkdownText.kt`、`BodyImage.kt`、`TableView.kt` 等 | 渲染、图片、代码和表格沿用现有引擎 |
| 原站页面 | `ui/screens/SourceAccountPage.kt`：`AccountPageGate`、`SourceAccountPage`；`WebScreens.kt` | 共用登录、外壳与 WebView 通道 |
| 环境能力 | `ui/UiCompositionLocals.kt`：`LocalLinkHandler`、表格/UID 等偏好；`LocalSurfaceOpacity` 位于 `theme/ThemeProvider.kt` | 从根部注入，不另建页面级全局变量 |

当前仍有分散的尺寸、圆角和参数规则；部分共享入口也持有导航或数据能力。例如 `SettingsPage` 接收 `NavHostController`，`UserAvatar` 通过环境中的 repository 提供信息弹层。它们是现存迁移项，不应被当作新基础组件的依赖范例。`Badge` 等入口仍有固定字号，主题字体尚未覆盖所有组件；迁移中应处理这些例外，不能把目标当作现状。

## 3. 目标分层与依赖

目标目录仍在 `ui/` 内；只在有实际代码迁入时创建目录。

```text
ui/
  theme/                 主题、语义颜色、字体、尺寸和组件默认样式
  components/
    action/              应用特有的按钮组合与操作状态
    layout/              分组、菜单行、基础表面
    feedback/            提示、确认、状态占位
    input/               通用输入与选项行
    navigation/          公共标题栏、分页等视觉组件
  patterns/
    settings/            设置页外壳和设置项组合
    user/                用户卡、交互头像及其容器
    topic/               帖子卡和回帖摘要
    web/                 原站页面门禁和外壳
  content/               HTML/Markdown 表现解析、图片、表格、代码
  model/                 跨功能共用的显示模型与枚举
  util/                  纯表现工具函数与 Modifier 扩展
  feature/<业务>/        ViewModel、业务 UiState、映射和业务组件
  screens/               Route 与页面组合，逐步按业务归属整理
  navigation/            路由、深链、返回栈和应用导航宿主
  session/               应用级会话与环境状态装配
```

允许的主要依赖方向：

```text
Route / navigation / session → ViewModel → domain 契约
Route → Screen → patterns → components → theme
Screen / patterns / components → ui/model、ui/util
Screen / patterns → content → theme、ui/model、ui/util
```

- `theme`、基础 `components`、`ui/model` 和纯 `ui/util` 不依赖 Screen、ViewModel、Session、NavController、repository 或 data。
- `patterns` 可以使用领域显示类型，但业务请求交给 ViewModel 或独立容器；可复用的展示部分只接收数据与回调。
- 导航控制器、账号生命周期、文件选择和 WebView 等能力留在 Route、宿主或明确的平台适配入口，不塞入基础卡片。
- `domain`、`data` 不反向依赖 UI。根包 `common/` 继续承载跨层规则，不能因为函数“公共”就把 Compose 相关工具放进去。
- 不要求把纯展示 HTML 解析移到 data；现有包含 Compose 类型的表现解析、主题相关缓存仍归 UI。源站协议解析继续归 data。

## 4. 主题、尺寸与默认样式

### 4.1 单一视觉来源

| 维度 | 统一来源 | 页面使用方式 |
| --- | --- | --- |
| 颜色 | `MaterialTheme.colorScheme`，由 `LsbTheme` 生成 | 使用 `surface*`、`onSurface*`、`primary`、`error*` 等语义角色 |
| 字体 | `MaterialTheme.typography` | 按标题、正文、说明、标签选择角色，不另建页面字体缩放 |
| 基础形状 | `MaterialTheme.shapes` | 基础容器采用主题形状 |
| 间距 | 已有 `AppSpacing` / `LocalAppSpacing` | 公共布局统一读取命名间距，设置页已接入 |
| 控件尺寸 | 已有 `AppSizes` / `LocalAppSizes`（当前仅 `icon`） | 头像、触控区域和内容宽度角色按实际需要继续迁移 |
| 特定组件样式 | 已有 `AppMenuDefaults`、`AppNoticeDefaults` 等 | 特殊圆角、内边距和密度由组件集中拥有 |
| 表面透明度 | 现有 `LocalSurfaceOpacity` 与主题策略 | 迁移时核对适用表面，不对全部内容重复乘透明度 |

`AppSpacing`、`AppSizes` 与 `AppMenuDefaults`、`AppNoticeDefaults`、`AppSummaryDefaults`、`AppStateDefaults`、`AppPaginationDefaults` 已在 `ui/theme` 落地（见第 11 节批次 A）；`AppPageScaffold`、`AppSwitchRow` 也已有实现。`AppChoiceRow`、`UiDensity`、`UiNoticeTone` 仍未实现。视觉值统一归属，不建立第二套颜色或字体主题；优先复用 Material 3 的现成能力。

建议先从当前组件提取以下角色，数值作为初始迁移基准：

| 目标角色 | 初始值或规则 |
| --- | --- |
| `spacing.inline` / `item` / `section` | 4 / 8 / 16 dp |
| `spacing.pageHorizontal` / `pageVertical` | 16 / 12 dp，沿用现有设置页 |
| `sizes.icon` / `avatar` | 22 / 40 dp，沿用菜单和头像默认值 |
| `sizes.minimumTouchTarget` | 至少 48 dp 的交互区域；视觉图标可以更小 |
| 菜单圆角与行内边距 | 从现有 `AppSection`、`AppMenuRow` 提取到 `AppMenuDefaults` |
| 提示与摘要圆角 | 分别从 `AppNotice`、`ReplySummaryCard` 提取，保留各自语义 |
| 分页尺寸 | 从 `PaginationBar` 提取；补足触控区域时不强制放大视觉胶囊 |

统一意味着同一种角色使用同一值，不意味着所有卡片、气泡、胶囊都使用同一圆角。提取常量先保持现有效果；若要调整视觉或交互，以单独批次列出影响，不与文件搬迁混在一起。

### 4.2 页面约束

- 普通背景使用 `background` / `surface*`，正文使用匹配的 `on*`；强调色用于操作、选中态和有含义的标签。
- 页面不得自行声明整套颜色、字号、圆角和禁用透明度，不复制 `Color(0x...)` 形成另一套主题。
- 自定义主题色板、源站称号色、代码高亮等内容语义颜色允许集中映射；普通卡片不能借此绕过主题。沿用 `SourceTitleColors` 等入口。
- 用户字体、字号、深浅模式、背景图和透明度由根部统一装配。组件不能固定系统字体、重复缩放字号或用固定高度裁掉大字体。
- 平板宽度、窗口安全区、IME 和顶栏间距由页面外壳负责；内部卡片不重复消费外层 inset。
- 页面仍可使用 `Row`、`Column`、`Text` 等 Compose 原语组织专有内容；共享视觉语义必须走公共组件。

## 5. 公共组件与接口契约

### 5.1 组件目录

| 场景 | 首选入口 | 应统一拥有的行为 |
| --- | --- | --- |
| 菜单导航与分组 | `AppMenuRow`、`AppSection`、`AppMenuGroup` | 标题、说明、图标、尾部、禁用样式与行间距 |
| 设置开关与选项 | `AppSwitchRow`（现有 `SwitchRow` 转发到它）；目标 `AppChoiceRow` | 整行语义、选中状态、禁用、一次点击只触发一次变更 |
| 页面外壳 | `ui/components/navigation/AppPageScaffold.kt`：`AppPageScaffold`；`ui/screens/SettingsPage.kt`：`SettingsPage`（设置页适配） | 顶栏、返回动作、actions、内容 inset；不接收 NavController，不强制滚动 |
| 首次加载/失败/空态 | `LoadingBox`、`ErrorBox`、`EmptyBox`、`LoginRequiredBox` | 一致版式、文案层级、重试或登录动作 |
| 已有内容的失败 | `AppNotice` | 保留旧内容与滚动位置，局部提示，不覆盖为全屏错误 |
| 简单操作确认 | `AppConfirmDialog` | 取消、危险操作颜色、提交期间禁用与关闭策略 |
| 头像和用户信息 | `Avatar`、`UserAvatar`、`UserInfoCard` | 裁切、在线态、信息卡排版和统一点击含义 |
| 帖子/回复/通知 | `TopicCardView`、`ReplySummaryCard` 及通知适配 | 同类摘要的标题、正文、时间、徽章与点击区域 |
| 标签与称号 | `Badge`、`TitleBadgeView`、帖子标签入口 | 大小、颜色语义、文字与基线对齐 |
| 内容与媒体 | 现有富文本、图像、表格、代码组件 | 链接入口、字体偏好、长内容性能与主题切换 |
| 普通按钮和输入 | Material 3 标准组件 | 直接继承主题；存在应用共同行为时再抽公共组合 |

表中“目标”入口只在对应迁移批次实现，不允许在新页面调用尚不存在的 API。无需为每个 Material 控件增加只有一行转发的 `App*` 包装。

### 5.2 接口规则

1. 可复用组件提供 `modifier: Modifier = Modifier`，将外部 modifier 应用到组件根部一次。现有缺少该参数的入口在相关批次补齐。
2. 必需数据与动作显式传入；状态向下、事件向上。回调返回业务意图，不在组件中拼路由、发请求或写设置。
3. 支持的差异用明确参数或语义变体表达；例如目标 `UiDensity.Standard/Compact`、`UiNoticeTone.Error/Info`。不要不断追加互相冲突的布尔开关。
4. `leading`、`trailing`、`actions`、`footer` 等 slot 用于真正可变的内容；固定的标题字体、圆角和状态行为仍由公共组件负责。
5. 禁用必须同时约束视觉和实际操作。忙碌期间不能只显示转圈却保留重复提交；是否提交由调用方拥有，组件负责反映状态。
6. 标题、说明、按钮文本和内容描述支持长文本、换行及大字体；图标按钮提供有意义的描述，装饰图标不重复朗读。
7. 可点击整行与内部开关应有一致的开关语义，避免一次触摸重复触发；选中和错误不能只通过颜色表示。
8. 只有展开、焦点等局部视觉状态可以由展示组件拥有。提交、权限、数据加载、分页与可恢复业务状态仍属于调用方。
9. 确认框的可见性和后续动作由调用方管理；不要因为“统一弹窗”而将所有表单和复杂结果塞进 `AppConfirmDialog`。
10. 默认值写在唯一入口；业务代码不得通过任意颜色、内边距或嵌套主题把公共组件重新画成另一套。

现有 `AppPageScaffold` 接收 `title`、`onBack`、`actions` 和带 `PaddingValues` 的内容 slot，不接收 `NavHostController`。设置页的普通滚动容器、长列表的 `LazyColumn` 分别消费 padding，避免外壳强制滚动导致列表嵌套或状态丢失。

设置页离开行为沿用 `ui/screens/SettingsExitGuard.kt` 的 `rememberSettingsExit`，让系统返回和工具栏返回共用未保存确认。迁移旧适配前先核对真实行为，例如 `GroupCard` 的兼容参数 `initiallyExpanded` 当前不控制折叠，不应据其名称增加新的展开状态。

### 5.3 当前入口的调用示例

以下片段沿用已有 API；状态和回调由页面参数提供，不表示要新增同名框架。

```kotlin
AppSection(title = "浏览设置") {
    AppMenuRow(
        title = "屏蔽词",
        subtitle = state.blockedWordsSummary,
        onClick = onOpenBlockedWords,
    )
}

if (state.error != null && state.hasContent) {
    AppNotice(message = state.error, onAction = onRetry)
}

if (showResetConfirmation) {
    AppConfirmDialog(
        title = "恢复默认设置",
        message = "恢复此分类的默认设置？",
        onDismiss = onDismissReset,
        onConfirm = onReset,
        enabled = !state.resetting,
    )
}
```

## 6. 公共类、模型与状态归属

公共类只承载稳定、重复使用的语义。不要建立 `BaseScreen`、`BaseViewModel`、万能 `UiManager` 或装下全部业务字段的 `CommonUiState`。

| 类型或职责 | 归属与约束 |
| --- | --- |
| `AppSpacing`、`AppSizes`（已有） | `ui/theme`；只描述视觉配置，不持有 Session、Context 或页面数据 |
| `UiDensity`、`UiNoticeTone`（目标） | `ui/model`；组件共享的有限变体，随实际需求引入 |
| `UserInfoCardData`（已有） | 迁入 `patterns/user`；唯一用户卡显示模型，不替代领域 `UserProfile` |
| `toUserInfoCardData()`（已有） | 与用户显示模型同域；供多个入口转换，不在各页面复制字段拼装 |
| `<Feature>UiState` | 对应 `ui/feature/<业务>`；保留该功能所需数据，不为了统一外观合并业务状态机 |
| 可复用交互状态持有者 | 仅对多个调用方确实共享的 UI 行为抽取；生命周期由宿主明确拥有 |
| 一次性反馈和系统能力 | Route / 应用宿主处理导航、提示、文件选择和权限；组件通过回调表达意图 |

状态统一的是展示和处理规则：首次请求没有内容时用全屏状态组件；刷新失败但已有内容时保留内容并用 `AppNotice`；加载更多失败留在列表尾部；后台刷新不重置整个页面。数据是否为空、是否有权限以及错误文案由 ViewModel/映射层判定，公共组件只展示。

列表页码、筛选条件、阅读位置等保留现有恢复机制，不能因为抽组件重新创建状态持有者。列表稳定 key、滚动状态以及输入焦点不得随视觉抽取被重置。

## 7. 公用工具与环境能力

### 7.1 按责任存放

| 能力 | 规则 |
| --- | --- |
| 数字、时间、文件大小的显示格式 | 按语义命名纯函数；先检索现有 `util/TimeFormat.kt`、`formatViews` 等，避免再次实现。只统一同一显示规则 |
| 主题色与源站颜色转换 | 位于主题或相应显示域；沿用 `sourceTitleColor`、`onColorFor` 等现有入口，明确主题和兜底输入 |
| Modifier 扩展 | 放 `ui/util`，只做可组合的视觉/语义操作，不暗中导航、请求数据或操作账号 |
| 内容解析和布局缓存 | 保持在 `ui/content` 对应责任中，保留容量和失效规则；主题、字体变化必须使相关缓存正确更新 |
| 链接打开 | 统一使用现有 `LocalLinkHandler` 或显式回调；Route/宿主落实站内外导航策略 |
| 日期计算、文本转义等跨层规则 | 沿用所属 `common` / `util`；不为 UI 目录整齐复制另一份 |
| WebView、文件选择、剪贴板等平台操作 | 通过已有平台适配或宿主回调，不伪装为无状态工具函数 |

工具文件按用途命名，如 `DisplayNumberFormat.kt`、`ColorContrast.kt`；不新增承载所有内容的 `UiUtils.kt`、`CommonHelper.kt`。只有被复用或本身代表公共规则的能力才抽取，不为凑目录拆出大量一次性包装。

### 7.2 CompositionLocal 的边界

CompositionLocal 用于广泛消费、低频变化的环境配置，例如主题尺寸、链接处理和正文偏好。单个页面的查询条件、加载错误、表单值、提交回调不放进去。根部负责提供值，预览给出可用默认值或替身，不要求连接真实账号。

现有 `LocalUserSummaryRepository` 属于带数据能力的交互头像容器。迁移时将查询、生命周期与纯展示分离；不要把该模式推广到所有公共组件，也不另建全局可变单例替代它。

## 8. 新增页面和组件的开发流程

1. 确定界面语义，并检索现有入口、调用方和本规范目录。可用 `rg -n 'AppMenuRow|AppNotice|AppConfirmDialog|EmptyBox|UserInfoCard' app/src/main/java/sb/linux/client/ui` 起步。
2. 将界面分成公共布局、跨页业务模式、当前业务内容；Route 处理接线，Screen 只做状态分支与组件组合。
3. 已有组件足够时直接使用。存在差异时优先增加语义变体或 slot，同时检查现有调用方不变；不要复制整段组件后换名字。
4. 公共能力缺失时，组件与首个调用方同批实现并可编译；若已有重复实现，同批替换至少两个实际调用方，才证明重复已收敛。
5. 只使用一次且具有独有业务含义的布局留在功能域。例如红包内容、聊天气泡、投票和复杂编辑器可以有专有组件，但必须复用主题与通用反馈。
6. 若必须保留独立布局，在变更说明中写清语义差异、为什么现有组件不适用及所复用的基础能力，无需额外审批流程。
7. 更新本文的公共入口表、调用示例或迁移记录中受到本批影响的部分，保持唯一推荐入口。

基础组件可以使用 Material 原语，页面内的业务组件也可以继续组合公共组件。禁止的是每个功能各自维护一份相同视觉与交互规则，而不是禁止创建有明确职责的 Composable。

## 9. 渐进迁移与验收

### 9.1 迁移顺序

| 批次 | 工作内容 | 可核对的结果 |
| --- | --- | --- |
| A：统一视觉值 | 从主题、菜单、提示、摘要提取尺寸和 defaults | 页面读取公共角色，同类组件不再各写一份值；原视觉差异有记录 |
| B：收敛基础组件 | 分组、菜单、状态、提示、确认、开关优先 | 原调用方改用唯一入口，禁用、重试和回调语义保留 |
| C：提取外壳与模式 | 设置页、用户卡、摘要、原站页面外壳 | 页面保留业务组合；NavController 和请求不进入基础展示组件 |
| D：整理归属 | 将稳定入口移入目标目录，修正导入 | 新位置唯一维护，旧适配仅转发并列明剩余调用方 |
| E：清理与最终验收 | 删除无调用的旧适配、重复样式和实现 | 组件目录与文档一致，完成规定的最终设备验收 |

每批记录修改入口、调用方、保留行为、检查结果和待验收项；从可恢复的差异开始，不覆盖工作区已有修改。每个手写源码文件保持不超过 **250 物理行**，按职责拆分，不压缩排版凑行数。

### 9.2 结构检查与视觉验收分开

按现有整体重构约定，结构迁移期间执行编译、静态检查、引用检查和代码审查，不运行具体业务测试、不要求阶段性账号操作；结构改造完成后统一实机验收。只修改本文不需要运行 Gradle。

实施时可使用以下检查，但应记录实际执行结果：

- `gradlew.bat :app:compileDebugKotlin`：发现类型、导入及组合调用问题。
- `git diff --check`：检查变更格式；结合脚本扫描本批源码的 250 行约束。
- 检索 `AlertDialog`、`RoundedCornerShape`、`Color(0x`、`Scaffold` 等位置，审查是否属于基础实现、已登记专有模式或重复实现。命中本身不是错误，不机械禁止合法 Material 用法。
- 检查公共基础组件没有新增 Session、NavController、data、repository 依赖；检查旧入口及旧实现是否仍有调用。

最终设备验收覆盖本轮实际改动的公共组件及其代表页面：浅色/深色、用户字号与字体、长文案、手机/平板、背景图与透明度、正常/空/错误/忙碌/禁用状态。交互核对返回栈、列表位置、重试、整行开关、重复提交和可访问性语义。使用相同配置对比同语义组件；预览或编译不能代替设备结果。

### 9.3 完成条件

- 本批同语义重复实现已收敛到公共入口，或者确有业务差异并明确保留。
- 页面没有重新声明一套主题和交互规则；公共组件、类、工具归属明确且没有反向依赖。
- 原有文案、动作、权限、状态恢复、源站语义及数据兼容要求得以保留；实际变化已单独说明。
- 相关编译和结构检查已记录；尚未进行的设备验收明确标为待完成，不声称业务行为已验证。
- 整体迁移结束时，完成最终设备验收并清除无用旧实现；验收前只能声明相应结构批次完成。

## 10. 开发和审查清单

- [ ] 是否检索并使用了已有公共组件，而非复制同一段界面？
- [ ] 新增抽象是否有明确职责和实际调用，是否仍可用现有参数或 slot 解决？
- [ ] 同语义颜色、字体、间距、形状和禁用状态是否由唯一入口维护？
- [ ] 展示组件是否只依赖显示数据、环境配置和回调，副作用是否留在对应宿主？
- [ ] 新增公共类是否避免复制领域模型、万能基类和不必要的全局状态？
- [ ] 工具函数是否复用既有规则，是否按用途命名并明确状态和缓存归属？
- [ ] 加载、刷新失败、空态、登录门禁是否使用正确的公共展示且保留内容状态？
- [ ] modifier、点击语义、大字体、忙碌与禁用行为是否在组件层统一处理？
- [ ] 本批源码行数、编译、引用与差异检查是否已完成并如实记录？
- [ ] 文档是否区分已有 API、目标 API 和尚未完成的设备验收？

## 11. 迁移记录

### 批次 A：统一视觉值（2026-10-20，结构批次）

新增入口全部位于 `ui/theme/`，均为视觉值归属，不持有 Session、Context、导航或数据能力：

| 入口 | 内容 | 实际调用方 |
| --- | --- | --- |
| `AppSpacing` / `LocalAppSpacing` | `inline` 4、`item` 8、`section` 16、`pageHorizontal` 16、`pageVertical` 12 | `AppSection`、`ReplySummaryCard` |
| `AppSizes` / `LocalAppSizes` | `icon` 22 | `AppMenuRow` |
| `AppMenuDefaults` | 卡片圆角 18、分组标签内边距 12/10/2、行内边距 16/12 与 12/8、行间距 12、标题与说明间距 3、禁用透明度 0.38 | `AppGroupLabel`、`AppSection`、`AppMenuRow` |
| `AppNoticeDefaults` | 圆角 14、内容内边距 14/6/4/4 | `AppNotice` |
| `AppSummaryDefaults` | 圆角 18、外层 14/5、内容内边距 14、行间距 6 | `ReplySummaryCard` |
| `AppStateDefaults` | 外边距 24、图标容器 56、图标 26、圆形底透明度 0.6、间距 12/10、按钮胶囊 | `ErrorBox`、`EmptyBox`、`LoginRequiredBox` |
| `AppPaginationDefaults` | 胶囊高 40、图标 20、上下边距 10、页码边距 6、禁用透明度 0.3 | `PaginationBar` |

`LsbTheme` 增加 `LocalAppSpacing`、`LocalAppSizes` 注入；未被装配时回退到基准值，预览可直接渲染。

**保留行为**：数值逐项与迁移前一致，本批不改变任何视觉或交互。`ReplySummaryCard` 标题行的 8dp 统一读 `AppSpacing.item`，不再另立同名常量；`AppNotice` 与摘要卡片的圆角各自持有，即使当前数值接近也不共用。

**有意未纳入本批**：

- `AppSizes.avatar`：现有 `Avatar(size: Int = 40)` 用 `Int` 表达 dp，改为 `Dp` 属接口变更，随用户卡批次（C）处理。
- `AppSizes.minimumTouchTarget`：补足 48dp 触控区会改变交互与版式，按 4.1 要求单独成批；分页触控区当前仍为 40dp，已在 `AppPaginationDefaults` 注释中标注。

**检查结果**：`:app:compileDebugKotlin` 成功，无 error 与相关 warning；`git diff --check` 通过；本批 12 个文件均不超过 250 物理行；4 个公共组件内已无散写 `dp`、`RoundedCornerShape` 与 `Color(0x`；`ui/theme` 仅依赖 Compose。

**待验收**：浅色/深色、用户字号与字体缩放下，菜单、提示、摘要、分页与三种状态占位的实机观感（按 9.2，结构批次内不以编译代替设备结果）。

### 批次 B：收敛基础组件（2026-10-20，结构批次）

**新增唯一入口**

| 入口 | 职责 | 收敛掉的重复实现 |
| --- | --- | --- |
| `AppSwitchRow` | 设置开关行：标题、说明、右置开关、整行可点 | 原 `SwitchRow` 与三处表单内联开关行 |
| `AppUpdatePromptDialog` | 「发现新版本」提示：版本号、发布说明摘要、前往下载/取消 | 关于页与常规设置页各写一份的相同 `AlertDialog` |

**接口补齐（5.2 第 1 条）**：`LoadingBox`、`ErrorBox`、`EmptyBox`、`LoginRequiredBox`、`PaginationBar`、`AppMenuRow`、`AppConfirmDialog` 增加 `modifier: Modifier = Modifier`，均作用于组件根部一次。

**调用方替换（8.4）**

| 原实现 | 现入口 | 替换点 |
| --- | --- | --- |
| `Box(Modifier.padding(...)) { LoadingBox() }` 一类包装 | `LoadingBox(modifier = …)` / `ErrorBox(…, modifier)` / `EmptyBox(…, modifier)` / `LoginRequiredBox(…, modifier)` | 11 处：`BlockWordsScreen`、`ChatScreen`、`NotificationsScreen`、`IdentityCenterScreen` |
| 关于页、常规设置页的更新提示 `AlertDialog` | `AppUpdatePromptDialog` | 2 处 |
| 图片长按「保存图片」`AlertDialog` | `AppConfirmDialog` | 1 处 |
| 前置开关的表单行（抽奖、发卡、资料字段） | `AppSwitchRow(compact = true)` | 3 处 |
| `SettingsComponents.SwitchRow` | 转发到 `AppSwitchRow` | 约 25 个设置页调用方无需改动 |

**保留行为**：禁用、重试、回调与整行点击语义不变。`ErrorBox` 的 `onRetry` 仍为末位参数，尾随 lambda 调用不受影响；`PaginationBar` 的 `onPage` 仍为末位参数，仅把一处位置传参改为具名。

**保留的合法例外（5.2 第 9 条，不硬塞 `AppConfirmDialog`）**

| 位置 | 原因 |
| --- | --- |
| `NewTopicDialogs` 恢复草稿 | 关闭与「丢弃」触发的副作用不同，一个入口无法表达 |
| `NewTopicDialogs` 保存并退出 | 三个按钮 |
| `NewTopicDialogs` 发布确认 | 可滚动富文本 + 忙碌态文案 |
| `navigation/AppRoot` 更新提示 | 三个按钮并含「不再提示」逻辑，与两处简单提示不同 |
| `DohSettingsScreen`、`TopicFloorJumpDialog`、`TopicCollectionComponents` | 表单类弹窗（输入框、复选） |
| `GachaPullResultDialog` | 结果展示 |
| `TopicScrollControls` | 目录列表 |
| `AppLinkProvider` | 链接预览富内容 |

**检查结果**：`:app:compileDebugKotlin` 在 `--rerun-tasks` 全量重编下成功，无 error，仅两条改动前既有 deprecation warning；`git diff --check` 通过；本批文件均不超过 250 物理行；`ui/` 下基础组件只依赖 `ui.theme`；`screens/` 内已无裸 `Switch` 实现。

**待验收**：开关行统一为「右置开关 + 整行可点」，抽奖、发卡、资料字段三处由左置改为右置，属可见的版式与交互变化，需在批次 E 实机验收中确认表单密度与点击手感；其余替换为等价重构。

### 批次 C：提取外壳与模式（2026-10-20，结构批次）

**新增唯一入口**

| 入口 | 职责 | 收敛掉的重复实现 |
| --- | --- | --- |
| `AppPageScaffold` | 二级页外壳：标题、返回、`actions`、`PaddingValues` 内容槽 | 约 30 个页面各自手拼的 `Scaffold + TopAppBar + ArrowBack` |

`AppPageScaffold` 不接收 `NavHostController`，也不内建滚动：普通 `Column` 与长列表分别消费 `PaddingValues`，避免外壳套滚动导致列表嵌套或恢复位置丢失。

**调用方替换（8.4）**

| 原实现 | 现入口 | 说明 |
| --- | --- | --- |
| `BlockWordsScreen` 手拼顶栏 | `AppPageScaffold(title, onBack)` | 无 actions 形态 |
| `IdentityCenterScreen` 手拼顶栏 + 刷新按钮 | `AppPageScaffold(title, onBack, actions)` | `actions` 槽形态；`return@Scaffold` 标签同步改为 `return@AppPageScaffold` |
| `AccountPageGate`（原站页面登录门禁）手拼顶栏 | `AppPageScaffold(title, onBack)` | 门禁内部仍用 `LoginRequiredBox`，未登录分支行为不变 |
| `SettingsPage` 自带 `Scaffold` | 转发到 `AppPageScaffold` | 保留设置页「整页滚动 + 命名页边距」约定（间距读 `LocalAppSpacing`，见批次 F）；5 个设置页调用方无需改动 |

`SettingsPage` 作为设置页适配仍接收 `nav`，但只用于默认返回动作；基础外壳不再持有导航能力。

**用户卡与摘要的核对结果**（本批不改代码，只确认归属）

| 组件 | 结论 |
| --- | --- |
| `UserInfoCard` | 已符合：只接收 `UserInfoCardData` 与 `onProfile` 回调，无 nav、无 repository |
| `UserSummaryContent` | 查询与生命周期在容器内，展示部分独立 |
| `UserAvatar` | 仍按 7.2 保留为「带数据能力的交互头像容器」，读取 `LocalUserSummaryRepository` 与 `LocalLinkHandler`；不再向其它组件推广该模式 |
| `ReplySummaryCard` | 已符合：数据 + 回调，无导航 |

**有意未纳入本批：`AppSizes.avatar`**。现有 `Avatar(size: Int = 40)` 以 Int 表达 dp，且 13 处调用全部显式传入尺寸（15–64）。此时登记一个只承担默认值的 token 不会形成唯一来源，反而成为没有消费方的抽象（见第 10 节清单）；`AppSizes` 保持只登记 `icon`。Int→Dp 的签名清理属独立的类型整理，不和外壳提取混做。

**检查结果**：`:app:compileDebugKotlin` 在 `--rerun-tasks` 全量重编下成功，无 error、无本批相关 warning；`git diff --check` 通过；本批文件均不超过 250 物理行；`AppPageScaffold` 只依赖 Compose，`ui/` 下已无接受 `NavHostController` 的基础组件。

**待验收**：顶栏视觉与返回行为在四种形态（无 actions、有 actions、门禁、设置页滚动）下的一致性，随批次 E 实机验收。

### 批次 D：整理归属（2026-10-20，结构批次）

**目录与 package 迁移**

| 原位置（`sb.linux.client.ui`） | 新位置 |
| --- | --- |
| `AppMenuComponents.kt` | `ui/components/layout/AppMenuComponents.kt`（`sb.linux.client.ui.components.layout`） |
| `AppNotice.kt`、`AppConfirmDialog.kt` | `ui/components/feedback/` |
| `ScreenStateComponents.kt` 的四种状态占位 | `ui/components/feedback/ScreenStateComponents.kt` |
| `ScreenStateComponents.kt` 的 `PaginationBar` | 拆出 `ui/components/navigation/PaginationBar.kt` |
| `AppPageScaffold.kt` | `ui/components/navigation/AppPageScaffold.kt` |
| `AppSwitchRow.kt` | `ui/components/input/AppSwitchRow.kt` |

**引用改写**：13 个公共入口的全限定引用（`sb.linux.client.ui.X` → 新包）批量改写，涉及 42 个文件；33 个使用 `import sb.linux.client.ui.*` 的页面补充 `components.feedback.*`、`components.layout.*`、`components.navigation.*` 三条通配导入。旧通配保留，用于仍留在根包的类型（`Avatar`、`HtmlContent`、`TopicCardView`、`ThemeProvider` 等）。

**未纳入本批**

| 项目 | 原因 |
| --- | --- |
| `patterns/`、`content/`、`model/`、`util/` 的搬迁 | 涉及 `HtmlContent`、`MarkdownText`、`TableView`、`UserInfoCard` 等 30 余文件与更大引用面，需单独一轮按簇迁移并逐批编译 |
| `AppUpdatePromptDialog` | 目标目录未定义（现有 `patterns/` 下 settings/user/topic/web 四类都不贴切），暂留 `ui/` 根，待目录约定明确 |
| 约 30 个页面仍手拼顶栏 | 属批次 C 的后续替换，与目录搬迁分开 |

**检查结果**：全量重编成功；`git diff --check` 通过；旧根包 13 个入口无残留引用；`ui/theme`、`ui/components` 全部文件 ≤250 物理行；14 个公共入口均有真实调用方（`AppConfirmDialog` 17 处、`LoadingBox` 23 处……`AppMenuGroup` 1 处）。

### 批次 E：清理与最终验收（结构清理完成，实机验收待执行）

**已清理**

- `SettingsComponents.SettingIcon`：0 个调用方，删除；随之失效的显式导入（`Modifier`、`dp`、`Alignment`、`Color`、`FontWeight`、`clickable`、`KeyboardArrowRight`）一并移除。
- 修正 `ResetAction` 上错位的「字节大小」注释。

**结构一致性核对**

- 第 2 节入口表路径已与迁移后的目录一致（6 处更新）。
- 14 个公共入口均有真实调用方；2026-10-20 初验发现的 `AppSpacing.section/pageHorizontal/pageVertical` 无消费方问题已在批次 F 修复，2026-10-21 复验通过（`AppSizes.avatar` 未登记，见批次 C 说明）。
- 全量重编通过，`git diff --check` 通过。

**未完成的最终验收（9.2，必须在设备上执行）**

覆盖浅色/深色、用户字号与字体、长文案、手机/平板、背景图与透明度、正常/空/错误/忙碌/禁用状态，以及返回栈、列表恢复位置、重试、整行开关、重复提交与可访问性语义。本轮有三处已知可见变化点需要在验收时重点确认：

1. 开关行由左置改为右置（抽奖、发卡、资料字段三处）。
2. `AppPageScaffold` 统一顶栏后，四种形态（无 actions / 有 actions / 登录门禁 / 设置页滚动）的视觉一致性。
3. `EmptyBox` 仍被若干页面放在 `LazyColumn` 的 `item {}` 内，该位置的高度约束不完整；问题在迁移前已存在，本轮未改动，验收时应确认是否改为按内容高度布局。

**声明边界**：以上三项及全部状态组合均未在设备上验证，因此当前只能声明结构批次完成，不能声称业务行为已验证。

**实机验收的执行条件核查（2026-10-20）**

| 项目 | 结果 |
| --- | --- |
| `:app:assembleDebug` | 成功。产出 `LinuxSB-v1.0.71-armv7a.apk`（29.1 MB）、`-armv8a.apk`（32.0 MB）、`-universal.apk`（58.3 MB），可直接侧载验收 |
| `:app:testDebugUnitTest` | 编译期失败，**与本次 UI 迁移无关**：`app/src/test` 仍引用 `GachaCatalogHtmlParser`、`GachaOperationHtmlParser`，这两个类在本次未提交的重构中已从生产代码删除；本轮未改动 `app/src/test`。需由该重构的负责人决定是修测试还是确认删除 |
| 模拟器验收 | 当前环境不可行。`app/build.gradle.kts` 的 ABI splits 只含 `armeabi-v7a` / `arm64-v8a`（另出 universal，同样只有 ARM），而本机 AVD `Pixel_10` 是 `x86_64` 镜像，APK 不含 x86_64 原生库。要做就要有 ARM 设备／ARM 系统镜像，或临时加 x86_64 ABI（会牵连 resvg、JNA 的原生依赖） |

**因此 E 的可自动化部分到此为止**：结构清理与编译、APK 产物、失败面归因都已完成并记录；9.2 的设备项需要在能运行该 APK 的环境上侧载上述产物执行，本文不代其结论。

### 批次 F：验收问题修复（2026-10-21，结构批次）

来源：[UI 公共层验收记录](UI_ACCEPTANCE_REPORT.md) 的两项 P2 未满足项。

**问题 1 · 开关行没有形成完整开关语义（§5.2 第 7 条）**

| 改动 | 内容 |
| --- | --- |
| `components/input/AppSwitchRow.kt` | 整行改由 `Modifier.toggleable(value, enabled, role = Role.Switch, onValueChange)` 提供语义：名称、`checked`、`enabled` 与切换动作落在同一个可操作节点；内部 `Switch` 改为 `onCheckedChange = null` 并 `Modifier.clearAndSetSemantics { }`，只承担视觉 |
| `components/layout/AppMenuComponents.kt` | `AppMenuRow` 的 `onClick` 改为可空且默认 `null`，`null` 时不挂 `clickable` |

`onClick` 可空是为了避免验收记录明确排除的做法——「只给现有父行再叠加一个点击处理器」。整行现在只有一个点击来源（`toggleable`），内部开关不再自带动作，因此点击整行或开关区域都只切换一次。现有 4 个 `AppMenuRow` 调用方全部传 `onClick`，行为不变；不传 `onClick` 时也不会显示可点击指示（默认尾部箭头仅在无 `trailing` 时出现，`AppSwitchRow` 始终提供 `trailing`）。

**问题 2 · 设置页间距未接入公共来源（§4.1 单一视觉来源）**

`SettingsPage` 改读 `LocalAppSpacing.current`：`pageHorizontal`、`pageVertical`、`section`。三个字段因此都具有真实消费方；默认值 16 / 12 / 16，与迁移前视觉一致，同时移除了不再使用的 `dp` 导入。

**检查结果**：`:app:compileDebugKotlin` 成功，无 error。

**未完成的复验（需设备）**：本次会话没有连接设备（`adb devices` 为空），验收记录要求的实机复验未执行。复验步骤与判据：

```bash
adb install -r app/build/outputs/apk/debug/LinuxSB-v1.0.71-armv8a.apk
# 我的 → 设置 → 阅读与交互
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml ui.xml
```

判据：每个开关只有一个可访问操作节点，该节点同时具备名称（标题与说明）、`checkable="true"` 与当前状态、以及点击动作；行内 `Switch` 不再作为独立可勾选节点出现；点击整行或开关区域各只切换一次；禁用时不可操作。间距判据：修改 `AppSpacing` 默认值后设置页边距随之变化，恢复默认时与迁移前一致。

**声明边界**：以上两条为源码修复与编译验证；验收记录 §2 的两项判据尚未在设备上复验，不能声称已通过。

**2026-10-21 后续复验更新**：设备连接后，已安装并核对当前 APK，完成两项问题的源码复核及开关语义、行/开关区域点击、禁用、代表页面浅深色和返回检查。两项 P2 已关闭，测试开关及主题恢复原值；详情及实机 XML、截图见 [验收记录第 5 节](UI_ACCEPTANCE_REPORT.md#5-定向复验2026-10-21)。完整设备矩阵仍按 §9.2 保留待验收。

### 批次 G：外壳替换续做（2026-10-21，结构批次）

两项验收问题关闭后，继续把二级页的手拼顶栏接到 `AppPageScaffold`（批次 C 的续做）。

**本批替换（8 个页面源码文件）**：`AboutScreen`、`BrowseSettingsScreen`、`CheckinScreen`、`ExportedTopicsScreen`、`GeneralSettingsScreen`、`InviteCenterScreen`、`ReportScreen`、`UserByNameScreen`。加上批次 C 的 4 个，目前 12 个文件使用该外壳。`CheckinScreen` 为当前未接线的旧页面，不代表需要新增手动签到入口，也不计为本次用户可达页面。

**外壳增强**：`AppPageScaffold` 的标题改为 `maxLines = 1` + `TextOverflow.Ellipsis`。依据是 `UserByNameScreen`、`CollectionDetailScreen`、`TopicCollectionManageScreen` 三处原本各自传同一组参数——这属于同一个顶栏约定，应由外壳拥有；现有调用方标题都较短，默认视觉不变。

**顺带修正**：`ReportScreen` 的 `return@Scaffold` 标签改为 `return@AppPageScaffold`（原因与批次 C 的 `IdentityCenterScreen` 相同：标签跟随被调用的组合函数名）。

**仍未替换的页面**

| 分类 | 页面 | 说明 |
| --- | --- | --- |
| 可继续替换 | `AiSettingsScreen`、`CollectionDetailScreen`、`DohSettingsScreen`、`FavoritesScreen`、`FootprintScreen`、`NetworkSettingsScreen`、`NotificationsScreen`、`ThemeSettingsScreen`、`TopicCollectionManageScreen`、`TransferSettingsScreen` | 形态同为「标题 + 返回（+ actions）」，可直接套用 |
| 形态不同 | `ChatScreen`、`DirectMessagesScreen`、`TopicScreenScaffold`（bottomBar）；`UserScreen`（FAB + containerColor）；`DataManagementScreen`（containerColor）；`WebScreens`（标题自定义字体样式） | 需要外壳扩展对应能力后再迁移，本轮不引入未使用的参数 |
| 需要新形态 | `TopicCollectionsScreen` | 返回按钮按 `showBack` 条件显示，外壳目前总是渲染返回；若要迁移需先定义「无返回」形态 |
| 非二级页 | `LoginScreen`、`MeScreen`、`LeaderboardScreen`、`AppContent` | tab 根或导航宿主，本就不属于带返回的二级页外壳 |

**检查结果**：`:app:compileDebugKotlin` 成功，无 error；`git diff --check` 通过；本批 9 个文件均不超过 250 行（最大 197 行）；`AppPageScaffold` 仍只依赖 Compose。

**2026-10-21 验收更新**：本批代码检查、编译和打包通过；关于、阅读设置、应用与账号、导出列表四个可访问页面完成浅深色与返回核对，阅读/设置页补查系统字体 1.3 倍，测试字体与主题已恢复。邀请、举报、用户名中转页本次仅静态审查，真实长标题尚待设备验证；签到按自动流程处理，不要求手动入口。详见 [验收记录第 6 节](UI_ACCEPTANCE_REPORT.md#6-批次-g-外壳迁移验收2026-10-21)。

### 批次 H：外壳替换收尾（2026-10-21，结构批次）

承接验收记录第 6 节「尚余工作」中列出的 10 个同形态二级页，全部接入 `AppPageScaffold`。

| 页面 | 顶栏构成 |
| --- | --- |
| `AiSettingsScreen` | 标题随 `section` 变化；返回走 `exit` |
| `CollectionDetailScreen` | 动态专辑标题；管理／订阅／刷新 actions |
| `DohSettingsScreen` | 固定标题；添加服务器 action |
| `FavoritesScreen` | 固定标题；同步／清空 actions（同步按 tab 条件显示） |
| `FootprintScreen` | 固定标题；清空 action（列表为空时禁用） |
| `NetworkSettingsScreen` | 固定标题；返回走 `exit` |
| `NotificationsScreen` | 固定标题；标记已读／刷新 actions |
| `ThemeSettingsScreen` | 标题随 `section` 变化 |
| `TopicCollectionManageScreen` | 动态标题；刷新 action |
| `TransferSettingsScreen` | 标题随 `cloud` 变化 |

`CollectionDetailScreen` 与 `TopicCollectionManageScreen` 原本各自传 `maxLines = 1, overflow = Ellipsis`，现由外壳统一提供，页面不再重复这组参数。

结果：22 个文件使用该外壳（批次 C 4 个 + G 8 个 + H 10 个）。`screens/` 内仍自建 `Scaffold` 的只剩 10 个文件，全部属于形态不同或非二级页。

**本批时点仍未迁移的 10 个：需要先定外壳职责，不是待套用**（后续处理以批次 I 为准）

| 需要的能力 | 页面 | 说明 |
| --- | --- | --- |
| `bottomBar` | `ChatScreen`、`DirectMessagesScreen`、`TopicScreenScaffold` | 输入栏与玻璃底栏；是否由公共外壳拥有需要单独决策 |
| `containerColor` | `DataManagementScreen`、`UserScreen` | 自定义表面色 |
| FAB | `UserScreen` | 悬浮动作按钮 |
| 可空 `onBack`（无返回形态） | `TopicCollectionsScreen` | 返回按钮按 `showBack` 条件渲染 |
| 自定义标题字体 | `WebScreens`（两处） | 标题使用 `titleMedium` |

这些页面各自只对应 1—3 个调用方，按 §5.2 第 3、10 条，先决定公共外壳是否应该拥有底栏／FAB／表面色，再决定是否加参数；未做该决策前不引入未使用的参数。

**检查结果**：`:app:compileDebugKotlin` 成功，无 error；`git diff --check` 通过；本批 10 个文件均不超过 250 行（最大 213 行）。

**2026-10-21 复验**：AI、网络、DoH、外观、历史与游客收藏门禁完成当前手机浅色下的标题、内容和返回抽查。其余页面、深色、完整 actions/禁用态和长动态标题仍未完整覆盖；不把 10 个页面全部记为实机通过。详见验收报告第 7 节。

### 批次 I：外壳能力收口（2026-10-21，结构批次）

**外壳接口收敛**：`AppPageScaffold.onBack` 改为可空。`null` 表示该页当前不渲染返回图标（标题随之左移），用于「按状态条件显示返回」和「本就无返回」的页面。这是本批唯一新增的语义变体，有 3 个真实调用方。

**本批替换（5 个页面）**

| 页面 | 形态 |
| --- | --- |
| `TopicCollectionsScreen` | 返回按 `showBack` 条件显示，嵌在宿主 tab 里时为 `null` |
| `DirectMessagesScreen` | 同上，返回按 `showBack` 条件显示；actions 保留拉黑名单与刷新 |
| `LeaderboardScreen` | 始终无返回（沿用系统返回），`onBack = null` |
| `DataManagementScreen` | 普通「标题 + 返回」 |
| `UserScreen` | 「标题 + 返回 + 用户屏蔽／私信 actions」 |

结果：27 个文件使用该外壳；`screens/` 内仍自建 `Scaffold` 的只剩 5 个。

**撤回三个曾计划的能力参数（记录决策）**

初判认为需要 `containerColor`、`floatingActionButton`、`bottomBar`，逐个核对调用方后发现都不成立：

| 参数 | 核对结果 |
| --- | --- |
| `containerColor` | `DataManagementScreen` 里的 `containerColor` 是 `ListItemDefaults.colors(...)` 的参数，与页面底色无关 |
| `floatingActionButton` | `UserScreen` 的「回到顶部」是叠在内容区内的 `SmallFloatingActionButton`，不经过 `Scaffold`，迁移后仍留在内容里 |
| `bottomBar` | `ChatScreen` 的底栏是表情＋输入框＋发送的整块输入组合，且顶栏标题是「头像＋可点击用户名」的富内容，`title: String` 表达不了；`TopicScreenScaffold` 的 `topBar` 实际是 `TopicSearchOverlay` 搜索浮层、底栏是帖子回复栏，都不是普通顶/底栏 |

因此三个参数全部撤回，外壳保持 5 个参数（`title`、`onBack`、`modifier`、`actions`、`content`）。按 §5.2 第 3、10 条，不保留没有调用方的能力开关。

**仍保留独立外壳的 5 个页面**

| 页面 | 原因 |
| --- | --- |
| `ChatScreen` | 顶栏标题为头像＋可点击用户名的富内容；底栏为输入组合 |
| `TopicScreenScaffold` | 顶栏是搜索浮层，底栏是回复栏，并带玻璃背景层 |
| `WebScreens`（两处） | 标题使用 `titleMedium`，与原站 Web 通道逻辑绑定 |
| `LoginScreen`、`MeScreen` | tab 根，挂在应用底栏下，不是带返回的二级页 |

**检查结果**：`:app:compileDebugKotlin` 成功，无 error；`git diff --check` 通过；本批 6 个文件均不超过 250 行（最大 183 行）；外壳仍只依赖 Compose。

**2026-10-21 复验**：私信/淘帖 tab 根的返回图标正确隐藏；榜单无顶栏返回且系统返回正常；数据清理/备份页及用户主页的标题与返回通过真机抽查。条件返回的独立二级入口形态和全部颜色/字号组合未完整覆盖。详见验收报告第 7 节。

### 批次 J：目录归属（patterns 与 util，2026-10-21，结构批次）

承接批次 D 的遗留项，迁出用户与帖子两类跨页模式。

| 原位置（`ui/`） | 新位置 |
| --- | --- |
| `Avatar.kt`、`UserAvatar.kt`、`UserInfoCard.kt`、`UserInfoCardData.kt`、`UserSummaryContent.kt` | `ui/patterns/user/` |
| `TopicCardView.kt`、`ReplySummaryCard.kt`、`TopicCardTags.kt`、`PinnedTopicCapsule.kt` | `ui/patterns/topic/` |

**顺带归位两个放错位置的声明**（原先都寄放在 `Avatar.kt` 里）：

| 声明 | 新归属 | 理由 |
| --- | --- | --- |
| `CardColorOverrides`、`LocalCardColorOverrides` | `ui/patterns/topic/TopicCardColors.kt` | 只被帖子卡片及其颜色预览使用，与头像无关 |
| `onColorFor` | `ui/util/ColorContrast.kt` | 纯颜色函数，7.1 举的正是 `ColorContrast.kt` 这个文件名 |

**引用改写**：10 个符号的全限定引用批量改写（19 个文件）；33 个使用 `import sb.linux.client.ui.*` 的文件补上 `patterns.user.*`、`patterns.topic.*` 两条通配导入；搬迁文件按目录重写 `package` 声明（7 个文件因行尾是 CRLF，首轮替换未命中，已单独修正并复查）。

**本批时点批次 D 仍需处理的部分**（content/web/theme 已由批次 K 接续）

| 目录 | 待迁文件 |
| --- | --- |
| `content/` | `HtmlContent`、`HtmlBlockParser`、`HtmlBlockParserSupport`、`HtmlInlineParser`、`HtmlContentCache`、`MarkdownText`、`MarkdownParser`、`MarkdownEditorTools`、`BodyImage`、`TableView`、`CodeBlockView`、`EmbeddedVideo` 系列、`ImagePager`、`ZoomableImageViewer`、`ContentBlockModels`、`ContentHeadingStyle`、`LinkText`、`TextSearchAnnotations` |
| `patterns/web` | `VerificationDialog`、`VerificationRegionScript` |
| `AppUpdatePromptDialog` | 目标目录仍未定义（`patterns/` 现有四类都不贴切） |
| `model/` | 暂无候选；`ContentBlockModels` 也可能留在 `content/` |

**检查结果**：`:app:compileDebugKotlin` 成功，无 error；`git diff --check` 通过；`patterns/` 下 10 个文件均不超过 250 行（最大 205 行）；新增 `ColorContrast.kt`（11 行）、`TopicCardColors.kt`（16 行）。

**2026-10-21 复验**：首页/用户主页帖子卡片、头像、用户弹层与用户卡完成代表显示和弹层进入主页的真机抽查；未专门配置卡片配色覆盖，也未覆盖全部主题。目录迁移的静态比较与本轮范围见验收报告第 7 节。

### 批次 K：目录归属（content、web 与 theme，2026-10-21，结构批次）

继续批次 D，把渲染内容、原站验证页与主题基础设施归入目标目录。

| 原位置（`ui/`） | 新位置 | 文件数 |
| --- | --- | --- |
| `HtmlContent`、`HtmlBlockParser`、`HtmlBlockParserSupport`、`HtmlInlineParser`、`HtmlContentCache`、`MarkdownText`、`MarkdownParser`、`MarkdownEditorTools`、`BodyImage`、`TableView`、`CodeBlockView`、`EmbeddedVideo` 系列、`ImagePager`、`ZoomableImageViewer`、`ContentBlockModels`、`ContentHeadingStyle`、`LinkText`、`TextSearchAnnotations` | `ui/content/` | 21 |
| `VerificationDialog`、`VerificationRegionScript` | `ui/patterns/web/` | 2 |
| `Theme`、`ThemeProvider`、`NeutralPageColors`、`SourceTitleColors` | `ui/theme/` | 4 |

**引用改写方式**：本轮把"搬迁后补 import"从逐条试错改成一次性核对——先收集根包现有声明名与各子目录的声明名到包映射，再扫描所有 `import sb.linux.client.ui.<Name>`，凡是根包里已不存在的声明就按映射改写到新包。这一步共改写 8 条失效 import（`clearBodyImageLayoutCache`、`renderInline`、`PaletteStyles`、`isCustomColorKey`、`colorToKey`、`paletteStyleLabel` 等）。另有 33 个使用根包通配导入的文件各补一条 `content.*` / `theme.*` 通配。

**`ui/` 根目录现在只剩 3 个文件**：`AppUpdatePromptDialog.kt`、`BadgeComponents.kt`、`UiCompositionLocals.kt`。

| 文件 | 保留原因 |
| --- | --- |
| `AppUpdatePromptDialog.kt` | 目录约定仍缺位：现有 `patterns/` 四类（settings/user/topic/web）都不贴切，它是横跨设置与关于的版本提示 |
| `BadgeComponents.kt` | `Badge`、`TitleBadgeView` 同时被用户卡与帖子标签使用，`components/` 现有五个子目录（action/layout/feedback/input/navigation）都不匹配；新增 `components/display` 属于扩大目录约定，先不做 |
| `UiCompositionLocals.kt` | 应用级环境注入（链接处理、表格显示、UID、智能解码），按 7.2 由根部提供，不属于任何单一目录 |

**检查结果**：`:app:compileDebugKotlin` 成功，无 error；`git diff --check` 通过；本轮搬迁文件的最大行数为 225（`VerificationDialog`），均不超过 250 行。

**过程教训（记录，避免重复）**：批 D 与批 J 两次搬迁都遇到同一个坑——用 `^package sb\.linux\.client\.ui$` 做正则替换时，行尾是 CRLF 的文件匹配不到 `$`，结果文件搬了但包名没改。本轮改为按行数组直接改第一行，一次通过。

**2026-10-21 复验**：应用构建和代表正文段落/链接、用户卡及浅色页面展示通过；21 个 content 文件中 20 个去除包/导入/空行后与 HEAD 原位置正文一致，另一个包含前批确认框改动。完整 Markdown/表格/代码/图片查看/视频、验证弹窗和所有主题组合未实机覆盖。

**新增本地验证缺口**：`MarkdownParserTest`、`EmbeddedVideoParserTest` 的物理副本仍位于原包，缺少新的 content 引用；`:app:compileDebugUnitTestKotlin` 实际失败，未运行测试。两份文件已暂存删除且受忽略规则屏蔽，本轮不改动该状态；不能继续将当前全部测试编译错误归为先前抽卡重构。详见验收报告第 7 节及编译日志。
