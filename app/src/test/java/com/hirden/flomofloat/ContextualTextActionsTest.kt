package com.hirden.flomofloat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.textclassifier.TextClassifier
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ScrollView
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.system.container.FxWindowContainer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ContextualTextActionsTest {
    private lateinit var controller: ServiceController<CaptureService>
    private lateinit var service: CaptureService
    private lateinit var root: FxWindowContainer
    private val field get() = root.findViewById<EditText>(R.id.note_input)
    private val toolbar get() = root.findViewById<ViewGroup>(R.id.text_edit_toolbar)
    private val clipboard get() = service.getSystemService(ClipboardManager::class.java)
    private fun idle() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)) }
    private fun call(name: String) { CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service) }
    private fun currentRoot(): FxWindowContainer {
        val editor = CaptureService::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(service) as FxControl
        return editor.contentView!!.parent as FxWindowContainer
    }
    private fun layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1100, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 800, 1100); idle()
    }
    private fun frame() = listOf(root.windowParams.x, root.windowParams.y, root.windowParams.width, root.windowParams.height)
    private fun longPress(text: String) {
        field.setText(text); field.setTextClassifier(TextClassifier.NO_OP); layout()
        val x = field.totalPaddingLeft + field.layout.getPrimaryHorizontal(minOf(2, field.length()))
        val y = field.totalPaddingTop + field.layout.getLineBottom(0) / 2f
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_DOWN, x, y, 0).let { field.dispatchTouchEvent(it); it.recycle() }
        field.performLongClick()
        MotionEvent.obtain(0, 600, MotionEvent.ACTION_UP, x, y, 0).let { field.dispatchTouchEvent(it); it.recycle() }
        idle(); layout()
    }
    private fun click(id: Int) { root.findViewById<Button>(id).performClick(); idle(); layout() }
    private fun active() {
        assertTrue(field.hasFocus())
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }
    @Before fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        controller = Robolectric.buildService(CaptureService::class.java).create()
        service = controller.get(); call("expand"); idle(); root = currentRoot(); layout()
        // WindowManager overlays need an explicit focus event in Robolectric;
        // real Android sends it after FLAG_NOT_FOCUSABLE is removed.
        val viewRoot = org.robolectric.util.ReflectionHelpers.callInstanceMethod<Any>(root, "getViewRootImpl")
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowViewRootImpl>(viewRoot).callWindowFocusChanged(true)
        idle()
        clipboard.clearPrimaryClip()
    }
    @After fun cleanup() { controller.destroy() }
    @Test fun defaultReservesNoEditorSpaceAndKeepsTheCompatibilityOptionOff() {
        assertFalse(LocalStore(service).alwaysShowEditToolbar())
        assertEquals(View.GONE, toolbar.visibility)
        assertSame(root, toolbar.parent)
        val shell = field.parent.parent.parent as SwipeLayout
        assertEquals(3, shell.childCount)
        active()
    }
    @Test fun longPressShowsNearbyActionsAndCopyPreservesDraftFocusAndFrame() {
        val before = frame(); longPress("hello world")
        assertTrue(field.hasSelection())
        assertEquals(View.VISIBLE, toolbar.visibility)
        assertSame(root, toolbar.parent)
        assertEquals(View.GONE, root.findViewById<View>(R.id.text_paste).visibility)
        val selected = field.text.subSequence(field.selectionStart, field.selectionEnd).toString()
        assertTrue(toolbar.translationX >= 0 && toolbar.translationY >= 0)
        assertTrue(toolbar.translationX + toolbar.width <= root.width)
        assertTrue(toolbar.translationY + toolbar.height <= root.height)
        click(R.id.text_copy)
        assertEquals(selected, clipboard.primaryClip!!.getItemAt(0).text.toString())
        assertEquals("hello world", LocalStore(service).draft().text)
        assertEquals(View.GONE, toolbar.visibility); assertEquals(before, frame()); active()
    }
    @Test fun longPressEmptyNoteOffersPasteAtTheCaret() {
        clipboard.setPrimaryClip(ClipData.newPlainText("external", "跨应用\n粘贴 🌱"))
        longPress("")
        assertEquals(View.VISIBLE, toolbar.visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.text_paste).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.text_copy).visibility)
        click(R.id.text_paste)
        assertEquals("跨应用\n粘贴 🌱", field.text.toString())
        assertEquals(field.text.toString(), LocalStore(service).draft().text)
        assertEquals(View.GONE, toolbar.visibility); active()
    }
    @Test fun selectingAllFromTheNativeMenuCanThenCopyTheWholeNote() {
        longPress("hello world")
        click(R.id.text_select_all)
        assertEquals(0, field.selectionStart); assertEquals(field.length(), field.selectionEnd)
        assertEquals(View.VISIBLE, toolbar.visibility)
        click(R.id.text_copy)
        assertEquals("hello world", clipboard.primaryClip!!.getItemAt(0).text.toString())
    }
    @Test fun backDismissesSelectionBeforeReleasingTheEditor() {
        longPress("hello world")
        root.onImeBack!!.invoke(); idle()
        assertEquals(View.GONE, toolbar.visibility); active()
        root.onImeBack!!.invoke(); idle()
        assertFalse(field.hasFocus())
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }
    @Test fun outsideTouchClosesMenuAndStillReturnsInputToOtherApps() {
        longPress("hello world")
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_OUTSIDE, 20f, 20f, 0).let { root.dispatchTouchEvent(it); it.recycle() }
        idle()
        assertFalse(field.hasFocus()); assertEquals(View.GONE, toolbar.visibility)
        assertEquals("hello world", LocalStore(service).draft().text)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }
    @Test fun compatibilityPreferencePersistsAndSwitchesInPlaceWithoutLosingSelection() {
        longPress("hello world")
        val start = field.selectionStart; val end = field.selectionEnd; val before = frame()
        val store = LocalStore(service)
        store.saveAlwaysShowEditToolbar(true); idle(); layout()
        assertTrue(LocalStore(service).alwaysShowEditToolbar())
        assertTrue(toolbar.parent is SwipeLayout); assertEquals(View.VISIBLE, toolbar.visibility)
        assertEquals(start, field.selectionStart); assertEquals(end, field.selectionEnd)
        store.saveAlwaysShowEditToolbar(false); idle(); layout()
        assertFalse(LocalStore(service).alwaysShowEditToolbar()); assertSame(root, toolbar.parent)
        assertEquals(start, field.selectionStart); assertEquals(end, field.selectionEnd)
        assertEquals(before, frame()); active()
    }
    @Test fun mainAndFloatingSettingsShareTheOptionAndKeepUnsavedFields() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val main = activity.get().findViewById<CheckBox>(R.id.always_show_edit_toolbar)
            assertFalse(main.isChecked); main.isChecked = true; idle()
            call("showSettings"); idle(); root = currentRoot(); layout()
            val check = root.findViewById<CheckBox>(R.id.always_show_edit_toolbar)
            val api = root.findViewById<EditText>(R.id.api_input)
            api.setText("待保存地址"); api.setSelection(1, 3)
            assertTrue(check.isChecked); check.isChecked = false; idle()
            assertFalse(main.isChecked); assertFalse(LocalStore(service).alwaysShowEditToolbar())
            assertEquals("待保存地址", api.text.toString()); assertEquals(1, api.selectionStart); assertEquals(3, api.selectionEnd)
        } finally { activity.pause().stop().destroy() }
    }
    @Test fun menuPlacementClampsEdgesAndUsesOnlyTheKeyboardVisibleArea() {
        val bounds = Rect(8, 8, 280, 160)
        for (anchor in listOf(Rect(-50, -20, 0, 0), Rect(270, 130, 400, 300), Rect(80, 70, 120, 90))) {
            val placed = OverlayTextActions.place(bounds, anchor, 240, 40, 8)
            assertTrue(bounds.contains(placed)); assertEquals(240, placed.width()); assertEquals(40, placed.height())
        }
        assertEquals(22, OverlayTextActions.place(bounds, Rect(80, 70, 120, 90), 240, 40, 8).top)
        assertEquals(48, OverlayTextActions.place(bounds, Rect(80, 10, 120, 40), 240, 40, 8).top)
        val keyboardVisible = Rect(8, 8, 280, 100)
        assertTrue(keyboardVisible.contains(OverlayTextActions.place(keyboardVisible, Rect(200, 90, 250, 110), 240, 40, 8)))
    }
    @Test fun contextualMenuFollowsThemeWithoutChangingTheSelection() {
        longPress("hello world")
        val start = field.selectionStart; val end = field.selectionEnd; val menu = toolbar
        LocalStore(service).saveThemeMode(ThemeMode.DARK); idle()
        assertSame(menu, toolbar); assertEquals(View.VISIBLE, toolbar.visibility)
        assertEquals(AppTheme.palette(service)[Ui.ink], root.findViewById<Button>(R.id.text_copy).currentTextColor)
        assertEquals(start, field.selectionStart); assertEquals(end, field.selectionEnd)
    }
    @Test fun scrollingTheEditorOutOfViewHidesItsMenu() {
        LocalStore(service).savePanelLayout(false, false, PanelLayout(280f, 180f, 20f, 12f))
        (CaptureService::class.java.getDeclaredField("panelLayout").apply { isAccessible = true }.get(service) as PanelWindowController).reset()
        longPress("hello world")
        val scroll = field.parent.parent as ScrollView
        scroll.scrollTo(0, scroll.getChildAt(0).height); idle()
        assertFalse("The editor must actually be outside the scroll viewport", field.getLocalVisibleRect(Rect()))
        // Robolectric does not run a renderer frame for every programmatic scroll.
        root.viewTreeObserver.dispatchOnPreDraw()
        assertEquals(View.GONE, toolbar.visibility)
    }
    @Test fun leavingImmediatelyAfterLongPressCancelsTheDeferredMenu() {
        field.setText("未完草稿"); layout()
        field.performLongClick()
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_OUTSIDE, 20f, 20f, 0).let { root.dispatchTouchEvent(it); it.recycle() }
        idle()
        assertEquals(View.GONE, toolbar.visibility); assertFalse(field.hasFocus())
        assertEquals("未完草稿", LocalStore(service).draft().text)
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
    }
    @Test fun typingDismissesTheMenuWhileSavingTheEditAndKeepingFocus() {
        longPress("hello world")
        assertEquals(View.VISIBLE, toolbar.visibility)
        val before = frame()
        field.onCreateInputConnection(android.view.inputmethod.EditorInfo())!!.commitText("继续", 1)
        idle()
        assertEquals(View.GONE, toolbar.visibility); active()
        assertEquals(field.text.toString(), LocalStore(service).draft().text)
        assertTrue(field.text.toString().contains("继续")); assertEquals(before, frame())
    }
    @Test fun tappingTheEditorAgainDismissesTheMenuWithoutDroppingInput() {
        longPress("hello world")
        val shell = field.parent.parent.parent as SwipeLayout
        val f = IntArray(2); field.getLocationOnScreen(f)
        val p = IntArray(2); shell.getLocationOnScreen(p)
        val x = (f[0] - p[0] + 10).toFloat(); val y = (f[1] - p[1] + 30).toFloat()
        MotionEvent.obtain(0, 1000, MotionEvent.ACTION_DOWN, x, y, 0).let { shell.dispatchTouchEvent(it); it.recycle() }
        MotionEvent.obtain(0, 1020, MotionEvent.ACTION_UP, x, y, 0).let { shell.dispatchTouchEvent(it); it.recycle() }
        idle()
        assertEquals(View.GONE, toolbar.visibility); active()
    }
}
