package com.hirden.flomofloat

import android.content.ClipboardManager
import android.graphics.Rect
import android.icu.text.BreakIterator
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import kotlin.math.abs

/** Host native text selection in a WindowManager overlay without a DecorView.
 * The contextual menu is a child of the same window: it neither takes focus nor
 * creates outside-touch notifications while the user taps an editing action.
 */
class OverlayTextActions(private val shell: SwipeLayout, private val focus: (EditText) -> Unit) {
    private val store = LocalStore(shell.context)
    private val clipboard = shell.context.getSystemService(ClipboardManager::class.java)
    private val host get() = shell.parent as? FrameLayout
    private var mode: TextMode? = null
    private var closed = false
    private var pinned = false
    private var gestureGeneration = 0
    private val longPressFallbacks = linkedMapOf<EditText, Runnable>()
    private val editors = mutableListOf<EditText>()
    private val watchers = linkedMapOf<EditText, TextWatcher>()
    private val pendingEdits = linkedMapOf<EditText, Runnable>()
    private data class Hold(val field: EditText, val x: Float, val y: Float, val rawX: Float,
        val rawY: Float, val started: Long, val generation: Int,
        var nativeHandled: Boolean = false, var fallbackUsed: Boolean = false)
    private var hold: Hold? = null
    private var pendingHold: Runnable? = null
    private val touchSlop = ViewConfiguration.get(shell.context).scaledTouchSlop
    private val buttons = linkedMapOf<Int, Button>()
    private val toolbar = Ui.row(shell.context).apply {
        id = R.id.text_edit_toolbar
        contentDescription = "文字编辑菜单"
        visibility = View.GONE
    }
    private val update = ViewTreeObserver.OnPreDrawListener { refresh(); true }

    init {
        listOf(
            android.R.id.selectAll to (R.id.text_select_all to "全选"),
            android.R.id.copy to (R.id.text_copy to "复制"),
            android.R.id.cut to (R.id.text_cut to "剪切"),
            android.R.id.paste to (R.id.text_paste to "粘贴")
        ).forEach { (action, display) ->
            val button = Ui.button(shell.context, display.second) { perform(action) }.apply {
                id = display.first
                textSize = 12f; minWidth = 0; minimumWidth = 0
                minHeight = 0; minimumHeight = 0; maxLines = 1
                setPadding(0, 0, 0, 0)
                isFocusable = false
            }
            buttons[action] = button
            toolbar.addView(button, LinearLayout.LayoutParams(0, Ui.dp(shell.context, 40), 1f))
        }
        shell.textActions = this
        shell.viewTreeObserver.addOnPreDrawListener(update)
        refreshPreference()
        fun bind(view: View) {
            if (view is EditText) {
                editors.add(view)
                if (view is OverlayEditText) view.onLongPressResult = { handled ->
                    hold?.takeIf { it.field === view }?.nativeHandled = handled
                }
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        gestureGeneration++
                        val current = mode?.takeIf { it.field === view } ?: return
                        pendingEdits.remove(view)?.let(view::removeCallbacks)
                        val dismiss = Runnable {
                            pendingEdits.remove(view)
                            if (!closed && mode === current) current.finish()
                        }
                        pendingEdits[view] = dismiss
                        view.post(dismiss)
                    }
                }
                watchers[view] = watcher; view.addTextChangedListener(watcher)
                view.setOnLongClickListener {
                    longPressFallbacks.remove(view)?.let(view::removeCallbacks)
                    val generation = gestureGeneration
                    val fallback = Runnable {
                        longPressFallbacks.remove(view)
                        // Let Android select the word and create its mode first. Some
                        // overlay/OEM paths select text without creating any ActionMode.
                        if (!closed && generation == gestureGeneration && view.isEnabled && view.hasFocus() &&
                            mode?.field !== view && (view.length() > 0 || clipboard.hasPrimaryClip())) {
                            start(view, EditorActions(view), ActionMode.TYPE_FLOATING)
                        }
                    }
                    longPressFallbacks[view] = fallback
                    view.post(fallback)
                    false
                }
            }
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) bind(view.getChildAt(i))
        }
        bind(shell)
    }

    fun refreshPreference() {
        if (closed) return
        pinned = store.alwaysShowEditToolbar()
        (toolbar.parent as? android.view.ViewGroup)?.removeView(toolbar)
        toolbar.translationX = 0f; toolbar.translationY = 0f
        if (pinned) {
            shell.addView(toolbar, 1, LinearLayout.LayoutParams(-1, Ui.dp(shell.context, 40)))
        } else {
            host?.addView(toolbar, FrameLayout.LayoutParams(1, Ui.dp(shell.context, 40), Gravity.TOP or Gravity.LEFT))
        }
        // Contextual mode is a sibling of the opaque card (12dp elevation).
        // Its Z must be above the card for both painting and touch dispatch.
        toolbar.elevation = if (pinned) 0f else shell.z + Ui.dp(shell.context, 4)
        Ui.background(toolbar, if (pinned) Ui.bg else Ui.surface, 8, if (pinned) null else Ui.line)
        buttons.values.forEach {
            Ui.textColor(it, if (pinned) Ui.green else Ui.ink)
            Ui.background(it, if (pinned) Ui.pale else Ui.surface, 8)
        }
        refresh()
    }

    fun refreshTheme() { Ui.refresh(toolbar) }

    private fun editor(): EditText? = (shell.findFocus() as? EditText)?.takeIf { it.isEnabled }

    private fun refresh() {
        if (closed) return
        val field = editor()
        val current = mode?.takeIf { it.field === field }
        val selected = field != null && field.selectionStart >= 0 && field.selectionEnd >= 0 &&
            field.selectionStart != field.selectionEnd
        val masked = field?.transformationMethod is PasswordTransformationMethod
        val canPaste = pinned || (current != null && clipboard.hasPrimaryClip())
        var visibleActions = 0
        buttons.forEach { (action, button) ->
            val enabled = field != null && when (action) {
                android.R.id.selectAll -> field.length() > 0 &&
                    (pinned || minOf(field.selectionStart, field.selectionEnd) != 0 ||
                        maxOf(field.selectionStart, field.selectionEnd) != field.length())
                android.R.id.copy, android.R.id.cut -> selected && !masked
                else -> canPaste
            }
            button.isEnabled = enabled
            button.alpha = if (enabled) 1f else .4f
            button.visibility = if (pinned || enabled) View.VISIBLE else View.GONE
            if (button.visibility == View.VISIBLE) visibleActions++
        }
        if (pinned) {
            toolbar.visibility = View.VISIBLE
        } else if (current == null || current.hidden || visibleActions == 0 || host == null) {
            toolbar.visibility = View.GONE
        } else {
            position(current, visibleActions)
        }
    }

    private fun position(current: TextMode, count: Int) {
        val parent = host ?: return
        if (parent.width <= 0 || parent.height <= 0) { toolbar.visibility = View.GONE; return }
        val fieldVisible = visibleOnScreen(current.field)
            ?: run { toolbar.visibility = View.GONE; return }
        val bounds = Rect(0, 0, parent.width, parent.height)
        val screen = IntArray(2); parent.getLocationOnScreen(screen)
        val visible = Rect(); parent.getWindowVisibleDisplayFrame(visible)
        if (!visible.isEmpty) {
            visible.offset(-screen[0], -screen[1])
            if (!bounds.intersect(visible)) { toolbar.visibility = View.GONE; return }
        }
        val margin = Ui.dp(shell.context, 6)
        if (bounds.width() > margin * 2 && bounds.height() > margin * 2) bounds.inset(margin, margin)
        val anchor = Rect()
        // OEM ActionMode callbacks can report coordinates in another window.
        // Use the public text layout in this window for both native and fallback modes.
        EditorActions(current.field).onGetContentRect(current, current.field, anchor)
        // An insertion caret can have zero width in the framework callback.
        anchor.right = maxOf(anchor.left + 1, anchor.right)
        anchor.bottom = maxOf(anchor.top + 1, anchor.bottom)
        val fieldPosition = IntArray(2); current.field.getLocationOnScreen(fieldPosition)
        anchor.offset(fieldPosition[0] - screen[0], fieldPosition[1] - screen[1])
        fieldVisible.offset(-screen[0], -screen[1])
        if (!anchor.intersect(fieldVisible)) { toolbar.visibility = View.GONE; return }
        val width = minOf(Ui.dp(shell.context, maxOf(64, count * 60)), bounds.width())
        val height = minOf(Ui.dp(shell.context, 40), bounds.height())
        val frame = place(bounds, anchor, width, height, Ui.dp(shell.context, 8))
        val lp = toolbar.layoutParams as FrameLayout.LayoutParams
        if (lp.width != frame.width() || lp.height != frame.height()) {
            lp.width = frame.width(); lp.height = frame.height(); toolbar.layoutParams = lp
        }
        toolbar.translationX = frame.left.toFloat(); toolbar.translationY = frame.top.toFloat()
        toolbar.visibility = View.VISIBLE
    }

    private fun perform(action: Int) {
        val field = editor() ?: return
        refresh()
        if (buttons[action]?.isEnabled != true) return
        focus(field)
        val current = mode?.takeIf { it.field === field }
        val item = current?.menu?.findItem(action)
        val handled = !pinned && current != null && item != null &&
            current.callback.onActionItemClicked(current, item)
        if (!handled) field.onTextContextMenuItem(action)
        if (!pinned && action != android.R.id.selectAll) dismissSelection()
        refresh()
    }

    fun start(view: View, callback: ActionMode.Callback, type: Int): ActionMode? {
        if (closed || view !is EditText || !view.isEnabled) return null
        val previous = mode
        val start = view.selectionStart; val end = view.selectionEnd
        mode?.finish()
        if (previous?.field === view && start >= 0 && end >= 0) view.setSelection(start, end)
        focus(view)
        val next = TextMode(view, callback, type)
        if (!callback.onCreateActionMode(next, next.menu)) return null
        mode = next; next.invalidate()
        return next
    }

    /** Called before hiding the IME so Back first dismisses a selection menu. */
    fun dismissSelection(): Boolean {
        val current = mode ?: return false
        gestureGeneration++; cancelHold()
        current.finish()
        return true
    }

    /** True means native UP would turn our long press into a tap; dispatch CANCEL instead. */
    fun onPanelTouch(event: MotionEvent): Boolean {
        var cancelNativeUp = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelHold(); gestureGeneration++
                if (!pinned) {
                    val current = mode
                    val rect = current?.let { visibleOnScreen(it.field) }
                    val location = IntArray(2); shell.getLocationOnScreen(location)
                    if (current != null && (current.callback is EditorActions || rect == null ||
                        !rect.contains((event.x + location[0]).toInt(), (event.y + location[1]).toInt()))) dismissSelection()
                }
                val point = IntArray(2); shell.getLocationOnScreen(point)
                val x = point[0] + event.x; val y = point[1] + event.y
                val field = editors.lastOrNull { it.isEnabled && visibleOnScreen(it)?.contains(x.toInt(), y.toInt()) == true }
                    ?: return false
                val origin = IntArray(2); field.getLocationOnScreen(origin)
                hold = Hold(field, x - origin[0], y - origin[1], event.rawX, event.rawY,
                    SystemClock.uptimeMillis(), gestureGeneration).also { pressed ->
                    pendingHold = Runnable {
                        pendingHold = null
                        showHold(pressed, afterRelease = false)
                    }.also { shell.postDelayed(it, ViewConfiguration.getLongPressTimeout().toLong() + 80) }
                }
            }
            MotionEvent.ACTION_MOVE -> hold?.let {
                if (abs(event.rawX - it.rawX) > touchSlop || abs(event.rawY - it.rawY) > touchSlop) {
                    it.field.cancelLongPress(); cancelHold(); gestureGeneration++
                }
            }
            MotionEvent.ACTION_UP -> {
                val pressed = hold
                cancelHold()
                if (pressed != null && SystemClock.uptimeMillis() - pressed.started >= ViewConfiguration.getLongPressTimeout()) {
                    cancelNativeUp = pressed.fallbackUsed && !pressed.nativeHandled
                    // Dispatch runs before EditText receives UP. Recheck afterwards:
                    // native selection/insertion modes may start asynchronously on release.
                    pendingHold = Runnable {
                        pendingHold = null
                        showHold(pressed, afterRelease = true)
                    }.also { shell.post(it) }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                hold?.field?.cancelLongPress(); cancelHold(); gestureGeneration++
            }
        }
        return cancelNativeUp
    }

    private fun cancelHold() {
        pendingHold?.let(shell::removeCallbacks); pendingHold = null; hold = null
    }

    private fun showHold(pressed: Hold, afterRelease: Boolean) {
        val field = pressed.field
        if (closed || pressed.generation != gestureGeneration || !field.isEnabled || !field.hasFocus() ||
            !field.isAttachedToWindow || visibleOnScreen(field) == null) return
        val current = mode?.takeIf { it.field === field }
        if (current != null) {
            if (!field.hasSelection()) { selectAtPress(pressed); pressed.fallbackUsed = true }
            if (afterRelease) current.show()
            else refresh()
            return
        }
        if (!field.hasSelection()) { selectAtPress(pressed); pressed.fallbackUsed = true }
        if (field.length() > 0 || clipboard.hasPrimaryClip()) start(field, EditorActions(field), ActionMode.TYPE_FLOATING)
    }

    private fun selectAtPress(pressed: Hold) {
        val field = pressed.field
        val layout = field.layout ?: return
        val offset = field.getOffsetForPosition(pressed.x, pressed.y).coerceIn(0, field.length())
        val x = pressed.x - field.totalPaddingLeft + field.scrollX
        val y = pressed.y - field.totalPaddingTop + field.scrollY
        val line = layout.getLineForVertical(y.toInt())
        if (field.length() == 0 || y < 0 || y >= layout.height ||
            x < layout.getLineLeft(line) || x >= layout.getLineRight(line)) {
            field.setSelection(offset); return
        }
        val text = field.text.toString()
        val caret = layout.getPrimaryHorizontal(offset)
        val rtl = offset > 0 && layout.isRtlCharAt(offset - 1)
        val index = (if (offset > 0 && (if (rtl) x > caret else x < caret)) offset - 1 else offset)
            .coerceIn(0, text.length - 1)
        val words = BreakIterator.getWordInstance(field.textLocale).apply { setText(text) }
        var start = words.preceding(index + 1).coerceAtLeast(0)
        var end = words.following(index).let { if (it == BreakIterator.DONE) text.length else it }
        if (text.substring(start, end).all { it.isWhitespace() }) { field.setSelection(offset); return }
        // Emoji, combining marks and ZWJ sequences must stay intact.
        val characters = BreakIterator.getCharacterInstance(field.textLocale).apply { setText(text) }
        start = minOf(start, characters.preceding(index + 1).coerceAtLeast(0))
        end = maxOf(end, characters.following(index).let { if (it == BreakIterator.DONE) text.length else it })
        field.setSelection(start, end)
    }

    private fun visibleOnScreen(view: View): Rect? {
        val rect = Rect()
        if (!view.getLocalVisibleRect(rect)) return null
        val location = IntArray(2); view.getLocationOnScreen(location)
        rect.offset(location[0] - view.scrollX, location[1] - view.scrollY)
        return rect
    }

    fun close() {
        if (closed) return
        closed = true; mode?.finish()
        cancelHold()
        longPressFallbacks.forEach { (field, callback) -> field.removeCallbacks(callback) }
        longPressFallbacks.clear()
        pendingEdits.forEach { (field, callback) -> field.removeCallbacks(callback) }; pendingEdits.clear()
        watchers.forEach { (field, watcher) -> field.removeTextChangedListener(watcher) }; watchers.clear()
        editors.forEach {
            it.setOnLongClickListener(null)
            if (it is OverlayEditText) it.onLongPressResult = null
        }; editors.clear()
        if (shell.viewTreeObserver.isAlive) shell.viewTreeObserver.removeOnPreDrawListener(update)
        (toolbar.parent as? android.view.ViewGroup)?.removeView(toolbar)
        shell.textActions = null
    }

    /** Public editor operations remain available even if an OEM omits its ActionMode. */
    private inner class EditorActions(private val field: EditText) : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            listOf(android.R.id.selectAll to "全选", android.R.id.copy to "复制",
                android.R.id.cut to "剪切", android.R.id.paste to "粘贴").forEach { (id, label) -> menu.add(0, id, 0, label) }
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = true
        override fun onActionItemClicked(mode: ActionMode, item: android.view.MenuItem) = field.onTextContextMenuItem(item.itemId)
        override fun onDestroyActionMode(mode: ActionMode) {}
        override fun onGetContentRect(mode: ActionMode, view: View, rect: Rect) {
            val layout = field.layout ?: return super.onGetContentRect(mode, view, rect)
            val start = field.selectionStart.coerceIn(0, field.length())
            val end = field.selectionEnd.coerceIn(0, field.length())
            val first = layout.getLineForOffset(minOf(start, end))
            val last = layout.getLineForOffset(maxOf(start, end))
            val x1 = layout.getPrimaryHorizontal(start); val x2 = layout.getPrimaryHorizontal(end)
            val left = if (first == last) minOf(x1, x2).toInt() else 0
            val right = if (first == last) maxOf(x1, x2).toInt() + 1 else field.width - field.totalPaddingLeft - field.totalPaddingRight
            rect.set(left + field.totalPaddingLeft - field.scrollX, layout.getLineTop(first) + field.totalPaddingTop - field.scrollY,
                right + field.totalPaddingLeft - field.scrollX, layout.getLineBottom(last) + field.totalPaddingTop - field.scrollY)
        }
    }

    private inner class TextMode(val field: EditText, val callback: ActionMode.Callback, modeType: Int) : ActionMode() {
        private val items = PopupMenu(shell.context, toolbar).menu
        private var modeTitle: CharSequence? = null
        private var modeSubtitle: CharSequence? = null
        private var custom: View? = null
        private var finished = false
        var hidden = false
            private set
        private val reveal = Runnable { if (!finished) { hidden = false; refresh() } }
        init { type = modeType }
        override fun getMenu(): Menu = items
        override fun getMenuInflater() = MenuInflater(shell.context)
        override fun setTitle(value: CharSequence?) { modeTitle = value }
        override fun setTitle(resId: Int) { modeTitle = shell.context.getText(resId) }
        override fun getTitle(): CharSequence? = modeTitle
        override fun setSubtitle(value: CharSequence?) { modeSubtitle = value }
        override fun setSubtitle(resId: Int) { modeSubtitle = shell.context.getText(resId) }
        override fun getSubtitle(): CharSequence? = modeSubtitle
        override fun setCustomView(view: View?) { custom = view }
        override fun getCustomView(): View? = custom
        override fun invalidate() {
            if (finished) return
            callback.onPrepareActionMode(this, items); refresh()
        }
        override fun invalidateContentRect() { if (!finished) refresh() }
        override fun hide(duration: Long) {
            if (finished) return
            shell.removeCallbacks(reveal)
            hidden = duration != 0L
            if (hidden) shell.postDelayed(reveal, if (duration < 0) ViewConfiguration.getDefaultActionModeHideDuration().toLong() else duration)
            refresh()
        }
        fun show() {
            if (finished) return
            shell.removeCallbacks(reveal); hidden = false; refresh()
        }
        override fun finish() {
            if (finished) return
            finished = true; shell.removeCallbacks(reveal)
            if (mode === this) mode = null
            callback.onDestroyActionMode(this); custom = null; refresh()
        }
    }

    companion object {
        /** Prefer above the selected text, then below, and always stay in the visible window. */
        internal fun place(bounds: Rect, anchor: Rect, width: Int, height: Int, gap: Int): Rect {
            val w = width.coerceIn(1, bounds.width().coerceAtLeast(1))
            val h = height.coerceIn(1, bounds.height().coerceAtLeast(1))
            val x = (anchor.centerX() - w / 2).coerceIn(bounds.left, (bounds.right - w).coerceAtLeast(bounds.left))
            val preferred = when {
                anchor.top - gap - h >= bounds.top -> anchor.top - gap - h
                anchor.bottom + gap + h <= bounds.bottom -> anchor.bottom + gap
                else -> bounds.top
            }
            val y = preferred.coerceIn(bounds.top, (bounds.bottom - h).coerceAtLeast(bounds.top))
            return Rect(x, y, x + w, y + h)
        }
    }
}
