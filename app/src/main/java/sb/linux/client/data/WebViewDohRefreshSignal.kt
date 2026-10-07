package sb.linux.client.data

/** Lets network policy changes refresh an initialized WebView bridge without a platform dependency. */
internal object WebViewDohRefreshSignal {
    @Volatile private var refresh: (() -> Unit)? = null

    fun install(callback: () -> Unit) {
        refresh = callback
    }

    fun notifyPolicyChanged() {
        refresh?.invoke()
    }
}
