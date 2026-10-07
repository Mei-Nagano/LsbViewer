package sb.linux.client.domain.account

data class SessionSnapshot(val login: LoginState, val unread: Int = 0)
