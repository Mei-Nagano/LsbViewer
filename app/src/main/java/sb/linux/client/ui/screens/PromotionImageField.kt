package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.SettingsSlider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.domain.account.AccountForm
import sb.linux.client.domain.account.AccountManagementRepository
import sb.linux.client.ui.theme.LocalAppSpacing
import java.io.File

/** 对齐源站 460 × 120 JPEG 裁切及 image_file 上传协议。 */
@Composable
internal fun PromotionImageField(
    repository: AccountManagementRepository, form: AccountForm, enabled: Boolean, imageUrl: String,
    onBusy: (Boolean) -> Unit, onUploaded: (String) -> Unit, onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var uploading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try {
                bitmap = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("图片无法读取")
                    check(bytes.size <= 10 * 1024 * 1024) { "原图不能超过 10MB" }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val options = BitmapFactory.Options().apply {
                        while (bounds.outWidth / inSampleSize > 4096 || bounds.outHeight / inSampleSize > 4096) inSampleSize *= 2
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("不支持的图片格式")
                }
                zoom = 1f; offsetX = 0f; offsetY = 0f
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "图片读取失败") }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.item)) {
        if (imageUrl.isNotBlank()) AsyncImage(imageUrl, "广告图片预览", Modifier.fillMaxWidth().aspectRatio(460f / 120f))
        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = enabled) { Text("选择广告图片") }
    }
    bitmap?.let { source ->
        val minScale = maxOf(460f / source.width, 120f / source.height)
        val scale = minScale * zoom
        fun cropX(): Float = ((460 - source.width * scale) / 2 + offsetX).coerceIn(460 - source.width * scale, 0f)
        fun cropY(): Float = ((120 - source.height * scale) / 2 + offsetY).coerceIn(120 - source.height * scale, 0f)
        fun draw(canvas: AndroidCanvas) {
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.save()
            canvas.translate(cropX(), cropY())
            canvas.scale(scale, scale)
            canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        }
        Dialog(onDismissRequest = { if (!uploading) bitmap = null }) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(LocalAppSpacing.current.pageHorizontal), verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap)) {
                    Text("调整广告图片", style = MaterialTheme.typography.titleLarge)
                    Text("拖动调整位置，缩放后按 460 × 120 保存。", style = MaterialTheme.typography.bodySmall)
                    Canvas(Modifier.fillMaxWidth().aspectRatio(460f / 120f).pointerInput(source, zoom, uploading) {
                        detectDragGestures { change, delta ->
                            if (!uploading) {
                                change.consume()
                                offsetX += delta.x * 460f / size.width
                                offsetY += delta.y * 120f / size.height
                            }
                        }
                    }) {
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            native.save(); native.clipRect(0f, 0f, size.width, size.height)
                            native.scale(size.width / 460f, size.height / 120f)
                            draw(native); native.restore()
                        }
                    }
                    SettingsSlider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..4f, enabled = !uploading)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { bitmap = null }, enabled = !uploading) { Text("取消") }
                        Button(enabled = !uploading, onClick = {
                            uploading = true; onBusy(true)
                            scope.launch {
                                var file: File? = null
                                try {
                                    file = withContext(Dispatchers.IO) {
                                        val output = Bitmap.createBitmap(460, 120, Bitmap.Config.ARGB_8888)
                                        draw(AndroidCanvas(output))
                                        File.createTempFile("lsb-promotion-", ".jpg", context.cacheDir).also { target ->
                                            try { target.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 100, it) } }
                                            finally { output.recycle() }
                                        }
                                    }
                                    val fields = form.hidden.filter { it.first in setOf("_csrf", "id", "placement_key") } + ("display_type" to "image")
                                    onUploaded(repository.uploadPromotionImage(form.imageUpload, fields, file!!))
                                    bitmap = null
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { onError(error.message ?: "上传失败") }
                                finally { file?.delete(); uploading = false; onBusy(false) }
                            }
                        }) { Text(if (uploading) "上传中…" else "保存并上传") }
                    }
                }
            }
        }
    }
}
