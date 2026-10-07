package sb.linux.client.data

import kotlinx.coroutines.flow.Flow
import sb.linux.client.domain.network.NetworkRecoveryRepository

internal object AppNetworkRecoveryRepository : NetworkRecoveryRepository {
    override val recoveryEvents: Flow<Unit> get() = AppNetwork.recoveryEvents
}
