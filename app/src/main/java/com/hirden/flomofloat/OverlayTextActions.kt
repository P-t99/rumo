package com.hirden.flomofloat

import android.content.ClipboardManager
import android.graphics.Rect
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
                            view.startActionMode(EditorActions(view), ActionMode.TYPE_FLOATING)
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
        toolbar.elevation = if (pinned) 0f else Ui.dp(shell.context, 10).toFloat()
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
        if (current.callback is ActionMode.Callback2) {
            current.callback.onGetContentRect(current, current.field, anchor)
        } else {
            current.field.getDrawingRect(anchor)
            anchor.offset(-current.field.scrollX, -current.field.scrollY)
        }
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
        mode?.finish()
        focus(view)
        val next = TextMode(view, callback, type)
        if (!callback.onCreateActionMode(next, next.menu)) return null
        mode = next; next.invalidate()
        return next
    }

    /** Called before hiding the IME so Back first dismisses a selection menu. */
    fun dismissSelection(): Boolean {
        val current = mode ?: return false
        current.finish()
        return true
    }

    fun onPanelTouch(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return
        gestureGeneration++
        if (pinned) return
        val current = mode ?: return
        val rect = visibleOnScreen(current.field) ?: return
        val location = IntArray(2); shell.getLocationOnScreen(location)
        if (current.callback is EditorActions || !rect.contains((event.x + location[0]).toInt(), (event.y + location[1]).toInt())) dismissSelection()
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
        longPressFallbacks.forEach { (field, callback) -> field.removeCallbacks(callback) }
        longPressFallbacks.clear()
        pendingEdits.forEach { (field, callback) -> field.removeCallbacks(callback) }; pendingEdits.clear()
        watchers.forEach { (field, watcher) -> field.removeTextChangedListener(watcher) }; watchers.clear()
        editors.forEach { it.setOnLongClickListener(null) }; editors.clear()
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
