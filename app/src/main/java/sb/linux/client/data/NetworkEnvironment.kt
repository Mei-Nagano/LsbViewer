package sb.linux.client.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Small platform queries shared by network policy components. */
internal object NetworkEnvironment {
    fun isVpnActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }
}
