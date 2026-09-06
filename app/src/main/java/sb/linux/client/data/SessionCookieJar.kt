package sb.linux.client.data

import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 会话 Cookie 的内存权威存储与持久化适配器。
 *
 * 会话 Cookie 在进程生命周期内保留，持久化 Cookie 仅在有效期内恢复；退出登录时同时清理两层。
 */
internal class SessionCookieJar(private val prefs: SharedPreferences) : CookieJar {
    /** 内存为权威存储，SharedPreferences 只承担跨进程恢复。 */
    private val memory: MutableMap<String, Cookie> by lazy { loadPersisted() }

    private fun loadPersisted(): MutableMap<String, Cookie> {
        val map = mutableMapOf<String, Cookie>()
        val set = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        for (serialized in set) {
            val parts = serialized.split("\u0001")
            if (parts.size != 5) continue
            val expiresAt = parts[4].toLongOrNull() ?: continue
            if (expiresAt <= System.currentTimeMillis()) continue
            map[parts[0]] = Cookie.Builder()
                .name(parts[0]).value(parts[1])
                .domain(parts[2]).path(parts[3])
                .expiresAt(expiresAt)
                .build()
        }
        return map
    }

    internal fun store(): MutableMap<String, Cookie> = synchronized(this) { LinkedHashMap(memory) }

    internal fun value(name: String): String? = synchronized(this) { memory[name]?.value }

    internal fun saveRawCookies(url: String, cookieHeader: String) = synchronized(this) {
        val httpUrl = url.toHttpUrl()
        for (pair in cookieHeader.split(";")) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val name = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (name.isBlank()) continue
            memory[name] = Cookie.Builder()
                .name(name).value(value)
                .domain(httpUrl.host).path("/")
                .expiresAt(System.currentTimeMillis() + 6 * 60 * 60 * 1000)
                .build()
        }
        persist()
    }

    private fun persist() {
        prefs.edit().putStringSet(
            "cookies",
            memory.values.filter { it.persistent }.map {
                "${it.name}\u0001${it.value}\u0001${it.domain}\u0001${it.path}\u0001${it.expiresAt}"
            }.toSet(),
        ).apply()
    }

    internal fun clear() = synchronized(this) {
        memory.clear()
        prefs.edit().putStringSet("cookies", emptySet()).apply()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = synchronized(this) {
        for (cookie in cookies) memory[cookie.name] = cookie
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(this) {
        val now = System.currentTimeMillis()
        memory.values.removeAll { it.persistent && it.expiresAt <= now }
        memory.values.filter { it.matches(url) }
    }
}
