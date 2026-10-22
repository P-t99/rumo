package com.hirden.flomofloat

import android.graphics.Rect
import android.text.method.PasswordTransformationMethod
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu

/** A WindowManager overlay has no DecorView to host Android's text ActionMode.
 * Keep the framework selection lifecycle, but render its editing actions inside
 * the existing window so tapping a menu cannot give another window input focus.
 */
class OverlayTextActions(private val shell: SwipeLayout, private val focus: (EditText) -> Unit) {
    private var mode: TextMode? = null
    private var closed = false
    private val buttons = linkedMapOf<Int, Button>()
    private val toolbar = Ui.row(shell.context).apply {
        id = R.id.text_edit_toolbar
        contentDescription = "文字编辑工具栏"
        Ui.background(this, Ui.bg, 8)
    }
    private val update = ViewTreeObserver.OnPreDrawListener { refresh(); true }

    init {
        val actions = listOf(
            android.R.id.selectAll to (R.id.text_select_all to "全选"),
            android.R.id.copy to (R.id.text_copy to "复制"),
            android.R.id.cut to (R.id.text_cut to "剪切"),
            android.R.id.paste to (R.id.text_paste to "粘贴")
        )
        actions.forEach { (action, display) ->
            val button = Ui.button(shell.context, display.second) { perform(action) }.apply {
                id = display.first
                textSize = 12f; minWidth = 0; minimumWidth = 0
                minHeight = 0; minimumHeight = 0; maxLines = 1
                setPadding(0, 0, 0, 0)
                // Touching an editing action must keep the editor and selection focused.
                isFocusable = false
            }
            buttons[action] = button
            toolbar.addView(button, LinearLayout.LayoutParams(0, Ui.dp(shell.context, 40), 1f))
        }
        // Fixed height within the saved frame; selection does not resize the window.
        shell.addView(toolbar, 1, LinearLayout.LayoutParams(-1, Ui.dp(shell.context, 40)))
        shell.textActions = this
        shell.viewTreeObserver.addOnPreDrawListener(update)
        refresh()
    }

    private fun editor(): EditText? = (shell.findFocus() as? EditText)?.takeIf { it.isEnabled }

    private fun refresh() {
        if (closed) return
        val field = editor()
        val selection = field != null && field.selectionStart >= 0 && field.selectionEnd >= 0 &&
            field.selectionStart != field.selectionEnd
        val masked = field?.transformationMethod is PasswordTransformationMethod
        buttons.forEach { (action, button) ->
            val enabled = field != null && when (action) {
                android.R.id.selectAll -> field.length() > 0
                android.R.id.copy, android.R.id.cut -> selection && !masked
                // Do not read the clipboard until the user explicitly taps Paste.
                else -> true
            }
            if (button.isEnabled != enabled) button.isEnabled = enabled
            button.alpha = if (enabled) 1f else .4f
        }
    }

    private fun perform(action: Int) {
        val field = editor() ?: return
        refresh()
        if (buttons[action]?.isEnabled != true) return
        focus(field)
        // Native EditText handles replacement, clipboard permissions, undo and IME state.
        field.onTextContextMenuItem(action)
        refresh()
    }

    fun start(view: View, callback: ActionMode.Callback, type: Int): ActionMode? {
        if (closed || view !is EditText || !view.isEnabled) return null
        mode?.finish()
        focus(view)
        val next = TextMode(view, callback, type)
        if (!callback.onCreateActionMode(next, next.menu)) return null
        mode = next
        next.invalidate()
        return next
    }

    fun close() {
        if (closed) return
        closed = true
        mode?.finish()
        if (shell.viewTreeObserver.isAlive) shell.viewTreeObserver.removeOnPreDrawListener(update)
        shell.textActions = null
    }

    private inner class TextMode(private val field: EditText, private val callback: ActionMode.Callback,
        modeType: Int
    ) : ActionMode() {
        private val items = PopupMenu(shell.context, toolbar).menu
        private var modeTitle: CharSequence? = null
        private var modeSubtitle: CharSequence? = null
        private var custom: View? = null
        private var finished = false
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
            callback.onPrepareActionMode(this, items)
            refresh()
        }
        override fun invalidateContentRect() {
            if (!finished && callback is Callback2) callback.onGetContentRect(this, field, Rect())
            refresh()
        }
        override fun finish() {
            if (finished) return
            finished = true
            if (mode === this) mode = null
            callback.onDestroyActionMode(this)
            custom = null
            refresh()
        }
    }
}
