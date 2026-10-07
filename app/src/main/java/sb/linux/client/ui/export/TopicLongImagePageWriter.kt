package sb.linux.client.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File

/** Splits and writes an already measured long-image view. */
internal object TopicLongImagePageWriter {
    fun write(
        root: View,
        widthPx: Int,
        height: Int,
        title: String,
        context: Context,
    ): List<File> {
        val maxHeight = TopicLongImageRenderer.MAX_IMAGE_HEIGHT
        val slices = TopicLongImageRenderer.imagePageSlices(height, maxHeight)
        val stamp = System.currentTimeMillis()
        return slices.mapIndexed { index, slice ->
            val bitmap = Bitmap.createBitmap(widthPx, slice.last - slice.first + 1, Bitmap.Config.ARGB_8888)
            try {
                Canvas(bitmap).apply {
                    translate(0f, -slice.first.toFloat())
                    root.draw(this)
                }
                val suffix = if (slices.size > 1) "_第${index + 1}页" else ""
                File(
                    TopicExportFiles.exportDir(context),
                    "${TopicExportFiles.safeExportName(title)}_${stamp}${suffix}.png",
                ).also { file ->
                    file.outputStream().use {
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 95, it)) { "图片写入失败" }
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
