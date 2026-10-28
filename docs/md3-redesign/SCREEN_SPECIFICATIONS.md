# 页面与交互规格

版本：2.0；日期：2026-10-21。状态：待实施设计，本文不代表已开发、已运行或已验收。
范围：Android 原生界面及应用侧网页外壳；保持现有业务、数据与源站协议。
设计总纲见 [MD3 UI 大更新开发方案](../MD3_UI_REDESIGN.md)；视觉数值以 [设计系统](DESIGN_SYSTEM.md) 为唯一来源；迁移与完成条件见 [实施与验收](IMPLEMENTATION_AND_ACCEPTANCE.md)。

## 1. 阅读方式与通用约束

P01—P18 是页面需求编号。一个编号可以覆盖多个共享布局的路由，不要求创建一个同名 Screen 或文件。
本篇给页面组合与交互规则；颜色、字号、间距和基础组件变体不得在每个页面重新定义。
以下源码位置均相对 app/src/main/java/sb/linux/client/ui/；它们是现状定位，不要求沿用现有视觉或逐文件照搬。
新增 UI 状态与事件可在现有表现层实现；不得为页面换肤新增私信后端、本地消息数据库、源站业务接口或自动提交机制。

| 通用事项 | 开发要求 |
| --- | --- |
| 顶栏 | 主栏标准高度 64dp；有返回的页面提供返回图标；根目的地不显示返回箭头。大字号时允许必要增高，不能裁切内容 |
| 正文 | 默认 18sp/30sp；阅读列最大 720dp，居中；遵循系统字体缩放并叠加已有用户文字偏好，不在页面关闭缩放 |
| 列表 | 帖子使用单列低装饰 TopicListItem；普通条目没有独立大胶囊底；仅当前选读条目使用 secondaryContainer |
| 头像 | 帖子/会话列表 40dp，回复 32dp，资料 64dp；交互点击区域至少 48dp，不能将头像尺寸当成点击区域尺寸 |
| 主题 | 默认支持系统动态色，静态回退为蓝 TonalSpot；primary 浅色 #485D92、深色 #B1C5FF；按设计系统解析 |
| 首次加载 | 内容区状态占位保留顶栏；刷新已有内容时保留列表，使用局部进度，不整页闪空 |
| 加载失败 | 无内容用整页错误；有内容用行内错误与重试；分页失败只影响尾部，已有内容可读 |
| 点击与状态 | 忙碌、选中、禁用由状态驱动；同一提交期间只允许一个请求；失败保留用户输入 |
| 返回 | 顶栏返回、系统返回、预测性返回遵守同一页面退出约束；模态层优先关闭，再处理页面退出 |
| 内容恢复 | 保留当前分类、排序、页码、选读 ID 和位置；主题切换与窗口变化不触发无条件业务重载 |
| 游客 | 公开阅读和本地功能可用；需要账号的操作按原权限门禁，说明原因并提供登录动作 |
| 登录续接 | 登录成功返回原目标；不得把原先的发帖、投币、收藏或发送动作自动执行一遍 |
| 破坏性操作 | 明确对象与后果，再使用已有确认策略；取消与返回不能提交操作 |
| 源站页面 | 统一应用外壳、加载与桥接；不统一改写 DOM、验证码、广告奖励或原站表单协议 |

## 2. 自适应页面规则

- 可用宽度以当前窗口、已消费的系统边衬、导航区和铰链区域计算，不以物理屏幕宽度或设备型号推断。
- 小于 600dp 使用单栏与底部 NavigationBar；大于等于 600dp 一般切为 NavigationRail，不自动等于双栏。
- 双栏至少容纳 280dp 列表、24dp 间距和 400dp 详情；这些是扣除 rail、边距和铰链后的实际内容宽度。
- 高度小于 480dp，或大字号下无法满足内容最小空间时，回退单内容栏；导航形式仍按窗口条件选择。
- 宽屏不因达到 840dp 就强制常驻 drawer。社区辅助抽屉、主目的地导航与列表详情三者不能相互替代。
- 单栏变双栏时将同一个选读条目投影为详情；双栏回单栏保留选读详情和返回到列表的路径。
- 详情没有选读条目时展示“选择内容查看”，不自动访问第一条、不改变阅读历史。
- 每个 pane 各自维护滚动与焦点；右栏系统返回先退详情栈，不能先退出整个应用。
- 正文列超过 720dp 的剩余空间用于留白或确有数据的辅助信息，不能把段落无限拉宽。

## P01. 应用外壳与主导航

新安装默认四个目的地：社区 / 淘帖 / 消息 / 我的；社区映射 home，消息复用现有私信与通知能力。
发帖是 FAB 或明确动作，永远不形成一个“已选中发帖”的导航目的地；编辑器中不显示主导航。
手机使用标准 MD3 NavigationBar，rail 使用同一目的地模型；本轮新默认不使用玻璃或实时折射效果。
仅两项有效根目的地的旧自定义使用顶部 PrimaryTabRow；此时根内私信/通知等分区用分段控件，取消底栏。少于两项时仅派生默认四项并说明，不覆盖原始偏好。
社区顶栏：左侧社区辅助抽屉；标题“社区”；右侧搜索和更多。消息顶栏提供“私信 / 通知”的当前分区信息。
根目的地切换保留各自状态；重选社区是否刷新沿用现有已保存行为，不由组件自行添加刷新。
已有底栏项目/顺序需按迁移规则读取；移出的入口必须在社区辅助区或我的页保持可发现，不能静默清空偏好。
旧 directMessagesRoot 指向消息的私信分区；旧 notifications 指向通知分区；旧 topicCollectionsRoot 指向淘帖。
具体路由组织可保留现名，或新增仅作用于 UI 的聚合入口；业务请求、未读来源和分页仍使用原状态模型。

~~~text
┌ 社区                      搜索  更多 ┐
│ 新评论   新帖子   精华                │
│ 筛选：全部内容                 调整   │
│ 帖子标题与摘要                       │
│ 作者 · 时间 · 板块 · 评论数           │
│ ──────────────────────────────────  │
│ 下一条帖子                       ＋  │
├ 社区       淘帖       消息       我的 ┤
└─────────────────────────────────────┘
~~~

## P02. 社区首页与帖子列表

位置：screens/HomeScreen.kt、HomeNavigationLayout.kt；共享条目位于 patterns/topic/TopicCardView.kt，实施时收口为统一列表模式。
内容从上到下：64dp 顶栏、排序 tabs、当前筛选摘要、新帖提示、帖子列表、尾部状态/分页。
“新评论 / 新帖子 / 精华”为排序；抽奖、发卡、申精、红包保留独立数据源，通过筛选入口选择，不伪装为同一请求的普通排序。
每个条目：标题默认最多两行，已有多行偏好按迁移保留；一行可选摘要；作者与时间；板块和评论数；标签按优先级换行，不挤压标题成窄列。
置顶、精华、抽奖中、红包等必须有可读文字；不靠颜色单独表达业务含义；来源没有该字段时不画占位徽章。
普通列表不以阅读条数堆积圆角容器。已读状态使用低强调文字并保留可读性；“已读”与当前“选读”是不同状态。
主动作点击条目进入帖子；头像打开用户摘要，长按操作通过统一菜单提供，禁止把全部子区域变成重叠点击层。
已有分页/无限滚动偏好继续生效；新帖折叠提示说明新增数量，用户展开后才合并，避免刷新时阅读位置跳动。
宽屏列表栏维持单列；打开详情后仅选读项使用 secondaryContainer；关闭详情后去掉选读态但保留浏览位置。

## P03. 板块与社区辅助区

位置：feature/forum/ForumListScreen.kt、ForumScreen.kt；screens/HomeSidebarDrawer.kt、HomeSidebarRows.kt、HomeSidebarHeader.kt。
社区辅助抽屉包含板块入口、快捷功能、热帖、统计与在线用户；当前没有数据的区域不编造数字或样板用户。
抽屉按内容分组使用 ListItem，选择项用 MD3 选中态；长板块名允许换行，不再给每一项独立大胶囊。
“全部板块”打开已有 forums；板块详情显示板块名称、简介/统计（有数据才显示）、帖子列表及可用发帖动作。
标题、标签、分页使用 P02 的同一帖子列表模式；不能因来自板块又写一套 TopicRow。
板块权限、空态、加载失败与游客状态来自原业务；不可访问时说明原原因，不将权限不足展示为“暂无帖子”。
侧板在线用户与双列偏好保留；窄宽度和大字号下允许布局降为单列，已保存值不被覆盖。
关闭抽屉恢复打开前焦点；点击遮罩、返回键或关闭按钮均可关闭；内容不能跨折叠铰链。
宽屏可按可用空间将辅助区作为支持 pane，但不与列表详情双栏同时强制挤压正文。

## P04. 搜索

位置：feature/search/SearchScreen.kt 及 SearchViewModel、搜索费用提示/确认组件；旧 field 参数继续映射 scope。
进入页：有返回的搜索顶栏、关键词输入、范围控件、排序入口、费用说明、结果区域；焦点明确落在搜索框。
范围保留标题、正文、回帖、全部与用户等已有定义；用户范围下不显示无效的主题排序选项。
窄屏将范围做可滚动 FilterChip；排序放下拉选择。大字号不能挤成两排互相遮挡的细小按钮。
费用与余额由源站返回；只呈现真实资料，错误时给出重试，不写死“免费”或固定积分。
收费提交使用明确确认框，显示本次关键词/范围与当前真实费用；取消不发起搜索；费用变化遵守原授权流程。
切换范围、排序和页码必须复用原请求/授权规则；UI 不能以新动画或重组触发第二次付费搜索。
无关键词、无结果、获取费用失败、搜索失败、分页失败分别呈现；正在重试不清空关键词。
主题结果用统一主题摘要，用户结果用统一用户条目；回到结果时恢复原页码、查询、筛选和位置。
宽屏结果列表与选读详情遵守第 2 节；输入区域不复制到两个 pane。

## P05. 帖子阅读与评论

位置：screens/TopicScreen.kt、TopicScreenScaffold.kt、TopicPostCardContent.kt；正文复用 content/，评论调用统一回复模式。
内容顺序：可收起顶栏、完整标题、作者与元信息、业务附属区、正文、签名/编辑信息、评论控制条、回复列表。
标题使用 headlineSmall 24sp/32sp，正文 18sp/30sp；完整标题出现在正文头部，滚动后顶栏可单行省略。
正文阅读列最大 720dp；页面左右留白由公共阅读容器拥有。评论可同列，不给每一楼叠套厚重大卡片。
作者行：头像、姓名、组别/楼主、称号、时间与可选 UID；长姓名优先换行，徽章不得把姓名压成不可识别碎片。
主动作“回复”固定于底部操作区；点赞、收藏按权限显示；低频动作进入菜单，保留当前业务能力与确认偏好。
菜单至少覆盖已有帖内搜索、跳楼、加入淘帖、导出、AI 总结、举报、钉住等能力，按权限与数据条件显示。
直达评论保留为顶栏快捷操作；投币/档位选择、打赏和自有内容编辑放在相应文章/评论动作菜单，沿用原资格和确认。评论的本地收藏、引用与对话展开不能随布局移除。
评论排序沿用原语义；树形层级限制缩进总宽，深层回复以摘要与“查看对话”展开，不逐层把文字挤成竖列。
跳楼/搜索命中保留原位置并提供返回阅读位置；未知总页数/楼层不能伪造精确进度。
引用、链接、代码、表格、图片及嵌入视频使用同一渲染管线；选择文字沿用系统选择，不接管原生复制/翻译菜单。
正文首次加载失败保留返回；追加评论失败保留正文；游客看到可读公开内容和明确回复登录提示。
宽屏详情独立处理滚动、分页、IME 与返回；钉住保留帖子快照与位置，不因改成 MD3 底栏丢失。

~~~text
┌ 返回  帖子标题（滚动后省略）  更多 ┐
│ 完整标题：按内容自然换行           │
│ 头像  作者 / 组别 / 称号           │
│ 时间 · 阅读/回复等真实元信息        │
│                                   │
│ 正文：18sp / 30sp                 │
│ 段落、链接、图片、代码与表格        │
│                                   │
│ 全部回复 52      排序 ▾   跳楼     │
│ 头像  作者与楼层                   │
│ 回复内容与引用                     │
├ 回复…               点赞  收藏  ⋯ ┤
└───────────────────────────────────┘
~~~

## P06. 回复与原站发帖编辑器

位置：screens/TopicReplyDialog.kt、TopicReplyDialogHost.kt、SourceEditorScreen.kt。
原生快捷回复使用 ModalBottomSheet：标题与关闭、引用预览、输入、工具栏、验证码、发送；大段编辑可展开全屏。
回复 sheet 与全屏是同一份草稿/选区状态，不复制两个输入源；切换、旋转与 IME 开合不丢文本。
图片上传必须显示 busy/错误；成功插入原链接，失败保留原文，不替用户重复上传；未配置图床提供设置入口。
验证码状态按原服务更新/刷新；失败清理验证码答案的规则沿用业务，不能清空回复内容。
提交 busy 禁止重复发送；失败保留草稿与引用；成功后沿用原返回/合并刷新位置，不跳到首页。
退出有未提交内容时按已有保存能力说明后果；不可宣称存在未实现的离线草稿或跨设备同步。
新建/编辑主题继续完整原站 Web 编辑器，应用仅提供标题、返回/关闭和加载反馈。
原站草稿、附件、奖励和发布规则原样保留；注入引用仅预填用户选择内容，不自动提交表单。
网页编辑器离开确认、发布后返回新帖子/正确页码与楼层的行为保留；不要求为这次换肤重建原生完整发帖协议。

## P07. 淘帖列表、详情与管理

位置：screens/TopicCollectionsScreen.kt、CollectionDetailScreen.kt、TopicCollectionManageScreen.kt、TopicCollectionSheet.kt。
根页顶栏“淘帖”，分区为大家的/我的；未登录可读公开集合，“我的”展示登录门禁。
集合条目使用标题、简介、作者与内容量，最多一张辅助封面（原数据存在时）；不编造封面或推荐算法。
集合详情顶部完整标题与说明，下方为统一帖子列表；订阅/取消等动作只在原业务支持时出现。
管理页采用明确选择/管理模式，顶部给选择数量与退出；删除、移动和权限变更使用各自原确认规则。
“加入淘帖”从帖子页打开选择 sheet，展示当前选择与提交状态；取消不改变集合，失败保留选择。
空集合与加载失败分开；不能让管理失败看起来像删除成功；退出后刷新对应集合并保留上一列表位置。
宽屏列表/集合详情可形成双栏；进入管理是详情内任务，不能把原公共集合直接替换为当前用户集合。

## P08. 消息中心：私信与通知

位置：screens/DirectMessagesScreen.kt、NotificationsScreen.kt；新增的中心仅聚合表现层入口。
根页顶栏“消息”；首层分区“私信 / 通知”；保留每个分区查询、分页、未读与滚动状态。
私信列表：40dp 头像、姓名、最后消息摘要、时间与真实未读；通知列表：类型图标、主句、上下文和时间。
未读使用主题角色、文字强调和已有计数；没有可靠计数时仅用真实状态点，不统计猜测数。
分区切换不自动标记另一分区已读；阅读/标记已读沿用原业务事件，不在 Composable 首次绘制直接修改。
刷新与屏蔽名单等低频动作放顶栏菜单；无消息、游客、同步失败、已有内容刷新失败分别呈现。
旧 notifications 链接直达通知分区，旧 directMessages/directMessagesRoot 直达私信分区，并保留来源返回语义。
根入口不显示返回箭头；从深链/二级入口进入时保留返回栈，不能把所有调用强制改成“返回退出应用”。
宽屏选会话后右侧聊天、选通知后右侧目标；无选中时空详情，不自动读取第一条。

## P09. 私信会话

位置：screens/ChatScreen.kt；数据来自原站完整会话，不聚合持久化为本地消息数据库。
顶栏：返回、对方姓名与可点资料入口、同步/关系菜单；姓名可单行省略，完整姓名可在资料查看。
接收气泡 surfaceContainer，发送气泡 primaryContainer，前景使用成对 on 色；时间与发送状态在气泡外。
聊天正文使用 bodyLarge 16sp/24sp，不照搬帖子长文 18sp；气泡最大占可用会话宽度的 80%，并限 480dp，长 URL/中英文均可换行。
底部：表情、1—4 行输入、计数/错误、发送；按原规则最多 500 个 Unicode code point，不能用 UTF-16 长度误判。
发送权限、屏蔽关系与忙碌共同决定按钮状态；键盘发送与点击发送走同一事件，失败保留文本。
已有内容同步失败用行内提示；有引用时先显示可辨认引用摘要；新消息滚动遵守阅读上下文，不强制打断查看旧消息。
IME 出现后输入栏位于键盘上方，消息列表可滚动；不要同时叠加两套 navigation/IME padding。
屏蔽/解除关系有明确对象与确认；不把网络发送成功前的气泡当成源站已送达结果。

## P10. 我的、游客与登录

位置：screens/MeScreen.kt、LoginScreen.kt；资料显示复用 patterns/user/。
已登录“我的”顶部为 64dp 头像、姓名、组别/称号与有来源的账号信息；不使用占半屏的装饰英雄区。
下方分组：我的内容、社区功能、应用。高频历史/收藏/我的淘帖前置；称号、邀请、附件、隐私、屏蔽保持入口。
应用设置与关于具有清晰独立入口；原站广告和社区活跃度保留名称与来源，不伪装成本地账号功能。
游客头部解释登录价值并提供一个主按钮；仍展示公开社区入口、应用设置、关于和本地内容。
登录页：简洁品牌/标题、用户名/邮箱、密码显示切换、源站验证码、登录按钮与错误反馈；支持滚动和 IME。
保留“记住密码”已有选项及原存储语义，不因改版改变用户已经保存的选择；CAP 与原生数学题验证沿用实际挑战类型。
错误说明放在对应字段或表单区域；不回显密码、不把验证码失败写成账号密码错误；忙碌保持按钮尺寸。
登录成功回到请求来源；取消登录回到原可用页；系统返回应能离开登录，不造成门禁循环。
自动签到不设按钮、菜单或 FAB；有真实签到结果时可在账号摘要显示只读状态，不把失败隐藏为成功。

## P11. 用户摘要与主页

位置：patterns/user/UserAvatar.kt、UserSummaryContent.kt、UserInfoCard.kt；screens/UserScreen.kt、UserByNameScreen.kt。
点击头像打开统一摘要 sheet/自适应资料弹层：头像、姓名、组别、简介、有来源统计、查看主页。
摘要加载与失败保留关闭动作；不能显示上一个用户的缓存姓名配新用户头像；重试作用于当前 UID。
资料页：64dp 头像，完整姓名优先换行，称号另占合适位置；主题/回帖/积分/收藏按原权限显示。
私信、屏蔽等动作取自已有权限状态；私有分区说明原因，不以空列表替代；不显示不可执行的装饰关注按钮。
username 深链先解析 UID：加载时显示解析状态；失败可返回/重试；成功替换中转路由，避免返回时重复解析。
宽屏资料内容适度限宽，分区切换保留各自页码/滚动；来自帖子弹层的返回不能重置原帖子阅读位置。

## P12. 我的内容与本地资料

位置：screens/SettingsDestinations.kt、FootprintScreen.kt、FavoritesScreen.kt、CardRedemptionsScreen.kt、ExportedTopicsScreen.kt、WebScreens.kt。
我的内容为资源入口页：历史、收藏、AI 历史、兑换记录、导出、附件、隐私及活跃度，不把所有资源混进一条时间线。
历史按真实日期分组，点击恢复原阅读位置；清空操作说明范围并确认，空列表时不可执行无效清空。
收藏区区分帖子与评论；摘要共享帖子/回复模式，保留取消收藏确认偏好及定位评论目标。
兑换记录中的敏感兑换信息以按需查看/复制呈现，错误不显示无意义空码；未执行兑换时不造成功记录。
导出列表显示真实格式/文件状态，查看本地 HTML 保留禁用 JS、相对资源基址与错误重试。
删除导出文件、清理阅读记录与取消源站收藏是不同操作；文案不能统称“删除数据”，执行范围不能扩大。
游客允许读取已有本地历史/导出等资料；账号所属资源按原门禁显示，不能为统一空态清空本地记录。

## P13. 设置首页与阅读交互

位置：screens/AppSettingsScreen.kt、BrowseSettingsScreen.kt、SettingsPage.kt、SettingsComponents.kt。
设置页使用同一列表容器：设置搜索、七分类、我的内容；子页使用同一 AppSettingsRow/AppSwitchRow/分组标题。
七分类保留：阅读与交互、外观与布局、发帖与图片上传、网络与链接、AI 助手、存储与备份、应用与账号。
搜索结果给设置名称与路径；点击定位到对应子页/项目，不因重新分组丢失搜索索引。
续用 feature/settings/SettingsSearchIndex；搜索仅匹配本地设置元数据，不请求源站。若支持定位具体条目，索引增加稳定设置 ID/锚点，由目标页消费一次并提示焦点，不反复触发跳转。
阅读设置保留各范围分页、页大小、启动缓存、新帖折叠、侧板布局/在线用户、打赏弹幕、表格、UID、编码、对话串联、取消收藏确认。
即时保存开关与显式保存表单必须区分；依赖项禁用时说明原因，不仅减透明度。
整行开关只有一个操作语义节点，同时拥有名称、checked 和 enabled；内部 Switch 不追加第二个动作。
字段与长说明允许自然增高，不能固定在 56dp 切断；类别重置明确类别，不能代替重置全部。
宽屏设置可列表详情，不把设置菜单页同时显示两套主导航；大字号字段不足宽时仍采用纵向表单。

~~~text
┌ 返回  阅读与交互                   ┐
│ 列表与分页                         │
│ 首页无限滚动              开关     │
│ 关闭后使用分页                     │
│ ───────────────────────────────── │
│ 每页帖子数量                      │
│ [ 15                         条 ] │
│ 正文与评论                         │
│ 显示 UID                  开关     │
│ 在作者头像下显示 UID              │
│                                   │
│ 重置阅读与交互设置                 │
└───────────────────────────────────┘
~~~

## P14. 外观、字体与导航设置

位置：screens/ThemeSettingsScreen.kt、ThemeLayoutSettings.kt；ui/theme 与 SessionThemePreferenceController。
概览展示实时示例、跟随系统/浅/深选择，以及配色、背景、字体、导航布局四个入口。
示例统一展示列表、正文、主次按钮与状态，示例中的点击区域不能触发真实收藏、发帖或导航业务。
配色明确“静态方案 / 动态取色 / 自定义覆盖”的生效来源；动态色不支持的平台提供静态回退，不能展示已生效假象。
已有种子、角色覆盖、卡片元素色、对比度、OLED、背景图/透明度、字体/字号/字重进入高级项并说明覆盖范围。
背景图与透明度不得让正文和输入框失去可读底；不以调低所有文字 alpha 模拟视觉层次。
字体导入提供名称、预览、错误与恢复系统字体；导入失败保留当前有效配置，不把空字体标记成功。
导航设置区分目的地与发帖动作；旧底栏顺序/项目、平板及玻璃样式按迁移表转换，确认重置前不得静默覆盖。
布局随窗口能力生效；“大屏”偏好不能强迫窄屏显示两栏；预览应说明当前窗口为何单栏。
新默认标准 MD3 导航与回复栏；兼容旧偏好时维持同一语义与事件，不为每种皮肤复制业务布局。

## P15. 网络、链接与上传

位置：screens/NetworkSettingsScreen.kt、DohSettingsScreen.kt、LinkSettingsContent.kt、SettingsDestinations.kt。
代理页面为单列表单：关闭/HTTP/SOCKS5、依赖字段、检测、保存；保存是主动作，检测为次动作。
代理/图床等有草稿的设置保留 dirty-exit；返回时选择继续编辑或放弃，不能由 switch UI 自动保存所有字段。
输入错误就地关联主机/端口/URL/令牌；检测 busy 支持取消/离开，错误不把已保存有效配置覆盖为空值。
DoH 列表区分默认/自定义/当前启用，添加与编辑复用同一表单；测速结果标注真实状态，不能靠固定颜色猜最快。
链接保留站内应用导航、站外打开方式、预览卡片二次打开规则；自定义浏览器策略不改变源站站内路径映射。
上传页展示已选图床/字段配置、验证错误与保存状态；令牌按敏感字段呈现，复制与显示由用户操作。
网络与上传成功反馈用统一 Snackbar/行内消息；表单失败必须可见且可再次保存，不只短暂 Toast。

## P16. AI 设置、总结与历史

位置：screens/AiSettingsScreen.kt、TopicAiSummaryActions.kt；feature/ai/。
设置概览区分功能开关、自动总结条件、接口配置、模型/方案与历史；关闭功能时依赖项显示禁用原因。
接口配置使用一致表单与密码字段；连接/模型获取是独立忙碌状态，不伪装为“保存成功”。
帖子总结在内容区或支持 pane 中展示生成中、部分内容、失败重试与完成状态，不覆盖原帖正文或改变原帖。
自动触发仍按既有开关与缓存策略；主题/窗口变化不重复发 AI 请求，不把重新绘制当成用户刷新。
历史/收藏与当前生成分开；显示真实关联帖子、保存结果与可用操作，不生成虚拟样例作为历史。
用户取消、超时或失败后明确可恢复动作；提交中的关键配置变更按已有能力阻止或取消，不能让 UI 显示另一方案的结果。

## P17. 存储、备份、更新与账号设置

位置：screens/SettingsDestinations.kt、DataManagementScreen.kt、TransferSettingsScreen.kt、GeneralSettingsScreen.kt、AboutScreen.kt。
存储概览先显示真实空间占用，再提供立即清理、自动清理、本地备份、WebDAV；自动策略与本次操作明显区分。
清理先选择对象、说明影响、确认，再显示执行与结果；没有实际计量时不宣称精确已释放容量。
备份明确范围、目标与当前状态；恢复确认写清覆盖/合并语义，沿用原实现，不从视觉规范创造新恢复协议。
WebDAV 表单保存、连接验证、上传、恢复分别有状态；凭据不进入错误全文或截图示例。
更新设置保留每次启动/间隔/从不与间隔输入；“立即检查”有 busy/失败/无更新/有更新结果。
账号资料入口仍指原站 settings；退出后原有会话刷新与 Cookie 同步保留。
关于页信息分层：应用与版本、项目/许可等已有链接；不加与阅读任务无关的大幅动效。
重置类别、重置所有应用设置、清除记录、退出登录保持不同名称与确认，不能用统一危险按钮误扩大范围。

## P18. 社区辅助页面与原站网页通道

位置：screens/LeaderboardScreen.kt、InviteCenterScreen.kt、ReportScreen.kt、SourceAccountPage.kt、SourceGachaScreen.kt、WebScreens.kt；patterns/web/。
榜单按真实类型与分页展示：排名、头像、姓名、对应分值；不虚构奖杯/成长进度。榜单是二级页时提供返回。
邀请中心按已有账号资格展示说明、列表与操作；没有资格显示原因；生成/兑换等行为仅由明确动作触发。
举报说明对象、选择原因、输入补充与提交状态；取消不提交，成功返回原位置，失败保留输入。
资料/隐私/活跃度/附件/屏蔽/称号/市场等原站页面共用网页外壳：返回、可靠标题、进度、错误重试、外部打开。
网页路由参数、Cookie、DoH、文件选择、多窗口与实际当前 URL 保留；不把初始 URL 当作所有后续外部打开目标。
验证 UI 提供真实 Web 验证与取消；成功/取消完成原等待流程，取消不能让请求永久 busy。
原站广告保留 DOM/脚本/CSRF/奖励弹窗；本轮仅统一外壳，不扩展原有 DOM 调整，也不自行领取奖励。
源站内容不支持 MD3 时以中性应用外壳承载；不能为了“视觉一致”注入全站 CSS、替换验证码或伪造原生表单。
原站返回历史、应用退出与编辑器离开保护按现有页类型处理；统一外壳不代表所有网页必须同一种返回行为。

## 3. 完整路由覆盖表

以下包括已注册路由与参数别名；复用 Screen 的多个路由仍分别列出，避免迁移时漏掉入口。
路由名称是兼容契约，不要求与新导航显示名一致。路径、查询值继续编码，不能直接拼接用户名、标题、URL 或文件路径。
表中的 Screen 名除明确 feature 路径外均位于 screens/；共用根由 navigation/Routes.kt 与 ExtraSettingsRoutes.kt 注册。

| 已有路由 | 对应新规格 | 现状实现/迁移约束 |
| --- | --- | --- |
| home | P01/P02 | HomeScreen；新显示名“社区”，保留缓存/分类/位置 |
| forums | P03 | feature/forum/ForumListScreen；社区辅助区可达 |
| forum/{id}?p={p} | P03 | feature/forum/ForumScreen；保留 id/p |
| me | P10 | MeScreen；四根目的地之一 |
| appSettings | P13 | AppSettingsScreen；设置搜索与分类 |
| detailEmpty | P01 | navigation/Routes.kt 内 DetailEmptyPane；宽屏未选中占位 |
| search?q={q}&scope={scope}&sort={sort} | P04 | feature/search/SearchScreen；保留请求语义 |
| search?q={q}&field={field} | P04 | 旧搜索兼容入口；field 映射 scope |
| login | P10 | LoginScreen；返回请求来源 |
| topic/{tid}?p={p}&floor={floor} | P05 | TopicScreen；页码/楼层定位不丢失 |
| newTopic | P06 | SourceEditorScreen；发帖动作，非选中目的地 |
| editTopic/{tid} | P06 | SourceEditorScreen(editId)；原站编辑器 |
| user/{uid}?tab={tab} | P11 | UserScreen；保留用户分区 |
| userByName?name={name}&tab={tab} | P11 | UserByNameScreen；解析后替换跳转 |
| settings | P17/P18 | SettingsScreen；原站个人资料与账号 |
| generalSettings | P17 | GeneralSettingsScreen；更新与账号 |
| networkSettings | P15 | NetworkSettingsScreen；保存/退出保护 |
| dohSettings | P15 | DohSettingsScreen；服务器/启用/测速 |
| browseSettings | P13 | BrowseSettingsScreen；阅读业务偏好 |
| dataManagement | P17 | DataManagementScreen；本次清理 |
| aiSettings | P16 | AiSettingsScreen；AI 概览 |
| transferSettings | P17 | TransferSettingsScreen；本地备份恢复 |
| themeSettings | P14 | ThemeSettingsScreen；外观概览 |
| exportedTopics | P12 | ExportedTopicsScreen；真实本地文件 |
| exportedHtml?path={path} | P12 | WebScreens.kt 的 ExportedHtmlScreen；离线/JS 关闭 |
| about | P17 | AboutScreen；版本与应用信息 |
| web?url={url} | P18 | WebScreens.kt 的 WebScreen；实际 URL/桥接 |
| blockWords | P13 | BlockWordsScreen；关键词/用户/板块规则 |
| inviteCenter | P18 | InviteCenterScreen；资格与已有操作 |
| userBlock?url={url} | P18 | SourceUserBlockScreen；保留源站目标参数 |
| gachaProfile | P18 | SourceGachaScreen(profile)；原站称号资料 |
| gachaCenter | P18 | SourceGachaScreen(center)；原站称号中心 |
| gachaMarket | P18 | SourceGachaScreen(market)；原站市场 |
| gachaOperation/{kind}?title={title} | P18 | SourceGachaScreen(kind)；保留 kind 与标题兼容 |
| leaderboard?type={type} | P18 | LeaderboardScreen；真实类型与来源返回 |
| notifications | P08 | NotificationsScreen；映射消息通知分区 |
| directMessages | P08 | DirectMessagesScreen；保留二级来源返回 |
| topicCollections?tab={tab} | P07 | TopicCollectionsScreen；保留 everyone/mine |
| directMessagesRoot | P01/P08 | DirectMessagesScreen(showBack=false)；消息私信根 |
| topicCollectionsRoot | P01/P07 | TopicCollectionsScreen(showBack=false)；淘帖根 |
| collectionActions?path={path} | P07 | CollectionDetailScreen；保留 path |
| topicCollectionManage?path={path} | P07 | TopicCollectionManageScreen；管理任务 |
| footprint | P12 | FootprintScreen；本地历史与位置 |
| favorites | P12 | FavoritesScreen；帖子/评论收藏 |
| chat/{userId}?name={name}&avatar={avatar} | P09 | ChatScreen；会话对象/展示参数 |
| report/{type}/{id} | P18 | ReportScreen；举报目标契约 |
| sourcePromotions | P18 | SourcePromotionsScreen；原站广告 |
| userPrivacy | P18 | UserPrivacyScreen；原站隐私 |
| communityActivity | P18 | CommunityActivityScreen；原站活跃度 |
| attachments | P12/P18 | AttachmentsScreen；原站附件 |
| uploadSettings | P15 | SettingsDestinations.kt 的 UploadSettingsScreen |
| storageSettings | P17 | SettingsDestinations.kt 的 StorageSettingsScreen |
| myContent | P12 | SettingsDestinations.kt 的 MyContentScreen |
| cardRedemptions | P12 | CardRedemptionsScreen；兑换记录 |
| autoClearSettings | P17 | DataManagementScreen(automatic=true)；自动策略 |
| webDavSettings | P17 | TransferSettingsScreen(cloud=true)；WebDAV |
| aiConfiguration | P16 | AiSettingsScreen(section=configuration) |
| aiHistory | P12/P16 | AiSettingsScreen(section=history) |
| themeColors | P14 | ThemeSettingsScreen(section=colors) |
| themeBackground | P14 | ThemeSettingsScreen(section=background) |
| themeFonts | P14 | ThemeSettingsScreen(section=fonts) |
| themeLayout | P14 | ThemeSettingsScreen(section=layout) |

## 4. 开发交付检查

1. 按 P01—P18 标记页面已采用的共享模式、真实状态和兼容路由；“已套用颜色”不能等同该页面已完成。
2. 将新增布局组件放入公共组件/模式层；页面只装配 UI 状态与事件，不复制按钮、行、标题栏或登录门禁样式。
3. 同一页面分别提供正常、首次加载、空、整页失败、有内容失败、忙碌、禁用、游客样例；仅提供适用状态，说明不适用原因。
4. 每个双栏候选提供窄窗口、可用双栏宽度、大字号回退和铰链边界的布局证据。
5. 保留路由/深链/返回来源/偏好迁移清单，核对消息分区和发帖动作转换；不以新路由名称掩盖旧入口丢失。
6. 完整验收执行与记录按 [实施与验收](IMPLEMENTATION_AND_ACCEPTANCE.md)；本篇线框和条款属于设计要求，不是实现结果。
