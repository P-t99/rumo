package com.hirden.flomofloat

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class OverlayInputTest {
    private lateinit var activity: Activity
    private lateinit var controller: ServiceController<CaptureService>
    private lateinit var service: CaptureService
    @Before fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        activity.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        controller = Robolectric.buildService(CaptureService::class.java).create()
        service = controller.get(); call("expand")
    }
    @After fun cleanup() { controller.destroy() }
    private fun call(name: String) {
        if (name == "collapse") CaptureService::class.java.getDeclaredMethod(name, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(service, true)
        else CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service)
    }
    private fun root(): FxWindowContainer {
        val editor = CaptureService::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(service) as FxControl
        return editor.contentView!!.parent as FxWindowContainer
    }
    private fun measure(root: View) {
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 800, 1200)
    }
    private fun touch(view: View, action: Int) {
        MotionEvent.obtain(0, 20, action, 20f, 20f, 0).let { view.dispatchTouchEvent(it); it.recycle() }
    }
    private fun passive(root: FxWindowContainer) {
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM != 0)
    }
    private fun active(root: FxWindowContainer) {
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }
    @Test fun openingAndMovingPanelNeverAutomaticallyClaimsInput() {
        val root = root(); measure(root)
        passive(root)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL != 0)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH != 0)
        val handle = root.findViewById<View>(R.id.drag_handle)
        touch(handle, MotionEvent.ACTION_DOWN); touch(handle, MotionEvent.ACTION_UP)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        passive(root)
        assertFalse(root.findViewById<EditText>(R.id.note_input).hasFocus())
    }
    @Test fun outsideTouchReturnsInputAndRetappingNoteCanEditAgainWithoutLosingDraft() {
        val root = root(); measure(root)
        val field = root.findViewById<EditText>(R.id.note_input)
        field.setText("保留笔记")
        touch(field, MotionEvent.ACTION_DOWN); active(root)
        touch(root, MotionEvent.ACTION_OUTSIDE)
        shadowOf(Looper.getMainLooper()).idle()
        passive(root); assertFalse(field.hasFocus())
        val otherInput = EditText(activity)
        activity.setContentView(otherInput); otherInput.requestFocus()
        assertTrue(otherInput.onCreateInputConnection(EditorInfo())!!.commitText("聊天输入", 1))
        assertEquals("聊天输入", otherInput.text.toString())
        assertEquals("保留笔记", field.text.toString())
        touch(field, MotionEvent.ACTION_DOWN); active(root)
        field.onCreateInputConnection(EditorInfo())!!.commitText("继续", 1)
        assertTrue(field.text.toString().contains("继续"))
        assertEquals("聊天输入", otherInput.text.toString())
    }
    @Test fun queuedKeyboardRequestCannotStealFocusBackAfterOutsideTouch() {
        val root = root(); measure(root)
        val field = root.findViewById<EditText>(R.id.note_input)
        touch(field, MotionEvent.ACTION_DOWN)
        touch(root, MotionEvent.ACTION_OUTSIDE)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        passive(root); assertFalse(field.hasFocus())
    }
    @Test fun losingWindowFocusAlsoReleasesInputWhenNoOutsideEventWasDelivered() {
        val root = root(); measure(root)
        val field = root.findViewById<EditText>(R.id.note_input)
        touch(field, MotionEvent.ACTION_DOWN); active(root)
        ViewTreeObserver::class.java.getDeclaredMethod("dispatchOnWindowFocusChange", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(root.viewTreeObserver, false)
        shadowOf(Looper.getMainLooper()).idle()
        passive(root); assertFalse(field.hasFocus())
    }
    @Test fun collapsingInactivePanelDoesNotHideAnotherInputsKeyboard() {
        val root = root(); measure(root)
        touch(root.findViewById<EditText>(R.id.note_input), MotionEvent.ACTION_DOWN)
        touch(root, MotionEvent.ACTION_OUTSIDE)
        val otherInput = EditText(activity)
        activity.setContentView(otherInput); otherInput.requestFocus()
        val imm = activity.getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(otherInput, 0)
        assertTrue(shadowOf(imm).isSoftInputVisible)
        call("collapse")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(shadowOf(imm).isSoftInputVisible)
    }
    @Test fun settingsEditorUsesTheSameInputHandoffAndBackReleasesIt() {
        call("showSettings")
        val root = root(); measure(root); passive(root)
        val field = root.findViewById<EditText>(R.id.api_input)
        touch(field, MotionEvent.ACTION_DOWN); active(root)
        touch(root, MotionEvent.ACTION_OUTSIDE); passive(root)
        touch(field, MotionEvent.ACTION_DOWN); active(root)
        assertNotNull(root.onImeBack); root.onImeBack!!.invoke()
        shadowOf(Looper.getMainLooper()).idle()
        passive(root); assertFalse(field.hasFocus())
    }
    @Test fun closingPanelCancelsOldRequestsBeforeReopening() {
        val old = root(); measure(old)
        val oldField = old.findViewById<EditText>(R.id.note_input)
        touch(oldField, MotionEvent.ACTION_DOWN)
        call("collapse"); call("expand")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        passive(root()); assertFalse(oldField.hasFocus())
        assertFalse(root().findViewById<EditText>(R.id.note_input).hasFocus())
    }
}
