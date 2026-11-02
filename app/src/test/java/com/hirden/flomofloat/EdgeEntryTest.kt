package com.hirden.flomofloat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.petterp.floatingx.core.FxControl
import com.petterp.floatingx.core.gesture.FxDrag
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdgeEntryTest {
    private lateinit var controller: ServiceController<CaptureService>
    private lateinit var service: CaptureService
    private val context get() = RuntimeEnvironment.getApplication()
    private val store get() = LocalStore(context)
    @Before fun setup() {
        context.getSharedPreferences("rumo_private", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("rumo_updates", Context.MODE_PRIVATE).edit().putBoolean("auto", false).commit()
        controller = Robolectric.buildService(CaptureService::class.java).create()
        service = controller.get()
        CaptureService::class.java.getDeclaredMethod("installBubble", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(service, true)
        idle()
    }
    @After fun cleanup() { controller.destroy() }
    private fun idle() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250)) }
    private fun control(name: String): FxControl? = CaptureService::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.get(service) as FxControl?
    private fun call(name: String) { CaptureService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service); idle() }
    private fun root(control: FxControl) = control.contentView!!.parent as FxWindowContainer
    private fun tapEntry() {
        val entry = control("bubble")!!
        val root = root(entry)
        val now = SystemClock.uptimeMillis()
        val x = entry.contentView!!.width / 2f; val y = entry.contentView!!.height / 2f
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to now, MotionEvent.ACTION_UP to now + 40)) {
            MotionEvent.obtain(now, time, action, x, y, 0).let { root.dispatchTouchEvent(it); it.recycle() }
        }
        idle()
    }
    @Test fun touchingHandleAndXKeepsPositionAndDraftAcrossRepeatedOpenings() {
        for (left in listOf(true, false)) {
            val before = control("bubble")!!
            before.moveTo(if (left) 0f else context.resources.displayMetrics.widthPixels - Ui.dp(context, 40).toFloat(), Ui.dp(context, 250).toFloat(), false)
            idle()
            val position = before.position
            repeat(2) {
                tapEntry()
                val close = control("bubble")!!
                assertTrue(close.isShowing)
                assertTrue((close.contentView as EdgeTabView).expanded)
                val windows = Shadow.extract<ShadowWindowManagerImpl>(context.getSystemService(WindowManager::class.java)).views
                assertSame("The close entry must remain above an overlapping note card", root(close), windows.last())
                assertEquals(position.x, close.position.x, 1f); assertEquals(position.y, close.position.y, 1f)
                assertEquals(FxDrag.DISABLED, close.config.gesture.drag)
                val flags = root(close).windowParams.flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                assertEquals(0, flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                control("editor")!!.contentView!!.findViewById<EditText>(R.id.note_input).setText("原位收起保留草稿")
                tapEntry()
                assertNull(control("editor"))
                assertFalse((control("bubble")!!.contentView as EdgeTabView).expanded)
                assertEquals(FxDrag.IMMEDIATE, control("bubble")!!.config.gesture.drag)
                assertEquals("原位收起保留草稿", store.draft().text)
            }
        }
    }
    @Test fun closeEntryStaysAvailableInSettingsAndSwitchingModes() {
        tapEntry(); call("showSettings")
        assertTrue((control("bubble")!!.contentView as EdgeTabView).expanded)
        tapEntry(); assertNull(control("editor"))
        tapEntry()
        store.saveAppearance(FloatingAppearance(OverlayMode.BALL, 40)); idle()
        assertFalse(control("bubble")!!.isShowing)
        store.saveAppearance(FloatingAppearance(OverlayMode.EDGE, 40)); idle()
        assertTrue(control("bubble")!!.isShowing)
        assertTrue((control("bubble")!!.contentView as EdgeTabView).expanded)
        tapEntry(); assertNull(control("editor"))
    }
    @Test fun accessibilityClickAlsoTogglesTheCloseEntry() {
        control("bubble")!!.contentView!!.performClick(); idle()
        assertNotNull(control("editor"))
        assertTrue(control("bubble")!!.contentView!!.contentDescription.contains("收起"))
        control("bubble")!!.contentView!!.performClick(); idle()
        assertNull(control("editor"))
    }
    @Test fun touchingCloseEntryDoesNotReleaseInputBeforeItsClickRemovesThePanel() {
        for (mode in listOf(OverlayMode.EDGE, OverlayMode.VERTICAL, OverlayMode.HORIZONTAL)) {
            store.saveAppearance(FloatingAppearance(mode)); idle(); tapEntry()
            val panel = root(control("editor")!!)
            val field = panel.findViewById<EditText>(R.id.note_input)
            field.setText("键盘不闪，草稿保留")
            val entry = root(control("bubble")!!)
            val location = IntArray(2); entry.getLocationOnScreen(location)
            val panelLocation = IntArray(2); panel.getLocationOnScreen(panelLocation)
            val x = location[0] + entry.width / 2f; val y = location[1] + entry.height / 2f
            MotionEvent.obtain(0, 20, MotionEvent.ACTION_OUTSIDE, x, y, 0).let {
                it.offsetLocation(-panelLocation[0].toFloat(), -panelLocation[1].toFloat())
                panel.dispatchTouchEvent(it); it.recycle()
            }
            assertEquals(0, panel.windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            assertTrue(field.hasFocus())
            assertEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED,
                panel.windowParams.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE)
            tapEntry()
            assertNull(control("editor")); assertEquals("键盘不闪，草稿保留", store.draft().text)
        }
    }
    @Test fun nativeDrawingShowsAnArcThenACrossWithoutChangingBounds() {
        val view = EdgeTabView(context)
        val width = view.layoutParams.width; val height = view.layoutParams.height
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        fun draw(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        fun rowCenter(bitmap: Bitmap, y: Int): Float {
            val xs = (0 until width).filter { bitmap.getPixel(it, y) ushr 24 > 128 }
            assertTrue(xs.isNotEmpty()); return xs.average().toFloat()
        }
        val arc = draw()
        assertTrue(rowCenter(arc, height / 2) > rowCenter(arc, Ui.dp(context, 11)) + Ui.dp(context, 3))
        view.expanded = true
        val cross = draw()
        assertEquals(width, view.layoutParams.width); assertEquals(height, view.layoutParams.height)
        val palette = AppTheme.palette(context)
        assertEquals(palette[Ui.onPrimary], cross.getPixel(width / 2, height / 2))
        assertEquals(palette[Ui.primary], cross.getPixel(width / 2 + Ui.dp(context, 10), height / 2))
    }
}
