package sb.linux.client.data

/** 帖子卡片（首页/板块/搜索/用户主题列表通用） */
data class TopicCard(
    val topicId: Long,
    val title: String,
    val authorId: Long,
    val authorName: String,
    val forumId: Long,
    val forumName: String,
    val replies: Int,
    val lastReplier: String,
    val timeText: String,
    val pinned: Boolean = false,
    val hot: Boolean = false,
    val featured: Boolean = false,     // 精华
    val unread: Boolean = false,       // 未读（登录后源站标记）
    val titleColor: String = "",       // 源站彩色标题（style color）
    val avatarUrl: String = "",
    val pages: List<Int> = emptyList(),
    val lotteryStatus: String = "",    // 抽奖帖状态：抽奖中 / 已开奖
    val cardStatus: String = "",       // 发卡帖状态：发卡中 / 发卡结束
    val views: Int = -1,               // 阅读量（源站首页列表无此数据，-1 = 未浏览过；来自详情页缓存）
    val online: Boolean = false,       // 作者在线（源站 data-online-users-ids 在线集合）
    val titleBadge: TitleBadge? = null, // 作者称号（源站 gacha-title-badge，仅展示不参与排序）
)

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
)

/** 本地收藏的评论快照（纯本地，34） */
data class CommentFavorite(
    val replyId: Long,
    val topicId: Long,
    val topicTitle: String,
    val floor: Int,
    val authorId: Long,
    val authorName: String,
    val avatarUrl: String,
    val content: String,     // 纯文本摘要（收藏时从正文 HTML 提取）
    val at: Long,
    val timeText: String = "",
)

/** 帖子页楼层 */
data class PostEntry(
    val id: Long,
    val floor: Int,            // 0 = 楼主
    val parentFloor: Int = 0,  // 树形评论：本楼回复的楼层（源站 data-quote-threads-parent-floor）；0 = 无上级
    val authorId: Long,
    val authorName: String,
    val avatarUrl: String,
    val userGroup: String,
    val authorUid: Long = 0,         // 源站 UID 徽章（有头衔的用户也会展示）
    val timeText: String,
    val ipLocation: String,
    val contentHtml: String,
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val coined: Boolean = false,
    val likeCoinType: String = "",   // topic / reply
    val canLike: Boolean = false,
    val isOp: Boolean = false,
    val editInfo: String = "",       // 最后编辑信息（从正文中拆出，与正文有分割线区分）
    val editUserId: Long = 0,        // 最后编辑人用户 ID（>0 时点击可跳转其主页）
    val editUserName: String = "",   // 最后编辑人用户名（editInfo 中高亮可点击部分）
    val referencedFloors: List<Int> = emptyList(), // 本楼引用的楼层（整楼 HTML 解析，含正文外的引用块）
    val online: Boolean = false,     // 作者在线（源站 data-online-users-ids 在线集合）
    val titleBadge: TitleBadge? = null, // 作者称号（源站 gacha-title-badge）
    val essenceVoteLabel: String = "", // 申精评议回复标签（支持/反对）
    val essenceVoteSupport: Boolean? = null,
)

data class TopicPageData(
    val topicId: Long,
    val title: String,
    val titleTag: String = "",       // 正文标签（标题旁的状态徽章：已开奖 / 抽奖中等）
    val posts: List<PostEntry>,
    val page: Int,
    val totalPages: Int,
    val csrf: String,
    val canFavorite: Boolean,
    val favorited: Boolean = false, // 打开帖子时是否已收藏（源站收藏表单按钮为「取消收藏」）
    val favAction: String,       // 收藏表单 action
    val donateUrl: String?,      // 打赏入口
    val virtualCard: VirtualCard? = null,   // 发卡帖兑换卡片
    val loginVisibleReplies: Int = -1,      // >=0 表示未登录时评论区隐藏条数
    val quickReplyAuthor: String = "",      // 快速回复目标（楼主）
    val viewsText: String = "",             // 查看次数（源站展示时）
    val repliesText: String = "",           // 回复总数（post-content-stats 第二个 span）
    val lottery: LotteryPanel? = null,      // 抽奖帖面板
    val poll: TopicPoll? = null,            // 源站投票表单/结果
    val replyCaptcha: NativeCaptcha? = null, // 回复表单人机验证（抽奖帖等）
    val essenceApplication: EssenceApplication? = null,
    val essenceReview: EssenceReview? = null,
)

/** 未申请状态与可用操作均来自源站，不在客户端猜测字数、权限或冷却规则。 */
data class EssenceApplication(
    val title: String,
    val note: String,
    val action: String = "",
    val fields: Map<String, String> = emptyMap(),
    val label: String = "申请加精",
    val enabled: Boolean = false,
)

/** 申精评议面板。进度可能为负数，因此不能直接复用 progress 的非负 value。 */
data class EssenceReview(
    val title: String = "精华申请",
    val subtitle: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val progress: Int = 0,
    val target: Int = 0,
    val progressText: String = "",
    val progressNote: String = "",
    val meta: List<String> = emptyList(),
    val poolTitle: String = "",
    val poolItems: List<String> = emptyList(),
    val actionNote: String = "",
    val topUp: EssencePointsTopUp? = null,
)

/** 作者给申精奖池追加积分的动态表单，字段名和额度限制均以源站 HTML 为准。 */
data class EssencePointsTopUp(
    val action: String,
    val fields: Map<String, String> = emptyMap(),
    val amountName: String,
    val defaultAmount: String = "",
    val min: Int = 1,
    val max: Int = Int.MAX_VALUE,
    val label: String = "追加积分",
    val hint: String = "",
    val submitLabel: String = "追加",
    val enabled: Boolean = true,
)

/** 源站 native-captcha 人机验证组件（数学题 + PoW） */
data class NativeCaptcha(
    val question: String,        // 例如 "10 - 3 = ?"
    val token: String,
    val powPrefix: String,
    val powZeros: Int = 3,
)

/** 抽奖中奖人：用户 + 奖品名 +（仅本人可见的）兑换码 */
data class LotteryWinner(
    val user: String,
    val prize: String,
    val code: String = "",      // 登录者本人中奖时源站下发，其余人为空
)

/** 发卡帖「我的购买记录」条目 */
data class CardOrder(
    val code: String,           // 兑换码
    val time: String,           // 例如 "3天前"
    val price: String,          // 例如 "140 积分"
)

data class LotteryPanel(
    val status: String = "",                // 抽奖中 / 已开奖
    val note: String = "",                  // 参与说明（回复满 5 字即参与…）
    val participants: String = "",          // 例如 "11 人参与"
    val prizes: List<String> = emptyList(), // 奖品列表
    val condition: String = "",             // 开奖条件（满 150 人自动开奖）
    val result: String = "",                // 开奖结果（实际中奖 5 人）
    val winners: List<LotteryWinner> = emptyList(), // 中奖名单
    val rules: String = "",                 // 规则说明
    val joinAction: String = "",            // 参与表单 action，空 = 不可参与
    val joinFields: Map<String, String> = emptyMap(), // 参与表单隐藏字段
    val options: List<Pair<String, String>> = emptyList(), // 参与选项 value to label
    val joinedText: String = "",            // 已参与的提示
)

/** 源站投票面板：字段名和 action 由页面动态提取，不绑定某一版接口。 */
data class TopicPoll(
    val title: String = "投票",
    val note: String = "",
    val action: String = "",
    val fields: Map<String, String> = emptyMap(),
    val options: List<TopicPollOption> = emptyList(),
    val multiple: Boolean = false,
    val submitLabel: String = "提交投票",
    val closed: Boolean = false,
    val unavailableReason: String = "",
    val reasonName: String = "",
    val reasonRequired: Boolean = false,
    val reasonHint: String = "",
    val reasonMaxLength: Int = 300,
)

data class TopicPollOption(
    val name: String,
    val value: String,
    val label: String,
    val selected: Boolean = false,
    val disabled: Boolean = false,
)

/** 打赏弹幕条目 */
data class DanmakuItem(
    val user: String,
    val amount: String,
    val action: String,
    val avatarUrl: String = "",
)

/** 发卡帖虚拟卡片 */
data class VirtualCard(
    val kicker: String,          // 例如 "积分兑换"
    val title: String,
    val status: String,          // 例如 "库存 14"
    val priceText: String,       // 例如 "1 积分 / 张"
    val tip: String,
    val footText: String,        // 例如 "已兑换 6 张 · 每人限购 1 张"
    val buyAction: String,            // 兑换表单 action，空 = 不可兑换（未登录无表单）
    val buyFields: Map<String, String> = emptyMap(), // 兑换表单隐藏字段
    val inStock: Boolean,
    val orders: List<CardOrder> = emptyList(), // 我的购买记录（登录且兑换过才有）
)

data class IdentityRequirement(
    val label: String,
    val met: Boolean,
)

data class IdentityCenterData(
    val benefits: List<Pair<String, String>> = emptyList(),
    val creatorRequirements: List<IdentityRequirement> = emptyList(),
    val accountRequirements: List<IdentityRequirement> = emptyList(),
    val email: String = "",
    val emailVerified: Boolean = false,
    val canApply: Boolean = false,
    val applicationRecords: List<String> = emptyList(),
    val notice: String = "",
)

data class ForumInfo(
    val id: Long,
    val name: String,
    val description: String = "",
    val topicCount: String = "",
)

data class UserProfile(
    val userId: Long,
    val username: String,
    val avatarUrl: String,
    val rankText: String,       // 例如 "饼友 · 积分 19"
    val userGroup: String = "", // 例如 "饼友"
    val points: String = "",    // 例如 "1324"
    val bio: String = "",
    val stats: List<Pair<String, String>>, // 侧栏统计
    val online: Boolean = false, // 用户当前在线（源站 data-online-users-ids 在线集合）
    val titleBadge: TitleBadge? = null, // 用户称号（源站 gacha-title-badge）
)

/** 源站称号系统徽章（gacha-title-badge：图标 + 名称 + 稀有度） */
data class TitleBadge(
    val icon: String = "",     // emoji 图标，如 🐉
    val name: String = "",     // 称号名，如 传说之龙
    val rarity: String = "",   // 稀有度，如 SSR（取自类名 gacha-title-<x>）
    val serial: String = "",   // UR 限定序列号，如 058
)

/** 邀请中心（/invite_center）：分享链接 + 奖励统计 + 已邀请用户 */
data class InviteCenter(
    val link: String,                        // 我的分享链接，如 https://linux.sb/s/xxxx
    val desc: String,                        // 奖励规则说明
    val rule: String,                        // 违规警告文案
    val invitedCount: String,                // 已邀请用户数
    val firstReward: String,                 // 首奖积分（N 人）
    val secondReward: String,                // 二奖积分（N 人）
    val users: List<InviteUser> = emptyList(),
)

/** 通过分享链接注册的用户（.invite-center-list 列表项） */
data class InviteUser(
    val name: String,
    val userId: Long = 0,     // 0 = 解析不到用户链接
    val statusText: String = "", // 条目里除用户名外的状态文本（注册时间/奖励状态等）
)

/** 我的称号（/gacha_profile）：称号收藏列表 */
data class GachaProfile(
    val stat: String,                             // 头部统计，如 "3 种称号"
    val titles: List<GachaTitleItem> = emptyList(),
)

/** 收藏的单个称号条目：徽章 + 装备状态 + 可执行操作（装备/卸下/赠送等表单） */
data class GachaTitleItem(
    val badge: TitleBadge,
    val equipped: Boolean = false,                // 已装备（条目标记/文本含「已装备」）
    val count: Int = 1,                           // 持有数量
    val actions: List<GachaAction> = emptyList(), // 页面内可提交的操作表单
)

/** 称号操作：源站表单（action + 隐藏字段 + 按钮文本），提交后由页面反馈结果 */
data class GachaAction(
    val label: String,                 // 按钮文本，如 装备 / 卸下 / 赠送
    val action: String,                // 表单提交地址
    val fields: Map<String, String>,   // 隐藏字段（含称号 id 等，不含 _csrf）
    val enabled: Boolean = true,
)

/** 称号抽取中心（/gacha）。 */
data class GachaCenter(
    val pointsText: String = "",
    val statsText: String = "",
    val pullActions: List<GachaAction> = emptyList(),
    val pool: List<GachaPoolRow> = emptyList(),
    val allTitles: List<TitleBadge> = emptyList(),
    val news: List<String> = emptyList(),
)

data class GachaPoolRow(
    val rarity: String,
    val countText: String,
    val rateText: String,
)

/** 称号交易市场当前页（/gacha_market）。 */
data class GachaMarketPage(
    val summary: String = "",
    val note: String = "",
    val listings: List<GachaMarketListing> = emptyList(),
    val currentPage: Int = 1,
    val lastPage: Int = 1,
)

data class GachaMarketListing(
    val badge: TitleBadge,
    val price: Int = 0,
    val available: Int = 1,
    val timeLeft: String = "",
    val action: GachaAction,
)

/** 称号系统的动态操作页。字段直接来自源站表单，兼容熔炼、回收、UR 合成和市场发布。 */
data class GachaOperationPage(
    val title: String = "",
    val notes: List<String> = emptyList(),
    val forms: List<GachaOperationForm> = emptyList(),
    val records: List<String> = emptyList(),
    val links: List<Pair<String, String>> = emptyList(),
)

data class GachaOperationForm(
    val label: String,
    val action: String,
    val hiddenFields: List<Pair<String, String>> = emptyList(),
    val fields: List<GachaFormField> = emptyList(),
    val enabled: Boolean = true,
    val minSelections: Int = 0,
)

data class GachaFormField(
    val name: String,
    val label: String,
    val groupLabel: String = "",
    val type: String = "text",
    val value: String = "",
    val placeholder: String = "",
    val min: String = "",
    val max: String = "",
    val required: Boolean = false,
    val checked: Boolean = false,
    val options: List<GachaFormOption> = emptyList(),
    val maxLength: Int = Int.MAX_VALUE,
    val multiple: Boolean = false,
)

data class GachaFormOption(
    val value: String,
    val label: String,
    val selected: Boolean = false,
    val disabled: Boolean = false,
)

data class NotificationItem(
    val fromUser: String,
    val typeText: String,
    val timeText: String,
    val content: String,
    val link: String,
    val unread: Boolean = false,   // 源站未读状态（li.unread / span.notification-unread）
    val isTopic: Boolean = true,   // 是否确为帖子类通知（link 为 /topic/{id}）；私信等非帖子通知为 false
    val partnerId: Long = 0,       // 私信对方 uid（/notify/{uid} 或 /user/{uid}）；非私信为 0
    val partnerAvatar: String = "",// 私信对方头像 URL
    val quoteText: String = "",    // 私信回复引用的我方消息文本（blockquote），无引用为空
)

/** 本地聚合的一条私信消息（收信来自通知，去信为本地发送记录） */
data class PmMessage(
    val partnerId: Long,
    val partnerName: String,   // 对方用户名
    val avatarUrl: String = "",//
    val incoming: Boolean,     // true = 对方发来；false = 我发出去的
    val content: String,
    val timeText: String,      // 精确：MM-dd HH:mm；天级（通知来的）：昨天 / MM-dd
    val ts: Long = 0L,
    val seq: Long = 0L,        // 插入序号：同一天内按此排序（天级时间分不出先后）
    val timeExact: Boolean = true, // 时间是否精确到分（我发出的=精确；通知解析的只有天级）
    val quoteText: String = "",    // 对方回复时引用的我方消息（回复上下文预览）
    val synthetic: Boolean = false,// true = 依据对方回复里的引用补造的我方消息（非 App 内发送）
)

/** 新版 /direct_messages 联系人列表与服务端会话。 */
data class DirectMessageContact(
    val userId: Long,
    val username: String,
    val avatarUrl: String = "",
    val timeText: String = "",
    val preview: String = "",
)

data class DirectMessageThread(
    val partnerId: Long,
    val partnerName: String,
    val avatarUrl: String = "",
    val messages: List<PmMessage> = emptyList(),
)

data class TopicCollectionCard(
    val collectionId: Long,
    val title: String,
    val authorId: Long,
    val authorName: String,
    val avatarUrl: String = "",
    val visibility: String = "",
    val articleCount: String = "",
    val updatedText: String = "",
    val description: String = "",
    val subscribed: Boolean = false,
)

data class LeaderRow(
    val rank: Int,
    val userId: Long,
    val username: String,
    val userGroup: String,
    val valueText: String,
    val avatarUrl: String = "",
    val online: Boolean = false,   // 用户当前在线（源站 data-online-users-ids 在线集合）
    val titleBadge: TitleBadge? = null, // 用户称号（源站 gacha-title-badge）
)

data class PointRow(
    val timeText: String,
    val reason: String,
    val change: String,
    val positive: Boolean,
    val timestamp: Long = 0,
    val topicId: Long = 0,
)

data class ReplyItem(
    val topicId: Long,
    val title: String,
    val excerpt: String,
    val timeText: String,
    val replyId: Long = 0,      // 源站 ?replyid=，列表唯一 key，防重复崩溃
    val forumName: String = "",
)

/** 首页侧板（源站 aside.sidebar） */
data class HomeSidebar(
    val forums: List<ForumCount> = emptyList(),      // 版块列表（含主题数）
    val hotTopics: List<HotTopic> = emptyList(),     // 每日热帖
    val statsText: String = "",                      // 站点统计
    val newUsers: List<NewUser> = emptyList(),       // 最新用户
    val onlineUsers: OnlineUsers = OnlineUsers(),    // 当前在线
)

/** 首页侧板「当前在线」卡片 */
data class OnlineUsers(
    val count: String = "",                          // 例如 "137 人"
    val items: List<OnlineUser> = emptyList(),       // 源站展示的在线用户（头像 + 用户名）
    val moreText: String = "",                       // 例如 "还有 120 人在线"
)

data class OnlineUser(
    val userId: Long,
    val username: String,
    val avatarUrl: String = "",
)


data class ForumCount(
    val id: Long,
    val name: String,
    val count: String,
    val color: String = "",
)

data class HotTopic(
    val topicId: Long,
    val title: String,
    val meta: String,          // 例如 "近 24 小时 103 回复"
)

data class NewUser(
    val userId: Long,
    val username: String,
    val avatarUrl: String = "",
)

data class LoginState(
    val loggedIn: Boolean = false,
    val userId: Long = 0,
    val username: String = "",
    val points: String = "",
    val avatarUrl: String = "",
    val userGroup: String = "",
    val titleBadge: TitleBadge? = null, // 当前佩戴称号，与用户组、积分分开保存
)
