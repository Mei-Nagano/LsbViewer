package sb.linux.client.data

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 仅监听本机随机端口的 HTTPS CONNECT 通路。只解析域名并转发加密字节，
 * 不解密 TLS、不改 URL/Host/证书，不接触 Cookie/POST 内容，WebView 自行验证证书。
 * 应用原有 cleartext 禁止策略不变。
 */
internal class LocalDnsTunnel(private val dns: Dns) : Closeable {
    private val listener = ServerSocket(0, 64, InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
    val port: Int get() = listener.localPort
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    // 一个页面会并行开很多连接；上限太低会让浏览器直接收到拒绝，表现为页面加载不全。
    private val slots = Semaphore(64)
    // 每条连接要占 2 个线程（下行在本线程、上行另起一个），上限需留出余量避免被拒。
    private val workers = ThreadPoolExecutor(0, 160, 30, TimeUnit.SECONDS, SynchronousQueue(),
        { work -> Thread(work, "webview-dns-tunnel").apply { isDaemon = true } })

    init {
        Thread({
            while (!listener.isClosed) {
                val socket = try { listener.accept() } catch (_: Exception) { break }
                // 整个循环体都要兜住异常：accept 线程上抛的任何东西都会被系统当成未捕获异常，直接杀进程。
                try {
                    // 满载时先等一会儿，再放弃；直接关掉会让页面上的资源零星失败。
                    if (!slots.tryAcquire(5, TimeUnit.SECONDS)) { runCatching { socket.close() }; continue }
                } catch (_: InterruptedException) { runCatching { socket.close() }; break }
                sockets.add(socket)
                try { workers.execute { try { forward(socket) } finally { sockets.remove(socket); slots.release() } } }
                catch (_: Throwable) { sockets.remove(socket); runCatching { socket.close() }; slots.release() }
            }
        }, "webview-dns-accept").apply { isDaemon = true; start() }
    }

    private fun forward(local: Socket) {
        local.use {
            var connected = false
            try {
                local.soTimeout = 15_000
                val input = BufferedInputStream(local.getInputStream())
                val authority = readConnectAuthority(input)
                val upstream = connect(authority.first, authority.second)
                sockets.add(upstream)
                try {
                    upstream.use { remote ->
                        local.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
                        local.getOutputStream().flush()
                        connected = true
                        local.soTimeout = 60_000
                        remote.soTimeout = 60_000
                        local.tcpNoDelay = true; remote.tcpNoDelay = true
                        val upload = workers.submit {
                            try { input.copyTo(remote.getOutputStream()); remote.shutdownOutput() }
                            catch (_: Exception) { runCatching { remote.close() }; runCatching { local.close() } }
                        }
                        try { remote.getInputStream().copyTo(local.getOutputStream()) }
                        finally { runCatching { local.close() }; runCatching { remote.close() }; upload.cancel(true) }
                    }
                } finally { sockets.remove(upstream) }
            } catch (_: Exception) {
                if (!connected) runCatching {
                    local.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                }
            }
        }
    }

    private fun connect(host: String, port: Int): Socket {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        for (address in dns.lookup(host)) {
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()).toInt()
            if (remaining <= 0) break
            val socket = Socket(Proxy.NO_PROXY)
            try {
                socket.connect(InetSocketAddress(address, port), remaining.coerceAtMost(3000))
                return socket
            } catch (_: Exception) { socket.close() }
        }
        throw java.io.IOException("Connection unavailable")
    }

    override fun close() {
        listener.close()
        // ConcurrentHashMap 在 size==1 的瞬间可能被工作线程移除；toList 的单元素捷径会抛异常。
        sockets.toTypedArray().forEach { runCatching { it.close() } }
        workers.shutdownNow()
    }
}

/** 严格限定 CONNECT 请求，限制头部大小，且不提前消费后续 TLS 字节。 */
internal fun readConnectAuthority(input: InputStream): Pair<String, Int> {
    val header = ByteArrayOutputStream()
    var tail = 0
    while (header.size() < 32 * 1024) {
        val byte = input.read()
        require(byte >= 0)
        header.write(byte)
        tail = (tail shl 8) or byte
        if (tail == 0x0d0a0d0a) break
    }
    require(tail == 0x0d0a0d0a)
    val line = header.toString("US-ASCII").substringBefore("\r\n").split(' ')
    require(line.size == 3 && line[0] == "CONNECT" && line[2] in setOf("HTTP/1.0", "HTTP/1.1"))
    val target = ("https://" + line[1]).toHttpUrlOrNull()
    require(target != null && target.username.isEmpty() && target.password.isEmpty() &&
        target.encodedPath == "/" && target.query == null && target.fragment == null &&
        !line[1].contains('/') && !line[1].contains('\\'))
    return target.host to target.port
}
