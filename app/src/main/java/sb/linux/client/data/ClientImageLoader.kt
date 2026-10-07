package sb.linux.client.data

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import sb.linux.client.common.error.LsbException

internal class ClientImageLoader(
    private val context: Context,
    private val http: OkHttpClient,
    private val solveChallenge: suspend (String, String) -> Boolean,
    private val isValidImage: (ByteArray) -> Boolean,
    private val hasChallenge: (String) -> Boolean,
    private val userAgent: String,
) {
    private val avatarMemoryCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    private val avatarInFlight = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()
    private val avatarCacheDir: File by lazy { File(context.cacheDir, "lsb_avatar_cache").apply { mkdirs() } }
    /**
     * 拉取图片原始字节（源站头像等静态资源被 /FL/CC/VALIDATOR 重定向验证盾保护，
     * OkHttp 自动跟随 302 验证链）。
     * 三重防护：并发去重（同 URL 只发一次请求）、魔数校验（防验证页 HTML 混入缓存）、
     * 磁盘缓存读取时再校验（历史坏缓存自动删除自愈，不再永久显示失败）。
     */
    /** 同步窥探头像内存缓存（无网络/磁盘 IO）：列表滚动时缓存命中的头像首帧直接出图，
     *  免去 IO 协程调度与占位符→图片的跳变（快滑掉帧与头像闪现的主因） */
    fun peekAvatarBytes(url: String): ByteArray? = avatarMemoryCache[url]

    suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        avatarMemoryCache[url]?.let { return@withContext it }
        // v2 前缀：读取时校验魔数；旧版（未校验内容即写入）残留的验证页 HTML 等坏文件在此自愈删除
        val cacheFile = File(avatarCacheDir, "v2_${url.hashCode().toString(16)}.dat")
        if (cacheFile.exists()) {
            val cached = runCatching { cacheFile.readBytes() }.getOrDefault(ByteArray(0))
            if (isValidImage(cached)) {
                avatarMemoryCache[url] = cached
                return@withContext cached
            }
            runCatching { cacheFile.delete() } // 坏缓存：删除后重新拉取
        }
        // 并发去重：首个请求完成前，同 URL 的后续请求共享其结果
        val mine = CompletableDeferred<ByteArray>()
        val existing = avatarInFlight.putIfAbsent(url, mine)
        if (existing != null) return@withContext existing.await()
        try {
            val bytes = fetchImageBytes(url)
            avatarMemoryCache[url] = bytes
            runCatching { cacheFile.writeBytes(bytes) }
            mine.complete(bytes)
            bytes
        } catch (e: Exception) {
            // 首个请求被取消不能连累等待者：等待者收到普通异常后仍可走 URL 兜底
            mine.completeExceptionally(
                if (e is kotlinx.coroutines.CancellationException) LsbException("图片请求中断") else e
            )
            throw e
        } finally {
            avatarInFlight.remove(url, mine)
        }
    }

    /** 图片网络拉取：魔数校验为准（不信任 Content-Type）；?v= 参数 504 时去参重试；验证页触发 solveUam 后重试 */
    private suspend fun fetchImageBytes(url: String): ByteArray {
        // 去参 URL 优先：?v= 时间戳只是缓存穿透参数，去掉不影响内容，还规避网关对带参 URL 的 504
        val candidates = if (url.contains('?')) listOf(url.substringBefore('?'), url) else listOf(url)

        var result: ByteArray? = null
        for (candidate in candidates) {
            val req = Request.Builder().url(candidate)
                .header("User-Agent", userAgent)
                .header("Accept", "image/avif,image/webp,image/svg+xml,image/*,*/*;q=0.8")
                .build()
            run attempt@{
                repeat(3) {
                    http.newCall(req).execute().use { r ->
                        val b = r.body?.bytes() ?: ByteArray(0)
                        if (isValidImage(b)) {
                            result = b
                            return@attempt
                        }
                        // 内容不是图片：仅真正命中验证特征时才触发验证恢复
                        // （504 空体等网关错误不该弹验证页，直接重试/换候选）
                        val html = String(b, Charsets.UTF_8)
                        if (hasChallenge(html)) {
                            val resolved = runCatching {
                                solveChallenge(r.request.url.toString(), html)
                            }.getOrDefault(false)
                            // 用户取消：别再为这张图重复弹验证，交给外层兜底逻辑
                            if (!resolved) return@attempt
                        }
                    }
                }
            }
            if (result != null) break
        }
        return result ?: throw LsbException("图片加载失败")
    }

}
