package sb.linux.client.ui.screens

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/** 网页自身不纵向滚动；触摸与惯性通过 AndroidView 的嵌套滚动传给 Compose。 */
internal class SourceContentWebView(context: Context) : WebView(context), NestedScrollingChild3 {
    private val nested = NestedScrollingChildHelper(this)
    private val consumed = IntArray(2)
    private val offset = IntArray(2)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var lastY = 0f
    private enum class GestureAxis { UNDECIDED, HORIZONTAL, VERTICAL }
    private var axis = GestureAxis.UNDECIDED
    private var velocity: android.view.VelocityTracker? = null

    init { isNestedScrollingEnabled = true }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX; startY = event.rawY; lastY = event.rawY; axis = GestureAxis.UNDECIDED
                // 由本视图判定方向并通过 nested scroll 转交纵向，父列表不提前抢走手势。
                parent?.requestDisallowInterceptTouchEvent(true)
                velocity?.recycle(); velocity = android.view.VelocityTracker.obtain()
                trackVelocity(event)
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }
            MotionEvent.ACTION_MOVE -> {
                trackVelocity(event)
                if (axis == GestureAxis.UNDECIDED && event.pointerCount == 1) {
                    val dx = abs(event.rawX - startX)
                    val dy = abs(event.rawY - startY)
                    if (maxOf(dx, dy) > slop) {
                        axis = if (dx >= dy) GestureAxis.HORIZONTAL else GestureAxis.VERTICAL
                        if (axis == GestureAxis.VERTICAL) {
                            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                            super.onTouchEvent(cancel); cancel.recycle()
                        } else {
                            // 一旦开始横滑，直到手指松开都交给 Chromium 的表格/代码滚动容器。
                            stopNestedScroll(ViewCompat.TYPE_TOUCH)
                        }
                    }
                }
                if (axis == GestureAxis.VERTICAL) {
                    consumed.fill(0); offset.fill(0)
                    val dy = (lastY - event.rawY).roundToInt()
                    dispatchNestedPreScroll(0, dy, consumed, offset, ViewCompat.TYPE_TOUCH)
                    dispatchNestedScroll(0, 0, 0, dy - consumed[1], offset, ViewCompat.TYPE_TOUCH)
                    lastY = event.rawY
                    return true
                }
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (axis == GestureAxis.VERTICAL && event.actionMasked == MotionEvent.ACTION_UP) {
                    trackVelocity(event); velocity?.computeCurrentVelocity(1000)
                    val speed = -(velocity?.yVelocity ?: 0f)
                    if (!dispatchNestedPreFling(0f, speed)) dispatchNestedFling(0f, speed, false)
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH); velocity?.recycle(); velocity = null
                parent?.requestDisallowInterceptTouchEvent(false)
                if (axis == GestureAxis.VERTICAL) return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun trackVelocity(event: MotionEvent) {
        val screenEvent = MotionEvent.obtain(event)
        screenEvent.offsetLocation(event.rawX - event.x, event.rawY - event.y)
        velocity?.addMovement(screenEvent)
        screenEvent.recycle()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) = nested.setNestedScrollingEnabled(enabled)
    override fun isNestedScrollingEnabled() = nested.isNestedScrollingEnabled
    override fun startNestedScroll(axes: Int) = nested.startNestedScroll(axes)
    override fun startNestedScroll(axes: Int, type: Int) = nested.startNestedScroll(axes, type)
    override fun stopNestedScroll() = nested.stopNestedScroll()
    override fun stopNestedScroll(type: Int) = nested.stopNestedScroll(type)
    override fun hasNestedScrollingParent() = nested.hasNestedScrollingParent()
    override fun hasNestedScrollingParent(type: Int) = nested.hasNestedScrollingParent(type)
    override fun dispatchNestedScroll(dx: Int, dy: Int, ux: Int, uy: Int, offset: IntArray?) = nested.dispatchNestedScroll(dx, dy, ux, uy, offset)
    override fun dispatchNestedScroll(dx: Int, dy: Int, ux: Int, uy: Int, offset: IntArray?, type: Int) = nested.dispatchNestedScroll(dx, dy, ux, uy, offset, type)
    override fun dispatchNestedScroll(dx: Int, dy: Int, ux: Int, uy: Int, offset: IntArray?, type: Int, consumed: IntArray) = nested.dispatchNestedScroll(dx, dy, ux, uy, offset, type, consumed)
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offset: IntArray?) = nested.dispatchNestedPreScroll(dx, dy, consumed, offset)
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offset: IntArray?, type: Int) = nested.dispatchNestedPreScroll(dx, dy, consumed, offset, type)
    override fun dispatchNestedFling(x: Float, y: Float, consumed: Boolean) = nested.dispatchNestedFling(x, y, consumed)
    override fun dispatchNestedPreFling(x: Float, y: Float) = nested.dispatchNestedPreFling(x, y)
}
