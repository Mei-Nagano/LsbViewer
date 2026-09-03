package sb.linux.client

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import sb.linux.client.data.LsbClient
import sb.linux.client.data.AppNetwork

class LsbApp : Application(), ImageLoaderFactory {
    lateinit var client: LsbClient
        private set

    override fun onCreate() {
        super.onCreate()
        AppNetwork.init(this)
        client = LsbClient(this)
    }

    /** 全局图片加载器：强磁盘/内存缓存加速头像加载（SVG 头像由 resvg 直接渲染，不经 Coil） */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .okHttpClient(client.http.newBuilder()
                .apply {
                    interceptors().removeAll { it is sb.linux.client.data.CronetFallbackInterceptor }
                    interceptors().add(0, okhttp3.Interceptor { chain ->
                        chain.proceed(chain.request().newBuilder()
                            .tag(sb.linux.client.data.ImageTraffic::class.java, sb.linux.client.data.ImageTraffic).build())
                    })
                }
                // 复用登录态和连接池，但不把网页强制验证缓存的请求头施加到静态图片上。
                // 仍由 Coil 尊重源站 Cache-Control / no-store，不强行缓存私有图片。
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                        .build())
                }.addInterceptor(sb.linux.client.data.CronetFallbackInterceptor(client.http.cookieJar)).build())
            .crossfade(120)
            .build()
}
