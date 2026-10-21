package com.hirden.flomofloat

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import kotlin.math.abs

/** Short left swipe or double tap on free space; input controls keep their own gestures. */
class SwipeLayout(context: Context, private val collapse: () -> Unit) : LinearLayout(context) {
    private var startX = 0f
    private var startY = 0f
    private var eligible = false
    private var intercepted = false
    private val swipeDistance = Ui.dp(context, 28).toFloat()
    private var doubleTapEligible = false
    private var doubleTapInProgress = false
    private val taps = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleTapInProgress = true
            post { collapse() }
            return true
        }
    })
    init { orientation = VERTICAL }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            doubleTapEligible = !controlAt(this, event.x, event.y, includeButtons = true)
            doubleTapInProgress = false
            if (!doubleTapEligible) {
                MotionEvent.obtain(event).also { it.action = MotionEvent.ACTION_CANCEL; taps.onTouchEvent(it); it.recycle() }
            }
        }
        // Observe the stream before ScrollView claims it, so empty space inside a
        // scrollable body supports double tap as well as the title and margins.
        if (doubleTapEligible) taps.onTouchEvent(event)
        if (doubleTapInProgress) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) doubleTapInProgress = false
            return true
        }
        return super.dispatchTouchEvent(event)
    }
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x; startY = event.y
                eligible = !controlAt(this, event.x, event.y, includeButtons = false)
                intercepted = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> if (eligible && isLeftSwipe(event.x - startX, event.y - startY, swipeDistance)) {
                intercepted = true
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return false
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> if (eligible && isLeftSwipe(event.x - startX, event.y - startY, swipeDistance)) intercepted = true
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (eligible && intercepted && isLeftSwipe(event.x - startX, event.y - startY, swipeDistance)) post { collapse() }
                else if (!intercepted) performClick()
            }
            MotionEvent.ACTION_CANCEL -> { eligible = false; intercepted = false; parent?.requestDisallowInterceptTouchEvent(false) }
        }
        return true
    }
    override fun performClick(): Boolean = super.performClick()
    private fun controlAt(view: View, x: Float, y: Float, includeButtons: Boolean): Boolean {
        if (view is EditText || view is android.widget.AbsSeekBar) return true
        if (includeButtons && view !is ViewGroup && view.isClickable) return true
        if (view is ViewGroup) for (i in view.childCount - 1 downTo 0) {
            val child = view.getChildAt(i)
            val cx = x + view.scrollX - child.left
            val cy = y + view.scrollY - child.top
            if (child.visibility == VISIBLE && cx >= 0 && cy >= 0 && cx < child.width && cy < child.height && controlAt(child, cx, cy, includeButtons)) return true
        }
        return false
    }
    companion object {
        fun isLeftSwipe(dx: Float, dy: Float, threshold: Float): Boolean = dx < -threshold && abs(dx) > abs(dy) * 1.6f
    }
}
