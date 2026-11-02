package com.hirden.flomofloat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/** Owns its pointer stream so resizing cannot become a collapse swipe or double tap. */
class PanelHandleView(context: Context, val resize: Boolean,
    private val begin: () -> Unit, private val change: (Float, Float) -> Unit,
    private val finish: (Boolean) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = Ui.dp(context, 2).toFloat(); strokeCap = Paint.Cap.ROUND }
    private var startX = 0f
    private var startY = 0f
    private var pointer = -1
    init {
        Ui.bind(this, "paint") { paint.color = it[Ui.green]; invalidate() }
        isClickable = true; isFocusable = true
        contentDescription = if (resize) "拖动缩放窗口，方向键调整大小" else "拖动移动窗口，方向键调整位置"
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        if (resize) {
            for (offset in listOf(0f, 6f, 12f)) {
                canvas.drawLine(width / 2f - 7 * d + offset * d, height / 2f + 8 * d,
                    width / 2f + 8 * d, height / 2f - 7 * d + offset * d, paint)
            }
        } else {
            for (x in listOf(-3f, 3f)) for (y in listOf(-6f, 0f, 6f)) canvas.drawCircle(width / 2f + x * d, height / 2f + y * d, 1.4f * d, paint)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointer = event.getPointerId(0); startX = event.rawX; startY = event.rawY
                parent?.requestDisallowInterceptTouchEvent(true); begin()
            }
            MotionEvent.ACTION_MOVE -> if (pointer != -1) {
                // Ignore extra fingers; switching the active pointer can cause a large jump.
                val index = event.findPointerIndex(pointer)
                if (index >= 0) change(event.getX(index) + event.rawX - event.x - startX,
                    event.getY(index) + event.rawY - event.y - startY)
            }
            MotionEvent.ACTION_UP -> if (pointer != -1) {
                finish(false); pointer = -1; parent?.requestDisallowInterceptTouchEvent(false); performClick()
            }
            MotionEvent.ACTION_CANCEL -> cancelGesture()
            MotionEvent.ACTION_POINTER_UP -> if (event.getPointerId(event.actionIndex) == pointer) cancelGesture()
        }
        return true
    }
    private fun cancelGesture() {
        if (pointer != -1) finish(true)
        pointer = -1; parent?.requestDisallowInterceptTouchEvent(false)
    }
    override fun onDetachedFromWindow() { cancelGesture(); super.onDetachedFromWindow() }
    override fun performClick(): Boolean = super.performClick()
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val step = Ui.dp(context, 16).toFloat()
        val delta = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -step to 0f
            KeyEvent.KEYCODE_DPAD_RIGHT -> step to 0f
            KeyEvent.KEYCODE_DPAD_UP -> 0f to -step
            KeyEvent.KEYCODE_DPAD_DOWN -> 0f to step
            else -> return super.onKeyDown(keyCode, event)
        }
        begin(); change(delta.first, delta.second); finish(false); return true
    }
}
