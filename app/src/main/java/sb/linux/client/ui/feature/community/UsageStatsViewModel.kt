package sb.linux.client.ui.feature.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.ai.AiAssistantConfig
import sb.linux.client.domain.ai.AiAssistantRepository
import sb.linux.client.domain.ai.AiConfigurationRepository
import sb.linux.client.domain.community.FavoritesRepository
import sb.linux.client.domain.community.PointRow
import sb.linux.client.domain.community.SourceUsageTotals
import sb.linux.client.domain.community.UsageEvent
import sb.linux.client.domain.community.UsageStatisticsRepository
import sb.linux.client.domain.topic.TopicCard

data class UsageStatsUiState(
    val days: Int = 30,
    val cutoff: Long = cutoffFor(30),
    val events: List<UsageEvent> = emptyList(),
    val sourceTotals: SourceUsageTotals? = null,
    val sourcePoints: List<PointRow> = emptyList(),
    val favorites: List<TopicCard> = emptyList(),
    val commentFavoriteCount: Int = 0,
    val aiSummary: String = "",
    val aiConfigured: Boolean = false,
    val syncingPoints: Boolean = false,
    val aiBusy: Boolean = false,
    val aiError: String? = null,
    val feedback: String? = null,
) {
    val visibleEvents: List<UsageEvent> get() = events.filter { it.at >= cutoff }
    val visiblePoints: List<PointRow> get() = sourcePoints.filter { it.timestamp >= cutoff }
    val counts: Map<String, Int> get() = visibleEvents.groupingBy { it.type }.eachCount()
    val pointsSpent: Int get() = visibleEvents
        .filter { it.type == "coin" || it.type == "donate" || it.type == "gacha" }.sumOf { it.value }
    val gachaSpent: Int get() = visibleEvents.filter { it.type == "gacha" }.sumOf { it.value }
    val sourceIncome: Long get() = visiblePoints.filter { it.positive }.sumOf { it.change.toLongOrNull() ?: 0 }
    val sourceExpense: Long get() = visiblePoints.filterNot { it.positive }.sumOf { it.change.toLongOrNull() ?: 0 }

    companion object {
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private fun cutoffFor(days: Int) = if (days == 0) 0L else System.currentTimeMillis() - days * DAY_MILLIS
    }
}

class UsageStatsViewModel(
    private val statistics: UsageStatisticsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val aiConfiguration: AiConfigurationRepository,
    private val assistant: AiAssistantRepository,
) : ViewModel() {
    var state by mutableStateOf(UsageStatsUiState())
        private set

    fun refresh(userId: Long) {
        val config = aiConfiguration.current()
        val totals = statistics.sourceTotals(userId)
        state = state.copy(
            events = statistics.events(),
            sourceTotals = totals,
            sourcePoints = if (totals != null) statistics.points() else emptyList(),
            favorites = favoritesRepository.topics(),
            commentFavoriteCount = favoritesRepository.comments().size,
            aiSummary = statistics.aiSummary(),
            aiConfigured = config.apiUrl.isNotBlank() && config.model.isNotBlank(),
        )
    }

    fun setDays(days: Int) { state = state.copy(days = days, cutoff = cutoffFor(days)) }
    fun consumeFeedback() { state = state.copy(feedback = null) }

    fun sync(userId: Long) {
        if (state.syncingPoints) return
        state = state.copy(syncingPoints = true, feedback = null)
        viewModelScope.launch {
            try {
                statistics.sync()
                refresh(userId)
                state = state.copy(feedback = "源站帖子、回帖、收藏、淘帖及积分已同步")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state = state.copy(feedback = error.message ?: "同步失败")
            } finally {
                state = state.copy(syncingPoints = false)
            }
        }
    }

    fun clear(userId: Long) {
        statistics.clear()
        refresh(userId)
    }

    fun summarize() {
        if (state.aiBusy || !state.aiConfigured) return
        val snapshot = state
        val config = aiConfiguration.current()
        state = state.copy(aiBusy = true, aiError = null)
        viewModelScope.launch {
            try {
                val result = assistant.summarize(
                    AiAssistantConfig(
                        config.apiUrl, config.apiKey, config.model, config.prompt, config.temperature.toDouble(),
                    ),
                    "我的 Linux.do/SB 使用统计",
                    summaryContent(snapshot),
                ) {}
                statistics.saveAiSummary(result)
                state = state.copy(aiSummary = result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state = state.copy(aiError = error.message ?: "AI 总结失败")
            } finally {
                state = state.copy(aiBusy = false)
            }
        }
    }

    private fun summaryContent(data: UsageStatsUiState): String {
        val topViewed = data.visibleEvents.filter { it.type == "view" && it.title.isNotBlank() }
            .groupingBy { it.title }.eachCount().entries.sortedByDescending { it.value }.take(8)
        return buildString {
            appendLine("统计范围：${if (data.days == 0) "全部" else "最近 ${data.days} 天"}")
            appendLine("浏览 ${data.counts["view"] ?: 0} 次，收藏 ${data.counts["favorite"] ?: 0} 次，发帖 ${data.counts["topic"] ?: 0} 次，回帖 ${data.counts["reply"] ?: 0} 次，本机记录积分支出 ${data.pointsSpent}，其中抽卡消耗 ${data.gachaSpent}。")
            appendLine("源站当前总数：发帖 ${data.sourceTotals?.topics}，回帖 ${data.sourceTotals?.replies}，收藏 ${data.sourceTotals?.favorites}，淘帖 ${data.sourceTotals?.collections}。")
            appendLine("本地帖子收藏 ${data.favorites.size} 帖，评论收藏 ${data.commentFavoriteCount} 条。")
            appendLine("已同步源站积分流水 ${data.visiblePoints.size} 条，收入 ${data.sourceIncome}，支出 ${data.sourceExpense}。源站流水和本地操作统计可能重叠，不得相加；仅分析已同步范围。")
            appendLine(data.visiblePoints.take(40).joinToString("\n") { "${it.timeText} ${it.reason} ${if (it.positive) "+" else "-"}${it.change}" })
            if (topViewed.isNotEmpty()) appendLine("常看主题：${topViewed.joinToString { "${it.key}（${it.value}次）" }}")
            if (data.favorites.isNotEmpty()) appendLine("近期收藏：${data.favorites.take(10).joinToString { it.title }}")
            append("请分析使用偏好、近期关注主题和参与习惯，并给出简短总结。")
        }
    }

    private fun cutoffFor(days: Int) =
        if (days == 0) 0L else System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
}

class UsageStatsViewModelFactory(
    private val statistics: UsageStatisticsRepository,
    private val favorites: FavoritesRepository,
    private val aiConfiguration: AiConfigurationRepository,
    private val assistant: AiAssistantRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UsageStatsViewModel(statistics, favorites, aiConfiguration, assistant) as T
}
