package com.hirden.flomofloat

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InteractionTest {
    private lateinit var activity: Activity
    private lateinit var store: LocalStore
    @Before fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        activity.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = LocalStore(activity) { key }
    }
    @Test fun tagSettingsActuallySaveAndReload() {
        val settings = SettingsContent(activity, store) {}
        activity.setContentView(settings)
        settings.findViewById<EditText>(R.id.preset_one).setText("研究")
        settings.findViewById<EditText>(R.id.preset_two).setText("生活")
        settings.findViewById<EditText>(R.id.preset_three).setText("随读")
        settings.findViewById<EditText>(R.id.default_tags).setText("研究 #默认")
        findButton(settings, "保存标签")!!.performClick()
        val reopened = SettingsContent(activity, store) {}
        assertEquals("研究", reopened.findViewById<EditText>(R.id.preset_one).text.toString())
        assertEquals("#研究 #默认", store.draft().tags)
    }
    @Test fun swipeCollapsesButVerticalMovementAndTextSelectionDoNot() {
        val layout = SwipeLayout(activity) {}
        val field = Ui.edit(activity, "笔记")
        layout.addView(field, ViewGroup.LayoutParams(300, 100))
        layout.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
        layout.layout(0, 0, 400, 300)
        fun event(type: Int, x: Float, y: Float) = MotionEvent.obtain(0, 20, type, x, y, 0)
        fun gesture(y: Float, endY: Float): Boolean {
            event(MotionEvent.ACTION_DOWN, 350f, y).let { layout.onInterceptTouchEvent(it); it.recycle() }
            return event(MotionEvent.ACTION_MOVE, 40f, endY).let { layout.onInterceptTouchEvent(it).also { _ -> it.recycle() } }
        }
        assertTrue(gesture(200f, 205f))
        assertFalse(gesture(200f, 900f))
        event(MotionEvent.ACTION_DOWN, 250f, 50f).let { layout.onInterceptTouchEvent(it); it.recycle() }
        event(MotionEvent.ACTION_MOVE, 20f, 50f).let { assertFalse(layout.onInterceptTouchEvent(it)); it.recycle() }
    }
    @Test fun transparencySliderKeepsItsHorizontalGesture() {
        val layout = SwipeLayout(activity) {}
        val slider = android.widget.SeekBar(activity)
        layout.addView(slider, ViewGroup.LayoutParams(400, 100))
        layout.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
        layout.layout(0, 0, 400, 300)
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 350f, 50f, 0)
        val move = MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, 20f, 50f, 0)
        layout.onInterceptTouchEvent(down)
        assertFalse(layout.onInterceptTouchEvent(move))
        down.recycle(); move.recycle()
    }
    @Test fun blankAreaActuallyReceivesWholeSwipeAndCollapsesOnce() {
        var collapsed = 0
        val layout = SwipeLayout(activity) { collapsed++ }
        activity.setContentView(layout)
        layout.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
        layout.layout(0, 0, 400, 300)
        for ((type, x) in listOf(MotionEvent.ACTION_DOWN to 350f, MotionEvent.ACTION_MOVE to 80f, MotionEvent.ACTION_UP to 40f)) {
            val event = MotionEvent.obtain(0, 20, type, x, 200f, 0)
            assertTrue(layout.dispatchTouchEvent(event)); event.recycle()
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, collapsed)
    }
    @Test fun floatingSettingsOpensAndReturnsWithoutLosingDraft() {
        store.saveDraft(Draft("切换设置时保留", "#临时"))
        val controller = Robolectric.buildService(CaptureService::class.java).create()
        val service = controller.get()
        fun invoke(name: String) { CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service) }
        fun panel(): View {
            val field = CaptureService::class.java.getDeclaredField("editor").apply { isAccessible = true }
            return (field.get(service) as com.petterp.floatingx.core.FxControl).contentView!!
        }
        try {
            invoke("expand")
            val composer = panel()
            assertNotNull(composer.findViewById<EditText>(R.id.note_input))
            findButton(composer, "设置")!!.performClick()
            val settings = panel()
            assertNotNull(settings.findViewById<EditText>(R.id.api_input))
            assertNull(settings.findViewById<EditText>(R.id.note_input))
            findButton(settings, "返回笔记")!!.performClick()
            assertEquals("切换设置时保留", panel().findViewById<EditText>(R.id.note_input).text.toString())
        } finally { controller.destroy() }
    }
    private fun findButton(view: View, label: String): Button? {
        if (view is Button && view.text.toString() == label) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findButton(view.getChildAt(i), label)?.let { return it }
        return null
    }
}
