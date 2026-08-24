package sb.linux.client

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import sb.linux.client.data.LsbClient

class LsbApp : Application(), ImageLoaderFactory {
    lateinit var client: LsbClient
        private set

    override fun onCreate() {
        super.onCreate()
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
            .okHttpClient(client.http)
            .crossfade(120)
            .build()
}
