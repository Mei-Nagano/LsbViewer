package sb.linux.client.domain.network

import kotlinx.coroutines.flow.Flow

interface NetworkRecoveryRepository {
    val recoveryEvents: Flow<Unit>
}
