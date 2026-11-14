package sb.linux.client.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.roundToInt

internal data class PreparedSourceAttachment(val file: File, val mime: String)

/** 与源站 2560px、长图保留、较小结果优先的图片规则一致；原图模式直接上传。 */
internal fun prepareSourceAttachment(file: File, mime: String, maxBytes: Long, original: Boolean): PreparedSourceAttachment {
    if (original || mime !in setOf("image/jpeg", "image/png", "image/webp")) return PreparedSourceAttachment(file, mime)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
        maxOf(bounds.outWidth, bounds.outHeight).toFloat() / minOf(bounds.outWidth, bounds.outHeight) >= 3f) return PreparedSourceAttachment(file, mime)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 5120) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return PreparedSourceAttachment(file, mime)
    var scaled: Bitmap? = null
    try {
        val ratio = (2560f / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        scaled = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1), true) else bitmap
        val output = File.createTempFile("source-prepared-", ".webp", file.parentFile)
        try {
            @Suppress("DEPRECATION")
            val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            for (quality in listOf(90, 86, 80, 72, 64)) {
                output.outputStream().use { check(scaled.compress(format, quality, it)) { "图片转换失败" } }
                if (output.length() <= maxBytes) break
            }
            if (output.length() < file.length()) return PreparedSourceAttachment(output, "image/webp")
            output.delete()
            return PreparedSourceAttachment(file, mime)
        } catch (error: Exception) { output.delete(); throw error }
    } finally {
        if (scaled !== bitmap) scaled?.recycle()
        bitmap.recycle()
    }
}
