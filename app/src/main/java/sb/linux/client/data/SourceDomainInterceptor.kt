package sb.linux.client.data

import okhttp3.Interceptor
import okhttp3.Response
import sb.linux.client.common.network.Endpoints

/** 在首次发送前统一实际地址；服务端后续重定向按 HTTP 规则执行。 */
internal class SourceDomainInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val destination = Endpoints.abs(request.url.toString())
        return chain.proceed(if (destination == request.url.toString()) request
            else request.newBuilder().url(destination).build())
    }
}
