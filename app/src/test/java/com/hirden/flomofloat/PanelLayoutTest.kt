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
import com.petterp.floatingx.core.FxControl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class PanelLayoutTest {
    private lateinit var activity: Activity
    private lateinit var store: LocalStore
    @Before fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        activity.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = LocalStore(activity) { key }
    }
    private fun call(service: CaptureService, method: String) {
        if (method == "collapse") {
            CaptureService::class.java.getDeclaredMethod(method, Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(service, true)
        } else CaptureService::class.java.getDeclaredMethod(method).apply { isAccessible = true }.invoke(service)
    }
    private fun field(service: CaptureService, name: String): Any? = CaptureService::class.java.getDeclaredField(name).apply { isAccessible = true }.get(service)
    private fun panel(service: CaptureService) = (field(service, "editor") as FxControl).contentView!!
    private fun geometry(service: CaptureService) = field(service, "panelLayout") as PanelWindowController
    private fun touch(handle: View, action: Int, x: Float, y: Float) {
        MotionEvent.obtain(0, 20, action, x, y, 0).let { handle.dispatchTouchEvent(it); it.recycle() }
    }
    @Test fun sizesAndPositionsPersistSeparatelyForPagesAndOrientations() {
        val portrait = PanelLayout(310f, 290f, 20f, 40f)
        val landscape = PanelLayout(420f, 200f, 100f, 10f)
        store.savePanelLayout(false, false, portrait)
        store.savePanelLayout(false, true, landscape)
        store.savePanelLayout(true, false, portrait.copy(height = 460f))
        val reopened = LocalStore(activity)
        assertEquals(portrait, reopened.panelLayout(false, false))
        assertEquals(landscape, reopened.panelLayout(false, true))
        assertEquals(460f, reopened.panelLayout(true, false)!!.height, 0f)
        assertNull(reopened.panelLayout(true, true))
    }
    @Test fun resetClearsAllLayoutsAndPreservesApiDraftTagsAndAppearance() {
        val endpoint = "https://flomoapp.com/iwh/fixture/secret"
        store.saveApi(endpoint); store.saveDraft(Draft("保留草稿", "#临时"))
        store.saveTags(listOf("生活", "研究", "阅读"), "#默认")
        store.saveAppearance(FloatingAppearance(OverlayMode.BALL, 60))
        for (settings in listOf(false, true)) for (landscape in listOf(false, true)) {
            store.savePanelLayout(settings, landscape, PanelLayout(320f, 260f, 10f, 20f))
        }
        val settings = SettingsContent(activity, store) {}
        findButton(settings, "重置窗口布局")!!.performClick()
        for (page in listOf(false, true)) for (landscape in listOf(false, true)) assertNull(store.panelLayout(page, landscape))
        assertEquals(endpoint, store.api()); assertEquals(Draft("保留草稿", "#临时"), store.draft())
        assertEquals(listOf("生活", "研究", "阅读"), store.presets())
        assertEquals("#默认", store.defaultTags())
        assertEquals(FloatingAppearance(OverlayMode.BALL, 60), store.appearance())
    }
    @Test fun realResizeHandlePersistsAndReopeningRestoresWithoutLosingDraft() {
        store.saveDraft(Draft("缩放时继续写", "#临时"))
        store.savePanelLayout(false, false, PanelLayout(380f, 360f, 0f, 20f))
        val serviceController = Robolectric.buildService(CaptureService::class.java).create()
        val service = serviceController.get()
        try {
            call(service, "expand")
            val before = geometry(service).frame
            val handle = panel(service).findViewById<View>(R.id.resize_handle)
            val d = activity.resources.displayMetrics.density
            touch(handle, MotionEvent.ACTION_DOWN, 200f, 200f)
            touch(handle, MotionEvent.ACTION_MOVE, 200f - 40 * d, 200f - 50 * d)
            touch(handle, MotionEvent.ACTION_UP, 200f - 40 * d, 200f - 50 * d)
            val after = geometry(service).frame
            assertTrue(after.width < before.width); assertTrue(after.height < before.height)
            assertNotNull(store.panelLayout(false, false))
            call(service, "collapse"); call(service, "expand")
            assertEquals(after, geometry(service).frame)
            assertEquals("缩放时继续写", panel(service).findViewById<EditText>(R.id.note_input).text.toString())
            panel(service).findViewById<Button>(R.id.reset_panel_layout).performClick()
            assertNull(store.panelLayout(false, false))
            assertNotEquals(after, geometry(service).frame)
            assertEquals("缩放时继续写", store.draft().text)
        } finally { serviceController.destroy() }
    }
    @Test fun keyboardAvoidanceAndMovingDoNotOverwriteRememberedHeight() {
        store.savePanelLayout(false, false, PanelLayout(320f, 480f, 0f, 150f))
        val shell = SwipeLayout(activity) {}.apply { layoutParams = ViewGroup.LayoutParams(600, 900) }
        val controller = PanelWindowController(activity, store, shell, false)
        val original = store.panelLayout(false, false)!!
        controller.updateViewport(PanelViewport(10, 30, 810, 600, keyboard = true))
        assertTrue(controller.frame.y + controller.frame.height <= 600)
        assertEquals(original, store.panelLayout(false, false))
        controller.begin(resize = false); controller.change(20f, 10f); controller.finish(false)
        assertEquals(original.height, store.panelLayout(false, false)!!.height, 0f)
        controller.updateViewport(PanelViewport(10, 30, 810, 1800))
        assertEquals((480 * activity.resources.displayMetrics.density).toInt(), controller.frame.height)
    }
    @Test fun cancelledResizeAndOutOfBoundsDragDoNotLoseReachableLayout() {
        store.savePanelLayout(false, false, PanelLayout(320f, 300f, 10f, 20f))
        val shell = SwipeLayout(activity) {}.apply { layoutParams = ViewGroup.LayoutParams(600, 900) }
        val controller = PanelWindowController(activity, store, shell, false)
        val original = store.panelLayout(false, false)
        val before = controller.frame
        controller.begin(true); controller.change(-9000f, -9000f); controller.finish(true)
        assertEquals(before, controller.frame); assertEquals(original, store.panelLayout(false, false))
        controller.begin(false); controller.change(9000f, 9000f); controller.finish(false)
        val saved = store.panelLayout(false, false)!!
        assertTrue(saved.isValid())
        controller.updateViewport(PanelViewport(10, 30, 600, 800))
        assertTrue(controller.frame.x + controller.frame.width <= 600)
        assertTrue(controller.frame.y + controller.frame.height <= 800)
    }
    @Test fun leftwardResizeDoesNotTriggerCollapseSwipe() {
        var collapsed = 0; var changes = 0
        val shell = SwipeLayout(activity) { collapsed++ }
        val handle = PanelHandleView(activity, true, {}, { _, _ -> changes++ }, {})
        shell.addView(handle, ViewGroup.LayoutParams(400, 100))
        activity.setContentView(shell)
        shell.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        shell.layout(0, 0, 600, 400)
        touch(shell, MotionEvent.ACTION_DOWN, 300f, 50f)
        touch(shell, MotionEvent.ACTION_MOVE, 100f, 50f)
        touch(shell, MotionEvent.ACTION_UP, 100f, 50f)
        assertTrue(changes > 0); assertEquals(0, collapsed)
    }
    @Test fun compactWindowKeepsFooterVisibleAndScrollableFieldsReachable() {
        val serviceController = Robolectric.buildService(CaptureService::class.java).create()
        val service = serviceController.get()
        try {
            call(service, "expand")
            val view = panel(service) as ViewGroup
            val width = Ui.dp(activity, 280); val height = Ui.dp(activity, 180)
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, height)
            val footer = view.getChildAt(view.childCount - 1) as ViewGroup
            assertTrue(footer.top >= 0 && footer.bottom <= height)
            val send = findButton(view, "存入 flomo")!!
            assertTrue(send.width >= Ui.dp(activity, 75))
            assertTrue(send.right <= footer.width)
            val scroll = view.getChildAt(1) as android.widget.ScrollView
            assertTrue(scroll.height > 0)
            assertTrue(scroll.getChildAt(0).height > scroll.height)
        } finally { serviceController.destroy() }
    }
    private fun findButton(view: View, label: String): Button? {
        if (view is Button && view.text.toString() == label) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findButton(view.getChildAt(i), label)?.let { return it }
        return null
    }
}
