package sb.linux.client.common.error

/**
 * 源站访问或客户端业务流程失败时使用的统一异常。
 *
 * 异常类型位于公共层，避免网络实现成为其他业务域的依赖入口。
 */
class LsbException(message: String) : Exception(message)
