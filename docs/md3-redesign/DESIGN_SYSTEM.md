# MD3 重设计 · 设计系统与公共组件

版本：2.0 / 2026-10-21；状态：待实施。返回 [总方案](../MD3_UI_REDESIGN.md)。  
本文件是新视觉值的唯一来源；[页面规格](SCREEN_SPECIFICATIONS.md) 负责组合，[实现计划](IMPLEMENTATION_AND_ACCEPTANCE.md) 负责迁移与验证。

## D01 · Token 层级与唯一入口

采用三层结构：参考值 → 语义角色 → 组件默认。页面只使用语义角色或组件默认，不复制参考值。

| 层 | 应承担的内容 | 建议归属 |
| --- | --- | --- |
| 参考值 | 静态色表、间距尺度、基础圆角 | `ui/theme/` |
| 系统角色 | ColorScheme、Typography、Shapes | 现有 `LsbTheme` 根部注入 |
| 产品角色 | 阅读字体、正向/警告状态、密度、阅读宽度 | `ui/theme/` 中明确命名的不可变类型 |
| 组件默认 | 菜单、状态块、确认框、分页、摘要的默认参数 | 现有 `App*Defaults` 或组件所属文件 |
| 业务模式 | 用户卡、帖子条目、阅读操作栏、原站壳 | `ui/patterns/` |
| 页面 | 状态适配、组合、回调接线 | `ui/screens/`、现有 feature 对应页面 |

续用 `AppSpacing`、`AppSizes`、`AppMenuDefaults` 等已有入口，按真实共享语义更新。新角色须有实际消费方，不为完整性创建空目录、全局工具大类或每个 Material 控件的同名包装。

基础 theme/components 不依赖 Session、ViewModel、NavController、repository。UI 模式通过数据与事件工作，Route 层注入现有能力。标准 Material3 组件可直接使用；当多处确需同一组合/行为时再抽取公共实现。[Compose Material3 的主题结构](https://developer.android.com/develop/ui/compose/designsystems/material3)

## D02 · 主题生成与颜色优先级

### 默认规则

新安装默认跟随系统浅深色；API 31+ 默认允许系统动态色，较旧系统回退蓝色静态方案。已有用户的主题模式、动态色选择、自定义种子和角色覆盖按迁移规则保留。

`keep_background_color` 缺失的新默认为 false，允许轻微带色的 MD3 表面；显式保存的中性面选择仍保留。静态基准预览需关闭动态色/自定义覆盖/OLED/背景图，使用默认对比度、默认字号并关闭中性面覆盖；普通用户的主题可按有效偏好呈现，不要求都与色表截图相同。

解析顺序为：

1. 由用户主题模式确定浅/深环境。
2. 满足 API 条件且用户选系统动态色时，取系统完整 scheme；否则选择静态默认或从用户种子生成完整 scheme。
3. 应用用户选择的对比度/中性表面/OLED 策略；系统动态色分支不支持某项时明确显示其适用条件。
4. 将已有角色/元素覆盖映射到语义角色，校验实际配对；不合格时显示原因并使用可读的保护配对。
5. 背景图作为独立层组合，不改写所有颜色角色的 alpha。

动态色的 API 31 门槛与回退方式依据 [Android 官方主题说明](https://developer.android.com/develop/ui/compose/designsystems/material3)。选择品牌蓝色种子及本节优先级是本项目决策。

### 静态默认方案

品牌种子为 **`#375BAA`**。下表是本方案冻结的浅/深参考色，种子不等于最终 primary。生成依据为官方 `@material/material-color-utilities` **0.3.0**、`SchemeTonalSpot`、contrastLevel=0，输入同一种子，仅切换 isDark；不是“当前最新版”的声明。[官方颜色算法项目](https://github.com/material-foundation/material-color-utilities)

本轮仅在临时目录运行颜色算法及对比度计算以校验文档；没有为 Android 工程安装 JS 依赖。默认静态主题按本表实现；用户自定义方案继续由工程已有引擎生成，不要求两种生成器在所有自定义输入下逐字节一致。后续若调整默认表，必须一起更新基准截图和对比证据。

| ColorScheme 角色 | 浅色 | 深色 |
| --- | --- | --- |
| primary | #485D92 | #B1C5FF |
| onPrimary | #FFFFFF | #162E60 |
| primaryContainer | #DAE2FF | #2F4578 |
| onPrimaryContainer | #2F4578 | #DAE2FF |
| secondary | #585E71 | #C0C6DC |
| onSecondary | #FFFFFF | #2A3042 |
| secondaryContainer | #DCE2F9 | #404659 |
| onSecondaryContainer | #404659 | #DCE2F9 |
| tertiary | #735572 | #E0BBDD |
| onTertiary | #FFFFFF | #412742 |
| tertiaryContainer | #FED7FA | #593D59 |
| onTertiaryContainer | #593D59 | #FED7FA |
| error | #BA1A1A | #FFB4AB |
| onError | #FFFFFF | #690005 |
| errorContainer | #FFDAD6 | #93000A |
| onErrorContainer | #93000A | #FFDAD6 |
| surface | #FAF8FF | #121318 |
| onSurface | #1A1B21 | #E2E2E9 |
| surfaceDim | #DAD9E0 | #121318 |
| surfaceBright | #FAF8FF | #38393F |
| surfaceContainerLowest | #FFFFFF | #0D0E13 |
| surfaceContainerLow | #F4F3FA | #1A1B21 |
| surfaceContainer | #EEEDF4 | #1E1F25 |
| surfaceContainerHigh | #E8E7EF | #282A2F |
| surfaceContainerHighest | #E2E2E9 | #33343A |
| onSurfaceVariant | #44464F | #C5C6D0 |
| outline | #757780 | #8F9099 |
| outlineVariant | #C5C6D0 | #44464F |
| inverseSurface | #2F3036 | #E2E2E9 |
| inverseOnSurface | #F1F0F7 | #2F3036 |
| inversePrimary | #B1C5FF | #485D92 |
| surfaceTint | #485D92 | #B1C5FF |
| scrim | #000000 | #000000 |
| shadow | #000000 | #000000 |

`background` 兼容映射为 surface；`onBackground` 映射 onSurface；旧 `surfaceVariant` 需要兼容时映射 surfaceContainerHighest。新组件不继续扩散这些旧别名。未使用的 fixed 色角色保持当前 Material3 默认，不凭空新增消费场景；以后确需使用时补充完整配对。

### 色彩的具体用途

| 对象 | 容器 | 内容/状态 |
| --- | --- | --- |
| 普通页面、帖子正文 | surface | onSurface；辅助信息 onSurfaceVariant |
| 设置分组、资料摘要、集合摘要 | surfaceContainerLow | onSurface |
| 导航栏、输入工具区域 | surfaceContainer | onSurface；选择态使用 secondaryContainer/onSecondaryContainer |
| 弹层、菜单 | surfaceContainerHigh | onSurface |
| 主要提交按钮 | primary | onPrimary |
| 次要强调操作 | secondaryContainer | onSecondaryContainer |
| 当前选中帖子/会话 | secondaryContainer | onSecondaryContainer，另有选中语义 |
| 链接、焦点、当前 Tab 指示 | primary | 链接增加下划线或明确操作语义 |
| 错误提示 | errorContainer | onErrorContainer，图标与文字共同表达 |
| 分隔线 | outlineVariant | 不承担唯一的输入边界识别 |
| 输入边界、必要图形 | outline 或组件状态色 | 核对实际相邻背景的对比 |
| Snackbar | inverseSurface | inverseOnSurface；动作 inversePrimary |

不得将普通文字全部设为 primary，也不得将“楼主/未读/抽奖/板块/在线/危险”都画成同一种蓝色胶囊。

### 产品状态扩展

信息状态复用 primaryContainer/onPrimaryContainer。正向状态与警告使用以下固定语义配对，集中存储；它们不随主色改变含义。颜色加文字或图标共同表达，不作为唯一识别方式。

| 状态 | 浅色 foreground / container / onContainer | 深色 foreground / container / onContainer |
| --- | --- | --- |
| 正向/完成 | #336940 / #B5F1BD / #19512A | #99D4A2 / #19512A / #B5F1BD |
| 警告/需注意 | #845416 / #FFDCBC / #683D00 | #FBBA73 / #683D00 / #FFDCBC |

foreground 用于 surface 上的状态图标/短文字；onContainer 只用于对应 container。已读状态不使用绿色，付费说明不默认使用 error。

### 背景与自定义颜色保护

- 标准模式表面不透明，界面层次来自角色明度，不叠多层主色 tint。
- 背景图可在页面外缘/非内容留白区显示；正文、表单、关键状态和模态保留不透明保护面。
- 保留背景强度/面板透明度能力；仅对不承担正文和关键操作的装饰面板生效，UI 明示实际作用范围。不得为满足旧设置而降低输入区可读性。
- 调高对比度时可以强制内容保护面不透明；不能把高对比度的视觉变化称为设置丢失。
- 称号等源站语义色保留身份信息，可由统一适配器调整明度并搭配文字；源站 WebView 内部不做此种改写。

## D03 · 对比度与可读性

本项目对所有实际 UI 文字统一采用至少 **4.5:1** 的默认验收门槛；必要状态图形/控件边界至少 **3:1**。大字的 WCAG 例外不作为降低正文/标题可读性的常规理由；禁用、装饰与源内容固有图像按相应例外处理。[文字对比依据](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)；[非文字对比依据](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)

文档色表已按 sRGB 相对亮度计算以下不透明配对。数值仅展示到两位，实际验收使用未四舍五入值。它们证明静态色对，不证明未来屏幕、动态色或背景图已验收。

| 配对 | 浅色比值 | 深色比值 |
| --- | --- | --- |
| onPrimary / primary | 6.45 | 7.72 |
| onPrimaryContainer / primaryContainer | 7.27 | 7.27 |
| onSecondaryContainer / secondaryContainer | 7.28 | 7.28 |
| onTertiaryContainer / tertiaryContainer | 7.29 | 7.29 |
| onErrorContainer / errorContainer | 7.24 | 7.24 |
| onSurface / surface | 16.31 | 14.39 |
| onSurfaceVariant / surfaceContainer | 8.08 | 9.67 |
| primary / surface | 6.12 | 10.87 |
| outline / surface | 4.24 | 5.85 |

outline 在这里是图形边界，不用于普通文字；装饰分隔线不要求承担 3:1 的必要边界职责。所有透明叠加、图片底色、角色覆盖和选择态必须对最终合成背景另测。

## D04 · 字体、字重与内容密度

默认使用系统 sans-serif 与系统中文 fallback；代码使用等宽字体。Roboto 是拉丁 Material 字体基准，不要求为了 MD3 额外下载字体或用它替代中文 fallback。

### 应用 Typography

以下为字号/行高（sp），字重为数值。未列出的 display 角色保留 Material3 基准，不在普通社区页面使用超大营销标题。中文为主的整段文字默认 tracking=0；混排由实际字体和换行结果验证。

| 角色 | 默认值 | 用途 |
| --- | --- | --- |
| headlineLarge | 32 / 40，400 | 极少量独立结果页主标题 |
| headlineMedium | 28 / 36，400 | 需要更高层级的页面标题 |
| headlineSmall | 24 / 32，400 | 帖子完整标题、对话框标题 |
| titleLarge | 22 / 28，400 | 64dp 顶栏标题 |
| titleMedium | 16 / 24，500 | 帖子列表标题、卡片标题 |
| titleSmall | 14 / 20，500 | 分组、Tab、次级标题 |
| bodyLarge | 16 / 24，400 | 设置名称、输入文字、私信会话正文 |
| bodyMedium | 14 / 20，400 | 说明、摘要、反馈 |
| bodySmall | 12 / 18，400 | 时间、UID、计数、元信息 |
| labelLarge | 14 / 20，500 | 按钮、筛选 Chip |
| labelMedium | 12 / 16，500 | 主导航标签 |
| labelSmall | 11 / 16，500 | 非关键徽标计数；不用于错误说明 |

headline/bodySmall 的使用和中文 tracking 是本项目适配；不能把这张应用表称为不可调整的 MD3 原始字表。

### 阅读专用角色

| 角色 | 默认值 | 附加规则 |
| --- | --- | --- |
| 正文 | 18 / 30，400 | 默认左对齐；段后 16dp；不强制两端对齐 |
| 引用正文 | 16 / 26，400 | 背景/起始边标识引用，不靠极浅灰文字 |
| 代码 | 14 / 22，400 | 等宽，可横滚、复制；代码符号不被自动换行破坏 |
| 正文 H1 | 28 / 36，500 | 标记 heading 语义，保留原始层次 |
| 正文 H2 | 24 / 32，500 | 段前 24dp、段后 8dp |
| 正文 H3 及以下 | 20 / 28，500 | 层级按模型保留，不全部统一成粗体正文 |
| 评论正文 | 16 / 26，400 | 展开长文；不将主帖所有行高复制给紧凑元信息 |

用户应用字号因子在主题/阅读样式生成时各应用一次；系统字号继续交给 Compose 的 sp 转换。禁止改写 LocalDensity 来消除系统放大，也禁止再乘一次系统 fontScale。Android 存在非线性字号缩放，不能用两个因子的乘积承诺精确物理字号。

已有全局字号、字重、自定义字体能力继续保留；未来若新增独立阅读字号，必须单独定义键与迁移，不能将本次角色拆分误实现成额外一次倍率。默认全文字重不额外 +100；只有真实选择态/标题使用强调字重。

### 密度

默认舒适密度。若保留/新增紧凑展示，只减少非文字留白及可选摘要；不得缩小触区、掩盖关键状态或抵消系统字体。正文阅读行高不跟随列表紧凑模式缩小。元信息可换行，正文不得为了固定卡片高度自动缩字号。

## D05 · 间距、尺寸、形状与层次

### 间距角色

| 语义 | 数值 | 消费场景 |
| --- | --- | --- |
| inline | 4dp | 标签内部、紧邻图文 |
| item | 8dp | 同组元素、控件与辅助信息 |
| contentGap | 12dp | 头像与标题列 |
| pageHorizontal | 16dp 窄窗 / 24dp 宽窗 | 外壳页面边距 |
| pageVertical | 16dp | 普通页内容起止留白 |
| section | 24dp | 独立分组之间 |
| paneGap | 24dp | 双栏之间 |
| largeSection | 32dp | 独立内容段/资料头与列表之间 |

以 8dp 为主要节奏，4/12dp 服务微间距；1dp 分隔线、图标描边等不机械改成 8dp。小于 360dp 的极窄窗口可将页面边距统一降为 12dp；只能由窗口策略集中决定。

### 尺寸角色

| 项目 | 设计默认 | 增长/回退规则 |
| --- | --- | --- |
| 交互触区 | 至少 48×48dp | 不用相邻区域重叠扩张来凑数 |
| 标准图标 | 24dp | 状态徽记可 16dp，交互仍保留触区 |
| 列表头像 | 40dp | 独立点击占 48dp 布局槽 |
| 评论头像 | 32dp | 48dp 布局槽；窄屏折叠树采用缩进预算 |
| 资料头像 | 64dp | 用户卡/我的；缩放不压住称号 |
| 普通顶栏 | 64dp 内容高度 | status bar inset 另算；单行省略时正文提供完整标题 |
| 根底栏 | 标准约 80dp 内容高度 | 系统底部 inset 另算；大字号允许增长 |
| rail | 80dp | 四目的地共用一个数据源 |
| 单/双/三行设置行 | 最小 56 / 72 / 88dp | 真实文字需要时继续增高 |
| 搜索栏 | 最小 56dp | 自适应高度，完整标签/清除语义 |
| 普通按钮 | Material 默认视觉高度，触区≥48dp | 文字较长换布局，不裁切关键动作名称 |
| FAB | 56dp | 社区发帖；不与回复底栏并列抢占 |
| 回复工具区 | 最小 64dp | 实际高度含换行，列表按实际占位避让 |
| 阅读最大宽度 | 720dp | 居中；表格遵循用户横滚/适屏偏好，局部溢出不扩大整页 |
| 普通表单最大宽度 | 640dp | 字段不跨整个宽屏 |
| 对话框最大宽度 | 560dp | 视口减安全边距；内容长时滚动或改页面 |

48dp 触区及 Material 默认行为依据 [Compose 可访问性说明](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)。其余具体角色为本项目定值；高度表中的最小值不能改写成固定 height 裁切文本。

### 形状与层次

| 角色 | 半径 | 用途 |
| --- | --- | --- |
| none | 0dp | 页面、普通列表背景、分隔容器 |
| extraSmall | 4dp | 标签、代码内联背景 |
| small | 8dp | 小菜单分组、局部高亮 |
| medium | 12dp | 集合卡、资料摘要、选择态条目 |
| large | 16dp | 大区块、输入/工具面板 |
| extraLarge | 28dp | Dialog、ModalBottomSheet 顶角 |
| full | 形状 50% | 按钮、导航指示器、头像；FAB 另按其组件角色 |

注意 FAB 使用所选 Material3 组件自身形状/角色，不因 full 角色存在而强制把 56dp FAB 改为圆形。普通内容不普遍套圆角 Surface；列表项 press/selection 可以使用公共形状而保持静止时平面。

常驻页面/列表默认 shadowElevation=0；分组由 tonal surfaces 区分。菜单、Sheet、Dialog 使用 Material 默认保护层；不要在显式 container 色上再次任意叠主色，或为每张帖子卡增加 6dp 阴影。

## D06 · 导航、动作、输入与反馈组件合同

以下为产品组件的设计合同，不是对当前已存在 API 的声明。能复用现有命名的入口优先续用。

| 公共入口/模式 | 数据/事件合同 | 必须覆盖的状态 |
| --- | --- | --- |
| AppPageScaffold | title、可空 onBack、actions、content；保持普通页职责 | 有/无返回、有/无 actions、长标题、滚动 |
| AppPrimaryNavigation | 相同目的地列表、选中 ID、onSelect | bar/rail、徽标、两项兼容布局、大字号 |
| AppMenuRow / SettingsRow | 名称、说明、leading/trailing、enabled、onClick | 说明换行、禁用原因、导航/纯展示区分 |
| AppSwitchRow | label、supporting、checked、enabled、onCheckedChange | 单一 Role.Switch 语义，行/开关只触发一次 |
| AppChoiceRow | label、当前值、候选数据、onSelect | Radio/选择弹层，较长值换行 |
| AppFormField | label、value、supporting/error、onValueChange | 空、编辑、错误、禁用、只读、密码/键值按实际类型 |
| AppNotice | 语义级别、文案、可选重试/关闭 | 说明、局部错误、持久失败、无动作 |
| LoadingBox / ErrorBox / EmptyBox / LoginRequiredBox | 页面适配器提供状态与回调 | 不同状态的标题、说明和下一步，不共享空白占位 |
| AppConfirmDialog | 对象、后果、确定/取消、busy | 危险动作、提交失败、重复点击、长文滚动 |
| PaginationBar | 当前页/总页/能力、onPage | 边界、加载中、失败重试、无更多 |
| TopicListItem | 单个条目展示数据、onOpen/onUser/onMore | 普通、选中、未读、状态标签、长标题、无头像 |
| UserIdentityHeader / UserSummary | 用户显示数据、明确 profile 事件 | 长名、称号、离线/在线、加载/失败 |
| ThreadPostItem / ReplyComposer | 显示模型或输入状态、事件 | 引用/树形/折叠/上传/发送/验证码/失败恢复 |

### 重要行为

1. 主操作每个上下文最多一个高强调 Filled；次操作使用 Tonal/Outlined/Text。危险确认明确对象及结果，不使用笼统“确定操作”。
2. 没有动作的展示行不添加 clickable；有独立二级操作的行不能把子操作清空语义后留成不可发现热点。
3. AppSwitchRow 的父行提供名称、Role.Switch、checked、enabled 和单一事件；内部 Switch 无独立事件/操作语义。当前已修复的这条约定继续保留。
4. TextField 有持续 label，placeholder 只作示例；错误贴近字段，提交后先聚焦首个可修复字段。不是每次输入都抢焦点。
5. 简短危险确认使用 Dialog；筛选/跳楼/动作列表使用 Sheet；长编辑、备份复杂配置和大量说明使用页面。不能把整页表单挤入 AlertDialog。
6. 所有模态均可按合理返回/取消路径关闭，关闭后焦点回触发器；提交不可取消时需明确 busy 与离开策略，不能把关闭动作误当撤销源站提交。
7. 原生组件使用 Material 默认 ripple、focus 和 disabled 行为。自定义模式才应用统一 state layer；不把父层 alpha 同时作用于文字、容器和子控件。

## D07 · 内容模式的具体排版

### 帖子条目

起始为 48dp 头像槽，其中图像 40dp；后接 12dp 间距与弹性文字列。标题 titleMedium，默认最多 2 行，已有多行偏好按映射保留；元信息 bodySmall，可换成两行。标题负责主要注意力，板块置于元信息而不是右侧占据整列的大胶囊。

普通条目纵向 padding 16dp，条目间不再叠 16dp 卡片外距；使用 inset divider 或相邻留白分界。选择态在 12dp 形状内使用 secondaryContainer；“读过”不等于“选中”。读过条目可降低标题强调，但仍满足文字对比。

辅助标签常显最多两枚，优先保持源站正在进行的状态；其余提供“更多状态”可访问入口。付费、权限和内容不可用等必要说明不受这个上限限制，可单独成行。未读使用点与语义，置顶使用图标/文字，二者不都依赖蓝底。

整个条目打开帖子；头像打开用户；更多按钮打开动作菜单。应为三个动作分配不重叠的实际触区和明确名称。帖子内容读取交互不能误触称号外链。

### 用户、称号与评论

用户名及主要身份在第一信息层；称号和用户组在第二层，必要时换行。长名允许省略，但资料页面提供完整名称，辅助技术名称完整。在线圆点不能盖住头像主体或变成独立无名操作。

评论采用纵向列表，不为每条回复包大圆角卡片。缩进每层 16dp，窄屏最多展示两级视觉缩进，之后用“回复某人”和展开线程表达深度；模型层级不被截断。折叠后显示作者、摘要及剩余条数，并可恢复。

### 富文本与媒体

- HTML/Markdown 使用现有解析模型，视觉层只映射样式。URL、锚点、引用、代码语言和链接目标不因排版被删改。
- 表格默认/横滚模式只在表格局部横滚并保留表头辨识；用户已关闭横滚时继续适屏压缩列宽、允许单元格换行，不自动覆盖偏好。列数多时可提示切换模式；表格模式切换不改变原数据。长代码局部横滚、可复制，页面本身不整体横向滚动。
- 图片保留长宽比和稳定占位，点击进入查看器；下载/保存错误就近可重试；不为每张正文图片套主题滤镜。
- 嵌入视频保持受控加载、全屏退出与原平台链接；不可因新卡片形状破坏 WebView 生命周期或播放器比例。
- AI 总结使用独立可折叠区块与 AI 标识；正文原内容保持可定位。错误只影响总结块，不替换整篇帖子。
- 红包/抽奖/奖励展示为独立信息块，最多一个明确主操作；金额/资格/状态来自现有模型，不制造本地奖励任务。

### 钉住的帖子

手机在根页面内容下沿、导航上方展示紧凑的钉住条：一行标题、恢复阅读、关闭；每个独立动作保留 48dp 触区。它参与占位，列表末项不被覆盖。详情、编辑和全屏查看器内隐藏该条但保留钉住状态；返回根页继续可恢复。宽屏有足够空间时可放支持 pane，否则沿用占位条。恢复与关闭继续调用原钉住事件，不重新请求或复制帖子数据源。

## D08 · 动效、输入方式与可访问性

标准 Material 组件保留内部状态反馈；可用的 Expressive 弹性行为只在组件层使用。页面转场设计默认淡入/轻共享轴 200ms，短反馈 150ms，面板展开 250ms；这些为项目定值，不冒充所有 MD3 组件的固定时长。长列表和正文不逐项延迟入场。

遵从系统动画设置；减少动画时改为立即或简短淡入，不自动循环装饰动效。骨架屏不持续高亮闪烁；忙碌同时具有文字/语义。无需引入实验 API 才能完成上述行为。

键盘 Tab 焦点依视觉阅读顺序；Enter/Space 触发标准控件；Esc/Back 关闭当前浮层。提交快捷键仅在合适编辑器且能力允许时提供，不截获正文正常输入。鼠标 hover 不能成为唯一操作入口。

TalkBack 可定位页面/正文标题，读取条目核心信息，区分“开关已开启”“已选中”和“不可用”。装饰图标 contentDescription=null；图标按钮有动作名；动态错误和结果使用适度 live region，避免每个列表刷新全部朗读。[Compose 默认语义与触区](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

## D09 · Insets 与内容保护

Activity 负责 edge-to-edge。顶栏消费上方安全区；应用壳处理根导航占位；页面消费传入 content padding；输入模式负责 IME。每条边只有一个明确消费方。[官方 Insets 协调机制](https://developer.android.com/develop/ui/compose/system/insets-ui)

不能在应用壳、子 Scaffold、LazyColumn 和输入栏同时重复添加相同底部 padding。标准栏参与布局时不再额外叠其高度；确有 overlay 时列表的末尾留白使用实测遮挡高度，不能写死一个全局“底栏 100dp”。

键盘出现后回复/私信输入区在键盘上方，Snackbar/FAB 不遮住输入和发送；系统手势区与 IME 通过消费机制协调，不把二者高度简单相加。折痕、安全区和局部横滚共同验收。

## D10 · 设计系统交付检查

- 每一项 token 至少有明确消费方；每一种公共交互只有一个实现来源。
- 相同模型可在浅/深、长文案、禁用、错误、大字号预览中展示，不需要真实账号。
- 主题矩阵包含静态、动态、自定义、OLED/高对比与背景图保护；角色配对按最终合成颜色验证。
- 从列表 → 正文 → 回复 → 错误恢复 → 返回能观察到一致的控件、状态和焦点行为。
- 页面特有的图片、徽章与源站原色不能被扫描脚本一概当作主题违规；例外要按内容/状态语义识别。

以上均为未来实现的检查要求。当前文档色值计算不等同于这些运行检查已通过。
