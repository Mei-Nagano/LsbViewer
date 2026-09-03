package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 解析结果缓存。DoH 每次查询都要重新握手，页面一次加载会问几十个域名；
 * 没有缓存时既慢又容易在某次抖动后掉回系统 DNS。fresh 期内直接命中，
 * 失败时还能用 stale 结果顶住，避免被污染的系统解析重新接管。
 */
internal object DnsCache {
    private class Entry(val addresses: List<InetAddress>, val freshUntil: Long, val staleUntil: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun get(key: String, allowStale: Boolean = false): List<InetAddress>? {
        val entry = entries[key] ?: return null
        val now = System.nanoTime()
        if (now < entry.freshUntil) return entry.addresses
        if (allowStale && now < entry.staleUntil) return entry.addresses
        if (now >= entry.staleUntil) entries.remove(key, entry)
        return null
    }

    fun put(key: String, addresses: List<InetAddress>, freshMs: Long, staleMs: Long) {
        if (addresses.isEmpty()) return
        if (entries.size > 512) entries.clear()
        val now = System.nanoTime()
        entries[key] = Entry(addresses,
            now + TimeUnit.MILLISECONDS.toNanos(freshMs), now + TimeUnit.MILLISECONDS.toNanos(staleMs))
    }

    fun clear() = entries.clear()
}

/**
 * 系统启动解析也有等待上限，不能让不可中断的 getaddrinfo 拖住整组测速。
 * 结果按主机名缓存：一组服务器同时测速时只解析一次，之后的查询直接命中。
 */
internal class BoundedBootstrapDns(
    private val delegate: Dns = Dns.SYSTEM,
    private val timeoutMs: Long = 1200,
    private val freshMs: Long = 10 * 60 * 1000L,
    private val staleMs: Long = 60 * 60 * 1000L,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        DnsCache.get("bootstrap:$hostname")?.let { return it }
        val task = FutureTask { delegate.lookup(hostname).sortedBy { it !is Inet4Address } }
        try {
            workers.execute(task)
            return task.get(timeoutMs, TimeUnit.MILLISECONDS)
                .also { DnsCache.put("bootstrap:$hostname", it, freshMs, staleMs) }
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            // getaddrinfo 在 Android 上不可中断：超时后线程仍会占着，此时用过期结果也好过整组失败。
            DnsCache.get("bootstrap:$hostname", allowStale = true)?.let { return it }
            throw UnknownHostException("DNS bootstrap unavailable").apply { initCause(e) }
        } finally { task.cancel(true) }
    }

    companion object {
        // SynchronousQueue + 足够的上限：某个 getaddrinfo 卡住时，新任务直接开新线程，
        // 不会像有界队列那样排在后面，把「同时测速」退化成一台一台来。
        private val workers = ThreadPoolExecutor(0, 32, 30, TimeUnit.SECONDS, SynchronousQueue(),
            { runnable -> Thread(runnable, "doh-bootstrap").apply { isDaemon = true } })
    }
}

/** 运行期解析要容忍跨境线路的抖动；测速要快出结果，两套超时不能共用。 */
internal data class DohProfile(
    val connectMs: Long,
    val readMs: Long,
    val callMs: Long,
    val includeIPv6: Boolean,
) {
    companion object {
        /** 应用与网页实际解析用：宁可多等，也不要退回被污染的系统 DNS。 */
        val RUNTIME = DohProfile(connectMs = 6000, readMs = 6000, callMs = 10_000, includeIPv6 = true)
        /**
         * 测速用：只问一条 A 记录。预算必须放得下「启动解析 + TCP + TLS + 查询」四段——
         * callTimeout 覆盖整个调用，跨境服务器冷启动轻松超过 2 秒，卡太紧会把能用的服务器判成失败。
         * 整组是并发跑的，单台放宽不会拖长整体耗时。
         */
        val BENCHMARK = DohProfile(connectMs = 3000, readMs = 3000, callMs = 4500, includeIPv6 = false)
    }
}

/**
 * 连接池 evictAll / executor 关闭都会写 socket（TLS close_notify）；设置监听器和网络回调
 * 都在主线程触发清理，直接调用会被 StrictMode 以 NetworkOnMainThreadException 杀进程。
 * 统一串行排队到这条守护线程执行。
 */
internal val networkMaintenanceExecutor = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
    LinkedBlockingQueue()) { runnable -> Thread(runnable, "doh-maintenance").apply { isDaemon = true } }

internal class DohTransport(
    url: String,
    bootstrapDns: Dns = BoundedBootstrapDns(),
    builder: OkHttpClient.Builder = OkHttpClient.Builder(),
    profile: DohProfile = DohProfile.RUNTIME,
) {
    private val client = builder
        .dispatcher(Dispatcher())
        .connectionPool(ConnectionPool())
        .connectTimeout(profile.connectMs, TimeUnit.MILLISECONDS)
        .readTimeout(profile.readMs, TimeUnit.MILLISECONDS)
        .callTimeout(profile.callMs, TimeUnit.MILLISECONDS)
        .build()
    // DnsOverHttps.Builder 会重建 client.dns，必须通过 systemDns 显式传入启动解析策略。
    val dns: Dns = DnsOverHttps.Builder().client(client).url(url.toHttpUrl())
        .systemDns(bootstrapDns).post(true).includeIPv6(profile.includeIPv6).build()

    fun close() {
        // cancelAll 只改取消标记不做 I/O，可以同步执行以尽快中断查询；
        // 关连接和关线程池会写 socket，必须挪出主线程。
        client.dispatcher.cancelAll()
        networkMaintenanceExecutor.execute {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdownNow()
        }
    }
}

/** 单次快速查询；超时/取消真正取消 HTTP 调用，不留下后台的三轮重试。 */
internal suspend fun measureDoh(
    dns: Dns,
    cancel: () -> Unit,
    host: String = "linux.sb",
    timeoutMs: Long = 5000,
): DohBenchmark {
    val start = System.nanoTime()
    return try {
        withTimeoutOrNull(timeoutMs) {
            try {
                val addresses = runInterruptible(Dispatchers.IO) { dns.lookup(host) }
                if (addresses.isEmpty()) DohBenchmark(emptyList(), emptyList(), 1)
                else DohBenchmark(listOf(((System.nanoTime() - start) / 1_000_000).coerceAtLeast(1)),
                    addresses.mapNotNull { it.hostAddress }, 1)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { DohBenchmark(emptyList(), emptyList(), 1) }
        } ?: DohBenchmark(emptyList(), emptyList(), 1)
    } finally { cancel() }
}
