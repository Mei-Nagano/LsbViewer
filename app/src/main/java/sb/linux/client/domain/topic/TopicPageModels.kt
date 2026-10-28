package sb.linux.client.domain.topic

import sb.linux.client.domain.account.LoginVerification

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
    val coinTiers: List<Int> = emptyList(), // 源站 data-tiers 限定的评论投币档位
    val isOp: Boolean = false,
    val editInfo: String = "",       // 最后编辑信息（从正文中拆出，与正文有分割线区分）
    val editUserId: Long = 0,        // 最后编辑人用户 ID（>0 时点击可跳转其主页）
    val editUserName: String = "",   // 最后编辑人用户名（editInfo 中高亮可点击部分）
    val referencedFloors: List<Int> = emptyList(), // 本楼引用的楼层（整楼 HTML 解析，含正文外的引用块）
    val online: Boolean = false,     // 作者在线（源站 data-online-users-ids 在线集合）
    val titleBadge: TitleBadge? = null, // 作者称号（源站 gacha-title-badge）
    val redPacketReward: String = "",
    val signatureHtml: String = "",
    val userBlocked: Boolean = false,
    val blockedPlaceholder: String = "",
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
    val redPacket: RedPacketPanel? = null,
    val lottery: LotteryPanel? = null,      // 抽奖帖面板
    val poll: TopicPoll? = null,            // 源站投票表单/结果
    val replyCaptcha: LoginVerification? = null, // 回复表单人机验证（CAP / 旧版数学题）
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

