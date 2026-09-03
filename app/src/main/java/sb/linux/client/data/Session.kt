package sb.linux.client.data
import androidx.compose.runtime.mutableFloatStateOf

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 首页各分类的浏览状态（保存在 Session，进帖返回后仍在原位置） */
class HomeTabState {
    var topics by mutableStateOf<List<TopicCard>>(emptyList())
    var page by mutableIntStateOf(1)
    var totalPages by mutableIntStateOf(1)
    /** 已加载到的源站页码与源站总页数（翻页模式下本地按条数切片分页用） */
    var sourcePage by mutableIntStateOf(1)
    var sourceTotalPages by mutableIntStateOf(1)
    var loaded by mutableStateOf(false)   // 是否已有数据（含缓存）
    var scrollIndex by mutableIntStateOf(0)
    var scrollOffset by mutableIntStateOf(0)
}

data class PendingVerification(
    val url: String,
    val html: String,
    val deferred: CompletableDeferred<Boolean>,
)

/** 搜索结果缓存条目（带时间戳，超时后失效重搜） */
data class SearchResultCache(
    val query: String,
    val field: String,
    val page: Int,
    val totalPages: Int,
    val results: List<TopicCard>,
    val time: Long,
)

data class TopicReadingPosition(
    val floor: Int,
    val listIndex: Int,
    val scrollOffset: Int,
)

data class AiSummaryRecord(
    val topicId: Long,
    val title: String,
    val summary: String,
    val savedAt: Long,
    val recordId: String = topicId.toString(),
    val favorite: Boolean = false,
)

/**
 * 全局会话状态：登录态、当前用户、主题偏好、应用设置。
 */
class Session(app: Application) : AndroidViewModel(app) {

    val client: LsbClient = (app as sb.linux.client.LsbApp).client
    val settings: AppSettings = AppSettings(app)

    /** 再次点击已选中的首页标签时递增，由 HomeScreen 消费并执行真实刷新。 */
    var homeRefreshRequest by mutableIntStateOf(0)
        private set

    fun requestHomeRefresh() { homeRefreshRequest++ }

    var loginState by mutableStateOf(LoginState())
        private set
    var booting by mutableStateOf(true)
        private set

    // 应用内偏好
    var themeMode by mutableStateOf(ThemeModePref.SYSTEM)
    var dynamicColor by mutableStateOf(true)
    var themeColorKey by mutableStateOf("default")
    var oledDark by mutableStateOf(false)
    // 调色风格（PaletteStyle 枚举序数）与对比度等级（-1..1，0 为默认）
    var themeStyle by mutableStateOf(0)
    var themeContrast by mutableStateOf(0f)
    // 元素级颜色覆盖（ColorBlendr 风格）：null=跟随种子色生成的方案，非空=独立覆盖该角色
    var themePrimary by mutableStateOf<Color?>(null)
    var themeSecondary by mutableStateOf<Color?>(null)
    var themeTertiary by mutableStateOf<Color?>(null)
    // 禁止页面染色：背景、卡片和普通文字使用中性色，主题强调色与手动背景保留；并非锁定日夜同色。
    var themeBackground by mutableStateOf<Color?>(null)
    var keepBackgroundColor by mutableStateOf(true)
    var backgroundImage by mutableStateOf("")
    var backgroundImageEnabled by mutableStateOf(false)
    var backgroundImageOpacity by mutableStateOf(0.25f)
    var surfaceOpacity by mutableFloatStateOf(1f)
    // 用户保存的自定义主题色预选（colorToKey 格式，如 custom#RRGGBB）
    var themeCustomColors by mutableStateOf<List<String>>(emptyList())
    // 应用字体（24）：default / sans / serif / monospace / custom（自定义字体文件）
    var fontKey by mutableStateOf("default")
    var customFontName by mutableStateOf("")
    var fontScale by mutableStateOf(1f)
    var fontWeightLevel by mutableIntStateOf(0)
    // 首页帖子卡片元素级自定义颜色（外观设置-实时预览点按改色）：
    // key ∈ tag/title/time/user/views/comments/forum，null=未覆盖（跟随主题）
    var cardColors by mutableStateOf<Map<String, Color>>(emptyMap())
        private set
    var toast by mutableStateOf<String?>(null)
    var pendingVerification by mutableStateOf<PendingVerification?>(null)
        private set

    // 应用设置（可变状态，改动即时生效）
    var infiniteScroll by mutableStateOf(true)
    // 各分类（home/forum/topic）滚动模式覆盖的 Compose 响应式镜像。
    // settings.scrollModeOverrides 存于 SharedPreferences（非 Compose 状态），
    // 若直接写它不会触发 UI 重组，导致切换翻页/无限滚动后仍按旧模式渲染（需刷新才生效、底部滑动错乱）。
    var scrollModeOverrides by mutableStateOf<Map<String, Boolean>>(emptyMap())
    var homeCacheEnabled by mutableStateOf(false)
    var collapseNewTopics by mutableStateOf(false)
    var danmakuOn by mutableStateOf(true)
    var sidebarTwoColumns by mutableStateOf(true)
    var showOnlineUsers by mutableStateOf(true)
    // 评论「对话串联」开关：Compose 响应式镜像（直接写 settings 不触发重组，开关显示不同步）
    var threadDialogEnabled by mutableStateOf(false)
    // 取消收藏确认框开关：Compose 响应式镜像
    var unfavoriteConfirm by mutableStateOf(true)
    // 翻页模式下每页展示的帖子条数（默认 15）
    var topicsPerPage by mutableIntStateOf(15)
    // 评论区排序：0 = 热度，1 = 正序，2 = 倒序（记住上次选择）
    var commentSortOrder by mutableIntStateOf(0)
    var tableDisplayMode by mutableIntStateOf(0)
    var showUid by mutableStateOf(true)
    var smartDecodeEnabled by mutableStateOf(false)
    // 平板模式（宽屏双栏布局）：Compose 响应式镜像，改动即时切换布局
    var tabletMode by mutableStateOf(false)
    // 底栏样式：0 = 经典通栏，1 = 液态玻璃悬浮胶囊（默认，原「贴底通栏」铺满样式已移除）：
    // 响应式镜像，改动即时切换
    var bottomBarStyle by mutableIntStateOf(1)
    var bottomBarItems by mutableStateOf(listOf("home", "me"))
    // 评论底栏样式（帖子内底部快捷回复栏）：0 = 经典通栏，1 = 液态玻璃悬浮胶囊（默认）：
    // 响应式镜像，改动即时切换
    var replyBarStyle by mutableIntStateOf(1)
    var linkPreviewEnabled by mutableStateOf(true)
    // 临时钉住的帖子：只在当前进程内保留，重新进入该帖时自动清除。
    var floatingTopicId by mutableStateOf(0L)
    var floatingTopicTitle by mutableStateOf("")
    var floatingTopicSnapshot: PinnedTopicSnapshot? = null

    // 首页状态保持：当前分类 / 组合过滤 / 各分类列表与滚动位置
    var homeTabIndex by mutableIntStateOf(0)
    var homeCombo by mutableIntStateOf(0)
    var homeSortDrawerOpen by mutableStateOf(true)   // 排序抽屉折叠状态（持久化）
    val homeTabs = List(5) { HomeTabState() }

    // 源站未读通知数（首页 .notify-badge 解析）。通知红点据此判断，完全跟随源站：
    // 进入通知页时源站会自动把全部通知标记为已读，届时置 0。
    var notifUnreadCount by mutableIntStateOf(0)
        private set

    // 帖子阅读量缓存（详情页解析后回填，首页卡片展示）
    val topicViews = mutableStateMapOf<Long, Int>()

    // 评论区解析缓存：同一次会话内不再重复请求源站；
    // 写入时间用于 TTL 过期判定（见 getTopicPage）
    val topicPageCache = mutableStateMapOf<String, TopicPageData>()
    private val topicPageCacheTimes = mutableMapOf<String, Long>()

    companion object {
        /** 帖子页缓存有效期：10 分钟。过期后重进帖子会重新抓取，
         *  保证能看到源站新增的评论与页码变化。 */
        private const val TOPIC_PAGE_CACHE_TTL_MS = 10 * 60 * 1000L
    }

    val aiSummaryCache = mutableStateMapOf<Long, String>()
    val danmakuCache = mutableStateMapOf<Long, List<DanmakuItem>>()

    // 搜索结果缓存：从结果页进入帖子再返回时直接复用，避免重新搜索
    var searchCache: SearchResultCache? by mutableStateOf(null)

    // 签到信息（连续/累计天数，登录后从源站解析）
    var checkinText by mutableStateOf("")
        private set

    /** 今日是否已签到（解析自签到页，决定侧边栏「去签到 / 已签到」文案） */
    var checkinCheckedToday by mutableStateOf(false)
        private set

    // 源站屏蔽词（服务端同步的缓存，登录后刷新）
    var blockedWords by mutableStateOf<Set<String>>(emptySet())
    var blockedUsers by mutableStateOf<Set<String>>(emptySet())

    private val sessionRecovery = SessionRecovery(
        scope = viewModelScope,
        hasCookie = client::hasAuthCookie,
        fetch = {
            val home = client.get("/")
            withContext(Dispatchers.IO) { sessionSnapshot(home.html, home.code) }
        },
        apply = ::applySessionSnapshot,
        settled = { booting = false },
    )

    /** 首页请求携带代次，防止登出或切换账号之前的旧响应重新覆盖身份。 */
    val sessionVersion: Long get() = sessionRecovery.version

    fun recoverSession() = sessionRecovery.recover()

    fun observeHomeSession(html: String, code: Int, version: Long) {
        sessionSnapshot(html, code)?.let { sessionRecovery.accept(it, version) }
            ?: sessionRecovery.recover()
    }

    private fun applySessionSnapshot(snapshot: SessionSnapshot) {
        val previous = loginState
        loginState = snapshot.login
        notifUnreadCount = snapshot.unread
        if (!loginState.loggedIn) {
            checkinText = ""
            checkinCheckedToday = false
        }
        if (previous.loggedIn != loginState.loggedIn || previous.userId != loginState.userId) {
            clearAllTopicPageCache()
            client.invalidateCsrf()
            settings.clearCheckinCache()
            if (loginState.loggedIn) refreshKeywordFilter()
        }
    }

    /** 从磁盘重新读取全部偏好到内存状态（导入设置备份后调用，立即生效无需重启） */
    fun reloadPrefs() {
        aiSummaryCache.clear()
        val prefs = getApplication<Application>().getSharedPreferences("lsb_prefs", android.content.Context.MODE_PRIVATE)
        themeMode = ThemeModePref.entries.getOrElse(prefs.getInt("theme_mode", 0)) { ThemeModePref.SYSTEM }
        dynamicColor = prefs.getBoolean("dynamic_color", true)
        themeColorKey = prefs.getString("theme_color", "default") ?: "default"
        oledDark = prefs.getBoolean("oled_dark", false)
        themeStyle = prefs.getInt("theme_style", 0)
        themeContrast = prefs.getFloat("theme_contrast", 0f)
        themePrimary = prefColor(prefs.getString("theme_primary_override", ""))
        themeSecondary = prefColor(prefs.getString("theme_secondary_override", ""))
        themeTertiary = prefColor(prefs.getString("theme_tertiary_override", ""))
        themeBackground = prefColor(prefs.getString("theme_background_override", ""))
        backgroundImage = prefs.getString("background_image", "").orEmpty()
        backgroundImageEnabled = prefs.getBoolean("background_image_enabled", false)
        backgroundImageOpacity = prefs.getFloat("background_image_opacity", 0.25f).coerceIn(0.05f, 0.65f)
        surfaceOpacity = prefs.getFloat("surface_opacity", 1f).coerceIn(0.2f, 1f)
        keepBackgroundColor = prefs.getBoolean("keep_background_color", true)
        themeCustomColors = (prefs.getStringSet("theme_custom_colors", emptySet()) ?: emptySet()).toList()
        fontKey = prefs.getString("font_key", "default") ?: "default"
        customFontName = prefs.getString("custom_font_name", "") ?: ""
        fontScale = prefs.getFloat("font_scale", 1f).coerceIn(0.85f, 1.30f)
        fontWeightLevel = prefs.getInt("font_weight_level", 0).coerceIn(0, 2)
        cardColors = parseCardColors(prefs.getString("card_colors", ""))
        sb.linux.client.ui.CardColorOverrides.map = cardColors
        infiniteScroll = settings.infiniteScroll
        scrollModeOverrides = settings.scrollModeOverrides
        homeCacheEnabled = settings.homeKeepCache
        collapseNewTopics = settings.collapseNewTopics
        danmakuOn = settings.danmakuOn
        homeSortDrawerOpen = settings.homeSortDrawerOpen
        sidebarTwoColumns = settings.sidebarTwoColumns
        threadDialogEnabled = settings.threadDialogEnabled
        unfavoriteConfirm = settings.unfavoriteConfirm
        topicsPerPage = settings.topicsPerPage
        commentSortOrder = settings.commentSortOrder
        tableDisplayMode = settings.tableDisplayMode
        showUid = settings.showUid
        smartDecodeEnabled = settings.smartDecodeEnabled
        showOnlineUsers = settings.sidebarShowOnlineUsers
        tabletMode = settings.tabletMode
        bottomBarStyle = settings.bottomBarStyle
        bottomBarItems = settings.bottomBarItems
        replyBarStyle = settings.replyBarStyle
        linkPreviewEnabled = settings.linkPreviewEnabled
        blockedWords = settings.blockedWords
        blockedUsers = settings.blockedUsers
    }

    init {
        reloadPrefs()
        if (settings.autoClearItems.isNotEmpty()) clearDataItems(settings.autoClearItems, notify = false)
        // 清理旧版本地已读通知键（已改为源站未读数方案）
        themePrefs().edit().remove("notif_read_keys").apply()
        client.verificationUi = { url, html -> verifyWithUi(url, html) }
        loadViewsCache()
        refreshSession()
        viewModelScope.launch {
            AppNetwork.recoveryEvents.collect { sessionRecovery.recover(networkChanged = true) }
        }
    }

    private fun themePrefs() =
        getApplication<Application>().getSharedPreferences("lsb_prefs", android.content.Context.MODE_PRIVATE)

    // ---------------- 通知未读状态（跟随源站） ----------------

    /** 更新源站未读通知数（首页 .notify-badge 解析结果；进入通知页 GET 后源站已全部标记已读，置 0） */
    fun updateNotifUnread(count: Int) {
        notifUnreadCount = count.coerceAtLeast(0)
    }

    fun saveTheme(mode: ThemeModePref, dyn: Boolean, colorKey: String = themeColorKey) {
        themeMode = mode
        dynamicColor = dyn
        themeColorKey = colorKey
        themePrefs().edit().putInt("theme_mode", mode.ordinal).putBoolean("dynamic_color", dyn)
            .putString("theme_color", colorKey).apply()
    }

    fun saveOledDark(v: Boolean) {
        oledDark = v
        themePrefs().edit().putBoolean("oled_dark", v).apply()
    }

    fun saveThemeStyle(index: Int) {
        themeStyle = index
        themePrefs().edit().putInt("theme_style", index).apply()
    }

    fun saveThemeContrast(v: Float) {
        themeContrast = v
        themePrefs().edit().putFloat("theme_contrast", v).apply()
    }

    // ---------------- 自定义字体（24） ----------------

    /** 自定义字体文件（固定私有路径）：custom 档位从此加载 */
    val customFontFile: java.io.File
        get() = java.io.File(getApplication<Application>().filesDir, "fonts/custom_font.ttf")

    fun saveFont(key: String) {
        fontKey = key
        themePrefs().edit().putString("font_key", key).apply()
    }

    fun saveFontScale(value: Float) {
        fontScale = value.coerceIn(0.85f, 1.30f)
        themePrefs().edit().putFloat("font_scale", fontScale).apply()
    }

    fun saveFontWeightLevel(value: Int) {
        fontWeightLevel = value.coerceIn(0, 2)
        themePrefs().edit().putInt("font_weight_level", fontWeightLevel).apply()
    }

    /** 导入自定义字体文件（24）：复制进私有目录并切换到 custom 档；校验可解析才生效 */
    suspend fun saveCustomFont(uri: android.net.Uri, displayName: String): Boolean {
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                customFontFile.parentFile?.mkdirs()
                app.contentResolver.openInputStream(uri)?.use { input ->
                    customFontFile.outputStream().use { input.copyTo(it) }
                } ?: return@runCatching false
                // 校验确为可用字体文件（损坏/选错文件时报失败）
                android.graphics.Typeface.createFromFile(customFontFile) != null
            }.getOrDefault(false)
        }
        if (ok) {
            customFontName = displayName
            fontKey = "custom"
            themePrefs().edit()
                .putString("font_key", "custom")
                .putString("custom_font_name", displayName)
                .apply()
        }
        return ok
    }

    /** 移除自定义字体文件并回到默认字体 */
    fun clearCustomFont() {
        runCatching { customFontFile.delete() }
        customFontName = ""
        fontKey = "default"
        themePrefs().edit()
            .putString("font_key", "default")
            .remove("custom_font_name")
            .apply()
    }

    /** 持久化元素级覆盖色（null=跟随种子色方案） */
    fun saveThemePrimary(c: Color?) { themePrimary = c; themePrefs().edit().putString("theme_primary_override", colorPref(c)).apply() }
    fun saveThemeSecondary(c: Color?) { themeSecondary = c; themePrefs().edit().putString("theme_secondary_override", colorPref(c)).apply() }
    fun saveThemeTertiary(c: Color?) { themeTertiary = c; themePrefs().edit().putString("theme_tertiary_override", colorPref(c)).apply() }
    fun saveThemeBackground(c: Color?) {
        themeBackground = c
        themePrefs().edit().putString("theme_background_override", colorPref(c)).apply()
    }
    suspend fun importBackgroundImage(uri: android.net.Uri) {
        val encoded = withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val result = coil.Coil.imageLoader(context).execute(coil.request.ImageRequest.Builder(context)
                .data(uri).size(1920, 1920).scale(coil.size.Scale.FIT).precision(coil.size.Precision.EXACT).allowHardware(false).build())
            val original = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: throw IllegalArgumentException("无法读取此背景图片")
            val flat = android.graphics.Bitmap.createBitmap(original.width, original.height, android.graphics.Bitmap.Config.ARGB_8888)
            val bytes = try {
                android.graphics.Canvas(flat).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(original, 0f, 0f, null) }
                java.io.ByteArrayOutputStream().use { out ->
                    check(flat.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)) { "图片处理失败" }
                    require(out.size() <= 4 * 1024 * 1024) { "背景图片处理后仍过大，请选择较小图片" }
                    out.toByteArray()
                }
            } finally { flat.recycle() }
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
        backgroundImage = encoded
        backgroundImageEnabled = true
        themePrefs().edit().putString("background_image", encoded).putBoolean("background_image_enabled", true).apply()
    }
    fun saveBackgroundImageEnabled(value: Boolean) {
        backgroundImageEnabled = value
        themePrefs().edit().putBoolean("background_image_enabled", value).apply()
    }
    fun saveBackgroundImageOpacity(value: Float) {
        backgroundImageOpacity = value.coerceIn(0.05f, 0.65f)
        themePrefs().edit().putFloat("background_image_opacity", backgroundImageOpacity).apply()
    }
    fun saveSurfaceOpacity(value: Float) {
        surfaceOpacity = value.coerceIn(0.2f, 1f)
        themePrefs().edit().putFloat("surface_opacity", surfaceOpacity).apply()
    }
    fun removeBackgroundImage() {
        backgroundImage = ""; backgroundImageEnabled = false
        themePrefs().edit().remove("background_image").remove("background_image_enabled").apply()
    }
    fun saveKeepBackgroundColor(v: Boolean) {
        keepBackgroundColor = v
        themePrefs().edit().putBoolean("keep_background_color", v).apply()
    }

    /** 把自定义主题色加入预选（去重），并持久化 */
    fun addThemeCustomColor(key: String) {
        if (key !in themeCustomColors) themeCustomColors = themeCustomColors + key
        persistCustomColors()
    }

    /** 从预选中移除自定义主题色 */
    fun removeThemeCustomColor(key: String) {
        themeCustomColors = themeCustomColors - key
        persistCustomColors()
    }

    private fun persistCustomColors() {
        themePrefs().edit().putStringSet("theme_custom_colors", themeCustomColors.toSet()).apply()
    }

    private fun colorPref(c: Color?): String =
        c?.let { (it.toArgb().toLong() and 0xFFFFFFL).toString(16).padStart(6, '0') } ?: ""

    private fun prefColor(s: String?): Color? =
        s?.takeIf { it.isNotEmpty() }?.toLongOrNull(16)?.let { Color(0xFF000000L or it) }

    /** 帖子卡片元素颜色持久化串：k1=hex;k2=hex */
    private fun serializeCardColors(m: Map<String, Color>): String =
        m.entries.joinToString(";") { "${it.key}=${colorPref(it.value)}" }

    private fun parseCardColors(s: String?): Map<String, Color> =
        s?.split(';')?.mapNotNull { e ->
            val i = e.indexOf('=')
            if (i <= 0) null else prefColor(e.substring(i + 1))?.let { e.substring(0, i) to it }
        }?.toMap() ?: emptyMap()

    /** 保存/清除帖子卡片某元素的自定义颜色（null=恢复跟随主题） */
    fun saveCardColor(key: String, c: Color?) {
        cardColors = if (c == null) cardColors - key else cardColors + (key to c)
        sb.linux.client.ui.CardColorOverrides.map = cardColors
        themePrefs().edit().putString("card_colors", serializeCardColors(cardColors)).apply()
    }

    fun saveInfiniteScroll(v: Boolean) {
        infiniteScroll = v
        settings.infiniteScroll = v
    }

    /** 持久化首页排序抽屉折叠状态 */
    fun saveHomeSortDrawerOpen(v: Boolean) {
        homeSortDrawerOpen = v
        settings.homeSortDrawerOpen = v
    }

    fun saveCollapseNewTopics(v: Boolean) {
        collapseNewTopics = v
        settings.collapseNewTopics = v
    }

    /** 某分类生效的滚动模式（3.13）：分类覆盖优先，否则全局开关 */
    fun effectiveInfiniteScroll(scope: String): Boolean =
        scrollModeOverrides[scope] ?: infiniteScroll

    /** 设置某分类的滚动模式覆盖值：即时更新 Compose 镜像并持久化 */
    fun saveScrollModeOverride(scope: String, infinite: Boolean) {
        scrollModeOverrides = scrollModeOverrides + (scope to infinite)
        settings.scrollModeOverrides = scrollModeOverrides
    }

    /** 清除全部分类覆盖：恢复跟随全局开关 */
    fun clearScrollModeOverrides() {
        scrollModeOverrides = emptyMap()
        settings.scrollModeOverrides = emptyMap()
    }

    // ---------------- 分类重置（3.9） ----------------

    /** 重置外观设置（含卡片元素色与自定义色预选） */
    fun resetThemeSettings() {
        themePrefs().edit()
            .remove("theme_mode").remove("dynamic_color").remove("theme_color")
            .remove("oled_dark").remove("theme_style").remove("theme_contrast")
            .remove("theme_primary_override").remove("theme_secondary_override").remove("theme_tertiary_override")
            .remove("theme_background_override").remove("keep_background_color")
            .remove("theme_custom_colors").remove("card_colors")
            .remove("font_key").remove("custom_font_name")
            .remove("font_scale").remove("font_weight_level")
            // exact_theme_colors 是已删除的「强调色精确取值」开关，顺手清掉旧值
            .remove("exact_theme_colors")
            .remove("background_image").remove("background_image_enabled").remove("background_image_opacity")
            .remove("surface_opacity")
            .apply()
        themeMode = ThemeModePref.SYSTEM
        dynamicColor = true
        themeColorKey = "default"
        oledDark = false
        themeStyle = 0
        themeContrast = 0f
        themePrimary = null; themeSecondary = null; themeTertiary = null
        themeBackground = null
        backgroundImage = ""; backgroundImageEnabled = false; backgroundImageOpacity = 0.25f
        surfaceOpacity = 1f
        keepBackgroundColor = true
        themeCustomColors = emptyList()
        cardColors = emptyMap()
        sb.linux.client.ui.CardColorOverrides.map = emptyMap()
        fontKey = "default"
        customFontName = ""
        fontScale = 1f
        fontWeightLevel = 0
    }

    /** 重置浏览设置并刷新内存镜像 */
    fun resetBrowseSettings() {
        settings.resetBrowse()
        infiniteScroll = settings.infiniteScroll
        scrollModeOverrides = settings.scrollModeOverrides
        homeCacheEnabled = settings.homeKeepCache
        collapseNewTopics = settings.collapseNewTopics
        danmakuOn = settings.danmakuOn
        homeSortDrawerOpen = settings.homeSortDrawerOpen
        sidebarTwoColumns = settings.sidebarTwoColumns
        threadDialogEnabled = settings.threadDialogEnabled
        unfavoriteConfirm = settings.unfavoriteConfirm
        topicsPerPage = settings.topicsPerPage
        commentSortOrder = settings.commentSortOrder
        tableDisplayMode = settings.tableDisplayMode
        showUid = settings.showUid
        smartDecodeEnabled = settings.smartDecodeEnabled
        showOnlineUsers = settings.sidebarShowOnlineUsers
    }

    /** 重置全部应用设置（一级菜单重置按钮）；平板模式迁入外观设置后此处显式恢复默认 */
    fun resetAllAppSettings() {
        resetThemeSettings()
        resetBrowseSettings()
        settings.resetAi()
        settings.resetGeneral()
        settings.resetWebdav()
        saveTabletMode(false)
    }

    fun saveTabletMode(v: Boolean) {
        tabletMode = v
        settings.tabletMode = v
    }

    /** 底栏样式：0 = 经典通栏，1 = 液态玻璃悬浮胶囊 */
    fun saveBottomBarStyle(v: Int) {
        val clamped = v.coerceIn(0, 1)
        bottomBarStyle = clamped
        settings.bottomBarStyle = clamped
    }

    fun saveBottomBarItems(items: List<String>) {
        settings.bottomBarItems = items
        bottomBarItems = settings.bottomBarItems
    }

    /** 评论底栏样式（帖子内底部快捷回复栏）：0 = 经典通栏，1 = 液态玻璃悬浮胶囊 */
    fun saveReplyBarStyle(v: Int) {
        val clamped = v.coerceIn(0, 1)
        replyBarStyle = clamped
        settings.replyBarStyle = clamped
    }

    fun saveLinkPreviewEnabled(v: Boolean) {
        linkPreviewEnabled = v
        settings.linkPreviewEnabled = v
    }

    fun pinFloatingTopic(topicId: Long, title: String) {
        floatingTopicId = topicId
        floatingTopicTitle = title.trim()
    }

    fun clearFloatingTopic(topicId: Long? = null) {
        if (topicId == null || floatingTopicId == topicId) {
            floatingTopicId = 0L
            floatingTopicTitle = ""
        }
    }

    fun saveDanmaku(v: Boolean) {
        danmakuOn = v
        settings.danmakuOn = v
    }

    fun saveSidebarTwoColumns(v: Boolean) {
        sidebarTwoColumns = v
        settings.sidebarTwoColumns = v
    }

    fun saveShowOnlineUsers(v: Boolean) {
        showOnlineUsers = v
        settings.sidebarShowOnlineUsers = v
    }

    fun saveThreadDialog(v: Boolean) {
        threadDialogEnabled = v
        settings.threadDialogEnabled = v
    }

    fun saveUnfavoriteConfirm(v: Boolean) {
        unfavoriteConfirm = v
        settings.unfavoriteConfirm = v
    }

    /** 翻页模式下每页展示的帖子条数（5..50） */
    fun saveTopicsPerPage(v: Int) {
        topicsPerPage = v.coerceIn(5, 50)
        settings.topicsPerPage = topicsPerPage
    }

    /** 评论区排序：0 = 热度，1 = 正序，2 = 倒序（持久化，记住上次选择） */
    fun saveCommentSortOrder(v: Int) {
        commentSortOrder = v.coerceIn(0, 2)
        settings.commentSortOrder = commentSortOrder
    }

    fun saveTableDisplayMode(v: Int) {
        tableDisplayMode = v.coerceIn(0, 1)
        settings.tableDisplayMode = tableDisplayMode
    }

    fun saveShowUid(v: Boolean) {
        showUid = v
        settings.showUid = v
    }

    fun saveSmartDecodeEnabled(v: Boolean) {
        smartDecodeEnabled = v
        settings.smartDecodeEnabled = v
    }

    /** 从源站拉取屏蔽词（含自定义词与屏蔽用户），仅更新缓存 */
    fun refreshKeywordFilter() {
        viewModelScope.launch {
            try {
                val s = client.getKeywordFilter()
                blockedWords = s.custom.toSet()
                blockedUsers = s.users.toSet()
                settings.blockedWords = blockedWords
                settings.blockedUsers = blockedUsers
            } catch (_: Exception) {
            }
        }
    }

    fun showToast(msg: String) {
        toast = msg
    }

    /** 通过全局对话框展示源站验证页，完成后返回结果供请求重试 */
    suspend fun verifyWithUi(url: String, challengeHtml: String): Boolean = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Boolean>()
        pendingVerification = PendingVerification(url, challengeHtml, deferred)
        // 超时给足时间完成 CF Turnstile / 手动答题，超时才让上层重试
        val ok = withTimeoutOrNull(150_000) { deferred.await() } ?: false
        if (pendingVerification?.deferred === deferred) pendingVerification = null
        ok
    }

    fun completeVerification(result: Boolean) {
        val p = pendingVerification
        if (p != null && !p.deferred.isCompleted) {
            p.deferred.complete(result)
            if (!result) pendingVerification = null
        }
    }

    // ---------------- 阅读量缓存 ----------------

    private val viewsPrefs by lazy {
        getApplication<Application>().getSharedPreferences("lsb_views", android.content.Context.MODE_PRIVATE)
    }

    private fun loadViewsCache() {
        runCatching {
            val o = org.json.JSONObject(viewsPrefs.getString("views", "{}") ?: "{}")
            o.keys().forEach { k -> k.toLongOrNull()?.let { topicViews[it] = o.optInt(k, -1) } }
        }
    }

    /**
     * 阅读量缓存持久化（上限 500 条）。
     * topicViews 是 SnapshotStateMap，迭代顺序不保证是插入序 —— 原先的 takeLast(500)
     * 因此丢弃的是「随机」条目而非最旧的。按 topicId 降序保留（id 越大帖子越新），
     * 淘汰结果稳定可预期。
     */
    private fun persistViews() {
        runCatching {
            val o = org.json.JSONObject()
            topicViews.entries.sortedByDescending { it.key }.take(500)
                .forEach { e -> o.put(e.key.toString(), e.value) }
            viewsPrefs.edit().putString("views", o.toString()).apply()
        }
    }

    /** 详情页解析到阅读量后回填（首页卡片同步展示） */
    fun saveTopicViews(topicId: Long, views: Int) {
        if (topicId > 0 && views >= 0 && topicViews[topicId] != views) {
            topicViews[topicId] = views
            persistViews()
        }
    }

    fun getTopicViews(topicId: Long): Int = topicViews[topicId] ?: -1

    fun saveTopicPage(topicId: Long, page: Int, data: TopicPageData) {
        if (data.page != page) return
        val key = "$topicId-$page"
        topicPageCache[key] = data
        topicPageCacheTimes[key] = System.currentTimeMillis()
    }

    /** 帖子页缓存读取：超过 TTL 视为过期并移除，回源重抓。
     *  评论区持续有新回复，进程常驻时旧缓存会导致重进帖子看不到新评论
     *  （表现为「评论显示不完全」、页码停留在旧值）。 */
    fun getTopicPage(topicId: Long, page: Int): TopicPageData? {
        val key = "$topicId-$page"
        val savedAt = topicPageCacheTimes[key] ?: return null
        if (System.currentTimeMillis() - savedAt > TOPIC_PAGE_CACHE_TTL_MS) {
            topicPageCache.remove(key)
            topicPageCacheTimes.remove(key)
            return null
        }
        return topicPageCache[key]?.takeIf { it.page == page }
    }

    /** 清除指定帖子的全部分页缓存（刷新时强制重新抓源站） */
    fun clearTopicPageCache(topicId: Long) {
        if (topicPageCache.isEmpty()) return
        val keys = topicPageCache.keys.filter { it.substringBefore("-") == topicId.toString() }
        keys.forEach {
            topicPageCache.remove(it)
            topicPageCacheTimes.remove(it)
        }
    }

    /** 清除全部帖子分页缓存（主页刷新后让帖子内容随之更新） */
    fun clearAllTopicPageCache() {
        topicPageCache.clear()
        topicPageCacheTimes.clear()
    }

    private val readingPositionPrefs by lazy {
        getApplication<Application>().getSharedPreferences(
            "lsb_reading_positions", android.content.Context.MODE_PRIVATE
        )
    }

    fun saveReadingPosition(topicId: Long, floor: Int, listIndex: Int, scrollOffset: Int) {
        if (topicId <= 0) return
        readingPositionPrefs.edit().putString(
            topicId.toString(), "$floor,$listIndex,$scrollOffset"
        ).apply()
    }

    fun getReadingPosition(topicId: Long): TopicReadingPosition? {
        val values = readingPositionPrefs.getString(topicId.toString(), null)
            ?.split(',') ?: return null
        if (values.size != 3) return null
        return TopicReadingPosition(
            floor = values[0].toIntOrNull() ?: return null,
            listIndex = values[1].toIntOrNull() ?: return null,
            scrollOffset = values[2].toIntOrNull() ?: return null,
        )
    }

    fun clearReadingPositions() = readingPositionPrefs.edit().clear().apply()

    private val aiSummaryPrefs by lazy {
        getApplication<Application>().getSharedPreferences("lsb_ai_summaries", android.content.Context.MODE_PRIVATE)
    }

    fun saveAiSummary(topicId: Long, summary: String, title: String = "") {
        if (summary.isBlank()) return
        aiSummaryCache[topicId] = summary
        val record = org.json.JSONObject()
            .put("topicId", topicId).put("title", title)
            .put("summary", summary).put("savedAt", System.currentTimeMillis())
        val recordId = "${topicId}_${java.util.UUID.randomUUID()}"
        aiSummaryPrefs.edit().putString(recordId, record.toString()).apply()
    }

    fun getAiSummary(topicId: Long): String? {
        aiSummaryCache[topicId]?.let { return it }
        val summary = aiSummaryHistory().firstOrNull { it.topicId == topicId }?.summary.orEmpty()
        return summary.takeIf { it.isNotBlank() }?.also { aiSummaryCache[topicId] = it }
    }

    fun aiSummaryHistory(): List<AiSummaryRecord> = aiSummaryPrefs.all.mapNotNull { (key, raw) ->
        runCatching {
            val o = org.json.JSONObject(raw as String)
            AiSummaryRecord(o.optLong("topicId"), o.optString("title"), o.optString("summary"), o.optLong("savedAt"), key, o.optBoolean("favorite"))
        }.getOrNull()?.takeIf { it.topicId > 0 && it.summary.isNotBlank() }
    }.sortedByDescending { it.savedAt }

    fun removeAiSummary(record: AiSummaryRecord) {
        aiSummaryCache.remove(record.topicId)
        aiSummaryPrefs.edit().remove(record.recordId).apply()
    }

    fun favoriteAiSummary(record: AiSummaryRecord, favorite: Boolean) {
        val raw = aiSummaryPrefs.getString(record.recordId, null) ?: return
        val value = org.json.JSONObject(raw).put("favorite", favorite)
        aiSummaryPrefs.edit().putString(record.recordId, value.toString()).apply()
    }

    fun clearAiSummaryHistory() {
        aiSummaryCache.clear()
        aiSummaryPrefs.edit().clear().apply()
    }

    fun saveDanmaku(topicId: Long, items: List<DanmakuItem>) {
        danmakuCache[topicId] = items
    }

    fun getDanmaku(topicId: Long): List<DanmakuItem>? = danmakuCache[topicId]

    // ---------------- 首页缓存（分类帖子列表持久化） ----------------

    private val homePrefs by lazy {
        getApplication<Application>().getSharedPreferences("lsb_home_cache", android.content.Context.MODE_PRIVATE)
    }

    fun saveHomeCache(sort: String, items: List<TopicCard>) {
        runCatching {
            val arr = org.json.JSONArray()
            items.forEach { t ->
                arr.put(
                    org.json.JSONObject()
                        .put("tid", t.topicId).put("title", t.title)
                        .put("aid", t.authorId).put("an", t.authorName)
                        .put("fid", t.forumId).put("fn", t.forumName)
                        .put("rp", t.replies).put("lp", t.lastReplier)
                        .put("tm", t.timeText).put("pin", t.pinned).put("hot", t.hot)
                        .put("fea", t.featured).put("tc", t.titleColor)
                        .put("av", t.avatarUrl)
                        .put("ls", t.lotteryStatus).put("cs", t.cardStatus)
                )
            }
            homePrefs.edit().putString("sort_$sort", arr.toString()).apply()
        }
    }

    fun loadHomeCache(sort: String): List<TopicCard> = runCatching {
        val arr = org.json.JSONArray(homePrefs.getString("sort_$sort", "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TopicCard(
                topicId = o.optLong("tid"), title = o.optString("title"),
                authorId = o.optLong("aid"), authorName = o.optString("an"),
                forumId = o.optLong("fid"), forumName = o.optString("fn"),
                replies = o.optInt("rp"), lastReplier = o.optString("lp"),
                timeText = o.optString("tm"), pinned = o.optBoolean("pin"),
                hot = o.optBoolean("hot"), featured = o.optBoolean("fea"),
                titleColor = o.optString("tc"), avatarUrl = o.optString("av"),
                lotteryStatus = o.optString("ls"), cardStatus = o.optString("cs"),
            )
        }
    }.getOrDefault(emptyList())

    fun clearHomeCache() = homePrefs.edit().clear().apply()

    // ---------------- 侧边栏本地永久缓存 ----------------

    private val sidebarPrefs by lazy {
        getApplication<Application>().getSharedPreferences("lsb_sidebar", android.content.Context.MODE_PRIVATE)
    }

    fun saveHomeSidebar(sb: HomeSidebar?) {
        if (sb == null) { sidebarPrefs.edit().clear().apply(); return }
        runCatching {
            val a = org.json.JSONArray()
            sb.forums.forEach { f ->
                a.put(org.json.JSONObject().put("id", f.id).put("name", f.name).put("count", f.count).put("color", f.color))
            }
            val h = org.json.JSONArray()
            sb.hotTopics.forEach { t ->
                h.put(org.json.JSONObject().put("tid", t.topicId).put("title", t.title).put("meta", t.meta))
            }
            val u = org.json.JSONArray()
            sb.newUsers.forEach { n ->
                u.put(org.json.JSONObject().put("uid", n.userId).put("name", n.username).put("av", n.avatarUrl))
            }
            // 在线用户（实时数据，缓存展示"上次已知在线状态"）
            val on = org.json.JSONArray()
            sb.onlineUsers.items.forEach { n ->
                on.put(org.json.JSONObject().put("uid", n.userId).put("name", n.username).put("av", n.avatarUrl))
            }
            val o = org.json.JSONObject()
                .put("forums", a).put("hot", h).put("users", u)
                .put("stats", sb.statsText)
                .put("onCount", sb.onlineUsers.count).put("onItems", on)
            sidebarPrefs.edit().putString("sidebar", o.toString()).apply()
        }
    }

    fun loadHomeSidebar(): HomeSidebar? = runCatching {
        val o = org.json.JSONObject(sidebarPrefs.getString("sidebar", "{}") ?: "{}")
        val forums = (0 until (o.optJSONArray("forums")?.length() ?: 0)).map { i ->
            val f = o.getJSONArray("forums").getJSONObject(i)
            ForumCount(f.optLong("id"), f.optString("name"), f.optString("count"), f.optString("color"))
        }
        val hot = (0 until (o.optJSONArray("hot")?.length() ?: 0)).map { i ->
            val t = o.getJSONArray("hot").getJSONObject(i)
            HotTopic(t.optLong("tid"), t.optString("title"), t.optString("meta"))
        }
        val users = (0 until (o.optJSONArray("users")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("users").getJSONObject(i)
            NewUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        val onItems = (0 until (o.optJSONArray("onItems")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("onItems").getJSONObject(i)
            OnlineUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        HomeSidebar(
            forums = forums, hotTopics = hot,
            statsText = o.optString("stats"), newUsers = users,
            onlineUsers = OnlineUsers(count = o.optString("onCount"), items = onItems),
        )
    }.getOrNull()

    // ---------------- 清除本地缓存 ----------------

    /**
     * 清除全部本地缓存：内存缓存 + 持久化的首页/阅读量/侧边栏缓存 +
     * 临时文件 + Coil 图片磁盘缓存。保留登录态、设置与登录信息。
     */
    fun clearLocalCache() {
        clearAllTopicPageCache()
        aiSummaryCache.clear()
        danmakuCache.clear()
        topicViews.clear()
        clearHomeCache()
        saveHomeSidebar(null)
        runCatching {
            val dir = getApplication<Application>().cacheDir
            // SharedPreferences 各文件
            getApplication<Application>().getSharedPreferences("lsb_home_cache", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
            getApplication<Application>().getSharedPreferences("lsb_views", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
            getApplication<Application>().getSharedPreferences("lsb_sidebar", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
            // 临时文件（头像上传、导出等）
            dir.listFiles()?.forEach { f ->
                if (f.name.startsWith("avatar_upload_") || f.name.endsWith(".png") ||
                    f.name.endsWith(".html") || f.name.endsWith(".md")) {
                    f.delete()
                }
            }
        }
        // Coil 图片磁盘缓存
        runCatching { coil.Coil.imageLoader(getApplication()).diskCache?.clear() }
        showToast("本地缓存已清除")
    }

    /** 数据管理页的按项清理；键同时用于自动清理设置。 */
    fun clearDataItems(items: Set<String>, notify: Boolean = true) {
        if ("home" in items) {
            clearHomeCache()
            homeTabs.forEach { tab -> tab.topics = emptyList(); tab.loaded = false }
            saveHomeSidebar(null)
        }
        if ("topic" in items) {
            clearAllTopicPageCache()
            danmakuCache.clear()
            topicViews.clear()
            getApplication<Application>().getSharedPreferences("lsb_views", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
        if ("images" in items) runCatching {
            coil.Coil.imageLoader(getApplication()).apply { diskCache?.clear(); memoryCache?.clear() }
            sb.linux.client.ui.clearBodyImageLayoutCache()
        }
        if ("reading_positions" in items) clearReadingPositions()
        if ("ai_history" in items) clearAiSummaryHistory()
        if ("ai_config" in items) settings.resetAi()
        if ("drafts" in items) getApplication<Application>().getSharedPreferences("lsb_topic_drafts", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        if ("link_previews" in items) sb.linux.client.data.LinkPreviewClient.clearCache()
        if ("background_image" in items) removeBackgroundImage()
        if ("history" in items) settings.clearHistory()
        if ("favorites" in items) settings.clearFavorites()
        if ("comment_favorites" in items) settings.clearCommentFavorites()
        if ("card_redemptions" in items) settings.clearCardRedemptions()
        if ("usage_stats" in items) settings.clearUsageStats()
        if (notify) showToast("已清理所选数据")
    }

    fun refreshSession() {
        sessionRecovery.refresh()
    }

    /** 把签到页解析结果应用到全局状态（侧边栏/我的页实时同步） */
    fun applyCheckinInfoPublic(info: HtmlParser.CheckinInfo) = applyCheckinInfo(info)

    /** 把签到页解析结果应用到全局状态（侧边栏/我的页实时同步） */
    private fun applyCheckinInfo(info: HtmlParser.CheckinInfo) {
        checkinCheckedToday = info.checkedToday || !info.canCheckin
        checkinText = buildString {
            if (info.streak > 0) append("连续 ${info.streak} 天")
            if (info.total > 0) {
                if (isNotEmpty()) append(" · ")
                append("累计 ${info.total} 天")
            }
        }
        // 持久化摘要，供今日后续启动直接使用本地缓存，避免重复查询源站
        settings.saveCheckinSummary(info.streak, info.total)
    }

    /** 解析签到页的连续/累计签到天数，供侧边栏展示（公开：签到完成后手动刷新缓存） */
    fun loadCheckinInfo(force: Boolean = false) {
        // 今日已签到且有本地缓存摘要时直接使用，不发源站请求
        if (!force) {
            settings.checkinSummaryToday()?.let { (streak, total) ->
                applyCheckinInfo(
                    HtmlParser.CheckinInfo(
                        streak = streak, total = total,
                        canCheckin = false, checkedToday = true,
                    )
                )
                return
            }
        }
        viewModelScope.launch {
            try {
                val resp = client.get("/daily_checkin")
                applyCheckinInfo(HtmlParser.parseCheckin(resp.html))
            } catch (_: Exception) {
            }
        }
    }

    /** 手动签到成功后调用：立即把状态置为已签到并刷新天数缓存 */
    fun onCheckinDone() {
        checkinCheckedToday = true
        settings.markCheckinDone()
        loadCheckinInfo()
    }

    fun onLoggedOut() {
        sessionRecovery.stop()
        clearAllTopicPageCache()
        client.invalidateCsrf()
        loginState = LoginState()
        blockedWords = emptySet()
        blockedUsers = emptySet()
        notifUnreadCount = 0
        checkinText = ""
        checkinCheckedToday = false
        // 清除签到缓存，避免切换账号后显示旧账号的签到信息
        settings.clearCheckinCache()
        viewModelScope.launch { client.logout() }
    }
}

enum class ThemeModePref { SYSTEM, LIGHT, DARK }
