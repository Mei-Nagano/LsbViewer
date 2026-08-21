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
    val online: Boolean = false,       // 作者在线（源站头像上的 online-users-dot）
)

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
)

/** 帖子页楼层 */
data class PostEntry(
    val id: Long,
    val floor: Int,            // 0 = 楼主
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
    val online: Boolean = false,     // 作者在线（源站头像上的 online-users-dot）
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
    val favAction: String,       // 收藏表单 action
    val donateUrl: String?,      // 打赏入口
    val virtualCard: VirtualCard? = null,   // 发卡帖兑换卡片
    val loginVisibleReplies: Int = -1,      // >=0 表示未登录时评论区隐藏条数
    val quickReplyAuthor: String = "",      // 快速回复目标（楼主）
    val viewsText: String = "",             // 查看次数（源站展示时）
    val repliesText: String = "",           // 回复总数（post-content-stats 第二个 span）
    val lottery: LotteryPanel? = null,      // 抽奖帖面板
    val replyCaptcha: NativeCaptcha? = null, // 回复表单人机验证（抽奖帖等）
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
    val online: Boolean = false, // 用户当前在线（主页大头像上的 online-users-dot）
)

data class NotificationItem(
    val fromUser: String,
    val typeText: String,
    val timeText: String,
    val content: String,
    val link: String,
    val unread: Boolean = false,   // 源站未读状态（li.unread / span.notification-unread）
    val isTopic: Boolean = true,   // 是否确为帖子类通知（link 为 /topic/{id}）；私信等非帖子通知为 false
)

data class LeaderRow(
    val rank: Int,
    val userId: Long,
    val username: String,
    val userGroup: String,
    val valueText: String,
    val avatarUrl: String = "",
    val online: Boolean = false,   // 用户当前在线
)

data class PointRow(
    val timeText: String,
    val reason: String,
    val change: String,
    val positive: Boolean,
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
    val recentForums: List<ForumInfo> = emptyList(), // 最近浏览版块
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

/** 邀请中心（源站 /invite_code） */
data class InviteCenter(
    val subtitle: String = "",             // 例如 "注册可选填写邀请码"
    val description: String = "",          // 兑换规则说明（消耗积分换码 + 被邀奖励）
    val canExchange: Boolean = false,      // 是否存在可提交的兑换表单
    val csrf: String = "",
    val codes: List<InviteCode> = emptyList(),
    val invited: List<InvitedUser> = emptyList(),
    val emptyCodesText: String = "",
    val emptyInvitedText: String = "",
)

/** 一个可用的邀请码 */
data class InviteCode(
    val code: String = "",
)

/** 通过邀请码注册的用户 */
data class InvitedUser(
    val userId: Long = 0,
    val username: String = "",
    val avatarUrl: String = "",
    val statusText: String = "",
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
)
