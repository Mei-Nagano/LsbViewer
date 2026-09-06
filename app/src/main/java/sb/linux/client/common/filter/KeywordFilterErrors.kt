package sb.linux.client.common.filter

/** 屏蔽词同步的可判定错误，供 UI 显示及服务层决定是否保留 pending。 */
sealed class KeywordFilterRemoteException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Authentication(cause: Throwable? = null) : KeywordFilterRemoteException("请先登录后使用源站屏蔽词", cause)
    class Network(cause: Throwable) : KeywordFilterRemoteException("屏蔽词同步失败，请检查网络", cause)
    class Rejected(message: String) : KeywordFilterRemoteException(message)
    class InvalidResponse(cause: Throwable? = null) : KeywordFilterRemoteException("源站返回格式异常", cause)
}
