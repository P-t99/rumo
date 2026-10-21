package com.hirden.flomofloat

import android.app.Activity
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollapseGestureTest {
    private lateinit var activity: Activity
    @Before fun setup() { activity = Robolectric.buildActivity(Activity::class.java).setup().get() }
    private fun dp(value: Int) = Ui.dp(activity, value)
    private fun place(view: View) {
        activity.setContentView(view)
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(360), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(320), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, dp(360), dp(320))
    }
    private fun touch(view: View, action: Int, down: Long, time: Long, x: Int, y: Int) {
        val event = MotionEvent.obtain(down, time, action, dp(x).toFloat(), dp(y).toFloat(), 0)
        view.dispatchTouchEvent(event); event.recycle()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun doubleTap(view: View, x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, now, now, x, y)
        touch(view, MotionEvent.ACTION_UP, now, now + 40, x, y)
        touch(view, MotionEvent.ACTION_DOWN, now + 120, now + 120, x, y)
        touch(view, MotionEvent.ACTION_UP, now + 120, now + 160, x, y)
    }
    @Test fun shortSwipeCollapsesButTapJitterDoesNot() {
        var collapsed = 0
        fun swipe(distance: Int) {
            val layout = SwipeLayout(activity) { collapsed++ }; place(layout)
            val now = SystemClock.uptimeMillis()
            touch(layout, MotionEvent.ACTION_DOWN, now, now, 240, 160)
            touch(layout, MotionEvent.ACTION_MOVE, now, now + 60, 240 - distance, 162)
            touch(layout, MotionEvent.ACTION_UP, now, now + 90, 240 - distance, 162)
        }
        swipe(35); assertEquals(1, collapsed)
        collapsed = 0; swipe(10); assertEquals(0, collapsed)
    }
    @Test fun doubleTapWorksInScrollableBlankArea() {
        var collapsed = 0
        val layout = SwipeLayout(activity) { collapsed++ }
        val scroll = ScrollView(activity)
        val body = Ui.column(activity)
        body.addView(Ui.text(activity, "笔记标题"), LinearLayout.LayoutParams(-1, dp(40)))
        body.addView(View(activity), LinearLayout.LayoutParams(-1, dp(600)))
        scroll.addView(body); layout.addView(scroll, LinearLayout.LayoutParams(-1, -1)); place(layout)
        doubleTap(layout, 200, 150)
        assertEquals(1, collapsed)
    }
    @Test fun doubleTapOnInputsAndButtonsKeepsTheirOwnActions() {
        var collapsed = 0; var clicks = 0
        val layout = SwipeLayout(activity) { collapsed++ }
        layout.addView(Ui.edit(activity, "正文").apply { setText("双击选词") }, LinearLayout.LayoutParams(-1, dp(100)))
        layout.addView(Ui.button(activity, "设置") { clicks++ }, LinearLayout.LayoutParams(-1, dp(60)))
        place(layout)
        doubleTap(layout, 100, 40)
        doubleTap(layout, 100, 130)
        assertEquals(0, collapsed)
        assertEquals(2, clicks)
    }
}
