package com.hirden.flomofloat

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.petterp.floatingx.core.FxControl

/** App-owned geometry; FloatingX owns the window lifecycle. */
class PanelWindowController(private val context: Context, private val store: LocalStore,
    private val shell: View, private val settings: Boolean
) {
    private val density = context.resources.displayMetrics.density
    private val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private val wm = context.getSystemService(WindowManager::class.java)
    private var viewport = readViewport(ignoreKeyboard = false)
    private var desired = loadLayout()
    var frame: PanelFrame = PanelGeometry.fit(desired, viewport, density)
        private set
    private var control: FxControl? = null
    private var root: View? = null
    private var scheduled = false
    private var gesture: Gesture? = null
    private data class Gesture(val resize: Boolean, val preference: PanelLayout, val frame: PanelFrame)
    private val globalLayout = ViewTreeObserver.OnGlobalLayoutListener { scheduleRefresh() }
    private val layoutChanged = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> moveWindow() }
    private val refresh = Runnable { scheduled = false; if (root != null) updateViewport(readViewport(ignoreKeyboard = false)) }

    init { render() }

    fun attach(value: FxControl) {
        control = value
        root = shell.rootView.also { view ->
            view.viewTreeObserver.addOnGlobalLayoutListener(globalLayout)
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> scheduleRefresh(); insets }
            ViewCompat.requestApplyInsets(view)
        }
        shell.addOnLayoutChangeListener(layoutChanged)
        scheduleRefresh()
    }

    fun close() {
        // Closing the window cancels an unfinished drag instead of saving a partial gesture.
        gesture = null
        root?.let {
            if (it.viewTreeObserver.isAlive) it.viewTreeObserver.removeOnGlobalLayoutListener(globalLayout)
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
        }
        shell.removeCallbacks(refresh); shell.removeOnLayoutChangeListener(layoutChanged)
        root = null; control = null; scheduled = false
    }

    fun reset() { gesture = null; desired = loadLayout(); render() }

    fun begin(resize: Boolean) { gesture = Gesture(resize, desired, frame) }

    fun change(dx: Float, dy: Float) {
        val start = gesture ?: return
        val f = start.frame
        desired = if (start.resize) {
            PanelLayout((f.width + dx) / density, (f.height + dy) / density,
                (f.x - viewport.left) / density, (f.y - viewport.top) / density)
        } else {
            // Moving a keyboard-constrained window must not save its temporary smaller size.
            start.preference.copy(x = (f.x - viewport.left + dx) / density,
                y = (f.y - viewport.top + dy) / density)
        }
        render()
    }

    fun finish(cancelled: Boolean) {
        val start = gesture ?: return
        gesture = null
        if (cancelled) { desired = start.preference; render(); return }
        if (desired == start.preference) return
        desired = (if (start.resize) desired.copy(width = frame.width / density, height = frame.height / density) else desired)
            .copy(x = (frame.x - viewport.left) / density, y = (frame.y - viewport.top) / density)
        runCatching { store.savePanelLayout(settings, landscape, desired) }
            .onFailure { Toast.makeText(context, "布局保存失败，请重试", Toast.LENGTH_SHORT).show() }
    }

    internal fun updateViewport(value: PanelViewport) {
        if (value == viewport) return
        finish(cancelled = true)
        viewport = value; render()
    }

    private fun loadLayout() = store.panelLayout(settings, landscape)
        ?: PanelGeometry.default(readViewport(ignoreKeyboard = true), density, settings)

    private fun render() {
        frame = PanelGeometry.fit(desired, viewport, density)
        val lp = shell.layoutParams
        if (lp.width != frame.width || lp.height != frame.height) {
            lp.width = frame.width; lp.height = frame.height; shell.layoutParams = lp
        }
        moveWindow()
    }

    private fun moveWindow() {
        control?.let {
            if (it.position.x != frame.x.toFloat() || it.position.y != frame.y.toFloat()) {
                it.moveTo(frame.x.toFloat(), frame.y.toFloat(), animate = false)
            }
        }
    }

    private fun scheduleRefresh() {
        if (!scheduled && root != null) { scheduled = true; shell.post(refresh) }
    }

    @SuppressLint("DiscouragedApi")
    @Suppress("DEPRECATION")
    private fun readViewport(ignoreKeyboard: Boolean): PanelViewport {
        val screen = Rect()
        var left = 0; var top = 0; var right = 0; var bottom = 0; var imeHeight = 0
        if (Build.VERSION.SDK_INT >= 30) {
            screen.set(wm.maximumWindowMetrics.bounds)
            val insets = wm.currentWindowMetrics.windowInsets
            val bars = insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
            left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom
            if (!ignoreKeyboard) imeHeight = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom
        } else {
            val real = Point(); val usable = Point()
            wm.defaultDisplay.getRealSize(real); wm.defaultDisplay.getSize(usable)
            screen.set(0, 0, real.x, real.y)
            val statusId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            top = if (statusId != 0) context.resources.getDimensionPixelSize(statusId) else Ui.dp(context, 24)
            right = (real.x - usable.x).coerceAtLeast(0)
            bottom = (real.y - usable.y).coerceAtLeast(0)
        }
        if (screen.width() <= 0 || screen.height() <= 0) {
            val metrics = context.resources.displayMetrics
            screen.set(0, 0, metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1))
        }
        val margin = Ui.dp(context, 10)
        val safeBottom = screen.bottom - bottom
        var visibleBottom = safeBottom
        if (!ignoreKeyboard) {
            if (imeHeight > Ui.dp(context, 80)) visibleBottom = minOf(visibleBottom, screen.bottom - imeHeight)
            val visible = Rect()
            // The display frame is in screen coordinates, unlike a small overlay's clipped insets.
            // It is also the fallback for Android 8–10 and OEMs with incomplete IME insets.
            shell.rootView.getWindowVisibleDisplayFrame(visible)
            if (visible.width() >= screen.width() * .8f && visible.bottom > screen.top + top &&
                safeBottom - visible.bottom > Ui.dp(context, 80)) visibleBottom = minOf(visibleBottom, visible.bottom)
        }
        return PanelViewport(screen.left + left + margin, screen.top + top + margin,
            screen.right - right - margin, (visibleBottom - margin).coerceAtLeast(screen.top + top + margin + 1),
            safeBottom - visibleBottom > Ui.dp(context, 80))
    }
}
