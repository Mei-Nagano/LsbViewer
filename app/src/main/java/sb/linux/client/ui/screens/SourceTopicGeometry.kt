package sb.linux.client.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

internal data class SourcePostPosition(val top: Float, val height: Float, val body: Float,
    val bodyHeight: Float, val match: Float?, val readingHeight: Float = bodyHeight)

/** 坐标统一为 Android 像素；CSS 像素比例由网页实际宽度确定。 */
internal class SourceTopicGeometry {
    val posts = mutableStateMapOf<Long, SourcePostPosition>()
    val headings = mutableStateMapOf<Int, Float>()
    var height by mutableFloatStateOf(1f)
    var slotTop by mutableFloatStateOf(0f)
    var commentsTop by mutableFloatStateOf(0f)
    var topInRoot by mutableFloatStateOf(0f)
    var pixelsPerCss by mutableFloatStateOf(1f)
    var ready by mutableStateOf(false)
    var query by mutableStateOf("")

    fun update(data: JSONObject, viewWidth: Int) {
        val width = data.optDouble("width", 0.0).toFloat()
        if (width <= 0 || viewWidth <= 0) return
        val ratio = viewWidth / width
        pixelsPerCss = ratio
        query = data.optString("query")
        height = (data.optDouble("height", 1.0).toFloat() * ratio).coerceAtLeast(1f)
        slotTop = data.optDouble("slot").toFloat() * ratio
        commentsTop = data.optDouble("comments").toFloat() * ratio
        val rows = data.optJSONArray("posts") ?: return
        val next = buildMap {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val id = row.optString("id").toLongOrNull() ?: continue
                fun pixel(key: String) = row.optDouble(key).toFloat() * ratio
                put(id, SourcePostPosition(pixel("top"), pixel("height"), pixel("body"), pixel("bodyHeight"),
                    if (row.isNull("match")) null else pixel("match"),
                    if (row.has("readingHeight")) pixel("readingHeight") else pixel("bodyHeight")))
            }
        }
        posts.keys.toList().filterNot { it in next }.forEach(posts::remove)
        next.forEach { (key, value) -> if (posts[key] != value) posts[key] = value }
        data.optJSONArray("headings")?.let { array ->
            headings.keys.toList().filter { it >= array.length() }.forEach(headings::remove)
            for (i in 0 until array.length()) headings[i] = array.getDouble(i).toFloat() * ratio
        }
        ready = true
    }
}
