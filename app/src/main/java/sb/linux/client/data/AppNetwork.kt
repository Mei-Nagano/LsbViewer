package sb.linux.client.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import android.content.SharedPreferences
import android.net.Network
import java.net.Inet4Address

/** 全应用共享 DNS 策略。设置在每次解析时读取，因此切换 DoH 无需重启或重建会话。 */
object AppNetwork {
    private const val ANSWER_FRESH_MS = 5 * 60 * 1000L
    private const val ANSWER_STALE_MS = 30 * 60 * 1000L

    @Volatile private var appContext: Context? = null
    private val recoverySignal = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    internal val recoveryEvents: kotlinx.coroutines.flow.SharedFlow<Unit> = recoverySignal

    private val generation = java.util.concurrent.atomic.AtomicLong()
    internal fun policyKey(): String = "${generation.get()}|${if (isDohActive()) appContext?.let { AppSettings(it).dohUrl } else "system"}"
    private val dohCache = ConcurrentHashMap<String, DohTransport>()
    private val activeTests = ConcurrentHashMap.newKeySet<DohTransport>()
    val connectionPool = okhttp3.ConnectionPool(5, 10, java.util.concurrent.TimeUnit.MINUTES)
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in setOf("doh_enabled", "doh_url", "doh_disable_on_vpn")) invalidate()
    }

    @Synchronized fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        context.getSharedPreferences("lsb_app_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(settingsListener)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        runCatching { cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            private var validated: Network? = null
            override fun onAvailable(network: Network) = invalidate()
            override fun onLost(network: Network) {
                if (validated == network) validated = null
                invalidate()
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                cancelTestsOnVpn()
                WebViewDoh.refreshIfInitialized()
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    if (validated != network) { validated = network; recoverySignal.tryEmit(Unit) }
                } else if (validated == network) validated = null
            }
        }) }
    }

    fun isDohActive(): Boolean {
        val context = appContext ?: return false
        val settings = AppSettings(context)
        return settings.dohEnabled && !(settings.dohDisableOnVpn && isVpnActive(context))
    }

    private fun invalidate() {
        generation.incrementAndGet()
        CronetTransport.invalidate()
        // 设置监听器在主线程回调；evictAll/close 会写 socket（TLS close_notify），
        // 必须挪到维护线程，否则 NetworkOnMainThreadException 直接闪退。
        cancelTestsOnVpn()
        networkMaintenanceExecutor.execute {
            connectionPool.evictAll()
            DnsCache.clear()
            val old = dohCache.values.toTypedArray()
            dohCache.clear()
            old.forEach { it.close() }
        }
        WebViewDoh.refreshIfInitialized()
        recoverySignal.tryEmit(Unit)
    }

    private fun cancelTestsOnVpn() {
        val ctx = appContext ?: return
        if (AppSettings(ctx).dohDisableOnVpn && isVpnActive(ctx)) activeTests.toTypedArray().forEach { it.close() }
    }

    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val context = appContext ?: throw UnknownHostException("网络组件尚未初始化")
            val settings = AppSettings(context)
            if (!settings.dohEnabled || (settings.dohDisableOnVpn && isVpnActive(context))) {
                return Dns.SYSTEM.lookup(hostname).sortedBy { it !is Inet4Address }
            }
            val endpoint = settings.dohUrl.trim().toHttpUrlOrNull()
            if (endpoint == null || endpoint.scheme != "https") return Dns.SYSTEM.lookup(hostname)
            val key = "${generation.get()}|${endpoint}|$hostname"
            DnsCache.get(key)?.let { return it }
            return try {
                resolver(endpoint.toString()).lookup(hostname).sortedBy { it !is Inet4Address }
                    .also { DnsCache.put(key, it, ANSWER_FRESH_MS, ANSWER_STALE_MS) }
            } catch (_: Exception) {
                // 抖动一次就掉回系统 DNS，等于让污染结果覆盖已经解析成功的域名：先用过期结果顶住。
                DnsCache.get(key, allowStale = true)
                    ?: Dns.SYSTEM.lookup(hostname).sortedBy { it !is Inet4Address }
            }
        }
    }

    /**
     * 所有共享客户端的统一入口：DoH 解析 + 共享连接池 + Cronet 回退拦截器。
     * 网络拦截器记录已进入发送阶段，避免在提交结果不明时自动重复 POST。
     * 业务客户端可将回退应用拦截器移至自定义请求头之后，仍能捕获连接阶段异常。
     */
    fun clientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder().dns(dns).connectionPool(connectionPool)
        .addInterceptor(CronetFallbackInterceptor())
        .addNetworkInterceptor { chain ->
            chain.request().tag(CronetAttempt::class.java)?.mayHaveSent = true
            chain.proceed(chain.request())
        }

    internal fun appContext(): Context? = appContext

    fun isValidEndpoint(url: String): Boolean = url.trim().toHttpUrlOrNull()?.let {
        it.scheme == "https" && it.username.isEmpty() && it.password.isEmpty() && it.fragment == null
    } == true

    private fun resolver(url: String): Dns = dohCache.computeIfAbsent(url) { DohTransport(it) }.dns

    fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    /** 测试必须直连所选解析器，不能把系统 DNS 回退误报成 DoH 成功。 */
    fun lookupForTest(host: String = "linux.sb"): List<InetAddress> {
        val context = appContext ?: throw UnknownHostException("网络组件尚未初始化")
        val settings = AppSettings(context)
        check(settings.dohEnabled) { "DoH 尚未启用" }
        check(!settings.dohDisableOnVpn || !isVpnActive(context)) { "VPN 已开启，当前按设置停用 DoH；未发送测试请求" }
        val url = settings.dohUrl.trim()
        require(isValidEndpoint(url)) { "请输入有效的 HTTPS DoH 地址" }
        return resolver(url).lookup(host)
    }

    /** 只测试指定端点：单次、短超时，不切换设置、不回退系统 DNS。 */
    suspend fun benchmark(url: String, host: String = "linux.sb"): DohBenchmark {
        require(isValidEndpoint(url)) { "请输入有效的 HTTPS DoH 地址" }
        val context = appContext ?: throw UnknownHostException("网络组件尚未初始化")
        check(!AppSettings(context).dohDisableOnVpn || !isVpnActive(context)) { "VPN 已开启" }
        val direct = DohTransport(url, profile = DohProfile.BENCHMARK)
        activeTests.add(direct)
        return try { measureDoh(direct.dns, direct::close, host) }
        finally { activeTests.remove(direct) }
    }
}
