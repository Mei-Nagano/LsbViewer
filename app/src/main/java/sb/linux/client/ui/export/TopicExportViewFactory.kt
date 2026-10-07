package sb.linux.client.ui.export

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

internal class TopicExportViewFactory(
    private val context: Context,
    private val density: Float,
) {
    fun tv(sizeSp: Float, bold: Boolean = false, color: Int = Color.DKGRAY): TextView =
        TextView(context).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            paint.isFakeBoldText = bold
            setTextIsSelectable(false)
        }

    fun tv(text: String, sizeSp: Float, bold: Boolean = false, color: Int = Color.DKGRAY): TextView =
        tv(sizeSp, bold, color).apply { setText(text) }

    fun lp(
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ): LinearLayout.LayoutParams = LinearLayout.LayoutParams(width, height)

    fun badge(text: String, backgroundColor: String, foregroundColor: String, sizeSp: Float = 10f): TextView =
        tv(text, sizeSp, color = Color.parseColor(foregroundColor)).apply {
            background = roundedBackground(backgroundColor, 4 * density)
            val padding = (sizeSp * 2.2f * context.resources.displayMetrics.scaledDensity / 3f).toInt().coerceAtLeast(8)
            setPadding(padding, padding / 3, padding, padding / 3)
        }

    fun roundedBackground(color: String, radiusPx: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusPx
        }
}
