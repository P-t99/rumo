package com.hirden.flomofloat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Looper
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class OverlayTextActionsTest {
    private lateinit var controller: ServiceController<CaptureService>
    private lateinit var service: CaptureService
    private lateinit var root: FxWindowContainer
    private val clipboard get() = service.getSystemService(ClipboardManager::class.java)
    private val field get() = root.findViewById<EditText>(R.id.note_input)
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun call(name: String) {
        CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service)
    }
    private fun currentRoot(): FxWindowContainer {
        val editor = CaptureService::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(service) as FxControl
        return editor.contentView!!.parent as FxWindowContainer
    }
    private fun click(id: Int) { idle(); root.findViewById<Button>(id).performClick(); idle() }
    private fun active() {
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        assertEquals(0, root.windowParams.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }
    @Before fun setup() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        app.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        controller = Robolectric.buildService(CaptureService::class.java).create()
        service = controller.get(); call("expand"); idle(); root = currentRoot()
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 800, 1200)
        clipboard.clearPrimaryClip()
    }
    @After fun cleanup() { controller.destroy() }
    @Test fun selectAllAndCopyUseCurrentFieldWithoutChangingDraftOrWindow() {
        field.setText("第一行\nemoji 🌱 和中文")
        val frame = listOf(root.windowParams.x, root.windowParams.y, root.windowParams.width, root.windowParams.height)
        click(R.id.text_select_all)
        assertEquals(0, field.selectionStart); assertEquals(field.length(), field.selectionEnd)
        click(R.id.text_copy)
        assertEquals("第一行\nemoji 🌱 和中文", clipboard.primaryClip!!.getItemAt(0).text.toString())
        assertEquals("第一行\nemoji 🌱 和中文", LocalStore(service).draft().text)
        assertEquals(frame, listOf(root.windowParams.x, root.windowParams.y, root.windowParams.width, root.windowParams.height))
        assertTrue(field.hasFocus()); active()
    }
    @Test fun partialSelectionCutsOnlySelectedTextAndPasteReplacesSelection() {
        field.setText("前面中间后面"); field.setSelection(2, 4)
        click(R.id.text_cut)
        assertEquals("中间", clipboard.primaryClip!!.getItemAt(0).text.toString())
        assertEquals("前面后面", field.text.toString())
        assertEquals("前面后面", LocalStore(service).draft().text)
        clipboard.setPrimaryClip(ClipData.newPlainText("external", "换行\n🌱"))
        field.setSelection(0, 2); click(R.id.text_paste)
        assertEquals("换行\n🌱后面", field.text.toString())
        assertEquals("换行\n🌱后面", LocalStore(service).draft().text)
        active()
    }
    @Test fun emptyNoteCanPasteAndEmptyClipboardDoesNotDeleteSelection() {
        clipboard.setPrimaryClip(ClipData.newPlainText("external", "从微信复制的文本"))
        click(R.id.text_paste)
        assertEquals("从微信复制的文本", field.text.toString())
        clipboard.clearPrimaryClip(); field.selectAll(); click(R.id.text_paste)
        assertEquals("从微信复制的文本", field.text.toString())
    }
    @Test fun actionsFollowTagsFocusAndDoNotModifyNote() {
        field.setText("正文保留")
        val tags = root.findViewById<EditText>(R.id.note_tags)
        tags.requestFocus(); tags.setText("#工作 #阅读")
        click(R.id.text_select_all); click(R.id.text_copy)
        assertEquals("#工作 #阅读", clipboard.primaryClip!!.getItemAt(0).text.toString())
        assertEquals("正文保留", field.text.toString())
    }
    @Test fun floatingRootHostsActionModeAndFinishesItExactlyOnceOnClose() {
        var destroyed = 0; var prepared = 0
        field.setText("可以选中"); field.setSelection(0, 2)
        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean { menu.add("复制"); return true }
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean { prepared++; return true }
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
            override fun onDestroyActionMode(mode: ActionMode) { destroyed++ }
        }
        val mode = field.startActionMode(callback, ActionMode.TYPE_FLOATING)
        assertNotNull(mode); assertEquals(ActionMode.TYPE_FLOATING, mode!!.type)
        assertTrue(prepared > 0); assertEquals(0, field.selectionStart); assertEquals(2, field.selectionEnd)
        controller.destroy(); mode.finish()
        assertEquals(1, destroyed)
    }
    @Test fun nativeLongPressSelectsTextWithoutCollapsingTheOverlay() {
        field.setText("hello world"); field.setSelection(2)
        field.setTextClassifier(android.view.textclassifier.TextClassifier.NO_OP)
        val x = field.totalPaddingLeft + field.layout.getPrimaryHorizontal(2)
        val y = field.totalPaddingTop + field.layout.getLineBottom(0) / 2f
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_DOWN, x, y, 0).let { field.dispatchTouchEvent(it); it.recycle() }
        field.performLongClick()
        MotionEvent.obtain(0, 600, MotionEvent.ACTION_UP, x, y, 0).let { field.dispatchTouchEvent(it); it.recycle() }
        idle()
        assertTrue("Native long press selection: ${field.selectionStart}..${field.selectionEnd}", field.selectionStart != field.selectionEnd)
        assertSame(root, currentRoot()); assertTrue(field.hasFocus()); active()
        click(R.id.text_copy)
        assertFalse(clipboard.primaryClip!!.getItemAt(0).text.isNullOrEmpty())
    }
    @Test fun outsideTouchStillReleasesInputAfterUsingSelectionActions() {
        field.setText("未完成"); click(R.id.text_select_all)
        MotionEvent.obtain(0, 20, MotionEvent.ACTION_OUTSIDE, 20f, 20f, 0).let { root.dispatchTouchEvent(it); it.recycle() }
        idle()
        assertTrue(root.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertFalse(field.hasFocus())
        assertEquals("未完成", LocalStore(service).draft().text)
    }
    @Test fun floatingSettingsPasteWorksAndMaskedApiCannotBeCopiedOrCut() {
        call("showSettings"); idle(); root = currentRoot()
        val api = root.findViewById<EditText>(R.id.api_input)
        api.requestFocus()
        clipboard.setPrimaryClip(ClipData.newPlainText("external", "https://flomoapp.com/iwh/test-not-a-secret"))
        click(R.id.text_paste)
        assertEquals("https://flomoapp.com/iwh/test-not-a-secret", api.text.toString())
        click(R.id.text_select_all)
        assertFalse("Masked API transformation: ${api.transformationMethod?.javaClass?.name}; focus: ${root.findFocus()?.id}", root.findViewById<Button>(R.id.text_copy).isEnabled)
        assertFalse(root.findViewById<Button>(R.id.text_cut).isEnabled)
        api.transformationMethod = null
        click(R.id.text_copy)
        assertEquals(api.text.toString(), clipboard.primaryClip!!.getItemAt(0).text.toString())
    }
}
